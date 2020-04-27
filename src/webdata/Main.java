package webdata;

public class Main {
    public static void main(String[] args) {
        SlowIndexWriter siw = new SlowIndexWriter();
        siw.slowWrite(args[0], "C:\\Users\\harelyac\\OneDrive\\Desktop\\InfoRetrieval\\indexfiles");
    }
}
