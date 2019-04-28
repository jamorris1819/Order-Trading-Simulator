package com.jamorris.tradingsimulator.Utils.Networking;

public class RspHandler {
    private byte[] _rsp = null;

    public synchronized boolean handleResponse(byte[] rsp) {
        _rsp = rsp;
        this.notify();
        return true;
    }

    public synchronized String waitForResponse() {
        while(_rsp == null) {
            try {
                this.wait();
            } catch (InterruptedException e) { }
        }

        String response = new String(_rsp);
        _rsp = null;
        return response;
    }
}
