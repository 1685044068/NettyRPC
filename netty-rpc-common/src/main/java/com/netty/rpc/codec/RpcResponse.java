package com.netty.rpc.codec;

import java.io.Serializable;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.codec
 * @Author: zero
 * @CreateTime: 2024-05-30  11:25
 * 封装了 RPC 调用的执行结果信息,包括是否成功、错误信息以及返回的结果对象。
 * 在 RPC 服务端执行完服务方法后,会创建一个 RpcResponse 对象,
 * 并将其序列化后通过网络发送给客户端。客户端收到响应后,
 * 会反序列化 RpcResponse 对象,并根据其中的信息来处理后续的逻辑
 * @Version: 1.0
 */
public class RpcResponse implements Serializable {
    private static final long serialVersionUID = 8215493329459772524L;

    private String requestId;//请求所对应的请求的唯一标识符
    private String error;//响应中的错误信息,如果请求执行过程中发生了错误,就会将错误信息设置到这个属性中
    private Object result;//响应的结果对象,如果请求执行成功,就会将结果存储到这个属性中。

    public boolean isError() {
        return error != null;
    }//用于检查响应是否包含错误信息

    //一系列的 getter 和 setter 方法,用于读取和设置上述各个属性

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}