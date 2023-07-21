package xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.util.copy
import xyz.xenondevs.bytebase.util.desc
import xyz.xenondevs.bytebase.util.replace
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter

internal typealias InstructionRemap = (InsnList, AbstractInsnNode) -> Unit

internal data class MappingsContainer(
    val fieldGetRemaps: MutableMap<String, InstructionRemap> = HashMap(),
    val fieldSetRemaps: MutableMap<String, InstructionRemap> = HashMap(),
    val methodRemaps: MutableMap<String, InstructionRemap> = HashMap()
) {
    
    fun addRemap(prop: KProperty<*>, remap: InstructionRemap) {
        prop.javaField?.let { fieldGetRemaps[it.name + "." + it.desc] = remap }
        prop.javaGetter?.let { methodRemaps[it.name + Type.getMethodDescriptor(it)] = remap }
    }
    
    fun addRemap(prop: KProperty<*>, insns: InsnList) =
        addRemap(prop) { list, insn -> list.replace(insn, insns.copy()) }
    
    fun addRemap(prop: KMutableProperty<*>, getRemap: InstructionRemap, setRemap: InstructionRemap) {
        prop.javaField?.let {
            val key = it.name + "." + it.desc
            fieldGetRemaps[key] = getRemap
            fieldSetRemaps[key] = setRemap
        }
        prop.javaGetter?.let { methodRemaps[it.name + Type.getMethodDescriptor(it)] = getRemap }
        prop.javaSetter?.let { methodRemaps[it.name + Type.getMethodDescriptor(it)] = setRemap }
    }
    
    fun addRemap(prop: KMutableProperty<*>, getInsns: InsnList, setInsns: InsnList) =
        addRemap(
            prop,
            { list, insn -> list.replace(insn, getInsns.copy()) },
            { list, insn -> list.replace(insn, setInsns.copy()) }
        )
    
    operator fun contains(insn: MethodInsnNode): Boolean {
        val key = insn.name + insn.desc
        return key in methodRemaps
    }
    
    operator fun contains(insn: FieldInsnNode): Boolean {
        val key = insn.name + "." + insn.desc
        return key in fieldGetRemaps || key in fieldSetRemaps
    }
    
    operator fun invoke(insns: InsnList, insn: MethodInsnNode) {
        val key = insn.name + insn.desc
        methodRemaps[key]!!.invoke(insns, insn)
    }
    
    operator fun invoke(insns: InsnList, insn: FieldInsnNode) {
        val key = insn.name + "." + insn.desc
        if (insn.opcode == Opcodes.GETFIELD || insn.opcode == Opcodes.GETSTATIC)
            fieldGetRemaps[key]!!.invoke(insns, insn)
        else fieldSetRemaps[key]!!.invoke(insns, insn)
    }
    
}