package com.netty.rpc.server.core;

import com.netty.rpc.server.registry.ServiceRegistry;
import com.netty.rpc.util.ServiceUtil;
import com.netty.rpc.util.ThreadPoolUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.server.core
 * @Author: zero
 * @CreateTime: 2024-06-03  10:44
 * @Description: netty服务器
 * @Version: 1.0
 */
public class NettyServer extends Server {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private Thread thread;
    private String serverAddress;//服务地址
    private ServiceRegistry serviceRegistry;
    private Map<String, Object> serviceMap = new HashMap<>();

    public NettyServer(String serverAddress, String registryAddress) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = new ServiceRegistry(registryAddress);
    }

    /**
     * 增加服务
     * @param interfaceName
     * @param version
     * @param serviceBean
     */
    //增加服务
    public void addService(String interfaceName, String version, Object serviceBean) {
        logger.info("Adding service, interface: {}, version: {}, bean：{}", interfaceName, version, serviceBean);
        String serviceKey = ServiceUtil.makeServiceKey(interfaceName, version);
        serviceMap.put(serviceKey, serviceBean);
    }

    /**
     * Netty服务器启动，都是基本架构代码
     */
    public void start() {
        //创建一个新的线程去开启server
        thread = new Thread(new Runnable() {
            //下面为启动任务
            //服务器自定义线程池
            ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtil.createThreadPool(
                    NettyServer.class.getSimpleName(), 16, 32);
            @Override
            public void run() {
                EventLoopGroup bossGroup = new NioEventLoopGroup();//负责处理连接事件
                EventLoopGroup workerGroup = new NioEventLoopGroup();//负责处理IO事件
                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new RpcServerInitializer(serviceMap, threadPoolExecutor))
                            .option(ChannelOption.SO_BACKLOG, 128)//backlog 队列是服务端 Socket 接收客户端连接请求的队列
                            .childOption(ChannelOption.SO_KEEPALIVE, true);//设置子 Channel 的 TCP KeepAlive 选项为 true,定期向对端发送探测报文,用于检测连接是否还存活

                    String[] array = serverAddress.split(":");//服务地址以及端口号
                    String host = array[0];
                    int port = Integer.parseInt(array[1]);
                    ChannelFuture future = bootstrap.bind(host, port).sync();

                    if (serviceRegistry != null) {
                        serviceRegistry.registerService(host, port, serviceMap);//本地服务到zookeeper进行服务注册
                    }
                    logger.info("Server started on port {}", port);
                    future.channel().closeFuture().sync();
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        logger.info("Rpc server remoting server stop");
                    } else {
                        logger.error("Rpc server remoting server error", e);
                    }
                } finally {
                    try {
                        serviceRegistry.unregisterService();
                        workerGroup.shutdownGracefully();
                        bossGroup.shutdownGracefully();
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            }
        });
        thread.start();
    }

    public void stop() {
        // destroy server thread
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

}
