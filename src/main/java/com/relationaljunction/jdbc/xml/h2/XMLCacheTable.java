package com.relationaljunction.jdbc.xml.h2;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.StoreTableReaderIF;
import com.relationaljunction.database.io.FileManager;
import com.relationaljunction.jdbc.common.h2.CacheTable;
import com.relationaljunction.jdbc.common.h2.CacheTableManager;
import com.relationaljunction.jdbc.xml.h2.store.XMLTable;
import com.relationaljunction.jdbc.xml.h2.store.XMLTableLoaderIF;
import com.relationaljunction.jdbc.xml.h2.store.XMLTableSaverIF;

public class XMLCacheTable extends CacheTable {
	
   private final Logger log = LoggerFactory.getLogger("XMLCacheTable");

   XMLCacheTable(CacheTableManager cacheTableManager, String sqlTableName) {
      super(cacheTableManager, sqlTableName);
   }

   protected void InsertDataToCacheTable(StoreTableReaderIF reader,
                                         PreparedStatement pst) throws Exception {
      XMLTable xmlTable = (XMLTable) reader.getStoreTable();
      XMLTableLoaderIF xmlTableLoader = xmlTable.getLoader();
      xmlTableLoader.setInsertPreparedStatement(pst);
      xmlTableLoader.loadTable();
      xmlTableLoader.clear();
      // PreparedStatement will be closed in super class CacheTable
   }

   protected void fullyRewrite(StoreTableIF store) throws Exception {

      XMLTable xmlTable = (XMLTable) store;

      // use jdbc2xml to save data
      if (xmlTable.getTableDescription().getXMLOutput() != null) {
         processViaJdbc2Xml(xmlTable);
      }
      // use default mode to save data
      else {
         com.relationaljunction.utils.OtherUtils.writeLogInfo(log, "fully rewriting the XML table '" +
                 sqlTableName + "' using default writer");

         Statement st = cacheTableManager.getConnection().getH2Connection().createStatement();
         ResultSet rs = st.executeQuery("SELECT * FROM " + sqlTableName);

         // ResultSet should be closed in fullyRewriteStoreTable()
         fullyRewriteTableUsingDefaults(rs, (XMLTable) store, null);

         st.close();
      }
   }

   /*
     writing an XML using Jdbc2Xml package
    */
   private void processViaJdbc2Xml(XMLTable xmlTable) throws Exception {
      com.relationaljunction.utils.OtherUtils.writeLogInfo(log, "fully rewriting the XML table '" +
              sqlTableName + "'(" + xmlTable.getFileManager().getName() + ") using jdbc2xml");

      FileManager tempFileManager = xmlTable.getFileManager().getDir().
              getFileManager(xmlTable.getFileManager().getName() + ".tmp");

//      xmlTable.getFileManager().getDir().dropFile(tempFileManager.getName());
//      xmlTable.getFileManager().getDir().createFile(tempFileManager.getName());

      Writer writer = new BufferedWriter(new OutputStreamWriter(
              tempFileManager.getOutputStream(false), xmlTable.charset));

      xmlTable.getTableDescription().getXMLOutput().
              execute(cacheTableManager.getConnection(), writer);

      writer.close();

      // rename a temp file to an original one
      xmlTable.getFileManager().getDir().
              rename(tempFileManager.getName(), xmlTable.getFileManager().getName());

      xmlTable.getFileManager().upload();
   }

   /*
    writing an XML using default writer
   */
   static void fullyRewriteTableUsingDefaults(ResultSet rs,
                                              XMLTable xmlTable, FileManager fmSave) throws
           Exception {
      XMLTableSaverIF xmlTableSaver = xmlTable.getSaver();

      if (fmSave == null) {
         xmlTableSaver.saveTable(rs, xmlTable.getFileManager());
      } else {
         xmlTableSaver.saveTable(rs, fmSave);
      }

      xmlTableSaver.clear();

      // ResultSet is being closed here
      rs.close();
   }

}
