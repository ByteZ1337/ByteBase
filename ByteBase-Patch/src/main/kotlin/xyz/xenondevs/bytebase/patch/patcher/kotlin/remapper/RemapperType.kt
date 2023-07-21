package xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper

import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.patch.Patcher
import xyz.xenondevs.bytebase.patch.Patcher.LoadedPatch
import xyz.xenondevs.bytebase.patch.annotation.FieldAccessor
import xyz.xenondevs.bytebase.patch.annotation.SelfReference
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.impl.FieldAccessorRemapper
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.impl.NewFieldRemapper
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.impl.SelfReferenceRemapper
import kotlin.reflect.KClass

internal typealias RemapperConstructor = (Patcher, LoadedPatch, MappingsContainer, MutableSet<ClassWrapper>) -> Remapper<*>

internal enum class RemapperType(val annotation: KClass<out Annotation>?, val constructor: RemapperConstructor) {
    FIELD_ACCESSOR(FieldAccessor::class, ::FieldAccessorRemapper),
    SELF_REFERENCE(SelfReference::class, ::SelfReferenceRemapper),
    NEW_FIELD(null, ::NewFieldRemapper);
    
    companion object {
        
        fun getRemapper(annotation: Annotation?, patcher: Patcher, patch: LoadedPatch, mappings: MappingsContainer, newDefinitions: MutableSet<ClassWrapper>): Remapper<*>? =
            entries
                .firstOrNull { (it.annotation?.isInstance(annotation)) ?: (annotation == null) }
                ?.constructor?.invoke(patcher, patch, mappings, newDefinitions)
        
    }
    
}