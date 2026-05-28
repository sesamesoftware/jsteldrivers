package com.relationaljunction.database;

import java.util.*;

abstract public class AbstractStoreTable implements StoreTableIF {
  /** file modification date **/
  public Date fileModificationDate = null;

  public Date getModificationDate() {
    return fileModificationDate;
  }

  public Date refreshModificationDate() throws StoreException{
    return null;
  }

  abstract public String getName();

  abstract public StoreTableReaderIF getReader() throws StoreException;

  abstract public Properties getTableProperties();

  abstract public StoreTableWriterIF getWriter(StoreFieldIF[] fields) throws
      StoreException;

  public boolean isReadOnly() {
    return false;
  }
}
