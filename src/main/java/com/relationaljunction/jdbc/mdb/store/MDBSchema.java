package com.relationaljunction.jdbc.mdb.store;

import static com.relationaljunction.utils.OtherUtils.writeLogInfo;
import static com.relationaljunction.utils.OtherUtils.writeWarnInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.complex.Attachment;
import com.healthmarketscience.jackcess.complex.ComplexDataType;
import com.healthmarketscience.jackcess.complex.ComplexValueForeignKey;
import com.healthmarketscience.jackcess.complex.SingleValue;
import com.healthmarketscience.jackcess.util.OleBlob;
import com.relationaljunction.database.AbstractStoreSchema;
import com.relationaljunction.database.DefaultStoreField;
import com.relationaljunction.database.StoreDataType;
import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.index.IndexSchemaIF;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.database.io.FileCacheManager;
import com.relationaljunction.database.io.FileManager;
import com.relationaljunction.database.io.LocalFileManager;
import com.relationaljunction.database.view.ViewSchemaIF;
import com.relationaljunction.jdbc.common.h2.CommonConnection2;
import com.relationaljunction.jdbc.common.h2.CommonDriver2;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.StringUtils;
import com.relationaljunction.utils.UnexpectedException;
import com.relationaljunction.utils.io.WatchdogFileManagerLock;

public class MDBSchema extends AbstractStoreSchema {
   private final Logger log = LoggerFactory.getLogger("MDBSchema");

   final public static String DEFAULT_CHARSET = null;
   final public static String DEFAULT_EXTENSION = ".mdb";
   final public static String IGNORE_CASE = "ignoreCase";
   final public static String READ_ONLY = "readOnly";
   final public static String CREATE = "create";
   final public static String FORMAT_STRING = "format";
   final static String FORMAT_1997 = "access1997";
   final static String FORMAT_2000 = "access2000";
   final static String FORMAT_2003 = "access2003";
   final static String FORMAT_2007 = "access2007";
   final static String FORMAT_2010 = "access2010";
   final public static String DEFAULT_FORMAT_STRING = "access2000";
   final public static Database.FileFormat DEFAULT_FORMAT = Database.FileFormat.
           V2000;

   final public static int DOUBLE_LIMIT = 15;
   final public static int DEFAULT_VARCHAR_LENGTH = 40;
   final public static int DEFAULT_DECIMAL_PRECISION = 20;
   final public static int DEFAULT_DECIMAL_DECIMAL_SCALE = 2;

   public static int databaseLockCheckPeriod = 200;
   public static int databaseLockTimeOut = 50000;

   private Database db = null;

   FileManager fm = null;
   String path = null;
   String charset = DEFAULT_CHARSET;
   String extension = DEFAULT_EXTENSION;
   boolean preserveColumnNames = CommonConnection2.DEFAULT_PRESERVE_COLUMN_NAMES;
   boolean dbCaching = CommonConnection2.DEFAULT_DB_CACHING;

   private ViewSchemaIF viewSchema = null;
   private IndexSchemaIF indexSchema = null;

   private final static String LOCK_CHECK_INTERVAL = "lockCheckInterval";
   private final static int DEFAULT_LOCK_CHECK_INTERVAL = 200;
   private final static String LOCK_DATABASE = "lockDatabase";
   private final static boolean DEFAULT_LOCK_DATABASE = false;

   private int lockCheckInterval = DEFAULT_LOCK_CHECK_INTERVAL;
   private boolean lockDatabase = DEFAULT_LOCK_DATABASE;
   private volatile WatchdogFileManagerLock fileLockFm;

   /**
    * temp path*
    */
   String tempPath = CommonDriver2.DEFAULT_TEMP_PATH;
   /**
    * web parameter*
    */
   String useWebParam = null;
   /**
    * autoSync *
    */
   boolean autoSync = false;
   /**
    * readOnly *
    */
   private boolean readOnly = false;
   /**
    * create *
    */
   private boolean create = false;
   /**
    * format *
    */
   private Database.FileFormat format = DEFAULT_FORMAT;

