package com.netty.rpc.client.proxy;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.client.proxy
 * @Author: zero
 * @CreateTime: 2024-05-30  20:47
 * @Description: 提供一种方式,可以获取实现该接口的lambda表达式的方法名
 * @Version: 1.0
 */
public interface SerializableFunction<T> extends Serializable {
    default String getName() throws Exception {
        //通过反射获取当前实现类的 writeReplace 方法，这个方法是 Java 序列化机制自动添加的一个方法,它用于在序列化时替换当前对象
        Method write = this.getClass().getDeclaredMethod("writeReplace");
        //将 writeReplace 方法设置为可访问,并调用它,获取一个 SerializedLambda 对象
        write.setAccessible(true);
        //SerializedLambda 是一个 Java 内部类,它包含了 lambda 表达式的各种元信息,比如实现方法名、所属类等
        SerializedLambda serializedLambda = (SerializedLambda) write.invoke(this);
        //最后,getName() 方法返回 SerializedLambda 对象的 getImplMethodName() 方法的结果,也就是 lambda 表达式的实现方法名
        return serializedLambda.getImplMethodName();
    }
}
