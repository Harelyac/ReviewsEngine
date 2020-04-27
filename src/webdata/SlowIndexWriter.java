package webdata;
import com.sun.jdi.IntegerType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SlowIndexWriter {
    /**
     * Given product review data, creates an on disk index
     * inputFile is the path to the file containing the review data
     * dir is the directory in which all index files will be created
     * if the directory does not exist, it should be created
     */
    public void slowWrite(String inputFile, String dir) {
        File file = new File(inputFile);

        String [] review_text_tokens;

        List<String> product_id_per_review = new ArrayList<>();
        List<Integer> help_numerator_per_review = new ArrayList<>();
        List<Integer> help_denumerator_per_review = new ArrayList<>();
        List<Double> score_per_review = new ArrayList<>();
        List<Integer> length_per_review = new ArrayList<>();



        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String row_type, row_data;


            while ((line = br.readLine()) != null) {
                if (!line.equals("")) {
                    row_type = line.substring(0, line.indexOf(":")).strip();
                    row_data = line.substring(line.indexOf(":") + 1).strip();
                }
                else {
                    row_type = "";
                    row_data = "";
                }
                switch (row_type)
                {
                    case "review/text":
                        review_text_tokens = row_data.split("[^a-zA-Z0-9]+"); // does split based on every alphanumeric delimeter
                        length_per_review.add(review_text_tokens.length);
                        break;
                    case "review/score":
                        score_per_review.add(Double.parseDouble(row_data));
                        break;
                    case "review/helpfulness":
                        help_numerator_per_review.add(Integer.parseInt(row_data.split("/")[0]));
                        help_denumerator_per_review.add(Integer.parseInt(row_data.split("/")[1]));
                        break;
                    case "product/productId":
                        product_id_per_review.add(row_data);
                        break;
                    default:
                        break;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
    /**
     * Delete all index files by removing the given directory
     */
    public void removeIndex(String dir) {}
}
