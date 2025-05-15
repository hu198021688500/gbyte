package com.electric.gbyte;

import com.electric.gbyte.annotations.GByteField;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Data;

import static org.junit.jupiter.api.Assertions.*;

class GByteTest {

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        System.out.println("setUp");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.out.println("tearDown");
    }

    @org.junit.jupiter.api.Test
    void testGByte() {
        GByte gByte = new GByteBuilder().create();
        ByteBuf data = Unpooled.buffer(19);

        String ip = "192.168.1.1";
        ByteBufUtil.writeAscii(data, ip);
        data.writeZero(15 - ip.length());
        data.writeInt(8080);

        Address address = gByte.fromByteBuf(data, Address.class, 1);
        System.out.println(address);

        assertNotNull(address);
        assertEquals(8080, address.getPort());

        data.clear();
        gByte.toByteBuf(data, address, 1);
        data.skipBytes(15);
        assertEquals(8080, data.readInt());
    }

    @Data
    public static class Address {

        @GByteField(length = 15)
        private String ip;

        @GByteField(length = 4, littleEndian = false)
        private int port;
    }
}