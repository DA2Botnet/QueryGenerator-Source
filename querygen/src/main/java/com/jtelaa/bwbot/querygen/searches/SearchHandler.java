package com.jtelaa.bwbot.querygen.searches;

import java.util.Random;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import com.jtelaa.bwbot.bwlib.Query;
import com.jtelaa.da2.lib.control.ComputerControl;
import com.jtelaa.da2.lib.files.FileUtil;
import com.jtelaa.da2.lib.log.Log;

/**
 * Random search query generator
 * 
 * @since 2
 * @author Joseph
 * @author Benjamin
 * 
 * @see com.jtelaa.bwbot.bwlib.Query
 * @see com.jtelaa.bwbot.querygen.processes.QueryGenerator
 * 
 * Utility that generates a variety of typos
 * Thanks so much https://github.com/KeywordDomains/TypoGenerator
 * 
 */

public class SearchHandler {

    /** Path of lists */
    private static volatile String PATH = "~/searches/";

    /** Logging prefix */
    private static volatile String log_prefix = "Search Handler: ";

    /** File list */
    private ArrayList<File> files;

    /**
     * Loads searches fromm the git repo
     */

    public static synchronized void loadRemoteSearches() {
        // Get searches
        Log.sendManSysMessage(log_prefix + "Loading searches");
        ComputerControl.sendCommand("cd ~/ && svn checkout https://github.com/DA2Botnet/QueryGenerator-Source/trunk/searches");
        
    }

    /**
     * Loads searches fromm the git repo using svn
     * 
     * @param git_url Folder url in a git repo
     */

    public static synchronized void loadRemoteSearches(String git_url) {
        // Get searches
        Log.sendManSysMessage(log_prefix + "Loading searches from " + git_url);
        ComputerControl.sendCommand("cd ~/ && svn checkout " + git_url);
        
    }

    /**
     * Check if searches are present in the directory
     *
     * @return if number of searches are aboce a minimum threshold (10)
     */

    public static synchronized boolean searchesPresent() { return searchesPresent(10); }

    /**
     * Check if searches are present in the directory
     *
     * @param min_search_source_files minimum count of files in the directory
     *
     * @return if number of searches are above a minimum threshold
     */

    public static synchronized boolean searchesPresent(int min_search_source_files) {
        try {
            int source_file_count = 0;

            DirectoryStream<Path> dirStream = Files.newDirectoryStream(PATH);
            
            while(dirStream.iterator().hasNext()) {
                source_file_count++;

            }

            return source_file_count >= min_search_source_files;

        } catch (Exception e) {
            Log.sendMessage(log_prefix, e, ConsoleColors.RED);
            return false;

        }
    }

    /**
     * Setup file list (run on init)
     */

    public SearchHandler setupFileList() {
        files = FileUtil.getFiles(PATH);
        return this;

    }

    /**
     * Generates random searches
     * 
     * @return new Query
     */

    public Query getRandomSearch() {
        // Generate an array and pick at a random index
        Query[] searches = getRandomSearches(100);
        return searches[new Random().nextInt(searches.length - 1)];

    }

    /**
     * Generates an array of random searches
     * 
     * @param count Size of the array
     * 
     * @return Array of random searches
     */

