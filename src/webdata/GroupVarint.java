package webdata;

public class GroupVarint {

    public static void encode(PostingList list)
    {
        Integer first = list.list.get(0);
        System.out.println(first.byteValue());
        for (int i = 0; i < list.list.size(); i++) {
            System.out.println(list.list.get(i));
        }
    }
}
