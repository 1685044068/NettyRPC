package com.netty.rpc.server.core;

import com.netty.rpc.codec.Beat;
import com.netty.rpc.codec.RpcRequest;
import com.netty.rpc.codec.RpcResponse;
import com.netty.rpc.util.ServiceUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import net.sf.cglib.reflect.FastClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.server.core
 * @Author: zero
 * @CreateTime: 2024-06-03  10:45
 * @Description: 对客户端的请求进行处理
 * @Version: 1.0
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);

    private final Map<String, Object> handlerMap;
    private final ThreadPoolExecutor serverHandlerPool;

    public RpcServerHandler(Map<String, Object> handlerMap, final ThreadPoolExecutor threadPoolExecutor) {
        this.handlerMap = handlerMap;
        this.serverHandlerPool = threadPoolExecutor;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final RpcRequest request) {
        // 如果是心跳包不做处理
        if (Beat.BEAT_ID.equalsIgnoreCase(request.getRequestId())) {
            logger.info("Server read heartbeat ping");
            return;
        }
        //从线程池中分配线程处理业务
        //execute没有返回值，submit有
        serverHandlerPool.execute(() -> {
            logger.info("Receive request " + request.getRequestId());//日志输出请求id
            //创建之后用于返回的response
            RpcResponse response = new RpcResponse();
            //给response进行一系列赋值
            response.setRequestId(request.getRequestId());
            try {
                //获取处理resquest的结果
                Object result = handle(request);
                //在response中进行设置
                response.setResult(result);
            } catch (Throwable t) {
                response.setError(t.toString());
                logger.error("RPC Server handle request error", t);
            }
            //绑定监听器
            ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    logger.info("Send response for request " + request.getRequestId());
                }
            });
        });
    }

    //核心处理函数
    private Object handle(RpcRequest request) throws Throwable {
        //获取request中服务的类名以及版本，并以此生成serviceKey
        String className = request.getClassName();
        String version = request.getVersion();
        String serviceKey = ServiceUtil.makeServiceKey(className, version);
        //根据serviceKey获取对应的服务节点
        Object serviceBean = handlerMap.get(serviceKey);
        //判空处理
        if (serviceBean == null) {
            logger.error("Can not find service implement with interface name: {} and version: {}", className, version);
            return null;
        }
        //从服务节点中获取对应的类，方法名称，和对应参数【为反射调用做准备】
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        logger.debug(serviceClass.getName());
        logger.debug(methodName);
        for (Class<?> parameterType : parameterTypes) {
            logger.debug(parameterType.getName());
        }
        for (Object parameter : parameters) {
            logger.debug(parameter.toString());
        }

        /**
         * 使用JDK reflect
         * Method method = serviceClass.getMethod(methodName, parameterTypes);
         * method.setAccessible(true);
         * return method.invoke(serviceBean, parameters);
         */

        /**
         * Cglib reflect
         * 1.创建一个拦截器：class MyApiInterceptor implements MethodInterceptor
         * 2.重写MyApiInterceptor的intercept方法，其中proxy.invokeSuper()表示调用原始类的被拦截到的方法
         * 3.创建类加强器Enhancer来生成代理对象Enhancer enhancer = new Enhancer();  enhancer.setSuperclass(Person.class);  enhancer.setCallback(new MyApiInterceptor());
         * 4.生成代理对象Person person = (Person) enhancer.create();
         */

        //FastClass 能够通过预先计算方法调用所需的参数类型和参数个数等信息,在运行时快速定位和调用目标方法,从而提高方法调用的性能
        //避免了反射调用时的动态查找和参数适配开销,从而提高了方法调用的性能
        FastClass serviceFastClass = FastClass.create(serviceClass);//为给定的 Java 类创建一个 FastClass 实例
        int methodIndex = serviceFastClass.getIndex(methodName, parameterTypes);//获取目标方法的索引
        //通过 serviceFastClass.invoke(methodIndex, serviceBean, parameters) 方法,调用目标方法，返回调用结果
        return serviceFastClass.invoke(methodIndex, serviceBean, parameters);
    }

    /**
     * 用于处理异常捕获
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("Server caught exception: " + cause.getMessage());
        ctx.close();
    }

    /**
     * 用户事件触发，当某些用户定义的事件发生时,Netty框架会调用这个方法,让开发者可以处理这些事件
     * 常见的用例是用于管理连接的生命周期,如连接的建立、关闭等
     * 这里用于连接空闲超时后关闭连接
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //判断当前事件是否为 IdleStateEvent(连接空闲超时事件)
        //如果是 IdleStateEvent，则关闭当前的 Channel，并记录日志信息
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();
            logger.warn("Channel idle in last {} seconds, close it", Beat.BEAT_TIMEOUT);
        } else {
            //如果不是 IdleStateEvent，则调用父类的 userEventTriggered() 方法进行进一步处理
            super.userEventTriggered(ctx, evt);
        }
    }
}
