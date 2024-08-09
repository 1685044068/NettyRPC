package com.netty.rpc.client.discovery;

import com.netty.rpc.client.connect.ConnectionManager;
import com.netty.rpc.config.Constant;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.zookeeper.CuratorClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.client.discovery
 * @Author: zero
 * @CreateTime: 2024-06-01  19:51
 * @Description: 实现服务注册中心的服务发现功能
 * @Version: 1.0
 */
public class ServiceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);
    private CuratorClient curatorClient;//zookeeper客户端

    public ServiceDiscovery(String registryAddress) {
        this.curatorClient = new CuratorClient(registryAddress);
        discoveryService();
    }

    private void discoveryService() {
        try {
            // 获取初始服务信息缓存到本地rpcProtocolSet
            logger.info("Get initial service info");
            getServiceAndUpdateServer();
            /**
             * 为服务注册中心的路径 Constant.ZK_REGISTRY_PATH 添加了一个 PathChildrenCacheListener 监听器
             * 1.监听服务注册中心的变化事件
             * 2.处理连接恢复事件
             * 3.处理节点新增/更新/删除事件
             */
            curatorClient.watchPathChildrenNode(Constant.ZK_REGISTRY_PATH, new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                    PathChildrenCacheEvent.Type type = pathChildrenCacheEvent.getType();
                    ChildData childData = pathChildrenCacheEvent.getData();
                    switch (type) {//针对不同的事件类型进行不同的处理
                        //重新连接服务中心
                        case CONNECTION_RECONNECTED:
                            logger.info("Reconnected to zk, try to get latest service list");
                            getServiceAndUpdateServer();
                            break;
                        //新增子节点事件
                        case CHILD_ADDED:
                            getServiceAndUpdateServer(childData, PathChildrenCacheEvent.Type.CHILD_ADDED);
                            break;
                        //子节点更新事件
                        case CHILD_UPDATED:
                            getServiceAndUpdateServer(childData, PathChildrenCacheEvent.Type.CHILD_UPDATED);
                            break;
                        //子节点删除事件
                        case CHILD_REMOVED:
                            getServiceAndUpdateServer(childData, PathChildrenCacheEvent.Type.CHILD_REMOVED);
                            break;
                    }
                }
            });
        } catch (Exception ex) {
            logger.error("Watch node exception: " + ex.getMessage());
        }
    }

    /**
     * 从服务注册中心获取最新的服务列表,并更新到客户端的内部服务状态
     */
    private void getServiceAndUpdateServer() {
        try {
            List<String> nodeList = curatorClient.getChildren(Constant.ZK_REGISTRY_PATH);//从 Constant.ZK_REGISTRY_PATH 路径下获取所有的服务节点名称列表
            List<RpcProtocol> dataList = new ArrayList<>();
            //构造RPC协议列表
            //遍历服务器节点,获取各节点服务信息
            for (String node : nodeList) {
                logger.debug("Service node: " + node);
                byte[] bytes = curatorClient.getData(Constant.ZK_REGISTRY_PATH + "/" + node);
                String json = new String(bytes);
                RpcProtocol rpcProtocol = RpcProtocol.fromJson(json);
                dataList.add(rpcProtocol);
            }
            logger.debug("Service node data: {}", dataList);
            //更新到客户端的内部状态中
            UpdateConnectedServer(dataList);
        } catch (Exception e) {
            logger.error("Get node exception: " + e.getMessage());
        }
    }

    /**
     * 负责处理服务注册中心节点的变更事件,并相应地更新客户端的内部服务状态,相当于更新单个服务
     * @param childData
     * @param type
     */
    private void getServiceAndUpdateServer(ChildData childData, PathChildrenCacheEvent.Type type) {
        String path = childData.getPath();
        String data = new String(childData.getData(), StandardCharsets.UTF_8);
        logger.info("Child data updated, path:{},type:{},data:{},", path, type, data);
        RpcProtocol rpcProtocol =  RpcProtocol.fromJson(data);
        updateConnectedServer(rpcProtocol, type);
    }

    /**
     * 改变客户端中的服务列表
     * @param dataList
     */
    private void UpdateConnectedServer(List<RpcProtocol> dataList) {
        ConnectionManager.getInstance().updateConnectedServer(dataList);
    }

    /**
     * 改变客户端中的某个服务
     * @param rpcProtocol
     * @param type
     */
    private void updateConnectedServer(RpcProtocol rpcProtocol, PathChildrenCacheEvent.Type type) {
        ConnectionManager.getInstance().updateConnectedServer(rpcProtocol, type);
    }

    /**
     * 关闭zookeeper
     */
    public void stop() {
        this.curatorClient.close();
    }
}

