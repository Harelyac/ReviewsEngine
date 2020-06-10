package webdata;

import org.w3c.dom.ls.LSOutput;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class IndexReader {
    private static final String WORDS_STRING_FILENAME = "words_lex_string.txt";
    private static final String WORDS_TABLE_FILENAME = "words_lex_table.ser";
    private static final String WORDS_POSTING_LISTS = "posting_lists_of_words.txt";

    private static final String PRODUCTS_STRING_FILENAME = "products_lex_string.txt";
    private static final String PRODUCTS_TABLE_FILENAME = "products_lex_table.ser";
    private static final String PRODUCTS_POSTING_LISTS = "posting_lists_of_products.txt";

    private static final String REVIEWS_DATA = "reviews_data.txt";


    String directory;
    ReviewData curr_review;
    int last_review_id;
    int number_of_reviews;
    int tokenCount;
    int totalFreqs;

    List<Map<String, Integer>> words_lex_table;
    String words_lex_string;

    List<Map<String, Integer>> products_lex_table;
    String products_lex_string;

    List<Integer> curr_pl; // load posting lists and frequencies

    // here if we load pl of token we will update last token and reset product id to "" and the other way around
    String last_token;
    String last_productId;

    /**
     * Creates an IndexReader which will read from the given directory
     */
    public IndexReader(String dir) {
        directory = dir;
        curr_review = new ReviewData();
        last_review_id = 0;
        number_of_reviews = 0;

        words_lex_table = new ArrayList<>();
        words_lex_string = "";
        readLexicon(WORDS_STRING_FILENAME, WORDS_TABLE_FILENAME);

        products_lex_table = new ArrayList<>();
        products_lex_string = "";
        readLexicon(PRODUCTS_STRING_FILENAME, PRODUCTS_TABLE_FILENAME);

        curr_pl = new ArrayList<>();

        last_token = "";
        last_productId = "";
    }



    /**
     * finds range where token might be
     * @param token
     * @return if token not found returns -1
     */
    public int binarySearch(String lexStr, List<Map<String, Integer>> lexTable, String token) {
        int block_ptr, next_block_ptr;
        int BLOCK_SIZE = 4;
        int l = 0, r = lexTable.size() - 1;
        int m = (r + l) / 2 ;
        m = m - (m % BLOCK_SIZE);
        int result = 0;


        while (l < r) {
            block_ptr = lexTable.get(m).get("term_ptr") + 1;

            next_block_ptr = lexStr.length();

            if (m < lexTable.size() - 4) {
                next_block_ptr = lexTable.get(m + BLOCK_SIZE).get("term_ptr");
            }

            String block = lexStr.substring(block_ptr, next_block_ptr);

            String[] block_words = block.split("([@]+)|([*]+)|([|]+)");

            String prefix = block_words[0];


            // check if we are lucky and the first word of block is the exact token
            result = token.compareTo(prefix + block_words[1]);
            if (result == 0) {
                return m;
            }

            // if token is higher alphabetically
            if (result > 0) {
                String term;
                // check if the token start with the prefix of the block
                if (token.substring(0, Math.min(prefix.length(), token.length())).equals(prefix)) {
                    // check first 4 words in block
                    for (int i = 1; i < 5 && i < block_words.length; i++) {
                        term = prefix + block_words[i];
                        if (token.equals(term)) {
                            return i + m - 1;
                        }
                    }
                }

                // here we know for sure it's not in the current block
                l = m + BLOCK_SIZE;
            }

            // if token is lower alphabetically
            else {
                r = m;
            }

            m = (r + l) / 2;
            m = m - (m % BLOCK_SIZE);
        }

        return -1;
    }


    // refactor later on do it more methodology FIXME
    public boolean readPostingList(String lexStr, List<Map<String, Integer>> lexTable ,String token, String filename){
        long indexIDs, indexFreqs, indexNextIDs;
        int index = binarySearch(lexStr ,lexTable ,token.toLowerCase());

        if (index != -1)
        {

            indexIDs = lexTable.get(index).get("pl_reviewsIds_ptr");

            // check if index does not reach end of table
            indexNextIDs = 0;
            if (index < lexTable.size()- 1){
                indexNextIDs = lexTable.get(index + 1).get("pl_reviewsIds_ptr");
            }

            // check if we have freqs at all
            indexFreqs = 0;
            if (filename.equals(WORDS_POSTING_LISTS)){
                indexFreqs = lexTable.get(index).get("pl_reviewsFreqs_ptr");
                totalFreqs = lexTable.get(index).get("total_freqs");
            }


            try
            {
                RandomAccessFile file = new RandomAccessFile(directory + "//" + filename, "rw");
                file.seek(indexIDs);
                if (indexFreqs == 0){
                    indexFreqs = indexNextIDs;
                }
                byte [] reviewIdsBytes = new byte[(int)(indexFreqs - indexIDs)];
                file.read(reviewIdsBytes);

                List<Integer> reviewIds = new ArrayList<>();
                reviewIds = GroupVarint.decode(reviewIdsBytes);

                List<Integer> reviewFreqs = new ArrayList<>();
                if (filename.equals(WORDS_POSTING_LISTS)){

                    file.seek(indexFreqs);
                    if (indexNextIDs == 0){
                        indexNextIDs = file.length();
                    }
                    byte [] reviewFreqsBytes = new byte[(int)(indexNextIDs - indexFreqs)];
                    file.read(reviewFreqsBytes);

                    reviewFreqs = GroupVarint.decode(reviewFreqsBytes);
                }

                curr_pl.clear();
                int curr_sum = 0;
                // initialize posting list with values being read
                for (int i = 0; i < reviewIds.size(); i++)
                {
                    curr_sum += reviewIds.get(i); // calculate id base on diffs
                    curr_pl.add(curr_sum);
                    if (filename.equals(WORDS_POSTING_LISTS)) {
                        curr_pl.add(reviewFreqs.get(i));
                    }
                }
            }

            catch (IOException e1)
            {
                e1.printStackTrace();
            }
            return true;
        }
        return false;
    }

    /**
     * read lexicon, reading the string of tokens + the info table for each token
     *
     * */
    public boolean readLexicon(String lexStrFile, String lextableFile){
        String lexStr = "";
        List<Map<String, Integer>> lexTable = new ArrayList<>(); ;

        try
        {
            RandomAccessFile br = new RandomAccessFile(directory + "//" + lexStrFile, "r");
            lexStr = br.readLine().replaceAll("[^\\p{Graph}\r\t\n ]", "");
            FileInputStream fileIn = new FileInputStream(directory + "//" + lextableFile);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            lexTable = (List<Map<String, Integer>>) objectIn.readObject();
            objectIn.close();
            br.close();
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }

        if (lexStrFile.equals(WORDS_STRING_FILENAME)){
            this.words_lex_string = lexStr;
            this.words_lex_table = lexTable;
        }
        else{
            this.products_lex_string = lexStr;
            this.products_lex_table = lexTable;
        }
        return true;
    }

    /**
     *
     * @param reviewId
     * gets review data at random access using the given review id
     * return exit code
     */
    public boolean readReview(int reviewId){

        try
        {
            Path path = Paths.get(directory + "//" + REVIEWS_DATA);
            long lineCount = Files.lines(path, StandardCharsets.ISO_8859_1).count();
            number_of_reviews = (int)lineCount - 1;

            // check review id range validity
            if (reviewId <= 0 || reviewId > number_of_reviews)
            {
                return false;
            }

            RandomAccessFile file = new RandomAccessFile(directory + "//" + REVIEWS_DATA, "rw");

            file.seek((reviewId - 1) * 31);

            byte [] bytes = new byte[31];
            file.read(bytes);
            String data = new String(bytes);
            curr_review.initialize(data.split("\t")[0].split(","));
            last_review_id = reviewId;

        }

        catch (IOException e1)
        {
            return false;
        }

        return true;
    }

    /**
     * Returns the product identifier for the given review
     * Returns null if there is no review with the given identifier
     */
    public String getProductId(int reviewId){
        if (reviewId != last_review_id | reviewId == 0){
            if(!readReview(reviewId)){
                return null;
            }
        }
        return curr_review.productId.toUpperCase();

    };
    /**
     * Returns the score for a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewScore(int reviewId) {
        if (reviewId != last_review_id | reviewId == 0){
            if(!readReview(reviewId)){
                return -1;
            }
        }
        return (int)curr_review.score;
    }
    /**
     * Returns the numerator for the helpfulness of a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewHelpfulnessNumerator(int reviewId) {
        if (reviewId != last_review_id | reviewId == 0){
            if(!readReview(reviewId)){
                return -1;
            }
        }
        return curr_review.helpfulnessNumerator;
    }
    /**
     * Returns the denominator for the helpfulness of a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewHelpfulnessDenominator(int reviewId) {
        if (reviewId != last_review_id | reviewId == 0){
            if(!readReview(reviewId)){
                return -1;
            }
        }
        return curr_review.helpfulnessDenominator;
    }
    /**
     * Returns the number of tokens in a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewLength(int reviewId) {
        if (reviewId != last_review_id | reviewId == 0){
            if(!readReview(reviewId)){
                return -1;
            }
        }
        return curr_review.length;
    }

    /**
     * Return the number of reviews containing a given token (i.e., word)
     * Returns 0 if there are no reviews containing this token
     */
    public int getTokenFrequency(String token) {
        if (!token.equals(last_token)) {
            if (!readPostingList(words_lex_string, words_lex_table, token, WORDS_POSTING_LISTS)){
                return 0;
            }
            last_token = token;
        }
        return curr_pl.size() / 2;
    }

    /**
     * Return the number of times that a given token (i.e., word) appears in
     * the reviews indexed
     * Returns 0 if there are no reviews containing this token
     */
    public int getTokenCollectionFrequency(String token) {
        if (getTokenFrequency(token) != 0){
            return totalFreqs;
        }

        return 0;
    }

    /**
     * Return a series of integers of the form id-1, freq-1, id-2, freq-2, ... such
     * that id-n is the n-th review containing the given token and freq-n is the
     * number of times that the token appears in review id-n
     * Note that the integers should be sorted by id
     *
     * Returns an empty Enumeration if there are no reviews containing this token
     */

    public Enumeration<Integer> getReviewsWithToken(String token) {
        if (getTokenFrequency(token) != 0){
            return Collections.enumeration(curr_pl);
        }

        return new Enumeration<Integer>() {
            @Override
            public boolean hasMoreElements() {
                return false;
            }

            @Override
            public Integer nextElement() {
                return null;
            }
        };

    }

    /**
     * Return the number of product reviews available in the system
     */
    public int getNumberOfReviews() {
        readReview(1); // read first review in order to initialize the number of reviews variable
        return number_of_reviews;
    }

    /**
     * Return the number of number of tokens in the system
     * (Tokens should be counted as many times as they appear)
     */
    public int getTokenSizeOfReviews() {

        int tokenCount = 0;
        // get number of reviews
        if (number_of_reviews == 0) {
            readReview(1);
        }

        try
        {
            RandomAccessFile file = new RandomAccessFile(directory + "//" + REVIEWS_DATA, "rw");
            file.seek(number_of_reviews * 31);
            tokenCount = file.readInt();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return tokenCount;
    }

    /**
     * Return the ids of the reviews for a given product identifier
     * Note that the integers returned should be sorted by id
     *
     * Returns an empty Enumeration if there are no reviews for this product
     */
    public Enumeration<Integer> getProductReviews(String productId) {
        if (!productId.equals(last_productId)) {
            readPostingList(products_lex_string, products_lex_table, productId, PRODUCTS_POSTING_LISTS);
            last_productId = productId;
        }

        return Collections.enumeration(curr_pl);
    }
}