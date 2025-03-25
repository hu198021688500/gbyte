package com.electric.gbyte;

import java.lang.reflect.Type;

/**
 * @author bingo
 */
public interface InstanceCreator<T> {

    T createInstance(Type type);

}
