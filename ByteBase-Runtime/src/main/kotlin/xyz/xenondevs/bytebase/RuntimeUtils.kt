package xyz.xenondevs.bytebase

import net.bytebuddy.agent.ByteBuddyAgent
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.bytebase.util.runtimeClass
import xyz.xenondevs.bytebase.util.toMap
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf
import org.objectweb.asm.Type as AsmType
import java.lang.reflect.Array as JavaArray

val INSTRUMENTATION: Instrumentation by lazy { ByteBuddyAgent.install() }

fun Class<*>.redefine(transformer: ClassWrapper.() -> Unit) {
    val wrapper = VirtualClassPath[this]
    wrapper.transformer()
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}

fun Class<*>.redefineMethod(method: String, desc: String, transformer: MethodNode.() -> Unit) {
    val wrapper = VirtualClassPath[this]
    wrapper.getMethod(method, desc)!!.transformer()
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}

fun Class<*>.redefineMethod(method: String, transformer: MethodNode.() -> Unit) {
    val wrapper = VirtualClassPath[this]
    wrapper.getMethod(method)!!.transformer()
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}

fun Class<*>.setInstructions(method: String, desc: String, instructions: InsnList) {
    val wrapper = VirtualClassPath.getClass(this.internalName)
    wrapper.getMethod(method, desc)!!.instructions = instructions
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}

fun Class<*>.setInstructions(method: String, instructions: InsnList) {
    val wrapper = VirtualClassPath.getClass(this.internalName)
    wrapper.getMethod(method)!!.instructions = instructions
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}

fun Class<*>.insertInstructions(method: String, desc: String, instructions: InsnList) {
    val wrapper = VirtualClassPath.getClass(this.internalName)
    wrapper.getMethod(method, desc)!!.instructions.insert(instructions)
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}

fun Class<*>.insertInstructions(method: String, instructions: InsnList) {
    val wrapper = VirtualClassPath.getClass(this.internalName)
    wrapper.getMethod(method)!!.instructions.insert(instructions)
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}

//<editor-fold desc="KClass extensions" defaultstate="collapsed">

fun KClass<*>.redefine(transformer: ClassWrapper.() -> Unit) = this.java.redefine(transformer)

fun KClass<*>.redefineMethod(method: String, desc: String, transformer: MethodNode.() -> Unit) = this.java.redefineMethod(method, desc, transformer)

fun KClass<*>.redefineMethod(method: String, transformer: MethodNode.() -> Unit) = this.java.redefineMethod(method, transformer)

fun KClass<*>.setInstructions(method: String, desc: String, instructions: InsnList) = this.java.setInstructions(method, desc, instructions)

fun KClass<*>.setInstructions(method: String, instructions: InsnList) = this.java.setInstructions(method, instructions)

fun KClass<*>.insertInstructions(method: String, desc: String, instructions: InsnList) = this.java.insertInstructions(method, desc, instructions)

fun KClass<*>.insertInstructions(method: String, instructions: InsnList) = this.java.insertInstructions(method, instructions)

//</editor-fold>

fun ClassWrapper.load(): Class<*> {
    return ClassWrapperLoader.DEFAULT.loadClass(this)
}

object RuntimeUtils {
    
    /**
     * **Only works for Kotlin annotations!**
     */
    fun <A : Annotation> constructAnnotation(clazz: KClass<A>, map: Map<String, Any?>): A {
        val javaClass = clazz.java
        if (!clazz.java.isAnnotation)
            throw IllegalArgumentException("Class ${clazz.internalName} is not an annotation")
        val constructor = clazz.primaryConstructor
            ?: throw IllegalArgumentException("Class ${clazz.internalName} has no primary constructor")
        
        val params = LinkedHashMap<KParameter, Any?>()
        
        constructor.parameters.forEach { param ->
            val name = param.name!!
            if (name in map) {
                params[param] = constructValueFromMap(param.type, map[name])
            } else {
                val default = javaClass.getMethod(name).defaultValue
                requireNotNull(default) { "Parameter $name is not present in the map and has no default value!" }
                params[param] = default
            }
        }
        
        return constructor.callBy(params)
    }
    
    /**
     * **Only works for Kotlin annotations!**
     */
    inline fun <reified A : Annotation> constructAnnotation(map: Map<String, Any?>) = constructAnnotation(A::class, map)
    
    @Suppress("UNCHECKED_CAST")
    private fun constructValueFromMap(type: KType, value: Any?, nonPrimitive: Boolean = false): Any? {
        if (value == null) return null
        
        if (!nonPrimitive) {
            when (type) {
                typeOf<Int>() -> return value as Int
                typeOf<IntArray>() -> return (value as List<Int>).toIntArray()
                typeOf<Long>() -> return value as Long
                typeOf<LongArray>() -> return (value as List<Long>).toLongArray()
                typeOf<Float>() -> return value as Float
                typeOf<FloatArray>() -> return (value as List<Float>).toFloatArray()
                typeOf<Double>() -> return value as Double
                typeOf<DoubleArray>() -> return (value as List<Double>).toDoubleArray()
                typeOf<Short>() -> return value as Short
                typeOf<ShortArray>() -> return (value as List<Short>).toShortArray()
                typeOf<Byte>() -> return value as Byte
                typeOf<ByteArray>() -> return (value as List<Byte>).toByteArray()
                typeOf<Char>() -> return value as Char
                typeOf<CharArray>() -> return (value as List<Char>).toCharArray()
                typeOf<Boolean>() -> return value as Boolean
                typeOf<BooleanArray>() -> return (value as List<Boolean>).toBooleanArray()
            }
        }
        
        if (type == typeOf<String>()) {
            return value as String
        } else if (type.isSubtypeOf(typeOf<Enum<*>>())) {
            value as Array<String>
            val enumClass = type.classifier as KClass<Enum<*>>
            val constantName = value[1]
            return enumClass.java.enumConstants.first { constantName == it.name }
        } else if (type.classifier == KClass::class) {
            value as AsmType
            return value.runtimeClass
        } else if (type.isSubtypeOf(typeOf<Array<*>>())) {
            value as List<Any>
            val arrayType = type.arguments[0].type!!
            val list = value.map { v -> constructValueFromMap(arrayType, v, nonPrimitive = true) }
            val javaArrayType = (arrayType.classifier as KClass<*>).java
            val javaArray = JavaArray.newInstance(javaArrayType, list.size)
            list.forEachIndexed { index, item -> JavaArray.set(javaArray, index, item) }
            return javaArray
        } else if (type.isSubtypeOf(typeOf<Annotation>())) {
            value as AnnotationNode
            return constructAnnotation(type.classifier as KClass<Annotation>, value.toMap())
        }
        
        throw IllegalArgumentException("Unsupported type $type")
    }
    
}