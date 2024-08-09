package com.netty.rpc.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for RPC service Autowired
 *这个注解的主要用途是标记某个字段需要自动注入一个 Netty RPC 服务。通常会配合前一个 NettyRpcService注解使用,用于在运行时自动发现和注入对应的 RPC 服务实例
 * 相当于Dubbo中的@DubboReference
 * @author zero
 */
@Target({ElementType.FIELD})//指定了该注解只能被应用在字段声明上,也就是类的成员变量上
@Retention(RetentionPolicy.RUNTIME)//指定了该注解的生命周期是在运行时
@Component
public @interface RpcAutowired {
    String version() default "";//可以用来标识服务的版本号
}
