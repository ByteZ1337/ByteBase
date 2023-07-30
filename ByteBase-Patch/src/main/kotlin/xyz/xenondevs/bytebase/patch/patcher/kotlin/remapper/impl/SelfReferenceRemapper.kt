package xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.impl

import kotlinx.metadata.KmProperty
import kotlinx.metadata.isVar
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.patch.Patcher
import xyz.xenondevs.bytebase.patch.Patcher.LoadedPatch
import xyz.xenondevs.bytebase.patch.annotation.FieldAccessor
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.MappingsContainer
import xyz.xenondevs.bytebase.patch.patcher.kotlin.remapper.PropertyRemapper

internal class SelfReferenceRemapper(
    patcher: Patcher,
    patch: LoadedPatch,
    mappings: MappingsContainer,
    newDefinitions: MutableMap<String, ClassWrapper>
) : PropertyRemapper<FieldAccessor>(patcher, patch, mappings, newDefinitions) {
    
    override fun processProperty(annotation: FieldAccessor, prop: KmProperty) {
        if (prop.isVar)
            throw IllegalArgumentException("Property ${prop.name} is mutable and cannot be used as a self reference")
        
        // aload_0 is already in front of the field get/method call
        mappings.addRemap(prop) { list, insn -> list.remove(insn) }
    }
    
}