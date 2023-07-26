package xyz.xenondevs.bytebase.patch.patcher.kotlin

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.patch.Patcher
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.MappingsContainer
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.NonAnnotatedPropertyRemapper
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.PropertyRemapper
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.Remapper
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.RemapperType
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

internal class FieldRemapper(
    val patcher: Patcher,
    val patch: Patcher.LoadedPatch
) {
    
    val logger get() = patcher.logger
    
    private val remappers = Object2ObjectOpenHashMap<KClass<out Annotation>?, Remapper<*>>()
    private val mappings = MappingsContainer()
    private val newDefs = Object2ObjectOpenHashMap<String, ClassWrapper>()
    
    @Suppress("UNCHECKED_CAST")
    fun generateMappings() {
        patch.patchClass.declaredMemberProperties.forEach { prop ->
            val annotation = prop.annotations.firstOrNull()
            val remapper = remappers.getOrPut(annotation?.annotationClass) {
                RemapperType.getRemapper(annotation, patcher, patch, mappings, newDefs) ?: return@forEach
            }
            
            if (remapper is NonAnnotatedPropertyRemapper)
                remapper.processProperty(prop)
            else if (remapper is PropertyRemapper<*>)
                (remapper as PropertyRemapper<Annotation>).processProperty(annotation!!, prop)
        }
        remappers.values.forEach { it.finish() }
        newDefs.forEach { (_, clazz) -> File(clazz.className + ".class").writeBytes(clazz.assemble()) }
    }
    
    fun remap(method: MethodNode) {
        logger.debug("- Remapping field calls")
        val insns = method.instructions
        method.instructions
            .asSequence()
            .filter { it is MethodInsnNode || it is FieldInsnNode }
            .forEach { insn ->
                when (insn) {
                    is MethodInsnNode -> {
                        if (insn in mappings)
                            mappings(insns, insn)
                    }
                    
                    is FieldInsnNode -> {
                        if (insn in mappings)
                            mappings(insns, insn)
                    }
                }
            }
    }
}