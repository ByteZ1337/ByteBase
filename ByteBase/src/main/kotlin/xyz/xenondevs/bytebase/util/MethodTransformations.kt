package xyz.xenondevs.bytebase.util

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode

private fun MethodNode.insertAtFirst(match: (AbstractInsnNode) -> Boolean, insertion: (AbstractInsnNode, InsnList) -> Unit) {
    val iterator = instructions.iterator()
    while (iterator.hasNext()) {
        val insn = iterator.next()
        if (match(insn)) {
            insertion(insn, instructions)
            break
        }
    }
}

private fun MethodNode.insertAtEvery(match: (AbstractInsnNode) -> Boolean, insertion: (AbstractInsnNode, InsnList) -> Unit) {
    val iterator = instructions.iterator()
    while (iterator.hasNext()) {
        val insn = iterator.next()
        if (match(insn)) {
            insertion(insn, instructions)
        }
    }
}

fun MethodNode.insertAfterFirst(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtFirst(match) { insn, list ->
        list.insert(insn, instructions.copy())
    }

fun MethodNode.insertAfterEvery(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtEvery(match) { insn, list ->
        list.insert(insn, instructions.copy())
    }

fun MethodNode.insertBeforeFirst(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtFirst(match) { insn, list ->
        list.insertBefore(insn, instructions.copy())
    }

fun MethodNode.insertBeforeEvery(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtEvery(match) { insn, list ->
        list.insertBefore(insn, instructions.copy())
    }