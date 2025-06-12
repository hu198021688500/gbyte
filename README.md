物联网二进制编解码（序列化反序列化）库（JAVA)

# 单独使用
```
public class Address {

    @GByteField(length = 15)
    private String ip;

    @GByteField(length = 4, littleEndian = false)
    private int port;
}

GByte gByte = new GByteBuilder().create();
ByteBuf data = Unpooled.buffer(41);

String ip = "192.168.1.1";
ByteBufUtil.writeAscii(data, ip);
data.writeZero(15 - ip.length());
data.writeInt(8080);

String ip1 = "192.168.1.2";
ByteBufUtil.writeAscii(data, ip1);
data.writeZero(15 - ip1.length());
data.writeInt(8081);

data.writeByte(1);
data.writeByte(2);
data.writeByte(3);

Addresses addresses = gByte.fromByteBuf(data, Addresses.class, 1);
System.out.println(addresses);

// 结果GByteTest.Addresses(addressList=[GByteTest.Address(ip=192.168.1.1, port=8080), GByteTest.Address(ip=192.168.1.2, port=8081)], data=[1, 2, 3])
```

# 使用Netty框架

## 上行协议接口
```
public interface IMessage {
    void handler(ChannelHandlerContext ctx);
}
```

## 上行协议
```
public class PileUpSendInfo implements IMessage {

    @GByteField(length = 15)
    private String ip;

    // 字段4个字节，大端序
    @GByteField(length = 4, littleEndian = false)
    private int port;

    // 功率，分辨率0.01kWh
    @GByteField(minVersion = 207, length = 4, offsetType = Constant.NUMBER_OFFSET_DIVIDE, offsetNum = 100)
    private BigDecimal power;

    public static int getCommand() {
        return 26;
    }
    
    public void handler(ChannelHandlerContext ctx) {
        System.out.println(this);
        
        # todo 业务流程
        
        PlatformReplyInfo platformReplyInfo = new PlatformReplyInfo();
        platformReplyInfo.setRs(0);
        GByteUtils.writeAndFlush(ctx, platformReplyInfo);
    }
}
```
## 下行协议
```
public class PlatformReplyInfo {

    @GByteField(length = 1)
    private Integer rs;

    public int getCommand(Integer protocolDocVersion) {
        return 27;
    }

    public int getLength(Integer protocolDocVersion) {
        return 1;
    }
}
public GByte gbyte() {
    return new GByteBuilder()
        .registerTypeAdapter(Pile.class, new PileTypeAdapter())
        .create();
}

```

