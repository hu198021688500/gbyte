package com.electric.gbyte.internal.bind;

import com.electric.gbyte.*;
import com.electric.gbyte.annotations.GByteFieldInfo;
import com.electric.gbyte.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * 基本数据类型编解码
 *
 * @author bingo
 */
@Slf4j
public final class TypeAdapters {

    public static <TT> TypeAdapterFactory newFactory(final Class<TT> type, final TypeAdapter<TT> typeAdapter) {
        return new TypeAdapterFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> TypeAdapter<T> create(GByte gByte, TypeToken<T> typeToken, Integer version) {
                return typeToken.getRawType() == type ? (TypeAdapter<T>) typeAdapter : null;
            }

            @Override
            public String toString() {
                return "Factory[type=" + type.getName() + ",adapter=" + typeAdapter + "]";
            }
        };
    }

    public static <TT> TypeAdapterFactory newFactory(final Class<TT> unBoxed, final Class<TT> boxed, final TypeAdapter<? super TT> typeAdapter) {
        return new TypeAdapterFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> TypeAdapter<T> create(GByte gByte, TypeToken<T> typeToken, Integer version) {
                Class<? super T> rawType = typeToken.getRawType();
                return (rawType == unBoxed || rawType == boxed) ? (TypeAdapter<T>) typeAdapter : null;
            }

            @Override
            public String toString() {
                return "Factory[type=" + boxed.getName() + "+" + unBoxed.getName() + ",adapter=" + typeAdapter + "]";
            }
        };
    }

    public static <TT> TypeAdapterFactory newFactory(final TypeToken<TT> type, final TypeAdapter<TT> typeAdapter) {
        return new TypeAdapterFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> TypeAdapter<T> create(GByte gByte, TypeToken<T> typeToken, Integer version) {
                return typeToken.equals(type) ? (TypeAdapter<T>) typeAdapter : null;
            }
        };
    }

    public static final TypeAdapter<String> STRING = new TypeAdapter<>() {
        @Override
        public String read(final ByteBuf in, final GByteFieldInfo gByteFieldInfo) {
            int length = gByteFieldInfo.getLength();
            if (length == 0) {
                length = in.readableBytes();
            } else if (!in.isReadable(length)) {
                return null;
            }

            byte[] bytes = new byte[length];
            in.readBytes(bytes);

            switch (gByteFieldInfo.getStringType()) {
                case Constant.STRING_BCD -> {
                    // 0x1A1B => 1A1B
                    return ByteBufUtil.hexDump(bytes);
                }
                case Constant.STRING_UTF8 -> {
                    // 0xE4B8ADE59BBD => 中国
                    return new String(bytes, StandardCharsets.UTF_8).trim();
                }
                case Constant.STRING_ASCII -> {
                    // 0x3141 => 1A
                    return new String(bytes, StandardCharsets.US_ASCII).trim();
                }
                default -> {
                    log.warn("未知的字符处理类型:{}读取", gByteFieldInfo.getStringType());
                    return null;
                }
            }
        }

        @Override
        public void write(ByteBuf out, final String value, final GByteFieldInfo gByteFieldInfo) {
            if (value == null) {
                GByteUtils.valueFillBytes(out, gByteFieldInfo.getLength(), gByteFieldInfo.getStringFill());
                return;
            }

            switch (gByteFieldInfo.getStringType()) {
                case Constant.STRING_BCD -> {
                    byte[] bytes = ByteBufUtil.decodeHexDump(value);
                    out.writeBytes(bytes);
                    GByteUtils.valueFillBytes(out, gByteFieldInfo.getLength() - bytes.length, gByteFieldInfo.getStringFill());
                }
                case Constant.STRING_ASCII -> {
                    int len = ByteBufUtil.writeAscii(out, value);
                    GByteUtils.valueFillBytes(out, gByteFieldInfo.getLength() - len, gByteFieldInfo.getStringFill());
                }
                case Constant.STRING_UTF8 -> {
                    int writtenBytesNumber = out.writeCharSequence(value, StandardCharsets.UTF_8);
                    GByteUtils.valueFillBytes(out, gByteFieldInfo.getLength() - writtenBytesNumber, gByteFieldInfo.getStringFill());
                }
                default -> log.warn("未知的字符处理类型:{}写入", gByteFieldInfo.getStringType());
            }
        }
    };

    public static final TypeAdapter<Byte> BYTE = new TypeAdapter<>() {
        @Override
        public Byte read(final ByteBuf in, final GByteFieldInfo gByteFieldInfo) {
            if (!in.isReadable()) {
                return null;
            }

            return (byte) in.readUnsignedByte();
        }

        @Override
        public void write(ByteBuf out, final Byte value, final GByteFieldInfo gByteFieldInfo) {
            if (value == null) {
                out.writeZero(1);
            } else {
                out.writeByte(value);
            }
        }
    };

    public static final TypeAdapter<Integer> INTEGER = new TypeAdapter<>() {
        @Override
        public Integer read(final ByteBuf in, final GByteFieldInfo gByteFieldInfo) {
            if (!in.isReadable(gByteFieldInfo.getLength())) {
                return null;
            }

            int value = 0;
            switch (gByteFieldInfo.getLength()) {
                case 1 -> value = in.readUnsignedByte();
                case 2 -> value = ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder() ? in.readUnsignedShortLE() : in.readUnsignedShort();
                case 3 -> value = ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder() ? in.readUnsignedMediumLE() : in.readUnsignedMedium();
                case 4 -> value = (int) (ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder() ? in.readUnsignedIntLE() : in.readUnsignedInt());
                default -> log.warn("未处理字节长度为{}的Integer型读取", gByteFieldInfo.getLength());
            }

            return switch (gByteFieldInfo.getOffsetType()) {
                case Constant.NUMBER_OFFSET_ADD -> value + gByteFieldInfo.getOffsetNum();
                case Constant.NUMBER_OFFSET_SUBTRACT -> value - gByteFieldInfo.getOffsetNum();
                case Constant.NUMBER_OFFSET_MULTIPLY -> value * gByteFieldInfo.getOffsetNum();
                case Constant.NUMBER_OFFSET_DIVIDE -> value / gByteFieldInfo.getOffsetNum();
                default -> value;
            };
        }

        @Override
        public void write(ByteBuf out, final Integer value, final GByteFieldInfo gByteFieldInfo) {
            int val = 0;
            if (value != null) {
                val = switch (gByteFieldInfo.getOffsetType()) {
                    case Constant.NUMBER_OFFSET_ADD -> value - gByteFieldInfo.getOffsetNum();
                    case Constant.NUMBER_OFFSET_SUBTRACT -> value + gByteFieldInfo.getOffsetNum();
                    case Constant.NUMBER_OFFSET_MULTIPLY -> value / gByteFieldInfo.getOffsetNum();
                    case Constant.NUMBER_OFFSET_DIVIDE -> value * gByteFieldInfo.getOffsetNum();
                    default -> value;
                };
            }

            switch (gByteFieldInfo.getLength()) {
                case 1 -> out.writeByte(val);
                case 2 -> {
                    if (ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder()) {
                        out.writeShortLE(val);
                    } else {
                        out.writeShort(val);
                    }
                }
                case 3 -> {
                    if (ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder()) {
                        out.writeMediumLE(val);
                    } else {
                        out.writeMedium(val);
                    }
                }
                case 4 -> {
                    if (ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder()) {
                        out.writeIntLE(val);
                    } else {
                        out.writeInt(val);
                    }
                }
                default -> log.warn("未处理字节长度为{}的Integer型写入", gByteFieldInfo.getLength());
            }
        }
    };

    public static final TypeAdapter<Number> LONG = new TypeAdapter<>() {
        @Override
        public Number read(final ByteBuf in, final GByteFieldInfo gByteFieldInfo) {
            if (!in.isReadable(gByteFieldInfo.getLength())) {
                return null;
            }

            long value = 0;
            if (gByteFieldInfo.getLength() == 8) {
                value = ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder() ? in.readLongLE() : in.readLong();
            } else {
                log.warn("未处理字节长度为{}的Long型读取", gByteFieldInfo.getLength());
            }

            return switch (gByteFieldInfo.getOffsetType()) {
                case Constant.NUMBER_OFFSET_ADD -> value + gByteFieldInfo.getOffsetNum();
                case Constant.NUMBER_OFFSET_SUBTRACT -> value - gByteFieldInfo.getOffsetNum();
                case Constant.NUMBER_OFFSET_MULTIPLY -> value * gByteFieldInfo.getOffsetNum();
                case Constant.NUMBER_OFFSET_DIVIDE -> value / gByteFieldInfo.getOffsetNum();
                default -> value;
            };
        }

        @Override
        public void write(ByteBuf out, final Number value, final GByteFieldInfo gByteFieldInfo) {
            long val = 0;

            if (value != null) {
                val = switch (gByteFieldInfo.getOffsetType()) {
                    case Constant.NUMBER_OFFSET_ADD -> value.longValue() - gByteFieldInfo.getOffsetNum();
                    case Constant.NUMBER_OFFSET_SUBTRACT -> value.longValue() + gByteFieldInfo.getOffsetNum();
                    case Constant.NUMBER_OFFSET_MULTIPLY -> value.longValue() / gByteFieldInfo.getOffsetNum();
                    case Constant.NUMBER_OFFSET_DIVIDE -> value.longValue() * gByteFieldInfo.getOffsetNum();
                    default -> value.longValue();
                };
            }

            if (gByteFieldInfo.getLength() == 8) {
                if (ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder()) {
                    out.writeLongLE(val);
                } else {
                    out.writeLong(val);
                }
            } else {
                log.warn("未处理字节长度为{}的Integer型写入", gByteFieldInfo.getLength());
            }
        }
    };

    public static final TypeAdapter<BigDecimal> BIG_DECIMAL = new TypeAdapter<>() {
        @Override
        public BigDecimal read(final ByteBuf in, final GByteFieldInfo gByteFieldInfo) {
            if (!in.isReadable(gByteFieldInfo.getLength())) {
                return null;
            }

            BigDecimal value = BigDecimal.ZERO;
            switch (gByteFieldInfo.getLength()) {
                case 1 -> value = new BigDecimal(in.readUnsignedByte());
                case 2 -> value = new BigDecimal(ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder() ? in.readUnsignedShortLE() : in.readUnsignedShort());
                case 3 -> value = new BigDecimal(ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder() ? in.readUnsignedMediumLE() : in.readUnsignedMedium());
                case 4 -> value = new BigDecimal(ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder() ? in.readUnsignedIntLE() : in.readUnsignedInt());
                default -> log.warn("未处理字节长度为{}的BigDecimal型读取", gByteFieldInfo.getLength());
            }

            int bit = switch (gByteFieldInfo.getOffsetNum()) {
                case 10 -> 1;
                case 100 -> 2;
                case 1000 -> 3;
                case 10000 -> 4;
                case 100000 -> 5;
                case 1000000 -> 6;
                default -> 0;
            };

            if (gByteFieldInfo.getOffsetType() == Constant.NUMBER_OFFSET_DIVIDE) {
                return value.divide(new BigDecimal(gByteFieldInfo.getOffsetNum()), bit, RoundingMode.HALF_UP);
            }

            return value;
        }

        @Override
        public void write(ByteBuf out, final BigDecimal value, final GByteFieldInfo gByteFieldInfo) {
            int val = 0;

            if (value != null && gByteFieldInfo.getOffsetType() == Constant.NUMBER_OFFSET_DIVIDE) {
                val = value.multiply(new BigDecimal(gByteFieldInfo.getOffsetNum())).intValue();
            }

            switch (gByteFieldInfo.getLength()) {
                case 1 -> out.writeByte(val);
                case 2 -> {
                    if (ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder()) {
                        out.writeShortLE(val);
                    } else {
                        out.writeShort(val);
                    }
                }
                case 3 -> {
                    if (ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder()) {
                        out.writeMediumLE(val);
                    } else {
                        out.writeMedium(val);
                    }
                }
                case 4 -> {
                    if (ByteOrder.LITTLE_ENDIAN == gByteFieldInfo.getByteOrder()) {
                        out.writeIntLE(val);
                    } else {
                        out.writeInt(val);
                    }
                }
                default -> log.warn("未处理字节长度为{}的BigDecimal型写入", gByteFieldInfo.getLength());
            }
        }
    };

    public static final TypeAdapter<Boolean> BOOLEAN = new TypeAdapter<>() {
        @Override
        public Boolean read(final ByteBuf in, final GByteFieldInfo gByteFieldInfo) {
            return in.readBoolean();
        }

        @Override
        public void write(ByteBuf out, final Boolean value, final GByteFieldInfo gByteFieldInfo) {
            out.writeBoolean(value);
        }
    };

}
