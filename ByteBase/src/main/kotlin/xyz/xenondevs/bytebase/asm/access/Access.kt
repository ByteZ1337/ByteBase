package xyz.xenondevs.bytebase.asm.access

import xyz.xenondevs.bytebase.util.Int32

/**
 * The access flags of a class, field, or method. Used to ease checking of access flags.
 *
 * @see ReferencingAccess
 * @see ValueAccess
 */
interface Access {
    
    /**
     * Checks if the access flag is public.
     */
    fun isPublic(): Boolean
    
    /**
     * Checks if the access flag is private.
     */
    fun isPrivate(): Boolean
    
    /**
     * Checks if the access flag is protected.
     */
    fun isProtected(): Boolean
    
    /**
     * Checks if the access flag is static.
     */
    fun isStatic(): Boolean
    
    /**
     * Checks if the access flag is final.
     */
    fun isFinal(): Boolean
    
    /**
     * Checks if the access flag is super.
     */
    fun isSuper(): Boolean
    
    /**
     * Checks if the access flag is synchronized.
     */
    fun isSynchronized(): Boolean
    
    /**
     * Checks if the access flag is open.
     */
    fun isOpen(): Boolean
    
    /**
     * Checks if the access flag is transitive.
     */
    fun isTransitive(): Boolean
    
    /**
     * Checks if the access flag is volatile.
     */
    fun isVolatile(): Boolean
    
    /**
     * Checks if the access flag is bridge.
     */
    fun isBridge(): Boolean
    
    /**
     * Checks if the access flag is static phase.
     */
    fun isStaticPhase(): Boolean
    
    /**
     * Checks if the access flag is varargs.
     */
    fun isVarargs(): Boolean
    
    /**
     * Checks if the access flag is transient.
     */
    fun isTransient(): Boolean
    
    /**
     * Checks if the access flag is native.
     */
    fun isNative(): Boolean
    
    /**
     * Checks if the access flag is interface.
     */
    fun isInterface(): Boolean
    
    /**
     * Checks if the access flag is abstract.
     */
    fun isAbstract(): Boolean
    
    /**
     * Checks if the access flag is strict.
     */
    fun isStrict(): Boolean
    
    /**
     * Checks if the access flag is synthetic.
     */
    fun isSynthetic(): Boolean
    
    /**
     * Checks if the access flag is annotation.
     */
    fun isAnnotation(): Boolean
    
    /**
     * Checks if the access flag is enum.
     */
    fun isEnum(): Boolean
    
    /**
     * Checks if the access flag is mandated.
     */
    fun isMandated(): Boolean
    
    /**
     * Checks if the access flag is module.
     */
    fun isModule(): Boolean
    
    /**
     * Checks if this access has the given flags.
     */
    fun hasFlags(vararg flags: Int32): Boolean
    
    /**
     * Checks if this access doesn't have the given flags.
     */
    fun none(vararg flags: Int32): Boolean
    
}