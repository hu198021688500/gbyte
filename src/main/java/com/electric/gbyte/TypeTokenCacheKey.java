package com.electric.gbyte;

import com.electric.gbyte.reflect.TypeToken;

/**
 * @author bingo
 */
public class TypeTokenCacheKey {

    private final Integer version;

    private final TypeToken<?> typeToken;

    public TypeTokenCacheKey(TypeToken<?> typeToken, Integer version) {
        this.version = version;
        this.typeToken = typeToken;
    }

    public Integer getVersion() {
        return version;
    }

    public TypeToken<?> getTypeToken() {
        return typeToken;
    }

    @Override
    public final int hashCode() {
        return version + typeToken.hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o instanceof TypeTokenCacheKey) {
            TypeTokenCacheKey typeTokenCacheKey = (TypeTokenCacheKey) o;
            if (!version.equals(typeTokenCacheKey.getVersion())) {
                return false;
            }
            return typeToken.equals(typeTokenCacheKey.getTypeToken());
        } else {
            return false;
        }
    }

}
