package com.alogfans.rpc.control;

import com.alogfans.rpc.marshal.MarshalHelper;
import com.alogfans.rpc.marshal.RequestPacket;
import com.alogfans.rpc.marshal.ResponsePacket;
import com.alogfans.rpc.stub.Invoker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.BiConsumer;

/**
 * RPC client manager. Its job is to establish the connection with RpcServer,
 * and dispatch the corresponding marshaled object to invokers.
 *
 * Created by Alogfans on 2015/8/5.
 */
public class RpcClient {
    private final int BUFFER_SIZE = 4096;

    private String hostname;
    private int port;
    private int timeout;

    // all invokers we will listening to, for others will just ignore them.
    private HashMap<Class<?>, Invoker> invokerHashMap;

    private SocketChannel socketChannel;
    private Selector selector;
    private Thread backgroundWorker = null;

    public RpcClient() {
        timeout = Integer.MAX_VALUE;
        invokerHashMap = new HashMap<>();
    }

    public RpcClient setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public RpcClient setPort(int port) {
        this.port = port;
        return this;
    }

    public RpcClient setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * Register the given rpc service, and the client will keep track of the response
     * of such invocations.
     * @param invoker new instance of a structure of client-end object for invocation
     * @return the caller RpcClient itself.
     */
    public RpcClient register(Invoker invoker) throws IllegalArgumentException {
        if (invokerHashMap.containsKey(invoker.getInterfaceClass()))
            throw new IllegalArgumentException("Invoker object registered.");

        invokerHashMap.put(invoker.getInterfaceClass(), invoker);
        invoker.setRpcClient(this);
        return this;
    }

    /**
     * Unregister the given rpc service, and the invoker will be collected because it's
     * useless unless registers again.
     * @param invoker instance of a structure of server-end object for invocation currently
     * @return the caller RpcClient itself.
     */
    public RpcClient unregister(Invoker invoker) throws IllegalArgumentException {
        if (!invokerHashMap.containsKey(invoker.getInterfaceClass()))
            throw new IllegalArgumentException("Invoker object not registered.");

        invokerHashMap.remove(invoker.getInterfaceClass(), invoker);
        invoker.setRpcClient(null);
        return this;
    }

    /**
     * Terminate all connections gracefully. Should be the last one
     */
    public void close() {
        if (backgroundWorker.isAlive()) {
            backgroundWorker.interrupt();
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ----- Now comes to the implementation dependent part -----

    public void establishConnection() {
        try {
            prepareNioObjects();
        } catch (IOException e) {
            e.printStackTrace();
        }

        backgroundWorker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (selector.select() > 0) {
                        frameProcess();
                    }
                    // System.out.print("background: terminate");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        // backgroundWorker.setDaemon(true);
        backgroundWorker.start();
    }

    private void prepareNioObjects() throws IOException {
        socketChannel = SocketChannel.open();
        selector = Selector.open();

        socketChannel.socket().setSoTimeout(timeout);
        socketChannel.socket().setReuseAddress(true);

        socketChannel.connect(new InetSocketAddress(hostname, port));

        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    public void frameProcess() throws IOException {
        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            SelectionKey selectionKey = iterator.next();
            iterator.remove();
            if (selectionKey.isReadable()) {
                onReadRequests();
            }
        }
    }

    private void onReadRequests() throws IOException {
        ByteBuffer byteBuffer;
        byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

        byteBuffer.clear();
        int countBytes = socketChannel.read(byteBuffer);
        byteBuffer.flip();

        int cursor = 0;
        while (cursor < countBytes) {
            // TODO: Not considered with split packet!
            int packetLength = byteBuffer.getInt();
            byte[] marshalObject = new byte[packetLength];
            cursor += Integer.BYTES + packetLength;

            byteBuffer.get(marshalObject);
            ResponsePacket responsePacket = null;

            try {
                responsePacket = (ResponsePacket) MarshalHelper.byteToObject(marshalObject);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            dispatchResponsePacket(responsePacket);
        }
    }

    private void dispatchResponsePacket(ResponsePacket responsePacket) {
        if (responsePacket == null)
            return;

        Invoker invoker = invokerHashMap.get(responsePacket.interfaceClass);
        if (invoker == null) {          // is dead, just ignore them.
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                invoker.notifyResponse(responsePacket);
            }
        }).start();
    }

    public void sendRequestPacket(RequestPacket requestPacket) {
        try {
            ByteBuffer byteBuffer;
            byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

            byte[] marshalObject = MarshalHelper.objectToBytes(requestPacket);
            byte[] marshalHeader = MarshalHelper.int32ToBytes(marshalObject.length);
            byteBuffer.clear();
            // System.out.println(marshalObject.length);
            byteBuffer.put(marshalHeader);
            byteBuffer.put(marshalObject);
            byteBuffer.flip();
            socketChannel.write(byteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
