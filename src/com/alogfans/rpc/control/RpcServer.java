package com.alogfans.rpc.control;

import com.alogfans.rpc.marshal.MarshalHelper;
import com.alogfans.rpc.marshal.RequestPacket;
import com.alogfans.rpc.marshal.ResponsePacket;
import com.alogfans.rpc.stub.Provider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

/**
 * The server that provides calling services. Several provided objected can be
 * published for invoking remotely. The callings are async and use by NIO mechanism.
 * (Non-blocking socket I/O)
 *
 * Created by Alogfans on 2015/8/5.
 */
public class RpcServer {
    private final int BUFFER_SIZE = 4096;

    private int port;
    private int timeout;

    // all provider we will listening to, for others will just ignore them.
    private HashMap<Class<?>, Provider> rpcProviderHashMap;

    // NIO implementation related elements
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    public RpcServer() {
        timeout = Integer.MAX_VALUE;
        rpcProviderHashMap = new HashMap<>();
    }

    public RpcServer setPort(int port) {
        this.port = port;
        return this;
    }

    public RpcServer setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Register the given rpc service, and the server will keep track of the request
     * of such invocations.
     * @param provider new instance of a structure of server-end object for invocation
     * @return the caller RpcServer itself.
     */
    public RpcServer register(Provider provider) throws IllegalArgumentException {
        if (rpcProviderHashMap.containsKey(provider.getInterfaceClass()))
            throw new IllegalArgumentException("Provider object registered.");

        rpcProviderHashMap.put(provider.getInterfaceClass(), provider);
        provider.setRpcServer(this);
        return this;
    }

    /**
     * Unregister the given rpc service, and the provider will be collected because it's
     * useless unless registers again.
     * @param provider instance of a structure of server-end object for invocation currently
     * @return the caller RpcServer itself.
     */
    public RpcServer unregister(Provider provider) throws IllegalArgumentException {
        if (!rpcProviderHashMap.containsKey(provider.getInterfaceClass()))
            throw new IllegalArgumentException("Provider object not registered.");

        rpcProviderHashMap.remove(provider.getInterfaceClass());
        provider.setRpcServer(null);
        return this;
    }

    // ----- Now comes to the implementation dependent part -----

    /**
     * Prepare NIO objects and start processing looping in current thread. Never exit unless
     * abortion because of exception.
     */
    public void startService() {
        try {
            prepareNioObjects();
            while (selector.select() > 0) {
                frameProcess();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareNioObjects() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        selector = Selector.open();

        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().setReuseAddress(true);

        serverSocketChannel.bind(new InetSocketAddress(port));

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void frameProcess() throws IOException {
        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            SelectionKey selectionKey = iterator.next();
            iterator.remove();
            if (selectionKey.isAcceptable()) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                socketChannel.register(selector, SelectionKey.OP_READ);
            }

            if (selectionKey.isReadable()) {
                SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                onReadRequests(socketChannel);
            }
        }
    }

    public void writeResponsePacket(SocketChannel socketChannel, ResponsePacket responsePacket) {
        try {
            ByteBuffer byteBuffer;
            byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

            byte[] marshalObject = MarshalHelper.objectToBytes(responsePacket);
            byte[] marshalHeader = MarshalHelper.int32ToBytes(marshalObject.length);
            byteBuffer.clear();
            byteBuffer.put(marshalHeader);
            byteBuffer.put(marshalObject);
            byteBuffer.flip();
            socketChannel.write(byteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onReadRequests(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer;
        byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

        byteBuffer.clear();
        socketChannel.read(byteBuffer);
        byteBuffer.flip();

        int countBytes = byteBuffer.limit();
        if (countBytes == 0) {
            socketChannel.close();
        }

        int cursor = 0;

        // System.out.println(countBytes);

        while (cursor < countBytes) {
            // TODO: Not considered with slitted packet!
            byte[] maeshalHeader = new byte[Integer.BYTES];
            byteBuffer.get(maeshalHeader);
            int packetLength = MarshalHelper.bytesToInt32(maeshalHeader);

            byte[] marshalObject = new byte[packetLength];
            cursor += Integer.BYTES + packetLength;

            byteBuffer.get(marshalObject);
            RequestPacket requestPacket = null;

            try {
                requestPacket = (RequestPacket) MarshalHelper.byteToObject(marshalObject);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            dispatchRequestPacket(socketChannel, requestPacket);
        }
    }

    private void dispatchRequestPacket(SocketChannel socketChannel, RequestPacket requestPacket) {
        if (requestPacket == null)
            return;

        Provider provider = rpcProviderHashMap.get(requestPacket.interfaceClass);
        if (provider == null) {
            writeResponsePacket(socketChannel,
                    new ResponsePacket()
                            .copyFromRequest(requestPacket)
                            .setException(new ClassNotFoundException("Such class not provided")));
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                ResponsePacket responsePacket = provider.invoke(requestPacket);
                writeResponsePacket(socketChannel, responsePacket);
            }
        }).start();
    }
}
