package com.netty.rpc.codec;

import java.io.Serializable;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.codec
 * @Author: zero
 * @CreateTime: 2024-05-30  11:22
 *  * 封装了一个 RPC 调用的所有必要信息,包括调用的服务类、方法、参数等。在 RPC 客户端发起调用时,
 *  * 会创建一个 RpcRequest 对象,并将其序列化后通过网络发送给服务端。服务端收到请求后,
 *  * 会反序列化 RpcRequest 对象,并根据其中的信息来执行相应的服务方法,最后将结果返回给客户端
 * @Version: 1.0
 */
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = -2524587347775862771L;//序列化ID，防止序列化和反序列化的版本不同

    private String requestId;//请求的唯一标识符
    private String className;//请求所调用的服务类的名称
    private String methodName;//请求所调用的方法名称
    private Class<?>[] parameterTypes;//请求所调用方法的参数类型数组
    private Object[] parameters;//请求所调用方法的参数值数组
    private String version;//请求所调用服务的版本号

    //一系列的 getter 和 setter 方法,用于读取和设置上述各个属性
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
