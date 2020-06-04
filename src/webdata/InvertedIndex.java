package webdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class InvertedIndex {
    SortedMap<String, PostingList> index;

    public InvertedIndex() {
        this.index = new TreeMap<>();
    }

    public void updateIndex(String token, int reviewId) {
        if (!index.containsKey(token)) {
            index.put(token, new PostingList());
        }
        index.get(token).post(reviewId);
    }

    // here we write the posting list of each entry (word/productID) and saves the info about pl_reviewsIds_ptr, pl_reviewsFreqs_ptr and length on the corresponding lexicon's table
    public void write(Lexicon lex, String file_name, String dir, int chunk_number) {
        int ptr = 0;
        byte[] encoded_reveiwsIds, encoded_freqs = new byte[0]; // FIXME - maybe this will cause future problems

        try {
            RandomAccessFile file = new RandomAccessFile(dir + "//" + file_name + "_" + Integer.toString(chunk_number) + ".txt", "rw");

            // first use posting list and update information into lexicon
            for (Map.Entry<String, PostingList> entry : index.entrySet()) {

                String token = entry.getKey();
                PostingList pl = entry.getValue();
                List<Integer> docList = pl.getDocIdsList(); // get reviews id (difference form)

                Map<String, Integer> row = new HashMap<>();
                //row.put("length", token.length());
                row.put("pl_reviewsIds_ptr", ptr); // saving beginning of reviews's IDs

                encoded_reveiwsIds = GroupVarint.encode(docList); // encode the doc's id in difference form
                ptr += encoded_reveiwsIds.length;

                if (file_name.startsWith("posting_lists_of_words")) {
                    row.put("pl_reviewsFreqs_ptr", ptr); // saving beginning of reviews's frequencies
                    List<Integer> docFreqList = new ArrayList<>(pl.freqMap.values());
                    encoded_freqs = GroupVarint.encode(docFreqList);
                    ptr += encoded_freqs.length;

                    // save the total freqs in table
                    Integer totalFreq = 0;
                    for (Integer freq : pl.freqMap.values()){
                        totalFreq+= freq;
                    }
                    row.put("total_freqs", totalFreq);
                }


                // Building lexicon table
                lex.table.put(token, row);


                file.write(encoded_reveiwsIds);
                if (file_name.startsWith("posting_lists_of_words")) {
                    file.write(encoded_freqs);
                }

            }
            file.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
}