package webdata;

import java.io.*;
import java.util.*;

public class IndexReader {
    private static final String WORDS_STRING_FILENAME = "words_lex_string.txt";
    private static final String WORDS_TABLE_FILENAME = "words_lex_table.ser";
    private static final String WORDS_POSTING_LISTS = "posting_lists_of_words.txt";

    // FIXME check that later
    private static final String PRODUCTS_STRING_FILENAME = "products_lex_string.txt";
    private static final String PRODUCTS_TABLE_FILENAME = "products_lex_table.ser";
    private static final String PRODUCTS_POSTING_LISTS = "posting_lists_of_productsIds.txt";

    private static final String REVIEWS_DATA = "reviews_data.txt";


    String directory;
    ReviewData curr_review;
    int last_review_id;
    int number_of_reviews;
    int tokenCount;

    List<Map<String, Integer>> table;
    String lexStr;
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

        table = new ArrayList<>();
        lexStr = "";
        curr_pl = new ArrayList<>();
        last_token = "";
        last_productId = "";
    }


    /**
     * finds range where token might be
     * @param token
     * @return if token not found returns -1
     */
    public int binarySearch(String token){
        String temp, head_word, block;
        String [] block_words;

        int right = this.table.size(), left = 0;

        // getting into middle index that dividable by zero, the idea is moving only on head of each block
        int middle = ((right + left) / 2) - (((right + left) / 2) % 4);
        int index;
        int next_index;

        while (right > left)
        {
            index = this.table.get(middle).get("term_ptr");
            next_index = this.table.get(middle + 4).get("term_ptr");

            block = this.lexStr.substring(index + 1, next_index);
            block_words = block.split("[^A-Za-z]{1,2}");
            head_word =  block_words[0] + block_words[1];

            // we are on correct block
            if (right - left == 4 | token.equals(block_words[0] + block_words[1]))
            {
                for (int i = 1; i < block_words.length; i++)
                {
                    if (token.equals(block_words[0] + block_words[i]))
                    {
                        return middle + (i - 1);
                    }
                }
            }


            // we are not on correct block
            if (token.compareTo(head_word) > 0)
            {
                left = middle;
            }
            else
            {
                right = middle;
            }

            middle = ((right + left) / 2) - (((right + left) / 2) % 4);
        }

        return -1;
    }


    public void readPostingList(String token, String filename){
        int indexIDs, indexFreqs, indexNextIDs;
        int index = binarySearch(token);

        indexIDs = this.table.get(index).get("pl_reviewsIds_ptr");
        indexNextIDs = this.table.get(index + 1).get("pl_reviewsIds_ptr");

        indexFreqs = indexNextIDs; // if we deal with productIds

        if (filename.equals(WORDS_POSTING_LISTS)){
            indexFreqs = this.table.get(index).get("pl_reviewsFreqs_ptr");
        }


        try
        {
            RandomAccessFile file = new RandomAccessFile(directory + "//" + filename, "rw");
            file.seek(indexIDs);
            byte [] reviewIdsBytes = new byte[indexFreqs - indexIDs];
            file.read(reviewIdsBytes);

            List<Integer> reviewIds = new ArrayList<>();
            reviewIds = GroupVarint.decode(reviewIdsBytes);
            //System.out.println(reviewIds);

            List<Integer> reviewFreqs = new ArrayList<>();

            if (filename.equals(WORDS_POSTING_LISTS)){
                file.seek(indexFreqs);
                byte [] reviewFreqsBytes = new byte[indexNextIDs - indexFreqs];
                file.read(reviewFreqsBytes);

                reviewFreqs = GroupVarint.decode(reviewFreqsBytes);
                //System.out.println(reviewFreqs);
            }


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

    }

    /**
     * read lexicon, reading the string of tokens + the info table for each token
     *
     * */
    public boolean readLexicon(String lexStrFile, String lextableFile){

        try
        {
            BufferedReader br = new BufferedReader(new FileReader(directory + "//" + lexStrFile));
            lexStr = br.readLine();
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }

        try {
            FileInputStream fileIn = new FileInputStream(directory + "//" + lextableFile);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            table = (List<Map<String, Integer>>) objectIn.readObject();
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
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
        String data;
        try
        {
            RandomAccessFile file = new RandomAccessFile(directory + "//" + REVIEWS_DATA, "rw");

            // get number of reviews
            if (number_of_reviews == 0) {
                number_of_reviews = (int) (file.length() / 26);
            }

            // check review id range validity
            if (reviewId < 0 || reviewId > number_of_reviews)
            {
                return false;
            }

            file.seek((reviewId - 1) * 26);
            byte [] bytes = new byte[26];
            file.read(bytes);
            data = new String(bytes);
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
        if (reviewId != last_review_id){
            if(!readReview(reviewId)){
                return null;
            }
        }
        return curr_review.productId;

    };
    /**
     * Returns the score for a given review
     * Returns -1 if there is no review with the given identifier
     */
    public int getReviewScore(int reviewId) {
        if (reviewId != last_review_id){
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
        if (reviewId != last_review_id){
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
        if (reviewId != last_review_id){
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
        if (reviewId != last_review_id){
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
        if (table.isEmpty() | lexStr == null){
            readLexicon(WORDS_STRING_FILENAME, WORDS_TABLE_FILENAME);
        }

        if (token != last_token) {
            readPostingList(token, WORDS_POSTING_LISTS);
            last_token = token;
            last_productId = "";
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
            int sum = 0;
            // sum all the reviews freqs for a given token
            for (int i = 1; i < curr_pl.size() - 2; i+=2){
                sum += curr_pl.get(i);
            }

            return sum;
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
        if (number_of_reviews == 0){
            readReview(1); // read first review in order to initialize the number of reviews variable
        }

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
            RandomAccessFile file = new RandomAccessFile(REVIEWS_DATA, "rw");
            file.seek(number_of_reviews * 26);
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
        if (table.isEmpty() | lexStr == null){
            readLexicon(PRODUCTS_STRING_FILENAME, PRODUCTS_TABLE_FILENAME);
        }

        if (productId != last_productId) {
            readPostingList(productId, PRODUCTS_POSTING_LISTS);
            last_productId = productId;
            last_token = "";
        }
        if (curr_pl.isEmpty()){
            return Collections.enumeration(curr_pl);
        }

        else
        {
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
    }
}