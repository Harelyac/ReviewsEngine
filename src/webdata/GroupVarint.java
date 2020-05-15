package webdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class GroupVarint {

    private static final int BLOCK_SIZE = 4;

    public static List<Integer> decode(byte[] bytes) {
        List<Integer> result = new ArrayList<>();
        byte b;
        int idx = 1;
        byte groups = bytes[0];
        int[] numOfBytes = new int[4];
        numOfBytes = decodeNumOfBytes(groups);

        for (int i = 0; i < 4; i++) {
            byte[] numberInBytes = new byte[4];
            for (int j = 0; j < numOfBytes[i]; j++) {
                numberInBytes[j] = (byte) (bytes[idx] );
                idx++;
            }
            result.add(fromByteArray(numberInBytes));

        }

        return result;
    }

    private static int[] decodeNumOfBytes(byte groups) {
        int[] numOfBytes = new int[4];
        for (int i = 0; i < 4; i++) {
            numOfBytes[i] = (groups >> 6 - i*2 & 3)+1;
        }

        return numOfBytes;
    }

    public static byte[] intToBytes(final int i) {
        return ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putInt(i).array();
    }

    public static byte[] encode(List<Integer> numbers) {
        byte[] result = new byte[8];
        int group = 0;
        for (int i = 0; i < 4; i++) {
            group = (group << 2 | getNumOfBytes(numbers.get(i)) - 1);
        }
        result[0] = (byte) group;

        int idx = 1;
        for (int i = 0; i < 4; i++) {
            int number = numbers.get(i);
            byte[] b = intToBytes(number);
            for (int j = 0; j < getNumOfBytes(number); j++) {
                result[idx] = (byte) (b[j]);
                idx++;
            }
        }
        return Arrays.copyOfRange(result,0,idx);
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
        return nBits / 8;

    }

    static int fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();

    }

}