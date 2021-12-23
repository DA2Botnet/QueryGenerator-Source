package com.jtelaa.bwbot.querygen.processes

import java.util.LinkedList

/**
 * This process serves the requested queries
 *
 * @since 2
 * @author Joseph
 *
 * @see com.jtelaa.bwbot.querygen.processes.RequestClient
 *
 * @see com.jtelaa.bwbot.querygen.processes.QueryGenerator
 */

class QueryServer : GenericThread {
    // ------------------------- Constructors

    /**
     * Program init
     */

    constructor() {
        log_prefix = std_log_prefix
        this.setName(log_prefix)
        log_prefix += ": "

        receive_port = BWPorts.QUERY_RECEIVE
    }

    /**
     * Program init
     *
     * @param receive_port Receive port
     */

    constructor(receive_port: Ports) {
        log_prefix = std_log_prefix
        this.setName(log_prefix)
        log_prefix += ": "

        this.receive_port = receive_port

    }

    /**
     * Program init
     *
     * @param id Thread id
     */

    constructor(id: Int) {
        log_prefix = std_log_prefix + "(" + id + ")"
        this.setName(log_prefix)
        log_prefix += ": "

        receive_port = BWPorts.QUERY_RECEIVE

    }

    /**
     * Program init
     *
     * @param id Thread id
     * @param receive_port Receive port
     */

    constructor(id: Int, receive_port: Ports) {
        log_prefix = std_log_prefix + "(" + id + ")"
        this.setName(log_prefix)
        log_prefix += ": "

        this.receive_port = receive_port

    }

    // ------------------------- Logging

    /** Logging prefix  */
    var log_prefix: String

    // ------------------------- Queue

    /** Bot queue  */
    @Volatile
    var bot_queue: Queue<Bot>? = null

    // ------------------------- Socket

    /** UDP Client   */
    private var query_socket: ClientUDP? = null

    /** Receive Port  */
    private var receive_port: Ports

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

    // ------------------------- Thread Processes

    /**
     * Thread codes
     */

    fun run() {
        // Ready
        Log.sendMessage(log_prefix)

        // Setup lists
        bot_queue = LinkedList()

        // Constantly fill requests
        while (run) {
            fillRequest()

        }

        // Exit
        Log.sendMessage(log_prefix + "Query Server Process Stopped!")

    }

    // ------------------------- Request Filler

    /**
     * Fills the search query request by establishing a connection
     * with the bot
     */

    private fun fillRequest() {
        // Pull a random query generator
        val gen: QueryGenerator = ThreadManager.generators.get(Random(ThreadManager.generators.length).nextInt())

        // If no requests or queries, wait
        if (bot_queue.size() === 0 || gen.query_queue.size() === 0) {
            MiscUtil.waitasec(.10)
            return

        }

        // Pick top off queue
        val query_to_send: Query = gen.query_queue.poll()
        val bot_to_serve: Bot = bot_queue.poll()

        // Notification
        Log.sendMessage(log_prefix, "Serving " + bot_to_serve.ip, ConsoleColors.YELLOW)

        // Setup client
        query_socket = ClientUDP(bot_to_serve.ip, receive_port, log_prefix, ConsoleColors.YELLOW)

        // Send and then close
        if (query_socket.startClient()) {
            query_socket.sendMessage(query_to_send.getQuery())
            query_socket.closeClient()
            Log.sendMessage(log_prefix, "Done serving " + bot_to_serve.ip, ConsoleColors.YELLOW)

        }
    }

    /**
     * Adds a query into the queue
     *
     *
     * Many queries are added. They will be served to the bots as needed.
     *
     * @param query Search query to enque
     */

    @Synchronized
    fun addQuery(query: Query?) {
        // Pull a random query generator
        var gen: QueryGenerator = ThreadManager.generators.get(Random(ThreadManager.generators.length).nextInt())
        if (gen.query_queue.size() < gen.MAX_QUERY_QUEUE_SIZE) {
            // Add to queue if the que is under specified size
            gen.query_queue.add(query)

        } else {
            do {
                // Run other generators to check
                MiscUtil.waitasec()
                gen = ThreadManager.generators.get(Random(ThreadManager.generators.length).nextInt())

            } while (gen.query_queue.size() < gen.MAX_QUERY_QUEUE_SIZE)

            // Add to queue if the que is under specified size
            gen.query_queue.add(query)

        }
    }

    /**
     * Checks if the queue size is small enough to
     * where it could accept another query
     *
     * @return if the query queue size is less than the max
     */

    @Deprecated("No longer necessary")
    @Synchronized
    fun readyForQuery(): Boolean {
        for (gen in ThreadManager.generators) {
            if (gen.query_queue.size() < gen.MAX_QUERY_QUEUE_SIZE) {
                return false

            }
        }

        return true

    }

    /**
     * Adds a bot into the queue
     *
     *
     * Whenever the request server sees a new request, it is enqued
     *
     * @param bot bot to enque
     */

    @Synchronized
    fun addBot(bot: Bot) {
        if (NetTools.isValid(bot.ip)) {
            // If bot has valid ip, add it to the queue
            bot_queue.add(bot)

        }
    }

    companion object {
        /** Standard logging prefix  */
        @Volatile
        var std_log_prefix = "Query Server"
        
    }
}