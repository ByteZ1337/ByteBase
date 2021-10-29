package xyz.xenondevs.bytebase.jvm

class ClassWrapperLoader(parent: ClassLoader) : ClassLoader(parent) {
    fun loadClass(clazz: ClassWrapper): Class<*> {
        val loadedClass = findLoadedClass(clazz.name)
        if(loadedClass != null)
            return loadedClass
        val bytecode = clazz.assemble()
        return defineClass(clazz.name, bytecode, 0, bytecode.size)
    }
}