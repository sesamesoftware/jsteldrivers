package com.relationaljunction.jdbc.dbf.store;

import java.io.*;
import java.util.*;

import org.xBaseJ.*;

import com.relationaljunction.database.*;
import com.relationaljunction.database.dbf.DBFBase;
import com.relationaljunction.database.index.*;
import com.relationaljunction.database.io.*;
import com.relationaljunction.database.view.ViewSchemaIF;
import com.relationaljunction.jdbc.common.h2.CommonDriver2;
import com.relationaljunction.jdbc.dbf.*;
import org.xBaseJ.DBFTypes;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class DBFSchema extends AbstractStoreSchema {
   final static char DEFAULT_PAD_CHAR = ' ';
   public final static boolean DEFAULT_PACK_DBF = false;
   public final static String PACK_DBF = "packDBF";
   private final static String PAD_CHAR = "padChar";
   public final static String FORMAT = "format";
   public final static DBFTypes DEFAULT_FORMAT = DBFTypes.DBASEIII;
   private final static String MEMO_EXTENSION = "memoExtension";
   private final static String MEMO_BLOCK_SIZE = "memoBlockSize";
   private final static String DBASEIII_FORMAT = "DBASEIII";
   public final static String DEFAULT_FORMAT_STRING = DBASEIII_FORMAT;
   private final static String DBASEIV_FORMAT = "DBASEIV";
   private final static String VFP_FORMAT = "VFP";
   private final static String USE_TAG = "useTag";
   public static final String TRIM_BLANKS = "trimBlanks";
   public final static boolean DEFAULT_TRIM_BLANKS = true;
   public static final String EMPTY_STRING_AS_NULL = "emptyStringAsNull";
   public final static boolean DEFAULT_EMPTY_STRING_AS_NULL = true;
   public final static String IGNORE_CASE = "ignoreCase";
   /**
    * scan deleted records while updating and deleting records*
    */
   public static final String SCAN_DELETED_RECORDS = "scanDeletedRecords";
   public static final String NEVER_SCAN_DELETED_RECORDS_STRING = "never";
   public static final int NEVER_SCAN_DELETED_RECORDS = 0;
   public static final String ALWAYS_SCAN_DELETED_RECORDS_STRING = "always";
   public static final int ALWAYS_SCAN_DELETED_RECORDS = 1;
   public static final String AUTO_SCAN_DELETED_RECORDS_STRING = "auto";
   public static final int AUTO_SCAN_DELETED_RECORDS = 2;
   public static final int DEFAULT_SCAN_DELETED_RECORDS =
           AUTO_SCAN_DELETED_RECORDS;
   public static final String USE_BIG_DECIMAL_TYPE = "useBigDecimalType";
   public final static boolean DEFAULT_USE_BIG_DECIMAL_TYPE = false;

   /**
    * use deleted records while reading records*
    */
   public static final String USE_DELETED_RECORDS = "useDeletedRecords";
   public static final String NO_USE_DELETED_RECORDS_STRING = "no";
   public static final int NO_USE_DELETED_RECORDS = 0;
   public static final int DEFAULT_USE_DELETED_RECORDS = NO_USE_DELETED_RECORDS;
   public static final String ONLY_USE_DELETED_RECORDS_STRING = "only";
   public static final int ONLY_USE_DELETED_RECORDS = 1;
   public static final String ADD_USE_DELETED_RECORDS_STRING = "add";
   public static final int ADD_USE_DELETED_RECORDS = 2;

   // from version 5.1
   public static final String DBF_CODEPAGE = "dbfCodepage";
   public static final byte DEFAULT_DBF_CODEPAGE = 3;
   private final static String IGNORE_MEMO_FILE = "ignoreMemoFile";
   public static final String NEVER_IGNORE_MEMO_FILE = "never";
   public static final String ALWAYS_IGNORE_MEMO_FILE = "always";
   public static final String IF_NOT_EXIST_IGNORE_MEMO_FILE = "if_not_exist";
   public static final String DEFAULT_IGNORE_MEMO_FILE = NEVER_IGNORE_MEMO_FILE;
   public static final boolean DEFAULT_VFP_MAP_TO_DATE_TYPE = false;
   public static final String VFP_MAP_TO_DATE_TYPE = "vfpMapToDateType";
   public static final boolean DEFAULT_CHECK_COLUMN_SIZE = false;
   public static final String CHECK_COLUMN_SIZE = "checkColumnSize";
   public final static String READ_ONLY = "readOnly";
   public final static boolean DEFAULT_READ_ONLY = false;

   // from version 5.3
   public final static boolean DEFAULT_GET_MEMO_AS_BLOB = false;
   public static final String GET_MEMO_AS_BLOB = "getMemoAsBlob";
   public static final int DEFAULT_MAX_MEMO_SIZE = 4 * 1024 * 1024; // 4 MB
   public static final String MAX_MEMO_SIZE = "maxMemoSize";

   public static final String WRITE_EOF = "writeEOF";
   public static final String DEFAULT_WRITE_EOF = DBFBase.WRITE_EOF_ON_RECORDS_NUMBER;


   protected String path = null;
   protected String charset = DBFDriver2.DEFAULT_CHARSET;
   protected DirectoryManager dir = null;
   protected String extension = DBFDriver2.DEFAULT_EXTENSION;

   char padChar = DEFAULT_PAD_CHAR;
   byte dbfCodepage = DEFAULT_DBF_CODEPAGE;
   String memoExtension = null;
   short memoBlockSize = 512;
   /**
    * DBF file format *
    */
   DBFTypes format = DEFAULT_FORMAT;
   String ignoreMemoFile = DEFAULT_IGNORE_MEMO_FILE;
   boolean readOnly = DEFAULT_READ_ONLY;

   // locks
   // lockEnabled for XBaseJ only. deprecated?
   private final boolean lockEnabled = false;
   // new property for locks
   boolean lockFiles = false;

//   String lock = CommonDriver.DEFAULT_LOCK;
//   int lockTimeout = CommonDriver.DEFAULT_LOCK_TIMEOUT;
//   int lockCheckTime = CommonDriver.DEFAULT_LOCK_CHECK_TIME;

   String useTag = null;
   /**
    * pack DBF *
    */
   boolean packDBF = DEFAULT_PACK_DBF;
   /**
    * temp path*
    */
   protected String tempPath = null;
   /**
    * web parameter*
    */
   String useWebParam = null;
   /**
    * trim blanks*
    */
   boolean trimBlanks = DEFAULT_TRIM_BLANKS;
   /**
    * empty string is treated as Null*
    */
   boolean emptyStringAsNull = DEFAULT_EMPTY_STRING_AS_NULL;
   /**
    * use deleted records while reading records*
    */
   int useDeletedRecords = DEFAULT_USE_DELETED_RECORDS;
   /**
    * scan deleted records while updating and deleting records*
    */
   int scanDeletedRecords = DEFAULT_SCAN_DELETED_RECORDS;
   /**
    * use big decimal type *
    */
   boolean useBigDecimalType = DEFAULT_USE_BIG_DECIMAL_TYPE;
   /**
    * map dates to DATE type for VFP *
    */
   boolean vfpMapToDateType = DEFAULT_USE_BIG_DECIMAL_TYPE;
   /**
    * check column size *
    */
   boolean checkColumnSize = DEFAULT_CHECK_COLUMN_SIZE;

   boolean getMemoAsBlob = DEFAULT_GET_MEMO_AS_BLOB;

   // maximum memo element size in bytes
   int maxMemoSizeInBytes = DEFAULT_MAX_MEMO_SIZE;

   // maximum memo element size in bytes
   String writeEOF = DEFAULT_WRITE_EOF;

   public DBFSchema(Properties props) throws StoreException {
      super(props);

      try {
         loadGlobalPropertiesInVars(props);
         buildManagers();
      } catch (Exception ex) {
         throw new StoreException("Error while initializing schema '" + path +
                 "': " + ex.getMessage(), ex);
      }
   }

   public String getDefaultFileExtension() {
      return DBFDriver2.DEFAULT_EXTENSION;
   }


   protected void buildManagers() throws StoreException {
      // init DirectoryManager
      try {
         dir = DirectoryManager.buildDirectoryManager(path, tempPath, useWebParam);

         if (!(dir instanceof LocalFileDirectoryManager) &&
                 !(dir instanceof FileCacheDirectoryManager)) {
            throw new StoreException("Error in DBFSchema: The URL '" +
                    dir.getPath() +
                    "' is not supported directly. Add the 'cache://' prefix before the URL");
         }
      } catch (Exception ex) {
//            ex.printStackTrace();
         throw new StoreException(ex);
      }
   }

   protected void loadGlobalPropertiesInVars(Properties props) throws StoreException {
      // load properties required only for the schema
      this.path = props.getProperty(CommonDriver2.PATH);

      // default values
      try {
         DBFBase.ignoreMemoFile = DBFBase.IGNORE_MEMO_FILE_NEVER;
         Util.setxBaseJProperty("ignoreMemoFile", "never");
         Util.setxBaseJProperty("memoFileExtension", "");
      } catch (IOException ex) {
      }

      // file extension
      if (props.getProperty(CommonDriver2.FILE_EXTENSION) != null) {
         extension = props.getProperty(CommonDriver2.FILE_EXTENSION).trim();
         if (extension.trim().equals("."))
            extension = "";
         else if (!extension.contains(".") &&
                 !extension.trim().isEmpty())
            extension = "." + extension;
      }
      // charset
      if (props.getProperty(CommonDriver2.CHARSET) != null) {
         this.charset = props.getProperty(CommonDriver2.CHARSET);
      }
      // dbf codepage
      if (props.getProperty(DBF_CODEPAGE) != null) {
         String dbfCodePageStr = props.getProperty(DBF_CODEPAGE);
         boolean isHexNumber = false;
         if (dbfCodePageStr.startsWith("0x")) {
            dbfCodePageStr = dbfCodePageStr.substring(2);
            isHexNumber = true;
         }
         if (dbfCodePageStr.endsWith("h") || dbfCodePageStr.endsWith("H")) {
            dbfCodePageStr = dbfCodePageStr.substring(0,
                    dbfCodePageStr.length() - 1);
            isHexNumber = true;
         }

         if (isHexNumber)
            this.dbfCodepage = Integer.valueOf(dbfCodePageStr, 16).byteValue();
         else
            this.dbfCodepage = Integer.valueOf((dbfCodePageStr)).byteValue();
      }
      // dbf format
      if (props.getProperty(FORMAT) != null) {
         String propFormat = props.getProperty(FORMAT);
         if (propFormat.equalsIgnoreCase(DBASEIII_FORMAT))
            this.format = DBFTypes.DBASEIII;
         else if (propFormat.equalsIgnoreCase(DBASEIV_FORMAT))
            this.format = DBFTypes.DBASEIV;
         else if (propFormat.equalsIgnoreCase(VFP_FORMAT))
            this.format = DBFTypes.VISUAL_FOXPRO;
      }
      // ignore memo file
      if (props.getProperty(IGNORE_MEMO_FILE) != null) {
         try {
            ignoreMemoFile = props.getProperty(IGNORE_MEMO_FILE);
            Util.setxBaseJProperty("ignoreMemoFile", ignoreMemoFile);
            if (ignoreMemoFile.equalsIgnoreCase(ALWAYS_IGNORE_MEMO_FILE))
               DBFBase.ignoreMemoFile = DBFBase.
                       IGNORE_MEMO_FILE_ALWAYS;
            else if (ignoreMemoFile.equalsIgnoreCase(IF_NOT_EXIST_IGNORE_MEMO_FILE))
               DBFBase.ignoreMemoFile = DBFBase.
                       IGNORE_MEMO_FILE_IF_NOT_EXIST;
            else
               DBFBase.ignoreMemoFile = DBFBase.IGNORE_MEMO_FILE_NEVER;
         } catch (IOException ex) {
           ex.printStackTrace();
         }
      }

      // read only mode
      if (props.getProperty(READ_ONLY) != null) {
         readOnly = Boolean.valueOf(props.getProperty(READ_ONLY));
      }

      // memo extension
      if (props.getProperty(MEMO_EXTENSION) != null) {
         memoExtension = props.getProperty(MEMO_EXTENSION);
         try {
            String memoExtensionWithoutPoint = "";
            if (memoExtension.startsWith("."))
               memoExtensionWithoutPoint = memoExtension.substring(1);
            Util.setxBaseJProperty("memoFileExtension", memoExtensionWithoutPoint);
         } catch (IOException ex) {
            ex.printStackTrace();
         }
      }
      if (props.getProperty(MEMO_BLOCK_SIZE) != null) {
         memoBlockSize = Short.parseShort(props.getProperty(MEMO_BLOCK_SIZE));
         if (memoBlockSize < 32) throw new StoreException(
                 "Memo block size must be greater than 32 bytes");
      }
      // use tag
      if (props.getProperty(USE_TAG) != null) {
         useTag = props.getProperty(USE_TAG);
      }
      // pad char
      if (props.getProperty(PAD_CHAR) != null) {
         String padCharStr = props.getProperty(PAD_CHAR);
         if (padCharStr.equals("\\0"))
            padChar = '\0';
         padChar = props.getProperty(PAD_CHAR).charAt(0);
      }
      // pack DBF
      if (props.getProperty(PACK_DBF) != null) {
         packDBF = Boolean.valueOf(props.getProperty(PACK_DBF));
      }
      // trim blanks
      if (props.getProperty(TRIM_BLANKS) != null) {
         trimBlanks = Boolean.valueOf(props.getProperty(
                 TRIM_BLANKS));
      }
      // empty string treated as null
      if (props.getProperty(EMPTY_STRING_AS_NULL) != null) {
         emptyStringAsNull = Boolean.valueOf(props.getProperty(
                 EMPTY_STRING_AS_NULL));
      }
      // get memo as BLOB
      if (props.getProperty(GET_MEMO_AS_BLOB) != null) {
         getMemoAsBlob = Boolean.valueOf(props.getProperty(
                 GET_MEMO_AS_BLOB));
      }
      // scan deleted records
      if (props.getProperty(SCAN_DELETED_RECORDS) != null) {
         String scanDeletedRecordsString = props.getProperty(SCAN_DELETED_RECORDS);
         if (scanDeletedRecordsString.equalsIgnoreCase(ALWAYS_SCAN_DELETED_RECORDS_STRING))
            scanDeletedRecords = ALWAYS_SCAN_DELETED_RECORDS;
         else if (scanDeletedRecordsString.equalsIgnoreCase(
                 NEVER_SCAN_DELETED_RECORDS_STRING))
            scanDeletedRecords = NEVER_SCAN_DELETED_RECORDS;
         else if (scanDeletedRecordsString.equalsIgnoreCase(
                 AUTO_SCAN_DELETED_RECORDS_STRING))
            scanDeletedRecords = AUTO_SCAN_DELETED_RECORDS;
      }
      // use deleted records
      if (props.getProperty(USE_DELETED_RECORDS) != null) {
         String useDeletedRecordsString = props.getProperty(USE_DELETED_RECORDS);
         if (useDeletedRecordsString.equalsIgnoreCase(NO_USE_DELETED_RECORDS_STRING))
            useDeletedRecords = NO_USE_DELETED_RECORDS;
         else if (useDeletedRecordsString.equalsIgnoreCase(
                 ONLY_USE_DELETED_RECORDS_STRING))
            useDeletedRecords = ONLY_USE_DELETED_RECORDS;
         else if (useDeletedRecordsString.equalsIgnoreCase(
                 ADD_USE_DELETED_RECORDS_STRING))
            useDeletedRecords = ADD_USE_DELETED_RECORDS;
      }
      // writing EOF
      if (props.getProperty(WRITE_EOF) != null) {
         writeEOF = props.getProperty(WRITE_EOF);
      }
      // log path
      if (props.getProperty(CommonDriver2.LOG_PATH) != null) {
         try {
            //        try {
            //          new FileOutputStream("c:/log1.txt");
            //        }
            //        catch (FileNotFoundException ex3) {
            //        }

            //        String filePath = props.getProperty(CommonDriver.LOG_PATH);
            //
            //	Properties logProps = new Properties();
            //	logProps.put("log4j.appender.STDOUT", "org.apache.log4j.ConsoleAppender");
            //	logProps.put("log4j.appender.STDOUT.layout", "org.apache.log4j.SimpleLayout");
            //
            //	logProps.put("log4j.appender.RJ_FILE", "org.apache.log4j.FileAppender");
            //	logProps.put("log4j.appender.RJ_FILE.file", filePath);
            //	logProps.put("log4j.appender.RJ_FILE.layout", "org.apache.log4j.PatternLayout");
            //	logProps.put("log4j.appender.RJ_FILE.layout.conversionPattern",
            //		     "%5p - %m%n");
            //
            //	logProps.put("log4j.logger.com.relationaljunction", "DEBUG, RJ_FILE");
            //	org.apache.log4j.PropertyConfigurator.configure(logProps);

            java.sql.DriverManager.setLogWriter(new PrintWriter(new
                    FileOutputStream(props.getProperty(CommonDriver2.LOG_PATH))));
         } catch (Exception ex) {
            ex.printStackTrace();
            throw new StoreException("Can't create a log file. Error was: " +
                    ex.getMessage());
         }
      }

      /*
      // lockType
      if (props.getProperty(LOCK_TYPE) != null) {
         String lockType = props.getProperty(LOCK_TYPE);
         try {
            if (lockType.equalsIgnoreCase(EXCLUSIVE_LOCK_STRING)) {
               Util.setxBaseJProperty("useSharedLocks", "false");
               lockEnabled = true;
            } else if (lockType.equalsIgnoreCase(SHARED_LOCK_STRING)) {
               Util.setxBaseJProperty("useSharedLocks", "true");
               lockEnabled = true;
            }
         } catch (IOException ex1) {
         }
      }
      */

      // lockFiles
      if (props.getProperty(LOCK_FILES) != null) {
         lockFiles = Boolean.valueOf(props.getProperty(LOCK_FILES));
      }

      if (props.getProperty(CommonDriver2.TEMP_PATH) != null) {
         tempPath = props.getProperty(CommonDriver2.TEMP_PATH);
         if (!new File(tempPath).exists())
            throw new StoreException("Temporary path '" + tempPath +
                    "' doesn't exist");
      }
      // use specified web parameter as a table name for a server page
      if (props.getProperty(CommonDriver2.USE_WEB_PARAM) != null) {
         useWebParam = props.getProperty(CommonDriver2.USE_WEB_PARAM);
      }
      // use big decimal type
      if (props.getProperty(USE_BIG_DECIMAL_TYPE) != null) {
         useBigDecimalType = Boolean.valueOf(props.getProperty(
                 USE_BIG_DECIMAL_TYPE));
      }
      // map dates to DATE type for VFP
      if (props.getProperty(VFP_MAP_TO_DATE_TYPE) != null) {
         vfpMapToDateType = Boolean.valueOf(props.getProperty(VFP_MAP_TO_DATE_TYPE));
      }
      // check column sizes
      if (props.getProperty(CHECK_COLUMN_SIZE) != null) {
         checkColumnSize = Boolean.valueOf(props.getProperty(CHECK_COLUMN_SIZE));
      }
      // max memo size
      if (props.getProperty(MAX_MEMO_SIZE) != null) {
         maxMemoSizeInBytes = Integer.parseInt(props.getProperty(MAX_MEMO_SIZE));
      }
   }

   public synchronized void createStoreTable(String storeName, StoreFieldIF[] fields, IndexTableIF[] indexTables,
                                             Properties props) throws StoreException {
      // more correct invocation. just created DBF does not contain deleted records
      getStoreTable(storeName).create(fields, indexTables);
   }

   public synchronized void deleteStoreTable(String storeName) throws StoreException {
      if (!dir.exists(storeName))
         throw new StoreException("File '" + dir.getPath() + storeName +
                 "' does not exist!");

      try {
         // delete a dbf file
         dir.dropFile(storeName);

         if (memoExtension == null) {
            // delete related memo files
            String fptFile = com.relationaljunction.utils.StringUtils.getFileNameWithoutExtension(
                    storeName) + ".fpt";
            if (dir.exists(fptFile))
               dir.dropFile(fptFile);
            String dbtFile = com.relationaljunction.utils.StringUtils.getFileNameWithoutExtension(
                    storeName) + ".dbt";
            if (dir.exists(dbtFile))
               dir.dropFile(dbtFile);
         } else {
            String customMemoFile = com.relationaljunction.utils.StringUtils.getFileNameWithoutExtension(
                    storeName) + memoExtension;
            if (dir.exists(customMemoFile))
               dir.dropFile(customMemoFile);
         }

         // not neccesary to call. File is already deleted from the cache by dropFile() method
//         deleteStoreTableFromCache(storeName);
      } catch (Exception ex) {
         throw new StoreException("Can't delete the file '" + storeName +
                 "'. Error was: " + ex.getMessage(), ex);
      }
   }

   public void deleteStoreTableFromCache(String storeName) {
      if (storeTablesHash.get(storeName) != null)
         storeTablesHash.remove(storeName);

      if (dir instanceof FileCacheDirectoryManager) {
         // for FTP, SFTP and SMB protocols when local cache file are used
         FileCacheDirectoryManager cacheDir = (FileCacheDirectoryManager) dir;
         // delete a local cache dbf file
         cacheDir.removeFileFromCache(storeName);

         if (memoExtension == null) {
            // delete local cache memo files
            String fptFile = com.relationaljunction.utils.StringUtils.getFileNameWithoutExtension(
                    storeName) + ".fpt";
            cacheDir.removeFileFromCache(fptFile);
            String dbtFile = com.relationaljunction.utils.StringUtils.getFileNameWithoutExtension(
                    storeName) + ".dbt";
            cacheDir.removeFileFromCache(dbtFile);
         } else {
            String customMemoFile = com.relationaljunction.utils.StringUtils.
                    getFileNameWithoutExtension(storeName) + memoExtension;
            cacheDir.removeFileFromCache(customMemoFile);
         }
      }
   }

   public void packStoreTable(String storeName) throws StoreException {
      if (!dir.exists(storeName))
         throw new StoreException("File '" + dir.getPath() + storeName +
                 "' does not exist!");

      StoreTableIF storeTable = getStoreTable(storeName);
      ((DBFTable) storeTable).pack();
   }

   protected StoreTableIF initStoreTable(String storeName) throws StoreException {
      return new DBFTable(storeName, this, dir);
   }

   public StoreTableIF[] getStoreTables(String templateName) throws StoreException {
      FileManager[] commFiles = dir.listFiles(extension, templateName, recursiveFolders);

      StoreTableIF[] tables = new StoreTableIF[commFiles.length];

      for (int i = 0; i < commFiles.length; i++) {
         String storeName = commFiles[i].getName();
         tables[i] = getStoreTable(storeName);
      }

      return tables;
   }

   public boolean supportsIndexes() {
      return false;
   }

   public IndexSchemaIF getIndexSchema() {
      return null;
   }

   public ViewSchemaIF getViewSchema() {
      return null;
   }

   public boolean isLockEnabled() {
      return lockEnabled;
   }

   public void close() {
      try {
         if (dir != null) {
            dir.close();
         }
         if (storeTablesHash != null) {
            storeTablesHash.clear();
         }
      } catch (Exception ex) {
         ex.printStackTrace();
      }

      dir = null;
      storeTablesHash = null;
   }
}
