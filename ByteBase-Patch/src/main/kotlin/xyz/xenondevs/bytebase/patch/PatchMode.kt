package xyz.xenondevs.bytebase.patch

import xyz.xenondevs.bytebase.patch.PatchMode.*
import xyz.xenondevs.bytebase.patch.patcher.kotlin.FieldHolder
import xyz.xenondevs.bytebase.patch.util.RuntimeClasspath
import xyz.xenondevs.bytebase.patch.util.UnsafeAccess
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.ref.WeakReference

/**
 * Tells ByteBase what restrictions the JVM imposes on the patched class. During runtime, the JVM heavily restricts
 * what can be done to already defined classes. ByteBase can pretty much only instrument method instructions.
 * Available:
 * * [CLASSLOADER]
 * * [INSTRUMENTATION]
 * * [AUTOMATIC]
 */
enum class PatchMode {
    /**
     * The class ByteBase is supposed to patch is not yet loaded, so a custom ClassLoader will be inserted into the
     * `ClassLoader` hierarchy to patch the class' bytecode before it is defined. This approach allows ByteBase to
     * freely edit the class, including adding new fields and methods, changing existing access restrictions or even
     * implementing interfaces.
     * This is the same approach as taken by [Sponge Mixins](https://github.com/SpongePowered/Mixin).
     */
    CLASSLOADER,
    
    /**
     * The class ByteBase is supposed to patch is already loaded, so ByteBase will have to instrument the class'
     * bytecode at runtime. This approach is much more limited than the [ClassLoader approach][CLASSLOADER], as the
     * JVM restricts what can be done to already defined classes. So ByteBase will automatically apply a few workarounds:
     *
     * ### Changing a field or method's access restrictions
     *
     * * For fields, instead of altering its access modifiers, ByteBase will calculate its offset from the outer object's
     * address. This address can then be used via [UnsafeAccess] to access the field (even change a final field's value!).
     * For static fields, the [static field offset][UnsafeAccess.getStaticFieldOffset] is used with the outer [Class]
     * instance being passed as the base. **!`final static` primitive and [String] fields are inlined during compilation so
     * their usages won't be affected by any value change!**
     *
     * * For methods, instead of altering the access modifiers, ByteBase will use a [`MethodHandle` lookup][MethodHandles.lookup]
     * to access the method. This [method's handle][MethodHandle] is then cached in a newly generated accompanying class
     * for better performance.
     *
     * ### Adding a new field or method
     *
     * * For new fields, ByteBase will redirect all field calls to the [FieldHolder] class. This class will then allocate
     * memory for primitives and save their address in a [Map] for later access. For non-primitive fields, the
     * [FieldHolder] will store the reference to the object in a different [Map]. Everything is saved using
     * [WeakReferences][WeakReference] to prevent any memory leaks.
     *
     * * For new methods, ByteBase will treat everything as a static method, similarly to how the Kotlin compiler
     * handles extension functions. If the method is not static, the first parameter will be the object the method is
     * called on, the second parameter will be the first parameter of the original method, and so on. Any now inaccessible
     * class members will again be accessed via the methods mentioned above.
     *
     * ### Adding a new interface
     *
     * This is the only thing that can't be done seamlessly during runtime. Instead of adding the interface to the patched
     * class, ByteBase will create a new class that implements the interface and delegates all calls to the patched class.
     * This obviously makes normal casting impossible so a method to "fake-cast" the patched class to the interface is
     * provided. This method will return a new instance of the generated class that implements the interface. This instance
     * can obviously not be used like the original class, since it doesn't inherit any of its members. It can however be
     * used as an instance of the desired interface.
     *
     */
    INSTRUMENTATION,
    
    /**
     * Automatically chooses between [CLASSLOADER] and [INSTRUMENTATION] based on whether the class is already loaded or not.
     */
    AUTOMATIC;
    
    fun getActualMode(name: String): PatchMode {
        if (this != AUTOMATIC) return this
        return if (RuntimeClasspath.isClassLoaded(name)) INSTRUMENTATION else CLASSLOADER
    }
    
}