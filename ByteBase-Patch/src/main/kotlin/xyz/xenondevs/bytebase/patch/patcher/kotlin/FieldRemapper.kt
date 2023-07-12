@file:Suppress("LiftReturnOrAssignment")

package xyz.xenondevs.bytebase.patch.patcher.kotlin

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.asm.OBJECT_TYPE
import xyz.xenondevs.bytebase.asm.access.Access
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.asm.insnListOf
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.MemberReference
import xyz.xenondevs.bytebase.patch.Patcher
import xyz.xenondevs.bytebase.patch.annotation.FieldAccessor
import xyz.xenondevs.bytebase.patch.util.UnsafeAccess
import xyz.xenondevs.bytebase.util.accessWrapper
import xyz.xenondevs.bytebase.util.desc
import xyz.xenondevs.bytebase.util.getLdcTypeInstruction
import xyz.xenondevs.bytebase.util.getLoadInstruction
import xyz.xenondevs.bytebase.util.getReturnInstruction
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.isGet
import xyz.xenondevs.bytebase.util.replace
import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod

private typealias InsnListConstructor = () -> InsnList

internal class FieldRemapper(
    val patcher: Patcher,
    val patch: Patcher.LoadedPatch
) {
    
    val logger get() = patcher.logger
    
    private val patchName = patch.patchWrapper.name;
    private val fieldGetRemaps = HashMap<String, () -> InsnList>()
    private val fieldSetRemaps = HashMap<String, () -> InsnList>()
    private val methodRemaps = HashMap<String, () -> InsnList>()
    
    fun generateMappings() {
        logger.debugLine()
        logger.debug("Generating field mappings")
        patch.patchClass.declaredMemberProperties.forEach { property ->
            // Check if the field is annotated with @FieldAccessor
            val fieldAccessor = property.findAnnotation<FieldAccessor>()
            if (fieldAccessor != null) { // The field shouldn't be added to the patched class since it's only used for accessing a field in the target class
                processFieldAccessor(property, fieldAccessor)
            } else { // The field should be added to the class
//                processNewField(property) TODO
            }
        }
        logger.debugLine()
    }
    
    private fun processFieldAccessor(prop: KProperty<*>, accessor: FieldAccessor) {
        val target = patch.target
        
        val name = accessor.name.ifEmpty(prop::name)
        val desc = accessor.desc.ifEmpty(prop::desc)
        
        // Check if the field and target field have the same descriptor
        check(desc == prop.desc) { "The descriptor of the field accessor \"${accessor.name}\" doesn't match the descriptor of the property \"${prop.name}\"." }
        
        
        logger.debugLine()
        logger.debug("- Processing property \"${prop.name}\" (Accessing \"$name.$desc\")")
        
        // Try to find the field in the target class' inheritance tree and throw an exception if it doesn't exist
        val fieldRef = target.inheritanceTree.resolveFieldRef(name, desc)
            ?: throw NoSuchFieldException("Could not find field $name with descriptor $desc in class ${target.name} or any of its superclasses.")
        val access = fieldRef.resolveField().accessWrapper
        
        val isAccessible = patch.target.canAccess(fieldRef, access, true)
        
        val getter = getCorrectGetter(fieldRef, access, isAccessible)
        
        val fieldKey = "$name.$desc"
        
        fieldGetRemaps[fieldKey] = getter
        prop.javaGetter?.let { m -> methodRemaps[m.name + Type.getMethodDescriptor(m)] = getter }
        if (prop is KMutableProperty) {
            val setter = getCorrectSetter(fieldRef, access, isAccessible)
            fieldSetRemaps[fieldKey] = setter
            prop.setter.javaMethod?.let { m -> methodRemaps[m.name + Type.getMethodDescriptor(m)] = setter }
        }
    }
    
    private fun getCorrectGetter(ref: MemberReference, access: Access, isAccessible: Boolean): InsnListConstructor {
        logger.debug("-- Generating getter mappings")
        
        // Check whether the target class can access the field directly
        if (isAccessible) {
            logger.debug("-- Field is accessible from the target class. Using direct access.")
            // The target class can access the field directly, so we can just use the field directly
            return {
                insnListOf(FieldInsnNode(if (access.isStatic()) GETSTATIC else GETFIELD, ref.owner, ref.name, ref.desc))
            }
        } else {
            // Can't access field, get field offset and use Unsafe
            val rtField = ref.resolveRuntimeField(access)
            val fieldType = Type.getType(rtField.type)
            val (uMethod, uDesc) = getUnsafeSignature(true, fieldType)
            
            logger.debug("-- Field isn't accessible from the target class. Using Unsafe to access the field ($uMethod$uDesc)")
            if (!access.isStatic()) {
                val offset = UnsafeAccess.getFieldOffset(rtField)
                return {
                    buildInsnList {
                        ldc(offset)
                        invokeStatic(UnsafeAccess::class.internalName, uMethod, uDesc)
                    }
                }
            } else {
                val offset = UnsafeAccess.getStaticFieldOffset(rtField)
                return {
                    buildInsnList {
                        pop()
                        add(Type.getType(rtField.declaringClass).getLdcTypeInstruction())
                        ldc(offset)
                        invokeStatic(UnsafeAccess::class.internalName, uMethod, uDesc)
                    }
                }
            }
        }
    }
    
    private fun getCorrectSetter(ref: MemberReference, access: Access, isAccessible: Boolean): InsnListConstructor {
        logger.debug("-- Generating setter mappings")
        // Check whether the target class can set the field directly
        if (isAccessible && !access.isFinal()) {
            logger.debug("-- Field is accessible and not final. Using direct access.")
            return {
                insnListOf(FieldInsnNode(if (access.isStatic()) PUTSTATIC else PUTFIELD, ref.owner, ref.name, ref.desc))
            }
        } else {
            // Can't access field, get field offset and use Unsafe.
            val rtField = ref.resolveRuntimeField(access)
            val fieldType = Type.getType(rtField.type)
            val (uSetName, uSetDesc) = getUnsafeSignature(false, fieldType)
            
            logger.debug("-- Field isn't accessible from the target class. Using Unsafe to access the field ($uSetName$uSetDesc)")
            if (access.isStatic()) {
                val offset = UnsafeAccess.getStaticFieldOffset(rtField)
                return {
                    buildInsnList {
                        swap()
                        pop()
                        add(Type.getType(rtField.declaringClass).getLdcTypeInstruction())
                        swap()
                        ldc(offset)
                        invokeStatic(UnsafeAccess::class.internalName, uSetName, uSetDesc)
                    }
                }
            } else {
                val offset = UnsafeAccess.getFieldOffset(rtField)
                return {
                    buildInsnList {
                        ldc(offset)
                        invokeStatic(UnsafeAccess::class.internalName, uSetName, uSetDesc)
                    }
                }
            }
        }
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
    
    private fun MemberReference.resolveRuntimeField(access: Access): Field {
        val clazz = patcher.runtimeClassGetter(resolveOwner())
        return if (access.isPublic()) clazz.getField(name) else clazz.getDeclaredField(name)
    }
    
    fun remap(method: MethodNode) {
        logger.debug("- Remapping field calls")
        method.instructions
            .asSequence()
            .filter { it is MethodInsnNode || it is FieldInsnNode }
            .forEach { insn ->
                when (insn) {
                    is MethodInsnNode -> {
                        val key = insn.name + insn.desc
                        if (key in methodRemaps)
                            method.instructions.replace(insn, methodRemaps[key]!!.invoke())
                    }
                    
                    is FieldInsnNode -> {
                        if (insn.isGet()) {
                            val key = insn.name + "." + insn.desc
                            if (key in fieldGetRemaps)
                                method.instructions.replace(insn, fieldGetRemaps[key]!!.invoke())
                        } else {
                            val key = insn.name + "." + insn.desc
                            if (key in fieldSetRemaps)
                                method.instructions.replace(insn, fieldSetRemaps[key]!!.invoke())
                        }
                        
                    }
                }
            }
    }
    
}
