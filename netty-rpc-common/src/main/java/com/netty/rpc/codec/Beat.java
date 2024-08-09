package com.netty.rpc.codec;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.codec
 * @Author: zero
 * @CreateTime: 2024-05-30  11:21
 * @Description: 处理Netty RPC服务的心跳机制
 * @Version: 1.0
 */
public final class Beat {//一个类如果是final的，那么其中所有的成员方法都无法进行覆盖重写,使当前这个类不能有任何子类
    public static final int BEAT_INTERVAL = 30;//心跳间隔时间,也就是说,客户端和服务端之间会每隔 30 秒发送一次心跳请求
    public static final int BEAT_TIMEOUT = 3 * BEAT_INTERVAL;//心跳超时时间,单位为秒。如果在 3 个心跳间隔时间内没有收到对方的心跳响应,就认为对方已经超时或断开连接了
    public static final String BEAT_ID = "BEAT_PING_PONG";//心跳请求的唯一标识符,用于在 RPC 请求中区分普通业务请求和心跳请求

    public static RpcRequest BEAT_PING;//一个静态的 RpcRequest 对象,代表一个心跳请求

    //一个静态代码块,在类加载时会自动执行。在这个代码块中,我们创建了 BEAT_PING 对象,并设置了它的 requestId 为 BEAT_ID
    static {
        BEAT_PING = new RpcRequest();
        BEAT_PING.setRequestId(BEAT_ID);
    }
}
