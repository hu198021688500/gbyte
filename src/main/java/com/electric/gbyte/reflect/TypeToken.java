package com.electric.gbyte.reflect;

import com.electric.gbyte.internal.$GByte$Preconditions;
import com.electric.gbyte.internal.$GByte$Types;

import java.lang.reflect.Type;

/**
 * @param <T>
 * @author bingo
 */
public class TypeToken<T> {

    final Class<? super T> rawType;
    final Type type;
    final int hashCode;

    @SuppressWarnings("unchecked")
    TypeToken(Type type) {
        this.type = $GByte$Types.canonicalize($GByte$Preconditions.checkNotNull(type));
        this.rawType = (Class<? super T>) $GByte$Types.getRawType(this.type);
        this.hashCode = this.type.hashCode();
    }

    public static TypeToken<?> get(Type type) {
        return new TypeToken<>(type);
    }

    public final Class<? super T> getRawType() {
        return rawType;
    }

    public final Type getType() {
        return type;
    }

    @Override
    public final int hashCode() {
        return this.hashCode;
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof TypeToken<?> && $GByte$Types.equals(type, ((TypeToken<?>) o).type);
    }

    @Override
    public final String toString() {
        return $GByte$Types.typeToString(type);
    }

}