   public MDBSchema(Properties props) throws SQLException {
      super(props);

      // load properties required only for the schema
      this.path = props.getProperty(CommonDriver2.PATH);

//      fileLockFm = new LockFile(new File(this.path + ".lock"));

/*
    // file extension
    if (props.getProperty(CommonDriver.FILE_EXTENSION) != null) {
      extension = props.getProperty(CommonDriver.FILE_EXTENSION).trim();
      if (extension.trim().equals("."))
        extension = "";
      else if (extension.indexOf(".") == -1 &&
               extension.trim().length() != 0)
        extension = "." + extension;
    }
*/
      // charset
      if (props.getProperty(CommonDriver2.CHARSET) != null) {
         this.charset = props.getProperty(CommonDriver2.CHARSET);
      }
      // log path
      if (props.getProperty(CommonDriver2.LOG_PATH) != null) {
         try {
            java.sql.DriverManager.setLogWriter(new PrintWriter(new
                    FileOutputStream(props.getProperty(CommonDriver2.LOG_PATH), false)));
         } catch (Exception ex) {
            //        ex.printStackTrace();
         }
      }
      // temp path
      if (props.getProperty(CommonDriver2.TEMP_PATH) != null) {
         tempPath = props.getProperty(CommonDriver2.TEMP_PATH);
         if (!new File(tempPath).exists())
            throw new SQLException("Temporary path '" + tempPath +
                    "' doesn't exist");
      }
      // use specified web parameter as a table name for a server page
      if (props.getProperty(CommonDriver2.USE_WEB_PARAM) != null) {
         useWebParam = props.getProperty(CommonDriver2.USE_WEB_PARAM);
      }
      // ignoreCase
//    if (props.getProperty(IGNORE_CASE) != null) {
//      com.sqlEngineAPI.Value.setIgnoreCase(Boolean.valueOf(props.getProperty(
//	  IGNORE_CASE)).booleanValue());
//    }
      if (props.getProperty(READ_ONLY) != null) {
         readOnly = Boolean.valueOf(props.getProperty(READ_ONLY));
      }
      if (props.getProperty(CREATE) != null) {
         create = Boolean.valueOf(props.getProperty(CREATE));
      }
      // access format
      if (props.getProperty(FORMAT_STRING) != null) {
         String propFormat = props.getProperty(FORMAT_STRING);
         if (propFormat.equalsIgnoreCase(FORMAT_1997))
            format = Database.FileFormat.V1997;
         else if (propFormat.equalsIgnoreCase(FORMAT_2000))
            format = Database.FileFormat.V2000;
         else if (propFormat.equalsIgnoreCase(FORMAT_2003))
            format = Database.FileFormat.V2003;
         else if (propFormat.equalsIgnoreCase(FORMAT_2007))
            format = Database.FileFormat.V2007;
         else if (propFormat.equalsIgnoreCase(FORMAT_2010))
            format = Database.FileFormat.V2010;
      }
      // dbCaching
      if (props.getProperty(CommonConnection2.DB_CACHING) != null) {
         dbCaching = Boolean.valueOf(props.getProperty(CommonConnection2.DB_CACHING));
      }
      // preserveColumnNames
      if (props.getProperty(CommonConnection2.PRESERVE_COLUMN_NAMES) != null) {
         preserveColumnNames = Boolean.valueOf(props.getProperty(CommonConnection2.PRESERVE_COLUMN_NAMES));
      }
      // lock database
      if (props.getProperty(LOCK_DATABASE) != null) {
         lockDatabase = Boolean.valueOf(props.getProperty(LOCK_DATABASE));
      }
      // max memo size
      if (props.getProperty(LOCK_CHECK_INTERVAL) != null) {
         lockCheckInterval = Integer.parseInt(props.getProperty(LOCK_CHECK_INTERVAL));
      }

      init();
   }

