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
    val desc: String
) {
    
    /**
     * Attempts to resolve this [MemberReference] to a [FieldNode].
     */
    fun resolveField(): FieldNode = VirtualClassPath[owner].getField(name, desc)
        ?: throw NoSuchFieldException("Could not find field $name with descriptor $desc in class $owner")
    
    /**
     * Attempts to resolve this [MemberReference] to a [MethodNode].
     */
    fun resolveMethod(): MethodNode = VirtualClassPath[owner].getMethod(name, desc)
        ?: throw NoSuchMethodException("Could not find method $name with descriptor $desc in class $owner")
    
    override fun toString(): String {
        return "$owner.$name$desc"
    }
    
}