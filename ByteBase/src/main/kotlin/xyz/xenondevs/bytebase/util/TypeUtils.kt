package xyz.xenondevs.bytebase.util

import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import java.lang.reflect.Array
import java.lang.reflect.Field
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

val Field.desc
    get() = Type.getDescriptor(type.representedClass)!!

fun Type.getReturnInstruction(): AbstractInsnNode = buildInsnList {
    when (sort) {
        Type.VOID -> _return()
        Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> ireturn()
        Type.FLOAT -> freturn()
        Type.LONG -> lreturn()
        Type.DOUBLE -> dreturn()
        Type.ARRAY, Type.OBJECT -> areturn()
        else -> throw IllegalStateException("Unknown type $this")
    }
}.first

fun Type.getLoadInstruction(index: Int): AbstractInsnNode = buildInsnList {
    when (sort) {
        Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> iLoad(index)
        Type.FLOAT -> fLoad(index)
        Type.LONG -> lLoad(index)
        Type.DOUBLE -> dLoad(index)
        Type.ARRAY, Type.OBJECT -> aLoad(index)
        else -> throw IllegalStateException("Unknown type $this")
    }
}.first

fun Type.getStoreInstruction(index: Int): AbstractInsnNode = buildInsnList {
    when (sort) {
        Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> iStore(index)
        Type.FLOAT -> fStore(index)
        Type.LONG -> lStore(index)
        Type.DOUBLE -> dStore(index)
        Type.ARRAY, Type.OBJECT -> aStore(index)
        else -> throw IllegalStateException("Unknown type $this")
    }
}.first

fun Type.getLdcTypeInstruction(): AbstractInsnNode = buildInsnList {
    when (sort) {
        Type.BOOLEAN -> getStatic("java/lang/Boolean", "TYPE", "Ljava/lang/Class;")
        Type.CHAR -> getStatic("java/lang/Character", "TYPE", "Ljava/lang/Class;")
        Type.BYTE -> getStatic("java/lang/Byte", "TYPE", "Ljava/lang/Class;")
        Type.SHORT -> getStatic("java/lang/Short", "TYPE", "Ljava/lang/Class;")
        Type.INT -> getStatic("java/lang/Integer", "TYPE", "Ljava/lang/Class;")
        Type.FLOAT -> getStatic("java/lang/Float", "TYPE", "Ljava/lang/Class;")
        Type.LONG -> getStatic("java/lang/Long", "TYPE", "Ljava/lang/Class;")
        Type.DOUBLE -> getStatic("java/lang/Double", "TYPE", "Ljava/lang/Class;")
        Type.ARRAY, Type.OBJECT -> ldc(this@getLdcTypeInstruction)
        else -> throw IllegalStateException("Unknown type $this")
    }
}.first