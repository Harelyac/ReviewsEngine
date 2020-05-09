package webdata;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class GroupVarint {

    private static final int BLOCK_SIZE = 4;

    public static int[] decode(RandomAccessFile reader) {
        try {
            int[] numOfBytes = new int[BLOCK_SIZE];
            byte[][] numbersInBytes = new byte[4][4];
            int[] numbers = new int[4];

            int nBytes = reader.readByte();
            for (int i = 0; i < 4; i++) {
                nBytes <<= i * 2;
                nBytes >>= 6;
                numOfBytes[i] = nBytes;
            }

            for (int i = 0; i < 4; i++) { //read number from file
                reader.read(numbersInBytes[i], 0, numOfBytes[i]);
            }

            for (int i = 0; i < 4; i++) { //convert to int
                numbers[i] = fromByteArray(numbersInBytes[i]);
            }

            return numbers;
        } catch (IOException e) {
            return new int[0];
        }
    }

    public static byte[] encode(List<Integer> numbers) {
        List<Byte> group = new ArrayList<>();
        byte sizeOfGroup = 0;
        int size, num;
        for (int i = 0; i < numbers.size(); i++) {
            num = numbers.get(i);
            if (num == 0) { //if num == 0, write padding
                group.add((byte) num);
                continue;
            }

            size = getNumOfBytes(num);
            addNumberToGroup(group, num);
            sizeOfGroup = (byte) (sizeOfGroup + (size << (6 - i * 2)));

        }
        group.add(0, sizeOfGroup);
        byte[] result = new byte[group.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = group.get(i);
        }
        return result;
    }

    private static void addNumberToGroup(List<Byte> group, int num) {
        byte[] numInBytes = tooByteArray(num); //get number in bytes
        boolean seenNumbers = false;
        for (byte b : numInBytes) { // add number minimal representation
            if (b != 0 || seenNumbers) { // do not add first 0 bytes
                seenNumbers = true;
                group.add(b);
            }
        }
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

    public static byte[] tooByteArray(int a) {
        byte[] ret = new byte[4];
        ret[3] = (byte) (a & 0xFF);
        ret[2] = (byte) ((a >> 8) & 0xFF);
        ret[1] = (byte) ((a >> 16) & 0xFF);
        ret[0] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }

    public static int circularShift(int n, int k) {
        return (n << k) | (n >> (Integer.SIZE - k));
    }
    static int fromByteArray(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

}