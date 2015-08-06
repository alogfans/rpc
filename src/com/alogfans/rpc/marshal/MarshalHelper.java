package com.alogfans.rpc.marshal;

import java.io.*;

/**
 * Marshal helper class, which allows serialize objects (however it's NOT portable via languages)
 *
 * Created by Alogfans on 2015/8/5.
 */
public class MarshalHelper {
    /**
     * Convert serializable object to byte flows
     * @param object the object for marshal
     * @return byte data that could be trans through socket
     * @throws IOException
     */
    public static byte[] objectToBytes(Object object) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();

        objectOutputStream.close();
        byteArrayOutputStream.close();

        return bytes;
    }

    /**
     * Convert byte flows to serializable object
     * @param bytes serialized bytes which was produced by <code>objectToBytes()</code> in the other
     *              part
     * @return the original object
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object byteToObject(byte[] bytes) throws IOException, ClassNotFoundException {
        Object object = null;
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        object = objectInputStream.readObject();

        objectInputStream.close();
        byteArrayInputStream.close();

        return object;
    }

    /**
     * Convert INT32 to Little-endian order
     * @param value integer value
     * @return associated byte array which is little endian
     */
    public static byte[] int32ToBytes(int value) {
        return new byte[] {
                (byte) ((value >> 24) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }

    public static int bytesToInt32(byte[] value) {
        return value[3] & 0xFF |
                (value[2] & 0xFF) << 8 |
                (value[1] & 0xFF) << 16 |
                (value[0] & 0xFF) << 24;
    }
}