   private void init() throws SQLException {
      // init an MDB Database
      if (lockDatabase) {
         try {
            lockDatabase();
         } catch (Exception e) {
            throw new SQLException("Can't lock an MDB file: '" + path +
                    "'. [Jackcess] " + e.getMessage(), e);
         }
      }

      try {
         // init a FileManager. If it has remote protocol, it should be cached further,
         // because Jackcess supports only local operations with java.io.File
         fm = FileManager.buildFileManager(null, path, tempPath, false);

         if (create) {
            // create a new MDB database

            if (!(fm instanceof LocalFileManager))
               // make FileManager cached
               fm = new FileCacheManager(fm, tempPath, true, true);

            // create a default Database instance used for writing and initializing views
            db = createDatabase();
         } else if (fm.exists()) {
            // open existing MDB database

            if (!(fm instanceof LocalFileManager))
               // make FileManager cached
               fm = new FileCacheManager(fm, tempPath, false, true);

            // open an existing MDB database
            db = openDatabase();
         } else
            throw new SQLException("Can't find the file '" + fm.getPath() +
                    "'. To create a new MDB file set the driver property 'create' to 'true'.");
      } catch (Exception ex) {
         throw new SQLException("Can't open an MDB file: '" + path +
                 "'. [Jackcess] " + ex.getMessage(), ex);
      }

      // init view schema
      viewSchema = new MDBViewSchema(this);
      // init index schema
      indexSchema = new MDBIndexSchema(this);

      setModificationDate(fm.getModificationTime());
   }

   /*
    * returns a default Database instance used for writinga and initializing views.
    * According Jackcess doc: Database instances do not implement any "transactional" support, and
    * therefore concurrent editing of the same database file by multiple Database
    * instances (or with outside programs such as MS Access) <i>will generally
    * result in database file corruption</i>.
    */
   Database getDatabase() {
      return db;
   }

   /**
    * open an another instance of Database to optimize concurrent reading operations
    * According to Jackcess doc: Database instances (and all the related objects) are <i>not</i>
    * thread-safe.  However, separate Database instances (and their respective
    * objects) can be used by separate threads without a problem.
    *
    * @return
    * @throws IOException
    */
   Database openDatabase() throws IOException {
      DatabaseBuilder databaseBuilder = new DatabaseBuilder();
      databaseBuilder.setAutoSync(autoSync);
      databaseBuilder.setReadOnly(readOnly);
      databaseBuilder.setFile(fm.getFile());

      // set charset
      if (charset != null) {
         databaseBuilder.setCharset(java.nio.charset.Charset.forName(this.charset));
      }

      Database db = databaseBuilder.open();

      // turn foreign keys constraints on
      db.setEnforceForeignKeys(false);
      // set autonumber counter strategy
      db.setAutonumberCounterEnabled(!dbCaching);

      return db;
   }

   /**
    * create a new MS Access database (i.e. new file).
    * The "format" property is used to set in which MS Access format this file should be created.
    *
    * @return
    * @throws IOException
    */
   Database createDatabase() throws IOException {
      DatabaseBuilder databaseBuilder = new DatabaseBuilder();
      databaseBuilder.setAutoSync(autoSync);
      databaseBuilder.setReadOnly(readOnly);
      databaseBuilder.setFileFormat(format);
      databaseBuilder.setFile(fm.getFile());

      // set charset
      if (charset != null) {
         databaseBuilder.setCharset(java.nio.charset.Charset.forName(this.charset));
      }

      Database db = databaseBuilder.create();

      // turn foreign keys constraints on
      db.setEnforceForeignKeys(false);
      // set autonumber counter strategy
      db.setAutonumberCounterEnabled(!dbCaching);

      return db;
   }

   /**
    * MS Access tables have no file extensions, like .CSV or .DBF tables
    *
    * @return
    */
   public String getDefaultFileExtension() {
      return "";
   }

   public void reload() throws StoreException {
      try {
         writeLogInfo(log,
                 "MDBSchema is being reloaded");
         close();
         storeTablesHash = new Hashtable<String, StoreTableIF>();
         init();
         writeLogInfo(log,
                 "MDBSchema is succesfully reloaded");
      } catch (Exception ex) {
         writeWarnInfo(log, "Error while reloading MDBSchema", true);
         throw new StoreException(ex);
      }
   }

