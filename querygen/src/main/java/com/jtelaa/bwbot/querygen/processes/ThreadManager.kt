package com.jtelaa.bwbot.querygen.processes

import com.jtelaa.bwbot.bwlib.BWPorts
import com.jtelaa.bwbot.querygen.util.InvalidThreadCountException
import com.jtelaa.da2.lib.console.ConsoleColors
import com.jtelaa.da2.lib.log.Log
import com.jtelaa.da2.lib.net.NetTools

import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException

import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException

/**
 * Manages all of the threads in the system (Except for main thread (which this runs on) and CLIs)
 *
 * @author Joseph
 * @since 2
 */
object ThreadManager {
    // ------------------------- Processes
    /** Query Generator threads  */
    @Volatile
    lateinit var generators: Array<QueryGenerator?>

    /** Query Server threads  */
    @Volatile
    lateinit var query_servers: Array<QueryServer?>

    /** Request Server threads  */
    @Volatile
    lateinit var request_servers: Array<RequestServer?>

    // ------------------------- Logs

    /** Standard logging prefix  */
    @Volatile
    var log_prefix = "Thread Manager:"

    // ------------------------- Start

    /**
     * Sets up a single instance of each process
     */

    @Synchronized
    fun setupProcesses() {
        setupProcesses(1, 1, 1)

    }

    /**
     * Sets up the processes given the count
     *
     * @param gen_count Query Generator Count
     * @param qserv_count Query Server Count
     * @param reqsrv_count Request Server Count
     */

