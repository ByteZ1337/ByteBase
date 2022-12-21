package xyz.xenondevs.bytebase.asm.access

import org.objectweb.asm.Opcodes.*
import xyz.xenondevs.bytebase.util.Int32
import xyz.xenondevs.bytebase.util.hasMask
import xyz.xenondevs.bytebase.util.setMask

/**
 * An [Access] implementation that's able to set access flags as well by using [get] and [set] lambdas.
 *
 * @param get The getter lambda
 * @param set The setter lambda
 */
class ReferencingAccess(val get: () -> Int32, set: (Int32) -> Unit) : Access {
    
    private val set: (Int32) -> ReferencingAccess = { set(it); this }
    
    /**
     * Checks if the access flag is public.
     */
    override fun isPublic() = get().hasMask(ACC_PUBLIC)
    
    /**
     * Sets the public access flag to the given [value].
     */
    fun setPublic(value: Boolean = true) = set(get().setMask(ACC_PUBLIC, value))
    
    /**
     * Checks if the access flag is private.
     */
    override fun isPrivate() = get().hasMask(ACC_PRIVATE)
    
    /**
     * Sets the private access flag to the given [value].
     */
    fun setPrivate(value: Boolean = true) = set(get().setMask(ACC_PRIVATE, value))
    
    /**
     * Checks if the access flag is protected.
     */
    override fun isProtected() = get().hasMask(ACC_PROTECTED)
    
    /**
     * Sets the protected access flag to the given [value].
     */
    fun setProtected(value: Boolean = true) = set(get().setMask(ACC_PROTECTED, value))
    
    /**
     * Checks if the access flag is static.
     */
    override fun isStatic() = get().hasMask(ACC_STATIC)
    
    /**
     * Sets the static access flag to the given [value].
     */
    fun setStatic(value: Boolean = true) = set(get().setMask(ACC_STATIC, value))
    
    /**
     * Checks if the access flag is final.
     */
    override fun isFinal() = get().hasMask(ACC_FINAL)
    
    /**
     * Sets the final access flag to the given [value].
     */
    fun setFinal(value: Boolean = true) = set(get().setMask(ACC_FINAL, value))
    
    /**
     * Checks if the access flag is super.
     */
    override fun isSuper() = get().hasMask(ACC_SUPER)
    
    /**
     * Sets the super access flag to the given [value].
     */
    fun setSuper(value: Boolean = true) = set(get().setMask(ACC_SUPER, value))
    
    /**
     * Checks if the access flag is synchronized.
     */
    override fun isSynchronized() = get().hasMask(ACC_SYNCHRONIZED)
    
    /**
     * Sets the synchronized access flag to the given [value].
     */
    fun setSynchronized(value: Boolean = true) = set(get().setMask(ACC_SYNCHRONIZED, value))
    
    /**
     * Checks if the access flag is open.
     */
    override fun isOpen() = get().hasMask(ACC_OPEN)
    
    /**
     * Sets the open access flag to the given [value].
     */
    fun setOpen(value: Boolean = true) = set(get().setMask(ACC_OPEN, value))
    
    /**
     * Checks if the access flag is transitive.
     */
    override fun isTransitive() = get().hasMask(ACC_TRANSITIVE)
    
    /**
     * Sets the transitive access flag to the given [value].
     */
    fun setTransitive(value: Boolean = true) = set(get().setMask(ACC_TRANSITIVE, value))
    
    /**
     * Checks if the access flag is volatile.
     */
    override fun isVolatile() = get().hasMask(ACC_VOLATILE)
    
    /**
     * Sets the volatile access flag to the given [value].
     */
    fun setVolatile(value: Boolean = true) = set(get().setMask(ACC_VOLATILE, value))
    
    /**
     * Checks if the access flag is bridge.
     */
    override fun isBridge() = get().hasMask(ACC_BRIDGE)
    
    /**
     * Sets the bridge access flag to the given [value].
     */
    fun setBridge(value: Boolean = true) = set(get().setMask(ACC_BRIDGE, value))
    
    /**
     * Checks if the access flag is static phase.
     */
    override fun isStaticPhase() = get().hasMask(ACC_STATIC_PHASE)
    
    /**
     * Sets the static phase access flag to the given [value].
     */
    fun setStaticPhase(value: Boolean = true) = set(get().setMask(ACC_STATIC_PHASE, value))
    