    public Query[] getRandomSearches(int count) {
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

    private ArrayList<String> pickList() {
        ArrayList<String> lines = new ArrayList<>();

        try {
            // Get list
            File file = files.get(new Random().nextInt(files.size()-1));

            // Lock
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel channel = raf.getChannel();
            FileLock lock = channel.lock();

            try {
                lock = channel.tryLock();
            
            } catch (OverlappingFileLockException e) {
                Log.sendMessage(log_prefix = "Retrying list pick");
                raf.close();
                return pickList();
            
            }

            lines = FileUtil.listLinesFile(file);

            // Close the file
            if (lock != null) { lock.release(); }
            raf.close();
            channel.close();

        } catch (Exception e) {
            Log.sendMessage(log_prefix + "Retrying list pick");
            return pickList();

        }

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

    private String mangle(String query) {
        // Pick case
        int num = new Random().nextInt(11);

        switch (num) {
            // All upper case
            case 8:
                query = query.toUpperCase();
                break;

            // Toggle case
            case 1: 
                query = randomToggle(query);
                break;

            // Random uppercase letter
            case 2: 
                query = upper(query);
                break;

            // Adjacent letter
            case 3:
                query = wrongKey(query);
                break;

            // Remove character
            case 4:
                query = missedChar(query);
                break;

            // Character Swap
            case 5:
                query = transposedChar(query);
                break;

            // Double characteer
            case 6:
                query = doubleChar(query);
                break;

            // Random word
            case 7:
                query = removeRandomWord(query);
                break;


        }

        // Return
        return query;

    }

    private String removeRandomWord(String query) {
        // Default regex
        char regex = query.contains('+') ? '+' : ' ';
        String new_query = "";

        // Split based on regex
        String[] split = query.split(regex);

        // Choose index to remove
        int remove_index = new Random.nextInt(split.length - 1);

        // Rebuild string
        for (int i = 0; i < split.length; i++) {
            if (i != remove_index) { new_query += split[i] + regex; }

        }

        // Return
        return split.substring(0, new_query.lastIndexOf(regex));

    }

    private String randomToggle(String query) {
        String word = "";

        for (int i = 0; i < query.length(); i++) {
            if (i % 2 == 0) {
                word.concat(query.substring(i, i + 1).toUpperCase());

            } else {
                word.concat(query.substring(i, i + 1).toLowerCase());

            }
        }

        return word;

    }

    private String upper(String query) {
        String up = "";

        for (int i = 0; i < query.length(); i++) {
            if (i % 2 == 0) {
                up.concat(query.substring(i, i + 1).toUpperCase());

            } else {
                up.concat(query.substring(i, i + 1).toLowerCase());

            }
        }

        return up;
        
    }

    /**
     * Generates a wrong key typo by matching one random letter in the query string to a letter adjacent to it on the QWERTY keyboard
     * 
     * @param query
     * 
     * @return query string with one char wrong
     */

    private String wrongKey(String query) {
        String typo;
        Random r = new Random();
        
        // Dictionary of close keys
        Dictionary<String, String> keyboard = new Hashtable<String, String>() {};
        
        // list of close keys, use \t and # to use .split() without regex matching
        String kbd = "1\t2,q#2\t1,q,w,3#3\t2,w,e,4#4\t3,e,r,5#5\t4,r,t,6#6\t5,t,y,7#7\t6,y,u,8#8\t7,u,i,9#9\t8,i,o,0#0\t9,o,p,-#-\t0,p#q\t1,2,w,a#w\tq,a,s,e,3,2#e\tw,s,d,r,4,3#r\te,d,f,t,5,4#t\tr,f,g,y,6,5#y\tt,g,h,u,7,6#u\ty,h,j,i,8,7#i\tu,j,k,o,9,8#o\ti,k,l,p,0,9#p\to,l,-,0#a\tz,s,w,q#s\ta,z,x,d,e,w#d\ts,x,c,f,r,e#f\td,c,v,g,t,r#g\tf,v,b,h,y,t#h\tg,b,n,j,u,y#j\th,n,m,k,i,u#k\tj,m,l,o,i#l\tk,p,o#z\tx,s,a#x\tz,c,d,s#c\tx,v,f,d#v\tc,b,g,f#b\tv,n,h,g#n\tb,m,j,h#m\tn,k,j";
        
        // Split
        for (String tuple : kbd.split("#")) {
            String[] pairs = tuple.split("\t");
            keyboard.put(pairs[0], pairs[1]);

        }

        String c = "";
        do {
            // Pull a random character
            int index = r.nextInt(query.length());
            c = Character.toString(query.charAt(index));

            // Get typos
            String[] typos = keyboard.get(c).split(",");

            // New character
            String c2 = typos[r.nextInt(typos.length)];

            // Form query with typo
            typo = query.substring(0, index) + c2 + query.substring(index + 1);

        } while (!c.equals("+"));

        // Return
        return typo;
        
    }

    /**
     * Generates a missed character typo - removes one character from a string
     * 
     * @param query
     * 
     * @return query string, with one character missing
     */

    private String missedChar(String query) {
        Random r = new Random();
        String typo;
        
        // Random char
        int index = r.nextInt(query.length());

        // Remove
        typo = query.substring(0, index) + query.substring(index + 1);

        // Return
        return typo;

    }

    /**
     * Generates a transposed character typo by swapping two characters
     * 
     * @param query
     * 
     * @return query string, with two letters swapped
     */

    private String transposedChar(String query) {
        String typo;
        Random r = new Random();

        // Random char
        int index = r.nextInt(query.length());

        // Swap
        typo = query.substring(0, index) + query.substring(index + 1, index + 2) + query.substring(index, index + 1) + query.substring(index + 2);

        // Return
        return typo;

    }

    /**
     * Generates a double character typo
     * 
     * @param query
     * 
     * @return query string, with one character duplicated
     */

    private String doubleChar(String query) {
        Random r = new Random();
        String typo;

        // Random char
        int index = r.nextInt(query.length());

        // Swap
        typo = query.substring(0, index) + query.substring(index - 1);

        // Return
        return typo;

    }

    /**
     * Mimics a bit flipping typo, chooses one character and replaces it with one of the possibilites of a bit flip
     * Checks regex to make sure the character is in the alphabet or set of known numbers
     * 
     * @param query
     * 
     * @deprecated Not the right type
     * 
     * @return query string, with one character replaced with a bit flipped version of itself
     */

    @Deprecated
    private String bitFlip(String query) {
        Random r = new Random();
        String typo, new_letter;

        // Regex/Mask
        String allowed_regex = "[a-zA-Z0-9_\\-\\.]";
        int[] masks = {128,64,32,16,8,4,2,1};

        // Random char
        int index = r.nextInt(query.length());
        
        // Bit flip
        do {
            new_letter = Character.toString(
                (Character.toChars
                (Integer.parseInt
                (Integer.toHexString
                (query.charAt(index))) ^ masks
                            [r.nextInt(masks.length)]
                            ))
                            [0]
                            ).toLowerCase();
                            
        } while (!new_letter.matches(allowed_regex));
        
        // Put typo together
        typo = query.substring(0, index) + String.valueOf(new_letter) + query.substring(index + 1);

        // Return
        return typo;

    }

}
