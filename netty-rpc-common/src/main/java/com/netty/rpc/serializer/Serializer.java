package com.netty.rpc.serializer;

import com.netty.rpc.annotation.SPI;

@SPI
public interface Serializer {
    public abstract <T> byte[] serialize(T obj);

    public abstract <T> Object deserialize(byte[] bytes, Class<T> clazz);
}
