package com.jamorris.tradingsimulator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jamorris.tradingsimulator.Utils.Reference.Exchange;
import com.jamorris.tradingsimulator.Utils.Reference.Ric;
import org.junit.Test;

public class RicTest {
    @Test
    public void testTickerName()
    {
        Ric ric = new Ric("VOD.L");
        String expected = "VOD";
        assertEquals(ric.getTicker(), expected);
    }

    @Test
    public void testExchangeLSE() {
        Ric ric = new Ric("VOD.L");
        assertEquals(ric.getExchange(), Exchange.LSE);
    }

    @Test
    public void testExchangeNYSE() {
        Ric ric = new Ric("IBM.N");
        assertEquals(ric.getExchange(), Exchange.NYSE);
    }

    @Test
    public void testExchangeUnknown() {
        Ric ric = new Ric("IBM.F");
        assertEquals(ric.getExchange(), Exchange.UNKNOWN);
    }
}
