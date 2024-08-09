package com.netty.rpc.client.route.impl;

import com.netty.rpc.client.handler.RpcClientHandler;
import com.netty.rpc.client.route.RpcLoadBalance;
import com.netty.rpc.protocol.RpcProtocol;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.client.route.impl
 * @Author: zero
 * @CreateTime: 2024-05-30  15:53
 * @Description: 基于最不经常使用(Least Frequently Used, LFU)算法的负载均衡策略
 * 这个负载均衡策略通过维护每个服务节点的访问次数统计(LFU 算法),
 * 选择最不经常被访问的节点进行 RPC 调用,以达到更加均衡的负载分配。
 * 同时,它还会定期清理过期的缓存,确保统计信息的准确性
 * @Version: 1.0
 */
public class RpcLoadBalanceLFU extends RpcLoadBalance {
    private ConcurrentHashMap<String, HashMap<RpcProtocol,Integer>> jobLfuMap=new ConcurrentHashMap<>();//用于存储服务名和相应的 LFU 统计信息
    private long CACHE_VALID_TIME = 0;//缓存时间

    public RpcProtocol doRoute(String serviceKey,List<RpcProtocol> addressList){
        //清空缓存
        if (System.currentTimeMillis()>CACHE_VALID_TIME){
            jobLfuMap.clear();
            CACHE_VALID_TIME=System.currentTimeMillis()+1000*60*60*24;//相当于每隔一天清除一次缓存
        }
        HashMap<RpcProtocol,Integer> lfuItemMap=jobLfuMap.get(serviceKey);//获取lfu记录比标
        //如果记录表为空就先创建
        if (lfuItemMap==null){
            lfuItemMap=new HashMap<>();
            jobLfuMap.putIfAbsent(serviceKey,lfuItemMap);// 避免重复覆盖
        }

        //存入新纪录
        for (RpcProtocol address:addressList){
            if (!lfuItemMap.containsKey(address) || lfuItemMap.get(address)>1000000){
                lfuItemMap.put(address, 0);
            }
        }
        //确保 lfuItemMap 中只包含当前可用的服务节点,避免出现已经不存在的节点影响负载均衡的情况
        //去除旧的记录
        List<RpcProtocol> delKeys=new ArrayList<>();
        for (RpcProtocol existKey : lfuItemMap.keySet()) {
            if (!addressList.contains(existKey)) {
                delKeys.add(existKey);
            }
        }

        if (delKeys.size() > 0) {
            for (RpcProtocol delKey : delKeys) {
                lfuItemMap.remove(delKey);
            }
        }

        // load least used count address
        List<Map.Entry<RpcProtocol, Integer>> lfuItemList = new ArrayList<Map.Entry<RpcProtocol, Integer>>(lfuItemMap.entrySet());
        Collections.sort(lfuItemList, new Comparator<Map.Entry<RpcProtocol, Integer>>() {
            @Override
            public int compare(Map.Entry<RpcProtocol, Integer> o1, Map.Entry<RpcProtocol, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        Map.Entry<RpcProtocol, Integer> addressItem = lfuItemList.get(0);
        RpcProtocol minAddress = addressItem.getKey();
        addressItem.setValue(addressItem.getValue() + 1);

        return minAddress;
    }


    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception {
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNodes);
        List<RpcProtocol> addressList = serviceMap.get(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(serviceKey, addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}
