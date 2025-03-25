package com.electric.gbyte.internal.bind;

import com.electric.gbyte.GByte;
import com.electric.gbyte.TypeAdapter;
import com.electric.gbyte.annotations.GByteFieldInfo;
import com.electric.gbyte.reflect.TypeToken;
import io.netty.buffer.ByteBuf;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * @author bingo
 */
final class TypeAdapterRuntimeTypeWrapper<T> extends TypeAdapter<T> {

    private final GByte context;

    private final TypeAdapter<T> delegate;

    private final Type type;

    private final Integer version;

    TypeAdapterRuntimeTypeWrapper(GByte context, TypeAdapter<T> delegate, Type type, Integer version) {
        this.context = context;
        this.delegate = delegate;
        this.type = type;
        this.version = version;
    }

    @Override
    public T read(final ByteBuf in, final GByteFieldInfo gByteFieldInfo) {
        return delegate.read(in, gByteFieldInfo);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void write(final ByteBuf out, final T value, final GByteFieldInfo gByteFieldInfo) {
        TypeAdapter chosen = delegate;
        Type runtimeType = getRuntimeTypeIfMoreSpecific(type, value);
        if (runtimeType != type) {
            TypeAdapter runtimeTypeAdapter = context.getAdapter(TypeToken.get(runtimeType), version);
            if (!(runtimeTypeAdapter instanceof ReflectiveTypeAdapterFactory.Adapter)) {
                chosen = runtimeTypeAdapter;
            } else if (!(delegate instanceof ReflectiveTypeAdapterFactory.Adapter)) {
                chosen = delegate;
            } else {
                chosen = runtimeTypeAdapter;
            }
        }
        chosen.write(out, value, gByteFieldInfo);
    }

    private Type getRuntimeTypeIfMoreSpecific(Type type, Object value) {
        if (value != null && (type instanceof TypeVariable<?> || type instanceof Class<?>)) {
            type = value.getClass();
        }
        return type;
    }
}
