package xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper

import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmProperty
import org.objectweb.asm.tree.InsnList
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.patch.Patcher
import xyz.xenondevs.bytebase.patch.Patcher.LoadedPatch

internal typealias InsnListConstructor = () -> InsnList

internal abstract class Remapper<A : Annotation>(
    protected val patcher: Patcher,
    protected val patch: LoadedPatch,
    protected val mappings: MappingsContainer,
    protected val newDefinitions: MutableMap<String, ClassWrapper>
) {
    
    protected fun addNewClass(classWrapper: ClassWrapper) {
        newDefinitions[classWrapper.name] = classWrapper
    }
    
    open fun finish() = Unit
    
}

internal abstract class PropertyRemapper<A : Annotation>(
    patcher: Patcher,
    patch: LoadedPatch,
    mappings: MappingsContainer,
    newDefinitions: MutableMap<String, ClassWrapper>
) : Remapper<A>(patcher, patch, mappings, newDefinitions) {
    
    abstract fun processProperty(annotation: A, prop: KmProperty)
    
}

internal abstract class NonAnnotatedPropertyRemapper(
    patcher: Patcher,
    patch: LoadedPatch,
    mappings: MappingsContainer,
    newDefinitions: MutableMap<String, ClassWrapper>
) : Remapper<Nothing>(patcher, patch, mappings, newDefinitions) {
    
    abstract fun processProperty(prop: KmProperty)
    
}

internal abstract class FunctionRemapper<A : Annotation>(
    patcher: Patcher,
    patch: LoadedPatch,
    mappings: MappingsContainer,
    newDefinitions: MutableMap<String, ClassWrapper>
) : Remapper<A>(patcher, patch, mappings, newDefinitions) {
    
    abstract fun processFunction(annotation: A, func: KmFunction)
    
}

internal abstract class NonAnnotatedFunctionRemapper(
    patcher: Patcher,
    patch: LoadedPatch,
    mappings: MappingsContainer,
    newDefinitions: MutableMap<String, ClassWrapper>
) : Remapper<Nothing>(patcher, patch, mappings, newDefinitions) {
    
    abstract fun processFunction(func: KmFunction)
    
}