package com.electric.gbyte;

import com.electric.gbyte.annotations.GByteFieldInfo;
import io.netty.buffer.ByteBuf;

/**
 * @param <T>
 * @author bingo
 */
public abstract class TypeAdapter<T> {

    public abstract T read(final ByteBuf in, final GByteFieldInfo gByteFieldInfo);

    public abstract void write(ByteBuf out, final T value, final GByteFieldInfo gByteFieldInfo);

}
