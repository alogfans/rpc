package com.alogfans.rpc.async;

import com.alogfans.rpc.marshal.ResponsePacket;

import java.lang.reflect.Method;

/**
 * Interface for the arrival of async call response.
 * Created by Alogfans on 2015/8/5.
 */
public interface AsyncBehavior {
    void onResponse(Object response);
    void onTimeout();
    void onException(Exception e);
}
