package com.alogfans.rpc.hook;

import com.alogfans.rpc.marshal.RequestPacket;

/**
 * Created by Alogfans on 2015/8/5.
 */
public interface InvokerHook {
    void before(RequestPacket requestPacket);
    void after(RequestPacket requestPacket);
}
