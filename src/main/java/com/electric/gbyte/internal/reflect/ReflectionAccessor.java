package com.electric.gbyte.internal.reflect;

import com.electric.gbyte.internal.JavaVersion;

import java.lang.reflect.AccessibleObject;

/**
 * @author bingo
 */
public abstract class ReflectionAccessor {

    private static final ReflectionAccessor INSTANCE = JavaVersion.getMajorJavaVersion() < 9 ? new PreJava9ReflectionAccessor() : new UnsafeReflectionAccessor();

    public abstract void makeAccessible(AccessibleObject ao);

    public static ReflectionAccessor getInstance() {
        return INSTANCE;
    }
}
