package com.netty.rpc.codec;

import com.netty.rpc.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.codec
 * @Author: zero
 * @CreateTime: 2024-05-30  11:26
 * 负责将从网络中接收到的字节流反序列化为 Java 对象,
 * 以便后续的业务逻辑处理。在RPC客户端或服务端接收到RPC请求或响应时,
 * 都会使用RpcDecoder将字节流反序列化为对应的 Java 对象,并进行进一步的处理
 * @Version: 1.0
 */
public class RpcDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(RpcDecoder.class);//日志记录器,用于记录解码过程中可能发生的异常
    private Class<?> genericClass;//泛型类型,表示要进行反序列化的 Java 对象的类型
    private Serializer serializer;//序列化器抽象类的具体实现

    public RpcDecoder(Class<?> genericClass, Serializer serializer) {
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    @Override
    public final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //首先检查输入的 ByteBuf 缓冲区中是否有足够的数据可以进行反序列化。如果缓冲区中的数据长度小于 4 字节(用于存储数据长度的整型值),则直接返回
        if (in.readableBytes() < 4) {
            return;
        }
        //读取数据的长度,并检查缓冲区中是否有足够的数据长度，
        in.markReaderIndex();
        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength) {//如果不够,则重置读取指针,并返回
            in.resetReaderIndex();
            return;
        }
        //读取数据到一个字节数组中,并使用 serializer 进行反序列化,得到一个 Java 对象
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        Object obj = null;
        try {
            obj = serializer.deserialize(data, genericClass);
            out.add(obj);
        } catch (Exception ex) {
            logger.error("Decode error: " + ex.toString());
        }
    }
}
