package com.netty.rpc.serializer.hessian;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.netty.rpc.serializer.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 继承自 Serializer 抽象类,实现了对象的序列化和反序列化
 */

public class Hessian1Serializer implements Serializer {

    @Override
    public <T> byte[] serialize(T obj) {//接受一个对象 obj 作为输入,将其序列化为字节数组
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        HessianOutput ho = new HessianOutput(os);
        try {
            ho.writeObject(obj);
            ho.flush();
            byte[] result = os.toByteArray();
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                ho.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                os.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public <T> Object deserialize(byte[] bytes, Class<T> clazz) {
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        HessianInput hi = new HessianInput(is);
        try {
            Object result = hi.readObject();
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                hi.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
