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
import xyz.xenondevs.bytebase.asm.access.Access
import xyz.xenondevs.bytebase.asm.access.ReferencingAccess
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.Int32
import xyz.xenondevs.bytebase.util.accessWrapper
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod

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
    
    constructor(reader: ClassReader, parsingOptions: Int32 = ClassReader.SKIP_FRAMES) : super(Opcodes.ASM9) {
        reader.accept(this, parsingOptions)
        this.fileName = "$name.class"
        this.originalName = fileName
    }
    
    constructor(fileName: String, byteCode: ByteArray, parsingOptions: Int32 = ClassReader.SKIP_FRAMES)
        : this(fileName, ClassReader(byteCode), parsingOptions)
    
    fun assemble(computeFrames: Boolean = true) = ClassWriter(if (computeFrames) COMPUTE_FRAMES else 0).also(this::accept).toByteArray()!!
    
    //<editor-fold desc="Field getters" defaultstate="collapsed">
    
    fun getField(name: String, desc: String) = fields?.find { it.name == name && it.desc == desc }
    
    fun getField(name: String) = fields?.find { it.name == name }
    
    operator fun get(field: Field) = getField(field.name, Type.getDescriptor(field.type))
    
    operator fun get(kProperty: KProperty<*>) = kProperty.javaField?.let { this[it] }
    
    operator fun contains(field: FieldNode) = getField(field.name, field.desc) != null
    
    fun canAccessField(memberReference: MemberReference, assertInSuper: Boolean = false) =
        canAccess(memberReference, memberReference.resolveField().accessWrapper, assertInSuper)
    
    //</editor-fold>
    
    //<editor-fold desc="Method getters" defaultstate="collapsed">
    
    fun getMethod(name: String, type: Type) = methods?.find { it.name == name && it.desc == type.descriptor }
    
    fun getMethod(name: String, desc: String) = methods?.find { it.name == name && it.desc == desc }
    
    fun getMethod(name: String, includesDesc: Boolean = false) = methods?.find {
        if (includesDesc) it.name + it.desc == name else it.name == name
    }
    
    @Deprecated("Use get(method) instead", ReplaceWith("get(method)"))
    fun getMethodLike(method: Method) = get(method)
    
    fun getOrCreateMethod(name: String, desc: String, access: Int = ACC_PUBLIC): MethodNode {
        val method = getMethod(name, desc)
        if (method != null) return method
        val newMethod = MethodNode(access, name, desc, null, null)
        newMethod.instructions = buildInsnList { _return() }
        methods?.add(newMethod)
        return newMethod
    }
    
    fun getOrCreateClassInit() = getOrCreateMethod("<clinit>", "()V", ACC_PUBLIC or ACC_STATIC)
    
    operator fun get(method: Method) = getMethod(method.name, Type.getMethodDescriptor(method))
    
    operator fun get(constructor: Constructor<*>) = getMethod("<init>", Type.getConstructorDescriptor(constructor))
    
    operator fun get(kFunction: KFunction<*>): MethodNode? {
        val method = kFunction.javaMethod
        if (method != null)
            return get(method)
        
        val constructor = kFunction.javaConstructor
        if (constructor != null)
            return get(constructor)
        
        return null
    }
    
    operator fun contains(method: MethodNode) = getMethod(method.name, method.desc) != null
    
    fun canAccessMethod(memberReference: MemberReference, assertInSuper: Boolean = false) =
        canAccess(memberReference, memberReference.resolveMethod().accessWrapper, assertInSuper)
    
    //</editor-fold>
    
    fun canAccess(ref: MemberReference, access: Access, assertInSuper: Boolean): Boolean {
        if (ref.owner == name)
            return true
        
        if (access.isPrivate())
            return false
        
        if (access.isPublic())
            return true
        
        val isSuperClass = assertInSuper || inheritanceTree.superClasses.any { it.name == ref.owner }
        if (access.isProtected() && isSuperClass)
            return true
        
        // package private
        return ref.owner.substringBeforeLast('/') == name.substringBeforeLast('/')
    }
    
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
