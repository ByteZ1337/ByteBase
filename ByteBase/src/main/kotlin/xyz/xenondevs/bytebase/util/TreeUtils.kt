package xyz.xenondevs.bytebase.util

import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import xyz.xenondevs.bytebase.asm.InsnBuilder
import xyz.xenondevs.bytebase.asm.access.ReferencingAccess
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import kotlin.reflect.KClass

/**
 * Converts an [AnnotationNode] to a map ``name -> value``
 */
fun AnnotationNode.toMap(): Map<String, Any?> {
    if (values.isNullOrEmpty())
        return emptyMap()
    
    val map = mutableMapOf<String, Any?>()
    for (i in 0 until this.values.size step 2) {
        map[this.values[i].toString()] = this.values[i + 1]
    }
    return map
}

/**
 * Gets the internal name of the class (e.g. ``java/lang/Object``)
 */
val Class<*>.internalName get() = name.replace('.', '/')

/**
 * Gets the internal name of the class (e.g. ``kotlin/Any``)
 */
val KClass<*>.internalName get() = java.internalName

/**
 * Checks if the class has any annotations
 *
 * @see ClassNode.visibleAnnotations
 * @see ClassNode.invisibleAnnotations
 */
fun ClassNode.hasAnnotations() = !this.visibleAnnotations.isNullOrEmpty() || !this.invisibleAnnotations.isNullOrEmpty()

/**
 * Gets the [ReferencingAccess] for the given field
 */
val FieldNode.accessWrapper
    get() = ReferencingAccess(this::access) { access = it }

/**
 * Checks if the field has any annotations
 *
 * @see FieldNode.visibleAnnotations
 * @see FieldNode.invisibleAnnotations
 */
fun FieldNode.hasAnnotations() = !this.visibleAnnotations.isNullOrEmpty() || !this.invisibleAnnotations.isNullOrEmpty()

/**
 * Gets the class that the field is in
 *
 * @throws ClassNotFoundException if the class is not found
 * @throws IllegalStateException if an error occurs while loading the class
 */
val FieldInsnNode.ownerClass
    get() = VirtualClassPath.getClass(this.owner)

/**
 * Gets the [FieldNode] for the field
 *
 * @throws ClassNotFoundException if the owner class is not found
 * @throws IllegalStateException if an error occurs while loading the owner class
 */
val FieldInsnNode.node
    get() = this.ownerClass.getField(this.name, this.desc)

/**
 * Gets the access flags for the field
 *
 * @throws IllegalStateException if the field/owner class is not found
 * @see FieldNode.accessWrapper
 */
val FieldInsnNode.access
    get() = node?.accessWrapper ?: error("Field $owner.$name does not exist") // ValueAccess(ACC_PRIVATE)

/**
 * Checks if the method is inherited in the given class
 */
fun MethodNode.isInherited(clazz: ClassWrapper) =
    clazz.inheritanceTree.superClasses.any { this in it }

/**
 * Gets the [ReferencingAccess] for the given method
 */
val MethodNode.accessWrapper
    get() = ReferencingAccess(this::access) { access = it }

/**
 * Checks if the method has any annotations
 *
 * @see MethodNode.visibleAnnotations
 * @see MethodNode.invisibleAnnotations
 */
fun MethodNode.hasAnnotations() = !this.visibleAnnotations.isNullOrEmpty() || !this.invisibleAnnotations.isNullOrEmpty()

/**
 * Creates a new [MethodNode] with the given instructions
 */
fun MethodNode(access: Int, name: String, descriptor: String, instructions: InsnBuilder.() -> Unit): MethodNode {
    val method = MethodNode(access, name, descriptor, null, null)
    method.instructions = buildInsnList(instructions)
    return method
}

/**
 * Gets the class that the method is in
 *
 * @throws ClassNotFoundException if the class is not found
 * @throws IllegalStateException if an error occurs while loading the class
 */
val MethodInsnNode.ownerClass
    get() = VirtualClassPath.getClass(this.owner)

/**
 * Gets the [MethodNode] for the method
 *
 * @throws ClassNotFoundException if the owner class is not found
 * @throws IllegalStateException if an error occurs while loading the owner class
 */
val MethodInsnNode.node
    get() = this.ownerClass.getMethod(this.name, this.desc)

/**
 * Gets the access flags for the method
 *
 * @throws IllegalStateException if the method/owner class is not found
 * @see MethodNode.accessWrapper
 */
val MethodInsnNode.access
    get() = node?.accessWrapper ?: error("Method $owner.$name does not exist") // ValueAccess(ACC_PRIVATE)

fun InsnList.copy() = MethodNode().also(this::accept).instructions!!