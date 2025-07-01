

# GByte

GByte 是一个基于 Netty 框架开发的 Java 序列化与反序列化工具类库，主要用于处理二进制数据流。它支持通过自定义注解（如 `GByteField`）来控制字段的序列化方式，并提供了一套灵活的 `TypeAdapter` 机制，方便开发者定义自己的数据类型处理逻辑。可用于物联网设备（充电桩，电单车控制器等）通讯协议的实现。

## 主要特性
- 基于 Netty 的 `ByteBuf` 进行数据读写。
- 支持注解驱动的序列化配置。
- 提供多种内置类型适配器（如 String、Byte、Integer、Long、BigDecimal、Boolean）。
- 支持集合类型（Array、Collection）的序列化与反序列化。
- 支持版本控制，允许不同版本的数据结构共存。
- 提供 CRC 校验、位运算、校验和计算等实用方法。

## 安装

本项目基于 Maven 构建，只需将以下依赖添加到您的 `pom.xml` 文件中即可使用：

```xml
<dependency>
    <groupId>com.electric</groupId>
    <artifactId>gbyte</artifactId>
    <version>1.0.0</version> <!-- 请替换为实际版本号 -->
</dependency>
```

## 快速使用

### 初始化 GByte

```java
GByte gByte = new GByteBuilder()
    .registerTypeAdapter(Address.class, new AddressTypeAdapter())
    .create();
```

### 序列化对象到 ByteBuf

```java
Address address = new Address("127.0.0.1", 8080);
ByteBuf byteBuf = Unpooled.buffer();
gByte.toByteBuf(byteBuf, address, 1); // version = 1
```

### 从 ByteBuf 反序列化对象

```java
Address result = gByte.fromByteBuf(byteBuf, Address.class, 1); // version = 1
```

### 使用注解定义字段信息

```java
@Data
public static class Address {
    @GByteField(length = 4)
    private String ip;

    @GByteField(length = 2)
    private int port;
}
```

### 集成到 Netty 的 pipeline

#### 上行协议接口

```java
public interface IMessage {
    void handler(ChannelHandlerContext ctx);
}
```

#### 上行协议

```java
public class PileUpSendInfo implements IMessage {

    @GByteField(length = 15)
    private String ip;

    // 字段4个字节，大端序
    @GByteField(length = 4, littleEndian = false)
    private int port;

    // 功率，分辨率0.01kWh
    @GByteField(minVersion = 207, length = 4, offsetType = Constant.NUMBER_OFFSET_DIVIDE, offsetNum = 100)
    private BigDecimal power;

    // 报文标识
    public static int getCommand() {
        return 26;
    }
    
    public void handler(ChannelHandlerContext ctx) {
        System.out.println(this);
        
        // todo 业务流程
        
        // 下发
        PlatformReplyInfo platformReplyInfo = new PlatformReplyInfo();
        platformReplyInfo.setRs(0);
        GByteUtils.writeAndFlush(ctx, platformReplyInfo);
    }
}
```

#### 下行协议

```java
public class PlatformReplyInfo {

    @GByteField(length = 1)
    private Integer rs;

    // 报文标识
    public int getCommand(Integer protocolDocVersion) {
        return 27;
    }

    public int getLength(Integer protocolDocVersion) {
        return 1;
    }
}
```

#### 加载上行协议

```java

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

```

#### pileline

```java
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

## 校验工具

GByte 提供了一些常用的校验方法，例如：

- CRC 校验：`GByteUtils.modBusCRC(ByteBuf buf)`
- 校验和计算：`GByteUtils.checksum(ByteBuf buf)`
- XOR 校验：`GByteUtils.getXor(ByteBuf buf)`
- 位操作：`GByteUtils.getOneBit(Integer value, int pos)` 等。

## 协议支持

GByte 支持上行协议接口和下行协议的处理，适用于网络通信中常见的数据格式定义场景。

## 业务流程

- 通过 `GByteBuilder` 构建 `GByte` 实例。
- 使用 `TypeAdapter` 处理具体的数据格式。
- 通过 `toByteBuf` 和 `fromByteBuf` 方法进行序列化与反序列化。
- 支持 Netty 的 `ByteToMessageDecoder` 扩展，可直接集成到 Netty 的 pipeline 中。

## 贡献者指南

欢迎贡献代码和提出建议！请遵循以下步骤：
1. Fork 本仓库。
2. 创建新分支 (`git checkout -b feature/your-feature-name`)。
3. 提交更改 (`git commit -am 'Add new feature'`).
4. Push 到分支 (`git push origin feature/your-feature-name`).
5. 创建新的 Pull Request。

## 协议

本项目采用 MIT 协议。有关协议内容，请查看项目根目录下的 `LICENSE` 文件。

## 联系方式

Email: hu198021688500@163.com  
Wechat: BingoHuGuoBing  

如有问题，请在 Gitee 上提交 Issue 或联系项目维护者。  
https://gitee.com/huaxuan/gbyte.git
