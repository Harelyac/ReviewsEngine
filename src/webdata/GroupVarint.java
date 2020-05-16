package webdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Native;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class GroupVarint {

    private static final int BLOCK_SIZE = 4;

    public static List<Integer> decode(byte[] bytes) {

        List<Integer> result = new ArrayList<>();
        int idx = 0;

        while(idx < bytes.length) {
            int[] numOfBytes;

            byte groups = bytes[idx];
            idx++;

            numOfBytes = decodeNumOfBytes(groups);

            for (int i = 0; i < 4 && idx < bytes.length;  i++) {
                int nbytes = numOfBytes[i];
                byte[] numberInBytes = new byte[nbytes];
                for (int j = 0; j < nbytes && idx < bytes.length; j++) {
                    numberInBytes[j] = bytes[idx];
                    idx++;
                }

                result.add(toInt(numberInBytes));
            }

        }
        return result;
    }

    public static byte[] encode(List<Integer> numbers) {
        List<Byte> result = new ArrayList<>();
        int idx = 0;
        while(idx < numbers.size()) {

            int group = 0;
            for (int i = idx; i < idx + 4 ; i++) {
                int numBytes;
                if (i < numbers.size()){
                    numBytes = getNumOfBytes(numbers.get(i)) - 1;
                }
                else {
                    numBytes = 0;
                }
                group = (group << 2 | numBytes);
            }
            result.add((byte) group);
            for (int i = 0; i < 4 && idx < numbers.size(); i++) {
                int number = numbers.get(idx);
                idx++;
                int numOfByte = getNumOfBytes(number);
                byte[] b = intToBytes(number);
                for (int j = 0; j < numOfByte; j++) {
                    //System.out.println(String.format("%8s",Integer.toBinaryString(b[j] & 0xFF)).replace(' ','0'));
                    result.add(b[j]);
                }

            }

        }

        byte[] encoded = new byte[result.size()];
        for (int i = 0; i < result.size(); i++) {
            encoded[i] = result.get(i);
        }

        return encoded;
    }

    private static int[] decodeNumOfBytes(byte groups) {
        int[] numOfBytes = new int[4];
        for (int i = 0; i < 4; i++) {
            numOfBytes[i] = (groups >> 6 - i*2 & 3)+1;
        }

        return numOfBytes;
    }

    public static byte[] intToBytes(final int i) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(i).array();
    }


    private static int getNumOfBytes(int val) {
        int nBits;
        if (val < 0x10000) {
            if (val < 0x100) {
                nBits = 8;
            } else {
                nBits = 16;
            }
        } else {
            nBits = 32;
        }
        return (nBits / 8);

    }
//    public static int toInt( byte[] bytes ) {
//        System.out.println("toint");
//        int result = 0;
//        for (byte b : bytes) {
//            result = (result << 8) | b;
//            System.out.println(result);
//        }
//        return (int) (result & 0xFFFFFFFFL);
//    }

    public static int toInt( byte[] bytes ) {
        int res = 0;
        if (bytes == null)
            return res;
        for (int i = bytes.length - 1; i >=0; i--) {
            byte aByte = bytes[i];
            res = (res << 8) + aByte;
        }
        return res & 0xFFFF;
    }

}