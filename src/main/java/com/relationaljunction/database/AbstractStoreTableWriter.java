package com.relationaljunction.database;

import java.sql.*;

public abstract class AbstractStoreTableWriter
        implements StoreTableWriterIF {

   // returns fields in the store
   public StoreFieldIF[] getFields() {
      return null;
   }

   public void clearRecords() throws StoreException {
   }

   public void close() {
   }

   public void deleteRecords(PreparedStatement st) throws
           StoreException {
   }

   public void updateRecords(PreparedStatement st) throws
           StoreException {
   }

   public void rewrite(StoreRecordsIF recs) throws StoreException {

   }

   public StoreTableIF getStoreTable() {
      return null;
   }

   public void pack() {
   }
}
