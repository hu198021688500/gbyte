package com.electric.gbyte.internal.bind;

import com.electric.gbyte.GByte;
import com.electric.gbyte.TypeAdapter;
import com.electric.gbyte.TypeAdapterFactory;
import com.electric.gbyte.annotations.GByteFieldAdapter;
import com.electric.gbyte.internal.ConstructorConstructor;
import com.electric.gbyte.reflect.TypeToken;

/**
 * @author bingo
 */
public final class GByteAdapterAnnotationTypeAdapterFactory implements TypeAdapterFactory {

    private final ConstructorConstructor constructorConstructor;

    public GByteAdapterAnnotationTypeAdapterFactory(ConstructorConstructor constructorConstructor) {
        this.constructorConstructor = constructorConstructor;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(final GByte gByte, final TypeToken<T> targetType, final Integer version) {
        Class<? super T> rawType = targetType.getRawType();
        GByteFieldAdapter annotation = rawType.getAnnotation(GByteFieldAdapter.class);
        if (annotation == null) {
            return null;
        }
        return (TypeAdapter<T>) getTypeAdapter(constructorConstructor, gByte, targetType, annotation, version);
    }

    TypeAdapter<?> getTypeAdapter(final ConstructorConstructor constructorConstructor, final GByte gByte, final TypeToken<?> type, final GByteFieldAdapter annotation, final Integer version) {
        Object instance = constructorConstructor.get(TypeToken.get(annotation.value())).construct();

        TypeAdapter<?> typeAdapter;
        if (instance instanceof TypeAdapter) {
            typeAdapter = (TypeAdapter<?>) instance;
        } else if (instance instanceof TypeAdapterFactory) {
            typeAdapter = ((TypeAdapterFactory) instance).create(gByte, type, version);
        } else {
            throw new IllegalArgumentException("Invalid attempt to bind an instance of "
                    + instance.getClass().getName() + " as a @GByteFieldAdapter for " + type.toString()
                    + ". @GByteFieldAdapter value must be a TypeAdapter, TypeAdapterFactory.");
        }

        return typeAdapter;
    }
}
