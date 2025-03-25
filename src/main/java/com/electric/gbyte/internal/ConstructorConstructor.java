package com.electric.gbyte.internal;

import com.electric.gbyte.InstanceCreator;
import com.electric.gbyte.internal.reflect.ReflectionAccessor;
import com.electric.gbyte.reflect.TypeToken;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author bingo
 */
public final class ConstructorConstructor {

    private final Map<Type, InstanceCreator<?>> instanceCreators;

    private final ReflectionAccessor accessor = ReflectionAccessor.getInstance();

    public ConstructorConstructor(Map<Type, InstanceCreator<?>> instanceCreators) {
        this.instanceCreators = instanceCreators;
    }

    public <T> ObjectConstructor<T> get(TypeToken<T> typeToken) {
        final Type type = typeToken.getType();
        final Class<? super T> rawType = typeToken.getRawType();

        @SuppressWarnings("unchecked") final InstanceCreator<T> typeCreator = (InstanceCreator<T>) instanceCreators.get(type);
        if (typeCreator != null) {
            return () -> typeCreator.createInstance(type);
        }

        @SuppressWarnings("unchecked") final InstanceCreator<T> rawTypeCreator = (InstanceCreator<T>) instanceCreators.get(rawType);
        if (rawTypeCreator != null) {
            return () -> rawTypeCreator.createInstance(type);
        }

        ObjectConstructor<T> defaultConstructor = newDefaultConstructor(rawType);
        if (defaultConstructor != null) {
            return defaultConstructor;
        }

        ObjectConstructor<T> defaultImplementation = newDefaultImplementationConstructor(type, rawType);
        return Objects.requireNonNullElseGet(defaultImplementation, () -> newUnsafeAllocator(type, rawType));

    }

    @SuppressWarnings({"unchecked", "deprecation"})
    private <T> ObjectConstructor<T> newDefaultConstructor(Class<? super T> rawType) {
        try {
            final Constructor<? super T> constructor = rawType.getDeclaredConstructor();
            if (!constructor.isAccessible()) {
                accessor.makeAccessible(constructor);
            }

            return () -> {
                try {
                    Object[] args = null;
                    return (T) constructor.newInstance(args);
                } catch (InstantiationException e) {
                    throw new RuntimeException("Failed to invoke " + constructor + " with no args", e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Failed to invoke " + constructor + " with no args", e.getTargetException());
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            };
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> ObjectConstructor<T> newDefaultImplementationConstructor(final Type type, Class<? super T> rawType) {
        if (Collection.class.isAssignableFrom(rawType)) {
            if (SortedSet.class.isAssignableFrom(rawType)) {
                return () -> (T) new TreeSet<>();
            } else if (EnumSet.class.isAssignableFrom(rawType)) {
                return () -> {
                    if (type instanceof ParameterizedType) {
                        Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
                        if (elementType instanceof Class) {
                            return (T) EnumSet.noneOf((Class) elementType);
                        }
                    }
                    return null;
                };
            } else if (Set.class.isAssignableFrom(rawType)) {
                return () -> (T) new LinkedHashSet<>();
            } else if (Queue.class.isAssignableFrom(rawType)) {
                return () -> (T) new ArrayDeque<>();
            } else {
                return () -> (T) new ArrayList<>();
            }
        }

        if (Map.class.isAssignableFrom(rawType)) {
            if (ConcurrentNavigableMap.class.isAssignableFrom(rawType)) {
                return () -> (T) new ConcurrentSkipListMap<>();
            } else if (ConcurrentMap.class.isAssignableFrom(rawType)) {
                return () -> (T) new ConcurrentHashMap<>();
            } else if (SortedMap.class.isAssignableFrom(rawType)) {
                return () -> (T) new TreeMap<>();
            } else if (type instanceof ParameterizedType && !(String.class.isAssignableFrom(TypeToken.get(((ParameterizedType) type).getActualTypeArguments()[0]).getRawType()))) {
                return () -> (T) new LinkedHashMap<>();
            } else {
                return () -> (T) new LinkedTreeMap<String, Object>();
            }
        }

        return null;
    }

    private <T> ObjectConstructor<T> newUnsafeAllocator(final Type type, final Class<? super T> rawType) {
        return new ObjectConstructor<>() {
            private final UnsafeAllocator unsafeAllocator = UnsafeAllocator.create();

            @Override
            @SuppressWarnings("unchecked")
            public T construct() {
                try {
                    Object newInstance = unsafeAllocator.newInstance(rawType);
                    return (T) newInstance;
                } catch (Exception e) {
                    throw new RuntimeException(("Unable to invoke no-args constructor for " + type + ". Registering an InstanceCreator with GByte for this type may fix this problem."), e);
                }
            }
        };
    }

    @Override
    public String toString() {
        return instanceCreators.toString();
    }

}
