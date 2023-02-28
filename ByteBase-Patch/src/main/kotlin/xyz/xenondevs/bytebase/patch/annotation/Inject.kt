package xyz.xenondevs.bytebase.patch.annotation

import xyz.xenondevs.bytebase.patch.annotation.target.At

annotation class Inject(val target: String, val at: At = At("HEAD"), val keepFallthroughReturn: Boolean = false)
