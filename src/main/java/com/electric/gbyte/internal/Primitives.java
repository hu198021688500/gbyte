package com.electric.gbyte.internal;

import java.lang.reflect.Type;

/**
 * @author bingo
 */
public final class Primitives {

    private Primitives() {

    }

    public static boolean isPrimitive(Type type) {
        return type instanceof Class<?> && ((Class<?>) type).isPrimitive();
    }

    public static boolean isWrapperType(Type type) {
        return type == Integer.class
                || type == Float.class
                || type == Byte.class
                || type == Double.class
                || type == Long.class
                || type == Character.class
                || type == Boolean.class
                || type == Short.class
                || type == Void.class;
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> wrap(Class<T> type) {
        if (type == int.class) return (Class<T>) Integer.class;
        if (type == float.class) return (Class<T>) Float.class;
        if (type == byte.class) return (Class<T>) Byte.class;
        if (type == double.class) return (Class<T>) Double.class;
        if (type == long.class) return (Class<T>) Long.class;
        if (type == char.class) return (Class<T>) Character.class;
        if (type == boolean.class) return (Class<T>) Boolean.class;
        if (type == short.class) return (Class<T>) Short.class;
        if (type == void.class) return (Class<T>) Void.class;
        return type;
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> unwrap(Class<T> type) {
        if (type == Integer.class) return (Class<T>) int.class;
        if (type == Float.class) return (Class<T>) float.class;
        if (type == Byte.class) return (Class<T>) byte.class;
        if (type == Double.class) return (Class<T>) double.class;
        if (type == Long.class) return (Class<T>) long.class;
        if (type == Character.class) return (Class<T>) char.class;
        if (type == Boolean.class) return (Class<T>) boolean.class;
        if (type == Short.class) return (Class<T>) short.class;
        if (type == Void.class) return (Class<T>) void.class;
        return type;
    }

}
