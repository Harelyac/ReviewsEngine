package webdata;

import java.io.*;
import java.net.SocketOption;
import java.util.*;


public class Main {
    public static void main(String[] args) throws IOException {
        IndexWriter siw = new IndexWriter();
        siw.write("C:\\Users\\harelyac\\OneDrive\\Desktop\\InfoRetrieval\\ReviewsEngine\\src\\webdata\\1000.txt", "src/files");
        IndexReader ir = new IndexReader("src/files");

        ReviewSearch rs = new ReviewSearch(ir);
        List<String> query = new ArrayList<>();
        query.add("It");
        query.add("is");
        query.add("a");
        query.add("light");
        query.add("citrus");
        query.add("gelatin");
        query.add("nuts");


        Collection<String> products = rs.productSearch(Collections.enumeration(query), 11);
        System.out.println(products);


       /* ArrayList<Integer> scores = Collections.list(rs.vectorSpaceSearch(Collections.enumeration(query), 5));

        System.out.println(scores);

        ArrayList<Integer> scores1 = Collections.list(rs.languageModelSearch(Collections.enumeration(query), 0.4, 5));

        System.out.println(scores1);*/

        /*System.out.println(ir.getTokenFrequency("Greatest"));
        System.out.println("========================================");
        System.out.println(ir.getTokenSizeOfReviews());*/

        //Enumeration enumy = ir.getReviewsWithToken("ZuCchini");

        /*Enumeration enumy = ir.getProductReviews("B006K2ZZ7K");
        while (enumy.hasMoreElements()){
            System.out.println(enumy.nextElement());
        }*/

    }

    public static void test(){
        List<Integer> l = new ArrayList<>();

        l.add(3);
        l.add(320);
        l.add(320);
        l.add(320);
        l.add(320);
        l.add(2222);
        l.add(320);
        l.add(320);
        l.add(3543);
        l.add(320);
        l.add(320);
        l.add(320);
        l.add(320);

        System.out.println(l.size());
        System.out.println("============\nNumbers:\n============");
        for (int a : l) {
            System.out.println(a);
        }

//        GroupVar
        byte[] arr;
        arr = GroupVarint.encode(l);
        System.out.println("============\nAfter encoding:\n============");
//        GroupVar
        for (byte b : arr) {
            System.out.println(String.format("%8s",Integer.toBinaryString(b & 0xFF)).replace(' ','0'));
        }
        System.out.println();
        System.out.println("============\nAfter decoding (binary):\n============");

//        l2 = GroupVarint.decode(arr);
//        for (int c : l2) {
//            System.out.println(String.format("%8s", Integer.toBinaryString(c & 0xFF)).replace(' ', '0'));
//        }

        System.out.println("============\nAfter decoding (decimal):\n============");
        List<Integer> l2 = new ArrayList<>();
        l2 = GroupVarint.decode(arr);
        for (int a : l2) {
            System.out.println(a);
        }
    }
}

