package xyz.xenondevs.bytebase.jvm

class Resource(var name: String, var content: ByteArray) {
    
    val originalName = name
    
    constructor(name: String) : this(name, ByteArray(0))
    
    override fun toString() = content.decodeToString()
    
    fun applyBlock(consumer: (ByteArray) -> ByteArray) {
        content = consumer(content)
    }
    
    fun applyConsumer(block: (ByteArray) -> Unit) = block(content)
    
}