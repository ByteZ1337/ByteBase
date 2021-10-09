package xyz.xenondevs.bytebase.jvm

import org.objectweb.asm.ClassReader

object VirtualClassPath {
    
    val classes = HashMap<String, ClassWrapper>()
    val inheritanceTrees = HashMap<ClassWrapper, InheritanceTree>()
    
    fun loadJar(jar: JavaArchive) {
        jar.classes.forEach {
            classes[it.name] = it
            if (it !in inheritanceTrees)
                addInheritanceTree(it, emptyList())
        }
        println(classes.size)
    }
    
    fun getClass(name: String): ClassWrapper {
        try {
            classes[name]?.let { return it }
            
            // The ClassWrapper was not found in the cache. So we check if the jvm knows this class
            val wrapper = ClassWrapper("${name.replace('.', '/')}.class").also {
                ClassReader(name).accept(it, ClassReader.SKIP_FRAMES)
            }
            classes[wrapper.name] = wrapper
            return wrapper
        } catch (ex: Exception) {
            throw IllegalStateException("$name not found! Did you add all dependencies?", ex)
        }
    }
    
    fun getTree(clazz: ClassWrapper, vararg knownSubClasses: ClassWrapper) = getTree(clazz, knownSubClasses.asList())
    
    fun getTree(clazz: ClassWrapper, knownSubClasses: List<ClassWrapper> = emptyList()): InheritanceTree {
        @Suppress("LiftReturnOrAssignment")
        if (clazz !in inheritanceTrees)
            return addInheritanceTree(clazz, knownSubClasses)
        else {
            val inheritanceTree = inheritanceTrees[clazz]!!
            inheritanceTree.subClasses += knownSubClasses
            inheritanceTree.superClasses.forEach { superClass ->
                // The tree has to exist because of the recursive calls in addInheritanceTree
                val superTree = inheritanceTrees[superClass]!!
                superTree.subClasses += knownSubClasses
            }
            return inheritanceTree
        }
    }
    
    private fun addInheritanceTree(clazz: ClassWrapper, knownSubClasses: List<ClassWrapper>): InheritanceTree {
        val tree = InheritanceTree(clazz)
        tree.subClasses.addAll(knownSubClasses)
        val subClasses by lazy {
            if (knownSubClasses.isNotEmpty())
                return@lazy knownSubClasses.toMutableList().apply { add(clazz) }
            return@lazy listOf(clazz)
        }
        
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