package xyz.xenondevs.bytebase.asm

import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.VirtualClassPath

/**
 * internal name for Object. Is always "java/lang/Object"
 */
const val OBJECT_TYPE = "java/lang/Object"

/**
 * The [ClassWrapper] of [Object]
 */
val OBJECT_CLASS by lazy { VirtualClassPath.getClass(OBJECT_TYPE) }