package com.jtelaa.bwbot.querygen.processes

import java.util.LinkedList

/**
 * This process generates random search queries to be searched by clients.
 * Uses the SearchHandler to generate and randomize the searches
 *
 * @since 2
 * @author Joseph
 *
 * @see com.jtelaa.bwbot.querygen.processes.QueryServer
 *
 * @see com.jtelaa.bwbot.querygen.processes.RequestServer
 *
 * @see com.jtelaa.bwbot.querygen.searches.SearchHandler
 */

class QueryGenerator : GenericThread {
    // ------------------------- Constructors

    /**
     * Program init
     */

    constructor() {
        // Sets up file list
        SearchHandler.setupFileList()

        log_prefix = std_log_prefix
        this.setName(log_prefix)
        log_prefix += ": "

    }

    /**
     * Program init
     *
     * @param id id
     */

    constructor(id: Int) {
        log_prefix = std_log_prefix + "(" + id + ")"
        this.setName(log_prefix)
        log_prefix += ": "

        SearchHandler.setupFileList()

    }

    // ------------------------- Thread Control

    /** Stops the thread  */

    @Synchronized
    fun stopGenerator() {
        run = false

    }

    /** Checks if the thread is ready  */

    @Synchronized
    fun generatorReady(): Boolean {
        return run

    }

    // ------------------------- Logging

    /** Logging prefix  */
    var log_prefix: String

    // ------------------------- Generation Settings Control

    /** Query queue  */
    var query_queue: Queue<Query>? = null

    /** Maximum size of the query queu  */
    var MAX_QUERY_QUEUE_SIZE = 10000

    // ------------------------- Query Generation Collection

    /**
     * Generates a random search query
     *
     * @return Random query
     */

    private fun generate(): Query {
        return SearchHandler.getRandomSearch()

    }

    /**
     * Generates a list of random search queries
     *
     * @param count Size of lisr
     *
     * @return Array of searches
     */

    private fun generate(count: Int): Array<Query> {
        return SearchHandler.getRandomSearches(count)

    }

    // ------------------------- Util

    /**
     * Startup sequence and logging
     */

    private fun startup() {
        // Set empty queue
        query_queue = LinkedList()

        // Wait
        Log.sendMessage(log_prefix, "Waiting until other threads begin!", ConsoleColors.GREEN_BRIGHT)
        Log.sendMessage(log_prefix, "Generator Wait 30s", ConsoleColors.GREEN_BRIGHT)
        MiscUtil.waitasec(15)
        Log.sendMessage(log_prefix, "Generator Wait 15s", ConsoleColors.GREEN_BRIGHT)
        MiscUtil.waitasec(15)
        Log.sendMessage(log_prefix, "Generator Wait Time Complete", ConsoleColors.GREEN_BRIGHT)

        // Max Query Size
        MAX_QUERY_QUEUE_SIZE = Integer.parseInt(App.my_config.getProperty("query_queue_size", "1000"))
        Log.sendMessage(log_prefix, "Queue size set to $MAX_QUERY_QUEUE_SIZE", ConsoleColors.PURPLE_BOLD_BRIGHT)

    }

    // ------------------------- Thread Processes

    /**
     * Run the thread
     */

    fun run() {
        // Startup
        startup()

        // Random
        val rand = Random()
        var rng: Int

        // While running
        run = true
        while (run) {
            // If ready for a query
            if (query_queue.size() < MAX_QUERY_QUEUE_SIZE) {
                // Generate random case
                rng = rand.nextInt(100)
                
                if (rng <= 50) {
                    // Add a single query
                    Log.sendMessage(log_prefix, "Generating (1) - " + query_queue.size().toString() + "/" + MAX_QUERY_QUEUE_SIZE, ConsoleColors.PURPLE)
                    query_queue.add(generate())

                } else {
                    // Rng number of queries
                    val count: Int = rand.nextInt(rng)

                    // Generate random query
                    Log.sendMessage(log_prefix, "Generating (" + count + ") - " + query_queue.size() + "/" + MAX_QUERY_QUEUE_SIZE, ConsoleColors.PURPLE)
                    val queries: Array<Query> = generate(count)

                    // Add queries
                    for (query in queries) {
                        query_queue.add(query)

                    }
                }

            } else {
                if (Log.history.get(Log.history.size() - 1).contains("Generation Stopped")) {
                    MiscUtil.waitamoment(50000)

                } else {
                    // Wait if not ready
                    Log.sendMessage(log_prefix, "Generation Stopped (" + query_queue.size().toString() + ")", ConsoleColors.PURPLE_BOLD_BRIGHT)
                    MiscUtil.waitamoment(10000)

                }
            }
        }

        // Exit
        Log.sendMessage(log_prefix + "Generator Process Stopped!")

    }

    companion object {
        /** Standard logging prefix  */
        @Volatile
        var std_log_prefix = "Query Generator"
        
    }
}