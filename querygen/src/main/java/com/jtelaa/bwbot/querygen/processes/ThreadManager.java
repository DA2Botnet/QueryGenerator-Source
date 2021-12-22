package com.jtelaa.bwbot.querygen.processes;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.jtelaa.bwbot.bwlib.BWPorts;
import com.jtelaa.bwbot.querygen.util.InvalidThreadCountException;
import com.jtelaa.da2.lib.console.ConsoleColors;
import com.jtelaa.da2.lib.log.Log;
import com.jtelaa.da2.lib.net.NetTools;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Manages all of the threads in the system (Except for main thread (which this runs on) and CLIs)
 * 
 * @author Joseph
 * @since 2
 * 
 */

public class ThreadManager {

    // ------------------------- Processes

    /** Query Generator threads */
    public static volatile QueryGenerator[] generators;

    /** Query Server threads */
    public static volatile QueryServer[] query_servers;

    /** Request Server threads */
    public static volatile RequestServer[] request_servers;

    // ------------------------- Logs

    /** Standard logging prefix */
    public volatile static String log_prefix = "Thread Manager:" ;

    // ------------------------- Start

    /**
     * Sets up a single instance of each process
     */

    public static synchronized void setupProcesses() {
        setupProcesses(1, 1, 1);

    }

    /**
     * Sets up the processes given the count
     * 
     * @param gen_count Query Generator Count
     * @param qserv_count Query Server Count
     * @param reqsrv_count Request Server Count
     */

    public static synchronized void setupProcesses(int gen_count, int qserv_count, int reqsrv_count) {
        Log.sendMessage(log_prefix, "initializing threads", ConsoleColors.GREEN);

        try {
            setupGenerators(gen_count);
            setupQueryServers(qserv_count, NetTools.getLocalIP(), BWPorts.QUERY_REQUEST.getPort());
            setupRequestServers(reqsrv_count, NetTools.getLocalIP(), BWPorts.QUERY_RECEIVE.getPort());

        } catch (InvalidThreadCountException e1) {
            Log.sendMessage(log_prefix, e1, ConsoleColors.RED);
            return;

        }

    }

    /**
     * Sets up the threads given a JSON config fi;e
     * 
     * @param json_file_path Path of the file
     * 
     * @throws ParseException Error in JSON file parsing
     * @throws IOException Error in file handling
     * @throws FileNotFoundException Error in file loading
     */

    public static synchronized void setupProcesses(String json_file_path) throws FileNotFoundException, IOException, ParseException {
        // Load File
        Log.sendMessage(log_prefix, "initializing threads", ConsoleColors.GREEN);
        JSONObject json_object = (JSONObject) new JSONParser().parse(new FileReader(json_file_path));

        // Count Generators
        JSONObject generator_object = (JSONObject) json_object.get("query_generator");

        try {
            // Setup generators with the desirec count
            setupGenerators((int) generator_object.get("generator_count"));

        // If invalid numberr
        } catch (InvalidThreadCountException e) {
            Log.sendMessage(log_prefix, e, ConsoleColors.RED);

            try {
                // Setup single instance
                setupGenerators(1);

            // If theres a strange error, stop
            } catch (InvalidThreadCountException e1) {
                Log.sendMessage(log_prefix, e1, ConsoleColors.RED);
                return;
                
            }
        }

        // Count Query Servers
        JSONObject query_server_object = (JSONObject) json_object.get("query_server");
        int query_servers_count = (int) query_server_object.get("interface_count");

        for (int interface_num = 0; interface_num < query_servers_count; interface_num++) {
            // Get each interface specification
            JSONObject query_servers_indiv_object = (JSONObject) query_server_object.get("interface" + interface_num);
            
            try {
                // Setup servers given parameters
                setupQueryServers(
                    (int) query_servers_indiv_object.get("thread_count"),
                    (String) query_servers_indiv_object.get("ip_address"),
                    (int) query_servers_indiv_object.get("port")
                    
                );

            // If error in the count
            } catch (InvalidThreadCountException e) {
                try {
                    // Setup single instance
                    setupQueryServers(1, NetTools.getLocalIP(), BWPorts.QUERY_RECEIVE.getPort());

                // If theres a strange error, stop
                } catch (InvalidThreadCountException e1) {
                    Log.sendMessage(log_prefix, e1, ConsoleColors.RED);
                    return;

                }
            }
        }
        
        // Request Servers COunt
        JSONObject request_server_object = (JSONObject) json_object.get("requst_server");
        int request_servers_count = (int) request_server_object.get("interface_count");

        for (int interface_num = 0; interface_num < request_servers_count; interface_num++) {
            // Get each interface specification
            JSONObject request_servers_indiv_object = (JSONObject) request_server_object.get("interface" + interface_num);

            try {
                // Setup servers given parameters
                setupQueryServers(
                    (int) request_servers_indiv_object.get("thread_count"),
                    (String) request_servers_indiv_object.get("ip_address"),
                    (int) request_servers_indiv_object.get("port")
                    
                );
            
            // If error in the count
            } catch (InvalidThreadCountException e) {
                try {
                    // Setup single instance
                    setupRequestServers(1, NetTools.getLocalIP(), BWPorts.QUERY_RECEIVE.getPort());

                // If theres a strange error, stop
                } catch (InvalidThreadCountException e1) {
                    Log.sendMessage(log_prefix, e1, ConsoleColors.RED);
                    return;

                }
            }
        }
        
        // Done
        Log.sendMessage(log_prefix, "Done!", ConsoleColors.GREEN);

    }

