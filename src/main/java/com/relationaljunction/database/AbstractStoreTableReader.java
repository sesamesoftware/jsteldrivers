package com.relationaljunction.database;

import java.util.List;

import org.h2.index.Cursor;

public class AbstractStoreTableReader
    implements StoreTableReaderIF {

  public void close() {
  }

  public StoreFieldIF[] getFields() {
    return null;
  }

  public int getRecordCount() {
    return 0;
  }

  public StoreTableIF getStoreTable() {
    return null;
  }

   public Cursor getIndexCursor(List<String> columnNames) {
      return null;
   }

  public StoreRecordIF nextRecord() throws StoreException {
    return null;
  }

  public static void main(String[] args) {
  }
}
