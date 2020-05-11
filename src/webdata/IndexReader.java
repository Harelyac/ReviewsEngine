package webdata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Enumeration;

public class IndexReader {
    File file;
    ReviewData curr_review;
    int curr_review_id;
    int number_of_reviews;
    Lexicon lex;


    /**
     * Creates an IndexReader which will read from the given directory
     */
    public IndexReader(String dir) {
        file = new File(dir);
        curr_review = new ReviewData();
        curr_review_id = 0;
        number_of_reviews = 0;
        lex = new Lexicon(4);
        lex.read();
    }


    /**
     *
     * @param reviewId
     * gets review at random access using the given review id
     * return exit code
     */
    public boolean getReview(int reviewId){
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
            if(!getReview(reviewId)){
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
            if(!getReview(reviewId)){
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
            if(!getReview(reviewId)){
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
            if(!getReview(reviewId)){
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
            if(!getReview(reviewId)){
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
        // FIXME need to relate table to string and do binary search
        return 0;
    }
    /**
     * Return the number of times that a given token (i.e., word) appears in
     * the reviews indexed
     * Returns 0 if there are no reviews containing this token
     */
    public int getTokenCollectionFrequency(String token) {
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
        return 0;

    }
    /**
     * Return the number of number of tokens in the system
     * (Tokens should be counted as many times as they appear)
     */
    public int getTokenSizeOfReviews() {
        return 0;
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