    /**
     * Checks if the access flag is varargs.
     */
    override fun isVarargs() = get().hasMask(ACC_VARARGS)
    
    /**
     * Sets the varargs access flag to the given [value].
     */
    fun setVarargs(value: Boolean = true) = set(get().setMask(ACC_VARARGS, value))
    
    /**
     * Checks if the access flag is transient.
     */
    override fun isTransient() = get().hasMask(ACC_TRANSIENT)
    
    /**
     * Sets the transient access flag to the given [value].
     */
    fun setTransient(value: Boolean = true) = set(get().setMask(ACC_TRANSIENT, value))
    
    /**
     * Checks if the access flag is native.
     */
    override fun isNative() = get().hasMask(ACC_NATIVE)
    
    /**
     * Sets the native access flag to the given [value].
     */
    fun setNative(value: Boolean = true) = set(get().setMask(ACC_NATIVE, value))
    
    /**
     * Checks if the access flag is interface.
     */
    override fun isInterface() = get().hasMask(ACC_INTERFACE)
    
    /**
     * Sets the interface access flag to the given [value].
     */
    fun setInterface(value: Boolean = true) = set(get().setMask(ACC_INTERFACE, value))
    
    /**
     * Checks if the access flag is abstract.
     */
    override fun isAbstract() = get().hasMask(ACC_ABSTRACT)
    
    /**
     * Sets the abstract access flag to the given [value].
     */
    fun setAbstract(value: Boolean = true) = set(get().setMask(ACC_ABSTRACT, value))
    
    /**
     * Checks if the access flag is strict.
     */
    override fun isStrict() = get().hasMask(ACC_STRICT)
    
    /**
     * Sets the strict access flag to the given [value].
     */
    fun setStrict(value: Boolean = true) = set(get().setMask(ACC_STRICT, value))
    
    /**
     * Checks if the access flag is synthetic.
     */
    override fun isSynthetic() = get().hasMask(ACC_SYNTHETIC)
    
    /**
     * Sets the synthetic access flag to the given [value].
     */
    fun setSynthetic(value: Boolean = true) = set(get().setMask(ACC_SYNTHETIC, value))
    
    /**
     * Checks if the access flag is annotation.
     */
    override fun isAnnotation() = get().hasMask(ACC_ANNOTATION)
    
    /**
     * Sets the annotation access flag to the given [value].
     */
    fun setAnnotation(value: Boolean = true) = set(get().setMask(ACC_ANNOTATION, value))
    
    /**
     * Checks if the access flag is enum.
     */
    override fun isEnum() = get().hasMask(ACC_ENUM)
    
    /**
     * Sets the enum access flag to the given [value].
     */
    fun setEnum(value: Boolean = true) = set(get().setMask(ACC_ENUM, value))
    
    /**
     * Checks if the access flag is mandated.
     */
    override fun isMandated() = get().hasMask(ACC_MANDATED)
    
    /**
     * Sets the mandated access flag to the given [value].
     */
    fun setMandated(value: Boolean = true) = set(get().setMask(ACC_MANDATED, value))
    
    /**
     * Checks if the access flag is module.
     */
    override fun isModule() = get().hasMask(ACC_MODULE)
    
    /**
     * Sets the module access flag to the given [value].
     */
    fun setModule(value: Boolean = true) = set(get().setMask(ACC_MODULE, value))
    
    /**
     * Checks if this access has the given flags.
     */
    override fun hasFlags(vararg flags: Int32): Boolean {
        val mask = flags.reduce { i1, i2 -> i1 or i2 }
        return get().hasMask(mask)
    }
    
    /**
     * Sets the given [flags] to [value].
     */
    fun setFlags(vararg flags: Int32, value: Boolean = true): ReferencingAccess {
        val mask = flags.reduce { i1, i2 -> i1 or i2 }
        return set(get().setMask(mask, value))
    }
    
    /**
     * Checks if this access doesn't have the given flags.
     */
    override fun none(vararg flags: Int32): Boolean {
        val access = get()
        return flags.none(access::hasMask)
    }
    
    /**
     * Checks if this access is neither an enum nor an interface and is public.
     */
    fun isPublicClass() = !isEnum() && !isInterface() && isPublic()
    
}