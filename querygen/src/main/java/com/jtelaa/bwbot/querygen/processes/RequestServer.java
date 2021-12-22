package com.jtelaa.bwbot.querygen.processes;

import java.util.Random;

import com.jtelaa.bwbot.bwlib.BWMessages;
import com.jtelaa.bwbot.bwlib.BWPorts;
import com.jtelaa.bwbot.querygen.App;
import com.jtelaa.bwbot.querygen.util.GenericThread;
import com.jtelaa.da2.lib.bot.Bot;
import com.jtelaa.da2.lib.console.ConsoleColors;
import com.jtelaa.da2.lib.log.Log;
import com.jtelaa.da2.lib.misc.MiscUtil;
import com.jtelaa.da2.lib.net.server.ServerUDP;

/**
 * This process accepts the requests for the query.
 * It enques them to the query server if the request is valid.
 * 
 * @since 2
 * @author Joseph
 * 
 * @see com.jtelaa.da2.querygen.QueryServer.java
 * @see com.jtelaa.da2.querygen.QueryGenerator.java
 */

public class RequestServer extends GenericThread {

    // ------------------------- Constructors

    /**
     * Program init
     */

    public RequestServer() {
        log_prefix = std_log_prefix;
        this.setName(log_prefix);
        log_prefix += ": ";

    }

    /**
     * Program init
     * 
     * @param id thread id
     */

    public RequestServer(int id) {
        this.log_prefix = std_log_prefix + "(" + id + ")";
        this.setName(log_prefix);
        this.log_prefix += ":";

    }

    // ------------------------- Thread Control

    /** Stops the thread */
    public synchronized void stopServer() { run = false; }

    /** Checks if the thread is ready */
    public synchronized boolean serverReady() { return run; }

    // ------------------------- Logging

    /** Logging prefix */
    public String log_prefix;

    /** Standard logging prefix */
    public volatile static String std_log_prefix = "Request Server";

    // ------------------------- Thread Processes

    /**
     * Thread code
     */

    public void run() {
        // Setup server
        ServerUDP server = new ServerUDP(BWPorts.QUERY_REQUEST.checkForPreset(App.my_config, "request_port"), log_prefix, ConsoleColors.GREEN);
        
        // Bot address var
        String bot_address;
        // If server is ready
        if (server.startServer()) {
            while (run) {
                // If a request message is receiver
                String response = server.getMessage();

                try {
                    // If message is a request
                    if (response.contains(BWMessages.QUERY_REQUEST_MESSAGE.getMessage())) {
                        bot_address = server.getClientAddress();

                        ThreadManager.query_servers[new Random(ThreadManager.query_servers.length).nextInt()].addBot(new Bot(bot_address));
                        Log.sendMessage(log_prefix, "Request from " + bot_address, ConsoleColors.YELLOW);
                
                    // If the message does not contain a request
                    } else {
                        Log.sendMessage(log_prefix, "Invalid Request", ConsoleColors.YELLOW);

                    }

                // Error handling
                } catch (Exception e) {
                    Log.sendMessage(log_prefix, "Could not resolve requesting bot's IP", ConsoleColors.RED);
                    MiscUtil.waitasec(.1);

                }
            }

        } else {
            // Is not ready
            run = false;

        }

        // Exit
        Log.sendMessage(log_prefix + "Request Server Process Stopped!");
        
    }
}