```

public Map<Integer, Class<IMessage>> protocols() {
    Map<Integer, Class<IMessage>> protocols = new HashMap<>();
    ClassUtil.scan("com.electric.enneagon.proto.message.up").forEach(clazz -> {
        try {
            protocols.put((Integer) clazz.getMethod("getCommand").invoke(clazz), clazz);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.warn("加载协议异常:{}", e.toString());
        }
    });
    return protocols;
}

public class ByteToMessageDecoder extends BaseDecoder {

    private static final GByte G_BYTE = SpringHelper.getBean(GByte.class);

    private static final ChannelService CHANNEL_SERVICE = SpringHelper.getBean(ChannelService.class);

    private static final Map<Integer, Class<IMessage>> PROTOCOLS = (Map<Integer, Class<IMessage>>) SpringHelper.getBean("protocols");

    public ByteToMessageDecoder(ByteOrder byteOrder, int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, boolean failFast, int headerValue, int headerLength) {
        super(byteOrder, maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast, headerValue, headerLength);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        ByteBuf packet = this.findCompletePacket(in);
        if (packet == null) {
            return;
        }

        String pileCode = CHANNEL_SERVICE.getPileCode(ctx.channel());
        log.trace("桩[{}]请求原始数据:{}", pileCode, ByteBufUtil.hexDump(packet));

        int length = packet.readableBytes();
        byte checkCode = packet.getByte(length - 1);
        byte realCheckCode = GByteUtils.getXor(packet.slice(2, length - 3));
        if (checkCode != realCheckCode) {
            log.info(ByteBufUtil.hexDump(packet));
            log.warn("桩[{}]校验码不正确,校验码为{},而实际为{}", pileCode, checkCode, realCheckCode);
        }

        int command = packet.getUnsignedByte(2);
        if (PROTOCOLS.containsKey(command)) {
            log.debug("桩[{}]请求命令[{}],长度[{}]", pileCode, String.format("0x%02X", command), length);
        } else {
            log.warn("桩[{}]请求命令[{}]未实现", pileCode, String.format("0x%02X", command));
            packet.skipBytes(packet.readableBytes());
            return;
        }

        ChannelCache channelCache = CHANNEL_SERVICE.getChannelCacheAndFixConnect(ctx.channel());
        if (channelCache == null) {
            channelCache = CHANNEL_SERVICE.cacheHeader(ctx.channel(), new ChannelCache(packet.copy(0, 14)));
            if (channelCache == null) {
                log.info(ByteBufUtil.hexDump(packet));
                packet.skipBytes(packet.readableBytes());
                ctx.channel().close();
                return;
            }
        }

        Object data;
        ByteBuf dataIn = packet.slice(14, length - 15);
        if (channelCache.getEncryption() && command != 1) {
            log.trace("桩[{}]请求数据需要解密", pileCode);
            byte[] bytes = ByteBufUtil.getBytes(dataIn, 0, length - 15, false);
            byte[] decryptData = CryptoUtil.aesCBCDecrypt(channelCache.getSecretKey(), Constant.JX_SECRET_IV, bytes);
            if (decryptData == null) {
                log.error("桩[{}]请求数据解密异常", pileCode);
                packet.skipBytes(packet.readableBytes());
                return;
            }
            ByteBuf decryptDataIn = Unpooled.wrappedBuffer(decryptData);
            data = G_BYTE.fromByteBuf(decryptDataIn, PROTOCOLS.get(command), channelCache.getProtocolDocVersion());
            decryptDataIn.release();
        } else {
            data = G_BYTE.fromByteBuf(dataIn, PROTOCOLS.get(command), channelCache.getProtocolDocVersion());
        }

        log.debug("桩[{}]请求数据:{}", pileCode, data);
        out.add(data);

        packet.skipBytes(packet.readableBytes());
    }

}

public class MessageToByteEncoder extends io.netty.handler.codec.MessageToByteEncoder<Object> {

    private static final GByte G_BYTE = SpringHelper.getBean(GByte.class);

    private static final ChannelService CHANNEL_SERVICE = SpringHelper.getBean(ChannelService.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        ChannelCache channelCache = CHANNEL_SERVICE.getChannelCache(ctx.channel());
        log.debug("桩[{}]响应数据:{}", channelCache.getPileCode(), msg.toString());

        int command = (int) msg.getClass().getMethod("getCommand", Integer.class).invoke(msg, channelCache.getProtocolDocVersion());
        int length = (int) msg.getClass().getMethod("getLength", Integer.class).invoke(msg, channelCache.getProtocolDocVersion());
        log.debug("桩[{}]响应命令[{}],长度[{}]", channelCache.getPileCode(), String.format("0x%02X", command), length);

        channelCache.updateCommandAndLength(command, length);

        ByteBuf dataOut = Unpooled.buffer(length);
        G_BYTE.toByteBuf(dataOut, msg, channelCache.getProtocolDocVersion());

        if (channelCache.getEncryption()) {
            log.debug("桩[{}]响应数据需要加密", channelCache.getPileCode());
            byte[] bytes = ByteBufUtil.getBytes(dataOut, 0, length, false);
            dataOut.release();

            byte[] encryptData = CryptoUtil.aesCBCEncrypt(channelCache.getSecretKey(), Constant.JX_SECRET_IV, bytes);
            if (encryptData == null) {
                log.error("枪[{}]响应数据加密异常", channelCache.getPileCode());
                return;
            }
            dataOut = Unpooled.wrappedBuffer(encryptData);
        }

        ByteBuf header = channelCache.getByteBuf().copy();
        ByteBuf headerAndData = Unpooled.wrappedBuffer(header, dataOut);

        byte checkCode = GByteUtils.getXor(headerAndData.slice(2, length + 12));
        ByteBuf checkCodeByteBuf = Unpooled.wrappedBuffer(new byte[]{checkCode});
        ByteBuf frameData = Unpooled.wrappedBuffer(headerAndData, checkCodeByteBuf);

        log.trace("桩[{}]响应原始数据:{}", channelCache.getPileCode(), ByteBufUtil.hexDump(frameData, 0, 15 + length));

        out.writeBytes(frameData);
        frameData.release();
    }
}
```
Email: hu198021688500@163.com 

Wechat: BingoHuGuoBing
