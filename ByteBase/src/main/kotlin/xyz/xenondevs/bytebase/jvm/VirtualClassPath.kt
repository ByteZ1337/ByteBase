package xyz.xenondevs.bytebase.jvm

import org.objectweb.asm.ClassReader
import java.util.*

object VirtualClassPath {
    
    val classes = HashMap<String, ClassWrapper>()
    val inheritanceTrees = HashMap<ClassWrapper, InheritanceTree>()
    
    fun loadJar(jar: JavaArchive) {
        jar.classes.forEach {
            classes[it.name] = it
            if (it !in inheritanceTrees)
                addInheritanceTree(it, emptyList())
        }
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