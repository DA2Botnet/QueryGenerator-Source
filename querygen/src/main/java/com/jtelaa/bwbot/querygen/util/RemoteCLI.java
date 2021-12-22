package com.jtelaa.bwbot.querygen.util;

import java.util.LinkedList;

import com.jtelaa.bwbot.querygen.App;
import com.jtelaa.bwbot.querygen.processes.QueryGenerator;
import com.jtelaa.bwbot.querygen.processes.ThreadManager;
import com.jtelaa.da2.lib.cli.Cases;
import com.jtelaa.da2.lib.cli.LocalCLI;
import com.jtelaa.da2.lib.console.ConsoleBanners;
import com.jtelaa.da2.lib.console.ConsoleColors;
import com.jtelaa.da2.lib.control.Command;
import com.jtelaa.da2.lib.control.ComputerControl;
import com.jtelaa.da2.lib.log.Log;
import com.jtelaa.da2.lib.misc.MiscUtil;

public class RemoteCLI extends LocalCLI {

    private boolean run_as_local;

    public RemoteCLI() { run_as_local = false; }
    public RemoteCLI(boolean run_as_local) { this.run_as_local = run_as_local; }

    @Override
    public void run() {
        if (!run_as_local && App.my_config.getProperty("remote_cli", "false").equalsIgnoreCase("true")) {
            Log.sendMessage("CLI: Preparing Remote CLI");

            while (!run) {
                MiscUtil.waitasec();

            }

            Log.sendMessage("CLI: Starting Local CLI");

            runRX();

        } else {
            MiscUtil.waitasec();

        }
    }

    @Override
    public synchronized String terminal(Command command) {
        Command[] commands = command.split(" ");
        Command cmd = commands[0];

        String response = "";

        // Shutdown
        if (Cases.exit(cmd)) { 
            return shutdown();

        // CMD
        } else if (Cases.command(cmd)) {
            return cmd(cmd);

        // Dump Query Queue
        } else if (Cases.checkCase(cmd, new String[] {"dump", "clear"})) {
            return dump(commands);

        // Get or change size of queue
        } else if (Cases.checkCase(cmd, new String[] {"size"})) {
            return size(commands);

        } else if (Cases.checkCase(cmd, new String[] {"title"})) {
            return title();
            
        } else if (Cases.help(cmd)) {
            return help();

        // } else if (Cases.checkCase(cmd, "add")) {
        //     if (commands.length == 1) {
        //         response += "Error: Add IP";
                
        //     } else if (commands.length == 2) {
        //         Command ip = commands[1];
        //         response += "Adding 1 request from " + ip.command() + " to request queue";
        //         QueryServer.bot_queue.add(new Bot(ip.command()));

        //     } else if (commands.length == 3) {
        //         Command ip = commands[1];
        //         int count = Integer.parseInt(commands[2].command());

        //         response += "Adding " + count + " request(s) from " + ip.command() + " to request queue";

        //         for (int i = 0; i < count; i++) { QueryServer.bot_queue.add(new Bot(ip.command())); }

        //     } else {
        //         response += "Error: To many args";

        //     }

        }

        return response;
        
    }

    private String shutdown() {
        run = false;
        return "Shutting Down CLI";

    }

    private String cmd(Command command) {
        return ComputerControl.sendCommand(command.addBlankUser().addBlankControlID().modifyforSys());

    }

    private String title() {
        String response = "";
        response += ConsoleColors.CLEAR.getEscape() + ConsoleColors.LINES.getEscape();
        response += ConsoleBanners.otherBanner("~banners/MainBanner.txt", ConsoleColors.CYAN_BOLD) + "\n";
        response += ConsoleBanners.otherBanner("~banners/QueryGen.txt", ConsoleColors.YELLOW) + "\n";
        return response;

    }

    private String help() {
        String response = "";
        response += ConsoleColors.CLEAR.getEscape() + ConsoleColors.LINES.getEscape();
        response += ConsoleBanners.otherBanner("~banners/MainBanner.txt", ConsoleColors.CYAN_BOLD) + "\n";
        response += ConsoleBanners.otherBanner("~banners/QueryGen.txt", ConsoleColors.YELLOW) + "\n";

        String help = (
            "Query Generator CLI Help:\n"
            + ConsoleColors.YELLOW_UNDERLINED.getEscape() + "cmd" + ConsoleColors.RESET.getEscape() + " -> pass through a command to the systems OS\n"
            + ConsoleColors.YELLOW_UNDERLINED.getEscape() + "dump/clear {count} {gen id}" + ConsoleColors.RESET.getEscape() + " -> clear x queries from the queue and clear bot queue (default all)\n"
            + ConsoleColors.YELLOW_UNDERLINED.getEscape() + "size {count} {gen id}" + ConsoleColors.RESET.getEscape() + " -> change the size to x (default print the queue size)\n"
            //+ ConsoleColors.YELLOW_UNDERLINED.getEscape() + "add x y" + ConsoleColors.RESET.getEscape() + " -> add ip x to the request queue y times(s)\n"  
        );

        response += help + ConsoleColors.LINES_SHORT.getEscape();
        return response;

    }

