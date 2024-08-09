package com.netty.rpc.server.core;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.server.core
 * @Author: zero
 * @CreateTime: 2024-06-03  10:45
 * @Description: 服务抽象类
 * @Version: 1.0
 */
public abstract class Server {
    /**
     * start server
     *
     * @param
     * @throws Exception
     */
    public abstract void start() throws Exception;

    /**
     * stop server
     *
     * @throws Exception
     */
    public abstract void stop() throws Exception;

}

