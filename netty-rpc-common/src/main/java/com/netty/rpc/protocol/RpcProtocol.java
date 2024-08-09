package com.netty.rpc.protocol;

import com.netty.rpc.util.JsonUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.protocol
 * @Author: zero
 * @CreateTime: 2024-05-30  13:45
 * 定义了一种用于在网络上传输服务提供者信息的协议格式。
 * 该协议包含了服务提供者的地址、端口和提供的服务列表等关键信息,可以在服务注册和发现过程中使用
 * @Version: 1.0
 */
public class RpcProtocol implements Serializable {
    private static final long serialVersionUID = -1102180003395190700L;
    // service host 服务提供者的主机地址
    private String host;
    // service port 服务提供者的端口号
    private int port;
    // service info list 服务提供者提供的服务信息列表
    private List<RpcServiceInfo> serviceInfoList;

    public String toJson() {// 将 RpcProtocol 对象转换为 JSON 格式的字符串,方便在网络上传输
        String json = JsonUtil.objectToJson(this);
        return json;
    }

    public static RpcProtocol fromJson(String json) {// 从 JSON 格式的字符串中解析出 RpcProtocol 对象
        return JsonUtil.jsonToObject(json, RpcProtocol.class);
    }

    @Override
    public boolean equals(Object o) {//重写equals方法，比较两个 RpcProtocol 对象是否相等
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcProtocol that = (RpcProtocol) o;
        return port == that.port &&
                Objects.equals(host, that.host) &&
                isListEquals(serviceInfoList, that.getServiceInfoList());
    }

    private boolean isListEquals(List<RpcServiceInfo> thisList, List<RpcServiceInfo> thatList) {//用于比较两个 RpcServiceInfo 列表是否相等
        if (thisList == null && thatList == null) {
            return true;
        }
        if ((thisList == null && thatList != null)
                || (thisList != null && thatList == null)
                || (thisList.size() != thatList.size())) {
            return false;
        }
        return thisList.containsAll(thatList) && thatList.containsAll(thisList);
    }

    @Override
    public int hashCode() {//实现了对象的哈希码计算逻辑,用于在哈希表中存储和查找 RpcProtocol 对象
        return Objects.hash(host, port, serviceInfoList.hashCode());
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<RpcServiceInfo> getServiceInfoList() {
        return serviceInfoList;
    }

    public void setServiceInfoList(List<RpcServiceInfo> serviceInfoList) {
        this.serviceInfoList = serviceInfoList;
    }
}
