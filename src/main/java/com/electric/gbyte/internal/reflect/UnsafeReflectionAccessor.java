package com.electric.gbyte.internal.reflect;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author bingo
 */
@Slf4j
final class UnsafeReflectionAccessor extends ReflectionAccessor {

    private static Class unsafeClass;
    private final Object theUnsafe = getUnsafeInstance();
    private final Field overrideField = getOverrideField();

    @Override
    public void makeAccessible(AccessibleObject ao) {
        boolean success = makeAccessibleWithUnsafe(ao);
        if (!success) {
            try {
                ao.setAccessible(true);
            } catch (SecurityException e) {
                log.error("GByte couldn't modify fields for {}\nand sun.misc.Unsafe not found.\nEither write a custom type adapter,"
                        + " or make fields accessible, or include sun.misc.Unsafe.{}", ao, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    boolean makeAccessibleWithUnsafe(AccessibleObject ao) {
        if (theUnsafe != null && overrideField != null) {
            try {
                Method method = unsafeClass.getMethod("objectFieldOffset", Field.class);
                long overrideOffset = (Long) method.invoke(theUnsafe, overrideField);
                Method putBooleanMethod = unsafeClass.getMethod("putBoolean", Object.class, long.class, boolean.class);
                putBooleanMethod.invoke(theUnsafe, ao, overrideOffset, true);
                return true;
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException ignored) {

            }
        }
        return false;
    }

    private static Object getUnsafeInstance() {
        try {
            unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return unsafeField.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static Field getOverrideField() {
        try {
            return AccessibleObject.class.getDeclaredField("override");
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
