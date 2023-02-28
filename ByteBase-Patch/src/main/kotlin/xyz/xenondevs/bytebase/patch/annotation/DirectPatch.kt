package xyz.xenondevs.bytebase.patch.annotation

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class DirectPatch(val target: KClass<*>, val priority: UInt = 100u)