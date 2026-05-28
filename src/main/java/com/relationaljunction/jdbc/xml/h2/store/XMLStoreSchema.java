package com.relationaljunction.jdbc.xml.h2.store;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;

import com.relationaljunction.database.*;
import com.relationaljunction.database.h2.StoreSchemaIF2;
import com.relationaljunction.database.index.IndexSchemaIF;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.database.io.FileManager;
import com.relationaljunction.database.view.ViewSchemaIF;
import com.relationaljunction.jdbc.common.h2.CommonDriver2;
import com.relationaljunction.jdbc.xml.XMLDriver2;
import com.relationaljunction.utils.DateFormatter;
import com.relationaljunction.utils.NumberRangeExpression;
import com.relationaljunction.utils.StringUtils;


public class XMLStoreSchema extends AbstractStoreSchema implements StoreSchemaIF2 {
   public final static String XML_NAMESPACE_URI = "http://www.w3.org/XML/1998/namespace";
   public final static String DEFAULT_CHARSET = "UTF-8";
   public final static String NAMESPACES = "namespaces";
   public final static String NAMESPACE_AWARE = "namespaceAware";

   /* since StelsXML 1.3 */
   public final static String DECIMAL_FORMAT_INPUT = "decimalFormatInput";
   public final static String DEFAULT_DECIMAL_FORMAT_INPUT = null;
   public final static String DECIMAL_FORMAT_OUTPUT = "decimalFormatOutput";
   public final static String DEFAULT_DECIMAL_FORMAT_OUTPUT = null;
   public final static String LOCALE = "locale";
   public final static Locale DEFAULT_LOCALE = Locale.getDefault();

   /* since StelsXML 2.0 */
   public final static String DEFAULT_NULL_STRING_INPUT = "(?i)null";
   public final static String NULL_STRING_INPUT = "nullStringInput";
   public final static String DEFAULT_NULL_STRING_OUTPUT = "";
   public final static String NULL_STRING_OUTPUT = "nullStringOutput";
   public final static boolean DEFAULT_TRIM_BLANKS = false;
   public static final String TRIM_BLANKS = "trimBlanks";
   public static final String EMPTY_STRING_AS_NULL = "emptyStringAsNull";
   public final static boolean DEFAULT_EMPTY_STRING_AS_NULL = true;
   public final static String LOG_PATH = "logPath";

   // API
   public final static String READ_API = "readAPI";
   public final static String WRITE_API = "writeAPI";
   public final static String DEFAULT_WRITE_API = "SAX";
   public final static String XMLDOG_LIBRARY = "XMLDog";

   public final static String SAX_API = "SAX";
   // XOM and NUX are equivalent names
   public final static String NUX_API = "NUX";
   public final static String XOM_API = "XOM";

   /* since StelsXML 3.2 */
   public final static boolean DEFAULT_DISABLE_DTD = false;
   public final static String DISABLE_DTD = "disableDTD";
   public final static String REPEAT_LAST_VALUES = "repeatLastValues";
   public final static boolean DEFAULT_REPEAT_LAST_VALUES = true;
   public final static String RETURN_LAST_VALUES = "returnLastValues";
   public final static boolean DEFAULT_RETURN_LAST_VALUES = false;

   public static String DEFAULT_SAX_LIBRARY = XMLDOG_LIBRARY;
   public static String DEFAULT_READ_API = "SAX";

   public final static String IGNORE_ROWS = "ignoreRows";

   String schemaPath = null;
   FileManager fmSchema = null;
   XMLSchemaLoader schemaLoader = null;

   /**
    * Charset that should be used to read the files
    */
   public String charset = DEFAULT_CHARSET;

   // Date formatter
   public String dateFormatString = DateFormatter.DEFAULT_FORMAT_STRING;

   // Decimal formats
   public String decimalFormatInput = DEFAULT_DECIMAL_FORMAT_INPUT;
   public String decimalFormatOut = DEFAULT_DECIMAL_FORMAT_OUTPUT;

   /**
    * Namespaces
    */
   public String namespaces = null;

   /**
    * Namespace aware
    */
   public boolean namespaceAware = true;

   /**
    * Locale
    */
   public Locale locale = DEFAULT_LOCALE;
   /**
    * input NULL string*
    */
   public String nullStringInput = DEFAULT_NULL_STRING_INPUT;
   /**
    * output NULL string*
    */
   public String nullStringOutput = DEFAULT_NULL_STRING_OUTPUT;
   /**
    * trim blanks*
    */
   public boolean trimBlanks = DEFAULT_TRIM_BLANKS;

