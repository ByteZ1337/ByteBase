package xyz.xenondevs.bytebase.patch.patcher.kotlin

import it.unimi.dsi.fastutil.ints.Int2LongMap
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import sun.misc.Unsafe
import xyz.xenondevs.bytebase.patch.util.weak.IntWeakIdentityHashMap

class FieldHolder {
    
    private val unsafe = Unsafe.getUnsafe()
    
    val primitiveFields = Int2ObjectOpenHashMap<Int2LongMap>() // identityHashCode -> (fieldName hash -> address)
    val objectFields = Int2ObjectOpenHashMap<Int2ObjectMap<Any?>>() // identityHashCode -> (fieldName hash -> object)
    
    val identityHashCodes = IntWeakIdentityHashMap<Any> { id -> // Object -> identityHashCode
        primitiveFields.remove(id)?.let { map -> map.values.forEach { unsafe.freeMemory(it) } }
        objectFields.remove(id)
    }
    
    //<editor-fold desc="Getter methods" defaultstate="collapsed">
    
    fun getObjectField(obj: Any, fieldHash: Int): Any? {
        val code = System.identityHashCode(obj)
        return objectFields[code]?.get(fieldHash)
    }
    
    private fun getAddress(obj: Any, fieldHash: Int): Long? {
        val code = System.identityHashCode(obj)
        return primitiveFields[code]?.get(fieldHash)
    }
    
    fun getBooleanField(obj: Any, fieldHash: Int): Boolean {
        val address = getAddress(obj, fieldHash) ?: return false
        return unsafe.getByte(address) != 0.toByte()
    }
    
    fun getByteField(obj: Any, fieldHash: Int): Byte {
        val address = getAddress(obj, fieldHash) ?: return 0
        return unsafe.getByte(address)
    }
    
    fun getCharField(obj: Any, fieldHash: Int): Char {
        val address = getAddress(obj, fieldHash) ?: return 0.toChar()
        return unsafe.getChar(address)
    }
    
    fun getDoubleField(obj: Any, fieldHash: Int): Double {
        val address = getAddress(obj, fieldHash) ?: return 0.0
        return unsafe.getDouble(address)
    }
    
    fun getFloatField(obj: Any, fieldHash: Int): Float {
        val address = getAddress(obj, fieldHash) ?: return 0.0f
        return unsafe.getFloat(address)
    }
    
    fun getIntField(obj: Any, fieldHash: Int): Int {
        val address = getAddress(obj, fieldHash) ?: return 0
        return unsafe.getInt(address)
    }
    
    fun getLongField(obj: Any, fieldHash: Int): Long {
        val address = getAddress(obj, fieldHash) ?: return 0
        return unsafe.getLong(address)
    }
    
    fun getShortField(obj: Any, fieldHash: Int): Short {
        val address = getAddress(obj, fieldHash) ?: return 0
        return unsafe.getShort(address)
    }
    
    //</editor-fold>
    
    //<editor-fold desc="Setter methods" defaultstate="collapsed">
    
    fun setObjectField(obj: Any, fieldHash: Int, value: Any?) {
        val code = System.identityHashCode(obj)
        identityHashCodes[obj] = code
        val fieldMap = objectFields.getOrPut(code) { Int2ObjectOpenHashMap() }
        fieldMap[fieldHash] = value
    }
    
    private fun allocateAddress(obj: Any, fieldHash: Int, bytes: Int): Long {
        val code = System.identityHashCode(obj)
        identityHashCodes[obj] = code
        val fieldMap = primitiveFields.getOrPut(code) { Int2LongOpenHashMap() }
        return fieldMap.getOrPut(fieldHash) { unsafe.allocateMemory(bytes.toLong()) }
    }
    
    fun setBooleanField(obj: Any, fieldHash: Int, value: Boolean) {
        val address = allocateAddress(obj, fieldHash, Byte.SIZE_BYTES)
        unsafe.putByte(address, if (value) 1 else 0)
    }
    
    fun setByteField(obj: Any, fieldHash: Int, value: Byte) {
        val address = allocateAddress(obj, fieldHash, Byte.SIZE_BYTES)
        unsafe.putByte(address, value)
    }
    
    fun setCharField(obj: Any, fieldHash: Int, value: Char) {
        val address = allocateAddress(obj, fieldHash, Char.SIZE_BYTES)
        unsafe.putChar(address, value)
    }
    
    fun setDoubleField(obj: Any, fieldHash: Int, value: Double) {
        val address = allocateAddress(obj, fieldHash, Double.SIZE_BYTES)
        unsafe.putDouble(address, value)
    }
    
    fun setFloatField(obj: Any, fieldHash: Int, value: Float) {
        val address = allocateAddress(obj, fieldHash, Float.SIZE_BYTES)
        unsafe.putFloat(address, value)
    }
    
    fun setIntField(obj: Any, fieldHash: Int, value: Int) {
        val address = allocateAddress(obj, fieldHash, Int.SIZE_BYTES)
        unsafe.putInt(address, value)
    }
    
    fun setLongField(obj: Any, fieldHash: Int, value: Long) {
        val address = allocateAddress(obj, fieldHash, Long.SIZE_BYTES)
        unsafe.putLong(address, value)
    }
    
    fun setShortField(obj: Any, fieldHash: Int, value: Short) {
        val address = allocateAddress(obj, fieldHash, Short.SIZE_BYTES)
        unsafe.putShort(address, value)
    }
    
    //</editor-fold>
    
}