package com.relationaljunction.jdbc.xml.h2;

import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;

import com.relationaljunction.database.*;
import com.relationaljunction.database.io.*;
import com.relationaljunction.jdbc.common.h2.*;
import com.relationaljunction.jdbc.common.h2.sql.*;
import com.relationaljunction.jdbc.xml.h2.store.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2006</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.5
 */

public class XMLStatement2
        extends CommonStatement2 {
   public XMLStatement2(CommonConnection2 conn, Statement h2Stat) {
      super(conn, h2Stat);
   }

//    protected void processOtherDML(DML dml) throws Exception {
/*
      if (dml instanceof CreateXMLTableUsingSpecDML){
	processCreateTableUsingSpec( (CreateXMLTableUsingSpecDML) dml);
	return;
      }

      java.util.Set tableNames = dml.getUsedTableNames();
      conn.loadSQLTablesFromStore(tableNames);

      if (dml instanceof SaveXMLTableFromQueryDML)
	processSaveSQLQuery((SaveXMLTableFromQueryDML)dml);
      else if (dml instanceof SaveXMLTableDML)
	processSaveTable((SaveXMLTableDML)dml);
      else throw new Exception("Unknown DML operation!");
*/
//    }

   protected void processCreateTable(SQLCommand command) throws SQLException {
      CreateTableSQLCommand createTableCommand = (CreateTableSQLCommand) command;
      String baseTable = command.getBaseTable();

      if (createTableCommand.isUsingSpecification()) {
         try {
            XMLTable xmlSpecTable = (XMLTable) conn.getSchemaIF2().getStoreTable(conn.
                    getCacheTableManager().getFileTableName(baseTable));
            StoreFieldIF[] fields = xmlSpecTable.getFields();
            xmlSpecTable.create(fields, null);
         } catch (StoreException ex) {
            throw conn.driver.createException("Can't create the file '" +
                    conn.getCacheTableManager().
                            getFileTableName(baseTable) +
                    "' using a specification in the schema file. Error was: " +
                    ex.getMessage(), ex);
         }
      } else {
         super.processCreateTable(command);
      }
   }

   protected void processSave(SQLCommand command) throws SQLException {
      SaveCommand saveCommand = (SaveCommand) command;
      String baseTable = saveCommand.getBaseTable();

      if (baseTable != null) {
         // SAVE table_name as_file_path_opt using_specification_opt
         try {
            processSaveTable(saveCommand, baseTable);
         } catch (Exception ex) {
            throw conn.driver.createException(
                    "Error while executing SAVE command: " + ex.getMessage(), ex);
         }
      } else {
         // SAVE query as_file_path using_specification
         try {
            processSaveQuery(saveCommand, baseTable);
         } catch (Exception ex) {
            throw conn.driver.createException(
                    "Error while executing SAVE command: " + ex.getMessage(), ex);
         }
      }
   }

   private void processSaveTable(SaveCommand saveCommand, String baseTable) throws
           Exception {
      XMLTable xmlTable;
      try {
         xmlTable = (XMLTable) conn.getSchemaIF2().getStoreTable(conn.
                 getCacheTableManager().getFileTableName(baseTable));
      } catch (StoreException ex) {
         if (saveCommand.getFilePath() == null && saveCommand.getSpecTable() == null)
            throw conn.driver.createException(
                    "Can't find the table specification in the schema for the table: '" +
                            saveCommand.getBaseTable() + "'. Describe the table in the schema " +
                            "or use obviously the options 'AS file_name USING SPECIFICATION table_spec' with the command SAVE TABLE", ex);
         else
            throw new SQLException(ex.getMessage());
      }

      XMLTable xmlSpecTable;
      try {
         if (saveCommand.getSpecTable() == null) {
            xmlSpecTable = xmlTable;
         } else {
            xmlSpecTable = (XMLTable) conn.getSchemaIF2().getStoreTable(conn.
                    getCacheTableManager().getFileTableName(saveCommand.getSpecTable()));
         }
      } catch (StoreException ex) {
         throw conn.driver.createException(
                 "Can't find the table specification in the schema for the table: '" +
                         saveCommand.getSpecTable() + "'.", ex);
      }

      FileManager fmSave;
      if (saveCommand.getFilePath() == null)
         fmSave = xmlTable.getFileManager();
      else
         fmSave = ((XMLStoreSchema) conn.getSchemaIF2()).initFileManager(saveCommand.
                 getFilePath());

      ResultSet rs = this.executeQuery("SELECT * FROM " + baseTable);
      XMLCacheTable.fullyRewriteTableUsingDefaults(rs, xmlSpecTable, fmSave);
   }

   private void processSaveQuery(SaveCommand saveCommand, String baseTable) throws
           Exception {
      XMLTable xmlSpecTable;
      try {
         xmlSpecTable = (XMLTable) conn.getSchemaIF2().getStoreTable(conn.
                 getCacheTableManager().getFileTableName(saveCommand.getSpecTable()));
      } catch (StoreException ex) {
         throw conn.driver.createException(
                 "Can't find the table specification in the schema for the table: '" +
                         saveCommand.getSpecTable() + "'.", ex);
      }

      FileManager fmSave = ((XMLStoreSchema) conn.getSchemaIF2()).initFileManager(
              saveCommand.getFilePath());
      ResultSet rs = this.executeQuery(saveCommand.getQuery());
      XMLCacheTable.fullyRewriteTableUsingDefaults(rs, xmlSpecTable, fmSave);
   }

/*
  private void processSaveTable(SaveXMLTableDML dml) throws Exception {
    XMLTable xmlTable = null;
    try {
      xmlTable = (XMLTable) conn.getSchema().getStoreTable(conn.
	  getFullTableName(dml.getPrimaryTableName()));
    }
    catch (StoreException ex) {
      if (dml.getSavePath() == null && dml.getTableSpecName() == null)
	throw new Exception(
	    "Can't find the table specification in the schema for the table: '" +
	    dml.getPrimaryTableName() + "'. Describe the table in the schema or use obviously the options 'AS file_name USING SPECIFICATION table_spec' with the command SAVE TABLE");
    }

    XMLTable xmlSpecTable = null;
    if (dml.getTableSpecName() == null)
      xmlSpecTable = xmlTable;
    else
      xmlSpecTable = (XMLTable) conn.getSchema().getStoreTable(conn.
	  getFullTableName(dml.getTableSpecName()));

    FileManager fmSave = null;
    if (dml.getSavePath() == null)
      fmSave = xmlTable.getFilePath();
    else
      fmSave = ( (XMLSchema) conn.getSchema()).getFileManager(dml.getSavePath());

    SQLTable t = conn.getTableCatalog().getTable(dml.getPrimaryTableName());
    XMLRecordProvider recProvider = new XMLRecordProvider(xmlSpecTable, t, false);
    recProvider.saveTable(fmSave.getOutputStream(false));
    conn.getTableCatalog().setRowProcessedCount(recProvider.getRowProcessed());
  }

  private void processSaveSQLQuery(SaveXMLTableFromQueryDML dml) throws Exception {
    FileManager fmSave = ( (XMLSchema) conn.getSchema()).getFileManager(dml.
	getSavePath());
    XMLTable xmlSpecTable = (XMLTable) conn.getSchema().getStoreTable(conn.
	getFullTableName(dml.getTableSpecName()));
    SQLTable queryResult = getCatalog().executeQuery(dml.getSQL());
    XMLRecordProvider recProvider = new XMLRecordProvider(xmlSpecTable,
	queryResult, false);
    recProvider.saveTable(fmSave.getOutputStream(false));
    conn.getTableCatalog().setRowProcessedCount(recProvider.getRowProcessed());
  }

  protected void processCreateTableUsingSpec(CreateXMLTableUsingSpecDML dml) throws
      Exception {
    if (!conn.isCaching() && !conn.isTransactSubMode())
      throw new Exception("This submode of the driver (swapping mode + 'readOnlySubMode=true') doesn't support write operations");
    String tableName = dml.getPrimaryTableName();
    XMLTable xmlSpecTable = (XMLTable) conn.getSchema().getStoreTable(conn.
	getFullTableName(tableName));
    StoreFieldIF[] fields = xmlSpecTable.getFields();
    Columns cols = DefaultSQLTableAdapter.convertFields2Columns(fields);
    SQLTable t = new SQLTable(cols, SwapManager.FIXED_SWAP_TYPE);
    if (conn.getTableCatalog().getTable(tableName) != null)
      throw new Exception("The table '" + tableName + "' already exists");
    conn.getTableCatalog().registerTable(t, tableName);
    xmlSpecTable.create(fields);
    conn.getTableCatalog().setRowProcessedCount(0);
  }
*/
}
