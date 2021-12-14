public class Parser implements AutoCloseable{
  private long ptr;

  Parser() {
    this.ptr = Treesitter.parserNew();
  }

  @Override
  public void close() {
    Treesitter.parserDelete(ptr);
  }

  public long get() {
    return ptr;
  }
}