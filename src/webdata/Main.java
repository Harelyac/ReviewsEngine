package webdata;

import java.io.*;
import java.util.*;


public class Main {
    public static void main(String[] args) throws IOException {
        IndexWriter siw = new IndexWriter();
        siw.write("C:/Users/harelyac/OneDrive/Desktop/InfoRetrieval/DataSets/1000.txt", "src/files");
        IndexReader ir = new IndexReader("src/files");

    /*    System.out.println(ir.getTokenFrequency("Greatest"));
        System.out.println("========================================");
        System.out.println(ir.getTokenSizeOfReviews());*/

        //Enumeration enumy = ir.getReviewsWithToken("ZuCchini");
        Enumeration enumy = ir.getProductReviews("B006K2ZZ7K");
        while (enumy.hasMoreElements()){
            System.out.println(enumy.nextElement());
        }

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

