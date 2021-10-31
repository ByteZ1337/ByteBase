package xyz.xenondevs.bytebase.asm

import xyz.xenondevs.bytebase.jvm.VirtualClassPath

const val OBJECT_TYPE = "java/lang/Object"
val OBJECT_CLASS by lazy { VirtualClassPath.getClass(OBJECT_TYPE) }