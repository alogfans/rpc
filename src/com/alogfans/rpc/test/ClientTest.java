package com.alogfans.rpc.test;

import com.alogfans.rpc.async.AsyncBehavior;
import com.alogfans.rpc.control.RpcClient;
import com.alogfans.rpc.stub.Invoker;

/**
 * Client test drive
 *
 * Created by Alogfans on 2015/8/5.
 */
public class ClientTest {
    public static void main(String[] args) {
        Invoker invoker = new Invoker()
                .setInterfaceClass(ISayHello.class)
                .setVersion("1.0.0");

        RpcClient rpcClient = new RpcClient()
                .setHostname("127.0.0.1")
                .setPort(10086)
                .register(invoker);

        AsyncBehavior asyncBehavior = new Behavior();

        rpcClient.establishConnection();
        invoker.asyncInvoke("sayHello", asyncBehavior);

        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        rpcClient.close();
    }
}
