package xyz.xenondevs.bytebase.jvm

import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

/**
 * A simple wrapper for the common fields `owner`, `name` and `desc` of [MethodInsnNode] and [FieldInsnNode].
 */
data class MemberReference(
    val owner: String,
    val name: String,
    val desc: String,
    val type: MemberType? = null
) {
    
    /**
     * Attempts to resolve this [MemberReference] to a [FieldNode].
     */
    fun resolveField(): FieldNode {
        if (type == MemberType.METHOD)
            throw IllegalStateException("MemberReference $this is not a field")
        return VirtualClassPath[owner].getField(name, desc)
            ?: throw NoSuchFieldException("Could not find field $name with descriptor $desc in class $owner")
    }
    
    /**
     * Attempts to resolve this [MemberReference] to a [MethodNode].
     */
    fun resolveMethod(): MethodNode {
        if (type == MemberType.FIELD)
            throw IllegalStateException("MemberReference $this is not a method")
        return VirtualClassPath[owner].getMethod(name, desc)
            ?: throw NoSuchMethodException("Could not find method $name with descriptor $desc in class $owner")
    }
    
    /**
     * Attempts to resolve the owner of this [MemberReference] to a [ClassWrapper].
     */
    fun resolveOwner() = VirtualClassPath[owner]
    
    override fun toString() =
        if (type == null || type == MemberType.METHOD) {
            "$owner.$name$desc"
        } else {
            "$owner.$name.$desc"
        }
    
}

enum class MemberType {
    FIELD,
    METHOD
}