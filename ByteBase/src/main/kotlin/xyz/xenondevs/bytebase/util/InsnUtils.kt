package xyz.xenondevs.bytebase.util

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.jvm.MemberReference
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod

/**
 * Converts an integer to the corresponding [LdcInsnNode] instruction or uses ``iconst``/``bipush``/``sipush`` if possible
 */
fun Int.toLdcInsn(): AbstractInsnNode {
    return when (this) {
        in -1..5 -> InsnNode(this + 3)
        in Byte.MIN_VALUE..Byte.MAX_VALUE -> IntInsnNode(Opcodes.BIPUSH, this)
        in Short.MIN_VALUE..Short.MAX_VALUE -> IntInsnNode(Opcodes.SIPUSH, this)
        else -> LdcInsnNode(this)
    }
}

/**
 * Gets the integer value of an instruction. Supported: ``iconst``, ``bipush``, ``sipush``, ``ldc``
 * @throws  IllegalStateException if the instruction is not one of the above
 */
val AbstractInsnNode.intValue: Int
    get() {
        return when {
            this.opcode in Opcodes.ICONST_M1..Opcodes.ICONST_5 -> this.opcode - 3
            this is IntInsnNode && (this.opcode == Opcodes.BIPUSH || this.opcode == Opcodes.SIPUSH) -> this.operand
            this is LdcInsnNode && this.cst is Int -> this.cst as Int
            else -> error("The given instruction is not an integer")
        }
    }

/**
 * Converts a long to the corresponding [LdcInsnNode] instruction or uses ``lconst`` if possible
 */
fun Long.toLdcInsn(): AbstractInsnNode {
    return when (this) {
        in 0..1 -> InsnNode((this + 9).toInt())
        else -> LdcInsnNode(this)
    }
}

/**
 * Gets the long value of an instruction. Supported: ``lconst``, ``ldc``
 * @throws  IllegalStateException if the instruction is not one of the above
 */
val AbstractInsnNode.longValue: Long
    get() {
        return when {
            this.opcode in Opcodes.LCONST_0..Opcodes.LCONST_1 -> (this.opcode - 9).toLong()
            this is LdcInsnNode && this.cst is Long -> this.cst as Long
            else -> error("The given instruction is not a long")
        }
    }

/**
 * Converts a float to the corresponding [LdcInsnNode] instruction or uses ``fconst`` if possible
 */
fun Float.toLdcInsn(): AbstractInsnNode {
    return when {
        this % 1 == 0f && this in 0f..2f -> InsnNode((this + 11).toInt())
        else -> LdcInsnNode(this)
    }
}

/**
 * Gets the float value of an instruction. Supported: ``fconst``, ``ldc``
 */
val AbstractInsnNode.floatValue: Float
    get() {
        return when {
            this.opcode in Opcodes.FCONST_0..Opcodes.FCONST_2 -> (this.opcode - 11).toFloat()
            this is LdcInsnNode && this.cst is Float -> this.cst as Float
            else -> error("The given instruction is not a float")
        }
    }

/**
 * Converts a double to the corresponding [LdcInsnNode] or uses ``dconst`` if possible
 */
fun Double.toLdcInsn(): AbstractInsnNode {
    return when {
        this % 1 == 0.0 && this in 0.0..1.0 -> InsnNode((this + 14).toInt())
        else -> LdcInsnNode(this)
    }
}

/**
 * Gets the double value of an instruction. Supported: ``dconst``, ``ldc``
 * @throws  IllegalStateException if the instruction is not one of the above
 */
val AbstractInsnNode.doubleValue: Double
    get() {
        return when {
            this.opcode in Opcodes.DCONST_0..Opcodes.DCONST_1 -> (this.opcode - 14).toDouble()
            this is LdcInsnNode && this.cst is Double -> this.cst as Double
            else -> error("The given instruction is not a double")
        }
    }

fun AbstractInsnNode.next(amount: Int): AbstractInsnNode? = skip(amount) { it.next }

