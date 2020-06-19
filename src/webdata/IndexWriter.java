package webdata;
import java.io.*;
import java.util.*;


public class IndexWriter {
    private static final int BLOCK_SIZE = 4;
    private static final int REVIEWS_NUMBER_LIMIT = 100000;

    private static final String WORDS_STRING_FILENAME = "words_lex_string";
    private static final String WORDS_TABLE_FILENAME = "words_lex_table";
    private static final String WORDS_POSTING_LISTS = "posting_lists_of_words";

    private static final String PRODUCTS_STRING_FILENAME = "products_lex_string";
    private static final String PRODUCTS_TABLE_FILENAME = "products_lex_table";
    private static final String PRODUCTS_POSTING_LISTS = "posting_lists_of_products";

    private static final int POSTING_LIST_LIMIT = 100000000; // size of output posting list file in bytes


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
    int fileCounter = 0;

    public IndexWriter() {
        this.productIdIndex = new InvertedIndex();
        this.wordsIndex = new InvertedIndex();
        this.reviewsIndex = new ReviewsIndex();
        this.reviewId = 0;
        this.productsLex = new Lexicon(BLOCK_SIZE);
        this.wordsLex = new Lexicon(BLOCK_SIZE);
    }

    public void write(String inputFile, String dir) {
        // here we create the directory if it does not exist for the first time and only
        File directory = new File(dir);
        if (!directory.exists())
        {
            boolean result = directory.mkdir();
        }

        // parse and write all types of index files in chunks according to review limit
        parseFile(inputFile, dir);
        if (this.chunkNumber > 1){
            // merge reviews index file
            mergeReviewsData(dir);

            // merge the product ids index files
            fileCounter = this.chunkNumber;
            for (int i = 1; i < chunkNumber; i++){
                mergeInvertedIndexes(dir, "products_");
            }

            // merge the words index files
            fileCounter = this.chunkNumber;

            for (int i = 1; i < chunkNumber; i++){
                mergeInvertedIndexes(dir, "words_");
            }
        }


        // change names of files to the original form
        File folder = new File(dir);
        for (File file : folder.listFiles()) {
            if (!file.getName().equals("reviews_data.txt")){ //fixme - check the ending later
                File newfile = new File(dir + "//" + file.getName().substring(0,file.getName().lastIndexOf("_")) + file.getName().substring(file.getName().lastIndexOf(".")));
                file.renameTo(newfile);
            }
        }
    }


