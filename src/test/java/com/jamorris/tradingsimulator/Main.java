package com.jamorris.tradingsimulator;

import com.jamorris.tradingsimulator.Utils.Networking.Client;
import com.jamorris.tradingsimulator.Utils.Networking.RspHandler;
import com.jamorris.tradingsimulator.Utils.Networking.Server;
import com.jamorris.tradingsimulator.Utils.Networking.ServerWorker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {
        ServerWorker serverWorker = new ServerWorker();
        new Thread(serverWorker).start();
        new Thread(new Server(null, 2000, serverWorker)).start();
        Client c1 = new Client(InetAddress.getByName("localhost"), 2000);
        c1.send("hello".getBytes(), new RspHandler());
    }
}

