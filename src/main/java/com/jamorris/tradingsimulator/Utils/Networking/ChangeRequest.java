package com.jamorris.tradingsimulator.Utils.Networking;

import java.nio.channels.SocketChannel;

public class ChangeRequest {
    public static final int REGISTER = 1;
    public static final int CHANGEOPS = 2;

    public SocketChannel _socket;
    public int _type;
    public int _ops;

    public ChangeRequest(SocketChannel socket, int type, int ops) {
        _socket = socket;
        _type = type;
        _ops = ops;
    }
}
