package com.jamorris.tradingsimulator.Utils.Networking;

import com.jamorris.tradingsimulator.Utils.MyLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

public class Client implements Runnable {
    private InetAddress _hostAddress;
    private int _port;
    private long _id;
    private Selector _selector;
    private ByteBuffer _readBuffer = ByteBuffer.allocate(1024);

    private List<ChangeRequest> _changeRequests = new LinkedList<>();
    private Map _pendingData = new HashMap();
    private Map _rspHandlers = Collections.synchronizedMap(new HashMap());

    public Client(InetAddress hostAddress, int port) throws IOException {
        _hostAddress = hostAddress;
        _port = port;
        _selector = this.initSelector();
    }

    /**
     * Creates a new selector.
     * @return Created selector
     * @throws IOException
     */
    private Selector initSelector() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    public void run() {
        while(true) {
            try {
                processChanges();

                // Wait for an event on one of our channels.
                _selector.select();

                // Iterate through the keys where events are available.
                Iterator selectedKeys = _selector.selectedKeys().iterator();
                while(selectedKeys.hasNext()) {
                    // Pull the next key.
                    SelectionKey key = (SelectionKey)selectedKeys.next();
                    selectedKeys.remove();

                    if(!key.isValid())
                        continue;

                    if(key.isConnectable()) {
                        finishConnection(key);
                    }
                    else if(key.isReadable()) {

                        MyLogger.out("readable");
                    }
                    else if(key.isWritable()) {

                        MyLogger.out("writable");
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private SocketChannel initiateConnection() throws IOException {
        // Create a non-blocking socket channel.
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        // Start connection with establishment.
        socketChannel.connect(new InetSocketAddress(_hostAddress, _port));

        // Queue a channel registration since the caller is not the selecting thread.
        // As part of the registration, we'll register an interest in connection events.
        // These are raised when a channel is ready to complete connection establishment.
        synchronized (_changeRequests) {
            _changeRequests.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
        }

        return socketChannel;
    }

    private void finishConnection(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel)key.channel();

        try {
            socketChannel.finishConnect();
        } catch(Exception e){
            e.printStackTrace();
            return;
        }

        // Register an interest in writing to this channel.
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void processChanges() throws ClosedChannelException {

    }

    public void send(byte[] data, RspHandler handler) throws IOException {
        // Start a new connection
        SocketChannel socket = initiateConnection();

        // Register the response handler.
        _rspHandlers.put(socket, handler);

        // Queue data to be written.
        synchronized (_pendingData) {
            List queue = (List)_pendingData.get(socket);
            if(queue == null) {
                queue = new ArrayList();
                _pendingData.put(socket, queue);
            }
            queue.add(ByteBuffer.wrap(data));
        }

        // Finally wake up our selecting thread so that it can make the required changes.
        _selector.wakeup();
    }
}
