package xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.impl

import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.patch.Patcher
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.MappingsContainer
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.NonAnnotatedPropertyRemapper
import kotlin.reflect.KProperty

internal class NewFieldRemapper(
    patcher: Patcher,
    patch: Patcher.LoadedPatch,
    mappings: MappingsContainer,
    newDefinitions: MutableSet<ClassWrapper>
) : NonAnnotatedPropertyRemapper(patcher, patch, mappings, newDefinitions) {
    
    override fun <T> processProperty(prop: KProperty<T>) {
        TODO("Not yet implemented")
    }
    
}