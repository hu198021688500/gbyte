package com.electric.gbyte;

import com.electric.gbyte.reflect.TypeToken;

/**
 * @author bingo
 */
public interface TypeAdapterFactory {

    <T> TypeAdapter<T> create(GByte gByte, TypeToken<T> type, Integer version);

}
