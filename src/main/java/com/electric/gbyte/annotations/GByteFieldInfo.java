package com.electric.gbyte.annotations;

import com.electric.gbyte.Constant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteOrder;

/**
 * 协议中每个字段的元信息
 *
 * @author bingo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GByteFieldInfo {

    // 默认小端序
    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

    private int minVersion = 1;

    private int maxVersion = Integer.MAX_VALUE;

    // 当前使用的协议文档版本
    private int currentVersion = 1;

    private int length = 0;

    private byte stringType = Constant.STRING_ASCII;

    // 字符串末尾填充
    private int stringFill = 0;

    private byte offsetType = Constant.NUMBER_OFFSET;

    private int offsetNum = 0;

    public GByteFieldInfo(int currentVersion) {
        this.currentVersion = currentVersion;
    }

}
