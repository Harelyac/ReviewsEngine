package webdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class InvertedIndex {
    Map<String, PostingList> index;
    public InvertedIndex(){
        this.index = new Hashtable<>();
    }

    public void updateIndex(String token, int reviewId) {
        if (!index.containsKey(token)) {
            index.put(token, new PostingList());
        }
        index.get(token).post(reviewId);

    }
    // FIXME - here we should save posting list byte offset for further retrieval (each posting list)
    // FIXME - + we need to use GroupVarint for decode and than writ it into disk
    public void write(Lexicon lex, int num) {
        int byte_offset = 0;
        byte [] encoded_pl;
        String [] files = {"PostingListsOfProductIDS.txt", "PostingListsOfWords.txt"};

        // first use posting list and update information into lexicon
        for (Map.Entry<String, PostingList> entry : index.entrySet()){
            Map<String, Integer> row = new HashMap<>();
            row.put("length", entry.getKey().length());
            row.put("pl_location", byte_offset);
            row.put("total_reviews", entry.getValue().list.size());
            row.put("total_freq", getTotalFreq(entry.getValue().freq.values()));

            // here we assume the term given will be ordered according to sort map in lexicon
            lex.table.put(entry.getKey(), row);

            // here we write the current pl into some file
            encoded_pl = GroupVarint.encode(entry.getValue().list);
            try {
                RandomAccessFile file = new RandomAccessFile(files[num], "rw");
                file.write(encoded_pl);
                file.close(); // FIXME - change later a lot of open and closing
            }
            catch (IOException e){
                e.printStackTrace();
            }

            // here we actually encode and than increment offset for next term to load
            byte_offset += encoded_pl.length;
        }

    }

    public int getTotalFreq(Collection<Integer> freqs){
        int total_freq = 0;
        for(int freq : freqs){
            total_freq += freq;
        }
        return total_freq;
    }
}
