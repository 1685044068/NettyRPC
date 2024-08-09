package com.netty.rpc.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务发布注解相当于Dubbo中的@DubboService
 * @author zero
 */
@Target({ElementType.TYPE})//指定了该注解只能被应用在类型声明上,也就是类、接口、枚举等声明上
@Retention(RetentionPolicy.RUNTIME)//指定了该注解的生命周期是在运行时,也就是说它可以被反射获取
@Component//标识了该注解是一个 Spring 组件,也就是说被这个注解标记的类会被 Spring 容器自动管理和注入
public @interface NettyRpcService {
    Class<?> value();//表示可以接受任意类型的 Java 类。通常这个属性用来标识被注解的类所对应的服务接口

    String version() default "";//可以用来标识服务的版本号
}

