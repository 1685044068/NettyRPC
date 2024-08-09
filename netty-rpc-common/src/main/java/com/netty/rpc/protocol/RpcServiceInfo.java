package com.netty.rpc.protocol;

import com.netty.rpc.util.JsonUtil;

import java.io.Serializable;
import java.util.Objects;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.protocol
 * @Author: zero
 * @CreateTime: 2024-05-30  13:46
 * @Description: 封装了远程服务提供者提供的接口名称(serviceName)和服务版本号(version),并且提供了equals判断，hashCode，toJson，toString方法
 * @Version: 1.0
 */
public class RpcServiceInfo implements Serializable {
    // interface name 服务提供者提供的接口名称
    private String serviceName;
    // service version 服务提供者提供的服务版本号
    private String version;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {// 比较两个 RpcServiceInfo 对象是否相等
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcServiceInfo that = (RpcServiceInfo) o;
        return Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, version);
    }

    public String toJson() {
        String json = JsonUtil.objectToJson(this);
        return json;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
