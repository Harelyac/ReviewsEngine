package webdata;

import java.io.Serializable;
import java.util.Set;

public class ReviewData implements Serializable {
    public int helpfulnessNumerator;
    public int helpfulnessDenominator;
    public double score;
    public int length; // number of terms in the review text
    String productId;

    public ReviewData() {
    }

    public void initialize(String [] raw_data)
    {
        this.helpfulnessNumerator = Integer.parseInt(raw_data[0]);
        this.helpfulnessDenominator = Integer.parseInt(raw_data[1]);
        this.score = Double.parseDouble(raw_data[2]);
        this.length = Integer.parseInt(raw_data[3]);
        this.productId = raw_data[4];

    }

    @Override
    public String toString() {
        return  helpfulnessNumerator +
                "," + helpfulnessDenominator +
                "," + score +
                "," + length +
                "," + productId;
    }
}
