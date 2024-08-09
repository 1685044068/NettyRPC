package com.netty.rpc.client.proxy;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.client.proxy
 * @Author: zero
 * @CreateTime: 2024-05-30  20:56
 * @Description: 与RpcFunction 接口类似,但接受三个参数
 * @Version: 1.0
 */
@FunctionalInterface
public interface RpcFunction2<T, P1, P2> extends SerializableFunction<T> {
    Object apply(T t, P1 p1, P2 p2);
}