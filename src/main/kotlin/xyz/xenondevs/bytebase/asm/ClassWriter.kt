package xyz.xenondevs.bytebase.asm

import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.OBJECT_TYPE
import org.objectweb.asm.ClassWriter as AsmClassWriter

class ClassWriter(flags: Int = COMPUTE_FRAMES) : AsmClassWriter(flags) {
    
    override fun getCommonSuperClass(type1: String, type2: String): String {
        if (OBJECT_TYPE == type1 || OBJECT_TYPE == type2)
            return OBJECT_TYPE
        
        val type1Class = VirtualClassPath.getClass(type1)
        val type2Class = VirtualClassPath.getClass(type1)
        
        val firstCommon = findCommonSuperName(type1Class, type2Class)
        val secondCommon = findCommonSuperName(type2Class, type1Class)
        
        if (OBJECT_TYPE != firstCommon)
            return firstCommon
        if (OBJECT_TYPE != secondCommon)
            return secondCommon
        
        return getCommonSuperClass(
            type1Class.superName,
            type2Class.superName
        )
    }
    
    private fun findCommonSuperName(class1: ClassWrapper, class2: ClassWrapper): String {
        if (class1.isAssignableFrom(class2))
            return class1.name
        if (class2.isAssignableFrom(class1))
            return class2.name
        
        if (class1.isInterface() || class2.isInterface())
            return OBJECT_TYPE
        
        var new: ClassWrapper
        do {
            new = class1.superClass!!
        } while (!new.isAssignableFrom(class2))
        
        return new.name
    }
    
}