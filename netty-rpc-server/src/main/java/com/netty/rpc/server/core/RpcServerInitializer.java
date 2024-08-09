package com.netty.rpc.server.core;

import com.netty.rpc.codec.*;
import com.netty.rpc.serializer.Serializer;
import com.netty.rpc.serializer.kryo.KryoSerializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.server.core
 * @Author: zero
 * @CreateTime: 2024-06-03  10:45
 * @Description: 初始化Pipeline
 * @Version: 1.0
 */
public class RpcServerInitializer extends ChannelInitializer<SocketChannel> {
    private Map<String, Object> handlerMap;
    private ThreadPoolExecutor threadPoolExecutor;

    public RpcServerInitializer(Map<String, Object> handlerMap, ThreadPoolExecutor threadPoolExecutor) {
        this.handlerMap = handlerMap;// 存储服务端的服务
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    public void initChannel(SocketChannel channel) throws Exception {
        //创建一个 Serializer 对象,这里使用的是 KryoSerializer
        Serializer serializer = KryoSerializer.class.newInstance();
        ChannelPipeline cp = channel.pipeline();
        cp.addLast(new IdleStateHandler(0, 0, Beat.BEAT_TIMEOUT, TimeUnit.SECONDS));//用于检测连接是否空闲,如果超过 Beat.BEAT_TIMEOUT 秒没有收到心跳,则断开连接
        cp.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));//用于处理粘包和拆包问题,基于消息长度字段进行解码
        cp.addLast(new RpcDecoder(RpcRequest.class, serializer));//用于将字节流反序列化为 RpcRequest 对象
        cp.addLast(new RpcEncoder(RpcResponse.class, serializer));//用于将 RpcResponse 对象序列化为字节流
        cp.addLast(new RpcServerHandler(handlerMap, threadPoolExecutor));//负责处理客户端发送的 RPC 请求,将请求路由到对应的服务实现类,并将结果返回给客户端
    }
}
