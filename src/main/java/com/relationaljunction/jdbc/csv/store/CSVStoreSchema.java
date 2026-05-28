package com.relationaljunction.jdbc.csv.store;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.AbstractStoreSchema;
import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.index.IndexSchemaIF;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.database.io.DirectoryManager;
import com.relationaljunction.database.io.FileCacheDirectoryManager;
import com.relationaljunction.database.io.FileManager;
import com.relationaljunction.database.view.ViewSchemaIF;
import com.relationaljunction.jdbc.csv.CsvDriver2;
import com.relationaljunction.jdbc.csv.schema.CSVSchemaLoader;
import com.relationaljunction.jdbc.csv.schema.CSVTableDescr;
import com.relationaljunction.utils.DateFormatter;
import com.relationaljunction.utils.NumberRangeExpression;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class CSVStoreSchema extends AbstractStoreSchema {
   private final Logger log = LoggerFactory.getLogger("CSVStoreSchema");

   public final static String FIXED_LENGTH_KEYWORD = "fixed";
   public final static String PATH = "path";

   public static final String DEFAULT_EXTENSION = ".txt";
   public static final String DEFAULT_SEPARATOR = "\t";
   public static final boolean DEFAULT_SUPPRESS = false;
   public static final String DEFAULT_COMMENT_LINE = null;
   public static final boolean DEFAULT_CACHING = true;
   public final static String DEFAULT_NULL_STRING = "(?i)null";
   public static final String DEFAULT_ROW_DELIMITER = System.getProperty(
           "line.separator");
   public static final boolean DEFAULT_CHECK_LAST_EOL = true;
   public static final String DEFAULT_DATE_FORMAT = DateFormatter.
           DEFAULT_FORMAT_STRING;
   public static final boolean DEFAULT_ESCAPE_EOL_IN_QUOTES = false;
   public static final String DEFAULT_USE_WEB_PARAM = null;
   public static final String DEFAULT_CHARSET = null;
   public final static boolean DEFAULT_QUOTE_STRING = true;
   public final static char DEFAULT_PADDING_CHAR = ' ';
   // properties
   public static final String FILE_EXTENSION = "fileExtension";
   public static final String SEPARATOR = "separator";
   public static final String SUPPRESS_HEADERS = "suppressHeaders";
   public static final String CACHING = "caching";
   public static final String CHARSET = "charset";
   public static final String COMMENT_LINE = "commentLine";
   public final static String DATE_FORMAT = "dateFormat";
   /* since StelsCSV 1.3.1*/
   public static final String ROW_DELIMITER = "rowDelimiter";
   public static final String SCHEMA = "schema";
   public static final String DEFAULT_SCHEMA = "schema.xml";
   /* since StelsCSV 2.2 */
   public final static String FUNCTION_PREFIX = "function:";
   public final static String USE_SCHEMA_META = "useSchemaMeta";
   /* since StelsCSV 2.4 */
   public final static String CHECK_LAST_EOL = "checkLastEOL";
   public final static String ESCAPE_EOL_IN_QUOTES = "escapeEOLInQuotes";
   public final static String USE_WEB_PARAM = "useWebParam";
   public final static String NULL_STRING = "nullString";
   public final static String QUOTE_STRING = "quoteString";
   public final static String PADDING_CHAR = "paddingChar";
   /* since StelsCSV 2.5 */
   public final static String TEMP_PATH = "tempPath";
   public static final String TRIM_BLANKS = "trimBlanks";
   public final static boolean DEFAULT_TRIM_BLANKS = true;
   public static final String EMPTY_STRING_AS_NULL = "emptyStringAsNull";
   public final static boolean DEFAULT_EMPTY_STRING_AS_NULL = true;
   /* since StelsCSV 3.0 */
   public final static String LOG_PATH = "logPath";
   /* since StelsCSV 4.0 */
   public final static String DEFAULT_COLUMN_TYPE = "defaultColumnType";
   public final static String DEFAULT_DEFAULT_COLUMN_TYPE = null;
   /* since StelsCSV 4.01 */
   public final static String DECIMAL_FORMAT_INPUT = "decimalFormatInput";
   public final static String DEFAULT_DECIMAL_FORMAT_INPUT = null;
   public final static String DECIMAL_FORMAT_OUTPUT = "decimalFormatOutput";
   public final static String DEFAULT_DECIMAL_FORMAT_OUTPUT = null;
   public final static String LOCALE = "locale";
   public final static Locale DEFAULT_LOCALE = Locale.getDefault();
   /* since StelsCSV 5.0 */
   public static final boolean DEFAULT_ESCAPE_SEPARATOR_IN_QUOTES = true;
   public final static String ESCAPE_SEPARATOR_IN_QUOTES = "escapeSeparatorInQuotes";
   public final static String IGNORE_CASE = "ignoreCase";
   /* since StelsCSV 5.1 */
   public static final boolean DEFAULT_IGNORE_BOM = false;
   public final static String IGNORE_BOM = "ignoreBOM";
   /* since StelsCSV 5.5 */
   public final static String NULL_STRING_TO_WRITE = "nullStringToWrite";
   public final static String DEFAULT_NULL_STRING_TO_WRITE = "NULL";
   public static final boolean DEFAULT_NULL_ROW_AS_BLANK_LINE = false;
   public static final String NULL_ROW_AS_BLANK_LINE = "nullRowAsBlankLine";
   /* since StelsCSV 6.01 */
   public final static String SUPPORTS_COLUMN_DETAILS_ROW = "supportsColumnDetailsRow";
   public final static boolean DEFAULT_SUPPORTS_COLUMN_DETAILS_ROW = false;
   public final static String IGNORE_ROWS = "ignoreRows";

   /**
    * URL Path
    */
   public String urlPath = null;
   /**
    * Schema name
    */
   public String schemaName = DEFAULT_SCHEMA;
   /**
    * Comment line
    */
   String comment = DEFAULT_COMMENT_LINE;
   List<String> commentTokens = new ArrayList<String>();
   /**
    * Field separator to use
    */
   String separator = DEFAULT_SEPARATOR;
   /**
    * Row delimiter to use
    */
   String rowDelimiter = DEFAULT_ROW_DELIMITER;
   /**
    * Should headers be suppressed
    */
   public boolean suppressHeaders = DEFAULT_SUPPRESS;
   /**
    * Fixed length type
    */
   boolean fixedLengthType = false;
   /**
    * Charset that should be used to read the files
    */
   String charset = DEFAULT_CHARSET;

   // Date formatter
   String dateFormatString = DateFormatter.DEFAULT_FORMAT_STRING;

   // Decimal formats. Since 4.01 version
   String decimalFormatInput = DEFAULT_DECIMAL_FORMAT_INPUT;
   String decimalFormatOut = DEFAULT_DECIMAL_FORMAT_OUTPUT;

   /**
    * Locale
    */
   Locale locale = DEFAULT_LOCALE;

   /**** advanced features ****/
   /**
    * Custom property allowing to use only column data in the schema for meta data*
    */
   public boolean useSchemaMeta = false;
   /**
    * check for last EOL existing at the end of file *
    */
   boolean checkLastEOL = DEFAULT_CHECK_LAST_EOL;
   /**
    * use web param for passing table name *
    */
   String useWebParam = DEFAULT_USE_WEB_PARAM;
   /**
    * escape EOL in quotes*
    */
   boolean escapeEOLInQuotes = DEFAULT_ESCAPE_EOL_IN_QUOTES;
   /**
    * escape separator in quotes*
    */
   boolean escapeSeparatorInQuotes = DEFAULT_ESCAPE_SEPARATOR_IN_QUOTES;

   // input NULL string
   String nullString = DEFAULT_NULL_STRING;

   // output NULL string
   String nullStringToWrite = DEFAULT_NULL_STRING_TO_WRITE;

   // quote string
   boolean quoteString = DEFAULT_QUOTE_STRING;

   /**
    * padding char for fixed length files*
    */
   char paddingChar = DEFAULT_PADDING_CHAR;
   /**
    * temp path*
    */
   String tempPath = null;
   /**
    * trim blanks*
    */
   boolean trimBlanks = DEFAULT_TRIM_BLANKS;
   /**
    * empty string is treated as Null*
    */
   boolean emptyStringAsNull = DEFAULT_EMPTY_STRING_AS_NULL;
   /**
    * defaultColumnType *
    */
   String defaultColumnType = DEFAULT_DEFAULT_COLUMN_TYPE;
   /**
    * locks *
    */
   boolean lockFiles = false;
   /**
    * ignore BOM in the begining of a CSV file. Feature request by a customer.
    */
   boolean ignoreBOM = DEFAULT_IGNORE_BOM;

   // null row (i.e. record with NULL values) as a blank line
   boolean nullRowAsBlankLine = DEFAULT_NULL_ROW_AS_BLANK_LINE;

   // supporting an internal row that holds info about column types. Feature request by a customer.
   boolean supportsColumnDetailsRow = DEFAULT_SUPPORTS_COLUMN_DETAILS_ROW;

   // rows that should be ignored
   NumberRangeExpression ignoreRowsExpression;

   public DirectoryManager csvDir = null;
   public CSVSchemaLoader schemaLoader = null;

   public CSVStoreSchema(String urlPath, Properties globalProps) throws SQLException {
      super(globalProps);

      this.urlPath = urlPath;

      // reset static SWAP parameters
//      SwapManager.resetParams();

      try {
         loadPropertiesFile(null, globalProps);

         seekSchemaParameters(globalProps);

         // load properties in variables
         loadGlobalPropertiesInVars(globalProps);

         // build a Directory instance
         csvDir = buildCSVDirectoryManager(urlPath, tempPath);

         // load the schema
         this.schemaLoader = new CSVSchemaLoader(buildSchemaFile(schemaName), globalProps);
      } catch (Exception ex) {
//      ex.printStackTrace();
         throw new SQLException("[Relational Junction CSV Driver] Error loading the schema: " + ex.getMessage());
      }
   }

   // loads global properties in variables
   private void loadGlobalPropertiesInVars(Properties props) throws Exception {
      if (props != null) {
         // schema name
         if (props.getProperty(SCHEMA) != null) {
            schemaName = props.getProperty(SCHEMA);
         }
         // separator character
         if (props.getProperty(SEPARATOR) != null) {
            String separatorString = props.getProperty(SEPARATOR);
            if (separatorString.trim().equals(FIXED_LENGTH_KEYWORD))
               fixedLengthType = true;
            else {
               fixedLengthType = false;

               if (separatorString.equalsIgnoreCase("\\t"))
                  separator = "\t";
               else
                  separator = separatorString;
            }
         }
         // header suppression flag
         if (props.getProperty(SUPPRESS_HEADERS) != null) {
            suppressHeaders = Boolean.valueOf(props.getProperty(
                    SUPPRESS_HEADERS));
         }
         // charset
         if (props.getProperty(CHARSET) != null) {
            charset = props.getProperty(CHARSET);
         }
         // comment line
         if (props.getProperty(COMMENT_LINE) != null) {
            comment = props.getProperty(COMMENT_LINE);

            StringTokenizer tokenizer = new StringTokenizer(comment, "|");
            while (tokenizer.hasMoreElements()) {
               commentTokens.add(tokenizer.nextToken());
            }
         }
         // date format
         if (props.getProperty(DATE_FORMAT) != null) {
            dateFormatString = props.getProperty(DATE_FORMAT);
         }
         // locale
         if (props.getProperty(LOCALE) != null) {
            StringTokenizer tokenizer =
                    new StringTokenizer(props.getProperty(LOCALE), "_");
            String language = tokenizer.nextToken();
            String country = tokenizer.nextToken();
            locale = new Locale(language, country);
         }
         // decimal format output
         if (props.getProperty(DECIMAL_FORMAT_OUTPUT) != null) {
            decimalFormatOut = props.getProperty(DECIMAL_FORMAT_OUTPUT);
         }
         // decimal format input
         if (props.getProperty(DECIMAL_FORMAT_INPUT) != null) {
            decimalFormatInput = props.getProperty(DECIMAL_FORMAT_INPUT);
         }

         // row delimeter
         if (props.getProperty(ROW_DELIMITER) != null) {
            rowDelimiter = props.getProperty(ROW_DELIMITER);
            if (rowDelimiter.contains("\""))
               throw new Exception(
                       "Parameter 'rowDelimiter' must not contain double quote characters");
            rowDelimiter = com.relationaljunction.utils.StringUtils.replaceSubString(rowDelimiter,
                    "\\r", "\r");
            rowDelimiter = com.relationaljunction.utils.StringUtils.replaceSubString(rowDelimiter,
                    "\\n", "\n");
         }
         // use schema meta
         if (props.getProperty(USE_SCHEMA_META) != null) {
            useSchemaMeta = Boolean.valueOf(props.getProperty(
                    USE_SCHEMA_META));
         }
         // check last EOL
         if (props.getProperty(CHECK_LAST_EOL) != null) {
            checkLastEOL = Boolean.valueOf(props.getProperty(
                    CHECK_LAST_EOL));
         }
         // escape EOL in quotes
         if (props.getProperty(ESCAPE_EOL_IN_QUOTES) != null) {
            escapeEOLInQuotes = Boolean.valueOf(props.getProperty(
                    ESCAPE_EOL_IN_QUOTES));
         }
         // escape separator in quotes
         if (props.getProperty(ESCAPE_SEPARATOR_IN_QUOTES) != null) {
            escapeSeparatorInQuotes = Boolean.valueOf(props.getProperty(
                    ESCAPE_SEPARATOR_IN_QUOTES));
         }
         // use specified web parameter as a table name for a server page
         if (props.getProperty(USE_WEB_PARAM) != null) {
            useWebParam = props.getProperty(USE_WEB_PARAM);
         }
         // input null string
         if (props.getProperty(NULL_STRING) != null) {
            nullString = props.getProperty(NULL_STRING);
         }
         // output null string
         if (props.getProperty(NULL_STRING_TO_WRITE) != null) {
            nullStringToWrite = props.getProperty(NULL_STRING_TO_WRITE);
         }
         // quote string
         if (props.getProperty(QUOTE_STRING) != null) {
            quoteString = Boolean.valueOf(props.getProperty(QUOTE_STRING));
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
         // padding char
         if (props.getProperty(PADDING_CHAR) != null) {
            String propPaddingChar = props.getProperty(PADDING_CHAR);
            if (propPaddingChar.isEmpty())
               paddingChar = ' ';
            else
               paddingChar = props.getProperty(PADDING_CHAR).charAt(0);
         }
         // default column type
         if (props.getProperty(DEFAULT_COLUMN_TYPE) != null) {
            defaultColumnType = props.getProperty(DEFAULT_COLUMN_TYPE);
         }
         // lockFiles
         if (props.getProperty(LOCK_FILES) != null) {
            lockFiles = Boolean.valueOf(props.getProperty(
                    LOCK_FILES));
            //  checkLastEOL should be set to 'false'
            checkLastEOL = false;
         }
         // ignore BOM
         if (props.getProperty(IGNORE_BOM) != null) {
            ignoreBOM = Boolean.valueOf(props.getProperty(IGNORE_BOM));
         }
         // null row as a blank line
         if (props.getProperty(NULL_ROW_AS_BLANK_LINE) != null) {
            nullRowAsBlankLine = Boolean.valueOf(props.getProperty(NULL_ROW_AS_BLANK_LINE));
         }
         // supporting internal column details row
         if (props.getProperty(SUPPORTS_COLUMN_DETAILS_ROW) != null) {
            supportsColumnDetailsRow = Boolean.valueOf(props.getProperty(SUPPORTS_COLUMN_DETAILS_ROW));
         }
         // rows that should be ignored
         if (props.getProperty(IGNORE_ROWS) != null) {
            String ignoreRowsString = props.getProperty(IGNORE_ROWS);
            ignoreRowsExpression = new NumberRangeExpression(ignoreRowsString);
         } // log path
         if (props.getProperty(LOG_PATH) != null) {
            try {
               java.sql.DriverManager.setLogWriter(new PrintWriter(new
                       FileOutputStream(props.getProperty(LOG_PATH), false)));
            } catch (Exception ex) {
               ex.printStackTrace();
            }
         }
      }
      // end of the properties analysis
   }

   public String getDefaultFileExtension() {
      return CsvDriver2.DEFAULT_EXTENSION;
   }

   public synchronized void createStoreTable(String storeName,
                                             StoreFieldIF[] fields,
                                             IndexTableIF[] indexTables,
                                             Properties props) throws StoreException {
      try {
         // append a table in schema file
         schemaLoader.createTable(storeName, fields, suppressHeaders);
         CSVTable table = new CSVTable(storeName, null, csvDir, this);
         table.create(fields, indexTables);
      } catch (Exception ex) {
//      ex.printStackTrace();
         throw new StoreException(ex.getMessage());
      }
   }

   public synchronized void deleteStoreTable(String storeName) throws StoreException {
      try {
         if (!csvDir.exists(storeName))
            throw new Exception("File '" + csvDir.getPath() + storeName +
                    "' does not exist!");
         // delete a file
         csvDir.dropFile(storeName);
         // remove the table from schema
         schemaLoader.dropTable(storeName);

         // not neccesary to call. File is already deleted from the cache by dropFile() method
//         deleteStoreTableFromCache(storeName);
      } catch (Exception ex) {
         throw new StoreException(ex.getMessage());
      }
   }

   public void deleteStoreTableFromCache(String storeName) {
      if (storeTablesHash.get(storeName) != null)
         storeTablesHash.remove(storeName);

      if (csvDir instanceof FileCacheDirectoryManager) {
         // for FTP and SFTP protocols when local cache file are used
         FileCacheDirectoryManager cacheDir = (FileCacheDirectoryManager) csvDir;
         // delete a local cache CSV/text file
         cacheDir.removeFileFromCache(storeName);
      }
   }

//   public StoreTableIF getStoreTable(String storeName) throws StoreException {
//      StoreTableIF storeTable = storeTablesHash.get(storeName);
//      if (storeTable == null) {
//         CSVTableDescr tableDescr = schemaLoader.getSchemaData().getTableDescription(
//                 storeName);
//         storeTable = new CSVTable(storeName, tableDescr, csvDir, this);
//         storeTablesHash.put(storeName, storeTable);
//      }
//      return storeTable;
//   }

   protected StoreTableIF initStoreTable(String storeName) throws StoreException {
      CSVTableDescr tableDescr = schemaLoader.getSchemaData().getTableDescription(
              storeName);

      if (tableDescr == null) {
         com.relationaljunction.utils.OtherUtils.writeWarnInfo(log, "can't find table description '"
                 + storeName + "' in the schema. The driver will use default settings.", true);
      }

      return new CSVTable(storeName, tableDescr, csvDir, this);
   }

   public StoreTableIF[] getStoreTables(String templateName) throws StoreException {
      FileManager[] commFiles = csvDir.listFiles(fileExtension, templateName);
      StoreTableIF[] tables = new StoreTableIF[commFiles.length];

      for (int i = 0; i < commFiles.length; i++) {
         String storeName = commFiles[i].getName();
         tables[i] = getStoreTable(storeName);
      }

      return tables;
   }

   private DirectoryManager buildCSVDirectoryManager(String path,
                                                     String tempPath) throws Exception {
      return DirectoryManager.buildDirectoryManager(path, tempPath, useWebParam);
   }

   private FileManager buildSchemaFile(String name) throws
           Exception {
      FileManager fm;
      try {
         fm = FileManager.buildFileManager(csvDir, name);
      } catch (Exception ex) {
         throw new Exception("Can't load the schema file. Error was: " +
                 ex.getMessage());
      }
      return fm;
   }

   public boolean supportsIndexes() {
      return false;
   }

   public boolean requiresLoadingTablesInMetaData() {
      return true;
   }

   public IndexSchemaIF getIndexSchema() {
      return null;
   }

   public ViewSchemaIF getViewSchema() {
      return null;
   }

   public void close() {
      try {
         if (csvDir != null) {
            csvDir.close();
         }
         if (storeTablesHash != null) {
            storeTablesHash.clear();
         }
      } catch (Exception ex) {
      }

      csvDir = null;
      storeTablesHash = null;
   }
}
