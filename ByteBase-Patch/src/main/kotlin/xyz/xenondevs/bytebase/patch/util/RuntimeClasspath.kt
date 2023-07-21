package xyz.xenondevs.bytebase.patch.util

import xyz.xenondevs.bytebase.INSTRUMENTATION

internal object RuntimeClasspath {
    
    var loadedClasses = getRuntimeClasses()
    
    fun isClassLoaded(name: String) = name in loadedClasses
    
    private fun getRuntimeClasses(): Map<String, Class<*>> {
        return INSTRUMENTATION.allLoadedClasses.asSequence().map { it.name.replace('.', '/') to it }.toMap()
    }
    
    fun refresh() {
        loadedClasses = getRuntimeClasses()
    }
    
}