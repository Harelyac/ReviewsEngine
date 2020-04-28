package webdata;
import com.sun.jdi.IntegerType;

import java.io.*;
import java.util.*;

public class SlowIndexWriter {
    /**
     * Given product review data, creates an on disk index
     * inputFile is the path to the file containing the review data
     * dir is the directory in which all index files will be created
     * if the directory does not exist, it should be created
     */
    private static String usingBufferedReader(String filePath)
    {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath)))
        {

            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null)
            {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }
    public void slowWrite(String inputFile) {
        File file = new File(inputFile);


        Map<String, List<Integer>> productIdIndex  = new Hashtable<String, List<Integer>>();
        Map<String, List<Integer>> wordsIndex  = new Hashtable<String, List<Integer>>();
        List<Integer> helpNumeratorPerReview = new ArrayList<>();
        List<Integer> helpDenumeratorPerReview = new ArrayList<>();
        List<Double> scorePerReview = new ArrayList<>();
        List<Integer> lengthPerReview = new ArrayList<>();


        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int reviewId = 0;
            while ((line = br.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line,":(),-'!\". ");

                while(tokenizer.hasMoreTokens() && reviewId < 3)
                    {
                        String firstToken = tokenizer.nextToken();
                        String token;
                        switch (firstToken)
                        {
                            case "product/productId":
                                System.out.println("product/productId");
                                reviewId += 1;
                                token = getToken(tokenizer);
                                updateIndex(token, reviewId, productIdIndex);
                                break;
                            case "review/score":
                                System.out.println("review/score");
                                int num = Integer.parseInt(getToken(tokenizer));
                                int dec = Integer.parseInt(getToken(tokenizer));
                                double score = num + (dec*0.1);
                                scorePerReview.add(score);
                                break;
                            case "review/helpfulness":
                                System.out.println("review/helpfulness");
                                token = getToken(tokenizer);
                                String[] split = token.split("/");
                                helpNumeratorPerReview.add(Integer.parseInt(split[0]));
                                helpDenumeratorPerReview.add(Integer.parseInt(split[1]));
                                break;
                            case "review/text":
                                System.out.println("review/text");
                                int textLen = tokenizer.countTokens();
                                while(tokenizer.hasMoreTokens()){
                                    token = getToken(tokenizer);
                                    updateIndex(token, reviewId, wordsIndex);
                                    lengthPerReview.add(textLen);
                                }

                                break;
                            default:
                                break;
                        }
                    }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getToken(StringTokenizer tokenizer) {
        return tokenizer.nextToken().toLowerCase();
    }

    private void updateIndex(String token, int reviewId, Map<String, List<Integer>> indexMap) {

        if(!indexMap.containsKey(token)){
            indexMap.put(token, new ArrayList<Integer>());
            indexMap.get(token).add((reviewId));
        }
        else {
            List<Integer> postingList = indexMap.get(token);
            Integer last = postingList.get(postingList.size() - 1);
            if (!postingList.contains(reviewId))
            {
                indexMap.get(token).add((reviewId - last));

            }
        }
    }

    /**
     * Delete all index files by removing the given directory
     */
    public void removeIndex(String dir) {}
}
