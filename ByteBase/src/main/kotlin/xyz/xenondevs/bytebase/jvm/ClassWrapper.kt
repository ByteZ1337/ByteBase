package xyz.xenondevs.bytebase.jvm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.asm.ClassWriter
import xyz.xenondevs.bytebase.asm.OBJECT_CLASS
import xyz.xenondevs.bytebase.asm.OBJECT_TYPE
import xyz.xenondevs.bytebase.asm.access.ReferencingAccess
import xyz.xenondevs.bytebase.util.Int32

class ClassWrapper : ClassNode {
    
    var fileName: String
    val originalName: String
    
    val accessWrapper = ReferencingAccess(::access) { this.access = it }
    val inheritanceTree
        get() = VirtualClassPath.getTree(this)
    val subClasses
        get() = inheritanceTree.subClasses
    val superClass
        get() = superName?.let { VirtualClassPath.getClass(superName) }
    
    constructor(fileName: String) : super(Opcodes.ASM9) {
        this.fileName = fileName
        this.originalName = fileName
        this.name = fileName.removeSuffix(".class")
        this.version = OBJECT_CLASS.version
        this.superName = OBJECT_TYPE
    }
    
    constructor(fileName: String, reader: ClassReader, parsingOptions: Int32 = ClassReader.SKIP_FRAMES) : super(Opcodes.ASM9) {
        this.fileName = fileName
        this.originalName = fileName
        reader.accept(this, parsingOptions)
    }
    
    constructor(fileName: String, byteCode: ByteArray, parsingOptions: Int32 = ClassReader.SKIP_FRAMES)
        : this(fileName, ClassReader(byteCode), parsingOptions)
    
    fun assemble() = ClassWriter().also(this::accept).toByteArray()!!
    
    fun load() = VirtualClassPath.classLoader.loadClass(this)
    
    fun getField(name: String, desc: String) = fields?.find { it.name == name && it.desc == desc }
    
    fun getField(name: String) = fields?.find { it.name == name }
    
    operator fun contains(field: FieldNode) = getField(field.name, field.desc) != null
    
    fun getMethod(name: String, desc: String) = methods?.find { it.name == name && it.desc == desc }
    
    fun getMethod(name: String) = methods?.find { it.name == name }
    
    operator fun contains(method: MethodNode) = getMethod(method.name, method.desc) != null
    
    fun isAssignableFrom(clazz: ClassWrapper): Boolean {
        if (this.name == OBJECT_TYPE || this == clazz)
            return true
        
        return clazz.inheritanceTree.superClasses.contains(this)
    }
    
    fun isInterface() = accessWrapper.isInterface()
    
    fun isEnum() = accessWrapper.isEnum()
    
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
