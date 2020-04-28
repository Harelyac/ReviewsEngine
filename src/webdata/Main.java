package webdata;

public class Main {
    public static void main(String[] args) {
        SlowIndexWriter siw = new SlowIndexWriter();
        siw.slowWrite("src/webdata/100.txt");
    }
}
