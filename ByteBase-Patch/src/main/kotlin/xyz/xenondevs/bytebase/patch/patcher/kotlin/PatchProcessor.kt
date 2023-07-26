package xyz.xenondevs.bytebase.patch.patcher.kotlin

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import xyz.xenondevs.bytebase.patch.Patcher

private class DummyVisitor(parent: MethodVisitor) : MethodVisitor(Opcodes.ASM9, parent)

internal class PatchProcessor(
    val patcher: Patcher,
    val patch: Patcher.LoadedPatch
) {
    
    val logger get() = patcher.logger
    
    private val fieldRemapper = FieldRemapper(patcher, patch)
    private val methodPatcher = MethodPatcher(patcher, patch) { fieldRemapper.remap(it) }
    
    fun runPatches() {
        fieldRemapper.generateMappings()
        methodPatcher.patchMethods()
    }
    
    
}