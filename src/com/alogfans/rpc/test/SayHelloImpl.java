package com.alogfans.rpc.test;

/**
 * Implementation of ISayHello, access in server only
 *
 * Created by Alogfans on 2015/8/5.
 */
public class SayHelloImpl implements ISayHello {
    @Override
    public String sayHello() {
        return "Hello, world";
    }
}