   public void close() {
      try {
         if (db != null)
            db.close();
         if (storeTablesHash != null) {
            storeTablesHash.clear();
         }
         if (fm != null) {
            fm.close();
         }
         if (fileLockFm != null) {
            unlockDatabase();
         }
      } catch (Exception ex) {
         ex.printStackTrace();
      }

      db = null;
      fm = null;
      storeTablesHash = null;
      viewSchema = null;
      indexSchema = null;
      fileLockFm = null;
   }

   public boolean isModificationDateChanged() throws StoreException {
      return !fm.getModificationTime().equals(getModificationDate());
   }

   public synchronized void createStoreTable(String storeName, StoreFieldIF[] fields, IndexTableIF[] indexTables,
                                             Properties props) throws StoreException {
      getStoreTable(storeName).create(fields, indexTables);
   }

   public synchronized void deleteStoreTable(String storeName) throws StoreException {
      try {
         Table systemTable = db.getSystemTable("MSysObjects");

         boolean found = false;

         for (int i = 0; i < systemTable.getRowCount(); i++) {
            Row row = systemTable.getNextRow();

            if (row.get("Name") != null) {
               String str = (String) row.get("Name");
               if (str.equalsIgnoreCase(storeName)) {
                  systemTable.deleteRow(row);
                  found = true;
                  break;
               }
            }
         }

         if (!found)
            throw new StoreException("Can't find a table '" + storeName +
                    "'");

         db.flush();
         db.close();

         // reopen an MDB database to apply changes
         db = openDatabase();
      } catch (IOException ex) {
         throw new StoreException("Can't drop a table '" + storeName +
                 "'. Error was: " + ex.getMessage(), ex);
      }

      // delete an StoreTableIF instance
      deleteStoreTableFromCache(storeName);

//    throw new StoreException("DROP TABLE is not supported!");
   }

   public IndexSchemaIF getIndexSchema() {
      return indexSchema;
   }

   public boolean supportsIndexes() {
      return true;
   }

   public ViewSchemaIF getViewSchema() {
      return viewSchema;
   }

   protected StoreTableIF initStoreTable(String storeName) {
      return new MDBTable(storeName, this);
   }

   public StoreTableIF[] getStoreTables(String templateName) throws
           StoreException {
      Set<String> tableNames;
      try {
         tableNames = db.getTableNames();
      } catch (IOException ex) {
         throw new StoreException(
                 "Unexpected error in MDBSchema.getStoreTables()", ex);
      }

      Vector<String> filteredTables = new Vector<String>();

      for (String tableName : tableNames) {
         if (templateName == null ||
                 templateName.trim().isEmpty() ||
                 StringUtils.isLike(tableName.toLowerCase(), templateName.toLowerCase(),
                         '%', '_'))
            filteredTables.add(tableName);
      }

      StoreTableIF[] result = new StoreTableIF[filteredTables.size()];
      for (int i = 0; i < filteredTables.size(); i++)
         result[i] = getStoreTable(filteredTables.get(i));

      return result;
   }

   // MDB -> SQLEngine
   public StoreFieldIF getStoreField(Column mdbCol) {
      // default type of a column is VARCHAR
      DefaultStoreField storeField = new DefaultStoreField(mdbCol.getName());

      switch (mdbCol.getType()) {
         case BYTE:
          case LONG:
          case INT: {
            storeField.setType(StoreDataType.INTEGER);
            break;
         }
          case FLOAT: {
            storeField.setType(StoreDataType.FLOAT);
            break;
         }
         case DOUBLE:
          case MONEY: {
            storeField.setType(StoreDataType.DOUBLE);
            break;
         }
         case NUMERIC: {
            int precision = mdbCol.getPrecision();

            if (precision > DOUBLE_LIMIT) {
               storeField.setType(StoreDataType.NUMERIC);
               storeField.setLength(precision);
               storeField.setDecimalCount(mdbCol.getScale());
            } else
               storeField.setType(StoreDataType.DOUBLE);

            break;
         }
          case BOOLEAN: {
            storeField.setType(StoreDataType.BOOLEAN);
            break;
         }
         case SHORT_DATE_TIME: {
            storeField.setType(StoreDataType.TIMESTAMP);
            break;
         }
         case TEXT: {
            storeField.setType(StoreDataType.VARCHAR);
            storeField.setLength(mdbCol.getLength() / 2);
            break;
         }
         case MEMO: {
            // size?
            storeField.setType(StoreDataType.VARCHAR);
            break;
         }
         case OLE: {
//      storeField.setType(java.sql.Types.JAVA_OBJECT);
            // now it is converted to String with length = 9
            storeField.setLength(9);
            break;
         }
      }
      return storeField;
   }

