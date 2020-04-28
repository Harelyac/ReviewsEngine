package webdata;

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

    public void write() {

    }
}
