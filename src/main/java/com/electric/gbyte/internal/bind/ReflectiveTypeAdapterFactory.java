package com.electric.gbyte.internal.bind;

import com.electric.gbyte.GByte;
import com.electric.gbyte.TypeAdapter;
import com.electric.gbyte.TypeAdapterFactory;
import com.electric.gbyte.annotations.GByteField;
import com.electric.gbyte.annotations.GByteFieldAdapter;
import com.electric.gbyte.annotations.GByteFieldInfo;
import com.electric.gbyte.internal.$GByte$Types;
import com.electric.gbyte.internal.ConstructorConstructor;
import com.electric.gbyte.internal.ObjectConstructor;
import com.electric.gbyte.internal.Primitives;
import com.electric.gbyte.internal.reflect.ReflectionAccessor;
import com.electric.gbyte.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * 针对自定义对象通过反射编解码
 *
 * @author bingo
 */
@Slf4j
public final class ReflectiveTypeAdapterFactory implements TypeAdapterFactory {

    private final ConstructorConstructor constructorConstructor;

    private final GByteAdapterAnnotationTypeAdapterFactory gByteAdapterFactory;

    private final ReflectionAccessor accessor = ReflectionAccessor.getInstance();

    public ReflectiveTypeAdapterFactory(ConstructorConstructor constructorConstructor, GByteAdapterAnnotationTypeAdapterFactory gByteAdapterFactory) {
        this.constructorConstructor = constructorConstructor;
        this.gByteAdapterFactory = gByteAdapterFactory;
    }

    @Override
    public <T> TypeAdapter<T> create(final GByte gByte, final TypeToken<T> type, final Integer version) {
        Class<? super T> raw = type.getRawType();
        if (!Object.class.isAssignableFrom(raw)) {
            return null;
        }

        ObjectConstructor<T> constructor = constructorConstructor.get(type);
        return new Adapter<>(constructor, getBoundFields(gByte, type, raw, version));
    }

    private ReflectiveTypeAdapterFactory.BoundField createBoundField(final GByte gByte, final Field field, final TypeToken<?> fieldType, final GByteFieldInfo gByteFieldInfo) {
        final boolean isPrimitive = Primitives.isPrimitive(fieldType.getRawType());

        TypeAdapter<?> mapped = null;
        GByteFieldAdapter annotation = field.getAnnotation(GByteFieldAdapter.class);
        if (annotation != null) {
            mapped = this.gByteAdapterFactory.getTypeAdapter(constructorConstructor, gByte, fieldType, annotation, gByteFieldInfo.getCurrentVersion());
        }

        final boolean gByteAdapterPresent = mapped != null;

        if (mapped == null) {
            mapped = gByte.getAdapter(fieldType, gByteFieldInfo.getCurrentVersion());
        }

        final TypeAdapter<?> typeAdapter = mapped;

        return new ReflectiveTypeAdapterFactory.BoundField(gByteFieldInfo) {
            @Override
            void read(ByteBuf in, Object value) {
                Object fieldValue = typeAdapter.read(in, this.gByteFieldInfo);
                if (fieldValue != null || !isPrimitive) {
                    try {
                        field.set(value, fieldValue);
                    } catch (IllegalAccessException e) {
                        log.warn("GByte set field exception:{}", e.getMessage());
                    }
                }
            }

            @Override
            @SuppressWarnings("all")
            void write(ByteBuf out, Object value) {
                Object fieldValue = null;
                try {
                    fieldValue = field.get(value);
                } catch (IllegalAccessException e) {
                    log.warn("GByte set field exception:{}", e.getMessage());
                }

                TypeAdapter t = gByteAdapterPresent ? typeAdapter : new TypeAdapterRuntimeTypeWrapper(gByte, typeAdapter, fieldType.getType(), this.gByteFieldInfo.getCurrentVersion());
                t.write(out, fieldValue, this.gByteFieldInfo);
            }
        };
    }

    private List<BoundField> getBoundFields(final GByte gByte, final TypeToken<?> type, final Class<?> raw, final Integer version) {
        List<BoundField> result = new ArrayList<>();
        if (raw.isInterface()) {
            return result;
        }

        Field[] fields = raw.getDeclaredFields();
        for (Field field : fields) {
            GByteField byteField = field.getAnnotation(GByteField.class);
            if (byteField == null) {
                continue;
            }
            if (version < byteField.minVersion() || version > byteField.maxVersion()) {
                continue;
            }

            accessor.makeAccessible(field);

            Type fieldType = $GByte$Types.resolve(type.getType(), raw, field.getGenericType());

            GByteFieldInfo gByteFieldInfo = new GByteFieldInfo(byteField.littleEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN,
                    byteField.minVersion(), byteField.maxVersion(), version, byteField.length(), byteField.stringType(), byteField.stringFill(),
                    byteField.offsetType(), byteField.offsetNum());

            result.add(createBoundField(gByte, field, TypeToken.get(fieldType), gByteFieldInfo));
        }

        return result;
    }

    static abstract class BoundField {

        final GByteFieldInfo gByteFieldInfo;

        BoundField(GByteFieldInfo gByteFieldInfo) {
            this.gByteFieldInfo = gByteFieldInfo;
        }

        abstract void write(ByteBuf out, Object value);

        abstract void read(ByteBuf in, Object value);
    }

    public static final class Adapter<T> extends TypeAdapter<T> {

        private final ObjectConstructor<T> constructor;

        private final List<BoundField> boundFields;

        Adapter(ObjectConstructor<T> constructor, List<BoundField> boundFields) {
            this.constructor = constructor;
            this.boundFields = boundFields;
        }

        @Override
        public T read(final ByteBuf in, final GByteFieldInfo gByteFieldInfo) {
            T instance = constructor.construct();
            for (BoundField field : boundFields) {
                field.read(in, instance);
            }
            return instance;
        }

        @Override
        public void write(final ByteBuf out, final T value, final GByteFieldInfo gByteFieldInfo) {
            for (BoundField field : boundFields) {
                field.write(out, value);
            }
        }
    }
}
