package com.relationaljunction.jdbc.xml.h2.store;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

import com.relationaljunction.database.*;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.database.io.FileManager;
import com.relationaljunction.jdbc.common.h2.CommonDriver2;
import com.relationaljunction.utils.DateFormatter;
import com.relationaljunction.utils.DecimalFormatter;
import com.relationaljunction.utils.NumberRangeExpression;
import com.relationaljunction.utils.UnexpectedException;

public class XMLTable extends AbstractStoreTable {
   public final static String XML_DECLARATION_PREFIX =
           "<?xml version=\"1.0\" encoding=\"";
   public final static String XML_DECLARATION_POSTFIX = "\"?>";

   public XMLTableDescr tableDescr = null;
   public FileManager fmTable = null;

   // XPath manager for reading
   private XPathManager readXPathManager = null;
   // XPath manager for writing
   private XPathManager writeXPathManager = null;

   // a DOM node that will be used for writing records in XML as a template
   org.w3c.dom.Node templateNode = null;

   // string representation of a template row with parameters
   String rowTemplateString = null;

   // store fields
   StoreFieldIF[] fields = null;

   // XPath expressions for columns
   public String[] columnSpec = null;

   // Table props
   Properties localProps = new Properties();

   // Table name
   public String name = null;

   // Charset that should be used to write XML files
   public String charset = XMLStoreSchema.DEFAULT_CHARSET;

   // Date formatter
   String dateFormatString = DateFormatter.DEFAULT_FORMAT_STRING;

   // Decimal formats
   String decimalFormatInput = XMLStoreSchema.DEFAULT_DECIMAL_FORMAT_INPUT;
   String decimalFormatOut = XMLStoreSchema.DEFAULT_DECIMAL_FORMAT_OUTPUT;

   // Namespaces
   private String namespaces = null;
   public HashMap<String,
           String> namespacesMap = new HashMap<String, String>();

   // Namespace aware
//   public boolean namespaceAware = true;

   // Locale
   Locale locale = XMLStoreSchema.DEFAULT_LOCALE;

   // output NULL String
   String nullStringOutput = XMLStoreSchema.DEFAULT_NULL_STRING_OUTPUT;

   // input NULL string
   private String nullStringInput = XMLStoreSchema.DEFAULT_NULL_STRING_INPUT;
   Pattern nullPattern;

   // trim blanks
   boolean trimBlanks = XMLStoreSchema.DEFAULT_TRIM_BLANKS;

   // empty string is treated as Null
   boolean emptyStringAsNull = XMLStoreSchema.DEFAULT_EMPTY_STRING_AS_NULL;

   // read API
   String readAPI = XMLStoreSchema.DEFAULT_READ_API;

   // write API
   String writeAPI = XMLStoreSchema.DEFAULT_WRITE_API;

   // pattern for boolean true value
   Pattern trueValuePattern = AbstractStoreSchema.DEFAULT_TRUE_VALUE_PATTERN;

   // rows that should be ignored
   NumberRangeExpression ignoreRowsExpression;

   // disable DTD parsing
   public boolean disableDTD = XMLStoreSchema.DEFAULT_DISABLE_DTD;

   public boolean repeatLastValues = XMLStoreSchema.DEFAULT_REPEAT_LAST_VALUES;

   public boolean returnLastValues = XMLStoreSchema.DEFAULT_RETURN_LAST_VALUES;


   public XMLTable(String name, XMLTableDescr tableDescr, FileManager fmTable,
                   XMLStoreSchema schema) throws Exception {
      this.name = name;
      this.tableDescr = tableDescr;
      this.fmTable = fmTable;

      // copy global properties
      this.dateFormatString = schema.dateFormatString;
      this.charset = schema.charset;
      this.namespaces = schema.namespaces;
//      this.namespaceAware = schema.namespaceAware;
      this.decimalFormatInput = schema.decimalFormatInput;
      this.decimalFormatOut = schema.decimalFormatOut;
      this.locale = schema.locale;
      this.nullStringInput = schema.nullStringInput;
      this.nullStringOutput = schema.nullStringOutput;
      this.emptyStringAsNull = schema.emptyStringAsNull;
      this.trimBlanks = schema.trimBlanks;
      this.localProps = tableDescr.getLocalProps();
      this.disableDTD = schema.disableDTD;
      this.repeatLastValues = schema.repeatLastValues;
      this.returnLastValues = schema.returnLastValues;
      this.ignoreRowsExpression = schema.ignoreRowsExpression;

      this.readAPI = schema.readAPI;
      this.writeAPI = schema.writeAPI;

      // load local properties for a table
      if (!tableDescr.getLocalProps().isEmpty()) {
         loadLocalPropertiesInVars(tableDescr.getLocalProps());
      }

      if (tableDescr.getTemplateRow() != null) {
         // custom template row is specified
         rowTemplateString = tableDescr.getTemplateRow();
      }

      // init table fields and XPath expressions
      initFields();

      // init namespaces
      if (namespaces != null)
         initNamespacesHash();

      if (nullStringInput != null)
         nullPattern = Pattern.compile(nullStringInput);

      if (readAPI.equalsIgnoreCase(XMLStoreSchema.NUX_API)) {
         // init NUX manager
         readXPathManager = new NuxXPathManager(this);
      } else if (XMLStoreSchema.DEFAULT_SAX_LIBRARY.equals(XMLStoreSchema.XMLDOG_LIBRARY)) {
         readXPathManager = new XMLDogXPathManager(this);
      }
   }

