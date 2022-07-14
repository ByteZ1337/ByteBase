package xyz.xenondevs.bytebase.jvm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.asm.ClassWriter
import xyz.xenondevs.bytebase.asm.OBJECT_CLASS
import xyz.xenondevs.bytebase.asm.OBJECT_TYPE
import xyz.xenondevs.bytebase.asm.access.ReferencingAccess
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.Int32
import java.lang.reflect.Method

class ClassWrapper : ClassNode {
    
    /**
     * The file name of the class.
     */
    var fileName: String
    
    /**
     * The original [fileName] of the class.
     */
    val originalName: String
    
    /**
     * Access flags of the class enclosed in a [ReferencingAccess] object
     * for easier flag manipulation.
     */
    val accessWrapper = ReferencingAccess(::access) { this.access = it }
    
    /**
     * The [InheritanceTree] of this class. Contains all known inheriting and
     * implementing classes.
     */
    val inheritanceTree
        get() = VirtualClassPath.getTree(this)
    
    /**
     * All classes that inherit from this class
     */
    val subClasses
        get() = inheritanceTree.subClasses
    
    /**
     * All classes that this class inherits from
     */
    val superClasses
        get() = inheritanceTree.superClasses
    
    /**
     * Only represents the direct super class, not the super classes of the super class nor any interfaces.
     */
    val superClass
        get() = superName?.let { VirtualClassPath.getClass(superName) }
    
    /**
     * The name of the class without the package
     */
    val className
        get() = name.substringAfterLast('/')
    
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
    
    fun assemble(computeFrames: Boolean = true) = ClassWriter(if (computeFrames) COMPUTE_FRAMES else 0).also(this::accept).toByteArray()!!
    
    fun getField(name: String, desc: String) = fields?.find { it.name == name && it.desc == desc }
    
    fun getField(name: String) = fields?.find { it.name == name }
    
    operator fun contains(field: FieldNode) = getField(field.name, field.desc) != null
    
    fun getMethod(name: String, type: Type) = methods?.find { it.name == name && it.desc == type.descriptor }
    
    fun getMethod(name: String, desc: String) = methods?.find { it.name == name && it.desc == desc }
    
    fun getMethod(name: String) = methods?.find { it.name == name }
    
    fun getMethodLike(method: Method) = getMethod(method.name, Type.getMethodDescriptor(method))
    
    fun getOrCreateClassInit(): MethodNode {
        val method = getMethod("<clinit>", "()V")
        if (method != null) return method
        val newMethod = MethodNode(ACC_PUBLIC or ACC_STATIC, "<clinit>", "()V", null, null)
        newMethod.instructions = buildInsnList { _return() }
        methods?.add(newMethod)
        return newMethod
    }
    
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
