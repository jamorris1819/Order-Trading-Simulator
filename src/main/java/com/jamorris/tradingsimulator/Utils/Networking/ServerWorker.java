package com.jamorris.tradingsimulator.Utils.Networking;

import com.jamorris.tradingsimulator.Utils.MyLogger;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

public class ServerWorker implements Runnable {
    protected List<ServerDataEvent> _queue = new LinkedList<>();

    public void processData(Server server, SocketChannel socket, byte[] data, int count) {
        byte[] dataCopy = new byte[count];
        System.arraycopy(data, 0, dataCopy, 0, count);
        synchronized (_queue) {
            _queue.add(new ServerDataEvent(server, socket, dataCopy));
            _queue.notify();
        }
    }

    public void start() {
        new Thread().start();
    }

    public void run() {
        ServerDataEvent dataEvent;

        while(true) {
            synchronized (_queue) {
                while(_queue.isEmpty()) {
                    try {_queue.wait(); } catch (InterruptedException e) {}
                }
                dataEvent = _queue.remove(0);
            }
            MyLogger.out("Message being processed");
            //dataEvent._server.send(dataEvent._socketChannel, dataEvent._data);
        }
    }
}