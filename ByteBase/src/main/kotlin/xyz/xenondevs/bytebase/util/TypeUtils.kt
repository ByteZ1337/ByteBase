package xyz.xenondevs.bytebase.util

import org.objectweb.asm.Type
import java.lang.reflect.Array
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType
import java.lang.reflect.Type as ReflectType

val ReflectType.representedClass: Class<*>
    get() = when (this) {
        is ParameterizedType -> rawType as Class<*>
        is WildcardType -> upperBounds[0] as Class<*>
        is GenericArrayType -> Array.newInstance(genericComponentType.representedClass, 0)::class.java
        is Class<*> -> this
        else -> throw IllegalStateException("Type $this is not a class")
    }

val KProperty<*>.desc
    get() = Type.getDescriptor(returnType.javaType.representedClass)!!
