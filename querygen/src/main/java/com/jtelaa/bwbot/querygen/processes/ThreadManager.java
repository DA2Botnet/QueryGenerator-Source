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
     * 
     * @throws ParseException
     * @throws IOException
     * @throws FileNotFoundException
     */

    public static synchronized void setupProcesses(String json_file_path) throws FileNotFoundException, IOException, ParseException {
        // Load File
        Log.sendMessage(log_prefix + "initializing threads", ConsoleColors.GREEN);
        JSONObject json_object = (JSONObject) new JSONParser().parse(new FileReader(json_file_path));

        // Count Generators
        JSONObject generator_object = (JSONObject) json_object.get("query_generator");

        try {
            setupGenerators((int) generator_object.get("generator_count"));

        } catch (InvalidThreadCountException e) {
            Log.sendMessage(log_prefix, e, ConsoleColors.RED);

            try {
                setupGenerators(1);
            } catch (InvalidThreadCountException e1) {
                Log.sendMessage(log_prefix, e1, ConsoleColors.RED);
                return;
                
            }
        }

        // Count Query Servers
        JSONObject query_server_object = (JSONObject) json_object.get("query_server");
        int query_servers_count = (int) query_server_object.get("interface_count");

        for (int interface_num = 0; interface_num < query_servers_count; interface_num++) {
            JSONObject query_servers_indiv_object = (JSONObject) query_server_object.get("interface" + interface_num);
            
            try {
                setupQueryServers(
                    (int) query_servers_indiv_object.get("thread_count"),
                    (String) query_servers_indiv_object.get("ip_address"),
                    (int) query_servers_indiv_object.get("port")
                    
                );
            } catch (InvalidThreadCountException e) {
                try {
                    setupQueryServers(1, NetTools.getLocalIP(), BWPorts.QUERY_RECEIVE.getPort());

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
            JSONObject request_servers_indiv_object = (JSONObject) request_server_object.get("interface" + interface_num);

            try {
                setupQueryServers(
                    (int) request_servers_indiv_object.get("thread_count"),
                    (String) request_servers_indiv_object.get("ip_address"),
                    (int) request_servers_indiv_object.get("port")
                    
                );
            } catch (InvalidThreadCountException e) {
                try {
                    setupQueryServers(1, NetTools.getLocalIP(), BWPorts.QUERY_REQUEST.getPort());

                } catch (InvalidThreadCountException e1) {
                    Log.sendMessage(log_prefix, e1, ConsoleColors.RED);
                    return;

                }
            }
        }
        
        Log.sendMessage(log_prefix + "Done!", ConsoleColors.GREEN);

    }

    // ------------------------- Thread Util

    /**
     * 
     */

    public static synchronized void startProcesses() {
        Log.sendMessage(log_prefix + "Starting all (" + (generators.length + query_servers.length + request_servers.length) + ") processes", ConsoleColors.GREEN);
        startGenerators();
        startQueryServers();
        startRequestServers();
        Log.sendMessage(log_prefix + "Done", ConsoleColors.GREEN);

    }

    /**
     * 
     */

    public static synchronized void stopProcesses() {
        Log.sendMessage(log_prefix + "Stopping all (" + (generators.length + query_servers.length + request_servers.length) + ") processes", ConsoleColors.GREEN);
        stopGenerators();
        stopQueryServers();
        stopRequestServers();
        Log.sendMessage(log_prefix + "Done", ConsoleColors.GREEN);

    }

    // ------------------------- Util

    /**
     * 
     * @param thread_count
     * @param type
     * 
     * @throws InvalidThreadCountException
     */

    private static synchronized void checkThreadCount(int thread_count, String type) throws InvalidThreadCountException {
        Log.sendLogMessage(log_prefix + "Building " + thread_count + " " + type + "s", ConsoleColors.GREEN);

        if (thread_count <= 0) {
            throw new InvalidThreadCountException(thread_count);

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
     * 
     * @throws InvalidThreadCountException
     */

    public static synchronized void setupGenerators(int count) throws InvalidThreadCountException {
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
        for (int i = 0; i < generators.length; i++) { startGenerator(i); }

    }

    /**
     * 
     */

    public static synchronized void startGenerators(int[] ids) {
        for (int id : ids) { startGenerator(id); }

    }

    /**
     * 
     */

    public static synchronized void startGenerator(int id) {
        if (id >= generators.length) { 
            Log.sendMessage(log_prefix + "Invalid Query Generator ID", ConsoleColors.RED);  
            return;
        
        }

        Log.sendMessage(log_prefix + "Starting Query Generator " + id, ConsoleColors.GREEN);
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
            Log.sendMessage(log_prefix + "Invalid Query Generator ID", ConsoleColors.RED);  
            return;
        
        }

        Log.sendMessage(log_prefix + "Stopping Query Generator " + id, ConsoleColors.GREEN);
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
            request_servers[i] = new RequestServer(RequestServer.std_log_prefix + " " + i + ": ");

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
            Log.sendMessage(log_prefix + "Invalid Request Server ID", ConsoleColors.RED);  
            return;
        
        }

        Log.sendMessage(log_prefix + "Starting Request Server " + id, ConsoleColors.GREEN);
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
            Log.sendMessage(log_prefix + "Invalid Request Server ID", ConsoleColors.RED);  
            return;
        
        }

        Log.sendMessage(log_prefix + "Stopping Request Server " + id, ConsoleColors.GREEN);
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
            query_servers[i] = new QueryServer(QueryServer.std_log_prefix + " " + i + ": ");

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
            Log.sendMessage(log_prefix + "Invalid Query Server ID", ConsoleColors.RED);  
            return;
        
        }

        Log.sendMessage(log_prefix + "Starting Query Server " + id, ConsoleColors.GREEN);
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
            Log.sendMessage(log_prefix + "Invalid Query Server ID", ConsoleColors.RED);  
            return;
        
        }

        Log.sendMessage(log_prefix + "Stopping Query Server " + id, ConsoleColors.GREEN);
        query_servers[id].stopServer();

    }
    
}