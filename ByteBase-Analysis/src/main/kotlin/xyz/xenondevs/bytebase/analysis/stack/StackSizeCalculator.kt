package xyz.xenondevs.bytebase.analysis.stack

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.util.Stack

object StackSizeCalculator {
    
    //<editor-fold desc="Hardcoded instruction sizes" defaultstate="collapsed">
    /**
     * Map of opcodes and the change in stack size they cause.
     */
    val PREDEFINED_SIZES = mapOf(
        ACONST_NULL to 1, // → null
        ICONST_M1 to 1, // → -1
        ICONST_0 to 1, // → 0
        ICONST_1 to 1, // → 1
        ICONST_2 to 1, // → 2
        ICONST_3 to 1, // → 3
        ICONST_4 to 1, // → 4
        ICONST_5 to 1, // → 5
        FCONST_0 to 1, // → 0.0f
        FCONST_1 to 1, // → 1.0f
        FCONST_2 to 1, // → 2.0f
        BIPUSH to 1, // → value
        SIPUSH to 1, // → value
        ILOAD to 1, // → value
        FLOAD to 1, // → value
        ALOAD to 1, // → objectref
        DUP to 1, // value → value, value
        DUP_X1 to 1, // value2, value1 → value1, value2, value1
        DUP_X2 to 1, // value3, value2, value1 → value1, value3, value2, value1
        I2L to 1, // value → {value, value_2nd}
        I2D to 1, // value → {value, value_2nd}
        F2L to 1, // value → {value, value_2nd}
        F2D to 1, // value → {value, value_2nd}
        NEW to 1, // → objectref
        
        LCONST_0 to 2, // → 0, 0
        LCONST_1 to 2, // → 1, 0
        DCONST_0 to 2, // → 0, 0
        DCONST_1 to 2, // → 1, 0
        LLOAD to 2, // → {value, value_2nd}
        DLOAD to 2, // → {value, value_2nd}
        DUP2 to 2, // {value2, value1} → {value2, value1}, {value2, value1}
        DUP2_X1 to 2, // value3, {value2, value1} → {value2, value1}, value3, {value2, value1}
        DUP2_X2 to 2, // {value4, value3}, {value2, value1} → {value2, value1}, {value4, value3}, {value2, value1}
        
        IALOAD to -1, // arrayref, index → value
        FALOAD to -1, // arrayref, index → value
        AALOAD to -1, // arrayref, index → objectref
        BALOAD to -1, // arrayref, index → value
        CALOAD to -1, // arrayref, index → value
        SALOAD to -1, // arrayref, index → value
        ISTORE to -1, // value →
        FSTORE to -1, // value →
        ASTORE to -1, // objectref →
        POP to -1, // value →
        IADD to -1, // value1, value2 → result
        FADD to -1, // value1, value2 → result
        ISUB to -1, // value1, value2 → result
        FSUB to -1, // value1, value2 → result
        IMUL to -1, // value1, value2 → result
        FMUL to -1, // value1, value2 → result
        IDIV to -1, // value1, value2 → result
        FDIV to -1, // value1, value2 → result
        IREM to -1, // value1, value2 → result
        FREM to -1, // value1, value2 → result
        ISHL to -1, // value1, value2 → result
        ISHR to -1, // value1, value2 → result
        IUSHR to -1, // value1, value2 → result
        LSHL to -1, // {value1, value1_2nd}, value2 → {result, result_2nd}
        LSHR to -1, // {value1, value1_2nd}, value2 → {result, result_2nd}
        LUSHR to -1, // {value1, value1_2nd}, value2 → {result, result_2nd}
        IAND to -1, // value1, value2 → result
        IOR to -1, // value1, value2 → result
        IXOR to -1, // value1, value2 → result
        L2I to -1, // {value, value_2nd} → result
        L2F to -1, // {value, value_2nd} → result
        D2I to -1, // {value, value_2nd} → result
        D2F to -1, // {value, value_2nd} → result
        FCMPL to -1, // value1, value2 → result
        FCMPG to -1, // value1, value2 → result
        IFEQ to -1, // value →
        IFNE to -1, // value →
        IFLT to -1, // value →
        IFGE to -1, // value →
        IFGT to -1, // value →
        IFLE to -1, // value →
        IFNULL to -1, // value →
        IFNONNULL to -1, // value →
        TABLESWITCH to -1, // index →
        LOOKUPSWITCH to -1, // key →
        IRETURN to -1, // value →
        FRETURN to -1, // value →
        ARETURN to -1, // objectref →
        ATHROW to -1, // objectref →
        MONITORENTER to -1, // objectref →
        MONITOREXIT to -1, // objectref →
        
        LSTORE to -2, // {value, value_2nd} →
        DSTORE to -2, // {value, value_2nd} →
        POP2 to -2, // {value2, value1} →
        LADD to -2, // {value1, value1_2nd}, {value2, value2_2nd} → {result, result_2nd}
        DADD to -2, // {value1, value1_2nd}, {value2, value2_2nd} → {result, result_2nd}
        LSUB to -2, // {value1, value1_2nd}, {value2, value2_2nd} → {result, result_2nd}
        DSUB to -2, // {value1, value1_2nd}, {value2, value2_2nd} → {result, result_2nd}
        LMUL to -2, // {value1, value1_2nd}, {value2, value2_2nd} → {result, result_2nd}
        DMUL to -2, // {value1, value1_2nd}, {value2, value2_2nd} → {result, result_2nd}
        LDIV to -2, // {value1, value1_2nd}, {value2, value2_2nd} → {result, result_2nd}
        DDIV to -2, // {value1, value1_2nd}, {value2, value2_2nd} → {result, result_2nd}
        LREM to -2, // {value1, value1_2nd}, {value2, value2_2nd} → {result, result_2nd}
        DREM to -2, // {value1, value1_2nd}, {value2, value2_2nd} → {result, result_2nd}
        LAND to -2, // {value1, value1_2nd}, {value2, value2_2nd} → {result, result_2nd}
        LOR to -2, // {value1, value1_2nd}, {value2, value2_2nd} → {result, result_2nd}
        LXOR to -2, // {value1, value1_2nd}, {value2, value2_2nd} → {result, result_2nd}
        IF_ICMPEQ to -2, // value1, value2 →
        IF_ICMPNE to -2, // value1, value2 →
        IF_ICMPLT to -2, // value1, value2 →
        IF_ICMPGE to -2, // value1, value2 →
        IF_ICMPGT to -2, // value1, value2 →
        IF_ICMPLE to -2, // value1, value2 →
        IF_ACMPEQ to -2, // value1, value2 →
        IF_ACMPNE to -2, // value1, value2 →
        LRETURN to -2, // {value, value_2nd} →
        DRETURN to -2, // {value, value_2nd} →
        
        IASTORE to -3, // arrayref, index, value →
        FASTORE to -3, // arrayref, index, value →
        AASTORE to -3, // arrayref, index, value →
        BASTORE to -3, // arrayref, index, value →
        CASTORE to -3, // arrayref, index, value →
        SASTORE to -3, // arrayref, index, value →
        LCMP to -3, // {value1, value1_2nd}, {value2, value2_2nd} → result
        DCMPL to -3, // {value1, value1_2nd}, {value2, value2_2nd} → result
        DCMPG to -3, // {value1, value1_2nd}, {value2, value2_2nd} → result
        
        LASTORE to -4, // arrayref, index, {value, value_2nd} →
        DASTORE to -4, // arrayref, index, {value, value_2nd} →
    )
    //</editor-fold>
    
