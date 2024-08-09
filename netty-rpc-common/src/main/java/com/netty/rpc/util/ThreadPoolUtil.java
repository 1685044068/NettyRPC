package com.netty.rpc.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 创建自定义线程池
 */
public class ThreadPoolUtil {
    public static ThreadPoolExecutor createThreadPool(final String name, int corePoolSize, int maxPoolSize) {
        ThreadPoolExecutor serverHandlerPool = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "netty-rpc-" + name + "-" + r.hashCode());
                    }
                },
                new ThreadPoolExecutor.AbortPolicy());

        return serverHandlerPool;
    }
}
