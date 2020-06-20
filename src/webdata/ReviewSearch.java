package webdata;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.list;
import static java.util.Map.Entry.comparingByKey;
import static java.util.Map.Entry.comparingByValue;

public class ReviewSearch {

    public static final int NUMBER_OF_DOCUMENTS = 1000;
    public static int max_id = 0;
    private IndexReader ir;

    /**
     * Constructor
     */
    public ReviewSearch(IndexReader iReader) {
        ir = iReader;
    }

    // pad missing review ids with zero freqs
    public List<Integer> pad_with_zeros(List<Integer> pl){
        List<Integer> padded_list = new ArrayList<>();
        int next_id = 1;
        for (int i=0; i < pl.size() - 1; i+=2){
            Integer curr_id = pl.get(i);
            Integer curr_freq = pl.get(i+1);

            if (curr_id.equals(next_id)){
                padded_list.add(curr_id);
                padded_list.add(curr_freq);
                next_id += 1;
            }

            else {
                while (next_id != curr_id){
                    padded_list.add(next_id);
                    padded_list.add(0);
                    next_id += 1;
                }
                padded_list.add(curr_id);
                padded_list.add(curr_freq);
                next_id += 1;
            }
        }

        // check if we have more review id to pad
        if (next_id <= max_id){
            for (int i=next_id; i <= max_id; i++){
                padded_list.add(i);
                padded_list.add(0);
            }
        }

        return padded_list;
    }

    /**
     * Returns a list of the id-s of the k most highly ranked reviews for the
     * given query, using the vector space ranking function lnn.ltc (using the
     * SMART notation)
     * The list should be sorted by the ranking
     */
    public Enumeration<Integer> vectorSpaceSearch(Enumeration<String> query, int k) {
        // calculate the tf-idf vector of the query
        double tf, lb_tf; // aka log based
        double idf, st_idf;
        double tf_idf;



        ArrayList<String> query_list = list(query);

        //List<String> distinct_query_list = query_list.stream()
                                            //.distinct()
                                            //.collect(Collectors.toList());

        List<Double> query_vector = new ArrayList<Double>();

        List<List<Integer>> postinglists_of_query_terms = new ArrayList<>();


        double magnitude = 0;

        // calculate the tf-idfs vector of the query
        for (int i= 0; i < query_list.size(); i++){

            String term = query_list.get(i);

            // get frequency of term in query
            tf = Collections.frequency(query_list, term);
            lb_tf = tf > 0 ? 1 + Math.log10(tf) : 0;

            idf = ir.getTokenFrequency(term);
            st_idf = idf > 0 ? Math.log10(NUMBER_OF_DOCUMENTS/ idf) : 0;

            tf_idf = lb_tf * st_idf;
            query_vector.add(tf_idf);

            // here we also save term posting list for later
            List<Integer> pl = list(ir.getReviewsWithToken(term));

            int last_id = pl.get(Math.max(pl.size() - 2, 0));
            if (last_id > max_id) {
                max_id = last_id;
            }

            postinglists_of_query_terms.add(pl);

            // calculate magnitude for normalization
            magnitude += tf_idf * tf_idf;
        }

        //System.out.println(max_id); sanity check

        List<List<Integer>> postinglists_of_query_terms_padded = new ArrayList<>();

        for (int i = 0; i <= postinglists_of_query_terms.size() - 1; i++){
            List<Integer> padded_pl = pad_with_zeros(postinglists_of_query_terms.get(i));
            postinglists_of_query_terms_padded.add(padded_pl);
        }


        // normalize the query vector
        double finalMagnitude = Math.sqrt(magnitude);
        query_vector.stream().map(element -> element / finalMagnitude);

        // create the documents vectors out of the posting list of query term
        Map<Integer, ArrayList<Double>> map = new HashMap<Integer, ArrayList<Double>>();


        for (int i=0; i < postinglists_of_query_terms_padded.size(); i++){
            for (int j=0; j < postinglists_of_query_terms_padded.get(i).size() - 1; j+=2){
                Integer review_id = postinglists_of_query_terms_padded.get(i).get(j);
                ArrayList<Double> values = map.computeIfAbsent(review_id, k1 -> new ArrayList<Double>());
                int freq = postinglists_of_query_terms_padded.get(i).get(j + 1);
                Double log_freq = freq > 0 ? 1 + Math.log10(freq) : 0;
                values.add(log_freq);
            }
        }


        // calculate importance of each document compare to the query
        // importance array of each document
        Map<Integer, Double> importance_vector = new HashMap<>();

        double sum = 0;
        for (Map.Entry<Integer, ArrayList<Double>> document_vector : map.entrySet()){
            double score = 0;
            for (int i = 0; i < query_vector.size(); i++){
                score += document_vector.getValue().get(i) * query_vector.get(i);
            }
            importance_vector.put(document_vector.getKey(), score);
        }

        // .thenComparing(Map.Entry::getKey)
        // sort first based on score then based on id
        Map<Integer, Double> sortedMap = importance_vector.entrySet().stream()
                .sorted(Comparator.comparing((Function<Map.Entry<Integer, Double>, Double>) Map.Entry::getValue).reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));


