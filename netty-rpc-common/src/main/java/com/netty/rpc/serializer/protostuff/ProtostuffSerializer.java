package com.netty.rpc.serializer.protostuff;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.netty.rpc.serializer.Serializer;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实现了序列化和反序列化功能,使用了 Protostuff 库作为底层实现
 */
public class ProtostuffSerializer implements Serializer {
    private Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();//缓存不同类的 Protostuff Schema 对象
    //objenesis 是一个 Objenesis 实例,用于创建对象,而无需调用其构造函数。这在反序列化过程中很有用,因为对象可能没有无参构造函数
    private Objenesis objenesis = new ObjenesisStd(true);

    @SuppressWarnings("unchecked")
    private <T> Schema<T> getSchema(Class<T> cls) {
        // for thread-safe
        return (Schema<T>) cachedSchema.computeIfAbsent(cls, RuntimeSchema::createFrom);
    }

    @Override
    public <T> byte[] serialize(T obj) {
        Class<T> cls = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(cls);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }

    @Override
    public <T> Object deserialize(byte[] bytes, Class<T> clazz) {
        try {
            T message = (T) objenesis.newInstance(clazz);
            Schema<T> schema = getSchema(clazz);
            ProtostuffIOUtil.mergeFrom(bytes, message, schema);
            return message;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