    /**
     * Returns the change on the stack size the instruction causes.
     *
     * @throws UnsupportedOperationException for JSR and RET instructions
     */
    fun getStackSize(insn: AbstractInsnNode): Int {
        when (insn.opcode) {
            in PREDEFINED_SIZES ->
                return PREDEFINED_SIZES[insn.opcode]!!
            
            LDC -> {
                insn as LdcInsnNode
                return if (insn.cst is Long || insn.cst is Double) 2 else 1
            }
            
            GETFIELD, GETSTATIC, PUTFIELD, PUTSTATIC -> {
                insn as FieldInsnNode
                val sizeChange = if (Type.getType(insn.desc).sort.let { it == Type.LONG || it == Type.DOUBLE }) 2 else 1
                val count = when (insn.opcode) {
                    GETFIELD -> sizeChange - 1 // objectref
                    GETSTATIC -> sizeChange
                    PUTFIELD -> -sizeChange - 1 // objectref
                    PUTSTATIC -> -sizeChange
                    else -> throw IllegalStateException()
                }
                return count
            }
            
            MULTIANEWARRAY ->
                return -(insn as MultiANewArrayInsnNode).dims + 1 // for arrayref
            
            INVOKEVIRTUAL, INVOKESPECIAL, INVOKEINTERFACE ->
                return getMethodStackSize((insn as MethodInsnNode).desc) - 1 // for objectref
            
            INVOKESTATIC ->
                return getMethodStackSize((insn as MethodInsnNode).desc)
            
            INVOKEDYNAMIC ->
                return getMethodStackSize((insn as InvokeDynamicInsnNode).desc)
            
            JSR, RET ->
                throw UnsupportedOperationException("JSR and RET are not supported")
            
            else ->
                return 0
        }
    }
    
