package com.relationaljunction.jdbc.dbf.h2;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.dbf.VFPFileReader;
import com.relationaljunction.database.index.IndexSchemaIF;
import com.relationaljunction.database.io.FileManager;
import com.relationaljunction.jdbc.dbf.store.DBCTable;
import com.relationaljunction.jdbc.dbf.store.DBFTable;
import com.relationaljunction.utils.StringUtils;

public class DBCSchema extends DBFSchema2 {
   private final Logger log = LoggerFactory.getLogger("DBCSchema");
   private static final Pattern FILE_NAME_PATTERN = Pattern.compile("([A-Za-z0-9-_ ]+?)\\.[A-Za-z0-9- ]+");

   private FileManager fmDBC;
   private Connection memConn;
   private PreparedStatement pstGetTableColumns, pstGetTableId, pstGetAllTables;

   public DBCSchema(Properties globalProps) throws StoreException {
      super(globalProps);

      try {
         createDBCTableInH2();
      } catch (Exception ex) {
         throw new StoreException(ex);
      }
   }

   protected void buildManagers() throws StoreException {
      try {
         // create FileManager instance by using dbcPath
         this.fmDBC = buildDBCFileManager(path);

         // get DirectoryManager
         dir = fmDBC.getDir();
      } catch (Exception ex) {
//            ex.printStackTrace();
         throw new StoreException(ex.getMessage());
      }
   }

   private FileManager buildDBCFileManager(String dbcPath) throws
           Exception {
      FileManager fm = FileManager.buildFileManager(null, dbcPath, tempPath, true);

      if (!fm.exists())
         throw new Exception("The path to the DBC file '" + dbcPath + "' doesn't exist");

      return fm;
   }

   private void createDBCTableInH2() throws Exception {
      try {
         this.memConn = DriverManager.getConnection("jdbc:h2_custom:mem:;IGNORECASE=true");
         Statement st = memConn.createStatement();

         st.execute("CREATE TABLE DBC_SCHEMA(objectid INTEGER, parentid INTEGER, " +
                 "objecttype VARCHAR(10), objectname VARCHAR(128), path VARCHAR, " +
                 "PRIMARY KEY(objectid))");
         st.execute("CREATE INDEX rj_dbc_schema_parentid ON DBC_SCHEMA(parentid)");
         st.execute("CREATE INDEX rj_dbc_schema_objecttype ON DBC_SCHEMA(objecttype)");
         st.close();

         PreparedStatement pstInsert = memConn.prepareStatement(
                 "INSERT INTO DBC_SCHEMA VALUES(?, ?, ?, ?, ?)");

         // loads DCT memo file if exists
         String dbcMemoFile = StringUtils.
                 getFileNameWithoutExtension(new File(path).getName()) + ".dct";
         try {
            dir.getFileManager(dbcMemoFile);
         } catch (Exception e) {
            log.debug("DBCSchema: DCT file '" + dbcMemoFile + "' does not exist");
         }

         VFPFileReader reader = new VFPFileReader(fmDBC.getFile(), ".dct", charset);

         Object[] objs;
         while ((objs = reader.nextRecord()) != null) {
            Integer objectId = (Integer) objs[0];
            Integer parentId = (Integer) objs[1];
            String objectType = (objs[2] == null ? null : objs[2].toString().trim());
            String objectName = (objs[3] == null ? null : objs[3].toString().trim());
            String property = (objs[4] == null ? null : objs[4].toString());
            String path = null;

            if (objectType.equalsIgnoreCase("TABLE") && property != null) {
               Matcher m = FILE_NAME_PATTERN.matcher(property);
               if (m.find()) {
                  path = m.group();
               }
            }

            pstInsert.setInt(1, objectId);
            pstInsert.setInt(2, parentId);
            pstInsert.setString(3, objectType);
            pstInsert.setString(4, objectName);
            pstInsert.setString(5, path);

            pstInsert.addBatch();
         }
         pstInsert.executeBatch();
         reader.close();
         pstInsert.close();

//         try {
//            ResultSet rsData = memConn.createStatement().executeQuery("SELECT * FROM DBC_SCHEMA");
//            TestUtils.printColumnsOut(rsData, System.out);
//            TestUtils.printResultSetOut(rsData, System.out);
//         } catch (SQLException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//         }

         // prepare statements that will be used in the future
         pstGetTableId = memConn.prepareStatement("SELECT objectid, objectname, path FROM DBC_SCHEMA " +
                 "WHERE objecttype = 'Table' AND (objectname = ? OR path = ?)");
         pstGetTableColumns = memConn.prepareStatement("SELECT objectname FROM DBC_SCHEMA " +
                 "WHERE parentid = ? AND objecttype = 'Field'");
         pstGetAllTables = memConn.prepareStatement("SELECT objectid, objectname, path  FROM DBC_SCHEMA " +
                 "WHERE objecttype = 'Table'");
      } catch (Exception ex) {
         throw new Exception("Error while loading data from the DBC file '" + path +
                 "' to H2 database. Error was: " + ex.getMessage(), ex);
      }
   }