   // empty string is treated as Null
   public boolean emptyStringAsNull = DEFAULT_EMPTY_STRING_AS_NULL;

   // disable DTD parsing
   public boolean disableDTD = DEFAULT_DISABLE_DTD;

   public boolean repeatLastValues = DEFAULT_REPEAT_LAST_VALUES;

   public boolean returnLastValues = DEFAULT_RETURN_LAST_VALUES;

   // read API
   String readAPI = DEFAULT_READ_API;

   // write API
   String writeAPI = DEFAULT_WRITE_API;

   // rows that should be ignored
   NumberRangeExpression ignoreRowsExpression;

   public XMLStoreSchema(Properties globalProps) throws Exception {
      super(globalProps);

      loadPropertiesFile(null, globalProps);

      seekSchemaParameters(globalProps);

      loadGlobalPropertiesInVars(globalProps);
      // form FileManager instance by using schema path
      fmSchema = buildSchemaFile(schemaPath);
      // parse and create the schema structure
      schemaLoader = new XMLSchemaLoader(fmSchema, globalProps);

      // Sun JAXP (Xerces/Xalan) default model
//      System.setProperty("javax.xml.xpath.XPathFactory:" +
//              XPathFactory.DEFAULT_OBJECT_MODEL_URI,
//              "com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl");
   }

   private FileManager buildSchemaFile(String schemaPath) throws
           Exception {
      FileManager fm = FileManager.buildFileManager(null, schemaPath);

      if (!fm.exists())
         throw new Exception("The schema path '" + schemaPath + "' doesn't exist");

      return fm;
   }

