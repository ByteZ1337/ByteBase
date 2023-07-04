package xyz.xenondevs.bytebase.patch.patcher.kotlin

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.patch.Patcher
import xyz.xenondevs.bytebase.patch.util.defineClass
import java.io.File

private class DummyVisitor(parent: MethodVisitor) : MethodVisitor(Opcodes.ASM9, parent)

internal class PatchProcessor(
    val patcher: Patcher,
    val patch: Patcher.LoadedPatch,
    val newClass: ClassWrapper
) {
    
    private val fieldRemapper = FieldRemapper(patcher, patch)
    private val methodPatcher = MethodPatcher(patch, newClass) { fieldRemapper.remap(it) }
    
    fun runPatches() {
        fieldRemapper.generateMappings()
        fieldRemapper.baseHolder.get()?.let {
            patch.patchClass.java.classLoader.defineClass(it)
        }
        methodPatcher.patchMethods()
    }
    
    
}