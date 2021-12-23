package com.jtelaa.bwbot.querygen.util

/**
 * Exception that is thrown whenever the thread manager mistakenly
 * parses the number assoicated with the desired thread count
 *
 * @author Joseph
 */
 
class InvalidThreadCountException : Exception {
    /**
     * Throws and invalid thread count exception with a passed in message
     *
     * @param message Message to pass
     */

    constructor(message: String?) : super(message) {}

    /**
     * Throws and invalid thread count exception and displays the error value
     *
     * @param thread_count Error value
     */

    constructor(thread_count: Int) : super("Invalid number of threads ($thread_count)") {}

    /**
     * Throws an invalid thread count exception
     */

    constructor() : super("Invalid number of threads") {}

}