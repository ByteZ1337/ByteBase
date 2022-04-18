package xyz.xenondevs.bytebase

import net.bytebuddy.agent.ByteBuddyAgent
import org.objectweb.asm.tree.InsnList
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.internalName
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation

val INSTRUMENTATION: Instrumentation by lazy { ByteBuddyAgent.install() }

fun Class<*>.redefine(transformer: ClassWrapper.() -> Unit) {
    val wrapper = VirtualClassPath.getClass(this.internalName)
    wrapper.transformer()
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}

fun Class<*>.setInstructions(method: String, desc: String, instructions: InsnList) {
    val wrapper = VirtualClassPath.getClass(this.internalName)
    wrapper.getMethod(method, desc)!!.instructions = instructions
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}

fun Class<*>.setInstructions(method: String, instructions: InsnList) {
    val wrapper = VirtualClassPath.getClass(this.internalName)
    wrapper.getMethod(method)!!.instructions = instructions
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}

fun Class<*>.insertInstructions(method: String, desc: String, instructions: InsnList) {
    val wrapper = VirtualClassPath.getClass(this.internalName)
    wrapper.getMethod(method, desc)!!.instructions.insert(instructions)
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}

fun Class<*>.insertInstructions(method: String, instructions: InsnList) {
    val wrapper = VirtualClassPath.getClass(this.internalName)
    wrapper.getMethod(method)!!.instructions.insert(instructions)
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}

fun ClassWrapper.load(): Class<*> {
    return ClassWrapperLoader.DEFAULT.loadClass(this)
}