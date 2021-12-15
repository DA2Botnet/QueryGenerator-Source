package com.jtelaa.bwbot.querygen.processes

import java.io.FileNotFoundException

/**
 *
 */
object ThreadManager {
    // ------------------------- Processes
    /**  */
    @Volatile
    private var generators: Array<QueryGenerator?>

    /**  */
    @Volatile
    private var query_servers: Array<QueryServer?>

    /**  */
    @Volatile
    private var request_servers: Array<RequestServer?>
    // ------------------------- Logs
    /** Standard logging prefix  */
    @Volatile
    var log_prefix = "Thread Manager:"
    // ------------------------- Start
    /**
     *
     * @param json_file_path
     *
     * @throws ParseException
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Synchronized
    @Throws(FileNotFoundException::class, IOException::class, ParseException::class)
    fun setupProcesses(json_file_path: String?) {
        // Load File
        Log.sendMessage(log_prefix + "initializing threads", ConsoleColors.GREEN)
        val json_object: JSONObject = JSONParser().parse(FileReader(json_file_path)) as JSONObject

        // Count Generators
        val generator_object: JSONObject = json_object.get("query_generator") as JSONObject
        try {
            setupGenerators(generator_object.get("generator_count") as Int)
        } catch (e: InvalidThreadCountException) {
            Log.sendMessage(log_prefix, e, ConsoleColors.RED)
            try {
                setupGenerators(1)
            } catch (e1: InvalidThreadCountException) {
                Log.sendMessage(log_prefix, e1, ConsoleColors.RED)
                return
            }
        }

        // Count Query Servers
        val query_server_object: JSONObject = json_object.get("query_server") as JSONObject
        val query_servers_count = query_server_object.get("interface_count") as Int
        for (interface_num in 0 until query_servers_count) {
            val query_servers_indiv_object: JSONObject = query_server_object.get("interface$interface_num") as JSONObject
            try {
                setupQueryServers(
                        query_servers_indiv_object.get("thread_count") as Int,
                        query_servers_indiv_object.get("ip_address") as String,
                        query_servers_indiv_object.get("port") as Int
                )
            } catch (e: InvalidThreadCountException) {
                try {
                    setupQueryServers(1, NetTools.getLocalIP(), BWPorts.QUERY_RECEIVE.getPort())
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
            val request_servers_indiv_object: JSONObject = request_server_object.get("interface$interface_num") as JSONObject
            try {
                setupQueryServers(
                        request_servers_indiv_object.get("thread_count") as Int,
                        request_servers_indiv_object.get("ip_address") as String,
                        request_servers_indiv_object.get("port") as Int
                )
            } catch (e: InvalidThreadCountException) {
                try {
                    setupQueryServers(1, NetTools.getLocalIP(), BWPorts.QUERY_REQUEST.getPort())
                } catch (e1: InvalidThreadCountException) {
                    Log.sendMessage(log_prefix, e1, ConsoleColors.RED)
                    return
                }
            }
        }
        Log.sendMessage(log_prefix + "Done!", ConsoleColors.GREEN)
    }
    // ------------------------- Thread Util
    /**
     *
     */
    @Synchronized
    fun startProcesses() {
        Log.sendMessage(log_prefix + "Starting all (" + (generators.size + query_servers.size + request_servers.size) + ") processes", ConsoleColors.GREEN)
        startGenerators()
        startQueryServers()
        startRequestServers()
        Log.sendMessage(log_prefix + "Done", ConsoleColors.GREEN)
    }

    /**
     *
     */
    @Synchronized
    fun stopProcesses() {
        Log.sendMessage(log_prefix + "Stopping all (" + (generators.size + query_servers.size + request_servers.size) + ") processes", ConsoleColors.GREEN)
        stopGenerators()
        stopQueryServers()
        stopRequestServers()
        Log.sendMessage(log_prefix + "Done", ConsoleColors.GREEN)
    }
    // ------------------------- Util
    /**
     *
     * @param thread_count
     * @param type
     *
     * @throws InvalidThreadCountException
     */
    @Synchronized
    @Throws(InvalidThreadCountException::class)
    private fun checkThreadCount(thread_count: Int, type: String) {
        Log.sendLogMessage(log_prefix + "Building " + thread_count + " " + type + "s", ConsoleColors.GREEN)
        if (thread_count <= 0) {
            throw InvalidThreadCountException(thread_count)
        }
    }

    /**
     *
     */
    @Synchronized
    fun printThreadStatus() {
        //
        var output = ""

        //
        val running: String = ConsoleColors.GREEN.toString() + "Running" + ConsoleColors.CLEAR
        val not_running: String = ConsoleColors.RED.toString() + "Not Running" + ConsoleColors.CLEAR

        //
        output += "Generators:\n"
        for (i in generators.indices) {
            output += """
                Generator $i; ${if (generators[i].generatorReady()) running else not_running}
                
                """.trimIndent()
        }

        //
        output += "Request Servers:\n"
        for (i in request_servers.indices) {
            output += """
                Request Servers $i; ${if (request_servers[i].serverReady()) running else not_running}
                
                """.trimIndent()
        }

        //
        output += "Query Servers:\n"
        for (i in query_servers.indices) {
            output += """
                Query Servers $i; ${if (query_servers[i].serverReady()) running else not_running}
                
                """.trimIndent()
        }

        //
        Log.sendLogMessage(output)
    }
    // ------------------------- Query Generators
    /**
     *
     * @param count
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
            generators[i] = QueryGenerator(QueryGenerator.std_log_prefix.toString() + " " + i + ": ")
        }
    }

    /**
     *
     */
    @Synchronized
    fun startGenerators() {
        for (i in generators.indices) {
            startGenerator(i)
        }
    }

    /**
     *
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
            Log.sendMessage(log_prefix + "Invalid Query Generator ID", ConsoleColors.RED)
            return
        }
        Log.sendMessage(log_prefix + "Starting Query Generator " + id, ConsoleColors.GREEN)
        generators[id].start()
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
            Log.sendMessage(log_prefix + "Invalid Query Generator ID", ConsoleColors.RED)
            return
        }
        Log.sendMessage(log_prefix + "Stopping Query Generator " + id, ConsoleColors.GREEN)
        generators[id].stopGenerator()
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
            request_servers[i] = RequestServer(RequestServer.std_log_prefix.toString() + " " + i + ": ")
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
            Log.sendMessage(log_prefix + "Invalid Request Server ID", ConsoleColors.RED)
            return
        }
        Log.sendMessage(log_prefix + "Starting Request Server " + id, ConsoleColors.GREEN)
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
            Log.sendMessage(log_prefix + "Invalid Request Server ID", ConsoleColors.RED)
            return
        }
        Log.sendMessage(log_prefix + "Stopping Request Server " + id, ConsoleColors.GREEN)
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
            query_servers[i] = QueryServer(QueryServer.std_log_prefix.toString() + " " + i + ": ")
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
            Log.sendMessage(log_prefix + "Invalid Query Server ID", ConsoleColors.RED)
            return
        }
        Log.sendMessage(log_prefix + "Starting Query Server " + id, ConsoleColors.GREEN)
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
            Log.sendMessage(log_prefix + "Invalid Query Server ID", ConsoleColors.RED)
            return
        }
        Log.sendMessage(log_prefix + "Stopping Query Server " + id, ConsoleColors.GREEN)
        query_servers[id].stopServer()
    }
}