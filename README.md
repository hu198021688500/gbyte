# gbyte
binary serialization and deserialization

# example 

### deserialization

```
public class PlatformDownSendIpSet {

    /**
     * 时标
     */
    @GByteField(length = 6)
    @GByteFieldAdapter(value = DatetimeTypeAdapter.class)
    private Datetime datetime;

    /**
     * 服务器地址
     */
    @GByteField(length = 50)
    private String serverAddress;

    /**
     * 端口
     */
    @GByteField(length = 2)
    private Integer port;

    public int getCommand(Integer protocolDocVersion) {
        return 53;
    }

    public int getLength(Integer protocolDocVersion) {
        return 58;
    }
}

public interface IMessage {

    void handler(ChannelHandlerContext ctx);
}

public class PileReplyIpSet implements IMessage {

    @ToString.Exclude
    private ChannelService channelService = SpringHelper.getBean(ChannelService.class);
    
    /**
     * 时标
     */
    @GByteField(length = 6)
    @GByteFieldAdapter(value = DatetimeTypeAdapter.class)
    private Datetime datetime;
    
    /**
     * 服务器地址ASCII
     */
    @GByteField(length = 50)
    private String serverAddress;

    /**
     * 端口
     */
    @GByteField(length = 2)
    private Integer port;

    public static int getCommand() {
        return 54;
    }

    @Override
    public void handler(ChannelHandlerContext ctx) {
        String pileCode = this.channelService.getPileCode(ctx.channel());
        log.info("桩[{}]回复新IP地址[{}]设置", pileCode, this.serverAddress + ":" + this.port);
    }

}

public class ByteToMessageDecoder extends BaseDecoder {

    private static final GByte G_BYTE = SpringHelper.getBean(GByte.class);

    private static final ChannelService CHANNEL_SERVICE = SpringHelper.getBean(ChannelService.class);

    @SuppressWarnings("unchecked")
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
