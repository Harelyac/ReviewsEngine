package webdata;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.*;
import java.nio.file.Files;


public class SlowIndexWriter {
    private static final int BLOCK_SIZE = 4;
    private static final int REVIEWS_NUMBER_LIMIT = 250;

    private static final String WORDS_STRING_FILENAME = "words_lex_string";
    private static final String WORDS_TABLE_FILENAME = "words_lex_table";
    private static final String WORDS_POSTING_LISTS = "posting_lists_of_words";

    private static final String PRODUCTS_STRING_FILENAME = "products_lex_string";
    private static final String PRODUCTS_TABLE_FILENAME = "products_lex_table";
    private static final String PRODUCTS_POSTING_LISTS = "posting_lists_of_productsIds";



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
    int chunkNumber = 0;

    public SlowIndexWriter() {
        this.productIdIndex = new InvertedIndex();
        this.wordsIndex = new InvertedIndex();
        this.reviewsIndex = new ReviewsIndex();
        this.reviewId = 0;
        this.productsLex = new Lexicon(BLOCK_SIZE);
        this.wordsLex = new Lexicon(BLOCK_SIZE);
    }

    public void slowWrite(String inputFile, String dir) {
        // parse and write all types of index files in chunks according to review limit
        parseFile(inputFile, dir);

        // merge intermediate files
        mergeReviewsData(dir);

        // merge Multiple inverted index of different chunks
        mergeInvertedIndexes(dir);
    }


    private void writeIndexFiles(String dir, int chunk_number) {

        // here we create the directory if it does not exist for the first time and only
        File directory = new File(dir);
        if (!directory.exists())
        {
            boolean result = directory.mkdir();
        }

        // writing reviews data to disk
        reviewsIndex.write(dir, chunk_number);

        // writing encoded posting lists to disk (before encoding lexicons - to get data on posting lists location on disk)
        wordsIndex.write(wordsLex, WORDS_POSTING_LISTS, dir, chunk_number);
        productIdIndex.write(productsLex, PRODUCTS_POSTING_LISTS, dir, chunk_number);


        // writing encoded lexicons to disk (table without words + the big string)
        wordsLex.write(WORDS_STRING_FILENAME, WORDS_TABLE_FILENAME, dir, chunk_number);
        productsLex.write(PRODUCTS_STRING_FILENAME, PRODUCTS_TABLE_FILENAME, dir, chunk_number);
    }

