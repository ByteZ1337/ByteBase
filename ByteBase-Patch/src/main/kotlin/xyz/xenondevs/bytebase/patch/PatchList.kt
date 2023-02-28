package xyz.xenondevs.bytebase.patch

import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.patch.annotation.DirectPatch
import xyz.xenondevs.bytebase.patch.annotation.Patch
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations

internal class PatchList : Iterable<Patcher.LoadedPatch> {
    
    private val patches = TreeSet<Patcher.LoadedPatch>()
    
    fun addPatch(patch: KClass<*>) {
        val patchAnnotation = patch.findAnnotations(Patch::class).firstOrNull()
        if (patchAnnotation != null) {
            addDefaultPatch(patch, patchAnnotation)
            return
        }
        val directPatchAnnotation = patch.findAnnotations(DirectPatch::class).firstOrNull()
        if (directPatchAnnotation != null) {
            addDirectPatch(patch, directPatchAnnotation)
            return
        }
        throw IllegalArgumentException("Patch class $patch does not have a @Patch or @DirectPatch annotation!")
    }
    
    @Suppress("DuplicatedCode")
    private fun addDefaultPatch(patchClass: KClass<*>, annotation: Patch) {
        try {
            val target = VirtualClassPath[annotation.target]
            patches += Patcher.LoadedPatch(target, annotation.priority, patchClass, VirtualClassPath[patchClass])
        } catch (ex: IllegalStateException) {
            if (ex.cause is ClassNotFoundException || ex.cause is NoClassDefFoundError) {
                throw IllegalStateException("Could not find target class ${annotation.target} for patch ${patchClass.qualifiedName}", ex)
            }
        }
        
    }
    
    @Suppress("DuplicatedCode")
    private fun addDirectPatch(patchClass: KClass<*>, annotation: DirectPatch) {
        try {
            val target = VirtualClassPath[annotation.target]
            patches += Patcher.LoadedPatch(target, annotation.priority, patchClass, VirtualClassPath[patchClass])
        } catch (ex: IllegalStateException) {
            if (ex.cause is ClassNotFoundException || ex.cause is NoClassDefFoundError) {
                throw IllegalStateException("Could not find target class ${annotation.target} for patch ${patchClass.qualifiedName}", ex)
            }
        }
    }
    
    override fun iterator() = patches.iterator()
    
}