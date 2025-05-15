package com.electric.gbyte.internal.reflect;

import java.lang.reflect.AccessibleObject;

/**
 * @author bingo
 */
final class PreJava9ReflectionAccessor extends ReflectionAccessor {

    @Override
    public void makeAccessible(AccessibleObject ao) {
        ao.setAccessible(true);
    }
}
