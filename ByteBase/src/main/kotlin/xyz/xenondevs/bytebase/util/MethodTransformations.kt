package xyz.xenondevs.bytebase.util

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode

private fun InsnList.insertAtFirst(match: (AbstractInsnNode) -> Boolean, insertion: (AbstractInsnNode, InsnList) -> Unit) {
    val iterator = iterator()
    while (iterator.hasNext()) {
        val insn = iterator.next()
        if (match(insn)) {
            insertion(insn, this)
            break
        }
    }
}

private fun InsnList.insertAtEvery(match: (AbstractInsnNode) -> Boolean, insertion: (AbstractInsnNode, InsnList) -> Unit) {
    val iterator = iterator()
    while (iterator.hasNext()) {
        val insn = iterator.next()
        if (match(insn)) {
            insertion(insn, this)
        }
    }
}

fun InsnList.insertAfterFirst(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtFirst(match) { insn, list ->
        list.insert(insn, instructions)
    }

fun InsnList.insertAfterEvery(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtEvery(match) { insn, list ->
        list.insert(insn, instructions.copy())
    }

fun InsnList.insertBeforeFirst(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtFirst(match) { insn, list ->
        list.insertBefore(insn, instructions)
    }

fun InsnList.insertBeforeEvery(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtEvery(match) { insn, list ->
        list.insertBefore(insn, instructions.copy())
    }

fun InsnList.replaceFirst(dropBefore: Int, dropAfter: Int, instructions: InsnList, match: (AbstractInsnNode) -> Boolean) {
    replaceNth(0, dropBefore, dropAfter, instructions, match)
}

fun InsnList.replaceNth(nth: Int, dropBefore: Int, dropAfter: Int, instructions: InsnList, match: (AbstractInsnNode) -> Boolean) {
    var matchIdx = 0
    var insnIdx = 0
    val iterator = iterator()
    while (iterator.hasNext()) {
        val insn = iterator.next()
        if (match(insn)) {
            if (matchIdx == nth) {
                replaceRange(insnIdx - dropBefore, insnIdx + dropAfter, instructions)
                break
            }
            
            matchIdx++
        }
        
        insnIdx++
    }
}

fun InsnList.replaceEvery(dropBefore: Int, dropAfter: Int, instructions: InsnList, match: (AbstractInsnNode) -> Boolean) {
    val ranges = ArrayList<IntRange>()
    
    var insnIdx = 0
    val iterator = iterator()
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
        replaceRange(offset + range.first, offset + range.last, instructions.copy())
    }
}

fun MethodNode.insertAfterFirst(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.insertAfterFirst(instructions, match)

fun MethodNode.insertAfterEvery(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.insertAfterEvery(instructions, match)

fun MethodNode.insertBeforeFirst(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.insertBeforeFirst(instructions, match)

fun MethodNode.insertBeforeEvery(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.insertBeforeEvery(instructions, match)

fun MethodNode.replaceFirst(dropBefore: Int, dropAfter: Int, instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.replaceFirst(dropBefore, dropAfter, instructions, match)

fun MethodNode.replaceNth(nth: Int, dropBefore: Int, dropAfter: Int, instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.replaceNth(nth, dropBefore, dropAfter, instructions, match)

fun MethodNode.replaceEvery(dropBefore: Int, dropAfter: Int, instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.replaceEvery(dropBefore, dropAfter, instructions, match)