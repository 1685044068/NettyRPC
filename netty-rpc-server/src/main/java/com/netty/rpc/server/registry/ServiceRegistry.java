package com.netty.rpc.server.registry;

import com.netty.rpc.config.Constant;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.protocol.RpcServiceInfo;
import com.netty.rpc.util.ServiceUtil;
import com.netty.rpc.zookeeper.CuratorClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.server.registry
 * @Author: zero
 * @CreateTime: 2024-06-03  10:44
 * @Description: 服务注册
 * @Version: 1.0
 */
public class ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    private CuratorClient curatorClient;//zookeeper客户端
    private List<String> pathList = new ArrayList<>();//用于存储已注册的服务的路径

    public ServiceRegistry(String registryAddress) {
        this.curatorClient = new CuratorClient(registryAddress, 5000);
    }

    /**
     * 服务注册
     * @param host
     * @param port
     * @param serviceMap
     */
    public void registerService(String host, int port, Map<String, Object> serviceMap) {
        // Register service info
        List<RpcServiceInfo> serviceInfoList = new ArrayList<>();
        //从 serviceMap 中提取服务信息,并将其添加到 serviceInfoList 中
        for (String key : serviceMap.keySet()) {
            //使用 ServiceUtil.SERVICE_CONCAT_TOKEN 作为分隔符将其拆分成一个字符串数组 serviceInfo
            String[] serviceInfo = key.split(ServiceUtil.SERVICE_CONCAT_TOKEN);//"#"
            if (serviceInfo.length > 0) {
                //创建一个 RpcServiceInfo 对象,并设置服务名称和版本
                RpcServiceInfo rpcServiceInfo = new RpcServiceInfo();
                rpcServiceInfo.setServiceName(serviceInfo[0]);
                //如果 serviceInfo 数组长度为 2,说明包含了服务名称和版本,则分别设置
                if (serviceInfo.length == 2) {
                    rpcServiceInfo.setVersion(serviceInfo[1]);
                } else {
                    rpcServiceInfo.setVersion("");
                }
                logger.info("Register new service: {} ", key);
                //将创建的 RpcServiceInfo 对象添加到 serviceInfoList 中
                serviceInfoList.add(rpcServiceInfo);
            } else {
                logger.warn("Can not get service name and version: {} ", key);
            }
        }
        try {
            //RPC协议封装
            RpcProtocol rpcProtocol = new RpcProtocol();
            rpcProtocol.setHost(host);
            rpcProtocol.setPort(port);
            rpcProtocol.setServiceInfoList(serviceInfoList);
            String serviceData = rpcProtocol.toJson();
            byte[] bytes = serviceData.getBytes();
            String path = Constant.ZK_DATA_PATH + "-" + rpcProtocol.hashCode();
            path = this.curatorClient.createPathData(path, bytes);
            pathList.add(path);
            logger.info("Register {} new service, host: {}, port: {}", serviceInfoList.size(), host, port);
        } catch (Exception e) {
            logger.error("Register service fail, exception: {}", e.getMessage());
        }
        //添加了一个连接状态监听器,当服务端重新连接到 Zookeeper 时,会自动重新注册服务
        curatorClient.addConnectionStateListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
                if (connectionState == ConnectionState.RECONNECTED) {
                    logger.info("Connection state: {}, register service after reconnected", connectionState);
                    registerService(host, port, serviceMap);
                }
            }
        });
    }

    /**
     * 服务注销
     */
    public void unregisterService() {
        logger.info("Unregister all service");
        for (String path : pathList) {
            try {
                this.curatorClient.deletePath(path);
            } catch (Exception ex) {
                logger.error("Delete service path error: " + ex.getMessage());
            }
        }
        this.curatorClient.close();
    }
}
