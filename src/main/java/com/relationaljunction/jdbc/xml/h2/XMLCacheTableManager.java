package com.relationaljunction.jdbc.xml.h2;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.jdbc.common.h2.CacheTable;
import com.relationaljunction.jdbc.common.h2.CacheTableManager;
import com.relationaljunction.jdbc.common.h2.CommonConnection2;
import com.relationaljunction.jdbc.xml.h2.store.XMLTable;
import com.relationaljunction.utils.UnexpectedException;

public class XMLCacheTableManager extends CacheTableManager {
   private final Logger log = LoggerFactory.getLogger("CacheTableManager");

   XMLCacheTableManager(CommonConnection2 conn) {
      super(conn);
   }

   @Override
   protected CacheTable createTable(String sqlTableName) {
      return new XMLCacheTable(this, sqlTableName);
   }

   @Override
   protected void commit(Collection<CacheTable> cacheTables) throws SQLException {
      // filter only cache tables with unique XML file names.
      // For example an XML file may contain several tables, but we
      // should rewrite the file only once, not several times
      Set<String> fileNameSet = new HashSet<String>();
      LinkedList<CacheTable> uniqueCacheTablesPerXmlFile = new LinkedList<CacheTable>();

      for (CacheTable cacheTable : cacheTables) {
         String storeName = getFileName(cacheTable.getFileTableName());

         if (!fileNameSet.contains(storeName)) {
            // add the first cache table contained in the unique XML file name
            fileNameSet.add(storeName);
            uniqueCacheTablesPerXmlFile.add(cacheTable);
         } else {
            // otherwise clear a transaction flag for a cache table
            cacheTable.clearTransactionFlag();
         }
      }

      super.commit(uniqueCacheTablesPerXmlFile);
   }

   /*
   get a file name which a table belong to
    */
   private String getFileName(String storeName) {
      try {
         XMLTable xmlTable = (XMLTable) conn.getSchemaIF2().getStoreTable(storeName);
         return xmlTable.getFileManager().getPath();
      } catch (StoreException e) {
         throw new UnexpectedException(e);
      }
   }
}