   // loads local table properties in variables
   private void loadLocalPropertiesInVars(Properties props) throws Exception {
      // charset
      if (props.getProperty(CommonDriver2.CHARSET) != null) {
         charset = props.getProperty(CommonDriver2.CHARSET);
      }
      // date format
      if (props.getProperty(CommonDriver2.DATE_FORMAT) != null) {
         dateFormatString = props.getProperty(CommonDriver2.DATE_FORMAT);
      }
      // locale
      if (props.getProperty(XMLStoreSchema.LOCALE) != null) {
         StringTokenizer tokenizer =
                 new StringTokenizer(props.getProperty(XMLStoreSchema.LOCALE), "_");
         locale = new Locale(tokenizer.nextToken(), tokenizer.nextToken());
      }
      // decimal format output
      if (props.getProperty(XMLStoreSchema.DECIMAL_FORMAT_OUTPUT) != null) {
         decimalFormatOut = props.getProperty(XMLStoreSchema.DECIMAL_FORMAT_OUTPUT);
      }
      // decimal format input
      if (props.getProperty(XMLStoreSchema.DECIMAL_FORMAT_INPUT) != null) {
         decimalFormatInput = props.getProperty(XMLStoreSchema.DECIMAL_FORMAT_INPUT);
      }
      // namespaces
      if (props.getProperty(XMLStoreSchema.NAMESPACES) != null) {
         namespaces = props.getProperty(XMLStoreSchema.NAMESPACES);
      }
      // namespaceAware
//      if (props.getProperty(XMLStoreSchema.NAMESPACE_AWARE) != null) {
//         namespaceAware = Boolean.valueOf(props.getProperty(XMLStoreSchema.NAMESPACE_AWARE));
//      }
      // input null string
      if (props.getProperty(XMLStoreSchema.NULL_STRING_INPUT) != null) {
         nullStringInput = props.getProperty(XMLStoreSchema.NULL_STRING_INPUT);
      }
      // output null string
      if (props.getProperty(XMLStoreSchema.NULL_STRING_OUTPUT) != null) {
         nullStringOutput = props.getProperty(XMLStoreSchema.NULL_STRING_OUTPUT);
      }
      // trim blanks
      if (props.getProperty(XMLStoreSchema.TRIM_BLANKS) != null) {
         trimBlanks = Boolean.valueOf(props.getProperty(
                 XMLStoreSchema.TRIM_BLANKS));
      }
      // empty string treated as null
      if (props.getProperty(XMLStoreSchema.EMPTY_STRING_AS_NULL) != null) {
         emptyStringAsNull = Boolean.valueOf(props.getProperty(
                 XMLStoreSchema.EMPTY_STRING_AS_NULL));
      }
      // disable DTD parsing
      if (props.getProperty(XMLStoreSchema.DISABLE_DTD) != null) {
         disableDTD = Boolean.valueOf(props.getProperty(XMLStoreSchema.DISABLE_DTD));
      }
      // repeat last values
      if (props.getProperty(XMLStoreSchema.REPEAT_LAST_VALUES) != null) {
         repeatLastValues = Boolean.valueOf(props.getProperty(XMLStoreSchema.REPEAT_LAST_VALUES));
      }
      // return last values
      if (props.getProperty(XMLStoreSchema.RETURN_LAST_VALUES) != null) {
         returnLastValues = Boolean.valueOf(props.getProperty(XMLStoreSchema.RETURN_LAST_VALUES));
      }
      // rows that should be ignored
      if (props.getProperty(XMLStoreSchema.IGNORE_ROWS) != null) {
         String ignoreRowsString = props.getProperty(XMLStoreSchema.IGNORE_ROWS);
         ignoreRowsExpression = new NumberRangeExpression(ignoreRowsString);
      }
      // read API
      if (props.getProperty(XMLStoreSchema.READ_API) != null) {
         String _readAPI = props.getProperty(XMLStoreSchema.READ_API);
         if (_readAPI.equalsIgnoreCase(XMLStoreSchema.SAX_API)) {
            readAPI = XMLStoreSchema.SAX_API;
         }
         // XOM and NUX are equivalent names
         else if (_readAPI.equalsIgnoreCase(XMLStoreSchema.NUX_API))
            readAPI = XMLStoreSchema.NUX_API;
         else if (_readAPI.equalsIgnoreCase(XMLStoreSchema.XOM_API))
            readAPI = XMLStoreSchema.NUX_API;
         else
            throw new IllegalArgumentException("Unknown value for 'readAPI' parameter");
      }
      // write API
      if (props.getProperty(XMLStoreSchema.WRITE_API) != null) {
         String _readAPI = props.getProperty(XMLStoreSchema.WRITE_API);
         if (_readAPI.equalsIgnoreCase(XMLStoreSchema.SAX_API))
            writeAPI = XMLStoreSchema.SAX_API;
         else
            throw new IllegalArgumentException("Unknown value for 'writeAPI' parameter");
      }
   }

