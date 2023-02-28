package xyz.xenondevs.bytebase.patch

import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import kotlin.reflect.KClass

interface PostProcessor {
    
    fun handleClassPatched(clazz: KClass<*>, newWrapper: ClassWrapper) {}
    
    fun handleMethodPatched(clazz: KClass<*>, method: MethodNode) {}
    
    fun handleMethodAdded(clazz: KClass<*>, method: MethodNode) {}
    
    fun handleFieldAdded(clazz: KClass<*>, field: FieldNode) {}
    
    fun handleFinished() {}
    
}