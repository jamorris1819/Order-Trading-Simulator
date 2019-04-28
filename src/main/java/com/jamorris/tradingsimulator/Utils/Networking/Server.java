package com.jamorris.tradingsimulator.Utils.Networking;

import com.jamorris.tradingsimulator.Utils.MyLogger;
import com.sun.beans.editors.ByteEditor;
import org.apache.log4j.Level;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

public class Server implements Runnable {
    private InetAddress _hostAddress;
    private int _port;
    private ServerSocketChannel _serverChannel;
    private Selector _selector;
    private ByteBuffer _readBuffer = ByteBuffer.allocate(1024);
    private final Map _pendingData = new HashMap();
    private ServerWorker _serverWorker;
    private final List _changeRequests = new LinkedList();


    public Server(InetAddress hostAddress, int port, ServerWorker serverWorker) throws IOException {
        _hostAddress = hostAddress;
        _port = port;
        _serverWorker = serverWorker;
        _selector = this.initSelector();
    }

    private Selector initSelector() throws IOException {
        // Create a selector.
        Selector socketSelector = SelectorProvider.provider().openSelector();

        // Create a non-blocking server socket channel.
        _serverChannel = ServerSocketChannel.open();
        _serverChannel.configureBlocking(false);

        // Bind the server socket to the specified address and port.
        InetSocketAddress socketAddress = new InetSocketAddress(_hostAddress, _port);
        _serverChannel.socket().bind(socketAddress);

        // Register the socket channel, so that new connections can be accepted.
        _serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        return socketSelector;
    }

    public void run() {
        MyLogger.out("Server started on " + _hostAddress + ":" + _port);
        while(true) {
            try {
                processChanges();

                // Wait for an event on one of our channels.
                _selector.select();

                // Iterate through the keys where events are available.
                Iterator selectedKeys = _selector.selectedKeys().iterator();
                while(selectedKeys.hasNext()) {
                    // Pull the first key from the list.
                    SelectionKey key = (SelectionKey)selectedKeys.next();
                    selectedKeys.remove();

                    if(!key.isValid())
                        continue;

                    // Incoming connection.
                    if(key.isAcceptable()) {
                        acceptKey(key);
                    }
                    // Data to be read.
                    else if(key.isReadable()) {
                        readKey(key);
                    }
                    // Data to be written.
                    else if(key.isWritable()) {
                        writeKey(key);
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Accepts an incoming connection and stores for updates.
     * @param key The selection key
     * @throws IOException
     */
    private void acceptKey(SelectionKey key) throws IOException {
        // For an accept to be pending, the channel must be a server socket channel.
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();

        // Accept the incoming connection and make it non-blocking.
        SocketChannel socketChannel = serverSocketChannel.accept();
        Socket socket = socketChannel.socket();
        socketChannel.configureBlocking(false);

        // Register the new SocketChannel with our selector.
        // This means we'll be notified when there is new data to read.
        socketChannel.register(_selector, SelectionKey.OP_READ);
        MyLogger.out("A client (" + socket.getInetAddress().toString() + ") has established a connection to the server.");
    }

    /**
     * Reads an incoming connection and passes data to worker.
     * @param key The selection key
     * @throws IOException
     */
    private void readKey(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel)key.channel();

        // Clear the buffer prior to data read.
        _readBuffer.clear();

        // Attempt to read from the channel.
        int numRead = -1;
        try {
            numRead = socketChannel.read(_readBuffer);
        } catch (Exception e) {
            // Connected entity forcibly closed the connection.
            // Close the channel.
            key.cancel();
            socketChannel.close();
            MyLogger.out("Client forcibly closed connection to the server.");
            return;
        }

        if(numRead == -1) {
            // Connected entity shut the socket properly.
            // We will do the same.
            key.channel().close();
            key.cancel();
            MyLogger.out("Client has disconnected from the server");
            return;
        }


        MyLogger.out("Server has received a message");

        // Pass the received data to the worker.
        _serverWorker.processData(this, socketChannel, _readBuffer.array(), numRead);
    }

    public void send(SocketChannel socket, byte[] data) {
        synchronized (_changeRequests) {
            // Indicate that we want the interest ops set changed.
            _changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
            // Queue the data which we want written.
            synchronized (_pendingData) {
                List queue = (List)_pendingData.get(socket);
                if(queue == null) {
                    queue = new ArrayList();
                    _pendingData.put(socket, queue);
                }
                queue.add(ByteBuffer.wrap(data));
            }
        }
        MyLogger.out("Server has sent response to client.");

        // Finally wake up our selecting thread so that it can make required changes.
        _selector.wakeup();
    }

    /**
     * Writes a buffer into a channel
     * @param key The selection key
     * @throws IOException
     */
    private void writeKey(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (_pendingData) {
            List queue = (List)_pendingData.get(socketChannel);
            ByteBuffer buffer = null;

            // Write until there is no more data available.
            while(!queue.isEmpty()) {
                buffer = (ByteBuffer)queue.get(0);
                int i = socketChannel.write(buffer);
                if(buffer.remaining() > 0) {
                    // Socket's buffer is full.
                    //buffer.clear();
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

    private void processChanges() {
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
                }
            }
            this._changeRequests.clear();
        }
    }
}
