public class Query implements AutoCloseable{
  private long queryPtr;
  private long queryCursorPtr;

  Query(byte[] queryBytes, int queryLength) {
    this.queryPtr = Treesitter.queryNew(queryBytes, queryLength);
    this.queryCursorPtr = Treesitter.queryCursorNew();
  }

  @Override
  public void close() {
    Treesitter.queryDelete(queryPtr);
    Treesitter.queryCursorDelete(queryCursorPtr);
  }

  public long getQuery() {
    return queryPtr;
  }

  public long getCursor() {
    return queryCursorPtr;
  }
}