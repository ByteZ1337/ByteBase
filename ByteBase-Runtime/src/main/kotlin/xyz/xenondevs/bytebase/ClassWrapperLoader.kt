package xyz.xenondevs.bytebase

import xyz.xenondevs.bytebase.jvm.ClassWrapper

class ClassWrapperLoader(parent: ClassLoader) : ClassLoader(parent) {
    
    fun loadClass(clazz: ClassWrapper): Class<*> {
        val loadedClass = findLoadedClass(clazz.name)
        if (loadedClass != null)
            return loadedClass
        val bytecode = clazz.assemble()
        return defineClass(clazz.name.replace('/', '.'), bytecode, 0, bytecode.size)
    }
    
    companion object {
        val DEFAULT = ClassWrapperLoader(this::class.java.classLoader)
    }
    
}