package com.jtelaa.bwbot.querygen.searches;

import java.util.Random;
import java.io.File;
import java.util.ArrayList;

import com.jtelaa.bwbot.bwlib.Query;
import com.jtelaa.da2.lib.control.ComputerControl;
import com.jtelaa.da2.lib.files.FileUtil;
import com.jtelaa.da2.lib.log.Log;

/**
 * Random search query generator
 * 
 * @since balls haha
 * @author Joseph
 * 
 * @see com.jtelaa.bwbot.bwlib.Query
 * @see com.jtelaa.bwbot.querygen.processes.QueryGenerator
 */

public class SearchHandler {

    /** Path of lists */
    // private static final String PATH =
    // "com/jtelaa/bwbot/querygen/searches/searchdata/";
    // TODO Make internal again

    /** Path of lists */
    private static final String PATH = "~/searches/";

    /** Logging prefix */
    private static String log_prefix = "Search Handler: ";

    /** File list */
    public static volatile ArrayList<File> files;

    /**
     * Loads searches fromm the git repo
     */

    public static synchronized void loadRemoteSearches() {
        // Get searches
        Log.sendManSysMessage("Loading searches");
        ComputerControl.sendCommand("cd ~/ && svn checkout https://github.com/DA2Botnet/QueryGenerator-Source/trunk/searches");
        
    }

    /**
     * Setup file list (run on init)
     */

    public static synchronized void setupFileList() {
        files = FileUtil.getFiles(PATH);

    }

    /**
     * Test that prints out searches
     * 
     * @param args Arguments
     */

    public static void main(String[] args) {
        while (true) {
            //System.out.println(Query.BING_URL + getRandomSearch());
            System.out.println(mangle("testme"));

        }
    }

    /**
     * Generates random searches
     * 
     * @return new Query
     */

    public synchronized static Query getRandomSearch() {
        Random rand = new Random();

        // Generate an array and pick at a random index
        Query[] searches = getRandomSearches(100);
        return searches[rand.nextInt(searches.length - 1)];

    }

    /**
     * Generates an array of random searches
     * 
     * @param count Size of the array
     * 
     * @return Array of random searches
     */

    public synchronized static Query[] getRandomSearches(int count) {
        Random rand = new Random();

        // Setup lists
        Query[] searches = new Query[count];
        ArrayList<String> search_list = pickList(); // all lines of one file

        // Populate lists
        for (int i = 0; i < searches.length; i++) {
            // Get query
            String query_string = search_list.get(rand.nextInt(search_list.size() - 1)).toLowerCase();
            
            // Mangle
            if (rand.nextInt(20) == 1) { query_string = mangle(query_string); }
            
            // Add search
            searches[i] = new Query(query_string);

        }

        // Return
        return searches;

    }

    /**
     * Pick a list of popular searches
     * 
     * @return List of popular searches
     */

    private synchronized static ArrayList<String> pickList() {
        // Get list
        Random rand = new Random();
        File file = files.get(rand.nextInt(files.size()-1));
        // ArrayList<String> lines = FileUtil.listLinesInternalFile(name);
        ArrayList<String> lines = FileUtil.listLinesFile(file);

        // Return list + path
        return lines;

    }

    /**
     * Mangle the search query
     * 
     * @param query query to mangle
     * 
     * @return mangled query
     */

    public static String mangle(String query) {
        Random random = new Random();
        int num = random.nextInt(11);

        switch (num) {
            case 8:
                query = query.toUpperCase();
                break;

            case 1: // toggle case
                String word = "";

                for (int i = 0; i < query.length(); i++) {
                    if (i % 2 == 0) {
                        word.concat(query.substring(i, i + 1).toUpperCase());

                    } else {
                        word.concat(query.substring(i, i + 1).toLowerCase());

                    }
                }

                query = word;
                break;

            case 2: // random uppercase letter
                String up = "";

                for (int i = 0; i < query.length(); i++) {
                    if (i % 2 == 0) {
                        up.concat(query.substring(i, i + 1).toUpperCase());

                    } else {
                        up.concat(query.substring(i, i + 1).toLowerCase());

                    }
                }
                query = up;
                break;

            case 3:
                query = Typo.wrongKey(query);
                break;

            case 4:
                query = Typo.missedChar(query);
                break;

            case 5:
                query = Typo.transposedChar(query);
                break;

            case 6:
                query = Typo.doubleChar(query);
                break;

            case 7:
                query = Typo.bitFlip(query);
                break;

        }

        return query;

    }
}
