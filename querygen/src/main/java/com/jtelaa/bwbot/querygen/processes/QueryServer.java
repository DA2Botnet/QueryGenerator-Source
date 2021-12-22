package com.jtelaa.bwbot.querygen.processes;

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
        log_prefix = std_log_prefix;
        this.setName(log_prefix);
        log_prefix += ": ";

        receive_port = BWPorts.QUERY_RECEIVE;

    }
    
    /**
     * Program init
     * 
     * @param receive_port Receive port
     */

    public QueryServer(Ports receive_port) {
        log_prefix = std_log_prefix;
        this.setName(log_prefix);
        log_prefix += ": ";

        this.receive_port = receive_port;

    }

    /**
     * Program init
     * 
     * @param id Thread id
     */

    public QueryServer(int id) {
        this.log_prefix = std_log_prefix + "(" + id + ")";
        this.setName(log_prefix);
        this.log_prefix += ": ";

        receive_port = BWPorts.QUERY_RECEIVE;

    }

    /**
     * Program init
     * 
     * @param id Thread id
     * @param receive_port Receive port
     */

    public QueryServer(int id, Ports receive_port) {
        this.log_prefix = std_log_prefix + "(" + id + ")";
        this.setName(log_prefix);
        this.log_prefix += ": ";

        this.receive_port = receive_port;

    }

    // ------------------------- Logging

    /** Logging prefix */
    public String log_prefix;

    /** Standard logging prefix */
    public volatile static String std_log_prefix = "Query Server";

    // ------------------------- Queue

    /** Bot queue */
    public volatile Queue<Bot> bot_queue;

    // ------------------------- Socket

    /** UDP Client  */
    private ClientUDP query_socket;

    /** */
    private Ports receive_port;

    // ------------------------- Thread Control

    /** Stops the thread */
    public synchronized void stopServer() { run = false; }

    /** Checks if the thread is ready */
    public synchronized boolean serverReady() { return run; }

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

    // ------------------------- Request Filler

    /**
     * Fills the search query request by establishing a connection
     * with the bot
     */

    private void fillRequest() {
        // Pull a random query generator
        QueryGenerator gen = ThreadManager.generators[new Random(ThreadManager.generators.length).nextInt()];

        // If no requests or queries, wait
        if (bot_queue.size() == 0 || gen.query_queue.size() == 0) {
            MiscUtil.waitasec(.10);
            return;

        } 

        // Pick top off queue
        Query query_to_send = gen.query_queue.poll();
        Bot bot_to_serve = bot_queue.poll();

        // Notification
        Log.sendMessage(log_prefix, "Serving " + bot_to_serve.ip, ConsoleColors.YELLOW);

        // Setup client
        query_socket = new ClientUDP(bot_to_serve.ip, receive_port, log_prefix, ConsoleColors.YELLOW);

        // Send and then close
        if (query_socket.startClient()) {
            query_socket.sendMessage(query_to_send.getQuery());
            query_socket.closeClient();
            Log.sendMessage(log_prefix, "Done serving " + bot_to_serve.ip, ConsoleColors.YELLOW);

        }
    }

    /**
     * Adds a query into the queue <p>
     * Many queries are added. They will be served to the bots as needed.
     * 
     * @param query Search query to enque
     */

    public synchronized void addQuery(Query query) {
        // Pull a random query generator
        QueryGenerator gen = ThreadManager.generators[new Random(ThreadManager.generators.length).nextInt()];

        if (gen.query_queue.size() < gen.MAX_QUERY_QUEUE_SIZE) {
            // Add to queue if the que is under specified size
            gen.query_queue.add(query);

        } else {
            do {
                // Run other generators to check
                MiscUtil.waitasec();
                gen = ThreadManager.generators[new Random(ThreadManager.generators.length).nextInt()];

            } while (gen.query_queue.size() < gen.MAX_QUERY_QUEUE_SIZE);

            // Add to queue if the que is under specified size
            gen.query_queue.add(query);

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
    public synchronized boolean readyForQuery() { 
        for (QueryGenerator gen : ThreadManager.generators) {
            if (gen.query_queue.size() < gen.MAX_QUERY_QUEUE_SIZE) {
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
    
    public synchronized void addBot(Bot bot) {
        if (NetTools.isValid(bot.ip)) {
            // If bot has valid ip, add it to the queue
            bot_queue.add(bot);

        }
    }
}