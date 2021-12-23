package com.jtelaa.bwbot.querygen.processes

import java.util.Random

/**
 * This process accepts the requests for the query.
 * It enques them to the query server if the request is valid.
 *
 * @since 2
 * @author Joseph
 *
 * @see com.jtelaa.da2.querygen.QueryServer.java
 *
 * @see com.jtelaa.da2.querygen.QueryGenerator.java
 */

class RequestServer : GenericThread {
    // ------------------------- Constructors

    /**
     * Program init
     */

    constructor() {
        log_prefix = std_log_prefix
        this.setName(log_prefix)
        log_prefix += ": "

    }

    /**
     * Program init
     *
     * @param id thread id
     */

    constructor(id: Int) {
        log_prefix = std_log_prefix + "(" + id + ")"
        this.setName(log_prefix)
        log_prefix += ":"

    }

    // ------------------------- Thread Control

    /** Stops the thread  */
    @Synchronized
    fun stopServer() {
        run = false

    }

    /** Checks if the thread is ready  */
    @Synchronized
    fun serverReady(): Boolean {
        return run

    }

    // ------------------------- Logging

    /** Logging prefix  */
    var log_prefix: String

    // ------------------------- Thread Processes

    /**
     * Thread code
     */

    fun run() {
        // Setup server
        val server = ServerUDP(BWPorts.QUERY_REQUEST.checkForPreset(App.my_config, "request_port"), log_prefix, ConsoleColors.GREEN)

        // Bot address var
        var bot_address: String

        // If server is ready
        if (server.startServer()) {
            while (run) {
                // If a request message is receiver
                val response: String = server.getMessage()
                
                try {
                    // If message is a request
                    if (response.contains(BWMessages.QUERY_REQUEST_MESSAGE.getMessage())) {
                        bot_address = server.getClientAddress()
                        ThreadManager.query_servers.get(Random(ThreadManager.query_servers.length).nextInt()).addBot(Bot(bot_address))
                        Log.sendMessage(log_prefix, "Request from $bot_address", ConsoleColors.YELLOW)

                    // If the message does not contain a request
                    } else {
                        Log.sendMessage(log_prefix, "Invalid Request", ConsoleColors.YELLOW)

                    }

                    // Error handling
                } catch (e: Exception) {
                    Log.sendMessage(log_prefix, "Could not resolve requesting bot's IP", ConsoleColors.RED)
                    MiscUtil.waitasec(.1)

                }
            }

        } else {
            // Is not ready
            run = false

        }

        // Exit
        Log.sendMessage(log_prefix + "Request Server Process Stopped!")

    }

    companion object {
        /** Standard logging prefix  */
        @Volatile
        var std_log_prefix = "Request Server"
        
    }
}