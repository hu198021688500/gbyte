package com.electric.gbyte.annotations;

import com.electric.gbyte.Constant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteOrder;

/**
 * @author bingo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GByteFieldInfo {

    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

    private int minVersion = 1;

    private int maxVersion = Integer.MAX_VALUE;

    private int currentVersion = 1;

    private int length = 0;

    private byte stringType = Constant.STRING_ASCII;

    private int stringFill = 0;

    private byte offsetType = Constant.NUMBER_OFFSET;

    private int offsetNum = 0;

    public GByteFieldInfo(int currentVersion) {
        this.currentVersion = currentVersion;
    }

}