    private void writeIndexFiles(String dir, int chunk_number) {
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

            while ((line = br.readLine()) != null) {
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
                        token = getProductIdToken(tokenizer);
                        if (token.equals("productid")) {
                            this.reviewId += 1;
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
            if (0 < reviewCount && reviewCount <= REVIEWS_NUMBER_LIMIT){
                chunkNumber++;
                writeIndexFiles(dir, chunkNumber);
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
        try {
            RandomAccessFile output = new RandomAccessFile(dir + "//" + "reviews_data.txt", "rw");

            int tokenCount = 0;

            for (File file : folder.listFiles()) {
                if (file.getName().startsWith("reviews_data_")) {
                    try {
                        // first getting all lines from intermediate file
                        //List <String> curr_file_lines = Files.readAllLines(Paths.get(file.getPath()), StandardCharsets.ISO_8859_1);
                        //Files.write(output.toPath(), curr_file_lines.subList(0, curr_file_lines.size() - 1), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                        // get directly to the last line
                        RandomAccessFile direct_access = new RandomAccessFile(file.getPath(), "rw");

                        int line_counter = 0;
                        while (line_counter < direct_access.length() / 31) {
                            byte [] line = new byte[31];
                            direct_access.read(line);
                            output.write(line);
                            line_counter++;
                        }

                        direct_access.seek((direct_access.length() / 31) * 31);
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
                output.seek(output.length());
                output.writeInt(tokenCount);
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    private void mergeInvertedIndexes(String dir, String filename){
        ArrayList<List<Map<String, Integer>>> tokensTypeTables = new ArrayList<>();
        ArrayList<String> wordsTypeStrings = new ArrayList<>();
        ArrayList<String> numbering = new ArrayList<>();

        try
        {
            fileCounter++;

            File folder = new File(dir);
            for (File file : folder.listFiles()) {
                if (file.getName().startsWith(filename + "lex_string_") && wordsTypeStrings.size() != 2) {
                    RandomAccessFile br = new RandomAccessFile(file, "r");
                    wordsTypeStrings.add(br.readLine().replaceAll("[^\\p{Graph}\r\t\n ]", ""));
                    numbering.add(String.valueOf(file.getName().substring(file.getName().lastIndexOf("_") + 1, file.getName().indexOf(".")))); // keep track on the input files numbers
                    br.close();
                    file.delete();
                }

                if (file.getName().startsWith(filename + "lex_table_") && tokensTypeTables.size() != 2) {
                    FileInputStream fileIn = new FileInputStream(file);
                    ObjectInputStream objectIn = new ObjectInputStream(fileIn);
                    tokensTypeTables.add((List<Map<String, Integer>>)objectIn.readObject());
                    objectIn.close();
                    file.delete();
                }
            }
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }


        // Some initialization before merge process

        int size1 = tokensTypeTables.get(0).size();
        int size2 = tokensTypeTables.get(1).size();

        int remainder1 = size1 % 4;
        int remainder2 = size2 % 4;

        // indexes for tables
        int ptr1 = 0 , ptr2 = 0;

        // index for each block - start from 1 because 0 is the prefix after the slice we do from string
        int index1 = 1, index2 = 1;

        List<List<String>> outputBlocks = new ArrayList<>();;
        List<Map<String, Integer>> outputTable = new ArrayList<>();
        StringBuilder outputStr = new StringBuilder();

        String[] block_words1 = null;
        String[] block_words2 = null;

        String prefix1 = "";
        String prefix2 = "";

        int block_size1 = 4;
        int block_size2 = 4;

        int startNextBlcok1 = 0;
        int startNextBlcok2 = 0;

        int pl_ptr = 0;
        int pl_size = 0;

        // load block from string 1

        int startBlock1 = tokensTypeTables.get(0).get(ptr1).get("term_ptr") + 1;

        if (ptr1 + block_size1 >= size1)
        {
            if (remainder1 == 0){
                remainder1 = 4;
            }
            block_size1 = remainder1;
            startNextBlcok1 =  wordsTypeStrings.get(0).length();
        }
        else
        {
            startNextBlcok1 = tokensTypeTables.get(0).get(ptr1 + block_size1).get("term_ptr") + 1;
        }


        String block1 = wordsTypeStrings.get(0).substring(startBlock1, startNextBlcok1);
        block_words1 = block1.split("([@]+)|([*]+)|([|]+)");
        prefix1 = block_words1[0];


        // load block from string 2

        int startBlock2 = tokensTypeTables.get(1).get(ptr2).get("term_ptr") + 1;

        if (ptr2 + block_size2 >= size2)
        {
            if (remainder2 == 0){
                remainder2 = 4;
            }
            block_size2 = remainder2;
            startNextBlcok2 =  wordsTypeStrings.get(1).length();
        }
        else
        {
            startNextBlcok2 = tokensTypeTables.get(1).get(ptr2 + block_size2).get("term_ptr") + 1;
        }


        String block2 = wordsTypeStrings.get(1).substring(startBlock2, startNextBlcok2);
        block_words2 = block2.split("([@]+)|([*]+)|([|]+)");
        prefix2 = block_words2[0];


        // init new output block
        List<String> outputBlock = new ArrayList<>();

        // list of posting list data - reviews ids + reviews freqs (in word case)
        // used mainly to deal with less transfer times
        //List<byte []> outputPostingList = new ArrayList<>();


        try {
            FileOutputStream fout = new FileOutputStream(dir + "//" + "posting_lists_of_" + filename + Integer.toString(fileCounter) + ".txt");
            BufferedOutputStream bout = new BufferedOutputStream(fout);

        // run on both tables until we reach head of last block
        while (ptr1 != size1 || ptr2 != size2){ // fixme
                if (pl_size > POSTING_LIST_LIMIT) { // we need to check the avg size of each array of bytes
                    //RandomAccessFile file = new RandomAccessFile(dir + "//" + "posting_lists_of_" + filename + Integer.toString(fileCounter) + ".txt", "rw");
                    //file.seek(file.length()); // append mode :)
                    //for (byte [] encoded_data : outputPostingList){
                    //    file.write(encoded_data);
                    //}
                    //outputPostingList = new ArrayList<>();
                    pl_size = 0;
                        //file.close();
                    bout.flush();

                }


            // check if we reached end of block 1
            if (index1 == block_size1 + 1){ // the +1 is just because on block we have prefix and suffixes to extra for prefix
                ptr1 += block_size1;
                index1 = 1;
                if (ptr1 >= size1){ // fixme
                    break;
                }

                startBlock1 = tokensTypeTables.get(0).get(ptr1).get("term_ptr") + 1;
                // check if we are going to be on the last block
                // if last block is also 4 words size
                if (remainder1 == 0){
                    remainder1 = 4;
                }
                if (ptr1 == size1 - remainder1){
                    startNextBlcok1 = wordsTypeStrings.get(0).length();
                    block_size1 = remainder1;
                }
                else
                {
                    startNextBlcok1 = tokensTypeTables.get(0).get(ptr1 + block_size1).get("term_ptr") + 1;
                }

                block1 = wordsTypeStrings.get(0).substring(startBlock1, startNextBlcok1);
                block_words1 = block1.split("([@]+)|([*]+)|([|]+)");
                prefix1 = block_words1[0];

            }

            if (index2 == block_size2 + 1) {
                ptr2 +=block_size2;
                index2 = 1;
                if (ptr2 >= size2){
                    break;
                }
                startBlock2 = tokensTypeTables.get(1).get(ptr2).get("term_ptr") + 1;
                if (remainder2 == 0){
                    remainder2 = 4;
                }
                if (ptr2 == size2 - remainder2)
                {
                    startNextBlcok2 = wordsTypeStrings.get(1).length();
                    block_size2 = remainder2;
                }
                else
                {
                    startNextBlcok2 = tokensTypeTables.get(1).get(ptr2 + block_size2).get("term_ptr") + 1;
                }

                block2 = wordsTypeStrings.get(1).substring(startBlock2, startNextBlcok2);
                block_words2 = block2.split("([@]+)|([*]+)|([|]+)");
                prefix2 = block_words2[0];
            }


            // merge until output block is full
            // run until we have output block fully loaded and ready to be written or
            // we reach end of one of the input blocks
            while (outputBlock.size() != 4 && index1 != block_size1 + 1 && index2 != block_size2 + 1)
            {
                int result;
                String suffix1 = "", suffix2 = "";

                // if we got no suffixes - fixme - i did a plaster here - maybe fix later
                if (block_words1.length > 1 && block_words1.length -1 == block_size1){

                    suffix1 = block_words1[index1];
                }

                if (block_words2.length > 1 && block_words2.length -1 == block_size2){
                    suffix2 = block_words2[index2];
                }

                result = (prefix1 + suffix1).compareTo(prefix2 + suffix2);

                // words are equal
                if (result == 0)
                {
                    outputBlock.add(prefix1 + suffix1);
                    Map<String, Integer> row = new HashMap<>();


                    // read posting list's pointers
                    int startReviewsID1 = tokensTypeTables.get(0).get(ptr1 + index1 - 1).get("pl_reviewsIds_ptr");

                    int startNextReviewsID1;
                    if (ptr1 + index1 == size1){
                        startNextReviewsID1 = 0;
                    }
                    else
                    {
                        startNextReviewsID1 = tokensTypeTables.get(0).get(ptr1 + index1).get("pl_reviewsIds_ptr");
                    }

                    int startFreqs1 = 0;

                    if (filename.equals("words_")){
                        // sum freqs from both tables
                        row.put("total_freqs", tokensTypeTables.get(0).get(ptr1 + index1 - 1).get("total_freqs") + tokensTypeTables.get(1).get(ptr2 + index2 - 1).get("total_freqs"));
                        startFreqs1 = tokensTypeTables.get(0).get(ptr1 + index1 - 1).get("pl_reviewsFreqs_ptr");
                    }



                    int startReviewsID2 = tokensTypeTables.get(1).get(ptr2 + index2 - 1).get("pl_reviewsIds_ptr");

                    int startNextReviewsID2; // alert - changed fron long to int
                    if (ptr2 + index2 == size2){
                        startNextReviewsID2 = 0;
                    }
                    else{
                        startNextReviewsID2 = tokensTypeTables.get(1).get(ptr2 + index2).get("pl_reviewsIds_ptr");
                    }

                    int startFreqs2 = 0;

                    if (filename.equals("words_")){
                        startFreqs2 = tokensTypeTables.get(1).get(ptr2 + index2 - 1).get("pl_reviewsFreqs_ptr");
                    }

                    try {
                        RandomAccessFile file1 = new RandomAccessFile(dir + "//" +  "posting_lists_of_" + filename + numbering.get(0) + ".txt", "rw");
                        // read reviewsID
                        file1.seek(startReviewsID1);

                        if (startNextReviewsID1 == 0){
                            startNextReviewsID1 = (int)file1.length();
                        }

                        // if we are on products
                        if (startFreqs1 == 0){
                            startFreqs1 = startNextReviewsID1;
                        }


                        byte [] reviewIdsBytes1 = new byte[(int)(startFreqs1 - startReviewsID1)];
                        file1.read(reviewIdsBytes1);
                        List<Integer> reviewIds1 = new ArrayList<>();
                        reviewIds1 = GroupVarint.decode(reviewIdsBytes1);

                        List<Integer> reviewFreqs1 = null;

                        // read freqs
                        if (filename.equals("words_")){
                            file1.seek(startFreqs1);
                            byte [] reviewFreqsBytes1 = new byte[(int)(startNextReviewsID1 - startFreqs1)];
                            file1.read(reviewFreqsBytes1);
                            reviewFreqs1 = new ArrayList<>();
                            reviewFreqs1 = GroupVarint.decode(reviewFreqsBytes1);
                        }

                        file1.close();

                        RandomAccessFile file2 = new RandomAccessFile(dir +  "//" + "posting_lists_of_" + filename + numbering.get(1) + ".txt", "rw");
                        file2.seek(startReviewsID2);

                        if (startNextReviewsID2 == 0){
                            startNextReviewsID2 = (int)file2.length();
                        }

                        // if we are on products
                        if (startFreqs2 == 0){
                            startFreqs2 = startNextReviewsID2;
                        }

                        byte [] reviewIdsBytes2 = new byte[(int)(startFreqs2 - startReviewsID2)];
                        file2.read(reviewIdsBytes2);
                        List<Integer> reviewIds2 = new ArrayList<>();
                        reviewIds2 = GroupVarint.decode(reviewIdsBytes2);

                        List<Integer> reviewFreqs2 = null;

                        if (filename.equals("words_")){
                            file2.seek(startFreqs2);
                            byte [] reviewFreqsBytes2 = new byte[(int)(startNextReviewsID2 - startFreqs2)];
                            file2.read(reviewFreqsBytes2);
                            reviewFreqs2 = new ArrayList<>();
                            reviewFreqs2 = GroupVarint.decode(reviewFreqsBytes2);
                            reviewFreqs1.addAll(reviewFreqs2);
                        }

                        file2.close();

                        // add them all
                        int sum = 0;
                        // first sum all reviewsID differences of 1
                        for (int i = 0; i < reviewIds1.size(); i ++){
                            sum += reviewIds1.get(i);
                        }
                        reviewIds2.set(0, reviewIds2.get(0) - sum);
                        // add it
                        reviewIds1.addAll(reviewIds2);



                        // write the re-encoded arrays
                        //RandomAccessFile file = new RandomAccessFile(dir + "//" + "posting_lists_of_" + filename + Integer.toString(fileCounter)  + ".txt", "rw");
                        //file.seek(file.length()); // append mode :)

                        byte[] encoded_reveiwsIds, encoded_freqs = new byte[0];
                        encoded_reveiwsIds = GroupVarint.encode(reviewIds1);

                        row.put("pl_reviewsIds_ptr", pl_ptr);
                        //file.write(encoded_reveiwsIds);
                        //outputPostingList.add(encoded_reveiwsIds);
                        bout.write(encoded_reveiwsIds);
                        pl_ptr += encoded_reveiwsIds.length;
                        pl_size += encoded_reveiwsIds.length;

                        if (filename.equals("words_")){
                            encoded_freqs = GroupVarint.encode(reviewFreqs1);
                            row.put("pl_reviewsFreqs_ptr", pl_ptr);
                            //file.write(encoded_freqs);
                            //outputPostingList.add(encoded_freqs);
                            bout.write(encoded_freqs);
                            pl_ptr += encoded_freqs.length;
                            pl_size += encoded_freqs.length;
                        }
                        //file.close();
                    }

                    catch (Exception e){
                        e.printStackTrace();
                    }

                    outputTable.add(row);
                    index1++;
                    index2++;
                }

                // we create row with new term_ptr of output str and new reviewId_ptr and new freqs_ptr
                else if (result < 0){
                    outputBlock.add(prefix1 + suffix1);
                    Map<String, Integer> row = new HashMap<>();

                    // read posting list's pointers
                    int startReviewsID1 = tokensTypeTables.get(0).get(ptr1 + index1 - 1).get("pl_reviewsIds_ptr");

                    int startNextReviewsID1;
                    if (ptr1 + index1 == size1){
                        startNextReviewsID1 = 0;
                    }
                    else{
                        startNextReviewsID1 = tokensTypeTables.get(0).get(ptr1 + index1).get("pl_reviewsIds_ptr");
                    }

                    int startFreqs1 = 0;

                    if (filename.equals("words_")){
                        // sum freqs from both tables
                        row.put("total_freqs", tokensTypeTables.get(0).get(ptr1 + index1 - 1).get("total_freqs"));
                        startFreqs1 = tokensTypeTables.get(0).get(ptr1 + index1 - 1).get("pl_reviewsFreqs_ptr");
                    }

                    try
                    {
                        RandomAccessFile file1 = new RandomAccessFile(dir + "//" +  "posting_lists_of_" + filename + numbering.get(0) + ".txt", "rw");
                        // read reviewsID
                        file1.seek(startReviewsID1);

                        if (startNextReviewsID1 == 0){
                            startNextReviewsID1 = (int)file1.length();
                        }

                        // if we are on products
                        if (startFreqs1 == 0){
                            startFreqs1 = startNextReviewsID1;
                        }

                        byte [] reviewIdsBytes1 = new byte[(int)(startFreqs1 - startReviewsID1)];
                        file1.read(reviewIdsBytes1);
                        List<Integer> reviewIds1 = new ArrayList<>();
                        reviewIds1 = GroupVarint.decode(reviewIdsBytes1);

                        List<Integer> reviewFreqs1 = null;

                        // read freqs
                        if (filename.equals("words_")){
                            file1.seek(startFreqs1);
                            byte [] reviewFreqsBytes1 = new byte[(int)(startNextReviewsID1 - startFreqs1)];
                            file1.read(reviewFreqsBytes1);
                            reviewFreqs1 = new ArrayList<>();
                            reviewFreqs1 = GroupVarint.decode(reviewFreqsBytes1);
                        }

                        file1.close();

                        // write the re-encoded arrays
                        //RandomAccessFile file = new RandomAccessFile(dir + "//" + "posting_lists_of_" + filename + Integer.toString(fileCounter)  + ".txt", "rw");
                        //file.seek(file.length()); // append mode :)

                        byte[] encoded_reveiwsIds, encoded_freqs = new byte[0];
                        encoded_reveiwsIds = GroupVarint.encode(reviewIds1);


                        row.put("pl_reviewsIds_ptr", pl_ptr);
                        //file.write(encoded_reveiwsIds);
                        //outputPostingList.add(encoded_reveiwsIds);
                        bout.write(encoded_reveiwsIds);
                        pl_ptr += encoded_reveiwsIds.length;

                        if (filename.equals("words_")){
                            encoded_freqs = GroupVarint.encode(reviewFreqs1);
                            row.put("pl_reviewsFreqs_ptr", pl_ptr);
                            //file.write(encoded_freqs);
                            //outputPostingList.add(encoded_freqs);
                            bout.write(encoded_freqs);
                            pl_ptr += encoded_freqs.length;
                        }
                        //file.close();
                    }

                    catch (Exception e){
                        e.printStackTrace();
                    }

                    outputTable.add(row);
                    index1++;
                }

                // we create row with new term_ptr of output str and new reviewId_ptr and new freqs_ptr
                else {
                    outputBlock.add(prefix2 + suffix2);
                    Map<String, Integer> row = new HashMap<>();


                    int startReviewsID2 = tokensTypeTables.get(1).get(ptr2 + index2 - 1).get("pl_reviewsIds_ptr");

                    int startNextReviewsID2; // alert - changed fron long to int
                    if (ptr2 + index2 == size2)
                    {
                        startNextReviewsID2 = 0;
                    }
                    else{
                        startNextReviewsID2 = tokensTypeTables.get(1).get(ptr2 + index2).get("pl_reviewsIds_ptr");
                    }

                    int startFreqs2 = 0;

                    if (filename.equals("words_")){
                        row.put("total_freqs", tokensTypeTables.get(1).get(ptr2 + index2 - 1).get("total_freqs"));
                        startFreqs2 = tokensTypeTables.get(1).get(ptr2 + index2 - 1).get("pl_reviewsFreqs_ptr");
                    }


                    try
                    {
                        RandomAccessFile file2 = new RandomAccessFile(dir +  "//" + "posting_lists_of_" + filename + numbering.get(1) + ".txt", "rw");
                        file2.seek(startReviewsID2);
                        if (startNextReviewsID2 == 0){
                            startNextReviewsID2 = (int)file2.length(); // alert - casted to int
                        }

                        // if we are on products
                        if (startFreqs2 == 0){
                            startFreqs2 = startNextReviewsID2;
                        }

                        byte [] reviewIdsBytes2 = new byte[(int)(startFreqs2 - startReviewsID2)];
                        file2.read(reviewIdsBytes2);
                        List<Integer> reviewIds2 = new ArrayList<>();
                        reviewIds2 = GroupVarint.decode(reviewIdsBytes2);

                        List<Integer> reviewFreqs2 = null;

                        if (filename.equals("words_")){
                            file2.seek(startFreqs2);
                            byte [] reviewFreqsBytes2 = new byte[(int)(startNextReviewsID2 - startFreqs2)];
                            file2.read(reviewFreqsBytes2);
                            reviewFreqs2 = new ArrayList<>();
                            reviewFreqs2 = GroupVarint.decode(reviewFreqsBytes2);
                        }

                        file2.close();

                        // write the re-encoded arrays
                        //RandomAccessFile file = new RandomAccessFile(dir + "//" + "posting_lists_of_" + filename + Integer.toString(fileCounter)  + ".txt", "rw");
                        //file.seek(file.length()); // append mode :)

                        byte[] encoded_reveiwsIds, encoded_freqs = new byte[0];
                        encoded_reveiwsIds = GroupVarint.encode(reviewIds2);


                        row.put("pl_reviewsIds_ptr", pl_ptr);
                        //file.write(encoded_reveiwsIds);
                        //outputPostingList.add(encoded_reveiwsIds);
                        bout.write(encoded_reveiwsIds);
                        pl_ptr += encoded_reveiwsIds.length;
                        pl_size += encoded_reveiwsIds.length;

                        if (filename.equals("words_")){
                            encoded_freqs = GroupVarint.encode(reviewFreqs2);
                            row.put("pl_reviewsFreqs_ptr", pl_ptr);
                            //file.write(encoded_freqs);
                            //outputPostingList.add(encoded_freqs);
                            bout.write(encoded_freqs);
                            pl_ptr += encoded_freqs.length;
                            pl_size += encoded_freqs.length;
                        }
                        //file.close();
                    }

                    catch (Exception e){

                    }
                    outputTable.add(row);
                    index2++;
                }
            }
            if (outputBlock.size() == 4){
                outputBlocks.add(outputBlock);
                outputBlock = new ArrayList<>();
            }
        }



        // if we end table 1
        if (ptr1 >= size1){
            // and we still got words from table 2 to work with
            if (ptr2 + index2 - 1 < size2 - 1) {
                while (ptr2 + index2 - 1 != size2 - 1)
                {
                    if (pl_size > POSTING_LIST_LIMIT) { // we need to check the avg size of each array of bytes
                        /*try {
                            RandomAccessFile file = new RandomAccessFile(dir + "//" + "posting_lists_of_" + filename + Integer.toString(fileCounter) + ".txt", "rw");
                            file.seek(file.length()); // append mode :)
                            for (byte [] encoded_data : outputPostingList){
                                file.write(encoded_data);
                            }
                            outputPostingList = new ArrayList<>();
                            pl_size = 0;
                            file.close();
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }*/
                        pl_size = 0;
                        bout.flush();
                    }

                    // check if we need to load another block
                    if (index2 == block_size2 + 1)
                    {
                        // if we are on last block
                        if (ptr2 + block_size2 == size2 - remainder2)
                        {
                            ptr2 += block_size2;
                            startNextBlcok2 = wordsTypeStrings.get(1).length();
                            block_size2 = remainder2;
                            index2 = 1;
                        }
                        else{
                            ptr2 += block_size2;
                            index2 = 1;
                            startNextBlcok2 = tokensTypeTables.get(1).get(ptr2 + block_size2).get("term_ptr") + 1;
                        }

                        startBlock2 = tokensTypeTables.get(1).get(ptr2).get("term_ptr") + 1;
                        block2 = wordsTypeStrings.get(1).substring(startBlock2, startNextBlcok2);
                        block_words2 = block2.split("([@]+)|([*]+)|([|]+)");
                        prefix2 = block_words1[0];
                    }
                    else
                    {
                        String suffix2 = "";
                        if (block_words2.length > 1 && block_words2.length -1 == block_size2){
                            suffix2 = block_words2[index2];
                        }

                        outputBlock.add(prefix2 + suffix2);
                        Map<String, Integer> row = new HashMap<>();


                        int startReviewsID2 = tokensTypeTables.get(1).get(ptr2 + index2 - 1).get("pl_reviewsIds_ptr");

                        int startNextReviewsID2; // alert - changed fron long to int
                        if (ptr2 + index2 == size2){
                            startNextReviewsID2 = 0;
                        }
                        else{
                            startNextReviewsID2 = tokensTypeTables.get(1).get(ptr2 + index2).get("pl_reviewsIds_ptr");
                        }

                        int startFreqs2 = 0;

                        if (filename.equals("words_")) {
                            // sum freqs from both tables
                            row.put("total_freqs", tokensTypeTables.get(1).get(ptr2 + index2 - 1).get("total_freqs"));
                            startFreqs2 = tokensTypeTables.get(1).get(ptr2 + index2 - 1).get("pl_reviewsFreqs_ptr");
                        }

                        try {
                            RandomAccessFile file2 = new RandomAccessFile(dir + "//" + "posting_lists_of_" + filename + numbering.get(1) + ".txt", "rw");
                            file2.seek(startReviewsID2);
                            if (startNextReviewsID2 == 0){
                                startNextReviewsID2 = (int)file2.length(); // alert - casted to int
                            }

                            // if we are on products
                            if (startFreqs2 == 0){
                                startFreqs2 = startNextReviewsID2;
                            }

                            byte [] reviewIdsBytes2 = new byte[(int)(startFreqs2 - startReviewsID2)];
                            file2.read(reviewIdsBytes2);
                            List<Integer> reviewIds2 = new ArrayList<>();
                            reviewIds2 = GroupVarint.decode(reviewIdsBytes2);


                            List<Integer> reviewFreqs2 = null;

                            if (filename.equals("words_")){
                                file2.seek(startFreqs2);
                                byte [] reviewFreqsBytes2 = new byte[(int)(startNextReviewsID2 - startFreqs2)];
                                file2.read(reviewFreqsBytes2);
                                reviewFreqs2 = new ArrayList<>();
                                reviewFreqs2 = GroupVarint.decode(reviewFreqsBytes2);
                            }

                            file2.close();

                            // write the re-encoded arrays
                            //RandomAccessFile file = new RandomAccessFile(dir + "//" + "posting_lists_of_" + filename + Integer.toString(fileCounter)  + ".txt", "rw");
                            //file.seek(file.length()); // append mode :)

                            byte[] encoded_reveiwsIds, encoded_freqs = new byte[0];
                            encoded_reveiwsIds = GroupVarint.encode(reviewIds2);

                            row.put("pl_reviewsIds_ptr", pl_ptr);
                            //file.write(encoded_reveiwsIds);
                            //outputPostingList.add(encoded_reveiwsIds);
                            bout.write(encoded_reveiwsIds);
                            pl_ptr += encoded_reveiwsIds.length;
                            pl_size += encoded_reveiwsIds.length;

                            if (filename.equals("words_")){
                                encoded_freqs = GroupVarint.encode(reviewFreqs2);
                                row.put("pl_reviewsFreqs_ptr", pl_ptr);
                                //file.write(encoded_freqs);
                                //outputPostingList.add(encoded_freqs);
                                bout.write(encoded_freqs);
                                pl_ptr += encoded_freqs.length;
                                pl_size += encoded_freqs.length;
                            }
                            //file.close();
                        }

                        catch (Exception e){
                            e.printStackTrace();
                        }

                        outputTable.add(row);
                        index2++;
                    }
                    if (outputBlock.size() == 4)
                    {
                        outputBlocks.add(outputBlock);
                        outputBlock = new ArrayList<>();
                    }
                }

            }
        }


        // if we end table 2
        if (ptr2 >= size2){
            // and we still got words from table 2 to work with
            if (ptr1 + index1 - 1 < size1 - 1) {
                while (ptr1 + index1 - 1 != size1 - 1)
                {
                    //System.out.println(pl_size);
                    if (pl_size > POSTING_LIST_LIMIT) { // we need to check the avg size of each array of bytes
                        /*try {
                            RandomAccessFile file = new RandomAccessFile(dir + "//" + "posting_lists_of_" + filename + Integer.toString(fileCounter) + ".txt", "rw");
                            file.seek(file.length()); // append mode :)
                            for (byte [] encoded_data : outputPostingList){
                                file.write(encoded_data);
                            }
                            outputPostingList = new ArrayList<>();
                            pl_size = 0;
                            file.close();
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }*/
                        pl_size = 0;
                        bout.flush();
                    }

                    // check if we need to load another block
                    if (index1 == block_size1 + 1)
                    {
                        // if we are on last block
                        if (ptr1 + block_size1 == size1 - remainder1)
                        {
                            ptr1 += block_size1;
                            startNextBlcok1 = wordsTypeStrings.get(0).length();
                            block_size1 = remainder1;
                            index1 = 1;
                        }
                        else{
                            ptr1 += block_size1;
                            index1 = 1;
                            startNextBlcok1 = tokensTypeTables.get(0).get(ptr1 + block_size1).get("term_ptr") + 1;
                        }

                        startBlock1 = tokensTypeTables.get(0).get(ptr1).get("term_ptr") + 1;
                        block1 = wordsTypeStrings.get(0).substring(startBlock1, startNextBlcok1);
                        block_words1 = block1.split("([@]+)|([*]+)|([|]+)");
                        prefix1 = block_words1[0];
                    }
                    else
                    {
                        String suffix1 = "";
                        if (block_words1.length > 1 && block_words1.length -1 == block_size1){
                            suffix1 = block_words1[index1];
                        }
                        outputBlock.add(prefix1 + suffix1);
                        Map<String, Integer> row = new HashMap<>();


                        // read posting list's pointers
                        int startReviewsID1 = tokensTypeTables.get(0).get(ptr1 + index1 - 1).get("pl_reviewsIds_ptr");

                        int startNextReviewsID1;
                        if (ptr1 + index1 == size1){
                            startNextReviewsID1 = 0;
                        }
                        else{
                            startNextReviewsID1 = tokensTypeTables.get(0).get(ptr1 + index1).get("pl_reviewsIds_ptr");
                        }

                        int startFreqs1 = 0;

                        if (filename.equals("words_")){
                            // sum freqs from both tables
                            row.put("total_freqs", tokensTypeTables.get(0).get(ptr1 + index1 - 1).get("total_freqs"));
                            startFreqs1 = tokensTypeTables.get(0).get(ptr1 + index1 - 1).get("pl_reviewsFreqs_ptr");
                        }

                        try {
                            RandomAccessFile file1 = new RandomAccessFile(dir + "//" + "posting_lists_of_" + filename + numbering.get(0) + ".txt", "rw");
                            file1.seek(startReviewsID1);

                            if (startNextReviewsID1 == 0){
                                startNextReviewsID1 = (int)file1.length();
                            }

                            // if we are on products
                            if (startFreqs1 == 0){
                                startFreqs1 = startNextReviewsID1;
                            }

                            byte [] reviewIdsBytes1 = new byte[(int)(startFreqs1 - startReviewsID1)];
                            file1.read(reviewIdsBytes1);
                            List<Integer> reviewIds1 = new ArrayList<>();
                            reviewIds1 = GroupVarint.decode(reviewIdsBytes1);

                            List<Integer> reviewFreqs1 = null;

                            // read freqs
                            if (filename.equals("words_")){
                                file1.seek(startFreqs1);
                                byte [] reviewFreqsBytes1 = new byte[(int)(startNextReviewsID1 - startFreqs1)];
                                file1.read(reviewFreqsBytes1);
                                reviewFreqs1 = new ArrayList<>();
                                reviewFreqs1 = GroupVarint.decode(reviewFreqsBytes1);
                            }

                            file1.close();

                            // write the re-encoded arrays
                            //RandomAccessFile file = new RandomAccessFile(dir + "//" + "posting_lists_of_" + filename + Integer.toString(fileCounter)  + ".txt", "rw");
                            //file.seek(file.length()); // append mode :)

                            byte[] encoded_reveiwsIds, encoded_freqs = new byte[0];
                            encoded_reveiwsIds = GroupVarint.encode(reviewIds1);


                            row.put("pl_reviewsIds_ptr", pl_ptr);
                            //file.write(encoded_reveiwsIds);
                            //outputPostingList.add(encoded_reveiwsIds);
                            bout.write(encoded_reveiwsIds);
                            pl_ptr += encoded_reveiwsIds.length;
                            pl_size += encoded_reveiwsIds.length;

                            if (filename.equals("words_")){
                                encoded_freqs = GroupVarint.encode(reviewFreqs1);
                                row.put("pl_reviewsFreqs_ptr", pl_ptr);
                                //file.write(encoded_freqs);
                                //outputPostingList.add(encoded_freqs);
                                bout.write(encoded_freqs);
                                pl_ptr += encoded_freqs.length;
                                pl_size += encoded_freqs.length;
                            }
                            //file.close();
                        }

                        catch (Exception e){
                            e.printStackTrace();
                        }

                        outputTable.add(row);
                        index1++;
                    }
                    if (outputBlock.size() == 4)
                    {
                        outputBlocks.add(outputBlock);
                        outputBlock = new ArrayList<>();
                    }
                }

            }
        }

        if (!outputBlock.isEmpty()){
            outputBlocks.add(outputBlock);
        }

            // here we write the output posting list, if we still got somthing left on buffer
            if (pl_size > 0){
                bout.flush();
            }
            bout.close();
            fout.close();
        }

        catch (Exception e){
            e.printStackTrace();
        }

        /*try {
            RandomAccessFile file = new RandomAccessFile(dir + "//" + "posting_lists_of_" + filename + Integer.toString(fileCounter) + ".txt", "rw");
            file.seek(file.length()); // append mode :)
            for (byte [] encoded_data : outputPostingList){
                file.write(encoded_data);
            }
            outputPostingList = new ArrayList<>();
            file.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }*/


        // here we delete the input posting lists files
        File pl1 = new File(dir +  "//" + "posting_lists_of_" + filename + numbering.get(0) + ".txt");
        File pl2 = new File(dir +  "//" + "posting_lists_of_" + filename + numbering.get(1) + ".txt");

        pl1.delete();
        pl2.delete();


        // here we encode the output string and put in an output file
        int head_counter = 0;
        for (List<String> block: outputBlocks) {
            int i = 0;
            String term = block.get(i);

            String prefix = Lexicon.getBlockCommonPrefix(block);
            String suffix = Lexicon.getSuffix(term, prefix);

            outputStr.append("|");

            // saving the location of the first term for each block
            int term_ptr = outputStr.length() - 1;
            outputTable.get(head_counter).put("term_ptr", term_ptr);
            outputStr.append(prefix).append("*").append(suffix);

            for (i = 1; i < block.size(); i++) { // check that later on - I think it will be alright
                term = block.get(i);
                suffix = Lexicon.getSuffix(term, prefix);
                outputStr.append("@").append(suffix);
            }
            head_counter+= block.size();
        }


        try{
            RandomAccessFile file = new RandomAccessFile(dir + "//" + filename + "lex_string_"  + Integer.toString(fileCounter) + ".txt", "rw");
            file.setLength(0);
            file.writeChars(outputStr.toString());
            file.close();

            // writing the full table into serialized file
            //List<Map<String, Integer>> rows = new ArrayList<>(outputTable);
            FileOutputStream fos = new FileOutputStream(dir + "//" + filename + "lex_table_" + Integer.toString(fileCounter)  + ".ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(outputTable);
            oos.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
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