   private void initNamespacesHash() throws StoreException {
      try {
         StringTokenizer st = new StringTokenizer(namespaces, "|");

         while (st.hasMoreElements()) {
            String set = st.nextToken();
            String prefix = set.substring(0, set.indexOf(":")).trim();
            String uri = set.substring(set.indexOf(":") + 1).trim();
            namespacesMap.put(prefix, uri);
         }
      } catch (Exception ex) {
         throw new StoreException(
                 "Incorrect parameter 'namespaces' for the table '" +
                         name + "'. Error was: " + ex.getCause().getMessage(), ex);
      }
   }

   private void initFields() {
      int colSize = tableDescr.getColumns().size();

      fields = new StoreFieldIF[colSize];
      columnSpec = new String[colSize];

      for (int i = 0; i < colSize; i++) {
         XMLColumnDescr colDescr = tableDescr.getColumns().get(i);

         // init XPath expressions for columns
         columnSpec[i] = colDescr.getXPath();

         // init StoreFieldIF instances
         DefaultStoreField storeField = new DefaultStoreField(colDescr.getName(),
                 colDescr.getType());
         if (colDescr.getSize() > 0)
            storeField.setLength(colDescr.getSize());
         if (colDescr.getDecimalCount() > 0)
            storeField.setDecimalCount(colDescr.getDecimalCount());
         fields[i] = storeField;
      }
   }

   public StoreFieldIF[] getFields() {
      return fields;
   }

   public InputStream getInputStream() throws StoreException {
      try {
         // check for file existing
         if (!fmTable.exists())
            throw new StoreException("File '" + tableDescr.getFilePath() +
                    "' doesn't exist");
         return fmTable.getInputStream();
      } catch (Exception ex) {
         throw new StoreException(ex.getMessage(), ex);
      }
   }

   public Date getModificationDate() {
      return null;
   }

   public String getName() {
      return name;
   }

   public FileManager getFileManager() {
      return fmTable;
   }

