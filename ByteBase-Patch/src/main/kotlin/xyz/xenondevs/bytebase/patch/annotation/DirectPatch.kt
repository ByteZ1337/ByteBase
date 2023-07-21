package xyz.xenondevs.bytebase.patch.annotation

import xyz.xenondevs.bytebase.patch.PatchMode
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class DirectPatch(
    val target: KClass<*>,
    val priority: UInt = 100u,
    val patchMode: PatchMode = PatchMode.AUTOMATIC
)