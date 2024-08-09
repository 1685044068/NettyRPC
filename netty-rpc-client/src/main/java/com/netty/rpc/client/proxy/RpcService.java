package com.netty.rpc.client.proxy;

import com.netty.rpc.client.handler.RpcFuture;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.client.proxy
 * @Author: zero
 * @CreateTime: 2024-05-30  20:51
 * @Description: 提供两种调用 RPC 的方式
 * @Version: 1.0
 */
public interface RpcService<T, P, FN extends SerializableFunction<T>> {
    /**
     * @param funcName 接受一个函数名 funcName
     * @param args 若干个参数 args
     * @return 返回一个 RpcFuture 对象,表示 RPC 调用的结果
     * @throws Exception
     */
    RpcFuture call(String funcName, Object... args) throws Exception;

    /**
     * lambda method reference
     */
    /**
     *
     * @param fn 接受一个 FN 类型的函数引用 fn，FN 是一个泛型类型,它必须是 SerializableFunction<T> 的子类型
     * @param args 若干个参数 args
     * @return 返回一个 RpcFuture 对象,表示 RPC 调用的结果
     * @throws Exception
     */
    RpcFuture call(FN fn, Object... args) throws Exception;

}
