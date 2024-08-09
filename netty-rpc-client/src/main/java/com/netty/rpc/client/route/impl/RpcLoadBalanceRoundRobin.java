package com.netty.rpc.client.route.impl;

import com.netty.rpc.client.handler.RpcClientHandler;
import com.netty.rpc.client.route.RpcLoadBalance;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.protocol.RpcServiceInfo;
import com.netty.rpc.util.ServiceUtil;
import org.apache.commons.collections4.map.HashedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.client.route.impl
 * @Author: zero
 * @CreateTime: 2024-05-30  20:44
 * @Description: 基于轮询(Round Robin)算法的负载均衡策略
 * @Version: 1.0
 */
public class RpcLoadBalanceRoundRobin implements RpcLoadBalance {
    private AtomicInteger roundRobin = new AtomicInteger(0);

    public RpcProtocol doRoute(List<RpcProtocol> addressList) {
        int size = addressList.size();
        // Round robin
        int index = (roundRobin.getAndAdd(1) + size) % size;
        return addressList.get(index);
    }


    @Override
    public Map<String, List<RpcProtocol>> getServiceMap(Map<RpcProtocol, RpcClientHandler> connectedServerNodes) {
        Map<String, List<RpcProtocol>> serviceMap = new HashedMap<>();
        //判空
        if(connectedServerNodes!=null && connectedServerNodes.size()>0){
            for(RpcProtocol rpcProtocol: connectedServerNodes.keySet()){//遍历 connectedServerNodes，获取每个服务节点的 RpcProtocol
                for (RpcServiceInfo serviceInfo: rpcProtocol.getServiceInfoList()){//遍历每个 RpcProtocol 对象的 RpcServiceInfo 列表
                    String serviceKey = ServiceUtil.makeServiceKey(serviceInfo.getServiceName(), serviceInfo.getVersion());//根据服务名称和版本号生成一个服务标识 serviceKey
                    List<RpcProtocol> rpcProtocolList = serviceMap.get(serviceKey);
                    if (rpcProtocolList == null) {
                        rpcProtocolList = new ArrayList<>();
                    }
                    rpcProtocolList.add(rpcProtocol);
                    serviceMap.putIfAbsent(serviceKey, rpcProtocolList);//将每个服务标识对应的 RpcProtocol 对象添加到 serviceMap 中
                }
            }
        }
        return serviceMap;
    }

    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception {
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNodes);
        List<RpcProtocol> addressList = serviceMap.get(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}