   // SQLEngine -> MDB
   public ColumnBuilder getMDBColumn(StoreFieldIF storeField) throws StoreException {
      ColumnBuilder columnBuilder = new ColumnBuilder(storeField.getName());

//      Column mdbCol = new Column();
      int length = storeField.getLength();
      int scale = storeField.getDecimalCount();
//      mdbCol.setName(storeField.getName());

      try {
         // NUMERIC || DOUBLE with length > 0 => native DECIMAL(length,decimalCount)
         if (storeField.getType() == StoreDataType.NUMERIC ||
                 (storeField.getType() == StoreDataType.DOUBLE && length > 0)) {
            if (length <= 0) length = DEFAULT_DECIMAL_PRECISION;
            if (scale <= 0) scale = DEFAULT_DECIMAL_DECIMAL_SCALE;
            columnBuilder.setType(DataType.NUMERIC);
            columnBuilder.setPrecision((byte) length);
            columnBuilder.setScale((byte) scale);
         }
         // other types
         else
            columnBuilder.setSQLType(storeField.getType().getJdbcType());
      } catch (Exception ex) {
         throw new StoreException(ex);
      }

      if (storeField.getType() == StoreDataType.VARCHAR) {
         // default size
         if (length <= 0) columnBuilder.setLength((short) (DEFAULT_VARCHAR_LENGTH * 2));
         else
            columnBuilder.setLength((short) (length * 2));
      }

      return columnBuilder;
   }

   // SQLEngine -> MDB
   Object getMDBObject(Column mdbCol, Object engineValue) {
      if (engineValue == null)
         return null;

      switch (mdbCol.getType()) {
         // better to pass a string value to Jackcess for NUMERIC and MONEY types
         // because Jackcess uses "new BigDecimal(Double d)" constructor
         // that may have unpredictable results
//         case BYTE:
//         case INT:
//         case LONG: {
         // for indexes to support converting String values in SQL queries
//            if (engineValue instanceof String)
//               return Integer.valueOf((String) engineValue);
//
//            break;
//         }
         case NUMERIC: {
            // Double -> Numeric (Money)
            if (engineValue instanceof BigDecimal)
               return engineValue;
            else
               return engineValue.toString();
         }
         case OLE: {
            Blob objectBlob = (Blob) engineValue;
            OleBlob oleBlob;

            try {
               oleBlob = new OleBlob.Builder().
                       setSimplePackageFileName("Noname").
                       setSimplePackageFilePath("Path_not_specified")
                       .setSimplePackageStream(objectBlob.getBinaryStream(), objectBlob.length())
                       .toBlob();
            } catch (Exception e) {
               throw new UnexpectedException("Error while " +
                       "retrieving a Blob stream to insert into MS Access file", e);
            }

            return oleBlob;
         }
         case COMPLEX_TYPE: {
            // ATTACHMENT
            if (mdbCol.getComplexInfo().getType() == ComplexDataType.ATTACHMENT) {
               return Column.AUTO_NUMBER;
            }
         }
//         case MONEY: {
//            Double -> Numeric (Money)
//            if (engineValue instanceof BigDecimal)
//               return engineValue;
//            else
//               return engineValue.toString();
//         }
      }

      return engineValue;
   }

