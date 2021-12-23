package com.jtelaa.bwbot.querygen.searches

import java.util.Random

/**
 * Random search query generator
 *
 * @since 2
 * @author Joseph
 * @author Benjamin
 *
 * @see com.jtelaa.bwbot.bwlib.Query
 *
 * @see com.jtelaa.bwbot.querygen.processes.QueryGenerator
 */

object SearchHandler {
    /** Path of lists  */ 
    // private static final String PATH =
    // "com/jtelaa/bwbot/querygen/searches/searchdata/";

    /** Path of lists  */
    private const val PATH = "~/searches/"

    /** Logging prefix  */
    private const val log_prefix = "Search Handler: "

    /** File list  */
    @Volatile
    var files: ArrayList<File>? = null

    /**
     * Loads searches fromm the git repo
     */

    @Synchronized
    fun loadRemoteSearches() {
        // Get searches
        Log.sendManSysMessage(log_prefix + "Loading searches")
        ComputerControl.sendCommand("cd ~/ && svn checkout https://github.com/DA2Botnet/QueryGenerator-Source/trunk/searches")
    
    }

    /**
     * Setup file list (run on init)
     */

    @Synchronized
    fun setupFileList() {
        files = FileUtil.getFiles(PATH)
    
    }

    /**
     * Test that prints out searches
     *
     * @param args Arguments
     */

    fun main(args: Array<String?>?) {
        while (true) {
            //System.out.println(Query.BING_URL + getRandomSearch());
            System.out.println(mangle("testme"))

        }
    }

    /**
     * Generates random searches
     *
     * @return new Query
     */

    @get:Synchronized
    val randomSearch: Query?
        get() {
            // Generate an array and pick at a random index
            val searches: Array<Query?> = getRandomSearches(100)
            return searches[Random().nextInt(searches.size - 1)]
            
        }

    /**
     * Generates an array of random searches
     *
     * @param count Size of the array
     *
     * @return Array of random searches
     */

    @Synchronized
    fun getRandomSearches(count: Int): Array<Query?> {
        val rand = Random()

        // Setup lists
        val searches: Array<Query?> = arrayOfNulls<Query>(count)
        val search_list: ArrayList<String> = pickList() // all lines of one file

        // Populate lists
        for (i in searches.indices) {
            // Get query
            var query_string: String = search_list.get(rand.nextInt(search_list.size() - 1)).toLowerCase()

            // Mangle
            if (rand.nextInt(20) === 1) {
                query_string = mangle(query_string)

            }

            // Add search
            searches[i] = Query(query_string)

        }

        // Return
        return searches

    }

    /**
     * Pick a list of popular searches
     *
     * @return List of popular searches
     */

    @Synchronized
    private fun pickList(): ArrayList<String> {
        // Get list
        val file: File =
            files.get(Random().nextInt(files.size() - 1))

        // Return list + path
        return FileUtil.listLinesFile(file)

    }

    /**
     * Mangle the search query
     *
     * @param query query to mangle
     *
     * @return mangled query
     */

    fun mangle(query: String): String {
        // Pick case
        var query = query
        val num: Int = Random().nextInt(11)
        
        when (num) {
            8 -> query = query.toUpperCase()

            1 -> {
                val word = ""
                var i = 0
                while (i < query.length()) {
                    if (i % 2 == 0) {
                        word.concat(query.substring(i, i + 1).toUpperCase())
                    } else {
                        word.concat(query.substring(i, i + 1).toLowerCase())
                    }
                    i++
                }
                query = word
            }

            2 -> {
                val up = ""
                var i = 0
                while (i < query.length()) {
                    if (i % 2 == 0) {
                        up.concat(query.substring(i, i + 1).toUpperCase())
                    } else {
                        up.concat(query.substring(i, i + 1).toLowerCase())
                    }
                    i++
                }
                query = up
            }

            3 -> query = Typo.wrongKey(query)

            4 -> query = Typo.missedChar(query)

            5 -> query = Typo.transposedChar(query)

            6 -> query = Typo.doubleChar(query)

            7 -> query = Typo.bitFlip(query)

        }

        // Return
        return query
        
    }
}