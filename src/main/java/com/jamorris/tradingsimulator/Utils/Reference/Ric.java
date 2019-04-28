package com.jamorris.tradingsimulator.Utils.Reference;

import java.util.regex.Pattern;

public class Ric {
    private String _ric;

    public Ric(String ric) {
        _ric = ric;
    }

    public String getTicker() {
        return _ric.split(Pattern.quote("."))[0];
    }

    public int getExchange() {
        switch (_ric.split(Pattern.quote("."))[1].toLowerCase()) {
            case "l":
                return Exchange.LSE;
            case "n":
                return Exchange.NYSE;
        }

        return Exchange.UNKNOWN;
    }
}
