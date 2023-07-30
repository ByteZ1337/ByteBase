package xyz.xenondevs.bytebase.patch.util

import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import java.security.ProtectionDomain
import kotlin.reflect.KClass

val DEFINE_METHOD = ClassLoader::class.java.getDeclaredMethod("defineClass", String::class.java, ByteArray::class.java, Int::class.java, Int::class.java, ProtectionDomain::class.java).apply { isAccessible = true }

internal fun ClassLoader.defineClass(name: String, bytecode: ByteArray, protectionDomain: ProtectionDomain? = null) =
    DEFINE_METHOD.invoke(this, name, bytecode, 0, bytecode.size, protectionDomain) as Class<*>

internal fun ClassLoader.defineClass(clazz: Class<*>) =
    defineClass(clazz.name, VirtualClassPath[clazz].assemble(true), clazz.protectionDomain)

internal fun ClassLoader.defineClass(clazz: KClass<*>) =
    defineClass(clazz.java)

internal fun ClassLoader.defineClass(classWrapper: ClassWrapper) =
    defineClass(classWrapper.name.replace('/', '.'), classWrapper.assemble(true), null)