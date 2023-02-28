package xyz.xenondevs.bytebase.patch.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Patch(val target: String, val priority: UInt = 100u)
