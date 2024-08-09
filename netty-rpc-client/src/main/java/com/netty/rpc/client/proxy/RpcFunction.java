package com.netty.rpc.client.proxy;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.client.proxy
 * @Author: zero
 * @CreateTime: 2024-05-30  20:54
 * @Description: 函数式接口,它定义了一个应用于两个参数的方法 apply(T t, P p)
 * 这种接口可以被用作 lambda 表达式或方法引用的目标类型
 * @Version: 1.0
 */
@FunctionalInterface
public interface RpcFunction<T, P> extends SerializableFunction<T> {
    Object apply(T t, P p);
}

