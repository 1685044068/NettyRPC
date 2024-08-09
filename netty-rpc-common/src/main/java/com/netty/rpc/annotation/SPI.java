package com.netty.rpc.annotation;

import java.lang.annotation.*;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.annotation
 * @Author: zero
 * @CreateTime: 2024-08-09  10:12
 * @Description: TODO
 * @Version: 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {
    //TODO value可以提供对默认实现的支持，但是这方面的切面并没有写，只是写在这儿表示有这个东西
    String value() default "";
}
