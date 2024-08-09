package com.netty.rpc.codec;

import com.netty.rpc.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.codec
 * @Author: zero
 * @CreateTime: 2024-05-30  11:30
 * 负责将 RPC 请求或响应对象序列化为字节流,以便通过网络传输。
 * 在 RPC 客户端或服务端发送请求或响应时,都会使用 RpcEncoder
 * 将对象编码为字节数组,并将其写入到 Netty 的 I/O 缓冲区中,
 * 最后由 Netty 框架负责将缓冲区的数据发送到网络
 * @Version: 1.0
 */
public class RpcEncoder extends MessageToByteEncoder {
    private static final Logger logger = LoggerFactory.getLogger(RpcEncoder.class);//一个日志记录器,用于记录编码过程中可能发生的异常。
    private Class<?> genericClass;//一个泛型类型,表示要进行编码的 Java 对象的类型
    private Serializer serializer;//一个序列化器接口的实现,用于将 Java 对象序列化为字节数组

    public RpcEncoder(Class<?> genericClass, Serializer serializer) {//构造方法
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) throws Exception {
        if (genericClass.isInstance(in)) {//首先检查输入的 Java 对象是否与 genericClass 兼容
            //使用 serializer 进行序列化,并将序列化后的字节数组写入到 ByteBuf 缓冲区中
            try {
                byte[] data = serializer.serialize(in);
                out.writeInt(data.length);//先写入数据长度
                out.writeBytes(data);//再写入具体的数据
            } catch (Exception ex) {
                logger.error("Encode error: " + ex.toString());
            }
        }
    }
}
