package webdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class InvertedIndex {
    SortedMap<String, PostingList> index;
    public InvertedIndex(){
        this.index = new TreeMap<>();
    }

    public void updateIndex(String token, int reviewId) {
        if (!index.containsKey(token)) {
            index.put(token, new PostingList());
        }
        index.get(token).post(reviewId);
    }

    // here we put info into table, then write the pl into file (the reviews id + their freqs)
    public void write(Lexicon lex, int num) {
        int byte_offset = 0;
        byte [] encoded_reveiwsIds, encoded_freqs;

        String [] files = {"PostingListsOfProductIDS.txt", "PostingListsOfWords.txt"};

        // first use posting list and update information into lexicon
        for (Map.Entry<String, PostingList> entry : index.entrySet()){

            Map<String, Integer> row = new HashMap<>();
            row.put("length", entry.getKey().length());
            row.put("pl_reviewsIds", byte_offset); // saving beginning of reviews's IDs


            // here we assume the term given will be ordered according to sort map in lexicon
            lex.table.put(entry.getKey(), row);

            // here we write the current pl into the file
            encoded_reveiwsIds = GroupVarint.encode(entry.getValue().list);

            byte_offset += encoded_reveiwsIds.length;
            row.put("pl_reviewsFreqs", byte_offset); // saving beginning of reviews's frequencies

            encoded_freqs = GroupVarint.encode(new ArrayList<Integer>(entry.getValue().freq.values()));

            byte_offset += encoded_freqs.length;

            try
            {
                RandomAccessFile file = new RandomAccessFile(files[num], "rw");
                file.write(encoded_reveiwsIds);
                file.write(encoded_freqs);
                file.close(); // FIXME - change later a lot of open and closing
            }
            catch (IOException e){
                e.printStackTrace();
            }


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
