public class Treesitter{

  public static native long parserNew(); 

  public static native void parserDelete(long parser);

  public static native long queryNew(byte[] queryBytes, int queryLength); 

  public static native void queryDelete(long query);

  public static native long queryCursorNew(); 

  public static native void queryCursorDelete(long parser);

  //returns triplets written in an array form: (start_byte,end_byte, color_index)
  public static native int[] highlight(byte[] source, int length, long parser, long query, long cursor);
}