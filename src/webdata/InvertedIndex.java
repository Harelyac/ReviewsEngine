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

    // here we put info into table, then write the pl into file (the reviews id + their freqs)
    public void write(Lexicon lex, int num) {
        int ptr = 0;
        byte[] encoded_reveiwsIds, encoded_freqs;

        String[] files = {"PostingListsOfProductIDS.txt", "PostingListsOfWords.txt"};

        // first use posting list and update information into lexicon
        for (Map.Entry<String, PostingList> entry : index.entrySet()) {
            String token = entry.getKey();
            PostingList pl = entry.getValue();
            List<Integer> docList = pl.getDocIdsList();
            Map<String, Integer> row = new HashMap<>();

            row.put("length", token.length());
            row.put("pl_reviewsIds_ptr", ptr); // saving beginning of reviews's IDs

            encoded_reveiwsIds = GroupVarint.encode(docList);
            ptr += encoded_reveiwsIds.length;
            row.put("pl_reviewsFreqs_ptr", ptr); // saving beginning of reviews's frequencies

            List<Integer> docFreqList = new ArrayList<>(pl.freqMap.values());
            encoded_freqs = GroupVarint.encode(docFreqList);
            ptr += encoded_freqs.length;

            // Building lexicon table
            lex.table.put(token, row);

            try {
                RandomAccessFile file = new RandomAccessFile(files[num], "rw");
                file.write(encoded_reveiwsIds);
                file.write(encoded_freqs);
                file.close(); // FIXME - change later a lot of open and closing
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

    }
}