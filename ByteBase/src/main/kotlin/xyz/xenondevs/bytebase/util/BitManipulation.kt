package xyz.xenondevs.bytebase.util

/**
 * A 16-bit integer
 */
typealias Int16 = Short

/**
 * A 32-bit integer
 */
typealias Int32 = Int

/**
 * A 64-bit integer
 */
typealias Int64 = Long

/**
 * Returns whether the given [mask] is present in the given [Int32].
 */
fun Int32.hasMask(mask: Int32) = this and mask == mask

/**
 * Sets the given [mask] to the given [value] in the given [Int32].
 */
fun Int32.setMask(mask: Int32, value: Boolean) = if (value) this or mask else this and mask.inv()

/**
 * Returns whether the given [mask] is present in the given [Int64].
 */
fun Int64.hasMask(mask: Int64) = this and mask == mask

/**
 * Sets the given [mask] to the given [value] in the given [Int64].
 */
fun Int64.setMask(mask: Int64, value: Boolean) = if (value) this or mask else this and mask.inv()