    @Synchronized
    fun setupProcesses(gen_count: Int, qserv_count: Int, reqsrv_count: Int) {
        Log.sendMessage(log_prefix, "initializing threads", ConsoleColors.GREEN)

        try {
            setupGenerators(gen_count)
            setupQueryServers(qserv_count, NetTools.getLocalIP(), BWPorts.QUERY_REQUEST.getPort())
            setupRequestServers(reqsrv_count, NetTools.getLocalIP(), BWPorts.QUERY_RECEIVE.getPort())

        } catch (e1: InvalidThreadCountException) {
            Log.sendMessage(log_prefix, e1, ConsoleColors.RED)
            return

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
    @Synchronized
    @Throws(FileNotFoundException::class, IOException::class, ParseException::class)
    fun setupProcesses(json_file_path: String?) {
        // Load File
        Log.sendMessage(log_prefix, "initializing threads", ConsoleColors.GREEN)
        val json_object: JSONObject = JSONParser().parse(FileReader(json_file_path)) as JSONObject

        // Count Generators
        val generator_object: JSONObject = json_object.get("query_generator") as JSONObject
        
        try {
            // Setup generators with the desired count
            setupGenerators(generator_object.get("generator_count") as Int)

        // If invalid number
        } catch (e: InvalidThreadCountException) {
            Log.sendMessage(log_prefix, e, ConsoleColors.RED)
            
            try {
                // Setup single instance
                setupGenerators(1)

            // If theres a strange error, stop
            } catch (e1: InvalidThreadCountException) {
                Log.sendMessage(log_prefix, e1, ConsoleColors.RED)
                return

            }
        }

        // Count Query Servers
        val query_server_object: JSONObject = json_object.get("query_server") as JSONObject
        val query_servers_count = query_server_object.get("interface_count") as Int
        
        for (interface_num in 0 until query_servers_count) {
            // Get each interface specification
            val query_servers_indiv_object: JSONObject = query_server_object.get("interface$interface_num") as JSONObject
            
            try {
                // Setup servers given parameters
                setupQueryServers(
                        query_servers_indiv_object.get("thread_count") as Int,
                        query_servers_indiv_object.get("ip_address") as String,
                        query_servers_indiv_object.get("port") as Int
                )

            // If error in the count
            } catch (e: InvalidThreadCountException) {
                try {
                    // Setup single instance
                    setupQueryServers(1, NetTools.getLocalIP(), BWPorts.QUERY_RECEIVE.getPort())

                // If theres a strange error, stop
                } catch (e1: InvalidThreadCountException) {
                    Log.sendMessage(log_prefix, e1, ConsoleColors.RED)
                    return

                }
            }
        }

        // Request Servers COunt
        val request_server_object: JSONObject = json_object.get("requst_server") as JSONObject
        val request_servers_count = request_server_object.get("interface_count") as Int
        
        for (interface_num in 0 until request_servers_count) {
            // Get each interface specification
            val request_servers_indiv_object: JSONObject = request_server_object.get("interface$interface_num") as JSONObject
            
            try {
                // Setup servers given parameters
                setupQueryServers(
                        request_servers_indiv_object.get("thread_count") as Int,
                        request_servers_indiv_object.get("ip_address") as String,
                        request_servers_indiv_object.get("port") as Int
                )

            // If error in the count
            } catch (e: InvalidThreadCountException) {
                try {
                    // Setup single instance
                    setupRequestServers(1, NetTools.getLocalIP(), BWPorts.QUERY_RECEIVE.getPort())

                // If theres a strange error, stop
                } catch (e1: InvalidThreadCountException) {
                    Log.sendMessage(log_prefix, e1, ConsoleColors.RED)
                    return

                }
            }
        }

        // Done
        Log.sendMessage(log_prefix, "Done!", ConsoleColors.GREEN)

    }

    // ------------------------- Thread Util

    /**
     * Start all the setup processes
     */

    @Synchronized
    fun startProcesses() {
        // Message
        Log.sendMessage(log_prefix, "Starting all (" + (generators.size + query_servers.size + request_servers.size) + ") processes", ConsoleColors.GREEN)

        // Start
        startGenerators()
        startQueryServers()
        startRequestServers()

        // Done
        Log.sendMessage(log_prefix, "Done", ConsoleColors.GREEN)

    }

    /**
     * Stop all of the processes
     */

    @Synchronized
    fun stopProcesses() {
        // Message
        Log.sendMessage(log_prefix, "Stopping all (" + (generators.size + query_servers.size + request_servers.size) + ") processes", ConsoleColors.GREEN)

        // Stop
        stopGenerators()
        stopQueryServers()
        stopRequestServers()

        // Done
        Log.sendMessage(log_prefix, "Done", ConsoleColors.GREEN)

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

    @Synchronized
    @Throws(InvalidThreadCountException::class)
    private fun checkThreadCount(thread_count: Int, type: String) {
        // Log
        Log.sendLogMessage(log_prefix + "Building " + thread_count + " " + type + "s", ConsoleColors.GREEN)

        // Throw if count is below -1 or above 20
        if (thread_count <= 0 || thread_count > 20) {
            throw InvalidThreadCountException(thread_count)

        }
    }

    /**
     * Print the status of each thread
     */

    @Synchronized
    fun printThreadStatus() {
        // Output
        var output = ""

        // Status indicators
        val running: String = ConsoleColors.GREEN.toString() + "Running" + ConsoleColors.CLEAR.toString()
        val not_running: String = ConsoleColors.RED.toString() + "Not Running" + ConsoleColors.CLEAR.toString()

        // Check status of the generators
        output += "Generators:\n"
        for (i in generators.indices) {
            output += generators[i]!!.log_prefix + (if (generators[i]!!.generatorReady()) running else not_running) + "\n"
        
        }

        // Check status of the request servers
        output += "Request Servers:\n"
        for (i in request_servers.indices) {
            output += request_servers[i]!!.log_prefix + (if (request_servers[i]!!.serverReady()) running else not_running) + "\n"
        
        }

        // Check statis of the query servers
        output += "Query Servers:\n"
        for (i in query_servers.indices) {
            output += query_servers[i]!!.log_prefix + (if (query_servers[i]!!.serverReady()) running else not_running) + "\n"
        
        }

        // Send output
        Log.sendMessage(output)

    }

    // ------------------------- Query Generators

    /**
     * Setup the query generators
     *
     * @param count count to setup
     *
     * @throws InvalidThreadCountException
     */

    @Synchronized
    @Throws(InvalidThreadCountException::class)
    fun setupGenerators(count: Int) {
        // Thread count and setup array
        checkThreadCount(count, "Query Generator")
        generators = arrayOfNulls<QueryGenerator>(count)
        
        for (i in 0 until count) {
            // Query generator setup
            generators[i] = QueryGenerator(i)

        }
    }

    /**
     * Start the generators
     */

    @Synchronized
    fun startGenerators() {
        for (i in generators.indices) {
            startGenerator(i)

        }
    }

    /**
     * Start specific generators
     */

    @Synchronized
    fun startGenerators(ids: IntArray) {
        for (id in ids) {
            startGenerator(id)

        }
    }

    /**
     *
     */
    @Synchronized
    fun startGenerator(id: Int) {
        if (id >= generators.size) {
            Log.sendMessage(log_prefix, "Invalid Query Generator ID", ConsoleColors.RED)
            return

        }

        Log.sendMessage(log_prefix, "Starting Query Generator $id", ConsoleColors.GREEN)
        generators[id]!!.start()

    }

    /**
     *
     */

    @Synchronized
    fun stopGenerators() {
        for (i in generators.indices) {
            stopGenerator(i)

        }
    }

    /**
     *
     */

    @Synchronized
    fun stopGenerators(ids: IntArray) {
        for (id in ids) {
            stopGenerator(id)

        }
    }

    /**
     *
     */

    @Synchronized
    fun stopGenerator(id: Int) {
        if (id >= generators.size) {
            Log.sendMessage(log_prefix, "Invalid Query Generator ID", ConsoleColors.RED)
            return

        }

        Log.sendMessage(log_prefix, "Stopping Query Generator $id", ConsoleColors.GREEN)
        generators[id]!!.stopGenerator()

    }

    // ------------------------- Request Servers

    /**
     *
     * @param count
     *
     * @throws InvalidThreadCountException
     */

    @Synchronized
    @Throws(InvalidThreadCountException::class)
    fun setupRequestServers(count: Int, ip: String?, port: Int) {
        // Thread count and setup array
        checkThreadCount(count, "Request Servers")
        request_servers = arrayOfNulls<RequestServer>(count)
        
        for (i in 0 until count) {
            // Query generator setup
            request_servers[i] = RequestServer(i)

        }
    }

    /**
     *
     */

    @Synchronized
    fun startRequestServers() {
        for (i in request_servers.indices) {
            startRequestServer(i)

        }
    }

    /**
     *
     */

    @Synchronized
    fun startRequestServers(ids: IntArray) {
        for (id in ids) {
            startRequestServer(id)

        }
    }

    /**
     *
     */

    @Synchronized
    fun startRequestServer(id: Int) {
        if (id >= request_servers.size) {
            Log.sendMessage(log_prefix, "Invalid Request Server ID", ConsoleColors.RED)
            return

        }

        Log.sendMessage(log_prefix, "Starting Request Server $id", ConsoleColors.GREEN)
        request_servers[id].start()

    }

    /**
     *
     */

    @Synchronized
    fun stopRequestServers() {
        for (i in generators.indices) {
            stopRequestServer(i)

        }
    }

    /**
     *
     */

    @Synchronized
    fun stopRequestServers(ids: IntArray) {
        for (id in ids) {
            stopRequestServer(id)

        }
    }

    /**
     *
     */

    @Synchronized
    fun stopRequestServer(id: Int) {
        if (id >= request_servers.size) {
            Log.sendMessage(log_prefix, "Invalid Request Server ID", ConsoleColors.RED)
            return

        }

        Log.sendMessage(log_prefix, "Stopping Request Server $id", ConsoleColors.GREEN)
        request_servers[id].stopServer()

    }

    // ------------------------- Query Servers

    /**
     *
     * @param count
     *
     * @throws InvalidThreadCountException
     */

    @Synchronized
    @Throws(InvalidThreadCountException::class)
    fun setupQueryServers(count: Int, ip: String?, port: Int) {
        // Thread count and setup array
        checkThreadCount(count, "Query Servers")
        query_servers = arrayOfNulls<QueryServer>(count)
        
        for (i in 0 until count) {
            // Query generator setup
            query_servers[i] = QueryServer(i)

        }
    }

    /**
     *
     */

    @Synchronized
    fun startQueryServers() {
        for (i in request_servers.indices) {
            startQueryServer(i)

        }
    }

    /**
     *
     */

    @Synchronized
    fun startQueryServers(ids: IntArray) {
        for (id in ids) {
            startQueryServer(id)

        }
    }

    /**
     *
     */

    @Synchronized
    fun startQueryServer(id: Int) {
        if (id >= query_servers.size) {
            Log.sendMessage(log_prefix, "Invalid Query Server ID", ConsoleColors.RED)
            return

        }

        Log.sendMessage(log_prefix, "Starting Query Server $id", ConsoleColors.GREEN)
        query_servers[id].start()

    }

    /**
     *
     */

    @Synchronized
    fun stopQueryServers() {
        for (i in generators.indices) {
            stopQueryServer(i)

        }
    }

    /**
     *
     */

    @Synchronized
    fun stopQueryServers(ids: IntArray) {
        for (id in ids) {
            stopQueryServer(id)

        }
    }

    /**
     *
     */

    @Synchronized
    fun stopQueryServer(id: Int) {
        if (id >= query_servers.size) {
            Log.sendMessage(log_prefix, "Invalid Query Server ID", ConsoleColors.RED)
            return

        }

        Log.sendMessage(log_prefix, "Stopping Query Server $id", ConsoleColors.GREEN)
        query_servers[id].stopServer()
        
    }
}