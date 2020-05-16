package webdata;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;


public class Main {
    public static void main(String[] args) throws IOException {

        SlowIndexWriter siw = new SlowIndexWriter();
        siw.slowWrite("src/webdata/100.txt");

        IndexReader ir = new IndexReader("ReviewsData.txt"); // FIXME - CHAGE TO DIR NOT SPECIFIC FILE
        String token = "at";
        System.out.println(ir.getTokenSizeOfReviews());

        //test();
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

