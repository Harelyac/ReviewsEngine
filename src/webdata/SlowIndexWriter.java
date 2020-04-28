package webdata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class SlowIndexWriter {
    /**
     * Given product review data, creates an on disk index
     * inputFile is the path to the file containing the review data
     * dir is the directory in which all index files will be created
     * if the directory does not exist, it should be created
     */
    InvertedIndex productIdIndex;
    InvertedIndex wordsIndex;
    ReviewsIndex reviewsIndex;
    int reviewId;

    public SlowIndexWriter() {
        this.productIdIndex = new InvertedIndex();
        this.wordsIndex = new InvertedIndex();
        this.reviewsIndex = new ReviewsIndex();
        this.reviewId = 0;
    }

    public void slowWrite(String inputFile) {
        parseFile(inputFile);
        writeIndexFiles();
    }

    private void writeIndexFiles() {
        productIdIndex.write();
        wordsIndex.write();
        reviewsIndex.write();
    }

    private void parseFile(String inputFile) {
        File file = new File(inputFile);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            ReviewData review = new ReviewData();
            while ((line = br.readLine()) != null) {
                if (line.equals("")){
                    continue;
                }

                StringTokenizer tokenizer = new StringTokenizer(line, ":(),-'!\". ");
                String firstToken = tokenizer.nextToken();
                String token;

                switch (firstToken) {
                    case "product/productId":
                        this.reviewId += 1;
                        token = getToken(tokenizer);
                        productIdIndex.updateIndex(token, reviewId);
                        review.productId = token;
                        break;
                    case "review/score":
                        int num = Integer.parseInt(getToken(tokenizer));
                        int dec = Integer.parseInt(getToken(tokenizer));
                        review.score = num + (dec * 0.1);
                        break;
                    case "review/helpfulness":
                        token = getToken(tokenizer);
                        String[] split = token.split("/");
                        review.helpfulnessNumerator = Integer.parseInt(split[0]);
                        review.helpfulnessDenominator = Integer.parseInt(split[1]);
                        break;
                    case "review/text":
                        while (tokenizer.hasMoreTokens()) {
                            token = getToken(tokenizer);
                            wordsIndex.updateIndex(token, reviewId);
                        }
                        review.length = tokenizer.countTokens();
                        reviewsIndex.put(reviewId, review);
                        review = new ReviewData();
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getToken(StringTokenizer tokenizer) {
        return tokenizer.nextToken().toLowerCase();
    }



    /**
     * Delete all index files by removing the given directory
     */
    public void removeIndex(String dir) {
    }
}