    private String size(Command[] commands) {
        String response = "";

        if (commands.length == 1) {
            int size = 0;

            for (QueryGenerator gen : ThreadManager.generators) {
                if (gen.query_queue.size() > size) {
                    size = gen.query_queue.size();

                }
            }

            response += "Max size is " + size;

        } else if (commands.length == 2) {
            if (commands[1].command().equalsIgnoreCase("max")) {
                for (QueryGenerator gen : ThreadManager.generators) {
                    gen.MAX_QUERY_QUEUE_SIZE = 10000;
                    response += "Query Queue size sey to " + gen.MAX_QUERY_QUEUE_SIZE; 

                }

            } else if (commands[1].command().equalsIgnoreCase("all")) {
                size(new Command[] {new Command("size")});

            } else {
                try {
                    int new_size = Integer.parseInt(commands[1].command());

                    if (new_size <= ThreadManager.generators.length - 1) {
                        ThreadManager.generators[new_size].MAX_QUERY_QUEUE_SIZE = 10000;
                        response += "Query Queue size set to " + ThreadManager.generators[new_size].MAX_QUERY_QUEUE_SIZE + " on generator " + new_size; 

                    } else {
                        response += "Setting all Query Queues to " + new_size;
                        for (QueryGenerator gen : ThreadManager.generators) {
                            gen.MAX_QUERY_QUEUE_SIZE = new_size;

                        }
                    }

                } catch (NumberFormatException e) {
                    size(new Command[] {new Command("size")});

                }
            }
            
        } else if (commands.length == 3) {
            QueryGenerator[] generators = ThreadManager.generators;
            int new_size = 10000;

            try {
                int tmp = Integer.parseInt(commands[1].command());

                if (tmp <= ThreadManager.generators.length - 1) {
                    generators = new QueryGenerator[] { ThreadManager.generators[tmp] };

                } else {
                    new_size = tmp;

                }

            } catch (NumberFormatException e) {
                if (commands[1].command().equalsIgnoreCase("all")) {
                    generators = ThreadManager.generators;

                } else if (commands[1].command().equalsIgnoreCase("max")) {
                    
                } else {
                    return "Err Arg";

                }
            }

            try {
                int tmp = Integer.parseInt(commands[2].command());

                if (tmp <= ThreadManager.generators.length - 1) {
                    generators = new QueryGenerator[] { ThreadManager.generators[tmp] };

                } else {
                    new_size = tmp;

                }

            } catch (NumberFormatException e) {
                if (commands[2].command().equalsIgnoreCase("all")) {
                    generators = ThreadManager.generators;

                } else if (commands[1].command().equalsIgnoreCase("max")) {
                    
                } else {
                    return "Err Arg";

                }
            }

            String gens_str = "";
            for (QueryGenerator generator : generators) { 
                String name = generator.getName();
                gens_str += name.substring(name.indexOf("("), name.indexOf(")")) + ", ";
            
            }

            gens_str = gens_str.substring(0, gens_str.lastIndexOf(", "));

            response += "Setting size to " + new_size + " from " + gens_str;
            
            for (QueryGenerator generator : generators) {
                generator.MAX_QUERY_QUEUE_SIZE = new_size;
                
            }
        }

        return response;

    }

    private String dump(Command[] commands) {
        String response = "Dumping Query Queue\n";
        QueryGenerator[] gen;
        int qty_to_dump = 100;

        if (commands.length == 1 || (commands.length == 2 && commands[1].command().equalsIgnoreCase("all"))) {
            response += "Dumping all";
            for (QueryGenerator generator : ThreadManager.generators) { generator.query_queue = new LinkedList<>(); }
            return response;
        
        } else if (commands.length == 2) {
            try {
                int gen_id = Integer.parseInt(commands[1].command());

                if (gen_id > ThreadManager.generators.length - 1) {
                    gen = ThreadManager.generators;
                    qty_to_dump = gen_id;
                    response += "Dumping " + qty_to_dump + " from all";

                    for (QueryGenerator generator : gen) { 
                        for (int i = 0; i < qty_to_dump; i++) {
                            generator.query_queue.remove();

                        }
                    }

                    return response;

                } else {
                    gen = new QueryGenerator[] { ThreadManager.generators[gen_id] };
                    response += "Dumping all from generator " + gen_id;
                    for (QueryGenerator generator : gen) { generator.query_queue = new LinkedList<>(); }

                }

            } catch (NumberFormatException e) {
                return dump(new Command[] { new Command("dump") });

            }

        } else if (commands.length == 3) {
            if (commands[1].command().equalsIgnoreCase("all")) {
                gen = ThreadManager.generators;

            } else {
                try {
                    int gen_id = Integer.parseInt(commands[1].command());

                    if (gen_id > ThreadManager.generators.length - 1) {
                        gen = new QueryGenerator[] { ThreadManager.generators[gen_id] };
                        qty_to_dump = gen_id;

                    } else {
                        gen = new QueryGenerator[] { ThreadManager.generators[gen_id] };

                    }

                } catch (NumberFormatException e) {
                    gen = ThreadManager.generators;

                }
            }

            if (commands[2].command().equalsIgnoreCase("all")) {
                for (QueryGenerator generator : gen) {
                    if (qty_to_dump < generator.query_queue.size()) {
                        qty_to_dump = generator.query_queue.size();

                    }
                }
                
            } else {
                try {
                    qty_to_dump = Integer.parseInt(commands[2].command());

                } catch (NumberFormatException e) {
                    for (QueryGenerator generator : gen) {
                        if (qty_to_dump < generator.query_queue.size()) {
                            qty_to_dump = generator.query_queue.size();
    
                        }
                    }
                }

            }

            String gens_str = "";
            for (QueryGenerator generator : gen) { 
                String name = generator.getName();
                gens_str += name.substring(name.indexOf("("), name.indexOf(")")) + ", ";
            
            }

            gens_str = gens_str.substring(0, gens_str.lastIndexOf(", "));

            response += "Dumping " + qty_to_dump + " from " + gens_str;
            
            for (QueryGenerator generator : gen) {
                for (int i = 0; i < qty_to_dump; i++) {
                    generator.query_queue.remove();

                }
            }

            return response;

        }

        return "Dumping Err";

    }

}
