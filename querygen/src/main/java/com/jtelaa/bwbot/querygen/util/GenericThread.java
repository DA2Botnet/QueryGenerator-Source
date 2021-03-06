package com.jtelaa.bwbot.querygen.util;

/**
 * Abstractionn for the threads/processes in this application.
 * 
 * @author Joseph
 */

public class GenericThread extends Thread {

    // ------------------------- Thread Control

    /** Boolean to control the thread */
    public boolean run = true;

    /** Stops the thread */
    public synchronized void stopThread() { run = false; }

    /** Checks if the thread is ready */
    public synchronized boolean threadReady() { return run; }
    
}
