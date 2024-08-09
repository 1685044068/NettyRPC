package com.netty.rpc.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.netty.rpc.codec.RpcRequest;
import com.netty.rpc.codec.RpcResponse;
import org.objenesis.strategy.StdInstantiatorStrategy;

/**
 * 封装了 Kryo 对象池的创建和管理逻辑。
 * 它使用单例模式确保全局只有一个 KryoPoolFactory 实例,
 * 并提供了一个获取 Kryo 对象池的入口点。这样可以避免在应用程序中频繁创建和销毁 Kryo 实例,提高性能。
 */
public class KryoPoolFactory {
    //使用了单例模式来确保在整个应用程序中只有一个 KryoPoolFactory 实例
    //Kryo是一个序列化工具
    private static volatile KryoPoolFactory poolFactory = null;

    private KryoFactory factory = new KryoFactory() {
        @Override
        public Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setReferences(false);
            kryo.register(RpcRequest.class);
            kryo.register(RpcResponse.class);
            Kryo.DefaultInstantiatorStrategy strategy = (Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy();
            strategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
            return kryo;
        }
    };

    private KryoPool pool = new KryoPool.Builder(factory).build();

    private KryoPoolFactory() {
    }
    //通过双重检查锁定的方式来确保只创建一个实例
    public static KryoPool getKryoPoolInstance() {
        if (poolFactory == null) {
            synchronized (KryoPoolFactory.class) {
                if (poolFactory == null) {
                    poolFactory = new KryoPoolFactory();
                }
            }
        }
        return poolFactory.getPool();
    }

    public KryoPool getPool() {
        return pool;
    }
}
