package com.netty.rpc.client;

import com.netty.rpc.annotation.RpcAutowired;
import com.netty.rpc.client.connect.ConnectionManager;
import com.netty.rpc.client.discovery.ServiceDiscovery;
import com.netty.rpc.client.proxy.ObjectProxy;
import com.netty.rpc.client.proxy.RpcService;
import com.netty.rpc.util.ThreadPoolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.client
 * @Author: zero
 * @CreateTime: 2024-06-01  19:55
 * @Description: TODO
 * @Version: 1.0
 */
public class RpcClient implements ApplicationContextAware, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private ServiceDiscovery serviceDiscovery;

    private static ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtil.createThreadPool(RpcClient.class.getSimpleName(), 8, 16);

    public RpcClient(String address) {
        this.serviceDiscovery = new ServiceDiscovery(address);
    }

    @SuppressWarnings("unchecked")
    public static <T, P> T createService(Class<T> interfaceClass, String version) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ObjectProxy<T, P>(interfaceClass, version)
        );
    }

    public static <T, P> RpcService createAsyncService(Class<T> interfaceClass, String version) {
        return new ObjectProxy<T, P>(interfaceClass, version);
    }

    public static void submit(Runnable task) {
        threadPoolExecutor.submit(task);
    }

    public void stop() {
        threadPoolExecutor.shutdown();
        serviceDiscovery.stop();
        ConnectionManager.getInstance().stop();
    }

    @Override
    public void destroy() throws Exception {
        this.stop();
    }

    /**
     * Spring 容器初始化并注入 ApplicationContext 实例时，就会调用这个方法
     * 这个方法的目的是自动给所有 Spring 管理的 bean 中标有 RpcAutowired 注解的字段进行赋值。
     * @param applicationContext
     * @throws BeansException
     */
    // 同样也是获取到所有带RpcAutowired注解的Bean【看哪些服务需要拉取】，
    // 这里获取的方式和server有所不同，因为这些RpcAutowired是字段级注解，NettyRpcService是类级
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Field[] fields = bean.getClass().getDeclaredFields();
            try {
                for (Field field : fields) {
                    RpcAutowired rpcAutowired = field.getAnnotation(RpcAutowired.class);
                    if (rpcAutowired != null) {
                        String version = rpcAutowired.version();
                        field.setAccessible(true);
                        field.set(bean, createAsyncService(field.getType(), version));
                    }
                }
            } catch (IllegalAccessException e) {
                logger.error(e.toString());
            }
        }
    }
}
