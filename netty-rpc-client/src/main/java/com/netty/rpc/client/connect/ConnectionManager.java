package com.netty.rpc.client.connect;

import com.netty.rpc.client.handler.RpcClientHandler;
import com.netty.rpc.client.handler.RpcClientInitializer;
import com.netty.rpc.client.route.RpcLoadBalance;
import com.netty.rpc.client.route.impl.RpcLoadBalanceRoundRobin;
import com.netty.rpc.loader.ExtensionLoader;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.protocol.RpcServiceInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.client.connect
 * @Author: zero
 * @CreateTime: 2024-05-30  14:36
 * @Description: TODO
 * @Version: 1.0
 */
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 8,
            600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));

    private Map<RpcProtocol, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();//连接的服务器节点
    private CopyOnWriteArraySet<RpcProtocol> rpcProtocolSet = new CopyOnWriteArraySet<>();//存储客户端已知的所有服务节点信息
    private ReentrantLock lock = new ReentrantLock();//一个可重入的互斥锁,它提供了比 synchronized 关键字更细粒度的锁控制
    private Condition connected = lock.newCondition();//一个条件变量,它与 ReentrantLock 配合使用,用于线程的等待和通知
    private long waitTimeout = 5000;
    //private RpcLoadBalance loadBalance = new RpcLoadBalanceRoundRobin();//负载均衡策略
    private RpcLoadBalance loadBalance = ExtensionLoader.getExtensionLoader(RpcLoadBalance.class).getExtension("rpcLoadBalance");

    private volatile boolean isRunning = true;//volatile保证可见性

    private ConnectionManager() {
    }

    private static class SingletonHolder {//利用了 Java 类的加载机制来实现线程安全的单例
        private static final ConnectionManager instance = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 更新服务列表
     * @param serviceList
     */
    public void updateConnectedServer(List<RpcProtocol> serviceList) {
        //使用两个collections来管理服务信息和TCP连接，使得连接异步
        //rpcProtocolSet是本地的缓存，serviceList是最新的服务节点信息
        //一旦服务信息在zk上改变，将会触发这个函数
        // 客户端只需要关心它正在使用的服务
        if (serviceList != null && serviceList.size() > 0) {
            // 更新本地服务器节点缓存
            HashSet<RpcProtocol> serviceSet = new HashSet<>(serviceList.size());
            for (int i = 0; i < serviceList.size(); ++i) {
                RpcProtocol rpcProtocol = serviceList.get(i);
                serviceSet.add(rpcProtocol);
            }

            // 增加新的服务器信息
            for (final RpcProtocol rpcProtocol : serviceSet) {
                if (!rpcProtocolSet.contains(rpcProtocol)) {
                    connectServerNode(rpcProtocol);
                }
            }

            // 关闭并删除失效的服务器节点
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                if (!serviceSet.contains(rpcProtocol)) {
                    logger.info("Remove invalid service: " + rpcProtocol.toJson());
                    removeAndCloseHandler(rpcProtocol);
                }
            }
        } else {
            // No available service
            logger.error("No available service!");
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                removeAndCloseHandler(rpcProtocol);
            }
        }
    }

    /**
     * 操作单个服务器节点
     * @param rpcProtocol
     * @param type
     */
    public void updateConnectedServer(RpcProtocol rpcProtocol, PathChildrenCacheEvent.Type type) {
        if (rpcProtocol == null) {
            return;
        }
        if (type == PathChildrenCacheEvent.Type.CHILD_ADDED && !rpcProtocolSet.contains(rpcProtocol)) {//新增
            connectServerNode(rpcProtocol);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_UPDATED) {//修改【删除老的，增加新的】
            removeAndCloseHandler(rpcProtocol);
            connectServerNode(rpcProtocol);
        } else if (type == PathChildrenCacheEvent.Type.CHILD_REMOVED) {//删除
            removeAndCloseHandler(rpcProtocol);
        } else {
            throw new IllegalArgumentException("Unknow type:" + type);
        }
    }

    /**
     * 连接新的服务器节点
     * @param rpcProtocol
     */
    private void connectServerNode(RpcProtocol rpcProtocol) {
        //该服务器上没有服务
        if (rpcProtocol.getServiceInfoList() == null || rpcProtocol.getServiceInfoList().isEmpty()) {
            logger.info("No service on node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
            return;
        }
        //加入本地的缓存
        rpcProtocolSet.add(rpcProtocol);
        logger.info("New service node, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
        for (RpcServiceInfo serviceProtocol : rpcProtocol.getServiceInfoList()) {
            logger.info("New service info, name: {}, version: {}", serviceProtocol.getServiceName(), serviceProtocol.getVersion());
        }
        final InetSocketAddress remotePeer = new InetSocketAddress(rpcProtocol.getHost(), rpcProtocol.getPort());
        //线程池分配线程连接新服务器节点
        threadPoolExecutor.submit(() -> {
            Bootstrap b = new Bootstrap();
            b.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new RpcClientInitializer());

            ChannelFuture channelFuture = b.connect(remotePeer);
            //当连接操作完成时,这个监听器会被调用
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        logger.info("Successfully connect to remote server, remote peer = " + remotePeer);
                        //从channel的pipeline获取RpcClientHandler.class，确保客户端在需要发送请求时能够及时找到对应的处理器。
                        RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                        connectedServerNodes.put(rpcProtocol, handler);
                        handler.setRpcProtocol(rpcProtocol);
                        signalAvailableHandler();
                    } else {
                        logger.error("Can not connect to remote server, remote peer = " + remotePeer);
                    }
                }
            });
        });
    }

    /**
     * 通知其他等待新服务器连接可用信号的线程,有新的连接已经建立好了
     */
    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 等待空闲的服务
     * @return
     * @throws InterruptedException
     */
    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            logger.warn("Waiting for available service");
            return connected.await(this.waitTimeout, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 选择空闲服务进行调用
     * @param serviceKey
     * @return
     * @throws Exception
     */
    public RpcClientHandler chooseHandler(String serviceKey) throws Exception {
        int size = connectedServerNodes.values().size();
        //看是否有空闲服务
        while (isRunning && size <= 0) {
            try {
                waitingForHandler();
                size = connectedServerNodes.values().size();
            } catch (InterruptedException e) {
                logger.error("Waiting for available service is interrupted!", e);
            }
        }
        //通过负载均衡策略获取服务
        //先获取服务节点的信息
        RpcProtocol rpcProtocol = loadBalance.route(serviceKey, connectedServerNodes);
        //再获取服务
        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
        if (handler != null) {
            return handler;
        } else {
            throw new Exception("Can not get available connection");
        }
    }

    /**
     * 删除并关闭服务
     * @param rpcProtocol
     */
    private void removeAndCloseHandler(RpcProtocol rpcProtocol) {
        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
        if (handler != null) {
            handler.close();
        }
        connectedServerNodes.remove(rpcProtocol);
        rpcProtocolSet.remove(rpcProtocol);
    }

    /**
     * 删除某个服务连接
     * @param rpcProtocol
     */
    public void removeHandler(RpcProtocol rpcProtocol) {
        rpcProtocolSet.remove(rpcProtocol);
        connectedServerNodes.remove(rpcProtocol);
        logger.info("Remove one connection, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
    }

    public void stop() {
        isRunning = false;
        for (RpcProtocol rpcProtocol : rpcProtocolSet) {
            removeAndCloseHandler(rpcProtocol);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }
}


