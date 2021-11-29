package com.jtelaa.bwbot.querygen;

import java.util.Properties;

import com.jtelaa.bwbot.bwlib.BWSQLQueries;
import com.jtelaa.bwbot.querygen.processes.QueryGenerator;
import com.jtelaa.bwbot.querygen.processes.QueryServer;
import com.jtelaa.bwbot.querygen.processes.RequestServer;
import com.jtelaa.bwbot.querygen.util.RemoteCLI;
import com.jtelaa.bwbot.querygen.util.SysCLI;
import com.jtelaa.da2.lib.config.PropertiesUtils;
import com.jtelaa.da2.lib.console.ConsoleBanners;
import com.jtelaa.da2.lib.console.ConsoleColors;
import com.jtelaa.da2.lib.control.ComputerControl;
import com.jtelaa.da2.lib.log.Log;
import com.jtelaa.da2.lib.misc.MiscUtil;
import com.jtelaa.da2.lib.net.NetTools;
import com.jtelaa.da2.lib.sql.DA2SQLQueries;
import com.jtelaa.da2.lib.sql.SQL;

/**
 * Main for the query generation application
 * 
 * @author Joseph
 * @since 2
 * 
 * @see com.jtelaa.bwbot.bwlib.Query
 * @see com.jtelaa.bwbot.querygen.processes.QueryGenerator
 */

 // TODO Find a way to include files in the jar

public class Main {
    /** The remote cli local object */ 
    public static RemoteCLI rem_cli;

    /** The system cli local object */ 
    public static SysCLI sys_cli;
    
    /** Local configuration handler */
    public static Properties my_config;

    /** Properties file path */
    public static String properties_path = "~/querygen_config.properties";
    public static String banners_directory = "~/banners/";

    public static void main(String[] args) {
        // Check for first time setup
        boolean first_time = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("setup")) {
                Log.sendManSysMessage("Loading first - time config");
                first_time = true;
            }
        }

        // Load normally if not first time
        if (first_time) {
            // Get banners
            Log.sendManSysMessage("Loading banners");
            ComputerControl.sendCommand("mkdir ~/banners/");
            ComputerControl.sendCommand("cd ~/banners/ && curl https://raw.githubusercontent.com/DA2Botnet/DA2Botnet.github.io/main/banners/MainBanner.txt > MainBanner.txt");
            ComputerControl.sendCommand("cd ~/banners/ && curl https://raw.githubusercontent.com/DA2Botnet/DA2Botnet.github.io/main/banners/QueryGen.txt > QueryGen.txt");

            // Get config template
            Log.sendManSysMessage("Loading config template");
            ComputerControl.sendCommand("cd ~/ && curl https://raw.githubusercontent.com/DA2Botnet/QueryGenerator-Source/main/config_template/querygen_config.properties > querygen_config.properties");

        }

        my_config = PropertiesUtils.importConfig(properties_path); 

        my_config.setProperty("log_ip", resolveDefaultLogIP());
        my_config.setProperty("db_ip", resolveDefaultDBIP());

        querySettings();

        // Start Logging
        Log.loadConfig(my_config, args);
        Log.clearHistory();

        // List properties
        for (String line : PropertiesUtils.listProperties(my_config)) { Log.sendSysMessage(line); }

        // Startup
        Log.sendSysMessage("Main: Starting.....\n");
        Log.sendSysMessage(ConsoleBanners.otherBanner(banners_directory + "Rewards.txt", ConsoleBanners.EXTERNAL, ConsoleColors.CYAN_BOLD));
        Log.sendSysMessage(ConsoleBanners.otherBanner(banners_directory + "QueryGen.txt", ConsoleBanners.EXTERNAL, ConsoleColors.YELLOW));
        Log.sendSysMessage("\n\n\n");

        // Start logging client
        Log.openClient(my_config.getProperty("log_ip", my_config.getProperty("log_ip")));
        Log.openConnector();

        // Request server setup
        RequestServer req_srv = new RequestServer();
        Log.sendMessage("Main: Starting request server", ConsoleColors.GREEN);
        req_srv.start();
        
        // Query server setup
        QueryServer qry_serv = new QueryServer();
        Log.sendMessage("Main: Starting query server", ConsoleColors.GREEN);
        qry_serv.start();

        // Query generator setup
        QueryGenerator qry_gen = new QueryGenerator();
        Log.sendMessage("Main: Starting query generator", ConsoleColors.GREEN);
        qry_gen.start();

        // Done
        Log.sendMessage("Main: Done", ConsoleColors.GREEN);

        // Remote CLI
        if (my_config.getProperty("remote_cli", "false").equalsIgnoreCase("true")) {
            Log.sendMessage("CLI: Remote Cli Enabled");
            rem_cli = new RemoteCLI();
            rem_cli.start();

        } else {
            Log.sendMessage("CLI: No Remote CLI");

        }

        // Local CLI
        if (my_config.getProperty("local_cli", "true").equalsIgnoreCase("true")) {
            Log.sendMessage("CLI: Local CLI Allowed");
            sys_cli = new SysCLI();
            sys_cli.start();

        } else {
            Log.sendMessage("CLI: No Local CLI");

        }

        Log.sendMessage("CLI: Waiting 45s for CLI bootup");
        MiscUtil.waitasec(45);
        Log.sendMessage("CLI: Done waiting");

        if (my_config.getProperty("remote_cli", "false").equalsIgnoreCase("true")) { rem_cli.runCLI(); }
        if (my_config.getProperty("local_cli", "true").equalsIgnoreCase("true")) { sys_cli.runCLI(); }

        /*

        // Wait
        MiscUtil.waitasec();

        Log.sendLogMessage("Done! Shutting down");
        Log.closeLog();
        req_srv.stopServer();
        qry_serv.stopReceiver();
        qry_gen.stopGen();

        */
    }

    /**
     * 
     * @return
     */

    private static void querySettings() {
        Properties my_new_config = new Properties();
        String connectionURL = SQL.getConnectionURL(my_config.getProperty("db_ip"), "BW_Main", "querygen", "Passw0rd!");

        // TODO Implement other config        

        // Get ID
        int id = DA2SQLQueries.getID("QueryGenerators", connectionURL);
        my_new_config.setProperty("id", id + "");
        
        // Get Request Port
        my_new_config.setProperty("request_port", BWSQLQueries.getRequestPort(id, "QueryGenerators", connectionURL) + "");

        // Get Request Port
        my_new_config.setProperty("receive_port", BWSQLQueries.getReceivePort(id, "QueryGenerators", connectionURL) + "");

        my_config = my_new_config;
        PropertiesUtils.exportConfig(properties_path, my_config);

    }

    /**
     * 
     * @return
     */

    private static String resolveDefaultDBIP() {
        String my_ip = NetTools.getLocalIP();

        return my_ip.substring(0, my_ip.lastIndexOf(".")) + 2;
    }

    /**
     * 
     * @return
     */

    private static String resolveDefaultLogIP() {
        String my_ip = NetTools.getLocalIP();

        return my_ip.substring(0, my_ip.lastIndexOf(".")) + 1;
    }
    
}
