package xyz.xenondevs.bytebase.patch

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import xyz.xenondevs.bytebase.INSTRUMENTATION
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.patch.logging.PatchLogger
import xyz.xenondevs.bytebase.patch.logging.SimpleLogger
import xyz.xenondevs.bytebase.patch.patcher.kotlin.PatchProcessor
import kotlin.reflect.KClass

class Patcher(
    val postProcessors: List<PostProcessor>,
    @Volatile var logger: PatchLogger = SimpleLogger
) {
    
    constructor(vararg postProcessors: PostProcessor) : this(ObjectArrayList(postProcessors))
    
    var runtimeClassGetter: (ClassWrapper) -> Class<*> = { wrapper -> Class.forName(wrapper.name.replace('/', '.')) }
    
    private val instrumentation by lazy { INSTRUMENTATION }
    
    private val patchList = PatchList()
    
    internal val additionalClassLoaderDefs = ObjectOpenHashSet<String>()
    
    fun addPatch(patch: KClass<*>) = patchList.addPatch(patch)
    
    fun runPatches(): Map<String, ByteArray> {
        val newClasses = Object2ObjectOpenHashMap<String, ByteArray>()
        patchList.forEach { patch ->
            logger.debug("")
            logger.debugLine()
            logger.debug("Running patch \"${patch.patchClass.simpleName}\" on \"${patch.target.name}\"")
            PatchProcessor(this, patch).runPatches()
            newClasses[patch.target.name] = patch.target.assemble()
            logger.debugLine()
        }
        return newClasses
    }
    
    internal class LoadedPatch(
        val target: ClassWrapper,
        val priority: UInt,
        val patchClass: KClass<*>,
        val patchWrapper: ClassWrapper,
        val patchMode: PatchMode
    ) : Comparable<LoadedPatch> {
        
        init {
            require(patchMode != PatchMode.AUTOMATIC) { "PatchMode.AUTOMATIC is not allowed for LoadedPatch! This should have been resolved by now!" }
        }
        
        override fun compareTo(other: LoadedPatch): Int {
            return priority.compareTo(other.priority)
        }
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as LoadedPatch
            
            return patchClass == other.patchClass
        }
        
        override fun hashCode(): Int {
            return patchClass.hashCode()
        }
        
    }
    
}