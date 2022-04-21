package xyz.xenondevs.bytebase.asm.refactor

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.JavaArchive
import org.objectweb.asm.commons.ClassRemapper as ASMClassRemapper

/**
 * Class that automatically applies mappings
 * to a [JavaArchive].
 *
 * Mapping formats are:
 *
 * * Classes: package/class
 * * Methods: class.methoddesc
 * * Fields: class.field.desc
 * * Local Variables: class.methoddesc.var.varDesc
 */
class Refactorer(private val jar: JavaArchive, private val mappings: Map<String, String>) {
    
    /**
     * The [MemberRemapper] with the given [mappings].
     */
    val remapper = MemberRemapper(mappings)
    
    /**
     * [ASMClassRemapper] implementation to support renaming local variables.
     */
    inner class ClassRemapper(classVisitor: ClassVisitor) : ASMClassRemapper(classVisitor, remapper) {
        
        override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            val basicRemapper = super.visitMethod(access, name, descriptor, signature, exceptions)
            return if (basicRemapper == null) null else MethodRemapper(basicRemapper, className, descriptor, name)
        }
        
    }
    
    /**
     * [MethodVisitor] implementation to support renaming local variables via the [ClassRemapper].
     */
    inner class MethodRemapper(
        methodVisitor: MethodVisitor,
        val owner: String,
        val desc: String,
        val name: String
    ) : MethodVisitor(Opcodes.ASM9, methodVisitor) {
        
        override fun visitLocalVariable(name: String, descriptor: String, signature: String?, start: Label, end: Label, index: Int) {
            super.visitLocalVariable(
                remapper.mapLocalVariableName(owner, this.name, this.desc, name, descriptor),
                descriptor, signature, start, end, index
            )
        }
        
    }
    
    /**
     * Refactors the [jar] with the given [mappings].
     *
     * **Old classes will be deleted**
     */
    fun refactor() {
        val newClasses = mutableListOf<ClassWrapper>()
        
        jar.classes.forEach { clazz ->
            val newClass = ClassWrapper(clazz.name)
            clazz.accept(ClassRemapper(newClass))
            newClass.fileName = newClass.name + ".class"
            if (!newClass.sourceFile.isNullOrBlank())
                newClass.sourceFile = "${clazz.className}.java"
            newClass.sourceDebug = null
            newClasses.add(newClass)
        }
        jar.classes.clear()
        jar.classes.addAll(newClasses)
    }
    
}