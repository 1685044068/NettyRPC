package com.netty.rpc.zookeeper;

import com.netty.rpc.config.Constant;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;

import java.util.List;

/**
 * Curator 是一个 Zookeeper 客户端库,它提供了更加简单易用的 API
 */
public class CuratorClient {
    private CuratorFramework client;

    public CuratorClient(String connectString, String namespace, int sessionTimeout, int connectionTimeout) {
        client = CuratorFrameworkFactory.builder().namespace(namespace).connectString(connectString)
                .sessionTimeoutMs(sessionTimeout).connectionTimeoutMs(connectionTimeout)
                .retryPolicy(new ExponentialBackoffRetry(1000, 10))
                .build();
        client.start();
    }

    public CuratorClient(String connectString, int timeout) {
        this(connectString, Constant.ZK_NAMESPACE, timeout, timeout);
    }

    public CuratorClient(String connectString) {
        this(connectString, Constant.ZK_NAMESPACE, Constant.ZK_SESSION_TIMEOUT, Constant.ZK_CONNECTION_TIMEOUT);
    }

    public CuratorFramework getClient() {
        return client;
    }

    /**
     * 允许客户端注册一个连接状态监听器,用于监听 Zookeeper 客户端连接状态的变化
     * @param connectionStateListener
     */
    public void addConnectionStateListener(ConnectionStateListener connectionStateListener) {
        client.getConnectionStateListenable().addListener(connectionStateListener);
    }

    /**
     * 在 Zookeeper 中创建一个临时顺序节点,并设置数据
     * @param path
     * @param data
     * @return
     * @throws Exception
     */
    public String createPathData(String path, byte[] data) throws Exception {
        return client.create().creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(path, data);
    }

    /**
     * 更新指定路径的节点数据
     * @param path
     * @param data
     * @throws Exception
     */
    public void updatePathData(String path, byte[] data) throws Exception {
        client.setData().forPath(path, data);
    }

    /**
     * 删除指定路径的节点
     * @param path
     * @throws Exception
     */
    public void deletePath(String path) throws Exception {
        client.delete().forPath(path);
    }

    /**
     * 在指定路径上设置数据观察器
     * @param path
     * @param watcher
     * @throws Exception
     */
    public void watchNode(String path, Watcher watcher) throws Exception {
        client.getData().usingWatcher(watcher).forPath(path);
    }

    /**
     * 获取某个路径节点的数据
     * @param path
     * @return
     * @throws Exception
     */
    public byte[] getData(String path) throws Exception {
        return client.getData().forPath(path);
    }

    /**
     * 获取某个路径节点的所有子节点
     * @param path
     * @return
     * @throws Exception
     */
    public List<String> getChildren(String path) throws Exception {
        return client.getChildren().forPath(path);
    }

    /**
     * 在指定路径上注册一个树形缓存监听器
     * @param path
     * @param listener
     */
    public void watchTreeNode(String path, TreeCacheListener listener) {
        TreeCache treeCache = new TreeCache(client, path);
        treeCache.getListenable().addListener(listener);
    }

    /**
     * 在指定路径上注册一个子节点缓存监听器
     * @param path
     * @param listener
     * @throws Exception
     */
    public void watchPathChildrenNode(String path, PathChildrenCacheListener listener) throws Exception {
        PathChildrenCache pathChildrenCache = new PathChildrenCache(client, path, true);
        //BUILD_INITIAL_CACHE 代表使用同步的方式进行缓存初始化。
        pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        pathChildrenCache.getListenable().addListener(listener);
    }

    public void close() {
        client.close();
    }
}
