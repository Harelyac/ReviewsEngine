package webdata;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class PostingList {
    List<Integer> list;
    Map<Integer, Integer> freq;
    int lastDocId;

    public PostingList() {
        this.list = new ArrayList<>();
        this.freq = new Hashtable<>();
        this.lastDocId = 0;
    }

    public void post(Integer docId) {
        if (list.contains(docId))
        {
            Integer count = freq.get(docId);
            freq.put(docId, count + 1);
        }

        else
        {
            int gap = docId - lastDocId;
            list.add(gap);
            freq.put(docId, 1);
            lastDocId = docId;

        }
    }

}
