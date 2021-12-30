package com.jtelaa.bwbot.querygen.processes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import com.jtelaa.bwbot.bwlib.BWPorts;
import com.jtelaa.bwbot.bwlib.Query;
import com.jtelaa.bwbot.querygen.util.GenericThread;
import com.jtelaa.da2.lib.bot.Bot;
import com.jtelaa.da2.lib.console.ConsoleColors;
import com.jtelaa.da2.lib.log.Log;
import com.jtelaa.da2.lib.misc.MiscUtil;
import com.jtelaa.da2.lib.net.NetTools;
import com.jtelaa.da2.lib.net.client.ClientUDP;
import com.jtelaa.da2.lib.net.ports.Ports;

/**
 * This process serves the requested queries
 * 
 * @since 2
 * @author Joseph
 * 
 * @see com.jtelaa.bwbot.querygen.processes.RequestClient
 * @see com.jtelaa.bwbot.querygen.processes.QueryGenerator
 */

public class QueryServer extends GenericThread {
    
    // ------------------------- Constructors

    /**
     * Program init
     */

    public QueryServer() {
        // Log prefix
        log_prefix = std_log_prefix;
        this.setName(log_prefix);
        log_prefix += ": ";

        // Port
        receive_port = BWPorts.QUERY_RECEIVE;

        // Setup stats file
        if (do_stats) { stats_ready = setupFile(); }

    }
    
    /**
     * Program init
     * 
     * @param receive_port Receive port
     */

    public QueryServer(Ports receive_port) {
        // Log prefix
        log_prefix = std_log_prefix;
        this.setName(log_prefix);
        log_prefix += ": ";

        // Port
        this.receive_port = receive_port;

        // Setup stats file
        if (do_stats) { stats_ready = setupFile(); }

    }

    /**
     * Program init
     * 
     * @param id Thread id
     */

    public QueryServer(int id) {
        // Log prefix
        this.log_prefix = std_log_prefix + "(" + id + ")";
        this.setName(log_prefix);
        this.log_prefix += ": ";

        // Port
        receive_port = BWPorts.QUERY_RECEIVE;

        // Setup stats file
        if (do_stats) { stats_ready = setupFile(); }

    }

    /**
     * Program init
     * 
     * @param id Thread id
     * @param receive_port Receive port
     */

    public QueryServer(int id, Ports receive_port) {
        // Log prefix
        this.log_prefix = std_log_prefix + "(" + id + ")";
        this.setName(log_prefix);
        this.log_prefix += ": ";

        // Port
        this.receive_port = receive_port;

        // Setup stats file
        if (do_stats) { stats_ready = setupFile(); }

    }

    // ------------------------- Logging

    /** Logging prefix */
    public String log_prefix;

    /** Standard logging prefix */
    public volatile static String std_log_prefix = "Query Server";

    // ------------------------- Queue

    /** Bot queue */
    private Queue<Bot> bot_queue;

    /** Add bot to queue @param bot bot to add */
    public void addToQueue(Bot bot) { bot_queue.add(bot); }

    /** Get the size of the query @return query size */
    public int queueSize() { return queueSize(); }

    /** Pop the top of the queue @return head of queue */
    public Bot popFromQueue() { return bot_queue.poll(); }

    /** Clear the bot queue */
    public void clearQueue() { bot_queue = new LinkedList<>(); }

    // ------------------------- Socket

    /** UDP Client  */
    private ClientUDP query_socket;

    /** */
    private Ports receive_port;

    // ------------------------- Thread Control

    /** Stops the thread */
    public void stopServer() { run = false; }

    /** Checks if the thread is ready */
    public boolean serverReady() { return run; }

    // ------------------------- Thread Processes

    /**
     * Thread codes
     */

    public void run() {
        // Ready
        Log.sendMessage(log_prefix);

        // Setup lists
        bot_queue = new LinkedList<>();

        // Constantly fill requests
        while (run) {
            fillRequest();

        }

        // Exit
        Log.sendMessage(log_prefix + "Query Server Process Stopped!");

    }

    // ------------------------- Stats

    /** Stats readieness */
    private boolean stats_ready = false, do_stats = true;