    private fun getMethodStackSize(desc: String): Int {
        val argumentTypes = Type.getArgumentTypes(desc)
        val returnType = Type.getReturnType(desc)
        var size = argumentTypes.sumOf { type ->
            if (type.sort == Type.LONG || type.sort == Type.DOUBLE) (-2).toInt() else -1 // toInt() needed because of an intellij bug (see https://youtrack.jetbrains.com/issue/KT-46360)
        }
        
        if (returnType.sort != Type.VOID)
            size += (if (returnType.sort == Type.LONG || returnType.sort == Type.DOUBLE) 2 else 1)
        
        return size
    }
    
    /**
     * Emulates the control flow of a method and determines the stack size when entering a label. These entry nodes are
     * called "gates" in the following docs and code.
     */
    fun emulateLabelGates(methodNode: MethodNode): Map<LabelNode, LabelGateInfo> {
        val gates = methodNode.instructions.asSequence().filterIsInstance<LabelNode>().map { it to LabelGateInfo() }.toMap()
        if (gates.isEmpty())
            return gates // No labels in this method
        
        val instructions = methodNode.instructions
        if (instructions.size() == 0)
            return gates
        
        val finished = mutableListOf<LabelNode>()
        val toProcess = ArrayDeque<LabelNode>()
        
        // If the first instruction is not a label, we need to manually determine the size
        var size = 0
        var current = instructions.first()
        while (current !is LabelNode) {
            size += getStackSize(current)
            current = current.next
        }
        gates[current]!!.entrySize = size // A label has been reached, so we can set the entry gate size
        toProcess.add(current)
        
        while (toProcess.isNotEmpty()) {
            val next = toProcess.removeFirst()
            emulateLabel(next, gates, finished, toProcess)
        }
        
        return gates
    }
    
    private fun emulateLabel(
        label: LabelNode,
        gates: Map<LabelNode, LabelGateInfo>,
        finished: MutableList<LabelNode>,
        toProcess: MutableList<LabelNode>,
    ) {
        var current = label.next
        var size = gates[label]!!.entrySize // the start stack size of the label
        
        while (current != null) { // Until we reach the end of the method
            size += getStackSize(current)
            if (current is JumpInsnNode && current.label !in finished) { // A new branch of the control flow with a previously unknown label
                toProcess += current.label
                gates[current.label]!!.entrySize = size // When jumping to this label, the starting stack size is the current size
                if (current.opcode == GOTO) // Can't fall through so we can stop here
                    break
            } else if (current is LabelNode && current !in finished) { // Fell through to the next label
                toProcess += current
                gates[current]!!.entrySize = size // When reaching this label, the starting stack size is the current size
                break
            }
            current = current.next
        }
        finished += label
    }
}