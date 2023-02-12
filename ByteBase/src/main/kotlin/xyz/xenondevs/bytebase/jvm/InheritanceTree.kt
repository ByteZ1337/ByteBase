package xyz.xenondevs.bytebase.jvm

import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class InheritanceTree(val wrapper: ClassWrapper) {
    val superClasses = HashSet<ClassWrapper>()
    val subClasses = HashSet<ClassWrapper>()
    
    /**
     * Searches all superClasses of this class for the given field and returns a [MemberReference] to it or null if it could not be found.
     */
    fun resolveFieldRef(name: String, desc: String): MemberReference? {
        val field = wrapper.getField(name, desc)
        if (field != null) return MemberReference(wrapper.name, name, desc)
        
        val superClass = superClasses.firstOrNull { it.getField(name, desc) != null }
        if (superClass != null) return MemberReference(superClass.name, name, desc, MemberType.FIELD)
        
        return null
    }
    
    /**
     * Searches all superClasses of this class for the given field and returns its owner and [FieldNode] or null if it could not be found.
     */
    fun resolveField(name: String, desc: String): Pair<String, FieldNode>? =
        resolveFieldRef(name, desc)?.let { it.owner to it.resolveField() }
    
    
    /**
     * Searches all superClasses of this class for the given method and returns a [MemberReference] to it or null if it could not be found.
     */
    fun resolveMethodRef(name: String, descriptor: String): MemberReference? {
        val method = wrapper.getMethod(name, descriptor)
        if (method != null) return MemberReference(wrapper.name, name, descriptor)
        
        val superClass = superClasses.firstNotNullOfOrNull { it.getMethod(name, descriptor) }
        if (superClass != null) return MemberReference(superClass.name, name, descriptor, MemberType.METHOD)
        
        return null
    }
    
    /**
     * Searches all superClasses of this class for the given method and returns its owner and [MethodNode] or null if it could not be found.
     */
    fun resolveMethod(name: String, descriptor: String): Pair<String, MethodNode>? =
        resolveMethodRef(name, descriptor)?.let { it.owner to it.resolveMethod() }
    
}