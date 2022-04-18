package xyz.xenondevs.bytebase.util

import org.objectweb.asm.tree.*
import xyz.xenondevs.bytebase.asm.InsnBuilder
import xyz.xenondevs.bytebase.asm.access.ReferencingAccess
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import kotlin.reflect.KClass

fun AnnotationNode.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    for (i in 0 until this.values.size step 2) {
        map[this.values[i].toString()] = this.values[i + 1]
    }
    return map
}

val Class<*>.internalName get() = name.replace('.', '/')

val KClass<*>.internalName get() = java.internalName

fun ClassNode.hasAnnotations() = !this.visibleAnnotations.isNullOrEmpty() || !this.invisibleAnnotations.isNullOrEmpty()

val FieldNode.accessWrapper
    get() = ReferencingAccess(this::access) { access = it }

fun FieldNode.hasAnnotations() = !this.visibleAnnotations.isNullOrEmpty() || !this.invisibleAnnotations.isNullOrEmpty()

val FieldInsnNode.ownerClass
    get() = VirtualClassPath.getClass(this.owner)

val FieldInsnNode.node
    get() = this.ownerClass.getField(this.name, this.desc)

val FieldInsnNode.access
    get() = node?.accessWrapper ?: error("Field $owner.$name does not exist") // ValueAccess(ACC_PRIVATE)

fun MethodNode.isInherited(clazz: ClassWrapper) =
    clazz.inheritanceTree.superClasses.any { this in it }

val MethodNode.accessWrapper
    get() = ReferencingAccess(this::access) { access = it }

fun MethodNode.hasAnnotations() = !this.visibleAnnotations.isNullOrEmpty() || !this.invisibleAnnotations.isNullOrEmpty()

fun MethodNode(access: Int, name: String, descriptor: String, instructions: InsnBuilder.() -> Unit): MethodNode {
    val method = MethodNode(access, name, descriptor, null, null)
    method.instructions = buildInsnList(instructions)
    return method
}

val MethodInsnNode.ownerClass
    get() = VirtualClassPath.getClass(this.owner)

val MethodInsnNode.node
    get() = this.ownerClass.getMethod(this.name, this.desc)

val MethodInsnNode.access
    get() = node?.accessWrapper ?: error("Method $owner.$name does not exist") // ValueAccess(ACC_PRIVATE)