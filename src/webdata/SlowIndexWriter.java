package webdata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class SlowIndexWriter {
    private static final int BLOCK_SIZE = 4;

    private static final String WORDS_STRING_FILENAME = "words_lex_string.txt";
    private static final String WORDS_TABLE_FILENAME = "words_lex_table.ser";
    private static final String WORDS_POSTING_LISTS = "posting_lists_of_words.txt";

    private static final String PRODUCTS_STRING_FILENAME = "products_lex_string.txt";
    private static final String PRODUCTS_TABLE_FILENAME = "products_lex_table.ser";
    private static final String PRODUCTS_POSTING_LISTS = "posting_lists_of_productsIds.txt";


    /**
     * Given product review data, creates an on disk index
     * inputFile is the path to the file containing the review data
     * dir is the directory in which all index files will be created
     * if the directory does not exist, it should be created
     */

    Lexicon wordsLex, productsLex;
    InvertedIndex productIdIndex, wordsIndex;
    ReviewsIndex reviewsIndex;
    int reviewId;

    public SlowIndexWriter() {
        this.productIdIndex = new InvertedIndex();
        this.wordsIndex = new InvertedIndex();
        this.reviewsIndex = new ReviewsIndex();
        this.reviewId = 0;
        this.productsLex = new Lexicon(BLOCK_SIZE);
        this.wordsLex = new Lexicon(BLOCK_SIZE);
    }

    public void slowWrite(String inputFile, String dir)  {
        parseFile(dir + "//" + inputFile);
        writeIndexFiles(dir);
    }



    private void writeIndexFiles(String dir)  {
        // writing reviews data to disk
        reviewsIndex.write(dir);

        // writing encoded posting lists to disk (before encoding lexicons - to get data on posting lists location on disk)
        wordsIndex.write(wordsLex, WORDS_POSTING_LISTS, dir);
        productIdIndex.write(productsLex, PRODUCTS_POSTING_LISTS, dir);


        // writing encoded lexicons to disk (table without words + the big string)
        wordsLex.write(WORDS_STRING_FILENAME, WORDS_TABLE_FILENAME, dir);
        productsLex.write(PRODUCTS_STRING_FILENAME, PRODUCTS_TABLE_FILENAME, dir);
    }

    private void parseFile(String inputFile) {
        File file = new File(inputFile);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            ReviewData review = new ReviewData();
            while ((line = br.readLine()) != null) {
                if (line.equals("")) {
                    continue;
                }

                StringTokenizer tokenizer = new StringTokenizer(line, ":(),-'!\".<>+ $&@");
                String firstToken = tokenizer.nextToken();
                String token;

                switch (firstToken) {
                    case "product/productId":
                        this.reviewId += 1;
                        token = getProductIdToken(tokenizer);
                        if (!token.isEmpty()) {
                            productIdIndex.updateIndex(token, reviewId);
                            review.productId = token;
                        }
                        break;
                    case "review/score":
                        token = getToken(tokenizer);
                        if (!token.isEmpty()) {
                            int num = Integer.parseInt(token);
                            int dec = Integer.parseInt(getToken(tokenizer));
                            review.score = num + (dec * 0.1);
                        }
                        break;
                    case "review/helpfulness":
                        token = getToken(tokenizer);
                        if (!token.isEmpty()) {
                            review.helpfulnessNumerator = Integer.parseInt(String.valueOf(token.charAt(0)));
                            review.helpfulnessDenominator = Integer.parseInt(String.valueOf(token.charAt(1)));
                        }
                        break;
                    case "review/text":
                        while (tokenizer.hasMoreTokens()) {
                            token = getToken(tokenizer);
                            if (!token.isEmpty()) {
                                wordsIndex.updateIndex(token, reviewId);
                                reviewsIndex.tokenCount++;
                            }
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
        String token = tokenizer.nextToken().toLowerCase();
        token = token.replaceAll("[^a-zA-Z0-9]+", "");
        return token;
    }


    private String getProductIdToken(StringTokenizer tokenizer) {
        return tokenizer.nextToken().toLowerCase();
    }


    /**
     * Delete all index files by removing the given directory
     */
    public void removeIndex(String dir) {
        File folder = new File(dir);

        for (File file : folder.listFiles()){
            if (file.getName().endsWith(".txt") | file.getName().endsWith(".ser")){
                file.delete();
            }
        }
    }
}
