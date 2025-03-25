package com.electric.gbyte.internal.bind;

import com.electric.gbyte.GByte;
import com.electric.gbyte.TypeAdapter;
import com.electric.gbyte.TypeAdapterFactory;
import com.electric.gbyte.annotations.GByteFieldInfo;
import com.electric.gbyte.internal.$GByte$Types;
import com.electric.gbyte.reflect.TypeToken;
import io.netty.buffer.ByteBuf;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @param <E>
 * @author bingo
 */
public final class ArrayTypeAdapter<E> extends TypeAdapter<Object> {

    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <T> TypeAdapter<T> create(GByte gByte, TypeToken<T> typeToken, Integer version) {
            Type type = typeToken.getType();
            if (!(type instanceof GenericArrayType || type instanceof Class && ((Class<?>) type).isArray())) {
                return null;
            }

            Type componentType = $GByte$Types.getArrayComponentType(type);
            TypeAdapter<?> componentTypeAdapter = gByte.getAdapter(TypeToken.get(componentType), version);
            return new ArrayTypeAdapter(gByte, componentTypeAdapter, $GByte$Types.getRawType(componentType), version);
        }
    };

    private final Class<E> componentType;

    private final TypeAdapter<E> componentTypeAdapter;

    private ArrayTypeAdapter(GByte context, TypeAdapter<E> componentTypeAdapter, Class<E> componentType, Integer version) {
        this.componentTypeAdapter = new TypeAdapterRuntimeTypeWrapper<>(context, componentTypeAdapter, componentType, version);
        this.componentType = componentType;
    }

    @Override
    public Object read(final ByteBuf in, GByteFieldInfo gByteFieldInfo) {
        List<E> list = new ArrayList<>();
        int length = gByteFieldInfo.getLength();
        while (length-- > 0) {
            E instance = componentTypeAdapter.read(in, gByteFieldInfo);
            list.add(instance);
        }

        int size = list.size();
        Object array = Array.newInstance(componentType, size);
        for (int i = 0; i < size; i++) {
            Array.set(array, i, list.get(i));
        }
        return array;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(ByteBuf out, Object array, GByteFieldInfo gByteFieldInfo) {
        for (int i = 0, l = Array.getLength(array); i < l; i++) {
            E value = (E) Array.get(array, i);
            componentTypeAdapter.write(out, value, gByteFieldInfo);
        }
    }
}
