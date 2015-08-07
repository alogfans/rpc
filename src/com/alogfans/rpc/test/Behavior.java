package com.alogfans.rpc.test;

import com.alogfans.rpc.async.ResponseCallbackListener;

/**
 * Created by Alogfans on 2015/8/6.
 */
public class Behavior implements ResponseCallbackListener {
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
