package com.alogfans.rpc.marshal;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created by Alogfans on 2015/8/5.
 */
public class ResponsePacket implements Serializable {
    private static final long serialVersionUID = 6238589897120159526L;

    public Class<?> interfaceClass;
    public String version;
    public String method;
    public Class<?>[] argTypes;
    public Object result = null;
    public Exception exception = null;

    public ResponsePacket copyFromRequest(RequestPacket requestPacket) {
        this.interfaceClass = requestPacket.interfaceClass;
        this.version = requestPacket.version;
        this.method = requestPacket.method;
        this.argTypes = requestPacket.argTypes;

        return this;
    }

    public ResponsePacket setException(Exception exception) {
        this.exception = exception;
        return this;
    }

    public ResponsePacket setResult(Object result) {
        this.result = result;
        return this;
    }

    public Method getMethod() throws NoSuchMethodException {
        return interfaceClass.getMethod(method, argTypes);
    }

    public boolean isSameSignature(RequestPacket requestPacket) {
        if (!this.interfaceClass.equals(requestPacket.interfaceClass))
            return false;
        if (!this.method.equals(requestPacket.method))
            return false;
        if (!Arrays.equals(this.argTypes, requestPacket.argTypes))
            return false;
        if (!this.version.equals(requestPacket.version))
            return false;

        return true;
    }
}
