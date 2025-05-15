package com.electric.gbyte.annotations;

import com.electric.gbyte.Constant;

import java.lang.annotation.*;

/**
 * @author bingo
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GByteField {

    /**
     * 字节序，默认小端序
     */
    boolean littleEndian() default true;

    /**
     * 该域有效的最小协议文档版本
     */
    int minVersion() default 1;

    /**
     * 该域有效的最大协议文档版本
     */
    int maxVersion() default Integer.MAX_VALUE;

    /**
     * 该域字节长度
     */
    int length() default 0;

    /**
     * 字符串编码类型ASCII或者BCD
     */
    byte stringType() default Constant.STRING_ASCII;

    /**
     * 字符串填充，默认填充0，有些要求填充255
     */
    int stringFill() default 0;

    /**
     * 数字型修正类型，加减乘除
     * 例如有时金额需要由整型除以100，则offsetType为NUMBER_OFFSET_DIVIDE，offsetNum为100
     */
    byte offsetType() default Constant.NUMBER_OFFSET;

    /**
     * 数字型修正值
     */
    int offsetNum() default 0;

}
