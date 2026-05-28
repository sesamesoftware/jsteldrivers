package com.relationaljunction.database.index;

import java.util.Properties;

import com.relationaljunction.database.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 * @author not attributable
 * @version 2.2
 */

public interface IndexSchemaIF {
  String INDEX_TYPE_PROPERTY = "index_type";
  String UNIQUE_PROPERTY = "unique";

  void addIndex(String indexName, String indexedTable,
                IndexFieldIF[] fields, Properties props);

  void dropIndex(String indexName);

  void reIndex(String indexedTable);

  IndexTableIF getStoreIndex(String indexName);

  IndexTableIF[] getStoreIndexes(String tableName)  throws StoreException;

  TablesRelationship[] getRelationships() throws StoreException;

  Properties getSchemaProperties();
}
