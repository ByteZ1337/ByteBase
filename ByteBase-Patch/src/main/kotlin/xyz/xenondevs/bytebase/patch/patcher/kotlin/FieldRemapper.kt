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
    
    val baseHolder = AtomicReference<ClassWrapper>() // TODO - Still unsure if all static fields in a class(?) have the same base?
    
    private val patchName = patch.patchWrapper.name;
    private val fieldGetRemaps = HashMap<String, () -> InsnList>()
    private val fieldSetRemaps = HashMap<String, () -> InsnList>()
    private val methodRemaps = HashMap<String, () -> InsnList>()
    
    fun generateMappings() {
        patch.patchClass.declaredMemberProperties.forEach { property ->
            // Check if the field is annotated with @FieldAccessor
            val fieldAccessor = property.findAnnotation<FieldAccessor>()
            if (fieldAccessor != null) { // The field shouldn't be added to the patched class since it's only used for accessing a field in the target class
                processFieldAccessor(property, fieldAccessor)
            } else { // The field should be added to the class
//                processNewField(property) TODO
            }
        }
    }
    
    private fun processFieldAccessor(prop: KProperty<*>, accessor: FieldAccessor) {
        val target = patch.target
        
        val name = accessor.name.ifEmpty(prop::name)
        val desc = accessor.desc.ifEmpty(prop::desc)
        
        // Check if the field and target field have the same descriptor
        check(desc == prop.desc) { "The descriptor of the field accessor ${accessor.name} doesn't match the descriptor of the property ${prop.name}." }
        
        // Try to find the field in the target class' inheritance tree and throw an exception if it doesn't exist
        val fieldRef = target.inheritanceTree.resolveFieldRef(name, desc)
            ?: throw NoSuchFieldException("Could not find field $name with descriptor $desc in class ${target.name} or any of its superclasses.")
        val access = fieldRef.resolveField().accessWrapper
        
        val isAccessible = patch.target.canAccess(fieldRef, access, true)
        
        val (getter, cSetter) = getCorrectGetter(prop, fieldRef, access, isAccessible)
        
        val fieldKey = "$name.$desc"
        
        fieldGetRemaps[fieldKey] = getter
        prop.javaGetter?.let { m -> methodRemaps[m.name + Type.getMethodDescriptor(m)] = getter }
        if (cSetter != null) {
            fieldSetRemaps[fieldKey] = cSetter
            (prop as KMutableProperty).setter.javaMethod?.let { m -> methodRemaps[m.name + Type.getMethodDescriptor(m)] = cSetter }
        } else if (prop is KMutableProperty) {
            val setter = getCorrectSetter(prop, fieldRef, access, isAccessible)
            fieldSetRemaps[fieldKey] = setter
            prop.setter.javaMethod?.let { m -> methodRemaps[m.name + Type.getMethodDescriptor(m)] = setter }
        }
    }
    
    private fun getCorrectGetter(property: KProperty<*>, ref: MemberReference, access: Access, isAccessible: Boolean): Pair<InsnListConstructor, InsnListConstructor?> {
        // Check whether the target class can access the field directly
        if (isAccessible) {
            // The target class can access the field directly, so we can just use the field directly
            return {
                insnListOf(FieldInsnNode(if (access.isStatic()) GETSTATIC else GETFIELD, ref.owner, ref.name, ref.desc))
            } to null
        } else {
            // Can't access field, get field offset and use Unsafe
            val rtField = ref.resolveRuntimeField(access)
            // If the field is static, we'll need the separate baseholder class to store the base object for the field somewhere
            if (!access.isStatic()) {
                val fieldType = Type.getType(rtField.type)
                val (uMethod, uDesc) = getUnsafeSignature(true, fieldType)
                val offset = UnsafeAccess.getFieldOffset(rtField)
                return {
                    buildInsnList {
                        ldc(offset)
                        invokeStatic(UnsafeAccess::class.internalName, uMethod, uDesc)
                    }
                } to null
            } else {
                return generateBaseHolder(property, ref, rtField, access)
            }
        }
    }
    
    private fun getCorrectSetter(property: KMutableProperty<*>, ref: MemberReference, access: Access, isAccessible: Boolean): InsnListConstructor {
        // Check whether the target class can set the field directly
        if (isAccessible && !access.isFinal()) {
            return {
                buildInsnList { FieldInsnNode(if (access.isStatic()) PUTSTATIC else PUTFIELD, ref.owner, ref.name, ref.desc) }
            }
        } else {
            // Can't access field, get field offset and use Unsafe.
            val rtField = ref.resolveRuntimeField(access)
            
            if (access.isStatic()) { // base holder can't exist yet since a setter would've been generated by getCorrectGetter
                return generateBaseHolder(property, ref, rtField, access).second!!
            } else {
                val fieldType = Type.getType(rtField.type)
                val offset = UnsafeAccess.getFieldOffset(rtField)
                val (uSetName, uSetDesc) = getUnsafeSignature(false, fieldType)
                return {
                    buildInsnList {
                        ldc(offset)
                        invokeStatic(UnsafeAccess::class.internalName, uSetName, uSetDesc)
                    }
                }
            }
        }
    }
    
    private fun generateBaseHolder(
        property: KProperty<*>,
        ref: MemberReference,
        rtField: Field,
        access: Access
    ): Pair<InsnListConstructor, InsnListConstructor?> {
        val fieldType = Type.getType(rtField.type)
        val (uMethod, uDesc) = getUnsafeSignature(true, fieldType)
        val baseHolder = getOrCreateBaseHolder()
        val field = FieldNode(ACC_PRIVATE or ACC_STATIC, "base_" + ref.name, "L$OBJECT_TYPE;", null, null)
        baseHolder.fields.add(field)
        val getterMethod = MethodNode(ACC_PUBLIC or ACC_STATIC, "getStatic_" + ref.name, "()${ref.desc}", null, null)
        val offset = UnsafeAccess.getStaticFieldOffset(rtField)
        
        var setterInstructions: InsnListConstructor? = null
        
        val clinit = baseHolder.getOrCreateClassInit()
        clinit.instructions.insert(buildInsnList {
            addLabel()
            ldc(Type.getObjectType(ref.owner))
            ldc(ref.name)
            add(Type.getType(ref.desc).getLdcTypeInstruction())
            invokeStatic(if (access.isPublic()) UnsafeAccess::getField else UnsafeAccess::getDeclaredField)
            invokeStatic(UnsafeAccess::getStaticBase)
            putStatic(baseHolder.name, field.name, field.desc)
        })
        
        getterMethod.instructions = buildInsnList {
            addLabel()
            getStatic(baseHolder.name, field.name, field.desc)
            ldc(offset)
            invokeStatic(UnsafeAccess::class.internalName, uMethod, uDesc)
            
            addLabel()
            add(fieldType.getReturnInstruction())
        }
        baseHolder.methods.add(getterMethod)
        
        if (property is KMutableProperty<*>) {
            val setterMethod = MethodNode(ACC_PUBLIC or ACC_STATIC, "setStatic_" + ref.name, "(${ref.desc})V", null, null)
            val (uSetName, uSetDesc) = getUnsafeSignature(false, fieldType)
            setterMethod.instructions = buildInsnList {
                addLabel()
                getStatic(baseHolder.name, field.name, field.desc)
                add(fieldType.getLoadInstruction(0))
                ldc(offset)
                invokeStatic(UnsafeAccess::class.internalName, uSetName, uSetDesc)
                _return()
            }
            baseHolder.methods.add(setterMethod)
            setterInstructions = {
                buildInsnList {
                    swap()
                    pop()
                    invokeStatic(baseHolder.name, setterMethod.name, setterMethod.desc)
                }
            }
        }
        
        return {
            buildInsnList {
                pop()
                invokeStatic(baseHolder.name, getterMethod.name, getterMethod.desc)
            }
        } to setterInstructions
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
    
    private fun getOrCreateBaseHolder(): ClassWrapper {
        this.baseHolder.get()?.let { return it }
        
        val baseHolder = ClassWrapper(patch.target.className + "$" + patchName + Random.nextInt(1337))
        this.baseHolder.set(baseHolder)
        return baseHolder
    }
    
    private fun MemberReference.resolveRuntimeField(access: Access): Field {
        val clazz = patcher.runtimeClassGetter(resolveOwner())
        return if (access.isPublic()) clazz.getField(name) else clazz.getDeclaredField(name)
    }
    
    fun remap(method: MethodNode) {
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
