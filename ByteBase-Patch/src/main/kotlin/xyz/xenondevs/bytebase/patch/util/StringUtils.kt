package xyz.xenondevs.bytebase.patch.util

import java.io.Writer

internal object StringUtils {
    
    fun String.startsWithAny(vararg strings: String) = strings.any { startsWith(it) }
    
    fun String.endsWithAny(vararg strings: String) = strings.any { endsWith(it) }
    
    fun String.possessive() = if (endsWithAny("s", "x", "z")) "$this'" else "$this's"
    
    fun String.between(start: Char, end: Char): String {
        val startIndex = indexOf(start) + 1
        val endIndex = indexOf(end, startIndex)
        check(startIndex != -1 && endIndex != -1) { "Could not find character $start or $end in $this" }
        return substring(startIndex, endIndex)
    }
    
}

class SingleStringWriter : Writer() {
    
    var string = ""
    
    override fun write(cbuf: CharArray, off: Int, len: Int) {
        string = String(cbuf, off, len).trim()
    }
    
    override fun close() {
    }
    
    override fun flush() {
    }
}