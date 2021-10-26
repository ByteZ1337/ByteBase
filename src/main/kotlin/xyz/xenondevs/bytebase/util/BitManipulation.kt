package xyz.xenondevs.bytebase.util

fun Int.hasMask(mask: Int) = this and mask == mask

fun Int.setMask(mask: Int, value: Boolean) = if (value) this or mask else this and mask.inv()

fun Long.hasMask(mask: Long) = this and mask == mask

fun Long.setMask(mask: Long, value: Boolean) = if (value) this or mask else this and mask.inv()