    private void parseFile(String inputFile, String dir) {
        File file = new File(inputFile);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            ReviewData review = new ReviewData();
            int reviewCount = 0;

            while ((line = br.readLine()) != null || reviewCount == REVIEWS_NUMBER_LIMIT) {

                // if we reached the limit on number of reviews, then we should write posting list and
                // lexicon to disk for this specific "chunk"
                if (reviewCount == REVIEWS_NUMBER_LIMIT){
                    chunkNumber++;
                    writeIndexFiles(dir, chunkNumber);
                    // clear arrays here - change reference to new object and gc will take care of that
                    this.productIdIndex = new InvertedIndex();
                    this.wordsIndex = new InvertedIndex();
                    this.reviewsIndex = new ReviewsIndex();
                    this.wordsLex = new Lexicon(BLOCK_SIZE);
                    this.productsLex = new Lexicon(BLOCK_SIZE);
                    reviewCount = 0;
                }


                if (line.equals("")) {
                    continue;
                }

                StringTokenizer tokenizer = new StringTokenizer(line, ",.\n<>()\"'/;-=+*@#$%^&~`][!_:|\b\t?{} ");
                String firstToken = tokenizer.nextToken();
                String token;

                switch (firstToken) {
                    case "product":
                        this.reviewId += 1;
                        token = getProductIdToken(tokenizer);
                        if (token.equals("productid")) {
                            token = getProductIdToken(tokenizer);
                            productIdIndex.updateIndex(token, reviewId);
                            review.productId = token;
                        }
                        break;
                    case "review": {
                        switch (tokenizer.nextToken()) {
                            case "score":
                                token = tokenizer.nextToken();
                                if (!token.isEmpty()) {
                                    int num = Integer.parseInt(token);
                                    int dec = Integer.parseInt(tokenizer.nextToken());
                                    review.score = num + (dec * 0.1);
                                }
                                break;
                            case "helpfulness":
                                token = tokenizer.nextToken();
                                if (!token.isEmpty()) {
                                    review.helpfulnessNumerator = Integer.parseInt(token);
                                    token = tokenizer.nextToken();
                                    review.helpfulnessDenominator = Integer.parseInt(token);
                                }
                                break;
                            case "text":
                                int reviewTokenCount = 0;
                                //System.out.println(reviewCount); // used for checking 1 gb review's capacity
                                while (tokenizer.hasMoreTokens()) {
                                    token = getToken(tokenizer);
                                    if (!token.isEmpty()) {
                                        wordsIndex.updateIndex(token, reviewId);
                                        reviewsIndex.tokenCount++;
                                        reviewTokenCount++;
                                    }
                                }
                                review.length = reviewTokenCount;
                                reviewsIndex.put(reviewId, review);
                                reviewCount++; // only when we finish dealing with text field
                                review = new ReviewData();
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getToken(StringTokenizer tokenizer) {
        String token = tokenizer.nextToken().toLowerCase();
        token = token.replaceAll("[^\\w_]+", "");

        return token;
    }


    private String getProductIdToken(StringTokenizer tokenizer) {
        return tokenizer.nextToken().toLowerCase();
    }

    private void mergeReviewsData(String dir){
        File folder = new File(dir);
        File output = new File(dir + "//" + "reviews_data.txt");
        int tokenCount = 0;


        for (File file : folder.listFiles()) {
            if (file.getName().startsWith("reviews_data_")) {
                try {
                    // first getting all lines from intermediate file
                    List <String> curr_file_lines = Files.readAllLines(Paths.get(file.getPath()), StandardCharsets.ISO_8859_1);
                    Files.write(output.toPath(), curr_file_lines.subList(0, curr_file_lines.size() - 1), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                    // get directly to the last line
                    RandomAccessFile direct_access = new RandomAccessFile(file.getPath(), "rw");
                    direct_access.seek(REVIEWS_NUMBER_LIMIT * 31);
                    tokenCount += direct_access.readInt();
                    direct_access.close();

                    // delete the intermediate file
                    file.delete();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            RandomAccessFile direct_access = new RandomAccessFile(dir + "//" + "reviews_data.txt", "rw");
            direct_access.seek(direct_access.length());
            direct_access.writeInt(tokenCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mergeInvertedIndexes(String dir){
        ArrayList<List<Map<String, Integer>>> wordsTypeTables = new ArrayList<>();
        ArrayList<String> wordsTypeStrings = new ArrayList<>();


        ArrayList<List<Map<String, Integer>>> productIDsTypeTables = new ArrayList<>();
        ArrayList<String> productIDsTypeStrings = new ArrayList<>();

        ArrayList<String> numbering = new ArrayList<>();

        int fileCounter = this.chunkNumber;
        // FIXME we need to make sure that string is read entirely with readline
        try
        {

            // maybe do a while here untill we reach 7 files at the end

            fileCounter++;

            File folder = new File(dir);
            for (File file : folder.listFiles()) {
                if (file.getName().startsWith("words_lex_string_") && wordsTypeStrings.size() != 2) {
                    RandomAccessFile br = new RandomAccessFile(file, "r");
                    wordsTypeStrings.add(br.readLine().replaceAll("[^\\p{Graph}\r\t\n ]", ""));
                    numbering.add(String.valueOf(file.getName().charAt(file.getName().length() - 5))); // check if it the correct number FIXME, it just to keep track of number of files
                    br.close();
                    file.delete();
                }

                if (file.getName().startsWith("words_lex_table_") && wordsTypeTables.size() != 2) {
                    FileInputStream fileIn = new FileInputStream(file);
                    ObjectInputStream objectIn = new ObjectInputStream(fileIn);
                    wordsTypeTables.add((List<Map<String, Integer>>)objectIn.readObject());
                    objectIn.close();
                    file.delete();
                }

                if (file.getName().startsWith("products_lex_string_") && productIDsTypeStrings.size() != 2) {
                    RandomAccessFile br = new RandomAccessFile(file, "r");
                    productIDsTypeStrings.add(br.readLine().replaceAll("[^\\p{Graph}\r\t\n ]", ""));
                    br.close();
                    file.delete();
                }

                if (file.getName().startsWith("products_lex_table_") && productIDsTypeTables.size() != 2) {
                    FileInputStream fileIn = new FileInputStream(file);
                    ObjectInputStream objectIn = new ObjectInputStream(fileIn);
                    productIDsTypeTables.add((List<Map<String, Integer>>)objectIn.readObject());
                    objectIn.close();
                    file.delete();
                }
            }
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }


        // here we start the merge process //

        int size1 = wordsTypeTables.get(0).size();
        int size2 = wordsTypeTables.get(1).size();

        int remainder1 = size1 % 4;
        int remainder2 = size2 % 4;

        // indexes for tables
        int ptr1 = 0 , ptr2 = 0;

        // index for each block - start from 1 because 0 is the prefix after the slice we do from string
        int index1 = 1, index2 = 1;

        List<List<String>> outputBlocks = new ArrayList<>();;
        List<Map<String, Long>> outputTable = null;
        StringBuilder outputStr = null;

        String[] block_words1 = null;
        String[] block_words2 = null;

        String prefix1;
        String prefix2;

        // run on both tables untill we reach last block
        while (ptr1 != (size1 - remainder1) || ptr2 != (size2 - remainder2)){
            // check
            if (index1 == 5){
                ptr1 +=4;
                int startBlock1 = wordsTypeTables.get(0).get(ptr1).get("term_ptr") + 1; // we always move 1 somthing related to long string - FIXME LATER
                int startNextBlcok1 = wordsTypeTables.get(0).get(ptr1 + 4).get("term_ptr") + 1;
                String block1 = wordsTypeStrings.get(0).substring(startBlock1, startNextBlcok1);
                block_words1 = block1.split("([@]+)|([*]+)|([|]+)");
                prefix1 = block_words1[0];
                index1 = 0;
            }

            if (index2 == 5) {
                ptr2 +=4;
                int startBlock2 = wordsTypeTables.get(1).get(ptr2).get("term_ptr") + 1;
                int startNextBlcok2 = wordsTypeTables.get(1).get(ptr2 + 4).get("term_ptr") + 1;
                String block2 = wordsTypeStrings.get(1).substring(startBlock2, startNextBlcok2);
                block_words2 = block2.split("([@]+)|([*]+)|([|]+)");
                prefix2 = block_words2[0];
                index2 = 0;
            }

            List<String> outputBlock = new ArrayList<>();

            // merge until output block is full
            while (outputBlock.size() != 4)
            {
                // check relation between two ptr1current words
                int result = (prefix1 + block_words1[index1]).compareTo(prefix2 + block_words2[index2]);

                // words are equal
                if (result == 0){
                    outputBlock.add(prefix1 + block_words1[index1]);
                    Map<String, Long> row = new HashMap<>();

                    // sum freqs from both tables
                    row.put("total_freqs", (long)wordsTypeTables.get(0).get(ptr1 + index1 - 1).get("total_freqs") + wordsTypeTables.get(1).get(ptr2 + index2).get("total_freqs"));

                    // fetch reviewID and freqs from both pl files and merge them and then put on the output file and save the location on output table
                    int startReviewsID1 = wordsTypeTables.get(0).get(ptr1 + index1 - 1).get("pl_reviewsIds_ptr") + 1;
                    int startFreqs1 = wordsTypeTables.get(0).get(ptr1 + index1 - 1).get("pl_reviewsFreqs_ptr") + 1;
                    int startNextReviewsID1 = wordsTypeTables.get(0).get(ptr1 + index1).get("pl_reviewsIds_ptr");


                    int startReviewsID2 = wordsTypeTables.get(1).get(ptr2 + index2 - 1).get("pl_reviewsIds_ptr") + 1;
                    int startFreqs2 = wordsTypeTables.get(1).get(ptr2 + index2 - 1).get("pl_reviewsFreqs_ptr") + 1;
                    int startNextReviewsID2 = wordsTypeTables.get(1).get(ptr2 + index2).get("pl_reviewsIds_ptr");


                    try {
                        RandomAccessFile file1 = new RandomAccessFile("posting_lists_of_words_" + numbering.get(0), "rw");

                        // read reviewsID
                        file1.seek(startReviewsID1);
                        byte [] reviewIdsBytes1 = new byte[(int)(startFreqs1 - startReviewsID1)];
                        file1.read(reviewIdsBytes1);
                        List<Integer> reviewIds1 = new ArrayList<>();
                        reviewIds1 = GroupVarint.decode(reviewIdsBytes1);

                        // read freqs
                        file1.seek(startFreqs1);
                        byte [] reviewFreqsBytes1 = new byte[(int)(startNextReviewsID1 - startFreqs1)];
                        file1.read(reviewFreqsBytes1);
                        List<Integer> reviewFreqs1 = new ArrayList<>();
                        reviewFreqs1 = GroupVarint.decode(reviewFreqsBytes1);


                        RandomAccessFile file2 = new RandomAccessFile("posting_lists_of_words_" + numbering.get(1), "rw");

                        file1.seek(startReviewsID2);
                        byte [] reviewIdsBytes2 = new byte[(int)(startFreqs2 - startReviewsID2)];
                        file1.read(reviewIdsBytes2);
                        List<Integer> reviewIds2 = new ArrayList<>();
                        reviewIds2 = GroupVarint.decode(reviewIdsBytes1);


                        file1.seek(startFreqs2);
                        byte [] reviewFreqsBytes2 = new byte[(int)(startNextReviewsID2 - startFreqs2)];
                        file1.read(reviewFreqsBytes2);
                        List<Integer> reviewFreqs2 = new ArrayList<>();
                        reviewFreqs2 = GroupVarint.decode(reviewFreqsBytes2);

                        // add it
                        reviewIds1.addAll(reviewIds2);
                        reviewFreqs1.addAll(reviewFreqs2);

                        // write the re-encoded arrays
                        RandomAccessFile file = new RandomAccessFile("posting_lists_of_words" + "_" + Integer.toString(fileCounter) + ".txt", "rw");
                        row.put("pl_reviewsIds_ptr", file.length());
                        file.write(GroupVarint.encode(reviewIds1));
                        row.put("pl_reviewsFreqs_ptr", file.length());
                        file.write(GroupVarint.encode(reviewFreqs1));
                    }

                    catch (Exception e){
                        e.printStackTrace();
                    }


                    //outputTable
                    index1++;
                    index2++;
                }

                // we create row with new term_ptr of output str and new reviewId_ptr and new freqs_ptr
                else if (result < 0){
                    outputBlock.add(prefix1 + block_words1[index1]);
                    Map<String, Long> row = new HashMap<>();

                    row.put("total_freqs", (long)wordsTypeTables.get(0).get(ptr1 + index1 - 1).get("total_freqs"));

                    int startReviewsID1 = wordsTypeTables.get(0).get(ptr1 + index1 - 1).get("pl_reviewsIds_ptr");
                    int startFreqs1 = wordsTypeTables.get(0).get(ptr1 + index1 - 1).get("pl_reviewsFreqs_ptr");
                    long startNextReviewsID1 = wordsTypeTables.get(0).get(ptr1 + index1).get("pl_reviewsIds_ptr");

                    try
                    {
                        RandomAccessFile file1 = new RandomAccessFile("posting_lists_of_words_" + numbering.get(0), "rw");

                        file1.seek(startReviewsID1);
                        byte [] reviewIdsBytes1 = new byte[(int)(startFreqs1 - startReviewsID1)];
                        file1.read(reviewIdsBytes1);
                        List<Integer> reviewIds1 = new ArrayList<>();
                        reviewIds1 = GroupVarint.decode(reviewIdsBytes1);


                        file1.seek(startFreqs1);
                        byte [] reviewFreqsBytes1 = new byte[(int)(startNextReviewsID1 - startFreqs1)];
                        file1.read(reviewFreqsBytes1);
                        List<Integer> reviewFreqs1 = new ArrayList<>();
                        reviewFreqs1 = GroupVarint.decode(reviewFreqsBytes1);

                        // write the re-encoded arrays
                        RandomAccessFile file = new RandomAccessFile("posting_lists_of_words" + "_" + Integer.toString(fileCounter)  + ".txt", "rw");
                        row.put("pl_reviewsIds_ptr", file.length());
                        file.write(GroupVarint.encode(reviewIds1));
                        row.put("pl_reviewsFreqs_ptr", file.length());
                        file.write(GroupVarint.encode(reviewFreqs1));
                    }

                    catch (Exception e){

                    }

                    index1++;
                }

                // we create row with new term_ptr of output str and new reviewId_ptr and new freqs_ptr
                else {
                    outputBlock.add(prefix1 + block_words2[index2]);
                    Map<String, Long> row = new HashMap<>();

                    row.put("total_freqs", (long)wordsTypeTables.get(1).get(ptr2 + index2 - 1).get("total_freqs"));

                    int startReviewsID2 = wordsTypeTables.get(1).get(ptr2 + index2 - 1).get("pl_reviewsIds_ptr");
                    int startFreqs2 = wordsTypeTables.get(1).get(ptr2 + index2 - 1).get("pl_reviewsFreqs_ptr");
                    int startNextReviewsID2 = wordsTypeTables.get(1).get(ptr2 + index2).get("pl_reviewsIds_ptr");


                    try
                    {
                        RandomAccessFile file1 = new RandomAccessFile("posting_lists_of_words_" + numbering.get(1), "rw");

                        file1.seek(startReviewsID2);
                        byte [] reviewIdsBytes1 = new byte[(int)(startFreqs2 - startReviewsID2)];
                        file1.read(reviewIdsBytes1);
                        List<Integer> reviewIds1 = new ArrayList<>();
                        reviewIds1 = GroupVarint.decode(reviewIdsBytes1);


                        file1.seek(startFreqs2);
                        byte [] reviewFreqsBytes1 = new byte[(int)(startNextReviewsID2 - startFreqs2)];
                        file1.read(reviewFreqsBytes1);
                        List<Integer> reviewFreqs1 = new ArrayList<>();
                        reviewFreqs1 = GroupVarint.decode(reviewFreqsBytes1);

                        // write the re-encoded arrays
                        RandomAccessFile file = new RandomAccessFile("posting_lists_of_words" + "_" + Integer.toString(fileCounter) + ".txt", "rw");
                        row.put("pl_reviewsIds_ptr", file.length());
                        file.write(GroupVarint.encode(reviewIds1));
                        row.put("pl_reviewsFreqs_ptr", file.length());
                        file.write(GroupVarint.encode(reviewFreqs1));
                    }

                    catch (Exception e){

                    }

                    index2++;
                }
            }
            outputBlocks.add(outputBlock);
        }

        if (ptr1 == (size1 - remainder1)){
            String block1 = wordsTypeStrings.get(0).substring(ptr1, wordsTypeStrings.get(0).length());
            block_words1 = block1.split("([@]+)|([*]+)|([|]+)");
            prefix1 = block_words1[0];
            index1 = 0;
        }

        if (ptr2 == (size2 - remainder2)){
            String block1 = wordsTypeStrings.get(0).substring(ptr2, wordsTypeStrings.get(1).length());
            block_words1 = block1.split("([@]+)|([*]+)|([|]+)");
            prefix1 = block_words1[1];
            index2 = 0;
        }

        // now we need to continue the merge until we finish one string
        // now we encode the output strings -> block of block
        // and now we need to add the term ptr of each head of block on the output table
    }

    /**
     * Delete all index files by removing the given directory
     */
    public void removeIndex(String dir) {
        File folder = new File(dir);

        for (File file : folder.listFiles()) {
            if (file.getName().endsWith(".txt") | file.getName().endsWith(".ser")) {
                file.delete();
            }
        }
    }
}
