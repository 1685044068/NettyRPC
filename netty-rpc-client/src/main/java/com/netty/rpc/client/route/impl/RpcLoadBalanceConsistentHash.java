package com.netty.rpc.client.route.impl;

import com.google.common.hash.Hashing;
import com.netty.rpc.client.handler.RpcClientHandler;
import com.netty.rpc.client.route.RpcLoadBalance;
import com.netty.rpc.protocol.RpcProtocol;

import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.client.route.impl
 * @Author: zero
 * @CreateTime: 2024-05-30  15:41
 * @Description: 基于一致性哈希(Consistent Hash)算法的负载均衡策略
 * @Version: 1.0
 */

/**
 * 实现使用一致性哈希算法来选择服务节点,具有以下特点
 * 1.相同的服务标识 serviceKey 总是被路由到同一个服务节点,这样可以保证同一个客户端的请求总是被路由到同一个服务节点,有利于缓存和会话保持
 * 2.当服务节点增加或减少时,只有少部分请求会被重新路由到其他节点,降低了系统的扰动
 * 3.相比于轮询算法,一致性哈希可以更好地保证请求的均匀分布
 */


public class RpcLoadBalanceConsistentHash extends RpcLoadBalance {

    public RpcProtocol doRoute(String serviceKey, List<RpcProtocol> addressList) {
        //使用一致性哈希算法计算 serviceKey 的哈希值,再对可用服务节点的数量取模得到索引值 index
        int index = Hashing.consistentHash(serviceKey.hashCode(), addressList.size());
        return addressList.get(index);
    }
    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception {//重写了父类 RpcLoadBalance 的抽象方法 route
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNodes);//首先调用父类的 getServiceMap 方法,获取服务标识到服务节点列表的映射
        List<RpcProtocol> addressList = serviceMap.get(serviceKey);//根据服务标识 serviceKey 从 serviceMap 中获取可用的服务节点列表 addressList
        if (addressList != null && addressList.size() > 0) {
            return doRoute(serviceKey, addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}
