package com.jtelaa.bwbot.querygen.util

import kotlin.jvm.Synchronized

/**
 * Abstractionn for the threads/processes in this application.
 *
 * @author Joseph
 */

open class GenericThread : Thread() {
    // ------------------------- Thread Control

    /** Boolean to control the thread  */
    var run = true

    /** Stops the thread  */
    @Synchronized
    fun stopThread() {
        run = false

    }

    /** Checks if the thread is ready  */
    @Synchronized
    fun threadReady(): Boolean {
        return run
        
    }
}