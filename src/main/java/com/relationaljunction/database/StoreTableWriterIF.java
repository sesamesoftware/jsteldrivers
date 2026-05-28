package com.relationaljunction.database;

import java.sql.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public interface StoreTableWriterIF {

   // returns an StoreTableIF instance
   StoreTableIF getStoreTable();

   // returns fields in the store
   StoreFieldIF[] getFields();

   // inserts record
   void clearRecords() throws StoreException;

   // inserts record
   void insertRecords(StoreRecordsIF recs) throws StoreException;

   // updates record
   @Deprecated
   void updateRecords(StoreRecordsIF recs) throws StoreException;

   // deletes record
   @Deprecated
   void deleteRecords(StoreRecordsIF recs) throws StoreException;

   void deleteRecords(PreparedStatement st) throws StoreException;

   void updateRecords(PreparedStatement st) throws StoreException;

   void rewrite(StoreRecordsIF recs) throws StoreException;

   // pack a file (remove deleted records from a file). It is used in StelsDBF
   void pack();

   void close();
}
