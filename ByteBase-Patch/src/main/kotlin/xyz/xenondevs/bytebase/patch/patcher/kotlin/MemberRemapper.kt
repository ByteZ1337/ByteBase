package xyz.xenondevs.bytebase.patch.patcher.kotlin

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.patch.Patcher
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.FieldRemapperType
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.MappingsContainer
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.NonAnnotatedPropertyRemapper
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.PropertyRemapper
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.Remapper
import xyz.xenondevs.bytebase.patch.util.defineClass
import xyz.xenondevs.bytebase.patch.util.resolveAnnotations
import java.io.File
import kotlin.reflect.KClass

internal class MemberRemapper(
    val patcher: Patcher,
    val patch: Patcher.LoadedPatch
) {
    
    val logger get() = patcher.logger
    
    private val fieldRemappers = Object2ObjectOpenHashMap<KClass<out Annotation>?, Remapper<*>>()
    private val functionRemappers = Object2ObjectOpenHashMap<KClass<out Annotation>?, Remapper<*>>()
    private val mappings = MappingsContainer(patch)
    private val newDefs = Object2ObjectOpenHashMap<String, ClassWrapper>()
    
    @Suppress("UNCHECKED_CAST")
    fun generateMappings() {
        patch.kmClass.properties.forEach { prop ->
            val annotation = prop.resolveAnnotations(patch.patchWrapper).firstOrNull()
            val remapper = fieldRemappers.getOrPut(annotation?.annotationClass) {
                FieldRemapperType.getRemapper(annotation, patcher, patch, mappings, newDefs) ?: return@forEach
            }
            
            if (remapper is NonAnnotatedPropertyRemapper)
                remapper.processProperty(prop)
            else if (remapper is PropertyRemapper<*>)
                (remapper as PropertyRemapper<Annotation>).processProperty(annotation!!, prop)
        }
        fieldRemappers.values.forEach(Remapper<*>::finish)
        newDefs.forEach { (_, clazz) ->
            File(clazz.className + ".class").writeBytes(clazz.assemble())
            this::class.java.classLoader.defineClass(clazz)
        }
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