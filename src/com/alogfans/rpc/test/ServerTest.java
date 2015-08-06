package com.alogfans.rpc.test;

import com.alogfans.rpc.control.RpcServer;
import com.alogfans.rpc.stub.Provider;

/**
 * Server test drive
 *
 * Created by Alogfans on 2015/8/5.
 */
public class ServerTest {
    public static void main(String[] args) {
        Provider provider = new Provider()
                .setInterfaceClass(ISayHello.class)
                .setInstance(new SayHelloImpl())
                .setVersion("1.0.0");

        RpcServer rpcServer = new RpcServer()
                .setPort(10086)
                .setTimeout(3000)
                .register(provider);

        rpcServer.startService();
    }
}
