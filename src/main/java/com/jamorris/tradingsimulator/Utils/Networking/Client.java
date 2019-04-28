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
    private SocketChannel _socketChannel = null;
    private final List<ChangeRequest> _changeRequests = new LinkedList<>();
    private final Map<SocketChannel, List> _pendingData = new HashMap<>();
    private final Map<SocketChannel, RspHandler> _rspHandlers = Collections.synchronizedMap(new HashMap());

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
                        read(key);
                    }
                    else if(key.isWritable()) {
                        write(key);
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Initiate a socket channel and connects.
     * @return SocketChannel
     * @throws IOException
     */
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

    private SocketChannel getSocketChannel() throws IOException {
        if(_socketChannel == null) {
            _socketChannel = initiateConnection();
        }
        return _socketChannel;
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

    /**
     * Process any change requests.
     * @throws ClosedChannelException
     */
    private void processChanges() throws ClosedChannelException {
        // Process pending changes.
        synchronized (_changeRequests) {
            Iterator changes = _changeRequests.iterator();
            while(changes.hasNext()) {
                ChangeRequest request = (ChangeRequest) changes.next();
                switch(request._type) {
                    case ChangeRequest.CHANGEOPS:
                        SelectionKey key = request._socket.keyFor(_selector);
                        key.interestOps(request._ops);
                        break;
                    case ChangeRequest.REGISTER:
                        request._socket.register(_selector, request._ops);
                        break;
                }
            }
            this._changeRequests.clear();
        }
    }

    public void send(byte[] data, RspHandler handler) throws IOException {
        // Start a new connection
        SocketChannel socket = initiateConnection();

        // Register the response handler.
        if(!_rspHandlers.containsKey(socket))
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

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel)key.channel();

        // Clear out the buffer so that it's ready for more.
        _readBuffer.clear();

        // Attempt to read from the channel.
        int numRead = -1;
        try {
            numRead = socketChannel.read(_readBuffer);
        } catch (IOException e){
            // Connected entity forcibly shut the connection.
            // Close the channel.
            key.cancel();
            socketChannel.close();
            return;
        }

        if(numRead == -1) {
            // Remote entity shut the socket properly.
            // We will do the same.
            key.channel().close();
            key.cancel();
            return;
        }

        handleResponse(socketChannel, _readBuffer.array(), numRead, key);
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (_pendingData){
            List queue = (List)_pendingData.get(socketChannel);

            // Write until there is no more data available.
            while(!queue.isEmpty()) {
                ByteBuffer buffer = (ByteBuffer)queue.get(0);
                socketChannel.write(buffer);
                if(buffer.remaining() > 0) {
                    // Socket's buffer is full.
                    break;
                }
                queue.remove(0);
            }

            if(queue.isEmpty()) {
                // All data has been written, so this socket no longer needs to write.
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    private void handleResponse(SocketChannel socketChannel, byte[] data, int numRead, SelectionKey key) throws IOException {
        // Make a copy of the data before handing it to the client.
        byte[] rspData = new byte[numRead];
        System.arraycopy(data, 0, rspData, 0, numRead);

        // Look up the handler for this channel.
        RspHandler handler = (RspHandler)_rspHandlers.get(socketChannel);

        // Pass the response to it.
        if(handler.handleResponse(rspData)) {
            // The handler is finished. Close connection.
            socketChannel.close();
            socketChannel.keyFor(_selector).cancel();
        }
    }
}
