package xyz.xenondevs.bytebase.patch.util

import java.io.Writer

class SingleStringWriter  : Writer() {

    var string = ""
    
    override fun write(cbuf: CharArray, off: Int, len: Int) {
        string = String(cbuf, off, len).trim()
    }
    
    override fun close() {
    }
    
    override fun flush() {
    }
}