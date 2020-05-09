package webdata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class SlowIndexWriter {
    private static final int BLOCK_SIZE = 4;
    private static final String WORDS_LEX_FILENAME = "words_lex.txt";
    private static final String PRODUCTS_LEX_FILENAME = "products_lex.txt";
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

    public void slowWrite(String inputFile) throws IOException {
        parseFile(inputFile);
        writeIndexFiles();
    }

    private void writeIndexFiles() throws IOException {
        productIdIndex.write();
        wordsIndex.write();
        reviewsIndex.write();
        wordsLex.createLexicon(WORDS_LEX_FILENAME);
        productsLex.createLexicon(PRODUCTS_LEX_FILENAME);
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

                StringTokenizer tokenizer = new StringTokenizer(line, ":(),-'!\".<>+ $&");
                String firstToken = tokenizer.nextToken();
                String token;

                switch (firstToken) {
                    case "product/productId":
                        this.reviewId += 1;
                        token = getProductIdToken(tokenizer);
                        if (!token.isEmpty()) {
                            productIdIndex.updateIndex(token, reviewId);
                            productsLex.addTerm(token);
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
                            String[] split = token.split("/");
                            review.helpfulnessNumerator = Integer.parseInt(split[0]);
                            review.helpfulnessDenominator = Integer.parseInt(split[1]);
                        }
                        break;
                    case "review/text":
                        while (tokenizer.hasMoreTokens()) {
                            token = getToken(tokenizer);
                            if (!token.isEmpty()) {
                                wordsIndex.updateIndex(token, reviewId);
                                wordsLex.addTerm(token);
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
        token = token.replaceAll("[\\d/](<\\w>)*[�]*[\\s\t\b]*", "");
        return token;
    }

    private String getProductIdToken(StringTokenizer tokenizer) {
        return tokenizer.nextToken().toLowerCase();
    }


    /**
     * Delete all index files by removing the given directory
     */
    public void removeIndex(String dir) {
    }
}
