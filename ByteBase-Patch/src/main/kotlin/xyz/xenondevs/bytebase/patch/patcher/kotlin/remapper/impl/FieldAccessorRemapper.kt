package xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.impl

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import xyz.xenondevs.bytebase.asm.access.Access
import xyz.xenondevs.bytebase.asm.access.ReferencingAccess
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.asm.insnListOf
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.MemberReference
import xyz.xenondevs.bytebase.patch.PatchMode
import xyz.xenondevs.bytebase.patch.Patcher
import xyz.xenondevs.bytebase.patch.annotation.FieldAccessor
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.MappingsContainer
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.PropertyRemapper
import xyz.xenondevs.bytebase.patch.util.RuntimeClassPath
import xyz.xenondevs.bytebase.patch.util.StringUtils.possessive
import xyz.xenondevs.bytebase.patch.util.UnsafeAccess
import xyz.xenondevs.bytebase.util.accessWrapper
import xyz.xenondevs.bytebase.util.desc
import xyz.xenondevs.bytebase.util.getLdcTypeInstruction
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.representedClass
import java.lang.reflect.Field
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType

internal class FieldAccessorRemapper(
    patcher: Patcher,
    patch: Patcher.LoadedPatch,
    mappings: MappingsContainer,
    newDefinitions: MutableSet<ClassWrapper>
) : PropertyRemapper<FieldAccessor>(patcher, patch, mappings, newDefinitions) {
    
    val logger get() = patcher.logger
    
    override fun <T> processProperty(annotation: FieldAccessor, prop: KProperty<T>) {
        val target = patch.target
        
        val name = annotation.name.ifEmpty(prop::name)
        val desc = annotation.desc.ifEmpty(prop::desc)
        
        // Check if the field and target field have the same descriptor
        check(desc == prop.desc) { "The descriptor of the field accessor \"${annotation.name}\" doesn't match the descriptor of the property \"${prop.name}\"." }
        
        logger.debugLine()
        logger.debug("- Processing property \"${prop.name}\" (Accessing \"$name.$desc\")")
        
        // Try to find the field in the target class' inheritance tree and throw an exception if it doesn't exist
        val fieldRef = target.inheritanceTree.resolveFieldRef(name, desc)
            ?: throw NoSuchFieldException("Could not find field $name with descriptor $desc in class ${target.name.possessive()} InheritanceTree.")
        val access = fieldRef.resolveField().accessWrapper
        
        // Check if the field is accessible and overwrite the access if allowed
        val (isAccessible, isMutable) = tryFixAccess(fieldRef, access, annotation, prop)
        
        // Get the getter and setter instructions and add them to the mappings
        val getterInsns = getCorrectGetter(fieldRef, fieldRef.resolveField().accessWrapper, isAccessible)
        if (prop is KMutableProperty<*>) {
            val setterInsn = getCorrectSetter(fieldRef, fieldRef.resolveField().accessWrapper, isAccessible, isMutable)
            mappings.addRemap(prop, getterInsns, setterInsn)
        } else {
            mappings.addRemap(prop, getterInsns)
        }
    }
    
    private fun tryFixAccess(
        ref: MemberReference,
        access: ReferencingAccess,
        annotation: FieldAccessor,
        prop: KProperty<*>
    ): Pair<Boolean, Boolean> { // isAccessible, isMutable
        logger.debug("-- Checking access of field \"${ref.name}\"")
        val target = patch.target
        val shouldBeMutable = prop is KMutableProperty<*>
        
        var isAccessible = target.canAccess(ref, access, assertInSuper = true)
        var isMutable = !access.isFinal()
        
        if (isAccessible && (isMutable || !shouldBeMutable)) {
            logger.debug("-- Field \"${ref.name}\" is accessible" + if (isMutable) " and mutable" else "")
            return isAccessible to isMutable
        }
        
        fun fixAccess() {
            if (!isAccessible) {
                access.setPublic(true).setProtected(false).setPrivate(false)
                isAccessible = true
            }
            if (!isMutable && shouldBeMutable) {
                access.setFinal(false)
                isMutable = true
                if (access.isStatic()) {
                    val type = Type.getType(prop.returnType.javaType.representedClass)
                    if (type.sort < Type.ARRAY || type.descriptor == "Ljava/lang/String;") {
                        logger.warn("-- Field \"${ref.name}\" was made mutable but is static and has a primitive or string type.")
                        logger.warn("-- Any changes to the field will not be visible to already compiled classes.")
                        logger.warn("-- Javac inlines such fields and no actual field access occurs during runtime.")
                    }
                }
            }
        }
        
        val owner = ref.owner
        if (owner == target.name) {
            if (patch.patchMode == PatchMode.CLASSLOADER) {
                // The patch is accessing a direct field of the target class and the patch is in CLASSLOADER mode.
                // We can safely overwrite the access.
                logger.debug("-- Field \"${ref.name}\" access can be overwritten in CLASSLOADER mode. Overwriting access...")
                fixAccess()
            } else {
                logger.debug("-- Field \"${ref.name}\" access cannot be overwritten.")
            }
        } else if (!RuntimeClassPath.isClassLoaded(owner)) {
            // The patch is accessing a field of a super class of the target class and the owner class is not loaded.
            // We can safely overwrite the access.
            // TODO: Should this check if the super class has its own patch and get that patch's patchMode?
            logger.debug("-- Field \"${ref.name}\" is not accessible, but the owner class is not loaded. Overwriting access.")
            fixAccess()
            patcher.additionalClassLoaderDefs.add(owner)
        } else {
            logger.debug("-- Field \"${ref.name}\" access cannot be overwritten.")
        }
        
        return isAccessible to isMutable
    }
    
    private fun getCorrectGetter(ref: MemberReference, access: Access, isAccessible: Boolean): InsnList {
        logger.debug("-- Generating getter mappings")
        
        // Check whether the target class can access the field directly
        if (isAccessible) {
            logger.debug("-- Field is accessible from the target class. Using direct access.")
            // The target class can access the field directly, so we can just use the field directly
            return insnListOf(FieldInsnNode(if (access.isStatic()) Opcodes.GETSTATIC else Opcodes.GETFIELD, ref.owner, ref.name, ref.desc))
        } else {
            // Can't access field, get field offset and use Unsafe
            val rtField = ref.resolveRuntimeField(access)
            val fieldType = Type.getType(rtField.type)
            val (uMethod, uDesc) = getUnsafeSignature(true, fieldType)
            
            logger.debug("-- Field isn't accessible from the target class. Using Unsafe to access the field ($uMethod$uDesc)")
            if (!access.isStatic()) {
                val offset = UnsafeAccess.getFieldOffset(rtField)
                return buildInsnList {
                    ldc(offset)
                    invokeStatic(UnsafeAccess::class.internalName, uMethod, uDesc)
                }
                
            } else {
                val offset = UnsafeAccess.getStaticFieldOffset(rtField)
                return buildInsnList {
                    pop()
                    add(Type.getType(rtField.declaringClass).getLdcTypeInstruction())
                    ldc(offset)
                    invokeStatic(UnsafeAccess::class.internalName, uMethod, uDesc)
                }
                
            }
        }
    }
    
    private fun getCorrectSetter(ref: MemberReference, access: Access, isAccessible: Boolean, isMutable: Boolean): InsnList {
        logger.debug("-- Generating setter mappings")
        // Check whether the target class can set the field directly
        if (isAccessible && isMutable) {
            logger.debug("-- Field is accessible and not final. Using direct access.")
            return insnListOf(FieldInsnNode(if (access.isStatic()) Opcodes.PUTSTATIC else Opcodes.PUTFIELD, ref.owner, ref.name, ref.desc))
            
        } else {
            // Can't access field, get field offset and use Unsafe.
            val rtField = ref.resolveRuntimeField(access)
            val fieldType = Type.getType(rtField.type)
            val (uSetName, uSetDesc) = getUnsafeSignature(false, fieldType)
            
            logger.debug("-- Field isn't accessible from the target class. Using Unsafe to access the field ($uSetName$uSetDesc)")
            if (access.isStatic()) {
                val offset = UnsafeAccess.getStaticFieldOffset(rtField)
                return buildInsnList {
                    swap()
                    pop()
                    add(Type.getType(rtField.declaringClass).getLdcTypeInstruction())
                    swap()
                    ldc(offset)
                    invokeStatic(UnsafeAccess::class.internalName, uSetName, uSetDesc)
                }
                
            } else {
                val offset = UnsafeAccess.getFieldOffset(rtField)
                return buildInsnList {
                    ldc(offset)
                    invokeStatic(UnsafeAccess::class.internalName, uSetName, uSetDesc)
                }
            }
        }
    }
    
    private fun MemberReference.resolveRuntimeField(access: Access): Field {
        val clazz = patcher.runtimeClassGetter(resolveOwner())
        return if (access.isPublic()) clazz.getField(name) else clazz.getDeclaredField(name)
    }
    
    private fun getUnsafeSignature(isGet: Boolean, type: Type): Pair<String, String> { // name, desc
        if (isGet) {
            return when (type.sort) {
                Type.BOOLEAN -> "getBoolean" to "(Ljava/lang/Object;J)Z"
                Type.BYTE -> "getByte" to "(Ljava/lang/Object;J)B"
                Type.CHAR -> "getChar" to "(Ljava/lang/Object;J)C"
                Type.DOUBLE -> "getDouble" to "(Ljava/lang/Object;J)D"
                Type.FLOAT -> "getFloat" to "(Ljava/lang/Object;J)F"
                Type.INT -> "getInt" to "(Ljava/lang/Object;J)I"
                Type.LONG -> "getLong" to "(Ljava/lang/Object;J)J"
                Type.SHORT -> "getShort" to "(Ljava/lang/Object;J)S"
                else -> "getObject" to "(Ljava/lang/Object;J)Ljava/lang/Object;"
            }
        } else {
            return when (type.sort) {
                Type.BOOLEAN -> "putBoolean" to "(Ljava/lang/Object;ZJ)V"
                Type.BYTE -> "putByte" to "(Ljava/lang/Object;BJ)V"
                Type.CHAR -> "putChar" to "(Ljava/lang/Object;CJ)V"
                Type.DOUBLE -> "putDouble" to "(Ljava/lang/Object;DJ)V"
                Type.FLOAT -> "putFloat" to "(Ljava/lang/Object;FJ)V"
                Type.INT -> "putInt" to "(Ljava/lang/Object;IJ)V"
                Type.LONG -> "putLong" to "(Ljava/lang/Object;JJ)V"
                Type.SHORT -> "putShort" to "(Ljava/lang/Object;SJ)V"
                else -> "putObject" to "(Ljava/lang/Object;Ljava/lang/Object;J)V"
            }
        }
    }
    
}