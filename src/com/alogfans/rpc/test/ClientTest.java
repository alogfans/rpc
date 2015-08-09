package com.alogfans.rpc.test;

import com.alogfans.rpc.async.ResponseCallbackListener;
import com.alogfans.rpc.control.RpcClient;
import com.alogfans.rpc.stub.Invoker;

import java.util.Date;

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
        rpcClient.establishConnection();

        ISayHello sayHello = (ISayHello) invoker.getInstance();

        int countHints = 0;

        Date startTime = new Date();

        while (countHints < 1000) {
            sayHello.sayHello();
            countHints++;
        }

        System.out.println(new Date().getTime() - startTime.getTime());
        rpcClient.close();
    }
}
