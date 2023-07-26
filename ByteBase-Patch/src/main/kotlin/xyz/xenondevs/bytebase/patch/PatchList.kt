package xyz.xenondevs.bytebase.patch

import org.objectweb.asm.Type
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.patch.annotation.DirectPatch
import xyz.xenondevs.bytebase.patch.annotation.Patch
import xyz.xenondevs.bytebase.util.desc
import xyz.xenondevs.bytebase.util.toMap
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations

internal class PatchList : Iterable<Patcher.LoadedPatch> {
    
    private val patches = TreeSet<Patcher.LoadedPatch>()
    
    fun addPatch(patch: KClass<*>) {
        if (checkForDirectPatch(patch))
            return
        
        val patchAnnotation = patch.findAnnotations(Patch::class).firstOrNull()
        if (patchAnnotation != null) {
            addDefaultPatch(patch, patchAnnotation)
            return
        }
        
        throw IllegalArgumentException("Patch class $patch does not have a @Patch or @DirectPatch annotation!")
    }
    
    @Suppress("DuplicatedCode")
    private fun addDefaultPatch(patchClass: KClass<*>, annotation: Patch) {
        val targetName = annotation.target.replace('.', '/')
        try {
            val target = VirtualClassPath[targetName]
            patches += Patcher.LoadedPatch(target, annotation.priority, patchClass, VirtualClassPath[patchClass], annotation.patchMode.getActualMode(targetName))
        } catch (ex: IllegalStateException) {
            if (ex.cause is ClassNotFoundException || ex.cause is NoClassDefFoundError) {
                throw IllegalStateException("Could not find target class $targetName for patch ${patchClass.qualifiedName}", ex)
            }
        }
        
    }
    
    @Suppress("UNCHECKED_CAST")
    fun checkForDirectPatch(patchClass: KClass<*>): Boolean {
        val patchWrapper = VirtualClassPath[patchClass]
        val map = patchWrapper.visibleAnnotations?.find { it.desc == DirectPatch::class.desc }?.toMap() ?: return false
        
        val targetName = (map["target"] as? Type)?.internalName
            ?: throw IllegalArgumentException("DirectPatch annotation on $patchClass does not have a target!")
        val priority = (map["priority"] as? Int)?.toUInt() ?: 100u
        val patchMode = enumValueOf<PatchMode>((map["patchMode"] as? Array<String>)?.getOrNull(1) ?: "AUTOMATIC")
        
        try {
            val target = VirtualClassPath[targetName]
            patches += Patcher.LoadedPatch(target, priority, patchClass, VirtualClassPath[patchClass], patchMode.getActualMode(targetName))
        } catch (ex: IllegalStateException) {
            if (ex.cause is ClassNotFoundException || ex.cause is NoClassDefFoundError) {
                throw IllegalStateException("Could not find target class $targetName for patch ${patchClass.qualifiedName}", ex)
            }
        }
        return true
    }
    
    override fun iterator() = patches.iterator()
    
}