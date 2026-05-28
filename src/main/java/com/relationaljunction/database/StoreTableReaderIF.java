package com.relationaljunction.database;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 * @author not attributable
 * @version 2.2
 */

public interface StoreTableReaderIF {
  // returns an StoreTableIF instance
  StoreTableIF getStoreTable();

  // returns a next record from the store
  StoreRecordIF nextRecord() throws StoreException;

  // returns fields in the store
  StoreFieldIF[] getFields();

  int getRecordCount();

  void close();
}
