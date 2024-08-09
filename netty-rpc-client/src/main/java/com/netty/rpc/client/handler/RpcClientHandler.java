package com.netty.rpc.client.handler;

import com.netty.rpc.client.connect.ConnectionManager;
import com.netty.rpc.codec.Beat;
import com.netty.rpc.codec.RpcRequest;
import com.netty.rpc.codec.RpcResponse;
import com.netty.rpc.protocol.RpcProtocol;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.client.handler
 * @Author: zero
 * @CreateTime: 2024-05-30  14:42
 * @Description: Netty 客户端通道处理器，负责处理与远程 RPC 服务的交互逻辑
 * @Version: 1.0
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);

    private ConcurrentHashMap<String, RpcFuture> pendingRPC = new ConcurrentHashMap<>();//一个线程安全的哈希映射,用于存储未完成的 RPC请求,当发送 RPC 请求时,会将其添加到这个集合中,等待服务端的响应
    private volatile Channel channel;//Netty 通道对象的引用,用于与远程服务进行通信
    private SocketAddress remotePeer;//远程服务的网络地址,用于记录当前 RPC 客户端连接的远程节点地址
    private RpcProtocol rpcProtocol;//记录当前的RpcProtocol【有服务器的地址端口和含有的服务】

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.remotePeer = this.channel.remoteAddress();
    }

    /**
     * 在通道激活时被调用,也就是说当客户端与服务端建立起连接时,这个方法就会被执行
     * 绑定channel
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        String requestId = response.getRequestId();//从服务器传过来的应答
        logger.debug("Receive response: " + requestId);
        RpcFuture rpcFuture = pendingRPC.get(requestId);//根据应答ID找到对应的rpcFuture
        if (rpcFuture != null) {
            pendingRPC.remove(requestId);//不为空的话就移出等待处理的map集合
            rpcFuture.done(response);//rpcFuture做对应的处理
        } else {
            logger.warn("Can not get pending response for request id: " + requestId);
        }
    }

    /**
     * 异常处理
     * 当发生异常时,主动关闭当前的 Netty 通道,防止异常继续传播,导致更多的错误发生
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Client caught exception: " + cause.getMessage());
        ctx.close();
    }

    /**
     * 关闭当前的handler
     *
     */
    public void close() {
        /**
         * channel.writeAndFlush(Unpooled.EMPTY_BUFFER)
         * 这行代码向当前的 Netty Channel 对象写入一个空的 Netty ByteBuf 对象
         * 这样做的目的是确保所有尚未发送的数据都能被刷新(flushed)到底层的网络套接字中
         * addListener(ChannelFutureListener.CLOSE)
         * 这行代码为上一步的 writeAndFlush 操作添加了一个 ChannelFutureListener.CLOSE 监听器
         * 这个监听器会在写入操作完成后,立即触发 Channel 的关闭操作
         */
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 将请求存入等待map中，并且发送请求给服务器
     * @param request
     * @return
     */
    public RpcFuture sendRequest(RpcRequest request) {
        RpcFuture rpcFuture = new RpcFuture(request);
        //将刚创建的 RpcFuture 对象存储到待处理的RPC请求中,以请求ID作为键
        //这样做的目的是建立一个映射关系,以便在收到服务端的响应时能够找到对应的 RpcFuture对象
        pendingRPC.put(request.getRequestId(), rpcFuture);
        try {
            //将当前的 RpcRequest 对象写入到 Netty 的 Channel 中,并立即进行刷新(flush)操作
            //为了确保写入操作完成,这里使用了 sync() 方法等待请求发送成功
            ChannelFuture channelFuture = channel.writeAndFlush(request).sync();
            if (!channelFuture.isSuccess()) {
                logger.error("Send request {} error", request.getRequestId());
            }
        } catch (InterruptedException e) {
            logger.error("Send request exception: " + e.getMessage());
        }

        return rpcFuture;
    }

    /**
     * RpcClientHandler 类中重写的 ChannelInboundHandler 接口方法之一。它用于处理网络连接上的各种用户事件,包括心跳检测
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //检查当前触发的事件是否是 IdleStateEvent
        if (evt instanceof IdleStateEvent) {
            //Send ping
            sendRequest(Beat.BEAT_PING);
            logger.debug("Client send beat-ping to " + remotePeer);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    public void setRpcProtocol(RpcProtocol rpcProtocol) {
        this.rpcProtocol = rpcProtocol;
    }

    /**
     * 用于处理网络连接关闭的事件,当网络连接关闭时,从 ConnectionManager 中删除当前的 RpcClientHandler 实例
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ConnectionManager.getInstance().removeHandler(rpcProtocol);
    }
}

