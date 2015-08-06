package com.alogfans.rpc.stub;

import com.alogfans.rpc.control.RpcServer;
import com.alogfans.rpc.hook.ProviderHook;
import com.alogfans.rpc.marshal.RequestPacket;
import com.alogfans.rpc.marshal.ResponsePacket;

import java.lang.reflect.Method;

/**
 * Expose a given interface for invoking, as a result invokers will communicate with
 * it through the dispatch of RpcServer.
 *
 * Created by Alogfans on 2015/8/5.
 */
public class Provider {
    private Class<?> interfaceClass;
    private String version;
    private Object instance;
    private RpcServer rpcServer;
    private ProviderHook providerHook = null;

    public Provider setInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return this;
    }

    public Provider setVersion(String version) {
        this.version = version;
        return this;
    }

    public Provider setRpcServer(RpcServer rpcServer) {
        this.rpcServer = rpcServer;
        return this;
    }

    public Provider setProviderHook(ProviderHook providerHook) {
        this.providerHook = providerHook;
        return this;
    }

    public Provider setInstance(Object instance) {
        this.instance = instance;
        return this;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    public ResponsePacket invoke(RequestPacket requestPacket) {
        ResponsePacket responsePacket = new ResponsePacket()
                .copyFromRequest(requestPacket);

        if (providerHook != null) {
            providerHook.before(responsePacket);
        }
        try {
            Method method = requestPacket.getMethod();
            responsePacket.result = method.invoke(instance, requestPacket.args);
        } catch (Exception e) {
            responsePacket.exception = e;
        }
        if (providerHook != null) {
            providerHook.after(responsePacket);
        }
        return responsePacket;
    }

    public void close() {
        rpcServer.unregister(this);
    }
}
