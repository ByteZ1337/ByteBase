package xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper

import kotlinx.metadata.KmProperty
import kotlinx.metadata.isNotDefault
import kotlinx.metadata.jvm.fieldSignature
import kotlinx.metadata.jvm.getterSignature
import kotlinx.metadata.jvm.setterSignature
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.patch.Patcher
import xyz.xenondevs.bytebase.util.copy
import xyz.xenondevs.bytebase.util.replace

internal typealias InstructionRemap = (InsnList, AbstractInsnNode) -> Unit

internal data class MappingsContainer(
    val patch: Patcher.LoadedPatch,
    val fieldGetRemaps: MutableMap<String, InstructionRemap> = HashMap(),
    val fieldSetRemaps: MutableMap<String, InstructionRemap> = HashMap(),
    val methodRemaps: MutableMap<String, InstructionRemap> = HashMap()
) {
    
    fun addRemap(prop: KmProperty, remap: InstructionRemap) {
        prop.fieldSignature?.let { fieldGetRemaps[it.name + "." + it.descriptor] = remap }
        prop.getterSignature?.takeUnless { prop.getter.isNotDefault }?.let { methodRemaps[it.toString()] = remap }
    }
    
    fun addRemap(prop: KmProperty, insns: InsnList) =
        addRemap(prop) { list, insn -> list.replace(insn, insns.copy()) }
    
    fun addRemap(prop: KmProperty, getRemap: InstructionRemap, setRemap: InstructionRemap) {
        prop.fieldSignature?.let {
            val key = it.name + "." + it.descriptor
            fieldGetRemaps[key] = getRemap
            fieldSetRemaps[key] = setRemap
        }
        prop.getterSignature?.takeUnless { prop.getter.isNotDefault }?.let { methodRemaps[it.toString()] = getRemap }
        prop.setterSignature?.takeUnless { prop.setter!!.isNotDefault }?.let { methodRemaps[it.toString()] = setRemap }
    }
    
    fun addRemap(prop: KmProperty, getInsns: InsnList, setInsns: InsnList) =
        addRemap(
            prop,
            { list, insn -> list.replace(insn, getInsns.copy()) },
            { list, insn -> list.replace(insn, setInsns.copy()) }
        )
    
    fun addFieldRemap(prop: KmProperty, remap: InstructionRemap) {
        prop.fieldSignature?.let { fieldGetRemaps[it.name + "." + it.descriptor] = remap }
    }
    
    fun addFieldRemap(prop: KmProperty, insns: InsnList) =
        addFieldRemap(prop) { list, insn -> list.replace(insn, insns.copy()) }
    
    fun addFieldRemap(prop: KmProperty, getRemap: InstructionRemap, setRemap: InstructionRemap) {
        prop.fieldSignature?.let {
            val key = it.name + "." + it.descriptor
            fieldGetRemaps[key] = getRemap
            fieldSetRemaps[key] = setRemap
        }
    }
    
    fun addFieldRemap(prop: KmProperty, getInsns: InsnList, setInsns: InsnList) =
        addFieldRemap(
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