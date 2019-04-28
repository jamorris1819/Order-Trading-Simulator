package com.jamorris.tradingsimulator;

import com.jamorris.tradingsimulator.Utils.MyLogger;
import com.jamorris.tradingsimulator.Utils.Networking.Client;
import com.jamorris.tradingsimulator.Utils.Networking.RspHandler;
import com.jamorris.tradingsimulator.Utils.Networking.Server;
import com.jamorris.tradingsimulator.Utils.Networking.ServerWorker;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {
        ServerWorker serverWorker = new ServerWorker();
        new Thread(serverWorker).start();
        new Thread(new Server(null, 2000, serverWorker)).start();
        Client c1 = new Client(InetAddress.getByName("localhost"), 2000);Thread t = new Thread(c1);
        t.setDaemon(true);
        t.start();

        RspHandler rsp = new RspHandler();
        RspHandler rsp2 = new RspHandler();

        c1.send("hello".getBytes(), rsp);
        c1.send("hello1".getBytes(), rsp2);

        String a = rsp.waitForResponse();
        MyLogger.out(a);
        a = rsp2.waitForResponse();
        MyLogger.out(a);

        MyLogger.out("Current threads: " + ManagementFactory.getThreadMXBean().getThreadCount());
    }
}

