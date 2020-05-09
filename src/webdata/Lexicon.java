package webdata;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Lexicon {
    private final int k;
    private final SortedMap<String, Map<String, Integer>> table = new TreeMap<>();
    private final StringBuilder lexStr = new StringBuilder();

    public Lexicon(int k) {
        this.k = k;
    }

    public void addTerm(String term) {
        if (!table.containsKey(term)) {
            Map<String, Integer> row = new HashMap<>();
            row.put("freq", 1);
            row.put("length", term.length());
            this.table.put(term, row);
        }
        else
            {
            int freq = table.get(term).get("freq") + 1;
            table.get(term).put("freq", freq);
        }
    }

    public void createLexicon(String filename) throws IOException {
        List<List<String>> blocks = convertMapToBlocks();
        String prefix, suffix , term;
        int length, gapSize;

        for (List<String> block: blocks) {
            int i = 0;
            term = block.get(i);
            length = this.table.get(term).get("length");
            prefix = getBlockCommonPrefix(block);
            suffix = getSuffix(term, prefix);

            lexStr.append(length).append(prefix).append("*").append(suffix);

            for (i = 1; i < this.k && block.size() > i; i++) {
                term = block.get(i);
                suffix = getSuffix(term, prefix);
                gapSize = term.length() - prefix.length();
                lexStr.append(gapSize).append("♢").append(suffix);
            }
        }

//        Writing file
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write(lexStr.toString());
        writer.close();
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

    private String getSuffix(String term, String prefix) {
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

    private static String getBlockCommonPrefix(List<String> block) {
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