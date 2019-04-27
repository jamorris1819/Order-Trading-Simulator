package com.jamorris.tradingsimulator.Utils.Networking;

import com.jamorris.tradingsimulator.Utils.MyLogger;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Server implements Runnable {
    private InetAddress _hostAddress;
    private int _port;
    private ServerSocketChannel _serverChannel;
    private Selector _selector;
    private ByteBuffer _readBuffer = ByteBuffer.allocate(1024);
    private Map _pendingData = new HashMap();
    private ServerWorker _serverWorker;

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
                // TODO: Data processing

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
                        MyLogger.out("readable");
                    }
                    // Data to be written.
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

    /**
     * Accepts an incoming connection and stores for udpates.
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

    private void processChanges() {

    }
}
