package com.electric.gbyte;

import com.electric.gbyte.annotations.GByteFieldInfo;
import com.electric.gbyte.internal.ConstructorConstructor;
import com.electric.gbyte.internal.bind.ArrayTypeAdapter;
import com.electric.gbyte.internal.bind.GByteAdapterAnnotationTypeAdapterFactory;
import com.electric.gbyte.internal.bind.ReflectiveTypeAdapterFactory;
import com.electric.gbyte.internal.bind.TypeAdapters;
import com.electric.gbyte.reflect.TypeToken;
import io.netty.buffer.ByteBuf;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化和反序列化，参考GSON实现
 *
 * @author bingo
 */
public class GByte {

    private final List<TypeAdapterFactory> factories;

    private final ThreadLocal<Map<TypeTokenCacheKey, FutureTypeAdapter<?>>> calls = new ThreadLocal<>();

    private final Map<TypeTokenCacheKey, TypeAdapter<?>> typeTokenCache = new ConcurrentHashMap<>();

    public GByte(final Map<Type, InstanceCreator<?>> instanceCreators, List<TypeAdapterFactory> factories) {
        // 自定义的适配器
        List<TypeAdapterFactory> f = new ArrayList<>(factories);

        // 添加内置的类型的适配器
        f.add(TypeAdapters.newFactory(String.class, TypeAdapters.STRING));
        f.add(TypeAdapters.newFactory(byte.class, Byte.class, TypeAdapters.BYTE));
        f.add(TypeAdapters.newFactory(int.class, Integer.class, TypeAdapters.INTEGER));
        f.add(TypeAdapters.newFactory(long.class, Long.class, TypeAdapters.LONG));
        f.add(TypeAdapters.newFactory(boolean.class, Boolean.class, TypeAdapters.BOOLEAN));
        f.add(TypeAdapters.newFactory(BigDecimal.class, TypeAdapters.BIG_DECIMAL));

        // 仅支持byte数组
        f.add(ArrayTypeAdapter.FACTORY);

        ConstructorConstructor constructorConstructor = new ConstructorConstructor(instanceCreators);
        GByteAdapterAnnotationTypeAdapterFactory gByteAdapterFactory = new GByteAdapterAnnotationTypeAdapterFactory(constructorConstructor);
        f.add(gByteAdapterFactory);

        f.add(new ReflectiveTypeAdapterFactory(constructorConstructor, gByteAdapterFactory));

        this.factories = Collections.unmodifiableList(f);
    }

    public void toByteBuf(ByteBuf out, Object src) {
        this.toByteBuf(out, src, 1);
    }

    @SuppressWarnings("unchecked")
    public void toByteBuf(ByteBuf out, Object src, Integer version) {
        TypeAdapter<?> adapter = getAdapter(TypeToken.get(src.getClass()), version);
        ((TypeAdapter<Object>) adapter).write(out, src, new GByteFieldInfo(version));
    }

    public <T> T fromByteBuf(ByteBuf in, Type typeOfT) {
        return this.fromByteBuf(in, typeOfT, 1);
    }

    @SuppressWarnings("unchecked")
    public <T> T fromByteBuf(ByteBuf in, Type typeOfT, Integer version) {
        TypeToken<T> typeToken = (TypeToken<T>) TypeToken.get(typeOfT);
        TypeAdapter<T> typeAdapter = getAdapter(typeToken, version);
        return typeAdapter.read(in, new GByteFieldInfo(version));
    }

    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> getAdapter(TypeToken<T> type, Integer version) {
        TypeAdapter<?> cached = typeTokenCache.get(new TypeTokenCacheKey(type, version));
        if (cached != null) {
            return (TypeAdapter<T>) cached;
        }

        Map<TypeTokenCacheKey, FutureTypeAdapter<?>> threadCalls = calls.get();
        boolean requiresThreadLocalCleanup = false;
        if (threadCalls == null) {
            threadCalls = new HashMap<>();
            calls.set(threadCalls);
            requiresThreadLocalCleanup = true;
        }

        GByte.FutureTypeAdapter<T> ongoingCall = (GByte.FutureTypeAdapter<T>) threadCalls.get(new TypeTokenCacheKey(type, version));
        if (ongoingCall != null) {
            return ongoingCall;
        }

        try {
            GByte.FutureTypeAdapter<T> call = new GByte.FutureTypeAdapter<>();
            threadCalls.put(new TypeTokenCacheKey(type, version), call);

            for (TypeAdapterFactory factory : factories) {
                TypeAdapter<T> candidate = factory.create(this, type, version);
                if (candidate != null) {
                    call.setDelegate(candidate);
                    typeTokenCache.put(new TypeTokenCacheKey(type, version), candidate);
                    return candidate;
                }
            }
            throw new IllegalArgumentException("cannot handle " + type);
        } finally {
            threadCalls.remove(new TypeTokenCacheKey(type, version));

            if (requiresThreadLocalCleanup) {
                calls.remove();
            }
        }
    }

    public static class FutureTypeAdapter<T> extends TypeAdapter<T> {

        private TypeAdapter<T> delegate;

        void setDelegate(TypeAdapter<T> typeAdapter) {
            if (delegate != null) {
                throw new AssertionError();
            }
            delegate = typeAdapter;
        }

        @Override
        public T read(final ByteBuf in, final GByteFieldInfo gByteFieldInfo) {
            if (delegate == null) {
                throw new IllegalStateException();
            }
            return delegate.read(in, gByteFieldInfo);
        }

        @Override
        public void write(ByteBuf out, final T value, final GByteFieldInfo gByteFieldInfo) {
            if (delegate == null) {
                throw new IllegalStateException();
            }
            delegate.write(out, value, gByteFieldInfo);
        }
    }
}
