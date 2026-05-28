package com.relationaljunction.database;

import com.relationaljunction.database.index.*;
import com.relationaljunction.database.view.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public interface StoreSchemaIF {
   String LOCK_TYPE = "lockType";
   String EXCLUSIVE_LOCK_STRING = "exclusive";
   String SHARED_LOCK_STRING = "shared";
   String LOCK_FILES = "lockFiles";

//   public static enum LockType {
//      SHARED("SHARED"),
//      EXCLUSIVE("EXCLUSIVE"),
//      NONE("NONE");
//
//      private String name;
//
//      private LockType(String name) {
//         this.name = name;
//      }
//   }

   String getName();

   String getFileTableName(String sqlTableName);

   String getFileExtension();

   String getDefaultFileExtension();

   boolean isModificationDateChanged() throws StoreException;

   void reload() throws StoreException;

   void createStoreTable(String storeName, StoreFieldIF[] fields, IndexTableIF[] indexTables,
                         java.util.Properties props) throws
           StoreException;

   void deleteStoreTable(String storeName) throws StoreException;

   void deleteStoreTableFromCache(String storeName);

   StoreTableIF getStoreTable(String storeName) throws StoreException;

   StoreTableIF[] getStoreTables(String templateName) throws
           StoreException;

   java.util.Properties getSchemaProperties();

   boolean supportsIndexes();

   boolean supportsExternalEngine();

   String getExternalEngineClass();

   IndexSchemaIF getIndexSchema();

   ViewSchemaIF getViewSchema();

   boolean requiresLockingForWritingOperations();

   boolean requiresLockingForReadingOperations();

   boolean requiresLoadingTablesInMetaData();

   boolean supportsIdentityInNonCachingMode();

   boolean includeRowIdColumn();

   /**
    * lock at thread level
    */
   void lockForReading();

   /**
    * lock at thread level
    */
   void lockForWriting();

   /**
    * lock at file level
    */
   void lockDatabase() throws Exception;

   /**
    * lock at file level
    */
   void unlockDatabase() throws Exception;

   void unlock();

   void close();
}
