package com.jtelaa.bwbot.querygen.processes;

import com.jtelaa.bwbot.querygen.util.InvalidThreadCountException;
import com.jtelaa.da2.lib.console.ConsoleColors;
import com.jtelaa.da2.lib.log.Log;

/**
 * 
 */

public class ThreadManager {

    // ------------------------- Processes

    /** */
    private static volatile QueryGenerator[] generators;

    /** */
    private static volatile QueryServer[] query_servers;

    /** */
    private static volatile RequestServer[] request_servers;

    // ------------------------- Logs

    /** Standard logging prefix */
    public volatile static String log_prefix = "Thread Manager:" ;

    // ------------------------- Start

    /**
     * 
     * @param json_file_path
     */

    public static synchronized void startProcesses(String json_file_path) {

    }

    // ------------------------- Util

    private static synchronized void checkThreadCount(int thread_count, String type) {
        Log.sendLogMessage(log_prefix + "Building " + thread_count + " " + type + "s", ConsoleColors.GREEN);

        try {
            if (thread_count <= 0) {
                throw new InvalidThreadCountException(thread_count);

            }

        } catch (InvalidThreadCountException e) {
            Log.sendMessage(e, ConsoleColors.RED);

        }
    }

    /**
     * 
     */

    public static synchronized void printThreadStatus() {
        //
        String output = "";

        //
        String running = ConsoleColors.GREEN + "Running" + ConsoleColors.CLEAR;
        String not_running = ConsoleColors.RED + "Not Running" + ConsoleColors.CLEAR;

        //
        output += "Generators:\n";
        for (int i = 0; i < generators.length; i++) {
            output += "Generator " + i + "; " + (generators[i].generatorReady() ? running : not_running ) + "\n"; 

        }

        //
        output += "Request Servers:\n";
        for (int i = 0; i < request_servers.length; i++) {
            output += "Request Servers " + i + "; " + (request_servers[i].serverReady() ? running : not_running ) + "\n";

        }

        //
        output += "Query Servers:\n";
        for (int i = 0; i < query_servers.length; i++) {
            output += "Query Servers " + i + "; " + (query_servers[i].serverReady() ? running : not_running ) + "\n";

        }

        //
        Log.sendLogMessage(output);

    }

    // ------------------------- Query Generators

    /**
     * 
     * @param count
     */

    public static synchronized void setupGenerators(int count) {
        // Thread count and setup array
        checkThreadCount(count, "Query Generator");
        generators = new QueryGenerator[count];

        for (int i = 0; i < count; i++) {
            // Query generator setup
            generators[i] = new QueryGenerator(QueryGenerator.std_log_prefix + " " + i + ": ");

        }
    }

    /**
     * 
     */

    public static synchronized void startGenerators() {
        for (int i = 0; i < generators.length; i++) {
            // Starting
            Log.sendMessage(log_prefix + "Starting query generator " + i, ConsoleColors.GREEN);
            generators[i].start();
            
        }
    }

    // ------------------------- Request Servers

    /**
     * 
     * @param count
     */

    public static synchronized void startRequestServers(int count, String ip, int[] ports) {
        // Thread count and setup array
        checkThreadCount(count, "Request Servers");
        request_servers = new RequestServer[count];

        for (int i = 0; i < count; i++) {
            // Query generator setup
            request_servers[i] = new RequestServer(RequestServer.std_log_prefix + " " + i + ": ");

        }

    }

    /**
     * 
     */

    public static synchronized void startRequestServers() {
        for (int i = 0; i < request_servers.length; i++) {
            // Starting
            Log.sendMessage(log_prefix + "Starting request servers " + i, ConsoleColors.GREEN);
            request_servers[i].start();
            
        }
    }

    // ------------------------- Query Servers

    /**
     * 
     * @param count
     */

    public static synchronized void startQueryServers(int count, String ip, int[] ports) {
        // Thread count and setup array
        checkThreadCount(count, "Query Servers");
        query_servers = new QueryServer[count];

        for (int i = 0; i < count; i++) {
            // Query generator setup
            query_servers[i] = new QueryServer(QueryServer.std_log_prefix + " " + i + ": ");

        }

    }

    /**
     * 
     */

    public static synchronized void startQueryServers() {
        for (int i = 0; i < query_servers.length; i++) {
            // Starting
            Log.sendMessage(log_prefix + "Starting query servers " + i, ConsoleColors.GREEN);
            query_servers[i].start();
            
        }
    }
    
}