package com.netty.rpc.util;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.util
 * @Author: zero
 * @CreateTime: 2024-08-09  10:41
 * @Description: TODO
 * @Version: 1.0
 */
public class Holder <T>{
    private volatile T value;
    public T get(){
        return value;
    }

    public void set(T value){
        this.value=value;
    }
}
