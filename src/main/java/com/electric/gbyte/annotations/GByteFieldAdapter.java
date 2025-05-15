package com.electric.gbyte.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义适配器
 * <p>
 * 协议字段编解码自定义
 *
 * @author bingo
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface GByteFieldAdapter {

    Class<?> value();

}
