package webdata;

import javax.swing.plaf.basic.BasicBorders;
import java.io.*;
import java.util.Hashtable;
import java.util.Map;


public class ReviewsIndex {
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
            RandomAccessFile file = new RandomAccessFile("ReviewsData.txt", "rw");
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


    // here we read encoded data from disk, decode it and put it into java objects
    public void read(){

    }
}