   // XML -> JDBC
   public Object getSQLObject(StoreFieldIF storeField,
                              String str,
                              DateFormatter dateFormatter,
                              DecimalFormatter decimalFormatter) throws
           StoreException {
      // check for a NULL value
//    if (str == null || str.trim().length() == 0 ||
//        str.trim().equalsIgnoreCase("NULL"))
//      return null;
      if (str == null ||
              nullPattern.matcher(str.trim()).matches() ||
              (storeField.getType() != StoreDataType.VARCHAR && str.trim().isEmpty())) {
         return null;
      }
      // date types (TIMESTAMP, TIME, DATE)
      else if (storeField.getType().isDateType()) {
         Date date;
         try {
            date = dateFormatter.parseDate(str.trim());
         } catch (Exception ex) {
            throw new StoreException("Can't convert {" + str +
                    "} to " + storeField.getType().getName() + " for the column '" +
                    storeField.getName() + "'. The current date format is '" +
                    dateFormatter.getDateFormatString() + "'", ex);
         }
         return date;
      }
      // any number (INTEGER, BIGINT, FLOAT, DOUBLE, BIGDECIMAL)
      else if (storeField.getType().isNumberType()) {
         try {
            if (decimalFormatter != null) {
               // return java.lang.Long for integers, Double for floating point numbers,
               // BigDecimal if setParseBigDecimal() is set to true
               return decimalFormatter.parseDecimal(str.trim());
            } else {
               switch (storeField.getType()) {
                  case INTEGER: {
                     try {
                        return Integer.valueOf(str.trim());
                     } catch (NumberFormatException e) {
                        try {
                           return Double.valueOf(str).intValue();
                        } catch (NumberFormatException e1) {
                           throw new UnexpectedException("Can't convert '" + str + "' to INTEGER", e1);
                        }
                     }
                  }
                  case BIGINT: {
                     try {
                        return Long.valueOf(str.trim());
                     } catch (NumberFormatException e) {
                        try {
                           return Double.valueOf(str).longValue();
                        } catch (NumberFormatException e1) {
                           throw new UnexpectedException("Can't convert '" + str + "' to LONG", e1);
                        }
                     }
                  }
                  case FLOAT:
                     return Float.valueOf(str.trim());
                  case DOUBLE:
                     return Double.valueOf(str.trim());
                  case NUMERIC:
                     return new BigDecimal(str.trim());
                  default:
                     throw new StoreException("Unknown type!");
               }
            }
         } catch (Exception ex) {
            String errorMsg = "Can't convert {" + str +
                    "} to " + storeField.getType().getName() + " type for the column '" +
                    storeField.getName() + "'.";
            if (decimalFormatter != null) errorMsg += " Decimal format is '" +
                    decimalFormatter.getDecimalFormat() + "'";
            throw new StoreException(errorMsg, ex);
         }
      }
      // VARCHAR
      else if (storeField.getType() == StoreDataType.VARCHAR) {
         String _str = str;
         if (trimBlanks)
            _str = str.trim();
         if (emptyStringAsNull && str.trim().isEmpty())
            _str = null;
         return _str;
      }
      // BOOLEAN
      else if (storeField.getType() == StoreDataType.BOOLEAN) {
         if (trueValuePattern.matcher(str.trim()).matches())
            return Boolean.TRUE;
         else
            return Boolean.FALSE;
      }
      // other types
      else
         return str;
   }

   // JDBC -> XML
   public String getXMLString(StoreFieldIF storeField,
                              Object obj,
                              DateFormatter dateFormatter,
                              DecimalFormatter decimalFormatter) throws
           StoreException {
      // NULL value
      if (obj == null) {
         return nullStringOutput;
      }
      // date types (TIMESTAMP, TIME, DATE)
      else if (storeField.getType().isDateType()) {
         return dateFormatter.format((Date) obj);
      }
      // any number (INTEGER, BIGINT, FLOAT, DOUBLE, BIGDECIMAL) and decimalFormat != null
      else if (storeField.getType().isNumberType() && decimalFormatter != null) {
         try {
            return decimalFormatter.formatDecimal(obj);
         } catch (Exception ex) {
            throw new StoreException(ex.getMessage(), ex);
         }
      }
      // other types
      else
         return obj.toString();
   }

   public StoreTableReaderIF getReader() throws StoreException {
      return new XMLTableReader2(this);
   }

   public XMLTableLoaderIF getLoader() throws Exception {
      return readXPathManager.getLoader();
   }

   public XMLTableSaverIF getSaver() throws Exception {
      initWriteXPathManager();
      return writeXPathManager.getSaver();
   }

   public void create(StoreFieldIF[] fields, IndexTableIF[] indexTables) throws StoreException {
      initWriteXPathManager();
      writeXPathManager.create(fields);
   }

   /**
    * init lazily XPath manager for writing (default is XMLDog)
    */
   synchronized void initWriteXPathManager() {
      if (writeXPathManager == null) {
         writeXPathManager = new XMLDogXPathManager(this);
      }
   }

   public StoreTableWriterIF getWriter(StoreFieldIF[] fields) throws
           StoreException {
      throw new UnsupportedOperationException(
              "Method getWriter() not yet implemented.");
   }

   public Properties getTableProperties() {
      return localProps;
   }

   public XMLTableDescr getTableDescription() {
      return tableDescr;
   }
}

