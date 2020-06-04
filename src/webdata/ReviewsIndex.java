package webdata;

import javax.swing.plaf.basic.BasicBorders;
import java.io.*;
import java.util.Hashtable;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


public class ReviewsIndex {
    private static final String REVIEWS_DATA = "reviews_data";
    public int tokenCount;

    SortedMap<Integer, ReviewData> index;

    public ReviewsIndex() {
        this.index = new TreeMap<>();
        tokenCount = 0;
    }

    public void put(int reviewId, ReviewData review) {
        index.put(reviewId, review);
    }

    // use encode to write object files to disk mimic the writeobject method
    public void write(String dir, int chunk_number) {

        try {
            RandomAccessFile file = new RandomAccessFile(dir + "//" + REVIEWS_DATA + "_" + Integer.toString(chunk_number) + ".txt", "rw");

            file.setLength(0);

            for (ReviewData rd : index.values()) {
                byte[] Bytes = (rd.toString() + "\t".repeat(30 - rd.toString().getBytes().length) + "\n").getBytes();
                file.write(Bytes);
            }

            file.writeInt(tokenCount); // write token count at the end of this file
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
