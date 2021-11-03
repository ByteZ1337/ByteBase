package xyz.xenondevs.bytebase

import net.bytebuddy.agent.ByteBuddyAgent
import org.objectweb.asm.tree.InsnList
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.internalName
import java.lang.instrument.ClassDefinition
import java.lang.instrument.Instrumentation

typealias ClassTransformer = ClassWrapper.() -> Unit
val INSTRUMENTATION: Instrumentation by lazy { ByteBuddyAgent.install() }

fun Class<*>.redefine(transformer: ClassTransformer) {
    val wrapper = VirtualClassPath.getClass(this.internalName)
    wrapper.transformer()
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}

fun Class<*>.setInstructions(method: String, instructions: InsnList) {
    val wrapper = VirtualClassPath.getClass(this.internalName)
    wrapper.getMethod(method)!!.instructions = instructions
    INSTRUMENTATION.redefineClasses(ClassDefinition(this, wrapper.assemble()))
}