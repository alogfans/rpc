package com.alogfans.rpc.async;

import com.alogfans.rpc.marshal.ResponsePacket;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Alogfans on 2015/8/7.
 */
public class ResponseFuture {
    public static ThreadLocal<Future<Object>> futureThreadLocal = new ThreadLocal<>();

    public static Object getResponse(long timeout) throws InterruptedException {
        if (null == futureThreadLocal.get()) {
            throw new RuntimeException("Thread [" + Thread.currentThread() + "] have not set the response future!");
        }

        try {
            ResponsePacket response = (ResponsePacket) (futureThreadLocal.get().get(timeout, TimeUnit.MILLISECONDS));
            if (response.exception != null) {
                throw new RuntimeException(response.exception);
            }
            return response.result;

        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Time out", e);
        }
    }

    public static void setFuture(Future<Object> future){
        futureThreadLocal.set(future);
    }
}
