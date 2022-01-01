package com.jtelaa.bwbot.querygen.processes;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import com.jtelaa.bwbot.bwlib.Query;
import com.jtelaa.bwbot.querygen.App;
import com.jtelaa.bwbot.querygen.searches.SearchHandler;
import com.jtelaa.bwbot.querygen.util.GenericThread;
import com.jtelaa.da2.lib.misc.MiscUtil;
import com.jtelaa.da2.lib.console.ConsoleColors;
import com.jtelaa.da2.lib.log.Log;

/**
 * This process generates random search queries to be searched by clients.
 * Uses the SearchHandler to generate and randomize the searches
 * 
 * @since 2
 * @author Joseph
 * 
 * @see com.jtelaa.bwbot.querygen.processes.QueryServer
 * @see com.jtelaa.bwbot.querygen.processes.RequestServer
 * @see com.jtelaa.bwbot.querygen.searches.SearchHandler
 */

public class QueryGenerator extends GenericThread {

    // ------------------------- Constructors

    /**
     * Program init
     */

    public QueryGenerator() {
        // Sets up file list
        this.searchHandler = new SearchHandler().setupFileList();

        log_prefix = std_log_prefix;
        this.setName(log_prefix);
        log_prefix += ": ";

    }

    /**
     * Program init
     * 
     * @param id id
     */

    public QueryGenerator(int id) {
        this.log_prefix = std_log_prefix + "(" + id + ")";
        this.setName(log_prefix);
        this.log_prefix += ": ";
        
        this.searchHandler = new SearchHandler().setupFileList();

    }

    // ------------------------- Thread Control

    /** Stops the thread */
    public void stopGenerator() { run = false; }

    /** Checks if the thread is ready */
    public boolean generatorReady() { return run; }

    // ------------------------- Logging

    /** Logging prefix */
    public String log_prefix;

    /** Standard logging prefix */
    public volatile static String std_log_prefix = "Query Generator";

    // ------------------------- Files

    public SearchHandler searchHandler;

    // ------------------------- Generated Query List

    /** Query queue */
    private Queue<Query> query_queue;

    /** Maximum size of the query queu */
    public int MAX_QUERY_QUEUE_SIZE = 10000;

    /** Add query to queue @param query Query to add */
    public void addToQueue(Query query) { query_queue.add(query); }

    /** Add queries to queue @param queries Queries to add */
    public void addToQueue(Query[] queries) { 
        for (Query query : queries) {
            query_queue.add(query);

        }  
    }

    /** Get the size of the query @return query size */
    public int queueSize() { return queueSize(); }

    /** Pop the top of the queue @return head of queue */
    public Query popFromQueue() { return query_queue.poll(); }

    /** Clear the query queue */
    public void clearQueue() { query_queue = new LinkedList<>(); }

    // ------------------------- Query Generation Collection
    
    /**
     * Generates a random search query
     * 
     * @return Random query
     */

    private Query generate() {
        return searchHandler.getRandomSearch();

    }

    /**
     * Generates a list of random search queries
     * 
     * @param count Size of lisr
     * 
     * @return Array of searches
     */

    private Query[] generate(int count) {
        return searchHandler.getRandomSearches(count);

    }

    // ------------------------- Util

    /**
     * Startup sequence and logging
     */

    private void startup() {
        // Set empty queue
        query_queue = new LinkedList<>();

        // Wait
        Log.sendMessage(log_prefix, "Waiting until other threads begin!", ConsoleColors.GREEN_BRIGHT);
        Log.sendMessage(log_prefix, "Generator Wait 30s", ConsoleColors.GREEN_BRIGHT);
        MiscUtil.waitasec(15);
        Log.sendMessage(log_prefix, "Generator Wait 15s", ConsoleColors.GREEN_BRIGHT);
        MiscUtil.waitasec(15);
        Log.sendMessage(log_prefix, "Generator Wait Time Complete", ConsoleColors.GREEN_BRIGHT);

        // Max Query Size
        MAX_QUERY_QUEUE_SIZE = Integer.parseInt(App.my_config.getProperty("query_queue_size", "1000"));
        Log.sendMessage(log_prefix, "Queue size set to " + MAX_QUERY_QUEUE_SIZE, ConsoleColors.PURPLE_BOLD_BRIGHT);

    }

    // ------------------------- Thread Processes

    /**
     * Run the thread
     */

    public void run() {
        // Startup
        startup();

        // Random
        Random rand = new Random();
        int rng;

        // While running
        run = true;
        while(run) {
            // If ready for a query
            if (queueSize() < MAX_QUERY_QUEUE_SIZE) {
                // Rng number of queries
                int count = rand.nextInt(200);

                // Generate random query
                Log.sendMessage(log_prefix, "Generating ("+count+") - " + queueSize() + "/" + MAX_QUERY_QUEUE_SIZE, ConsoleColors.PURPLE);
                addToQueue(generate(count));

            } else {
                if (Log.history.get(Log.history.size()-1).contains("Generation Stopped")) {
                    MiscUtil.waitamoment(50000);

                } else {
                    // Wait if not ready
                    Log.sendMessage(log_prefix, "Generation Stopped (" + queueSize() + ")", ConsoleColors.PURPLE_BOLD_BRIGHT);
                    MiscUtil.waitamoment(10000);

                }
            }
        }

        // Exit
        Log.sendMessage(log_prefix + "Generator Process Stopped!");
        
    }
}