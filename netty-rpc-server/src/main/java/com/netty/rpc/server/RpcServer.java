package com.netty.rpc.server;

import com.netty.rpc.annotation.NettyRpcService;
import com.netty.rpc.server.core.NettyServer;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.server
 * @Author: zero
 * @CreateTime: 2024-06-03  10:43
 * @Description: RPC服务
 * @Version: 1.0
 */
public class RpcServer extends NettyServer implements ApplicationContextAware, InitializingBean, DisposableBean {
    public RpcServer(String serverAddress, String registryAddress) {
        super(serverAddress, registryAddress);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        //获取到所有带NettyRpcService注解的Bean【看哪些服务需要发布】，NettyRpcService是类级注解
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(NettyRpcService.class);
        //非空判断后遍历需要发布的服务
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            for (Object serviceBean : serviceBeanMap.values()) {
                //获取这个类上NettyRpcService注解的相关信息
                NettyRpcService nettyRpcService = serviceBean.getClass().getAnnotation(NettyRpcService.class);
                String interfaceName = nettyRpcService.value().getName();
                String version = nettyRpcService.version();
                //加入到服务器本地的serviceMap进行记录
                super.addService(interfaceName, version, serviceBean);
            }
        }
    }

    /**
     * 在afterPropertiesSet时启动server
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        super.start();
    }

    /**
     * 在destroy的时候停止server
     */
    @Override
    public void destroy() {
        super.stop();
    }
}