    /** Standard stats file name */
    public volatile static String std_stats_file_name = "qstats.csv";
    
    /** Stats file path */
    public volatile static String stats_file_path = "~/qgen/stats/";

    /** Stats file name */
    private String stats_file_name;

    /** Stats file */
    private FileWriter stats_file;

    /**
     * Setup the file
     */

    private boolean setupFile() {
        stats_file_name = stats_file_path + this.getName() + "_" + stats_file_name;
        File file = new File(stats_file_name);

        try {
            stats_file = new FileWriter(file);

            stats_ready = true;
            return stats_ready;

        } catch (IOException e) {
            Log.sendMessage(log_prefix, e, ConsoleColors.RED);
            
            stats_ready = false;
            return stats_ready;

        }
    }
    
    /**
     * Adds a new entry into the log
     * 
     * @param host IP address of the requesting host
     */

    public void newStatsEntry(String host) {
        if (!stats_ready) { return; }

        try {
            stats_file.write(System.currentTimeMillis() + "," + host + "\n");

        } catch (FileNotFoundException e) {
            Log.sendMessage(log_prefix, "File was not created", ConsoleColors.RED);
            setupFile();

        } catch (IOException e1) {
            Log.sendMessage(log_prefix, e1);

        }
    }

    // ------------------------- Request Filler

    /**
     * Fills the search query request by establishing a connection
     * with the bot
     */

    private void fillRequest() {
        // Pull a random query generator
        QueryGenerator gen = ThreadManager.generators[new Random(ThreadManager.generators.length).nextInt()];

        // If no requests or queries, wait
        if (bot_queue.size() == 0 || gen.queueSize() == 0) {
            MiscUtil.waitasec(.10);
            return;

        } 

        // Pick top off queue
        Query query_to_send = gen.popFromQueue();
        Bot bot_to_serve = bot_queue.poll();

        // Notification
        Log.sendMessage(log_prefix, "Serving " + bot_to_serve.ip, ConsoleColors.CYAN);
        newStatsEntry(bot_to_serve.ip);

        // Setup client
        query_socket = new ClientUDP(bot_to_serve.ip, receive_port, log_prefix, ConsoleColors.CYAN);

        // Send and then close
        if (query_socket.startClient()) {
            query_socket.sendMessage(query_to_send.getQuery());
            query_socket.closeClient();
            Log.sendMessage(log_prefix, "Done serving " + bot_to_serve.ip, ConsoleColors.CYAN);

        }
    }

    /**
     * Adds a query into the queue <p>
     * Many queries are added. They will be served to the bots as needed.
     * 
     * @param query Search query to enque
     */

    public void addQuery(Query query) {
        // Pull a random query generator
        QueryGenerator gen = ThreadManager.generators[new Random(ThreadManager.generators.length).nextInt()];

        if (gen.queueSize() < gen.MAX_QUERY_QUEUE_SIZE) {
            // Add to queue if the que is under specified size
            gen.addToQueue(query);

        } else {
            do {
                // Run other generators to check
                MiscUtil.waitasec();
                gen = ThreadManager.generators[new Random(ThreadManager.generators.length).nextInt()];

            } while (gen.queueSize() < gen.MAX_QUERY_QUEUE_SIZE);

            // Add to queue if the que is under specified size
            gen.addToQueue(query);

        }
    }

    /**
     * Checks if the queue size is small enough to
     * where it could accept another query
     * 
     * @return if the query queue size is less than the max
     * @deprecated No longer necessary
     */

    @Deprecated
    public boolean readyForQuery() { 
        for (QueryGenerator gen : ThreadManager.generators) {
            if (gen.queueSize() < gen.MAX_QUERY_QUEUE_SIZE) {
                return false;

            }
        }

        return true;
    
    }

    /**
     * Adds a bot into the queue <p>
     * Whenever the request server sees a new request, it is enqued
     * 
     * @param bot bot to enque
     */
    
    public void addBot(Bot bot) {
        if (NetTools.isValid(bot.ip)) {
            // If bot has valid ip, add it to the queue
            bot_queue.add(bot);

        }
    }
}