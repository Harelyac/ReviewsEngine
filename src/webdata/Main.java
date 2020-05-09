package webdata;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        SlowIndexWriter siw = new SlowIndexWriter();
        siw.slowWrite("src/webdata/100.txt");
    }
}
