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
        list.insert(insn, instructions)
    }

fun MethodNode.insertAfterEvery(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtEvery(match) { insn, list ->
        list.insert(insn, instructions.copy())
    }

fun MethodNode.insertBeforeFirst(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtFirst(match) { insn, list ->
        list.insertBefore(insn, instructions)
    }

fun MethodNode.insertBeforeEvery(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtEvery(match) { insn, list ->
        list.insertBefore(insn, instructions.copy())
    }

fun MethodNode.replaceFirst(dropBefore: Int, dropAfter: Int, instructions: InsnList, match: (AbstractInsnNode) -> Boolean) {
    replaceNth(0, dropBefore, dropAfter, instructions, match)
}

fun MethodNode.replaceNth(nth: Int, dropBefore: Int, dropAfter: Int, instructions: InsnList, match: (AbstractInsnNode) -> Boolean) {
    var matchIdx = 0
    var insnIdx = 0
    val iterator = this.instructions.iterator()
    while (iterator.hasNext()) {
        val insn = iterator.next()
        if (match(insn)) {
            if (matchIdx == nth) {
                this.instructions.replaceRange(insnIdx - dropBefore, insnIdx + dropAfter, instructions)
                break
            }
            
            matchIdx++
        }
        
        insnIdx++
    }
}

fun MethodNode.replaceEvery(dropBefore: Int, dropAfter: Int, instructions: InsnList, match: (AbstractInsnNode) -> Boolean) {
    val ranges = ArrayList<IntRange>()
    
    var insnIdx = 0
    val iterator = this.instructions.iterator()
    while (iterator.hasNext()) {
        val insn = iterator.next()
        if (match(insn)) {
            ranges += (insnIdx - dropBefore)..(insnIdx + dropAfter)
        }
        insnIdx++
    }
    
    val idxChange = instructions.size() - (dropBefore + dropAfter + 1)
    ranges.forEachIndexed { offsetIdx, range ->
        val offset = offsetIdx * idxChange
        this.instructions.replaceRange(offset + range.first, offset + range.last, instructions.copy())
    }
}