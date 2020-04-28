package webdata;

import java.util.Hashtable;
import java.util.Map;

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

    public void write() {

    }
}