fun AbstractInsnNode.previous(amount: Int): AbstractInsnNode? = skip(amount) { it.previous }

private fun AbstractInsnNode.skip(amount: Int, next: (AbstractInsnNode) -> AbstractInsnNode?): AbstractInsnNode? {
    var i = 0
    var current: AbstractInsnNode? = this
    while (i < amount) {
        current = next(current!!)
        if (current == null)
            return null
        ++i
    }
    return current
}

fun MethodInsnNode.calls(owner: String, name: String, desc: String) =
    this.owner == owner && this.name == name && this.desc == desc

fun MethodInsnNode.calls(method: Method) =
    calls(method.declaringClass.internalName, method.name, Type.getMethodDescriptor(method))

fun MethodInsnNode.calls(constructor: Constructor<*>) =
    calls(constructor.declaringClass.internalName, "<init>", Type.getConstructorDescriptor(constructor))

fun MethodInsnNode.calls(kFunction: KFunction<*>): Boolean {
    val method = kFunction.javaMethod
    if (method != null)
        return calls(method)
    
    val constructor = kFunction.javaConstructor
    if (constructor != null)
        return calls(constructor)
    
    return false
}

val MethodInsnNode.reference: MemberReference
    get() = MemberReference(owner, name, desc)

fun FieldInsnNode.accesses(owner: String, name: String, desc: String) =
    this.owner == owner && this.name == name && this.desc == desc

fun FieldInsnNode.gets(owner: String, name: String, desc: String) =
    (this.opcode == Opcodes.GETFIELD || this.opcode == Opcodes.GETSTATIC)
        && this.owner == owner && this.name == name && this.desc == desc

fun FieldInsnNode.puts(owner: String, name: String, desc: String) =
    (this.opcode == Opcodes.PUTFIELD || this.opcode == Opcodes.PUTSTATIC)
        && this.owner == owner && this.name == name && this.desc == desc

fun FieldInsnNode.accesses(field: Field) =
    accesses(field.declaringClass.internalName, field.name, Type.getDescriptor(field.type))

fun FieldInsnNode.accesses(kProperty: KProperty<*>) =
    kProperty.javaField?.let { accesses(it) } ?: false

fun FieldInsnNode.gets(field: Field) =
    gets(field.declaringClass.internalName, field.name, Type.getDescriptor(field.type))

fun FieldInsnNode.gets(kProperty: KProperty<*>) =
    kProperty.javaField?.let { gets(it) } ?: false

fun FieldInsnNode.puts(field: Field) =
    puts(field.declaringClass.internalName, field.name, Type.getDescriptor(field.type))

fun FieldInsnNode.puts(kProperty: KProperty<*>) =
    kProperty.javaField?.let { puts(it) } ?: false

val FieldInsnNode.reference: MemberReference
    get() = MemberReference(owner, name, desc)

/**
 * Removes the given [instructions][insn] from the list
 */
fun InsnList.remove(vararg insn: AbstractInsnNode): Unit = insn.forEach(this::remove)

/**
 * Replaces the given [instruction][insn] with the given [replacement]
 */
fun InsnList.replace(insn: AbstractInsnNode, replacement: AbstractInsnNode) {
    insertBefore(insn, replacement)
    remove(insn)
}

/**
 * Replaces the given [instruction][insn] with the given [replacement]
 */
fun InsnList.replace(insn: AbstractInsnNode, replacement: InsnList) {
    insertBefore(insn, replacement)
    remove(insn)
}

fun InsnList.isEmpty() = first == null

fun InsnList.isNotEmpty() = first != null

fun InsnList.replaceRange(from: Int, to: Int, insnList: InsnList) {
    repeat(to - from + 1) { remove(get(from)) }
    insertBefore(get(from), insnList)
}

fun InsnList.replaceRange(from: Int, to: Int, insn: AbstractInsnNode) {
    repeat(to - from + 1) { remove(get(from)) }
    insertBefore(get(from), insn)
}