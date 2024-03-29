package xyz.xenondevs.bytebase.jvm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

/**
 * A virtual classpath used to build a class hierarchy out of [ClassWrappers][ClassWrapper]. Can be used to process and
 * check inheritance via [InheritanceTrees][InheritanceTree].
 */
object VirtualClassPath {
    
    /**
     * A [List] of known [Jars][JavaArchive]
     */
    val knownJars = ArrayList<JavaArchive>()
    
    val classes = HashMap<String, ClassWrapper>()
    val inheritanceTrees = HashMap<ClassWrapper, InheritanceTree>()
    
    /**
     * A [List] of additional [ClassLoaders][ClassLoader] to check in [getClass]. The system classloader and the classloader
     * of [VirtualClassPath] are always checked.
     */
    val classLoaders = HashSet<ClassLoader>()
    
    /**
     * Loads a [JavaArchive] into the VirtualClassPath. Please note that this will replace classes in the VirtualClassPath
     * if a conflict occurs. If you want to load multiple Jars at once, use [loadJarWithDependencies] instead.
     */
    fun loadJar(jar: JavaArchive) {
        jar.classes.forEach {
            classes[it.name] = it
        }
        // Separate run because getTree indirectly calls getClass
        jar.classes.forEach {
            if (it !in inheritanceTrees)
                addInheritanceTree(it, emptyList())
        }
        knownJars.add(jar)
    }
    
    /**
     * Loads multiple [JavaArchive]s into the VirtualClassPath. Please note that this will replace classes in the VirtualClassPath
     * if a conflict occurs. The order of the Jars is important to ensure that the correct classes are loaded when multiple Jars
     * contain a class with the same name.
     *
     * **Also clears [knownJars]!**
     */
    fun loadJarWithDependencies(jar: JavaArchive, libraries: List<JavaArchive>, fromReload: Boolean = false) {
        libraries.forEach { lib ->
            lib.classes.filter { it.name !in classes }.forEach { classes[it.name] = it }
        }
        loadJar(jar)
        if (!fromReload) {
            knownJars.clear()
            knownJars.add(jar)
            knownJars.addAll(libraries)
        }
    }
    
    /**
     * Clears the [classes] and [inheritanceTrees] maps. And loads all [knownJars]. Please note that this will not
     * reload classes that weren't loaded with [loadJar] or [loadJarWithDependencies].
     */
    fun reload() {
        classes.clear()
        inheritanceTrees.clear()
        loadJarWithDependencies(knownJars.first(), knownJars.drop(1), fromReload = true)
    }
    
    fun getClass(name: String): ClassWrapper {
        try {
            val internalName = name.replace('.', '/')
            classes[internalName]?.let { return it }
            
            // The ClassWrapper was not found in the cache. So we check if the jvm knows this class
            // and if so, we create a new ClassWrapper and add it to the cache.
            val systemStream = ClassLoader.getSystemResourceAsStream("$internalName.class")
                ?: javaClass.classLoader.getResourceAsStream("$internalName.class")
                // Neither the System ClassLoader nor the current ClassLoader knows this class
                ?: classLoaders.firstNotNullOfOrNull { it.getResourceAsStream("$internalName.class") }
                // Class could not be found in the JVM via the provided ClassLoaders
                ?: throw ClassNotFoundException("$name not found! Did you add all dependencies?")
            
            val wrapper = ClassWrapper("$internalName.class", ClassReader(systemStream))
            classes[wrapper.name] = wrapper
            return wrapper
        } catch (ex: Exception) {
            throw IllegalStateException("An error occurred while trying to load $name", ex)
        }
    }
    
    fun getClassWrapper(clazz: Class<*>): ClassWrapper {
        return getClass(clazz.name)
    }
    
    operator fun get(className: String) = getClass(className)
    
    operator fun get(clazz: Class<*>) = getClass(clazz.name)
    
    operator fun get(clazz: KClass<*>) = getClass(clazz.java.name)
    
    operator fun get(method: Method) = getClass(method.declaringClass.name)[method]
        ?: throw NoSuchMethodException("Method ${method.name} not found in ${method.declaringClass.name}")
    
    operator fun get(kFunction: KFunction<*>) = get(kFunction.javaMethod!!)
    
    operator fun get(constructor: Constructor<*>) = getClass(constructor.declaringClass.name)[constructor]
        ?: throw NoSuchMethodException("Constructor ${constructor.name} not found in ${constructor.declaringClass.name}")
    
    fun getInstructions(clazz: Class<*>, method: String, desc: String) =
        getClass(clazz.name).getMethod(method, desc)!!.instructions
    
    fun getInstructions(clazz: Class<*>, method: String) =
        getClass(clazz.name).getMethod(method)!!.instructions
    
    fun getInstructions(clazz: KClass<*>, method: String, desc: String) =
        getInstructions(clazz.java, method, desc)
    
    fun getInstructions(clazz: KClass<*>, method: String) =
        getInstructions(clazz.java, method)
    
    fun getInstructions(method: Method) =
        getInstructions(method.declaringClass, method.name, Type.getMethodDescriptor(method))
    
    fun getInstructions(kFunction: KFunction<*>) = 
        getInstructions(kFunction.javaMethod!!)
    
    fun getInstructions(constructor: Constructor<*>) =
        getInstructions(constructor.declaringClass, "<init>", Type.getConstructorDescriptor(constructor))
    
    fun getTree(clazz: ClassWrapper, vararg knownSubClasses: ClassWrapper) = getTree(clazz, knownSubClasses.asList())
    
    fun getTree(clazz: ClassWrapper, knownSubClasses: List<ClassWrapper> = emptyList()): InheritanceTree {
        @Suppress("LiftReturnOrAssignment")
        if (clazz !in inheritanceTrees)
            return addInheritanceTree(clazz, knownSubClasses)
        else {
            val inheritanceTree = inheritanceTrees[clazz]!!
            if (knownSubClasses.isNotEmpty()) {
                inheritanceTree.subClasses += knownSubClasses
                inheritanceTree.superClasses.forEach { superClass ->
                    // The tree has to exist because of the recursive calls in addInheritanceTree
                    val superTree = inheritanceTrees[superClass]!!
                    superTree.subClasses += knownSubClasses
                }
            }
            return inheritanceTree
        }
    }
    
    private fun addInheritanceTree(clazz: ClassWrapper, knownSubClasses: List<ClassWrapper>): InheritanceTree {
        val tree = InheritanceTree(clazz)
        tree.subClasses.addAll(knownSubClasses)
        val subClasses = if (knownSubClasses.isNotEmpty())
            knownSubClasses.toMutableList().apply { add(clazz) }
        else Collections.singletonList(clazz)
        
        clazz.superName?.let { superName ->
            val superClass = getClass(superName)
            tree.superClasses += superClass
            getTree(superClass, subClasses)
        }
        
        clazz.interfaces?.let { interfaces ->
            interfaces.forEach { i ->
                val superClass = getClass(i)
                tree.superClasses += superClass
                getTree(superClass, subClasses)
            }
        }
        
        inheritanceTrees[clazz] = tree
        return tree
    }
    
}