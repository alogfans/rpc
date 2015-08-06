package com.alogfans.rpc.test;

import com.alogfans.rpc.async.AsyncBehavior;

/**
 * Created by Alogfans on 2015/8/6.
 */
public class Behavior implements AsyncBehavior {
    @Override
    public void onException(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onResponse(Object response) {
        System.out.println((String) response);
    }

    @Override
    public void onTimeout() {

    }
}