   // MDB -> SQLEngine
   Object getEngineObject(Column mdbCol, Object mdbValue) {
      if (mdbValue == null) {
         return null;
      }

      switch (mdbCol.getType()) {
         case NUMERIC: {
            // Numeric (Money) -> Double
            BigDecimal bg = (BigDecimal) mdbValue;
            int precision = mdbCol.getPrecision();

            if (precision > DOUBLE_LIMIT)
               return bg;
            else
               return bg.doubleValue();

            //      BigDecimal bg = (BigDecimal) mdbValue;
            //      return new Double(bg.doubleValue());
         }
//         case MONEY: {
         // Numeric (Money) -> Double
//            BigDecimal bg = (BigDecimal) mdbValue;
//            return bg.doubleValue();
//         }
         case SHORT_DATE_TIME: {
            return mdbValue;
         }
         case OLE: {
            try {
               OleBlob oleBlob = OleBlob.Builder.fromInternalData((byte[]) mdbValue);
               return new MDBBlob(oleBlob);
            } catch (Exception e) {
               e.printStackTrace();
               log.warn("Error while getting the content for OLE column '" +
                       mdbCol.getName() + "'", e);
               return null;
            }
//            return "OLE VALUE";
         }
         case COMPLEX_TYPE: {
            // ATTACHMENT
            if (mdbCol.getComplexInfo().getType() == ComplexDataType.ATTACHMENT) {
               try {
                  MDBAttachmentContainer attachmentContainer = new MDBAttachmentContainer();

                  ComplexValueForeignKey complexValue = (ComplexValueForeignKey) mdbValue;
                  for (Attachment mdbAttachment : complexValue.getAttachments()) {
                     MDBAttachmentContainer.Attachment attachment =
                             new MDBAttachmentContainer.Attachment(mdbAttachment.getFileData());
                     attachment.setFileName(mdbAttachment.getFileName());
                     attachment.setFileFlags(mdbAttachment.getFileFlags());
                     attachment.setFileType(mdbAttachment.getFileType());
                     attachment.setFileTimeStamp(mdbAttachment.getFileTimeStamp());
                     attachment.setFileUrl(mdbAttachment.getFileUrl());
                     attachmentContainer.addAttachment(attachment);
                  }

                  return attachmentContainer;
               } catch (Exception e) {
                  e.printStackTrace();
                  log.warn("Error while getting the content for Attachment column '" +
                          mdbCol.getName() + "'", e);
                  return null;
               }
            } // MULTI VALUE
            else if (mdbCol.getComplexInfo().getType() == ComplexDataType.MULTI_VALUE) {
               try {
                  ComplexValueForeignKey complexValue = (ComplexValueForeignKey) mdbValue;
                  List<SingleValue> multiValueList = complexValue.getMultiValues();

                  if (!multiValueList.isEmpty()) {
                     StringBuilder result = new StringBuilder();

                     for (int i = 0; i < multiValueList.size(); i++) {
                        result.append(multiValueList.get(i).get());
                        if (i < multiValueList.size() - 1) result.append("; ");
                     }

                     return result.toString();
                  } else {
                     return null;
                  }
               } catch (IOException e) {
                  e.printStackTrace();
                  log.warn("Error while getting the content for Multivalue column '" +
                          mdbCol.getName() + "'", e);
                  return null;
               }
            }
         }
      }
      // other data types: DOUBLE, MONEY, TEXT
      return mdbValue;
   }

   @Override
   public void lockDatabase() throws Exception {
      // DCL init of a database lock
      if (fileLockFm == null) {
         synchronized (this) {
            // init a path to a lock file (not create it)
            if (fileLockFm == null)
               fileLockFm = new WatchdogFileManagerLock(
                       FileManager.buildFileManager(null, path + ".lock", tempPath, false),
                       lockCheckInterval);
         }
      }

      fileLockFm.lock(databaseLockTimeOut);

      OtherUtils.writeLogInfo(log, "Lock file: ", path, " is locked");
   }

   @Override
   public void unlockDatabase() throws Exception {
      fileLockFm.unlock();
      OtherUtils.writeLogInfo(log, "Lock file: ", path, " is unlocked");
   }
}
