package xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper

import org.objectweb.asm.tree.InsnList
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.patch.Patcher
import xyz.xenondevs.bytebase.patch.Patcher.LoadedPatch
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

internal typealias InsnListConstructor = () -> InsnList

internal abstract class Remapper<A : Annotation>(
    protected val patcher: Patcher,
    protected val patch: LoadedPatch,
    protected val mappings: MappingsContainer,
    protected val newDefinitions: MutableSet<ClassWrapper>
) {
    
    protected fun addNewClass(classWrapper: ClassWrapper) {
        newDefinitions.add(classWrapper)
    }
    
}

internal abstract class PropertyRemapper<A : Annotation>(
    patcher: Patcher,
    patch: LoadedPatch,
    mappings: MappingsContainer,
    newDefinitions: MutableSet<ClassWrapper>
) : Remapper<A>(patcher, patch, mappings, newDefinitions) {
    
    abstract fun <T> processProperty(annotation: A, prop: KProperty<T>)
    
}

internal abstract class NonAnnotatedPropertyRemapper(
    patcher: Patcher,
    patch: LoadedPatch,
    mappings: MappingsContainer,
    newDefinitions: MutableSet<ClassWrapper>
) : Remapper<Nothing>(patcher, patch, mappings, newDefinitions) {
    
    abstract fun <T> processProperty(prop: KProperty<T>)
    
}

internal abstract class FunctionRemapper<A : Annotation>(
    patcher: Patcher,
    patch: LoadedPatch,
    mappings: MappingsContainer,
    newDefinitions: MutableSet<ClassWrapper>
) : Remapper<A>(patcher, patch, mappings, newDefinitions) {
    
    abstract fun <R> processFunction(annotation: A, func: KFunction<R>)
    
}

internal abstract class NonAnnotatedFunctionRemapper(
    patcher: Patcher,
    patch: LoadedPatch,
    mappings: MappingsContainer,
    newDefinitions: MutableSet<ClassWrapper>
) : Remapper<Nothing>(patcher, patch, mappings, newDefinitions) {
    
    abstract fun <R> processFunction(func: KFunction<R>)
    
}