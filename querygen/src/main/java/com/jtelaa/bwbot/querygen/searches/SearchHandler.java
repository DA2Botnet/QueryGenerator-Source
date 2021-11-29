package com.jtelaa.bwbot.querygen.searches;

import java.util.Random;
import java.io.File;
import java.util.ArrayList;

import com.jtelaa.bwbot.bwlib.Query;
import com.jtelaa.da2.lib.files.FileUtil;
import com.jtelaa.da2.lib.log.Log;

/**
 * Random search query generator
 * 
 * @since 2
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
            if (rand.nextInt(100) == 1) { query_string = mangle(query_string); }
            
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
        ArrayList<File> files = FileUtil.getFiles(PATH);
        File file = files.get(files.size()-1);

        // Read file
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
        Random random = new Random(42);
        int num = random.nextInt(4);

        switch (num) {
            case 0:
                query = query.substring(1);
                break;
            case 1:
                query = query.substring(0, query.length() - 2);
                break;
            case 3:
                query = query.toUpperCase();
                break;
            case 4:
                String word = "";

                for (int i = 0; i < query.length(); i++) {
                    if (i % 2 == 0) {
                        word = word + query.substring(i, i + 1).toUpperCase();
                    } else {
                        word = word + query.substring(i, i + 1).toLowerCase();
                    }
                }

                query = word;
                break;

        }

        return query;

    }

}
