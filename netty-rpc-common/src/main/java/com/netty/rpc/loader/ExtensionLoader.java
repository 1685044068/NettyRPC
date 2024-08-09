package com.netty.rpc.loader;


import com.netty.rpc.annotation.SPI;
import com.netty.rpc.util.Holder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @BelongsProject: myNettyRPC
 * @BelongsPackage: com.netty.rpc.loader
 * @Author: zero
 * @CreateTime: 2024-08-09  10:14
 * @Description: TODO
 * @Version: 1.0
 */

public class ExtensionLoader<T> {
    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);
    /**
     *扩展类存放的地址
     */
    private static final String SERVICE_DIRECTORY = "META-INF/services/";
    /**
     *本地缓存，Dubbo会先通过getExtensionLoader方法从缓存中获取一个ExtensionLoader
     *若缓存未命中，则会生成一个新的实例
     */
    private static final Map<Class<?>,ExtensionLoader<?>> EXTENSION_LOADER = new ConcurrentHashMap<>();
    /**
     *扩展类实例的缓存,目标扩展类的字节码和实例对象
     */
    private static final Map<Class<?>,Object> EXTENSION_INSTANCE = new ConcurrentHashMap<>();


    /**
     *扩展类Class文件
     */
    private final Class<?> type;
    /**
     *扩展类实例持有者Holder的缓存
     */
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    /**
     *扩展类配置列表缓存,扩展类实例对象，key为配置文件中的key，value为实例对象的全限定名称
     */
    private final Holder<Map<String,Class<?>>> cachedClasses = new Holder<>();

    /**
     *构造器指定Class类型
     */
    public ExtensionLoader(Class<?> type){this.type = type;}

    /**
     * 得到拓展加载程序
     * @param type 要扩展的接口，必须被Spi标记
     * @return
     * @param <S>
     */
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type){
        //判空
        if(type==null){
            throw new IllegalArgumentException("Extension type should not be null");
        }
        //判断是否是接口
        if (!type.isInterface()){
            throw new IllegalArgumentException("Extension type must be an interface");
        }
        //判断是被被Spi注解标记
        if(type.getAnnotation(SPI.class)==null){
            throw new IllegalArgumentException("Extension type must be annotated by @SPI");
        }
        ExtensionLoader<S> extensionLoader=(ExtensionLoader<S>) EXTENSION_LOADER.get(type);
        if(extensionLoader == null){
            //未命中则创建，并放入缓存
            EXTENSION_LOADER.putIfAbsent(type,new ExtensionLoader<S>(type));
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADER.get(type);
        }
        return extensionLoader;
    }

    /**
     * 得到扩展类对象实例
     * @param name
     * @return
     */
    public T getExtension(String name){
        if(StringUtils.isBlank(name)){
            throw new IllegalArgumentException("Extension name should not be null or empty");
        }
        //先从缓存中获取，如果未命中，新建
        Holder<Object> holder=cachedInstances.get(name);
        if(holder==null){
            cachedInstances.putIfAbsent(name,new Holder<>());
            holder=cachedInstances.get(name);
        }
        //如果Holder还未持有目标对象，则为其创建一个单例对象
        Object instance = holder.get();
        //双重检查锁
        if(instance == null){
            synchronized (holder){
                instance = holder.get();
                if(instance == null){
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * 通过扩展类字节码创建实例对象
     * @param name
     * @return
     */
    private T createExtension(String name) {
        //从文件中加载所有类型为 T 的扩展类并按名称获取特定的扩展类
        Class<?> clazz=getCachedClasses().get(name);
        if (clazz==null){
            throw new RuntimeException("No such extension of name "+name);
        }
        T instance=(T) EXTENSION_INSTANCE.get(clazz);
        if(instance==null){
            try{
                EXTENSION_INSTANCE.putIfAbsent(clazz,clazz.newInstance());
                instance=(T) EXTENSION_INSTANCE.get(clazz);
            }catch (InstantiationException|IllegalAccessException e){
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
        return instance;
    }

    /**
     * 获取所有拓展类
     * @return
     */
    private Map<String,Class<?>> getCachedClasses(){
        //从缓存中获取已经加载的扩展类
        Map<String,Class<?>> classes=cachedClasses.get();
        //双重检查
        if(classes==null){
            synchronized (cachedClasses){
                classes=cachedClasses.get();
                if(classes==null){
                    classes=new HashMap<>();
                    //从配置文件中加载所有扩展类
                    loadDirectory(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * 从配置文件中加载所有扩展类
     * @param extensionsClasses
     */
    private void loadDirectory(Map<String, Class<?>> extensionsClasses){
        String fileName=ExtensionLoader.SERVICE_DIRECTORY+type.getName();
        try{
            //获取配置文件的资源路径
            Enumeration<URL> urls;
            //获取ExtensionLoader的类加载器
            ClassLoader classLoader=ExtensionLoader.class.getClassLoader();
            //用ExtensionLoader的类加载器加载拓展类
            urls=classLoader.getResources(fileName);
            if (urls!=null){
                while (urls.hasMoreElements()) {
                    URL resourceUrl = urls.nextElement();
                    loadResource(extensionsClasses, classLoader, resourceUrl);
                }
            }
        }catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * 通过url加载资源
     * @param extensionClasses
     * @param classLoader
     * @param resourceUrl
     */
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL resourceUrl){
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), StandardCharsets.UTF_8))){
            String line;
            //读取文件中的每一行数据
            while ((line = reader.readLine()) != null) {
                //先排除配置文件中的注释
                final int noteIndex = line.indexOf('#');
                //我们应该忽略掉注释后的内容
                if (noteIndex > 0) {
                    line = line.substring(0, noteIndex);
                }
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        final int keyIndex = line.indexOf('=');
                        String key = line.substring(0, keyIndex).trim();
                        String value = line.substring(keyIndex + 1).trim();
                        if (key.length() > 0 && value.length() > 0) {
                            Class<?> clazz = classLoader.loadClass(value);
                            extensionClasses.put(key, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        logger.error(e.getMessage());
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }
}
