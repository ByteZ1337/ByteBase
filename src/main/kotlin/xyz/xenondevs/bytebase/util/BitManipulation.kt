package xyz.xenondevs.bytebase.util

typealias Int16 = Short
typealias Int32 = Int
typealias Int64 = Long

fun Int32.hasMask(mask: Int32) = this and mask == mask

fun Int32.setMask(mask: Int32, value: Boolean) = if (value) this or mask else this and mask.inv()

fun Int64.hasMask(mask: Int64) = this and mask == mask

fun Int64.setMask(mask: Int64, value: Boolean) = if (value) this or mask else this and mask.inv()