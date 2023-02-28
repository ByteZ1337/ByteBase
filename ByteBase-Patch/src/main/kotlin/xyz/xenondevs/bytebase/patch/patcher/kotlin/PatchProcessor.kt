package xyz.xenondevs.bytebase.patch.patcher.kotlin

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.patch.Patcher

private class DummyVisitor(parent: MethodVisitor) : MethodVisitor(Opcodes.ASM9, parent)

internal class PatchProcessor(
    val patch: Patcher.LoadedPatch,
    val newClass: ClassWrapper
) {
    
    //    private val fieldPatcher = FieldPatcher(patch, newClass)
    private val methodPatcher = MethodPatcher(patch, newClass) { /*fieldPatcher.FieldCallRemapper(it)*/  DummyVisitor(it) }
    
    fun runPatches() {
//        fieldPatcher.patchFields()
        methodPatcher.patchMethods()
    }
    
    
}