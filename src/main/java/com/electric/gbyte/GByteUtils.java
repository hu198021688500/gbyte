package com.electric.gbyte;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bingo
 */
public class GByteUtils {

    public static short[] gabyCRCHi = {
            0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0, 0x80, 0x41, 0x01, 0xc0,
            0x80, 0x41, 0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0, 0x80, 0x41,
            0x00, 0xc1, 0x81, 0x40, 0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0,
            0x80, 0x41, 0x01, 0xc0, 0x80, 0x41, 0x00, 0xc1, 0x81, 0x40,
            0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0, 0x80, 0x41, 0x00, 0xc1,
            0x81, 0x40, 0x01, 0xc0, 0x80, 0x41, 0x01, 0xc0, 0x80, 0x41,
            0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0, 0x80, 0x41, 0x00, 0xc1,
            0x81, 0x40, 0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0, 0x80, 0x41,
            0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0, 0x80, 0x41, 0x01, 0xc0,
            0x80, 0x41, 0x00, 0xc1, 0x81, 0x40, 0x00, 0xc1, 0x81, 0x40,
            0x01, 0xc0, 0x80, 0x41, 0x01, 0xc0, 0x80, 0x41, 0x00, 0xc1,
            0x81, 0x40, 0x01, 0xc0, 0x80, 0x41, 0x00, 0xc1, 0x81, 0x40,
            0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0, 0x80, 0x41, 0x01, 0xc0,
            0x80, 0x41, 0x00, 0xc1, 0x81, 0x40, 0x00, 0xc1, 0x81, 0x40,
            0x01, 0xc0, 0x80, 0x41, 0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0,
            0x80, 0x41, 0x01, 0xc0, 0x80, 0x41, 0x00, 0xc1, 0x81, 0x40,
            0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0, 0x80, 0x41, 0x01, 0xc0,
            0x80, 0x41, 0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0, 0x80, 0x41,
            0x00, 0xc1, 0x81, 0x40, 0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0,
            0x80, 0x41, 0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0, 0x80, 0x41,
            0x01, 0xc0, 0x80, 0x41, 0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0,
            0x80, 0x41, 0x00, 0xc1, 0x81, 0x40, 0x00, 0xc1, 0x81, 0x40,
            0x01, 0xc0, 0x80, 0x41, 0x01, 0xc0, 0x80, 0x41, 0x00, 0xc1,
            0x81, 0x40, 0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0, 0x80, 0x41,
            0x00, 0xc1, 0x81, 0x40, 0x01, 0xc0, 0x80, 0x41, 0x01, 0xc0,
            0x80, 0x41, 0x00, 0xc1, 0x81, 0x40
    };

    public static short[] gabyCRCLo = {
            0x00, 0xc0, 0xc1, 0x01, 0xc3, 0x03, 0x02, 0xc2, 0xc6, 0x06,
            0x07, 0xc7, 0x05, 0xc5, 0xc4, 0x04, 0xcc, 0x0c, 0x0d, 0xcd,
            0x0f, 0xcf, 0xce, 0x0e, 0x0a, 0xca, 0xcb, 0x0b, 0xc9, 0x09,
            0x08, 0xc8, 0xd8, 0x18, 0x19, 0xd9, 0x1b, 0xdb, 0xda, 0x1a,
            0x1e, 0xde, 0xdf, 0x1f, 0xdd, 0x1d, 0x1c, 0xdc, 0x14, 0xd4,
            0xd5, 0x15, 0xd7, 0x17, 0x16, 0xd6, 0xd2, 0x12, 0x13, 0xd3,
            0x11, 0xd1, 0xd0, 0x10, 0xf0, 0x30, 0x31, 0xf1, 0x33, 0xf3,
            0xf2, 0x32, 0x36, 0xf6, 0xf7, 0x37, 0xf5, 0x35, 0x34, 0xf4,
            0x3c, 0xfc, 0xfd, 0x3d, 0xff, 0x3f, 0x3e, 0xfe, 0xfa, 0x3a,
            0x3b, 0xfb, 0x39, 0xf9, 0xf8, 0x38, 0x28, 0xe8, 0xe9, 0x29,
            0xeb, 0x2b, 0x2a, 0xea, 0xee, 0x2e, 0x2f, 0xef, 0x2d, 0xed,
            0xec, 0x2c, 0xe4, 0x24, 0x25, 0xe5, 0x27, 0xe7, 0xe6, 0x26,
            0x22, 0xe2, 0xe3, 0x23, 0xe1, 0x21, 0x20, 0xe0, 0xa0, 0x60,
            0x61, 0xa1, 0x63, 0xa3, 0xa2, 0x62, 0x66, 0xa6, 0xa7, 0x67,
            0xa5, 0x65, 0x64, 0xa4, 0x6c, 0xac, 0xad, 0x6d, 0xaf, 0x6f,
            0x6e, 0xae, 0xaa, 0x6a, 0x6b, 0xab, 0x69, 0xa9, 0xa8, 0x68,
            0x78, 0xb8, 0xb9, 0x79, 0xbb, 0x7b, 0x7a, 0xba, 0xbe, 0x7e,
            0x7f, 0xbf, 0x7d, 0xbd, 0xbc, 0x7c, 0xb4, 0x74, 0x75, 0xb5,
            0x77, 0xb7, 0xb6, 0x76, 0x72, 0xb2, 0xb3, 0x73, 0xb1, 0x71,
            0x70, 0xb0, 0x50, 0x90, 0x91, 0x51, 0x93, 0x53, 0x52, 0x92,
            0x96, 0x56, 0x57, 0x97, 0x55, 0x95, 0x94, 0x54, 0x9c, 0x5c,
            0x5d, 0x9d, 0x5f, 0x9f, 0x9e, 0x5e, 0x5a, 0x9a, 0x9b, 0x5b,
            0x99, 0x59, 0x58, 0x98, 0x88, 0x48, 0x49, 0x89, 0x4b, 0x8b,
            0x8a, 0x4a, 0x4e, 0x8e, 0x8f, 0x4f, 0x8d, 0x4d, 0x4c, 0x8c,
            0x44, 0x84, 0x85, 0x45, 0x87, 0x47, 0x46, 0x86, 0x82, 0x42,
            0x43, 0x83, 0x41, 0x81, 0x80, 0x40
    };

