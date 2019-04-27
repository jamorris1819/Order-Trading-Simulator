package com.jamorris.tradingsimulator.Utils.Networking;

import java.nio.channels.SocketChannel;

public class ServerDataEvent {
    public Server _server;
    public SocketChannel _socketChannel;
    public byte[] _data;

    public ServerDataEvent(Server server, SocketChannel socket, byte[] data) {
        _server = server;
        _socketChannel = socket;
        _data = data;
    }
}
