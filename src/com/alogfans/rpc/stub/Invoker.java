package com.alogfans.rpc.stub;

import com.alogfans.rpc.async.ResponseCallbackListener;
import com.alogfans.rpc.async.ResponseFuture;
import com.alogfans.rpc.control.RpcClient;
import com.alogfans.rpc.hook.InvokerHook;
import com.alogfans.rpc.marshal.RequestPacket;
import com.alogfans.rpc.marshal.ResponsePacket;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Redirect methods for executing remotely.
 *
 * Created by Alogfans on 2015/8/5.
 */
public class Invoker implements InvocationHandler {
    private Class<?> interfaceClass;
    private RpcClient rpcClient;
    private Object instance = null;
    private String version;
    private InvokerHook invokerHook = null;


    class WaitChainObject {
        public WaitChainObject(boolean isBlocking) {
            this.isBlocking = isBlocking;
            this.responsePacket = null;
        }

        public WaitChainObject setResponse(ResponsePacket response) {
            responsePacket = response;
            return this;
        }

        public WaitChainObject setResponseCallbackListener(ResponseCallbackListener responseCallbackListener) {
            this.responseCallbackListener = responseCallbackListener;
            return this;
        }

        public boolean isBlocking;
        public ResponsePacket responsePacket; // valid only for blocking
        public ResponseCallbackListener responseCallbackListener;   // valid only for non-blocking
    }

    private CountDownLatch blockLatch = new CountDownLatch(1);
    private HashMap<RequestPacket, WaitChainObject> waitChainObjects;

    public Invoker() {
        waitChainObjects = new HashMap<>();
    }

    public Invoker setInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return this;
    }

    public Invoker setRpcClient(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
        return this;
    }

    public Invoker setVersion(String version) {
        this.version = version;
        return this;
    }

    public Invoker setInvokerHook(InvokerHook invokerHook) {
        this.invokerHook = invokerHook;
        return this;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    public Object getInstance() {
        if (instance == null) {
            instance = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                    new Class[] {this.interfaceClass},
                    this);
        }

        return instance;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;

        RequestPacket requestPacket = new RequestPacket()
                .setInterfaceClass(interfaceClass)
                .setMethod(method)
                .setArgs(args)
                .setVersion(version);

        if (invokerHook != null)
            invokerHook.before(requestPacket);

        rpcClient.sendRequestPacket(requestPacket);

        waitChainObjects.put(requestPacket, new WaitChainObject(true));
        blockLatch.await(rpcClient.getTimeout(), TimeUnit.MILLISECONDS);

        ResponsePacket responsePacket = waitChainObjects.get(requestPacket).responsePacket;
        waitChainObjects.remove(requestPacket);

        result = responsePacket.result;

        if (invokerHook != null)
            invokerHook.after(requestPacket);

        if (responsePacket.exception != null)
            throw responsePacket.exception;

        return result;
    }

    public void asyncInvoke(String methodName) {
        asyncInvoke(methodName, null);
    }

    public void cancelInvoke(String methodName) {
        Iterator<RequestPacket> iterator = waitChainObjects.keySet().iterator();
        while (iterator.hasNext()) {
            RequestPacket requestPacket = iterator.next();
            if (requestPacket.method.equals(methodName) && !waitChainObjects.get(requestPacket).isBlocking) {
                // found async, stop listening it
                waitChainObjects.remove(requestPacket);
            }
        }
    }

    public <T extends ResponseCallbackListener> void asyncInvoke(String methodName, T callbackListener) {
        try {

            Method method = interfaceClass.getMethod(methodName);
            RequestPacket requestPacket = new RequestPacket()
                    .setInterfaceClass(interfaceClass)
                    .setMethod(method)
                    .setArgs(null)
                    .setVersion(version);

            if (invokerHook != null)
                invokerHook.before(requestPacket);

            rpcClient.sendRequestPacket(requestPacket);

            waitChainObjects.put(requestPacket,
                    new WaitChainObject(false).setResponseCallbackListener(callbackListener));

        } catch (Exception e) {
            callbackListener.onException(e);
        }
    }

    public void notifyResponse(ResponsePacket responsePacket) {
        Iterator<RequestPacket> iterator = waitChainObjects.keySet().iterator();
        while (iterator.hasNext()) {
            RequestPacket requestPacket = iterator.next();
            if (responsePacket.isSameSignature(requestPacket)) {
                ResponseCallbackListener responseCallbackListener =
                        waitChainObjects.get(requestPacket).responseCallbackListener;

                if (waitChainObjects.get(requestPacket).isBlocking) {
                    // Sync parsing here
                    waitChainObjects.replace(requestPacket,
                            new WaitChainObject(true).setResponse(responsePacket));
                    blockLatch.countDown();
                    blockLatch = new CountDownLatch(1);
                } else if (responseCallbackListener != null) {
                    responseCallbackListener.onResponse(responsePacket.result);
                    if (responsePacket.exception != null)
                        responseCallbackListener.onException(responsePacket.exception);
                    waitChainObjects.remove(requestPacket);
                }
                return;
            }
        }
    }

}
