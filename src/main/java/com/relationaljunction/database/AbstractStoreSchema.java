package com.relationaljunction.database;

import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.index.IndexSchemaIF;
import com.relationaljunction.database.io.DirectoryManager;
import com.relationaljunction.database.io.FileManager;
import com.relationaljunction.database.view.ViewSchemaIF;
import com.relationaljunction.jdbc.common.h2.CommonDriver2;
import com.relationaljunction.utils.OtherUtils;

/**
 * performs I/0 operations and getting meta data with external files (CSV, DBF, XML, MDB, etc)
 */
abstract public class AbstractStoreSchema implements StoreSchemaIF {
   public final static char[] QUOTE_CHARS = new char[]{'"', '!', '`'};
   private static final String PROPERTIES_FILE = "propertiesFile";
   public final static String PARAMETER_PREFIX = "parameter_";
   public final static Pattern DEFAULT_TRUE_VALUE_PATTERN = Pattern.compile("(?i)true|yes|t|y|1");

   // should be scanned internal folders recursively
   private static final String RECURSIVE_FOLDERS = "recursiveFolders";
   private static final boolean DEFAULT_RECURSIVE_FOLDERS = false;

   private static final String INCLUDE_ROW_ID_COLUMN = "includeRowIdColumn";
   private static final boolean DEFAULT_INCLUDE_ROW_ID_COLUMN = false;
   public static final String ROW_ID_COLUMN_NAME = "row_id_";

   private final Logger log = LoggerFactory.getLogger("AbstractStoreSchema");

   protected Hashtable<String, StoreTableIF> storeTablesHash = new Hashtable<String, StoreTableIF>();
   protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

   // schema file modification date
   protected Date schemaFileModificationDate = null;
   protected String fileExtension = "";
   protected boolean recursiveFolders = DEFAULT_RECURSIVE_FOLDERS;
   private Properties props = null;
   private boolean includeRowIdColumn = DEFAULT_INCLUDE_ROW_ID_COLUMN;

   public AbstractStoreSchema(Properties props) {
      this.props = props;
      this.fileExtension = getDefaultFileExtension();

      // "fileExtension" property
      if (props.getProperty(CommonDriver2.FILE_EXTENSION) != null) {
         fileExtension = props.getProperty(CommonDriver2.FILE_EXTENSION).trim();
         if (fileExtension.trim().equals("."))
            fileExtension = "";
         else if (!fileExtension.contains(".") &&
                 !fileExtension.trim().isEmpty())
            fileExtension = "." + fileExtension;
      }
      // "extension" property is the same as "fileExtension"
      if (props.getProperty(CommonDriver2.FILE_EXTENSION2) != null) {
         fileExtension = props.getProperty(CommonDriver2.FILE_EXTENSION2).trim();
         if (fileExtension.trim().equals("."))
            fileExtension = "";
         else if (!fileExtension.contains(".") &&
                 !fileExtension.trim().isEmpty())
            fileExtension = "." + fileExtension;
      }
      // scan internal folder for files
      if (props.getProperty(RECURSIVE_FOLDERS) != null) {
         recursiveFolders = Boolean.valueOf(props.getProperty(
                 RECURSIVE_FOLDERS));
      }
      // include reserved row id column
      if (props.getProperty(INCLUDE_ROW_ID_COLUMN) != null) {
         includeRowIdColumn = Boolean.valueOf(props.getProperty(
                 INCLUDE_ROW_ID_COLUMN));
      }
   }

   public Date getModificationDate() {
      return schemaFileModificationDate;
   }

   public void setModificationDate(Date schemaFileModificationDate) {
      this.schemaFileModificationDate = schemaFileModificationDate;
   }

   public boolean isModificationDateChanged() throws StoreException {
      return false;
   }

   /**
    * return a file name in accordance with tableName specified in SQL
    */
   public String getFileTableName(String sqlTableName) {
      String fileTableName = com.relationaljunction.utils.StringUtils.unquote(sqlTableName,
              QUOTE_CHARS);

      if (!fileTableName.contains("."))
         fileTableName += getFileExtension();
      return fileTableName;
   }

   public String getFileExtension() {
      return fileExtension;
   }

   public void reload() throws StoreException {
   }

   public void close() {
   }


   public abstract void deleteStoreTable(String storeName) throws StoreException;

   public void deleteStoreTableFromCache(String storeName) {
      if (storeTablesHash.get(storeName) != null)
         storeTablesHash.remove(storeName);
   }

   public IndexSchemaIF getIndexSchema() {
      return null;
   }

   public String getName() {
      return "default";
   }

   public Properties getSchemaProperties() {
      return props;
   }

   /**
    * It should be thread-safe.
    *
    * @param storeName
    * @return
    * @throws StoreException
    */
   public synchronized StoreTableIF getStoreTable(String storeName) throws StoreException {
      StoreTableIF storeTable = storeTablesHash.get(storeName);
      if (storeTable == null) {
         storeTable = initStoreTable(storeName);
         storeTablesHash.put(storeName, storeTable);
      }
      return storeTable;
   }

   protected StoreTableIF initStoreTable(String storeName) throws StoreException {
      return null;
   }

   public StoreTableIF[] getStoreTables(String templateName) throws
           StoreException {
      return null;
   }

   public ViewSchemaIF getViewSchema() {
      return null;
   }

   public boolean supportsIndexes() {
      return false;
   }

   public String getExternalEngineClass() {
      return null;
   }

   public boolean supportsExternalEngine() {
      return false;
   }

   protected void seekSchemaParameters(Properties props) {
      Enumeration enumer = props.keys();
      while (enumer.hasMoreElements()) {
         String property = (String) enumer.nextElement();
         if (property.startsWith(PARAMETER_PREFIX)) {
            String paramName = property.substring(PARAMETER_PREFIX.
                    length());
            String replacement = props.getProperty(property);
            props.remove(PARAMETER_PREFIX + paramName);
            props.setProperty("{@" + paramName + "}", replacement);
         }
      }
   }

   protected void loadPropertiesFile(DirectoryManager dir, Properties props)
           throws StoreException {
      String propsPath = props.getProperty(PROPERTIES_FILE);
      if (propsPath == null) return;

      try {
         FileManager fm = FileManager.buildFileManager(dir, propsPath);

         InputStream is = fm.getInputStream();
         props.load(is);
         is.close();
      } catch (Exception e) {
         e.printStackTrace();
         throw new StoreException("Can't load the properties file '" + propsPath +
                 "'. Error was: " + e.getMessage(), e);
      }
   }

   public boolean requiresLoadingTablesInMetaData() {
      return false;
   }

   public boolean supportsIdentityInNonCachingMode() {
      return false;
   }

   public boolean includeRowIdColumn() {
      return includeRowIdColumn;
   }

   // ##### locks #####

   public boolean requiresLockingForWritingOperations() {
      return false;
   }

   public boolean requiresLockingForReadingOperations() {
      return false;
   }

   public void lockForReading() {
   }

   public void lockForWriting() {
   }

   public void lockDatabase() throws Exception {
   }

   public void unlockDatabase() throws Exception {
   }

   public void lock(boolean exclusive) {
      if (exclusive) {
         lock.writeLock().lock();
      } else {
         lock.readLock().lock();
      }

      OtherUtils.writeTraceInfo(log, "schema " + this + " is LOCKED (exclusive = " + exclusive + ")");
   }

   public void unlock() {
      if (lock.isWriteLockedByCurrentThread()) {
         lock.writeLock().unlock();
      } else {
         lock.readLock().unlock();
      }

      OtherUtils.writeTraceInfo(log, "schema " + this + " is unlocked");
   }
}
