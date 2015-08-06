package com.alogfans.rpc.marshal;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Created by Alogfans on 2015/8/1.
 */
public class RequestPacket implements Serializable {
    public String version;
    public Class<?> interfaceClass;
    public String method;
    public Class<?>[] argTypes;
    public Object[] args;

    public RequestPacket setVersion(String version) {
        this.version = version;
        return this;
    }

    public RequestPacket setInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return this;
    }

    public RequestPacket setMethod(Method method) {
        this.method = method.getName();
        this.argTypes = method.getParameterTypes();
        return this;
    }

    public Method getMethod() throws NoSuchMethodException {
        return interfaceClass.getMethod(method, argTypes);
    }

    public RequestPacket setArgs(Object[] args) {
        this.args = args;
        return this;
    }
}
