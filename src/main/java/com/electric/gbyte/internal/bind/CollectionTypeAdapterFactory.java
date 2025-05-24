package com.electric.gbyte.internal.bind;

import com.electric.gbyte.GByte;
import com.electric.gbyte.TypeAdapter;
import com.electric.gbyte.TypeAdapterFactory;
import com.electric.gbyte.annotations.GByteFieldInfo;
import com.electric.gbyte.internal.$GByte$Types;
import com.electric.gbyte.internal.ConstructorConstructor;
import com.electric.gbyte.internal.ObjectConstructor;
import com.electric.gbyte.reflect.TypeToken;
import io.netty.buffer.ByteBuf;

import java.lang.reflect.Type;
import java.util.Collection;

/**
 * @author bingo
 */
public final class CollectionTypeAdapterFactory implements TypeAdapterFactory {

    private final ConstructorConstructor constructorConstructor;

    public CollectionTypeAdapterFactory(ConstructorConstructor constructorConstructor) {
        this.constructorConstructor = constructorConstructor;
    }

    @Override
    public <T> TypeAdapter<T> create(GByte gByte, TypeToken<T> typeToken, Integer version) {
        Type type = typeToken.getType();

        Class<? super T> rawType = typeToken.getRawType();
        if (!Collection.class.isAssignableFrom(rawType)) {
            return null;
        }

        Type elementType = $GByte$Types.getCollectionElementType(type, rawType);
        TypeAdapter<?> componentTypeAdapter = gByte.getAdapter(TypeToken.get(elementType), version);
        ObjectConstructor<T> constructor = constructorConstructor.get(typeToken);

        @SuppressWarnings({"unchecked", "rawtypes"}) // create() doesn't define a type parameter
        TypeAdapter<T> result = new Adapter(gByte, elementType, componentTypeAdapter, constructor, version);
        return result;
    }

    private static final class Adapter<E> extends TypeAdapter<Collection<E>> {

        private final TypeAdapter<E> componentTypeAdapter;

        private final ObjectConstructor<? extends Collection<E>> constructor;

        public Adapter(GByte context, Type elementType, TypeAdapter<E> componentTypeAdapter, ObjectConstructor<? extends Collection<E>> constructor, Integer version) {
            this.componentTypeAdapter = new TypeAdapterRuntimeTypeWrapper<>(context, componentTypeAdapter, elementType, version);
            this.constructor = constructor;
        }

        @Override
        public Collection<E> read(ByteBuf in, GByteFieldInfo gByteFieldInfo) {
            if (!in.isReadable()) {
                return null;
            }

            Collection<E> collection = constructor.construct();

            int length = gByteFieldInfo.getLength();
            while (length-- > 0) {
                E instance = componentTypeAdapter.read(in, gByteFieldInfo);
                collection.add(instance);
            }
            return collection;
        }

        @Override
        public void write(ByteBuf out, Collection<E> value, GByteFieldInfo gByteFieldInfo) {
            if (value == null) {
                return;
            }

            for (E element : value) {
                componentTypeAdapter.write(out, element, gByteFieldInfo);
            }
        }
    }
}