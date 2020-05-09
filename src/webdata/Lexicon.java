package webdata;

import java.util.ArrayList;
import java.util.List;

public class Lexicon {
    String concatLex;
    int k;

    List<Integer> freq;
    List<Integer> postingPtr;
    List<Integer> length;
    List<Integer> prefixSize;
    List<Integer> termPtr;

    public Lexicon(int k) {
        this.k = k;
        this.concatLex = "";

        this.freq = new ArrayList<>();
        this.postingPtr = new ArrayList<>();
        this.length = new ArrayList<>();
        this.prefixSize = new ArrayList<>();
        this.termPtr = new ArrayList<>();
    }

    public void addRow(int freq, int postingPtr, int length, int prefixSize, int termPtr)
    {
        this.freq.add(freq);
        this.postingPtr.add(postingPtr);
        this.length.add(length);
        this.prefixSize.add(prefixSize);
        this.termPtr.add(termPtr);
    }

    public Integer getFreq(int idx)
    {
        return this.freq.get(idx);
    }

    public Integer getPostingPtr(int idx)
    {
        return this.postingPtr.get(idx);
    }

    public Integer getLength(int idx)
    {
        return this.length.get(idx);
    }

    public Integer getPrefixSize(int idx)
    {
        return this.prefixSize.get(idx);
    }

    public Integer getTermPtr(int idx)
    {
        return this.termPtr.get(idx);
    }
}