    public static int modBusCRC(ByteBuf buf) {
        int byCRCHi = 0xff;
        int byCRCLo = 0xff;

        int byIdx;

        while (buf.isReadable()) {
            short v = buf.readUnsignedByte();
            byIdx = (byCRCHi ^ v) & 0xff;
            byCRCHi = (byCRCLo ^ gabyCRCHi[byIdx]) & 0xff;
            byCRCLo = gabyCRCLo[byIdx];
        }

        int crc = byCRCHi;
        crc = (crc & 0xff) << 8;
        crc += byCRCLo;
        return crc & 0xffff;
    }

    /**
     * 如果想判断十进制数d的二进制第n位是否是1，应该用以下的数m来进行&运算 m = 1 << n-1 （n是大于0的整数) <p>
     * 以上公式 n=1的时候m=1 n=2的时候m=2 n=3的时候m=4 n=4的时候m=8 ...
     * <p>
     * 如果同时mask多个位，只需要将m相加即可。 例如第2位和第3位， 将m2 + m3 = 2 + 4 = 6
     * <p>
     * 判断二进制数据中第n位是否为1, d & (1 << n-1) != 0
     * 00000000 00000000 00000000 00000101  => d为5
     * 第二位是否为1
     * 00000000 00000000 00000000 00000010  => 1 << n-1
     * d & (1 << n-1) 为 0
     * 这里的n是从右往左，下面实际应用中是从左往右
     *
     * @param value 四个字节int型值
     * @param pos   位置
     * @return 是否为1，是true否则false
     */
    public static boolean getOneBit(Integer value, int pos) {
        return (value & (1 << (Integer.SIZE - pos))) != 0;
    }

    public static Integer getTwoBit(Integer value, int pos) {
        return getBit(value, pos, 2);
    }

    public static int getFourBit(Integer value, int pos) {
        return getBit(value, pos, 4);
    }

    /**
     * @param value  4字节int型
     * @param pos    位置
     * @param length 长度
     * @return 值
     */
    public static int getBit(Integer value, int pos, int length) {
        return value >> (Integer.SIZE - pos * length) & ((1 << length) - 1);
    }

    /**
     * 获取指定长度二进制的十进制
     *
     * @param value 四字节整型值
     * @param from  开始位置
     * @param to    结束位置
     * @return 十进制值
     */
    public static int getLengthBit(Integer value, int from, int to) {
        return (value << from) >> (Integer.SIZE - to + from);
    }

    public static byte accSum(ByteBuf buf) {
        byte temp = 0;
        while (buf.isReadable()) {
            temp += buf.readByte();
        }
        return temp;
    }

    public static byte getXor(ByteBuf buf) {
        byte temp = 0;
        while (buf.isReadable()) {
            temp ^= buf.readByte();
        }
        return temp;
    }

    public static byte checksum(ByteBuf buf) {
        int sum = 0;
        while (buf.isReadable()) {
            sum += buf.readByte();
        }

        if (sum > 0xff) {
            sum = ~sum;
            sum += 1;
        }

        sum = sum & 0xff;

        return (byte) sum;
    }

    public static int calculateChecksum(ByteBuf byteBuf) {
        return calculateChecksum(byteBuf, true);
    }

    public static int calculateChecksum(ByteBuf byteBuf, boolean littleEndian) {
        int temp = 0xffffffff;
        while (byteBuf.isReadable(4)) {
            temp ^= (int) (littleEndian ? byteBuf.readUnsignedIntLE() : byteBuf.readUnsignedInt());
        }
        return temp;
    }

    public static boolean byteArrayIsFull255(byte[] array) {
        for (byte b : array) {
            if (b != -1) {
                return false;
            }
        }
        return true;
    }

    public static void valueFillBytes(ByteBuf byteBuf, int num, int value) {
        if (num <= 0) {
            return;
        }

        if (value == 0) {
            byteBuf.writeZero(num);
        } else {
            for (int i = 0; i < num; i++) {
                byteBuf.writeByte(value);
            }
        }
    }

    public static List<String> getTroubleDesc(String[] troubleDesc, int trouble, int len) {
        List<String> hasTroubleDesc = new ArrayList<>();
        for (int i = len - 1; i >= 0; i--) {
            if ((trouble & (1 << i)) != 0) {
                hasTroubleDesc.add(troubleDesc[i]);
            }
        }
        return hasTroubleDesc;
    }

    public static void writeAndFlush(ChannelHandlerContext ctx, Object msg) {
        if (ctx == null) {
            System.out.println("ctx is null:" + msg.toString());
        } else if (ctx.channel().isWritable()) {
            ctx.channel().writeAndFlush(msg).addListener(writeFuture -> {
                if (!writeFuture.isSuccess()) {
                    System.out.println("send msg fail:" + msg.toString());
                }
            });
        } else {
            System.out.println("channel is not writable");

            try {
                ctx.writeAndFlush(msg).sync();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage() + ":" + msg.toString());
            }
        }
    }

}