    // ------------------------- Thread Util

    /**
     * Start all the setup processes
     */

    public static synchronized void startProcesses() {
        // Message
        Log.sendMessage(log_prefix, "Starting all (" + (generators.length + query_servers.length + request_servers.length) + ") processes", ConsoleColors.GREEN);
        
        // Start
        startGenerators();
        startQueryServers();
        startRequestServers();

        // Done
        Log.sendMessage(log_prefix, "Done", ConsoleColors.GREEN);

    }

    /**
     * Stop all of the processes
     */

    public static synchronized void stopProcesses() {
        // Message
        Log.sendMessage(log_prefix, "Stopping all (" + (generators.length + query_servers.length + request_servers.length) + ") processes", ConsoleColors.GREEN);
        
        // Stop
        stopGenerators();
        stopQueryServers();
        stopRequestServers();

        // Done
        Log.sendMessage(log_prefix, "Done", ConsoleColors.GREEN);

    }

    // ------------------------- Util

    /**
     * Check if the thread count is correct
     * 
     * @param thread_count Thread count
     * @param type Type of thread being built
     * 
     * @throws InvalidThreadCountException
     */

    private static synchronized void checkThreadCount(int thread_count, String type) throws InvalidThreadCountException {
        // Log
        Log.sendLogMessage(log_prefix + "Building " + thread_count + " " + type + "s", ConsoleColors.GREEN);

        // Throw if count is below -1 or above 20
        if (thread_count <= 0 || thread_count > 20) {
            throw new InvalidThreadCountException(thread_count);

        }
    }

    /**
     * Prin the status of each thread
     */

    public static synchronized void printThreadStatus() {
        // Output
        String output = "";

        // Status indicators
        String running = ConsoleColors.GREEN + "Running" + ConsoleColors.CLEAR;
        String not_running = ConsoleColors.RED + "Not Running" + ConsoleColors.CLEAR;

        // Check status of the generators
        output += "Generators:\n";
        for (int i = 0; i < generators.length; i++) {
            output += generators[i].log_prefix + (generators[i].generatorReady() ? running : not_running ) + "\n"; 

        }

        // Check status of the request servers
        output += "Request Servers:\n";
        for (int i = 0; i < request_servers.length; i++) {
            output += request_servers[i].log_prefix + (request_servers[i].serverReady() ? running : not_running ) + "\n";

        }

        // Check statis of the query servers
        output += "Query Servers:\n";
        for (int i = 0; i < query_servers.length; i++) {
            output += query_servers[i].log_prefix + (query_servers[i].serverReady() ? running : not_running ) + "\n";

        }

        // Send output
        Log.sendMessage(output);

    }

    // ------------------------- Query Generators

    /**
     * Setup the query generators
     * 
     * @param count count to setup
     * 
     * @throws InvalidThreadCountException
     */

    public static synchronized void setupGenerators(int count) throws InvalidThreadCountException {
        // Thread count and setup array
        checkThreadCount(count, "Query Generator");
        generators = new QueryGenerator[count];

        for (int i = 0; i < count; i++) {
            // Query generator setup
            generators[i] = new QueryGenerator(i);

        }
    }

    /**
     * Start the generators
     */

    public static synchronized void startGenerators() {
        for (int i = 0; i < generators.length; i++) { startGenerator(i); }

    }

    /**
     * Start specific generators
     */

    public static synchronized void startGenerators(int[] ids) {
        for (int id : ids) { startGenerator(id); }

    }

    /**
     * 
     */

    public static synchronized void startGenerator(int id) {
        if (id >= generators.length) { 
            Log.sendMessage(log_prefix, "Invalid Query Generator ID", ConsoleColors.RED);  
            return;
        
        }

        Log.sendMessage(log_prefix, "Starting Query Generator " + id, ConsoleColors.GREEN);
        generators[id].start();

    }

