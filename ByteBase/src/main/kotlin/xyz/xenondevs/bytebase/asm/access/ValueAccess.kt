package xyz.xenondevs.bytebase.asm.access

import org.objectweb.asm.Opcodes.*
import xyz.xenondevs.bytebase.util.Int32
import xyz.xenondevs.bytebase.util.hasMask

/**
 * [Access] implementation for a constant value.
 */
class ValueAccess(private val access: Int32) : Access {
    
    /**
     * Checks if the access flag is public.
     */
    override fun isPublic() = access.hasMask(ACC_PUBLIC)
    
    /**
     * Checks if the access flag is private.
     */
    override fun isPrivate() = access.hasMask(ACC_PRIVATE)
    
    /**
     * Checks if the access flag is protected.
     */
    override fun isProtected() = access.hasMask(ACC_PROTECTED)
    
    /**
     * Checks if the access flag is static.
     */
    override fun isStatic() = access.hasMask(ACC_STATIC)
    
    /**
     * Checks if the access flag is final.
     */
    override fun isFinal() = access.hasMask(ACC_FINAL)
    
    /**
     * Checks if the access flag is super.
     */
    override fun isSuper() = access.hasMask(ACC_SUPER)
    
    /**
     * Checks if the access flag is synchronized.
     */
    override fun isSynchronized() = access.hasMask(ACC_SYNCHRONIZED)
    
    /**
     * Checks if the access flag is open.
     */
    override fun isOpen() = access.hasMask(ACC_OPEN)
    
    /**
     * Checks if the access flag is transitive.
     */
    override fun isTransitive() = access.hasMask(ACC_TRANSITIVE)
    
    /**
     * Checks if the access flag is volatile.
     */
    override fun isVolatile() = access.hasMask(ACC_VOLATILE)
    
    /**
     * Checks if the access flag is bridge.
     */
    override fun isBridge() = access.hasMask(ACC_BRIDGE)
    
    /**
     * Checks if the access flag is static phase.
     */
    override fun isStaticPhase() = access.hasMask(ACC_STATIC_PHASE)
    
    /**
     * Checks if the access flag is varargs.
     */
    override fun isVarargs() = access.hasMask(ACC_VARARGS)
    
    /**
     * Checks if the access flag is transient.
     */
    override fun isTransient() = access.hasMask(ACC_TRANSIENT)
    
    /**
     * Checks if the access flag is native.
     */
    override fun isNative() = access.hasMask(ACC_NATIVE)
    
    /**
     * Checks if the access flag is interface.
     */
    override fun isInterface() = access.hasMask(ACC_INTERFACE)
    
    /**
     * Checks if the access flag is abstract.
     */
    override fun isAbstract() = access.hasMask(ACC_ABSTRACT)
    
    /**
     * Checks if the access flag is strict.
     */
    override fun isStrict() = access.hasMask(ACC_STRICT)
    
    /**
     * Checks if the access flag is synthetic.
     */
    override fun isSynthetic() = access.hasMask(ACC_SYNTHETIC)
    
    /**
     * Checks if the access flag is annotation.
     */
    override fun isAnnotation() = access.hasMask(ACC_ANNOTATION)
    
    /**
     * Checks if the access flag is enum.
     */
    override fun isEnum() = access.hasMask(ACC_ENUM)
    
    /**
     * Checks if the access flag is mandated.
     */
    override fun isMandated() = access.hasMask(ACC_MANDATED)
    
    /**
     * Checks if the access flag is module.
     */
    override fun isModule() = access.hasMask(ACC_MODULE)
    
    /**
     * Checks if this access has the given flags.
     */
    override fun hasFlags(vararg flags: Int32): Boolean {
        val mask = flags.reduce { i1, i2 -> i1 or i2 }
        return access.hasMask(mask)
    }
    
    /**
     * Checks if this access doesn't have the given flags.
     */
    override fun none(vararg flags: Int32) = flags.none(access::hasMask)
    
    /**
     * Checks if this access is neither an enum nor an interface and is public.
     */
    fun isPublicClass() = !isEnum() && !isInterface() && isPublic()
}