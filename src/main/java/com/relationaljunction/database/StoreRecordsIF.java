package com.relationaljunction.database;

public interface StoreRecordsIF {

  void beforeFirst();

  boolean hasNext();

  StoreRecordIF nextRecord();

  void clear();
}
