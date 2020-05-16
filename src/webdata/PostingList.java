package webdata;

import java.util.*;

public class PostingList {
    SortedMap<Integer, Integer> freqMap;

    public PostingList() {
        this.freqMap = new TreeMap<>();
    }

    public void post(Integer docId) {
        freqMap.put(docId, freqMap.getOrDefault(docId, 0) + 1);
    }

    public List<Integer> getDocIdsList(){
        List<Integer> docIdsList = new ArrayList<>();
        docIdsList.add(freqMap.firstKey());
        int last = freqMap.firstKey();
        int curr;
        List<Integer> docs = new ArrayList<>(freqMap.keySet());
        for (int i = 1; i < freqMap.size(); i++) {
            curr =  docs.get(i);
            docIdsList.add(curr - last);
            last = curr;
        }
        return docIdsList;
    }

}
