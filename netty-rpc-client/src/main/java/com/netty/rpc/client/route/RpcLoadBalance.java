package com.netty.rpc.client.route;

import com.netty.rpc.client.handler.RpcClientHandler;
import com.netty.rpc.protocol.RpcProtocol;
import com.netty.rpc.protocol.RpcServiceInfo;
import com.netty.rpc.util.ServiceUtil;
import org.apache.commons.collections4.map.HashedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.client.route
 * @Author: zero
 * @CreateTime: 2024-05-30  15:29
 * @Description: 定义了一个负载均衡的基本逻辑
 * @Version: 1.0
 */
public abstract class RpcLoadBalance {
    protected Map<String, List<RpcProtocol>> getServiceMap(Map<RpcProtocol, RpcClientHandler> connectedServerNodes){
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
    //这是一个抽象方法,由子类实现具体的负载均衡算法
    public abstract RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception;
}
