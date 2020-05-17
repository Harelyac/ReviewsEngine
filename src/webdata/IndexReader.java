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

        init();
    }
    private void init(){
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(directory + "//" + REVIEWS_DATA, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // get number of reviews
        if (number_of_reviews == 0) {
            try {
                number_of_reviews = (int) (file.length() / 26);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * finds range where token might be
     * @param token
     * @return if token not found returns -1
     */
//    public int binarySearch(String token){
//        String prefix;
//        String temp, head_word, block;
//        String [] block_words;
//        int index, next_index;
//        int BLOCK_SIZE = 4;
//        int right = table.size()-1, left = 0;
//        System.out.println(lexStr);
//        // getting into middle index that dividable by zero, the idea is moving only on head of each block
//        int middle = ((right + left) / 2);
//        middle = middle - middle % BLOCK_SIZE;
//
//
//        while (right > left)
//        {
//            index = table.get(middle).get("term_ptr");
//            next_index = lexStr.length();
//            System.out.println(next_index);
//            if (middle <= table.size() - BLOCK_SIZE){
//                next_index = table.get(middle + 4).get("term_ptr");
//            }
//            block = lexStr.substring(index + 1, next_index);
//            System.out.println("block = "+block);
//            block_words = block.split("\\P{Alpha}+");
//
//
//            for (String w :
//                    block_words) {
//                System.out.println("block word: " + w);
//            }
//
//            if (block_words.length > 1){
//                prefix = block_words[0];
//                head_word =  prefix + block_words[1];
//                if (token.compareTo(prefix) == 0)
//                {
//                    for (int i = 0; i < block_words.length; i++) {
//                        String term = prefix + block_words[i];
//                        System.out.println("term");
//                        System.out.println(term);
//                        if (token.equals(term)){
//                            return middle;
//                        }
//                    }
//                }
//                else if (token.compareTo(prefix) > 0)
//                {
//                    System.out.println("token = " + token + ", left = " + left + ", right = " + right + ", middle = " + middle);
//                    if (left < middle) {
//                        left = middle;
//                    }
//                    else
//                        {
//                        left+=4;
//                    }
//                }
//                else if (token.compareTo(prefix) < 0)
//                {
//                    right = middle;
//                }
//
//            }
//
//            // we are not on correct block
//            middle = ((right + left) / 2) + (4 - (((right + left) / 2) % 4));
//            System.out.println("token = " + token + ", left = " + left + ", right = " + right + ", middle = " + middle);
//        }
//        return -1;
//    }

    public int binarySearch(String token)
    {
        int block_ptr, next_index;
        int BLOCK_SIZE = 4;
        int l = 0, r = table.size() - 1;
        int m = l + (r - l) / 2;
        m = m - (m % BLOCK_SIZE);
        System.out.println(lexStr);
        while (l <= r) {
            block_ptr = table.get(m).get("term_ptr");
            next_index = lexStr.length();
            String block = lexStr.substring(block_ptr + 1, next_index);
            System.out.println(block);

            String[] block_words = block.split("\\P{Alpha}+");
            String prefix = block_words[0];

            System.out.println("token = " + token + ", left = " + l + ", right = " + r + ", middle = " + m);
            int res = token.compareTo(prefix);

            // Check if x is present at mid
            if (res == 0 || prefix.equals("")) {
                int i;
                String term;
                for (i = 1; i < 5; i++) {
                    term = prefix + block_words[i];
                    if (token.equals(term)) {
                        return i + m - 1;
                    }
                }
                if (prefix.equals(""))
                {
                    res = token.compareTo(block_words[i]);
                }
                else {
                    return -1;
                }
            }
            // If x greater, ignore left half
            if (res > 0) {
                l = m + BLOCK_SIZE;
            }

                // If x is smaller, ignore right half
            else {
                r = m - BLOCK_SIZE;
            }
            int new_m = (l + (r - l) / 2);
            new_m = new_m - (new_m % BLOCK_SIZE);
            if (new_m == m)
            {
                return - 1;
            }
            else{
                 m = new_m;
            }
        }

        return -1;
    }



    public void readPostingList(String token, String filename){
        long indexIDs, indexFreqs, indexNextIDs;
        int index = binarySearch(token.toLowerCase());

        if (index != -1)
        {
            indexIDs = table.get(index).get("pl_reviewsIds_ptr");

            indexNextIDs = 0;
            if (index < table.size()){
                indexNextIDs = table.get(index + 1).get("pl_reviewsIds_ptr");
            }

            indexFreqs = 0;
            if (filename.equals(WORDS_POSTING_LISTS)){
                indexFreqs = table.get(index).get("pl_reviewsFreqs_ptr");
            }


            try
            {
                RandomAccessFile file = new RandomAccessFile(directory + "//" + filename, "rw");
                file.seek(indexIDs);
                if (indexFreqs == 0){
                    indexFreqs = file.length();
                }
                byte [] reviewIdsBytes = new byte[(int)(indexFreqs - indexIDs)];
                file.read(reviewIdsBytes);

                List<Integer> reviewIds = new ArrayList<>();
                reviewIds = GroupVarint.decode(reviewIdsBytes);
                //System.out.println(reviewIds);

                List<Integer> reviewFreqs = new ArrayList<>();
                if (filename.equals(WORDS_POSTING_LISTS)){

                    file.seek(indexFreqs);
                    if (indexNextIDs == 0){
                        indexNextIDs = file.length();
                    }
                    byte [] reviewFreqsBytes = new byte[(int)(indexNextIDs - indexFreqs)];
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
    }

    /**
     * read lexicon, reading the string of tokens + the info table for each token
     *
     * */
    public boolean readLexicon(String lexStrFile, String lextableFile){

        try
        {
            RandomAccessFile br = new RandomAccessFile(directory + "//" + lexStrFile, "r");
            lexStr = br.readLine().replaceAll("[^\\p{Graph}\r\t\n ]", "");;
            FileInputStream fileIn = new FileInputStream(directory + "//" + lextableFile);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            table = (List<Map<String, Integer>>) objectIn.readObject();
            br.close();
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

        try
        {
            RandomAccessFile file = new RandomAccessFile(directory + "//" + REVIEWS_DATA, "rw");

            // check review id range validity
            if (reviewId < 0 || reviewId > number_of_reviews)
            {
                return false;
            }

            file.seek((reviewId - 1) * 26);

            byte [] bytes = new byte[26];
            file.read(bytes);
            String data = new String(bytes);
            curr_review.initialize(data.split("\t")[0].split(","));
            last_review_id = reviewId;

            // get number of reviews
            number_of_reviews = (int) (file.length() / 26);
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
        if (table.isEmpty() | lexStr == null | last_token.equals("")){
            readLexicon(WORDS_STRING_FILENAME, WORDS_TABLE_FILENAME);
        }

        if (!token.equals(last_token)) {
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
        if (table.isEmpty() | lexStr == null | last_productId.equals("")){
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