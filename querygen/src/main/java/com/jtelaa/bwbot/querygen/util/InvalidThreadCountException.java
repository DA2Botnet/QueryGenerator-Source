package com.jtelaa.bwbot.querygen.util;

/**
 * Exception that is thrown whenever the thread manager mistakenly 
 * parses the number assoicated with the desired thread count
 * 
 * @author Joseph
 */

public class InvalidThreadCountException extends Exception {

    /**
     * Throws and invalid thread count exception with a passed in message
     * 
     * @param message Message to pass
     */

    public InvalidThreadCountException(String message) {
        super(message);

    }

    /**
     * Throws and invalid thread count exception and displays the error value
     * 
     * @param thread_count Error value
     */

    public InvalidThreadCountException(int thread_count) {
        super("Invalid number of threads (" + thread_count + ")");

    }

    /**
     * Throws an invalid thread count exception
     */

    public InvalidThreadCountException() {
        super("Invalid number of threads");

    }
}