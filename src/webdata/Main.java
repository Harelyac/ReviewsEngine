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

//        SlowIndexWriter siw = new SlowIndexWriter();
//        siw.slowWrite("src/webdata/100.txt");
//
//        IndexReader ir = new IndexReader("ReviewsData.txt");
//        String token = "several";

        List<Integer> l = new ArrayList<>();
        List<Integer> l2 = new ArrayList<>();
        l.add(80);
        l.add(320);
        l.add(31);
        l.add(255);

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
            System.out.print(String.format("%8s",Integer.toBinaryString(b & 0xFF)).replace(' ','0'));
        }
        System.out.println();
        System.out.println("============\nAfter decoding (binary):\n============");
        l2 = GroupVarint.decode(arr);
        for (int c : l2) {
            System.out.println(String.format("%8s", Integer.toBinaryString(c & 0xFF)).replace(' ', '0'));
        }
        System.out.println("============\nAfter decoding (decimal):\n============");
        for (int a : l2) {
            System.out.println(a);
        }
    }
}

