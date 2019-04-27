package com.jamorris.tradingsimulator.Utils;

import org.apache.log4j.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/*
    Utility used for managing logger
 */
public class MyLogger {
    private static HashMap<String, Logger> _loggerList = new HashMap<>();

    // Set properties here.
    static  {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HHmmss");
        System.setProperty("current_date", dateFormat.format(new Date()));
        PropertyConfigurator.configure("resources/log4j.properties");
    }

    /**
     * Used for retrieving a logger for the specified class.
     * @param id Class Name
     * @return Logger instance
     */
    private static Logger getLogger(String id) {
        if(_loggerList.containsKey(id)) {
            return _loggerList.get(id);
        }

        _loggerList.put(id, Logger.getLogger(id));
        return _loggerList.get(id);
    }

    /**
     * Identify the calling class
     * @return Name of the class
     */
    private static String getClassName() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String part = stackTraceElements[stackTraceElements.length - 2].toString();
        return part;
    }

    public static void out(String message) {
        out(message, Level.INFO);
    }

    public static void out(String message, Level logLevel) {
        Logger logger = getLogger(getClassName());
        logger.log(logLevel, message);

        if(logLevel == Level.WARN || logLevel == Level.ERROR || logLevel == Level.FATAL)
            System.err.println(getClassName() + ": " + message);
        else
            System.out.println(getClassName() + ": " + message);
    }
}