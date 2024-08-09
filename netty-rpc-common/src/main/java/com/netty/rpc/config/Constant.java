package com.netty.rpc.config;

/**
 * ZooKeeper constant
 *
 * @author zero
 */
public interface Constant {
    int ZK_SESSION_TIMEOUT = 5000;//Zookeeper 客户端的会话超时时间
    int ZK_CONNECTION_TIMEOUT = 5000;//Zookeeper 客户端的连接超时时间

    String ZK_REGISTRY_PATH = "/registry";//Zookeeper 中用于服务注册的根路径
    String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";//Zookeeper 数据存储路径

    String ZK_NAMESPACE = "netty-rpc";//Zookeeper 中使用的命名空间

}
