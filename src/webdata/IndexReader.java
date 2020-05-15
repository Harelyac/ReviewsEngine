package webdata;

import java.io.*;
import java.util.*;

public class IndexReader {
    private static final String WORDS_STRING_FILENAME = "words_lex_string.txt";
    private static final String PRODUCTS_STRING_FILENAME = "products_lex_string.txt";
    private static final String WORDS_TABLE_FILENAME = "words_lex_table.ser";
    private static final String PRODUCTS_TABLE_FILENAME = "products_lex_table.ser";

    File file;
    ReviewData curr_review;
    int curr_review_id;
    int number_of_reviews;

    List<Map<String, Integer>> table;
    String lexStr;
    PostingList curr_posting_list; // load posting lists and frequencies



    /**
     * Creates an IndexReader which will read from the given directory
     */
    public IndexReader(String dir) {
        file = new File(dir);
        curr_review = new ReviewData();
        curr_review_id = 0;
        number_of_reviews = 0;

    }

    /**
     *  this method parse over Lex string and finds that words on given range
     * @param token
     * @param right
     * @param left
     */
    public int lookForWord(String token, int right, int left){
        return 1;
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
            index = this.table.get(middle).get("term_location");
            next_index = this.table.get(middle + 4).get("term_location");

            block = this.lexStr.substring(index + 1, next_index);
            block_words = block.split("[^A-Za-z]{1,2}");
            head_word =  block_words[0] + block_words[1];

            // we are on correct block
            if (head_word.equals(token) | right - left == 4)
            {
                for (int i = 1; i < block_words.length; i++){
                    if (token.equals(block_words[0] + block_words[i]))
                    {
                        return middle;
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


    public void readPostingList(String token){
        int indexIDs, indexFreqs, indexNextIDs ,size;
        int index = binarySearch(token);

        indexIDs = this.table.get(index).get("pl_reviewsIds");
        indexFreqs = this.table.get(index).get("pl_reviewsFreqs");
        indexNextIDs = this.table.get(index + 1).get("pl_reviewsIds");

        try
        {
            RandomAccessFile file = new RandomAccessFile("PostingListsOfWords.txt", "rw");
            file.seek(indexIDs);
            byte [] reviewIds = new byte[indexFreqs - indexIDs];
            file.read(reviewIds);

            //GroupVarint.decode(reviewIds); //FIXME - check decode, it returns list of int

            file.seek(indexFreqs);
            byte [] reviewFreqs = new byte[indexNextIDs - indexFreqs];
            file.read(reviewFreqs);

            //GroupVarint.decode(reviewFreqs);
        }

        catch (IOException e1)
        {
            e1.printStackTrace();
        }

    }


    public boolean readLexicon(String lexStrFile, String lextableFile){
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(lexStrFile));
            lexStr = br.readLine();
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }

        try {
            FileInputStream fileIn = new FileInputStream(lextableFile);
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
     * gets review at random access using the given review id
     * return exit code
     */
    public boolean readReview(int reviewId){
        String data;
        try
        {
            RandomAccessFile file = new RandomAccessFile("ReviewsData.txt", "rw");

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
            curr_review_id = reviewId;
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
        if (reviewId != curr_review_id){
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
        if (reviewId != curr_review_id){
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
        if (reviewId != curr_review_id){
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
        if (reviewId != curr_review_id){
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
        if (reviewId != curr_review_id){
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
        readLexicon(WORDS_STRING_FILENAME, WORDS_TABLE_FILENAME);
        readPostingList(token);


        return 1;
    }
    /**
     * Return the number of times that a given token (i.e., word) appears in
     * the reviews indexed
     * Returns 0 if there are no reviews containing this token
     */
    public int getTokenCollectionFrequency(String token) {

        return 1;
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
         readPostingList(token);
         return new Enumeration<>() {

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
        return 1;
    }
    /**
     * Return the number of number of tokens in the system
     * (Tokens should be counted as many times as they appear)
     */
    public int getTokenSizeOfReviews() {
        return 1;
    }
    /**
     * Return the ids of the reviews for a given product identifier
     * Note that the integers returned should be sorted by id
     *
     * Returns an empty Enumeration if there are no reviews for this product
     */
    public Enumeration<Integer> getProductReviews(String productId) {
        return new Enumeration<>() {
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