package webdata;

import javax.swing.plaf.basic.BasicBorders;
import java.io.*;
import java.util.Hashtable;
import java.util.Map;


public class ReviewsIndex {
    private static final String REVIEWS_DATA = "reviews_data.txt";

    Map<Integer, ReviewData> index;
    public ReviewsIndex()
    {
        this.index = new Hashtable<>();
    }
    public void put(int reviewId, ReviewData review) {
        index.put(reviewId,review);
    }

    // use encode to write object files to disk mimic the writeobject method
    public void write() {

        try
        {
            RandomAccessFile file = new RandomAccessFile(REVIEWS_DATA, "rw");
            for (ReviewData rd : index.values())
            {
                byte[] Bytes = (rd.toString() + "\t".repeat(25 - rd.toString().getBytes().length) + "\n").getBytes();
                file.write(Bytes);
            }
            file.close();
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
