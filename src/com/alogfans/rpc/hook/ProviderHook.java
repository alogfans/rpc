package com.alogfans.rpc.hook;

import com.alogfans.rpc.marshal.ResponsePacket;

/**
 * Created by Alogfans on 2015/8/5.
 */
public interface ProviderHook {
    void before(ResponsePacket responsePacket);
    void after(ResponsePacket responsePacket);
}
