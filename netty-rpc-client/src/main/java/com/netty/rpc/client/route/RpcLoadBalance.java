package com.netty.rpc.client.route;

import com.netty.rpc.annotation.SPI;
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
@SPI
public interface RpcLoadBalance {
    Map<String, List<RpcProtocol>> getServiceMap(Map<RpcProtocol, RpcClientHandler> connectedServerNodes);
    //这是一个抽象方法,由子类实现具体的负载均衡算法
    RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception;
}