        List<Integer> ids = new ArrayList<>(sortedMap.keySet());


        return  Collections.enumeration(ids.subList(0, Math.min(k, ids.size()))); // it's sorted from low to high
    }
    /**
     * Returns a list of the id-s of the k most highly ranked reviews for the
     * given query, using the language model ranking function, smoothed using a
     * mixture model with the given value of lambda
     * The list should be sorted by the ranking
     * @return
     */
     public Enumeration<Integer> languageModelSearch(Enumeration<String> query,
                                                    double lambda, int k) {
         // calculate the tf-idf vector of the query
         double cf; // aka log based
         double T = ir.getTokenSizeOfReviews();

         ArrayList<String> query_list = list(query);

         List<List<Integer>> postinglists_of_query_terms = new ArrayList<>();

         List<Double> probability_based_on_corpus = new ArrayList<>();

         // calculate the tf-idfs vector of the query
         for (int i= 0; i < query_list.size(); i++){

             String term = query_list.get(i);

             // get frequency of term in query
             cf = ir.getTokenCollectionFrequency(term);
             probability_based_on_corpus.add(cf / T);

             // here we also save term posting list for later
             List<Integer> pl = list(ir.getReviewsWithToken(term));

             int last_id = pl.get(Math.max(pl.size() - 2, 0));
             if (last_id > max_id) {
                 max_id = last_id;
             }

             postinglists_of_query_terms.add(pl);
         }

         //System.out.println(max_id); sanity check

         List<List<Integer>> postinglists_of_query_terms_padded = new ArrayList<>();

         for (int i = 0; i <= postinglists_of_query_terms.size() - 1; i++){
             List<Integer> padded_pl = pad_with_zeros(postinglists_of_query_terms.get(i));
             postinglists_of_query_terms_padded.add(padded_pl);
         }


         // create the documents vectors out of the posting list of query term
         Map<Integer, ArrayList<Double>> map = new HashMap<Integer, ArrayList<Double>>();


         for (int i=0; i < postinglists_of_query_terms_padded.size(); i++){
             for (int j=0; j < postinglists_of_query_terms_padded.get(i).size() - 1; j+=2){
                 Integer review_id = postinglists_of_query_terms_padded.get(i).get(j);
                 ArrayList<Double> values = map.computeIfAbsent(review_id, k1 -> new ArrayList<Double>());
                 double freq = postinglists_of_query_terms_padded.get(i).get(j + 1);
                 double review_len = ir.getReviewLength(review_id);
                 values.add(freq / review_len);
             }
         }

         Map<Integer, Double> scores = new HashMap<>();
         for (Map.Entry<Integer, ArrayList<Double>> document : map.entrySet()){
             double score = 1;
             for (int i = 0; i < document.getValue().size(); i++){
                 score *= (((1- lambda) * probability_based_on_corpus.get(i)) + (lambda * document.getValue().get(i)));
             }
             scores.put(document.getKey(), score);
         }

         // sort first based on score then based on id
         Map<Integer, Double> sortedMap = scores.entrySet().stream()
                 .sorted(Comparator.comparing((Function<Map.Entry<Integer, Double>, Double>) Map.Entry::getValue).reversed())
                 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));


         List<Integer> ids = new ArrayList<>(sortedMap.keySet());

         return  Collections.enumeration(ids.subList(0, Math.min(k, ids.size()))); // it's sorted from low to high
     }
     /**
     * Returns a list of the id-s of the k most highly ranked productIds for the
     * given query using a function of your choice
     * The list should be sorted by the ranking
     */public Collection<String> productSearch(Enumeration<String> query, int k) {

         List<String> list_query = Collections.list(query);
         List<String> chosen_products_id = new ArrayList<>();

         while (chosen_products_id.size() < k){
             Set<Integer> bag1 = new HashSet<>(Collections.list(vectorSpaceSearch(Collections.enumeration(list_query), 3*k)));
             Set<Integer> bag2 = new HashSet<>(Collections.list(languageModelSearch(Collections.enumeration(list_query), 0.4, 3*k)));
             bag1.retainAll(bag2);

             Iterator<Integer> it = bag1.iterator();
             while (it.hasNext()){
                 int curr = it.next();
                 if (ir.getReviewScore(curr) >= 1 || (ir.getReviewHelpfulnessNumerator(curr) / ir.getReviewHelpfulnessDenominator(curr)) >= 1){
                     chosen_products_id.add(ir.getProductId(curr));
                 }
                 it.remove(); // either way
             }
         }

         Map<String ,Integer> scores = new HashMap<>();
         if (chosen_products_id.size() > k){
             for (String id : chosen_products_id){
                 scores.put(id, Collections.list(ir.getProductReviews(id)).size());
             }
         }

         Map<String, Integer> sortedMap = scores.entrySet().stream()
                 .sorted(Comparator.comparing((Function<Map.Entry<String, Integer>, Integer>) Map.Entry::getValue).reversed())
                 .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

         List<String> ids = new ArrayList<>(sortedMap.keySet());

         return ids.subList(0, Math.min(k, ids.size()));
     }
}


