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
 * This process generates random search queries to be searched by clients
 * 
 * @since 2
 * @author Joseph
 * 
 * @see com.jtelaa.bwbot.querygen.processes.QueryServer
 * @see com.jtelaa.bwbot.querygen.processes.RequestServer
 */

public class QueryGenerator extends GenericThread {

    // ------------------------- Constructors

    /**
     * Program init
     */

    public QueryGenerator() {
        // Sets up file list
        SearchHandler.setupFileList();
        log_prefix = std_log_prefix;

    }

    /**
     * Program init
     * 
     * @param log_prefix Log prefix (Contains ID)
     */

    public QueryGenerator(String log_prefix) {
        this.log_prefix = log_prefix;
        SearchHandler.setupFileList();

    }

    // ------------------------- Thread Control

    /** Stops the thread */
    public synchronized void stopGenerator() { run = false; }

    /** Checks if the thread is ready */
    public synchronized boolean generatorReady() { return run; }

    // ------------------------- Logging

    /** Logging prefix */
    private String log_prefix;

    /** Standard logging prefix */
    public volatile static String std_log_prefix = "Query Generator:";

    // ------------------------- Generation Settings Control

    /** Query queue */
    public volatile static Queue<Query> query_queue;

    /** Maximum size of the query queu */
    public volatile static int MAX_QUERY_QUEUE_SIZE = 10000;

    // ------------------------- Query Generation Collection
    
    /**
     * Generates a random search query
     * 
     * @return Random query
     */

    private Query generate() {
        return SearchHandler.getRandomSearch();
    }

    /**
     * Generates a list of random search queries
     * 
     * @param count Size of lisr
     * 
     * @return Array of searches
     */

    private Query[] generate(int count) {
        return SearchHandler.getRandomSearches(count);

    }

    // ------------------------- Thread Processes

    /**
     * Run the thread
     */

    public void run() {
        // Random
        Random rand = new Random();
        query_queue = new LinkedList<>();
        int rng;

        // Wait
        Log.sendMessage(log_prefix + "Generator Wait 30s", ConsoleColors.GREEN_BRIGHT);
        MiscUtil.waitasec(15);
        Log.sendMessage(log_prefix + "Generator Wait 15s", ConsoleColors.GREEN_BRIGHT);
        MiscUtil.waitasec(15);
        Log.sendMessage(log_prefix + "Generator Wait Time Complete", ConsoleColors.GREEN_BRIGHT);

        // Max Query Size
        MAX_QUERY_QUEUE_SIZE = Integer.parseInt(App.my_config.getProperty("query_queue_size", "1000"));
        Log.sendMessage(log_prefix + "Queue size set to " + MAX_QUERY_QUEUE_SIZE, ConsoleColors.PURPLE_BOLD_BRIGHT);

        // While running
        run = true;
        while(run) {
            // If ready for a query
            if (query_queue.size() < MAX_QUERY_QUEUE_SIZE) {
                // Generate random case
                rng = rand.nextInt(100);

                if (rng <= 50) {
                    // Add a single query
                    Log.sendMessage(log_prefix + "Generating (1) - " + query_queue.size() + "/" + MAX_QUERY_QUEUE_SIZE, ConsoleColors.PURPLE);
                    query_queue.add(generate());

                } else {
                    int count = rand.nextInt(rng);

                    // Generate random query
                    Log.sendMessage(log_prefix + "Generating ("+count+") - " + query_queue.size() + "/" + MAX_QUERY_QUEUE_SIZE, ConsoleColors.PURPLE);
                    Query[] queries = generate(count);
                    
                    // Add queries
                    for (Query query : queries) {
                        query_queue.add(query);

                    }
                }

            } else {
                if (Log.history.get(Log.history.size()-1).contains("Generation Stopped")) {
                    MiscUtil.waitamoment(50000);

                } else {
                    // Wait if not ready
                    Log.sendMessage(log_prefix + "Generation Stopped (" + query_queue.size() + ")", ConsoleColors.PURPLE_BOLD_BRIGHT);
                    MiscUtil.waitamoment(10000);

                }
            }
        }

        Log.sendMessage(log_prefix + "Generator Process Stopped!");
        
    }
}