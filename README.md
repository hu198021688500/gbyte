

# GByte

GByte 是一个基于 Netty 框架开发的 Java 序列化与反序列化工具类库，主要用于处理二进制数据流。它支持通过自定义注解（如 `GByteField`）来控制字段的序列化方式，并提供了一套灵活的 `TypeAdapter` 机制，方便开发者定义自己的数据类型处理逻辑。

## 主要特性
- 基于 Netty 的 `ByteBuf` 进行数据读写。
- 支持注解驱动的序列化配置。
- 提供多种内置类型适配器（如 String、Byte、Integer、Number、BigDecimal、Boolean）。
- 支持集合类型（List、Set、Map）的序列化与反序列化。
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

如有问题，请在 Gitee 上提交 Issue 或联系项目维护者。
