package com.jtelaa.bwbot.querygen;

import java.util.Properties;

import com.jtelaa.bwbot.querygen.processes.ThreadManager;
import com.jtelaa.bwbot.querygen.searches.SearchHandler;
import com.jtelaa.bwbot.querygen.util.RemoteCLI;
import com.jtelaa.bwbot.querygen.util.SysCLI;
import com.jtelaa.da2.lib.config.PropertiesUtils;
import com.jtelaa.da2.lib.console.ConsoleBanners;
import com.jtelaa.da2.lib.console.ConsoleColors;
import com.jtelaa.da2.lib.control.ComputerControl;
import com.jtelaa.da2.lib.franchise.FranchiseUtils;
import com.jtelaa.da2.lib.log.Log;
import com.jtelaa.da2.lib.misc.MiscUtil;
import com.jtelaa.da2.lib.net.NetTools;
import com.jtelaa.da2.lib.sql.DA2SQLQueries;
import com.jtelaa.da2.lib.sql.EmptySQLURLException;
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

public class App {
    /** The remote cli local object */ 
    public static RemoteCLI rem_cli;

    /** The system cli local object */ 
    public static SysCLI sys_cli;
    
    /** Local configuration handler */
    public static Properties my_config;

    /** Properties file path */
    public static String properties_path = "~/qgen/querygen_config.properties";
    public static String json_path = "~/qgen/thread_config.json";
    public static String banners_directory = "~/qgen/banners/";
    public volatile static String stats_file_path = "~/qgen/stats/";
    
    public static void main(String[] args) {
        // Check for first time setup
        boolean first_time = false;
        boolean multi_interface = true;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("setup")) {
                Log.sendManSysMessage("Loading first - time config");
                first_time = true;

            } if (arg.equalsIgnoreCase("single")) {
                multi_interface = false;

            }
        }

        createDirectories();

        // Load normally if not first time
        if (first_time) { 
            loadRemote(); 

        }

        // Load
        loadConfig();
        logBegin(args);
        interfaceSetup(multi_interface);

        // Done
        Log.sendMessage("Main: Done", ConsoleColors.GREEN);

        // CLI
        CLIBegin();

        // Shutdown
        Log.sendMessage("Main: Shutting down!");

    }

    /**
     * Setup the interfaces
     * 
     * @param multi_interface
     */

    private static void interfaceSetup(boolean multi_interface) {
        if (multi_interface) {
            try {
                ThreadManager.setupProcesses(json_path);
    
            } catch (Exception e) {
                Log.sendMessage("Main: ", e, ConsoleColors.RED);
                System.exit(0);
    
            }

        } else {
            ThreadManager.setupProcesses();

        }

    }

    /**
     * Setup the logging
     * 
     * @param args
     */

    private static void logBegin(String[] args) {
        // Start Logging
        Log.loadConfig(my_config, args);
        Log.clearHistory();

        // Startup
        Log.sendSysMessage("Main: Starting.....\n");
        Log.sendSysMessage(ConsoleBanners.otherBanner(banners_directory + "MainBanner.txt", ConsoleBanners.EXTERNAL, ConsoleColors.CYAN_BOLD));
        Log.sendSysMessage(ConsoleBanners.otherBanner(banners_directory + "QueryGen.txt", ConsoleBanners.EXTERNAL, ConsoleColors.YELLOW));
        Log.sendSysMessage("\n\n\n");

        // Start logging client
        Log.openClient(my_config.getProperty("log_ip", my_config.getProperty("log_ip")));
        Log.openConnector();

    }

    /**
     * Load the config files
     */

    private static void loadConfig() {
        // Load config
        my_config = PropertiesUtils.importConfig(properties_path); 

        // Set franchise
        my_config.setProperty("log_ip", FranchiseUtils.resolveDefaultLogIP());
        my_config.setProperty("db_ip", FranchiseUtils.resolveDefaultDBIP());

        try {
            querySettings();

        } catch (EmptySQLURLException e) {
            e.printStackTrace();

        }
    }

    /**
     * Load the remote files
     */

    private static void createDirectories() {
        // Make root directory
        ComputerControl.sendCommand("cd ~/ && mkdir qgen");

    }

    /**
     * Load the remote files
     */

    private static void loadRemote() {
        // Load banners
        ConsoleBanners.loadRemoteBanners("QueryGen");

        // Get config template
        Log.sendManSysMessage("Loading config template");
        ComputerControl.sendCommand("cd ~/ && curl https://raw.githubusercontent.com/DA2Botnet/QueryGenerator-Source/main/config_template/querygen_config.properties > querygen_config.properties");

        // Get thread config template
        Log.sendManSysMessage("Loading thread config template");
        ComputerControl.sendCommand("cd ~/ && curl https://raw.githubusercontent.com/DA2Botnet/QueryGenerator-Source/main/config_template/thread_config.jsom > thread_config.json");

        // Load searches
        if (!SearchHandler.searchesPresent()) { SearchHandler.loadRemoteSearches(); }

    }

    /**
     * Query for new settings
     * 
     * @return
     */

    private static void querySettings() throws EmptySQLURLException {
        String database = my_config.getProperty("database");
        String table_name = my_config.getProperty("table");
        String id_type = my_config.getProperty("key");
        String db_ip = my_config.getProperty("db_ip");
        String user = my_config.getProperty("db_user");
        String passwd = my_config.getProperty("db_passwd");

        DA2SQLQueries.connectionURL = SQL.getConnectionURL(db_ip, database, user, passwd);

        // TODO Implement other config        

        // Get ID
        int id = DA2SQLQueries.getID(database, table_name, id_type, "IP", NetTools.getLocalIP());
        my_config.setProperty("id", id + "");
        
        // Get Request Port
        my_config.setProperty("request_port", DA2SQLQueries.queryByID(database, table_name, id_type, id, "RequestPort"));

        // Get Request Port
        my_config.setProperty("receive_port", DA2SQLQueries.queryByID(database, table_name, id_type, id, "RecievePort"));

        PropertiesUtils.exportConfig(properties_path, my_config);

    }

    /**
     * Start the CLI
     */

    private static void CLIBegin() {
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

    }
}