   protected StoreTableIF initStoreTable(String storeName) throws StoreException {
      // storeName is passed with a file extension, e.g. test.dbf
      String storeNameWithoutExtension = StringUtils.getFileNameWithoutExtension(storeName);

      List<String> newColumnNames = new Vector<String>();

      try {
         pstGetTableId.setString(1, storeNameWithoutExtension);
         pstGetTableId.setString(2, storeName);
         ResultSet rs = pstGetTableId.executeQuery();
         if (!rs.next()) {
//            throw new StoreException("Can't find '" + tableName +
//                    "' description in the DBC file");

            String logMessage = "Can't find '" + storeName + "' table description in the DBC file. " +
                    "SQL Table was '" + storeName + "'. Redirecting to external DBF files.";
            log.warn(logMessage);
            System.err.println(logMessage);
            return new DBFTable(storeName, this, dir);
         }

         int tableId = rs.getInt("objectid");
         String dbcTableName = (rs.getString("path") != null ?
                 rs.getString("path") :
                 rs.getString("objectname") + extension);

         rs.close();

         pstGetTableColumns.setInt(1, tableId);
         rs = pstGetTableColumns.executeQuery();
         while (rs.next()) {
            newColumnNames.add(rs.getString("objectname"));
         }

         rs.close();
         if (newColumnNames.isEmpty()) throw new StoreException("Can't find columns " +
                 "description in the DBC file for the table '" + storeName + "'");

         return new DBCTable(dbcTableName, this, dir, newColumnNames);
      } catch (Exception e) {
         throw new StoreException("[DBCSchema] " + e.getMessage(), e);
      }
   }

   public StoreTableIF[] getStoreTables(String templateName) throws StoreException {
      List<StoreTableIF> storeTables = new Vector<StoreTableIF>();

      try {
         // get all tables
         ResultSet rsTables = pstGetAllTables.executeQuery();

         while (rsTables.next()) {
            // fetch a table
            int tableId = rsTables.getInt("objectid");
            String dbcTableName = (rsTables.getString("path") != null ?
                    rsTables.getString("path") : rsTables.getString("objectname") + extension);

            List<String> newColumnNames = new Vector<String>();

            // get columns for the table
            pstGetTableColumns.setInt(1, tableId);
            ResultSet rsColumns = pstGetTableColumns.executeQuery();
            while (rsColumns.next()) {
               newColumnNames.add(rsColumns.getString("objectname"));
            }

            storeTables.add(new DBCTable(dbcTableName, this, fmDBC.getDir(), newColumnNames));
         }
      } catch (SQLException e) {
         throw new StoreException(e);
      }

      return storeTables.toArray(new StoreTableIF[0]);
   }

   public boolean supportsIndexes() {
      return false;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public IndexSchemaIF getIndexSchema() {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public void close() {
      try {
         pstGetTableColumns.close();
         pstGetTableId.close();
         pstGetAllTables.close();
         memConn.close();
         fmDBC.close();
         dir.close();
      } catch (Exception e) {
         e.printStackTrace();
      }

      memConn = null;
      pstGetTableColumns = null;
      pstGetTableId = null;
      pstGetAllTables = null;
      fmDBC = null;
      dir = null;
   }
}
