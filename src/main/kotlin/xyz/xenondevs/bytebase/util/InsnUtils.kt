package xyz.xenondevs.bytebase.util

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode

fun Int32.toLdcInsn(): AbstractInsnNode {
    return when (this) {
        in -1..5 -> InsnNode(this + 3)
        in Byte.MIN_VALUE..Byte.MAX_VALUE -> IntInsnNode(Opcodes.BIPUSH, this)
        in Short.MIN_VALUE..Short.MAX_VALUE -> IntInsnNode(Opcodes.SIPUSH, this)
        else -> LdcInsnNode(this)
    }
}

val AbstractInsnNode.intValue: Int32
    get() {
        return when {
            this.opcode in Opcodes.ICONST_M1..Opcodes.ICONST_5 -> this.opcode - 3
            this is IntInsnNode && (this.opcode == Opcodes.BIPUSH || this.opcode == Opcodes.SIPUSH) -> this.operand
            this is LdcInsnNode && this.cst is Int32 -> this.cst as Int32
            else -> error("The given instruction is not an integer")
        }
    }

fun Int64.toLdcInsn(): AbstractInsnNode {
    return when (this) {
        in 0..1 -> InsnNode((this + 9).toInt())
        else -> LdcInsnNode(this)
    }
}

val AbstractInsnNode.longValue: Int64
    get() {
        return when {
            this.opcode in Opcodes.LCONST_0..Opcodes.LCONST_1 -> (this.opcode - 9).toLong()
            this is LdcInsnNode && this.cst is Int64 -> this.cst as Int64
            else -> error("The given instruction is not a long")
        }
    }

fun Float.toLdcInsn(): AbstractInsnNode {
    return when {
        this % 1 == 0f && this in 0f..2f -> InsnNode((this + 11).toInt())
        else -> LdcInsnNode(this)
    }
}

val AbstractInsnNode.floatValue: Float
    get() {
        return when {
            this.opcode in Opcodes.FCONST_0..Opcodes.FCONST_2 -> (this.opcode - 11).toFloat()
            this is LdcInsnNode && this.cst is Float -> this.cst as Float
            else -> error("The given instruction is not a float")
        }
    }

fun Double.toLdcInsn(): AbstractInsnNode {
    return when {
        this % 1 == 0.0 && this in 0.0..1.0 -> InsnNode((this + 14).toInt())
        else -> LdcInsnNode(this)
    }
}

val AbstractInsnNode.doubleValue: Double
    get() {
        return when {
            this.opcode in Opcodes.DCONST_0..Opcodes.DCONST_1 -> (this.opcode - 14).toDouble()
            this is LdcInsnNode && this.cst is Double -> this.cst as Double
            else -> error("The given instruction is not a double")
        }
    }