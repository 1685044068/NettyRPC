package com.netty.rpc.client.handler;

import com.netty.rpc.codec.*;
import com.netty.rpc.loader.ExtensionLoader;
import com.netty.rpc.serializer.Serializer;
import com.netty.rpc.serializer.kryo.KryoSerializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.client.handler
 * @Author: zero
 * @CreateTime: 2024-05-30  15:22
 * @Description: 初始化pipeline
 * @Version: 1.0
 */
public class RpcClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        //Serializer serializer = KryoSerializer.class.newInstance();
        Serializer serializer= ExtensionLoader.getExtensionLoader(Serializer.class).getExtension("serializer");
        ChannelPipeline cp = socketChannel.pipeline();
        cp.addLast(new IdleStateHandler(0, 0, Beat.BEAT_INTERVAL, TimeUnit.SECONDS));//心跳连接
        cp.addLast(new RpcEncoder(RpcRequest.class, serializer));//编码处理
        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));// Netty 中的一个解码器组件,它的主要作用是帮助我们从粘包或者分包的数据流中提取出完整的消息帧
        cp.addLast(new RpcDecoder(RpcResponse.class, serializer));//解码处理
        cp.addLast(new RpcClientHandler());//新创建RpcClientHandler
    }
}
