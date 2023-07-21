package xyz.xenondevs.bytebase.patch.annotation

import xyz.xenondevs.bytebase.patch.PatchMode

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Patch(val target: String, val priority: UInt = 100u, val patchMode: PatchMode = PatchMode.AUTOMATIC)