    /**
     * 
     */

    public static synchronized void stopGenerators() {
        for (int i = 0; i < generators.length; i++) { stopGenerator(i); }
        
    }

    /**
     * 
     */

    public static synchronized void stopGenerators(int[] ids) {
        for (int id : ids) { stopGenerator(id); }

    }

    /**
     * 
     */

    public static synchronized void stopGenerator(int id) {
        if (id >= generators.length) { 
            Log.sendMessage(log_prefix, "Invalid Query Generator ID", ConsoleColors.RED);  
            return;
        
        }

        Log.sendMessage(log_prefix, "Stopping Query Generator " + id, ConsoleColors.GREEN);
        generators[id].stopGenerator();

    }

    // ------------------------- Request Servers

    /**
     * 
     * @param count
     * 
     * @throws InvalidThreadCountException
     */

    public static synchronized void setupRequestServers(int count, String ip, int port) throws InvalidThreadCountException {
        // Thread count and setup array
        checkThreadCount(count, "Request Servers");
        request_servers = new RequestServer[count];

        for (int i = 0; i < count; i++) {
            // Query generator setup
            request_servers[i] = new RequestServer(i);

        }
    }

    /**
     * 
     */

    public static synchronized void startRequestServers() {
        for (int i = 0; i < request_servers.length; i++) { startRequestServer(i); }

    }

    /**
     * 
     */

    public static synchronized void startRequestServers(int[] ids) {
        for (int id : ids) { startRequestServer(id); }

    }

    /**
     * 
     */

    public static synchronized void startRequestServer(int id) {
        if (id >= request_servers.length) { 
            Log.sendMessage(log_prefix, "Invalid Request Server ID", ConsoleColors.RED);  
            return;
        
        }

        Log.sendMessage(log_prefix, "Starting Request Server " + id, ConsoleColors.GREEN);
        request_servers[id].start();

    }

    /**
     * 
     */

    public static synchronized void stopRequestServers() {
        for (int i = 0; i < generators.length; i++) { stopRequestServer(i); }
        
    }

    /**
     * 
     */

    public static synchronized void stopRequestServers(int[] ids) {
        for (int id : ids) { stopRequestServer(id); }

    }

    /**
     * 
     */

    public static synchronized void stopRequestServer(int id) {
        if (id >= request_servers.length) { 
            Log.sendMessage(log_prefix, "Invalid Request Server ID", ConsoleColors.RED);  
            return;
        
        }

        Log.sendMessage(log_prefix, "Stopping Request Server " + id, ConsoleColors.GREEN);
        request_servers[id].stopServer();

    }

    // ------------------------- Query Servers

    /**
     * 
     * @param count
     * 
     * @throws InvalidThreadCountException
     */

    public static synchronized void setupQueryServers(int count, String ip, int port) throws InvalidThreadCountException {
        // Thread count and setup array
        checkThreadCount(count, "Query Servers");
        query_servers = new QueryServer[count];

        for (int i = 0; i < count; i++) {
            // Query generator setup
            query_servers[i] = new QueryServer(i);

        }
    }

    /**
     * 
     */

    public static synchronized void startQueryServers() {
        for (int i = 0; i < request_servers.length; i++) { startQueryServer(i); }

    }

    /**
     * 
     */

    public static synchronized void startQueryServers(int[] ids) {
        for (int id : ids) { startQueryServer(id); }

    }

    /**
     * 
     */

    public static synchronized void startQueryServer(int id) {
        if (id >= query_servers.length) { 
            Log.sendMessage(log_prefix, "Invalid Query Server ID", ConsoleColors.RED);  
            return;
        
        }

        Log.sendMessage(log_prefix, "Starting Query Server " + id, ConsoleColors.GREEN);
        query_servers[id].start();

    }

    /**
     * 
     */

    public static synchronized void stopQueryServers() {
        for (int i = 0; i < generators.length; i++) { stopQueryServer(i); }
        
    }

    /**
     * 
     */

    public static synchronized void stopQueryServers(int[] ids) {
        for (int id : ids) { stopQueryServer(id); }

    }

    /**
     * 
     */

    public static synchronized void stopQueryServer(int id) {
        if (id >= query_servers.length) { 
            Log.sendMessage(log_prefix, "Invalid Query Server ID", ConsoleColors.RED);  
            return;
        
        }

        Log.sendMessage(log_prefix, "Stopping Query Server " + id, ConsoleColors.GREEN);
        query_servers[id].stopServer();

    }
    
}