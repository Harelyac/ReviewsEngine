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
    TreeMap<String, Integer> index_table;
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
        index_table = new TreeMap<>();
    }


    public void decodeTokens()
    {
        String prefix, raw_prefix;
        String [] suffixes;
        String block;
        int curr_index = 0;
        int start_block;
        int end_block;

        for (int i=0; i< this.table.size(); i+=4) {
            block = this.lexStr.substring(this.table.get(i).get("term_location"), this.table.get(i+4).get("term_location"));
            prefix = block.split("\\*")[0];
            suffixes = block.split("\\*")[1].split("[^A-Za-z]{1,2}");

            System.out.println(Arrays.toString(suffixes));
            System.out.println(this.table.size());
            this.index_table.put(prefix+suffixes[0], i);
            this.index_table.put(prefix+suffixes[1], i+1);
            this.index_table.put(prefix+suffixes[2], i+2);
            this.index_table.put(prefix+suffixes[3], i+3);

            //FIXME DEAL WITH REMAINDER
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
        decodeTokens();
        return this.table.get(this.index_table.get(token)).get("total_reviews");
    }
    /**
     * Return the number of times that a given token (i.e., word) appears in
     * the reviews indexed
     * Returns 0 if there are no reviews containing this token
     */
    public int getTokenCollectionFrequency(String token) {
        readLexicon(WORDS_STRING_FILENAME, WORDS_TABLE_FILENAME);
        decodeTokens();
        return this.table.get(this.index_table.get(token)).get("total_freq");
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
         int from, size;
         from = this.table.get(this.index_table.get(token)).get("pl_location");
         size = this.table.get(this.index_table.get(token)+1).get("total_reviews");

         try
         {
             RandomAccessFile file = new RandomAccessFile("PostingListsOfWords.txt", "rw");
             file.seek(from);
             byte [] reviewIds = new byte[size];
             file.read(reviewIds);
             byte [] reviewFreqs = new byte[size];
             file.read(reviewFreqs);
         }

         catch (IOException e1)
         {
             e1.printStackTrace();
         }

         return new Enumeration<>() {

             @Override
             public boolean hasMoreElements() {

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
        readLexicon(PRODUCTS_STRING_FILENAME, PRODUCTS_TABLE_FILENAME);
        //binarySearch();

        return this.table.get(0).get("total_reviews");
    }
    /**
     * Return the number of number of tokens in the system
     * (Tokens should be counted as many times as they appear)
     */
    public int getTokenSizeOfReviews() {
        //FIXME - check what this method should return
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