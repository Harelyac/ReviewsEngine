package webdata;

import java.io.*;
import java.util.*;

public class Lexicon {
    private final int k;
    public final SortedMap<String, Map<String, Integer>> table;
    private final StringBuilder lexStr;

    public Lexicon(int k) {
        this.k = k;
        this.table = new TreeMap<>();
        this.lexStr = new StringBuilder();
    }


    // create one big encoded string out of all the terms in table keys, then write it straight to a file
    public void write(String filename1, String filename2, String dir, int chunk_number) {

        List<List<String>> blocks = convertMapToBlocks();
        String prefix, suffix , term;
        //int length, gapSize;

        for (List<String> block: blocks) {
            int i = 0;
            term = block.get(i);


            //length = table.get(term).get("length");
            prefix = getBlockCommonPrefix(block);
            suffix = getSuffix(term, prefix);

            lexStr.append("|");

            // saving the location of the first term for each block
            int term_ptr = lexStr.length() - 1;
            table.get(term).put("term_ptr", term_ptr);
            lexStr.append(prefix).append("*").append(suffix);

            for (i = 1; i < this.k && block.size() > i; i++) {
                term = block.get(i);
                suffix = getSuffix(term, prefix);
                //gapSize = term.length() - prefix.length();
                lexStr.append("@").append(suffix);
            }
        }

        try{
            RandomAccessFile file = new RandomAccessFile(dir + "//" + filename1 + "_" + Integer.toString(chunk_number) + ".txt", "rw");
            file.setLength(0);
            file.writeChars(lexStr.toString());
            file.close();

            // writing the full table into serialized file
            List<Map<String, Integer>> rows = new ArrayList<>(this.table.values());
            FileOutputStream fos = new FileOutputStream(dir + "//" + filename2 + "_" + Integer.toString(chunk_number) + ".ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(rows);
            oos.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }


    private List<List<String>> convertMapToBlocks() {
        List<List<String>> blocks = new ArrayList<>();
        String term;
        for (Iterator<String> it = this.table.keySet().iterator(); it.hasNext(); ) {
            List<String> block = new ArrayList<>();
            for (int i = 0; i < 4 && it.hasNext(); i++) {
                term = it.next();
                block.add(term);
            }
            blocks.add(block);
        }
        return blocks;
    }

    public static String getSuffix(String term, String prefix) {
        String suffix;
        if (term.length() > prefix.length()){
            suffix = term.substring(prefix.length());
        }
        else
        {
            suffix = "";
        }
        return suffix;
    }

    public static String getBlockCommonPrefix(List<String> block) {
        String prefix = block.get(0);

        for (int i = 1; i < 4 && block.size() > i; i++) {
            prefix = commonPrefixUtil(prefix, block.get(i));
        }
        return prefix;
    }

    private static String commonPrefixUtil(String term1, String term2) {
        StringBuilder result = new StringBuilder();
        int n1 = term1.length(), n2 = term2.length();
        for (int i = 0, j = 0; i <= n1 - 1 && j <= n2 - 1; i++, j++) {
            if (term1.charAt(i) != term2.charAt(j)) {
                break;
            }
            result.append(term1.charAt(i));
        }
        return result.toString();
    }
}