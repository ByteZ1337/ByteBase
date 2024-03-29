package xyz.xenondevs.bytebase.util

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.asm.InsnBuilder
import xyz.xenondevs.bytebase.asm.buildInsnList

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

private fun InsnList.insertAtLast(match: (AbstractInsnNode) -> Boolean, insertion: (AbstractInsnNode, InsnList) -> Unit) {
    var lastMatch: AbstractInsnNode? = null
    val iterator = iterator()
    while (iterator.hasNext()) {
        val insn = iterator.next()
        if (match(insn)) {
            lastMatch = insn
        }
    }
    
    if (lastMatch != null) {
        insertion(lastMatch, this)
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

fun InsnList.insertAfterLast(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtLast(match) { insn, list ->
        list.insert(insn, instructions)
    }

fun InsnList.insertBeforeFirst(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtFirst(match) { insn, list ->
        list.insertBefore(insn, instructions)
    }

fun InsnList.insertBeforeEvery(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtEvery(match) { insn, list ->
        list.insertBefore(insn, instructions.copy())
    }

fun InsnList.insertBeforeLast(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    insertAtLast(match) { insn, list ->
        list.insertBefore(insn, instructions)
    }

fun InsnList.replaceFirst(dropBefore: Int, dropAfter: Int, instructions: InsnList, match: (AbstractInsnNode) -> Boolean) {
    replaceNth(0, dropBefore, dropAfter, instructions, match)
}

fun InsnList.replaceNth(nth: Int, dropBefore: Int, dropAfter: Int, instructions: InsnList, match: (AbstractInsnNode) -> Boolean) {
    replaceNthAfter(nth, dropBefore, dropAfter, instructions, match = match)
}

fun InsnList.replaceFirstAfter(dropBefore: Int, dropAfter: Int, instructions: InsnList, vararg preMatch: (AbstractInsnNode) -> Boolean, match: (AbstractInsnNode) -> Boolean) {
    replaceNthAfter(0, dropBefore, dropAfter, instructions, preMatch = preMatch, match = match)
}

fun InsnList.replaceNthAfter(nth: Int, dropBefore: Int, dropAfter: Int, instructions: InsnList, vararg preMatch: (AbstractInsnNode) -> Boolean, match: (AbstractInsnNode) -> Boolean) {
    var preMatchIdx = 0
    
    var matchIdx = 0
    var insnIdx = 0
    val iterator = iterator()
    while (iterator.hasNext()) {
        val insn = iterator.next()
        
        if (preMatchIdx < preMatch.size) {
            val preMatcher = preMatch[preMatchIdx]
            if (preMatcher(insn)) {
                preMatchIdx++
            }
        } else if (match(insn)) {
            if (matchIdx == nth) {
                replaceRange(insnIdx - dropBefore, insnIdx + dropAfter, instructions)
                break
            }
            
            matchIdx++
            preMatchIdx = 0
        }
        
        insnIdx++
    }
}

fun InsnList.replaceEvery(dropBefore: Int, dropAfter: Int, builder: InsnBuilder.() -> Unit, match: (AbstractInsnNode) -> Boolean) {
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
    
    var offset = 0
    ranges.forEach { range ->
        val insns = buildInsnList(builder)
        val size = insns.size()
        replaceRange(offset + range.first, offset + range.last, insns)
        offset += size - (dropBefore + dropAfter + 1)
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
    builder: InsnBuilder.() -> Unit
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
        val insns = buildInsnList(builder)
        val size = insns.size()
        replaceRange(offset + range.first, offset + range.last, insns)
        offset += size - (range.last - range.first + 1)
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

fun MethodNode.insertAfterLast(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.insertAfterLast(instructions, match)

fun MethodNode.insertBeforeFirst(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.insertBeforeFirst(instructions, match)

fun MethodNode.insertBeforeLast(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.insertBeforeLast(instructions, match)

fun MethodNode.insertBeforeEvery(instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.insertBeforeEvery(instructions, match)

fun MethodNode.replaceFirst(dropBefore: Int, dropAfter: Int, instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.replaceFirst(dropBefore, dropAfter, instructions, match)

fun MethodNode.replaceNth(nth: Int, dropBefore: Int, dropAfter: Int, instructions: InsnList, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.replaceNth(nth, dropBefore, dropAfter, instructions, match)

fun MethodNode.replaceFirstAfter(dropBefore: Int, dropAfter: Int, instructions: InsnList, vararg preMatch: (AbstractInsnNode) -> Boolean, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.replaceFirstAfter(dropBefore, dropAfter, instructions, preMatch = preMatch, match = match)

fun MethodNode.replaceNthAfter(nth: Int, dropBefore: Int, dropAfter: Int, instructions: InsnList, vararg preMatch: (AbstractInsnNode) -> Boolean, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.replaceNthAfter(nth, dropBefore, dropAfter, instructions, preMatch = preMatch, match = match)

fun MethodNode.replaceEvery(dropBefore: Int, dropAfter: Int, builder: InsnBuilder.() -> Unit, match: (AbstractInsnNode) -> Boolean) =
    this.instructions.replaceEvery(dropBefore, dropAfter, builder, match)

fun MethodNode.replaceFirstRange(start: (AbstractInsnNode) -> Boolean, end: (AbstractInsnNode) -> Boolean, dropBefore: Int, dropAfter: Int, instructions: InsnList) =
    this.instructions.replaceFirstRange(start, end, dropBefore, dropAfter, instructions)

fun MethodNode.replaceNthRange(n: Int, start: (AbstractInsnNode) -> Boolean, end: (AbstractInsnNode) -> Boolean, dropBefore: Int, dropAfter: Int, instructions: InsnList) =
    this.instructions.replaceNthRange(n, start, end, dropBefore, dropAfter, instructions)

fun MethodNode.replaceEveryRange(start: (AbstractInsnNode) -> Boolean, end: (AbstractInsnNode) -> Boolean, dropBefore: Int, dropAfter: Int, builder: InsnBuilder.() -> Unit) =
    this.instructions.replaceEveryRange(start, end, dropBefore, dropAfter, builder)