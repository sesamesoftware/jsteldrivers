package com.relationaljunction.database;

import com.relationaljunction.database.index.IndexTableIF;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 * @author not attributable
 * @version 2.2
 */


public interface StoreTableIF {

  // returns the name of a store
  String getName();

  StoreTableReaderIF getReader() throws StoreException;

  java.util.Date refreshModificationDate() throws StoreException;

  void create(StoreFieldIF[] fields, IndexTableIF[] indexTables) throws StoreException;

  // returns writer. If the fields param is not null a table must be rewritten
  StoreTableWriterIF getWriter(StoreFieldIF[] fields) throws
      StoreException;

  boolean isReadOnly();

  // returns additional properties, if it is applied
  java.util.Properties getTableProperties();

}
