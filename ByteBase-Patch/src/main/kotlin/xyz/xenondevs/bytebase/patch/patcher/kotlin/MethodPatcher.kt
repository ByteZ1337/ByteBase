package xyz.xenondevs.bytebase.patch.patcher.kotlin

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.patch.Patcher
import xyz.xenondevs.bytebase.patch.annotation.Inject
import xyz.xenondevs.bytebase.patch.annotation.Replace
import xyz.xenondevs.bytebase.patch.annotation.target.At
import xyz.xenondevs.bytebase.patch.annotation.target.Position
import xyz.xenondevs.bytebase.util.copy
import xyz.xenondevs.bytebase.util.insertBefore
import xyz.xenondevs.bytebase.util.nextLabelOrNull
import xyz.xenondevs.bytebase.util.previousLabel
import xyz.xenondevs.bytebase.util.replaceRange
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaMethod

internal class MethodPatcher(
    val patcher: Patcher,
    val patch: Patcher.LoadedPatch,
    val newClass: ClassWrapper,
    val fieldCallRemapper: (MethodNode) -> Unit
) {
    
    val logger get() = patcher.logger
    
    fun patchMethods() {
        logger.debug("Patching methods")
        patch.patchClass.declaredMemberFunctions.forEach { function ->
            val replaceAnnotation = function.findAnnotation<Replace>()
            if (replaceAnnotation != null) {
                replaceMethod(function, replaceAnnotation)
                return
            }
            val injectAnnotation = function.findAnnotation<Inject>()
            if (injectAnnotation != null) {
                injectMethod(function, injectAnnotation)
                return
            }
        }
    }
    
    private fun replaceMethod(function: KFunction<*>, replaceAnnotation: Replace) {
        logger.debugLine()
        logger.debug("- Replacing method instructions of \"${function.name}\" with instructions of \"${replaceAnnotation.target}\"")
        val target = getAndRemoveTarget(replaceAnnotation.target)
        newClass.methods.removeIf { it.name == target.name && it.desc == target.desc }
        val patchedMethod = VirtualClassPath[function.javaMethod!!]
        fieldCallRemapper(patchedMethod)
    }
    
    private fun injectMethod(function: KFunction<*>, inject: Inject) {
        logger.debugLine()
        logger.debug("- Injecting instructions of \"${function.name}\" into \"${inject.target}\" at specified position")
        val target = getAndRemoveTarget(inject.target)
        val at = inject.at
        val instructions = target.instructions.copy()
        val injectedInstructions = VirtualClassPath[function.javaMethod!!].instructions.copy().apply { removeAll { it is LineNumberNode } }
        
        var returnLabel: AbstractInsnNode? = null
        
        // if requested, save the fallthrough return label for later
        if (!inject.keepFallthroughReturn)
            returnLabel = injectedInstructions.lastOrNull { it.opcode == Opcodes.RETURN && it.previous is LabelNode }?.previousLabel()
        
        if (at.position != Position.REPLACE) {
            val insertionIndex = resolveTargetIndex(inject.target, at, instructions)
            instructions.insertBefore(insertionIndex, injectedInstructions)
        } else {
            val (start, end) = searchForMatch(inject.target, at, instructions)
            instructions.replaceRange(start, end, injectedInstructions)
        }
        
        // Jump over injected fallthrough return
        if (returnLabel != null) {
            val jumpLabel = returnLabel.nextLabelOrNull()
            if (jumpLabel != null) {
                instructions.insert(returnLabel, buildInsnList {
                    goto(jumpLabel)
                    addLabel()
                })
            }
        }
        
        // Add the patched method to the class
        val newMethod = MethodNode(target.access, target.name, target.desc, null, null).apply { this.instructions = instructions }
        newClass.methods.add(newMethod)
        fieldCallRemapper(newMethod)
    }
    
    private fun getAndRemoveTarget(targetMethod: String): MethodNode {
        val target = patch.target.getMethod(targetMethod, includesDesc = targetMethod.contains('('))
            ?: throw IllegalStateException("Target method $targetMethod not found in ${patch.target.name}")
        newClass.methods.removeIf { it.name == target.name && it.desc == target.desc }
        return target
    }
    
    private fun resolveTargetIndex(target: String, at: At, insns: InsnList): Int {
        return when (at.value.uppercase()) {
            "HEAD" -> 0
            "TAIL" -> insns.size()
            else -> searchForMatch(target, at, insns).first + at.position.offset
        } + at.offset
    }
    
    private fun searchForMatch(target: String, at: At, insns: InsnList): Pair<Int, Int> {
        logger.debug("--- Searching for match of \n---\"${target.trim().replace("\n", "\n---")}\"")
        val toMatch = at.value.trimIndent().split('\n').map { it.split(' ') }
        var matchIndex = 0
        val textifier = Textifier()
        val visitor = TraceMethodVisitor(textifier)
        insns.accept(visitor)
        
        fun insnMatches(insn: AbstractInsnNode): Boolean {
            val toMatchArray = toMatch[matchIndex]
            if (toMatchArray[0] == "*") return true
            if (toMatchArray[0] == "L*" && insn is LabelNode) return true
            insn.accept(visitor)
            val insnText = textifier.text.last().toString().trim().split(' ')
            for (i in insnText.indices) {
                if (i >= toMatchArray.size) return false
                if (toMatchArray[i] == "*") continue
                if (toMatchArray[i] != insnText[i]) return false
            }
            return true
        }
        
        var currentInsn: AbstractInsnNode? = insns.first
        var firstInsnIndex = -1
        var lastInsnIndex = -1
        while (currentInsn != null) {
            
            // Skip unnecessary instructions
            if (currentInsn is FrameNode || (at.ignoreLineNumbers && currentInsn is LineNumberNode)) {
                currentInsn = currentInsn.next
                continue
            }
            
            // Check if the current instruction matches
            if (insnMatches(currentInsn)) {
                ++matchIndex
                
                // if this is the first match, save the index
                if (firstInsnIndex == -1)
                    firstInsnIndex = insns.indexOf(currentInsn)
                
                // All instructions matched
                if (matchIndex == toMatch.size) {
                    lastInsnIndex = insns.indexOf(currentInsn)
                    break
                }
                
            } else {
                // Reset the match index
                firstInsnIndex = -1
                matchIndex = 0
            }
            
            currentInsn = currentInsn.next
        }
        
        if (matchIndex != toMatch.size) {
            val instructionText = StringWriter().apply { textifier.print(PrintWriter(this)) }.toString()
            throw IllegalStateException("""
                |Could not find match for $at in ${patch.target.name}#$target
                |Instructions:
                |$instructionText
                |Searched for:
                |${at.value}
            """.trimMargin())
        }
        return firstInsnIndex to lastInsnIndex
    }
    
}