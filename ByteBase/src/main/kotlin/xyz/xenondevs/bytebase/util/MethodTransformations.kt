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

fun InsnList.replaceFirstRange(
    start: (AbstractInsnNode) -> Boolean,
    end: (AbstractInsnNode) -> Boolean,
    dropBefore: Int,
    dropAfter: Int,
    instructions: InsnList
) {
    replaceNthRange(0, start, end, dropBefore, dropAfter, instructions)
}

fun InsnList.replaceNthRange(
    n: Int,
    start: (AbstractInsnNode) -> Boolean,
    end: (AbstractInsnNode) -> Boolean,
    dropBefore: Int,
    dropAfter: Int,
    instructions: InsnList
) {
    var matchIdx = 0
    
    var idx = 0
    var startIdx: Int? = null
    
    val iterator = iterator()
    while (iterator.hasNext()) {
        val insn = iterator.next()
        if (startIdx == null && start(insn)) {
            if (matchIdx == n) {
                startIdx = idx
            } else {
                matchIdx++
            }
        } else if (startIdx != null && end(insn)) {
            replaceRange(startIdx - dropBefore, idx + dropAfter, instructions)
            break
        }
        
        idx++
    }
}

fun InsnList.replaceEveryRange(
    start: (AbstractInsnNode) -> Boolean,
    end: (AbstractInsnNode) -> Boolean,
    dropBefore: Int,
    dropAfter: Int,
    instructions: InsnList
) {
    val ranges = ArrayList<IntRange>()
    
    var idx = 0
    var startIdx: Int? = null
    
    val iterator = iterator()
    while (iterator.hasNext()) {
        val insn = iterator.next()
        if (startIdx == null && start(insn)) {
            startIdx = idx
        } else if (startIdx != null && end(insn)) {
            ranges += (startIdx - dropBefore)..(idx + dropAfter)
            startIdx = null
        }
        
        idx++
    }
    
    var offset = 0
    ranges.forEach { range ->
        replaceRange(offset + range.first, offset + range.last, instructions.copy())
        offset += instructions.size() - (range.last - range.first + 1)
    }
}

fun InsnList.insert(index: Int, instructions: InsnList) =
    insert(get(index), instructions)

fun InsnList.insertBefore(index: Int, instructions: InsnList) =
    insertBefore(get(index), instructions)

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

fun MethodNode.replaceFirstRange(start: (AbstractInsnNode) -> Boolean, end: (AbstractInsnNode) -> Boolean, dropBefore: Int, dropAfter: Int, instructions: InsnList) =
    this.instructions.replaceFirstRange(start, end, dropBefore, dropAfter, instructions)

fun MethodNode.replaceNthRange(n: Int, start: (AbstractInsnNode) -> Boolean, end: (AbstractInsnNode) -> Boolean, dropBefore: Int, dropAfter: Int, instructions: InsnList) =
    this.instructions.replaceNthRange(n, start, end, dropBefore, dropAfter, instructions)

fun MethodNode.replaceEveryRange(start: (AbstractInsnNode) -> Boolean, end: (AbstractInsnNode) -> Boolean, dropBefore: Int, dropAfter: Int, instructions: InsnList) =
    this.instructions.replaceEveryRange(start, end, dropBefore, dropAfter, instructions)