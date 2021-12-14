package com.jtelaa.bwbot.querygen.util;

/**
 * 
 */

public class InvalidThreadCountException extends Exception {

    /**
     * 
     * @param message
     */

    public InvalidThreadCountException(String message) {
        super(message);

    }

    /**
     * 
     * @param thread_count
     */

    public InvalidThreadCountException(int thread_count) {
        super("Invalid number of threads (" + thread_count + ")");

    }

    /**
     * 
     */

    public InvalidThreadCountException() {
        super("Invalid number of threads");

    }
}