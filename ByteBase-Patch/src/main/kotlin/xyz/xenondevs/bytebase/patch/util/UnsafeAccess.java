
package xyz.xenondevs.bytebase.patch.util;

import jdk.internal.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;

@SuppressWarnings({"JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE", "unused"})
public final class UnsafeAccess {
    
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    
    public static Field getField(Class<?> clazz, String name, Class<?> type) {
        return Arrays.stream(clazz.getFields())
            .filter(field -> field.getName().equals(name) && field.getType() == type)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Field " + name + " not found in " + clazz));
    }
    
    public static Field getDeclaredField(Class<?> clazz, String name, Class<?> type) {
        return Arrays.stream(clazz.getDeclaredFields())
            .filter(field -> field.getName().equals(name) && field.getType() == type)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Field " + name + " not found in " + clazz));
    }
    
    public static long getFieldOffset(Field field) {
        return UNSAFE.objectFieldOffset(field);
    }
    
    public static long getStaticFieldOffset(Field field) {
        return UNSAFE.staticFieldOffset(field);
    }
    
    public static Object getStaticBase(Field field) {
        return UNSAFE.staticFieldBase(field);
    }
    
    public static boolean getBoolean(Object object, long offset) {
        return UNSAFE.getBoolean(object, offset);
    }
    
    public static byte getByte(Object object, long offset) {
        return UNSAFE.getByte(object, offset);
    }
    
    public static char getChar(Object object, long offset) {
        return UNSAFE.getChar(object, offset);
    }
    
    public static double getDouble(Object object, long offset) {
        return UNSAFE.getDouble(object, offset);
    }
    
    public static float getFloat(Object object, long offset) {
        return UNSAFE.getFloat(object, offset);
    }
    
    public static int getInt(Object object, long offset) {
        return UNSAFE.getInt(object, offset);
    }
    
    public static long getLong(Object object, long offset) {
        return UNSAFE.getLong(object, offset);
    }
    
    public static short getShort(Object object, long offset) {
        return UNSAFE.getShort(object, offset);
    }
    
    public static Object getObject(Object object, long offset) {
        return UNSAFE.getReference(object, offset);
    }
    
    public static void putBoolean(Object object, boolean value, long offset) {
        UNSAFE.putBoolean(object, offset, value);
    }
    
    public static void putByte(Object object, byte value, long offset) {
        UNSAFE.putByte(object, offset, value);
    }
    
    public static void putChar(Object object, char value, long offset) {
        UNSAFE.putChar(object, offset, value);
    }
    
    public static void putDouble(Object object, double value, long offset) {
        UNSAFE.putDouble(object, offset, value);
    }
    
    public static void putFloat(Object object, float value, long offset) {
        UNSAFE.putFloat(object, offset, value);
    }
    
    public static void putInt(Object object, int value, long offset) {
        UNSAFE.putInt(object, offset, value);
    }
    
    public static void putLong(Object object, long value, long offset) {
        UNSAFE.putLong(object, offset, value);
    }
    
    public static void putShort(Object object, short value, long offset) {
        UNSAFE.putShort(object, offset, value);
    }
    
    public static void putObject(Object object, Object value, long offset) {
        UNSAFE.putReference(object, offset, value);
    }
    
}