   // loads global properties in variables
   protected void loadGlobalPropertiesInVars(Properties props) throws Exception {
      // load required properties for the schema
      this.schemaPath = props.getProperty(CommonDriver2.PATH);

      // charset
      if (props.getProperty(CommonDriver2.CHARSET) != null) {
         charset = props.getProperty(CommonDriver2.CHARSET);
      }
      // date format
      if (props.getProperty(CommonDriver2.DATE_FORMAT) != null) {
         dateFormatString = props.getProperty(CommonDriver2.DATE_FORMAT);
      }
      // locale
      if (props.getProperty(LOCALE) != null) {
         StringTokenizer tokenizer =
                 new StringTokenizer(props.getProperty(LOCALE), "_");
         locale = new Locale(tokenizer.nextToken(), tokenizer.nextToken());
      }
      // decimal format output
      if (props.getProperty(DECIMAL_FORMAT_OUTPUT) != null) {
         decimalFormatOut = props.getProperty(DECIMAL_FORMAT_OUTPUT);
      }
      // decimal format input
      if (props.getProperty(DECIMAL_FORMAT_INPUT) != null) {
         decimalFormatInput = props.getProperty(DECIMAL_FORMAT_INPUT);
      }
      // namespaces
      if (props.getProperty(NAMESPACES) != null) {
         namespaces = props.getProperty(NAMESPACES);
      }
      // namespaceAware
      if (props.getProperty(NAMESPACE_AWARE) != null) {
         namespaceAware = Boolean.valueOf(props.getProperty(NAMESPACE_AWARE));
      }
      // input null string
      if (props.getProperty(NULL_STRING_INPUT) != null) {
         nullStringInput = props.getProperty(NULL_STRING_INPUT);
      }
      // output null string
      if (props.getProperty(NULL_STRING_OUTPUT) != null) {
         nullStringOutput = props.getProperty(NULL_STRING_OUTPUT);
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
      // disable DTD parsing
      if (props.getProperty(DISABLE_DTD) != null) {
         disableDTD = Boolean.valueOf(props.getProperty(DISABLE_DTD));
      }
      // repeat last values
      if (props.getProperty(REPEAT_LAST_VALUES) != null) {
         repeatLastValues = Boolean.valueOf(props.getProperty(REPEAT_LAST_VALUES));
      }
      // return last values
      if (props.getProperty(RETURN_LAST_VALUES) != null) {
         returnLastValues = Boolean.valueOf(props.getProperty(RETURN_LAST_VALUES));
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
      // read API
      if (props.getProperty(READ_API) != null) {
         String _readAPI = props.getProperty(READ_API);
         if (_readAPI.equalsIgnoreCase(SAX_API))
            readAPI = SAX_API;
         else if (_readAPI.equalsIgnoreCase(NUX_API))
            readAPI = NUX_API;
            // XOM and NUX are equivalent names
         else if (_readAPI.equalsIgnoreCase(XOM_API))
            readAPI = NUX_API;
         else
            throw new IllegalArgumentException("Unknown value for 'readAPI' parameter");
      }
      // write API
      if (props.getProperty(WRITE_API) != null) {
         String _readAPI = props.getProperty(WRITE_API);
         if (_readAPI.equalsIgnoreCase(SAX_API))
            writeAPI = SAX_API;
         else
            throw new IllegalArgumentException("Unknown value for 'writeAPI' parameter");
      }
   }

   public String getDefaultFileExtension() {
      return XMLDriver2.DEFAULT_EXTENSION;
   }

   protected StoreTableIF initStoreTable(String storeName) throws StoreException {
      try {
         XMLTableDescr tableDescr;
         FileManager fmStore;
         tableDescr = schemaLoader.getTableDescription(storeName);

         String filePath;
         if (tableDescr.getName() == null)
            // if the table descr is specified by file name only
            filePath = storeName;
         else
            filePath = tableDescr.getFilePath();

         fmStore = initFileManager(filePath);

         return initStoreTable(storeName, tableDescr, fmStore);
      } catch (Exception ex) {
         // ex.printStackTrace();
         throw new StoreException(ex);
      }
   }

   public FileManager initFileManager(String filePath) throws Exception {
      return FileManager.buildFileManager(fmSchema.getDir(), filePath);
   }

   protected StoreTableIF initStoreTable(String storeName,
                                         XMLTableDescr tableDescr,
                                         FileManager fmStore) throws Exception {
      return new XMLTable(storeName, tableDescr, fmStore, this);
   }

   public synchronized void createStoreTable(String storeName, StoreFieldIF[] fields, IndexTableIF[] indexTables,
                                             Properties props) throws StoreException {
      // Table to be created must be existed in the schema.
      // Otherwise, the exception is throwed
      StoreTableIF storeTable = getStoreTable(storeName);

      // check for table existence
      boolean isExist;
      try {
         isExist = ((XMLTable) storeTable).getFileManager().exists();
      } catch (Exception ex) {
         throw new StoreException("Error while creating table: " +
                 ex.getMessage(), ex);
      }
      if (isExist)
         throw new StoreException("File '" + storeName + "' already exists!");

      // create a table
      storeTable.create(fields, indexTables);
   }

   public synchronized void deleteStoreTable(String storeName) throws StoreException {
      XMLTable storeTable = (XMLTable) getStoreTable(storeName);

      // check for table existence
      boolean isExist;
      try {
         isExist = storeTable.getFileManager().exists();
      } catch (Exception ex) {
         throw new StoreException("Error while deleting table: " +
                 ex.getMessage(), ex);
      }
      if (!isExist)
         throw new StoreException("File '" + storeName + "' does not exist!");

      // delete a file
      try {
         storeTable.getFileManager().getDir().dropFile(storeTable.getFileManager().getName());
      } catch (Exception ex) {
         throw new StoreException("Can't delete the file '" + storeName +
                 "' Error was: " + ex.getMessage(), ex);
      }

      // delete an StoreTableIF instance
      deleteStoreTableFromCache(storeName);
   }

   public StoreTableIF[] getStoreTables(String templateName) throws
           StoreException {
      Set<XMLTableDescr> tableDescrSet = schemaLoader.getTables();
      List<StoreTableIF> storeTables = new ArrayList<StoreTableIF>(tableDescrSet.size());

      for (XMLTableDescr tableDescr : tableDescrSet) {
         if (tableDescr.getName() != null &&
                 (templateName == null ||
                         templateName.trim().isEmpty() ||
                         StringUtils.isLike(tableDescr.getName(),
                                 templateName.toLowerCase(), '%', '_'))) {

            StoreTableIF storeTable;

            try {
               storeTable = getStoreTable(tableDescr.getName());
            } catch (StoreException ex) {
               ex.printStackTrace();
               continue;
//               throw new UnexpectedException(ex);
            }

            storeTables.add(storeTable);
         }
      }

      return storeTables.toArray(new StoreTableIF[0]);
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
      if (storeTablesHash != null) {
         storeTablesHash.clear();
      }

      fmSchema = null;
      schemaLoader = null;
      storeTablesHash = null;
   }
}
