package xyz.xenondevs.bytebase.jvm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import xyz.xenondevs.bytebase.util.OBJECT_TYPE

class ClassWrapper(var fileName: String) : ClassNode(Opcodes.ASM9) {
    
    val originalName = fileName
    val inheritanceTree
        get() = VirtualClassPath.getTree(this)
    
    constructor(fileName: String, byteCode: ByteArray, parsingOptions: Int = ClassReader.SKIP_FRAMES) : this(fileName) {
        ClassReader(byteCode).accept(this, parsingOptions)
    }
    
    fun assemble() = ClassWriter(ClassWriter.COMPUTE_FRAMES).also(this::accept).toByteArray()!!
    
    fun isAssignableFrom(clazz: ClassWrapper): Boolean {
        if (this.name == OBJECT_TYPE || this == clazz)
            return true
        
        return clazz.inheritanceTree.superClasses.contains(this)
    }
    
    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + originalName.hashCode()
        return result
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as ClassWrapper
        
        return name == other.name && fileName == other.fileName
    }
    
    override fun toString() = name ?: fileName
    
}
