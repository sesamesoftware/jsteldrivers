package com.relationaljunction.jdbc.csv.store;

import java.util.*;
import java.util.regex.Pattern;

import com.relationaljunction.database.*;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.database.io.*;
import com.relationaljunction.jdbc.csv.schema.*;
import com.relationaljunction.utils.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class CSVTable
        extends AbstractStoreTable {
   String tableName = null;
   DirectoryManager dir = null;
   CSVTableDescr tableDescr = null;
   StoreFieldIF[] storeFields = null;
   String columnDetailsRow;
   ArrayList<StoreDataType> columnDetails = new ArrayList<StoreDataType>();
   List<CSVComment> comments = new ArrayList<CSVComment>();

   // File extension to use
   String extension = null;

   // Comment line
   String comment = null;
   List<String> commentTokens = new ArrayList<String>();

   // Field separator to use
   String separator = CSVStoreSchema.DEFAULT_SEPARATOR;

   // Row delimiter to use
   String rowDelimiter = CSVStoreSchema.DEFAULT_ROW_DELIMITER;

   // Should headers be suppressed
   boolean suppressHeaders = CSVStoreSchema.DEFAULT_SUPPRESS;

   //Fixed length type
   boolean fixedLengthType = false;

   // Charset that should be used to read the files
   String charset = null;

   // Date formatter
   String dateFormatString = DateFormatter.DEFAULT_FORMAT_STRING;

   // check for last EOL in EOF
   boolean checkLastEOL = CSVStoreSchema.DEFAULT_CHECK_LAST_EOL;

   // escape EOL in quotes
   boolean escapeEOLInQuotes = CSVStoreSchema.DEFAULT_ESCAPE_EOL_IN_QUOTES;

   // escape separator in quotes
   boolean escapeSeparatorInQuotes = CSVStoreSchema.
           DEFAULT_ESCAPE_SEPARATOR_IN_QUOTES;

   // input NULL string
   String nullString = CSVStoreSchema.DEFAULT_NULL_STRING;
   Pattern nullPattern;

   // output NULL string
   String nullStringToWrite = CSVStoreSchema.DEFAULT_NULL_STRING_TO_WRITE;

   // quote string
   boolean quoteString = CSVStoreSchema.DEFAULT_QUOTE_STRING;

   /**
    * padding char for fixed length files*
    */
   char paddingChar = CSVStoreSchema.DEFAULT_PADDING_CHAR;
   /**
    * trim blanks*
    */
   boolean trimBlanks = CSVStoreSchema.DEFAULT_TRIM_BLANKS;
   /**
    * empty string treated as Null*
    */
   boolean emptyStringAsNull = CSVStoreSchema.DEFAULT_EMPTY_STRING_AS_NULL;
   /**
    * defaultColumnType *
    */
   String defaultColumnType = CSVStoreSchema.DEFAULT_DEFAULT_COLUMN_TYPE;
   StoreDataType defaultColumnJDBCCode = StoreDataType.VARCHAR;

   // Decimal formats. Since 4.01 version
   String decimalFormatInput = CSVStoreSchema.DEFAULT_DECIMAL_FORMAT_INPUT;
   String decimalFormatOut = CSVStoreSchema.DEFAULT_DECIMAL_FORMAT_OUTPUT;

   /**
    * Locale
    */
   public Locale locale = CSVStoreSchema.DEFAULT_LOCALE;
   /**
    * locks *
    */
   boolean lockFiles = false;

   // ignore BOM
   boolean ignoreBOM = CSVStoreSchema.DEFAULT_IGNORE_BOM;

   // write NULL row (i.e. record with NULL values) as a blank line
   boolean nullRowAsBlankLine = CSVStoreSchema.DEFAULT_NULL_ROW_AS_BLANK_LINE;

   // pattern for boolean true value
   Pattern trueValuePattern = AbstractStoreSchema.DEFAULT_TRUE_VALUE_PATTERN;

   // supporting an internal row that holds info about column types. Feature request by a customer.
   boolean supportsColumnDetailsRow = CSVStoreSchema.DEFAULT_SUPPORTS_COLUMN_DETAILS_ROW;

   // rows that should be ignored
   NumberRangeExpression ignoreRowsExpression;

   CSVTable(String tableName, CSVTableDescr tableDescr,
            DirectoryManager csvDir, CSVStoreSchema schema) throws
           StoreException {
      this.tableName = tableName;
      this.dir = csvDir;
      this.tableDescr = tableDescr;

      // copy global properties
      this.extension = schema.getFileExtension();
      this.comment = schema.comment;
      this.commentTokens = schema.commentTokens;
      this.separator = schema.separator;
      this.rowDelimiter = schema.rowDelimiter;
      this.suppressHeaders = schema.suppressHeaders;
      this.fixedLengthType = schema.fixedLengthType;
      this.charset = schema.charset;
      this.dateFormatString = schema.dateFormatString;

      this.decimalFormatInput = schema.decimalFormatInput;
      this.decimalFormatOut = schema.decimalFormatOut;
      this.locale = schema.locale;

      this.checkLastEOL = schema.checkLastEOL;
      this.escapeEOLInQuotes = schema.escapeEOLInQuotes;
      this.escapeSeparatorInQuotes = schema.escapeSeparatorInQuotes;
      this.nullString = schema.nullString;
      this.nullStringToWrite = schema.nullStringToWrite;
      this.quoteString = schema.quoteString;
      this.paddingChar = schema.paddingChar;
      this.trimBlanks = schema.trimBlanks;
      this.emptyStringAsNull = schema.emptyStringAsNull;
      this.defaultColumnType = schema.defaultColumnType;
      this.lockFiles = schema.lockFiles;
      this.ignoreBOM = schema.ignoreBOM;
      this.nullRowAsBlankLine = schema.nullRowAsBlankLine;
      this.supportsColumnDetailsRow = schema.supportsColumnDetailsRow;
      this.ignoreRowsExpression = schema.ignoreRowsExpression;

      // no table description for a fixed-length file
      if (fixedLengthType &&
              (tableDescr == null || tableDescr.getDescribedColumns().isEmpty()))
         throw new StoreException(
                 "Can't find a table description in the schema for the fixed-length file '" +
                         tableName + "'");

      // overwrite params using local props if they exist
      try {
         if (tableDescr != null && !tableDescr.getLocalProps().isEmpty()) {
            loadLocalPropertiesInVars(tableDescr.getLocalProps());
         }
      } catch (Exception ex) {
//         ex.printStackTrace();
         throw new StoreException(
                 "Error while loading local properties for the table: " + tableName +
                         ". Error was: " + ex.getMessage(), ex);
      }

      if (defaultColumnType != null)
         defaultColumnJDBCCode = StoreDataType.getDataTypeByName(defaultColumnType);

      if (nullString != null)
         nullPattern = Pattern.compile(nullString);
   }

   // loads local table properties in variables
   private void loadLocalPropertiesInVars(Properties props) throws Exception {
      if (props != null) {
         // separator character
         if (props.getProperty(CSVStoreSchema.SEPARATOR) != null) {
            String separatorString = tableDescr.getLocalProps().getProperty(
                    CSVStoreSchema.SEPARATOR);
            if (separatorString.trim().equals(CSVStoreSchema.FIXED_LENGTH_KEYWORD))
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
         if (props.getProperty(CSVStoreSchema.SUPPRESS_HEADERS) != null) {
            suppressHeaders = Boolean.valueOf(props.getProperty(
                    CSVStoreSchema.SUPPRESS_HEADERS));
         }
         // charset
         if (props.getProperty(CSVStoreSchema.CHARSET) != null) {
            charset = props.getProperty(CSVStoreSchema.CHARSET);
         }
         // comment line
         if (props.getProperty(CSVStoreSchema.COMMENT_LINE) != null) {
            comment = props.getProperty(CSVStoreSchema.COMMENT_LINE);

            StringTokenizer tokenizer = new StringTokenizer(comment, "|");
            while (tokenizer.hasMoreElements()) {
               commentTokens.add(tokenizer.nextToken());
            }
         }
         // locale
         if (props.getProperty(CSVStoreSchema.LOCALE) != null) {
            StringTokenizer tokenizer =
                    new StringTokenizer(props.getProperty(CSVStoreSchema.LOCALE), "_");
            String language = tokenizer.nextToken();
            String country = tokenizer.nextToken();
            locale = new Locale(language, country);
         }
         // date format
         if (props.getProperty(CSVStoreSchema.DATE_FORMAT) != null) {
            dateFormatString = props.getProperty(CSVStoreSchema.DATE_FORMAT);
         }
         // decimal format output
         if (props.getProperty(CSVStoreSchema.DECIMAL_FORMAT_OUTPUT) != null) {
            decimalFormatOut = props.getProperty(CSVStoreSchema.DECIMAL_FORMAT_OUTPUT);
         }
         // decimal format input
         if (props.getProperty(CSVStoreSchema.DECIMAL_FORMAT_INPUT) != null) {
            decimalFormatInput = props.getProperty(CSVStoreSchema.DECIMAL_FORMAT_INPUT);
         }
         // row delimeter
         if (props.getProperty(CSVStoreSchema.ROW_DELIMITER) != null) {
            rowDelimiter = props.getProperty(CSVStoreSchema.ROW_DELIMITER);
            if (rowDelimiter.contains("\""))
               throw new Exception(
                       "Parameter 'rowDelimiter' must not contain double quote characters");
            rowDelimiter = StringUtils.replaceSubString(rowDelimiter,
                    "\\r", "\r");
            rowDelimiter = StringUtils.replaceSubString(rowDelimiter,
                    "\\n", "\n");
         }
         // check last EOL
         if (props.getProperty(CSVStoreSchema.CHECK_LAST_EOL) != null) {
            checkLastEOL = Boolean.valueOf(props.getProperty(
                    CSVStoreSchema.CHECK_LAST_EOL));
         }
         // escape EOL in quotes
         if (props.getProperty(CSVStoreSchema.ESCAPE_EOL_IN_QUOTES) != null) {
            escapeEOLInQuotes = Boolean.valueOf(props.getProperty(
                    CSVStoreSchema.ESCAPE_EOL_IN_QUOTES));
         }
         // escape separator in quotes
         if (props.getProperty(CSVStoreSchema.ESCAPE_SEPARATOR_IN_QUOTES) != null) {
            escapeSeparatorInQuotes = Boolean.valueOf(props.getProperty(
                    CSVStoreSchema.ESCAPE_SEPARATOR_IN_QUOTES));
         }
         // input null string
         if (props.getProperty(CSVStoreSchema.NULL_STRING) != null) {
            nullString = props.getProperty(CSVStoreSchema.NULL_STRING);
         }
         // output null string
         if (props.getProperty(CSVStoreSchema.NULL_STRING_TO_WRITE) != null) {
            nullStringToWrite = props.getProperty(CSVStoreSchema.NULL_STRING_TO_WRITE);
         }
         // quote string
         if (props.getProperty(CSVStoreSchema.QUOTE_STRING) != null) {
            quoteString = Boolean.valueOf(props.getProperty(CSVStoreSchema.QUOTE_STRING));
         }
         // trim blanks
         if (props.getProperty(CSVStoreSchema.TRIM_BLANKS) != null) {
            trimBlanks = Boolean.valueOf(props.getProperty(
                    CSVStoreSchema.TRIM_BLANKS));
         }
         // empty string treated as null
         if (props.getProperty(CSVStoreSchema.EMPTY_STRING_AS_NULL) != null) {
            emptyStringAsNull = Boolean.valueOf(props.getProperty(
                    CSVStoreSchema.EMPTY_STRING_AS_NULL));
         }
         // null row as a blank line
         if (props.getProperty(CSVStoreSchema.NULL_ROW_AS_BLANK_LINE) != null) {
            nullRowAsBlankLine = Boolean.valueOf(props.getProperty(CSVStoreSchema.NULL_ROW_AS_BLANK_LINE));
         }
         // ignore BOM
         if (props.getProperty(CSVStoreSchema.IGNORE_BOM) != null) {
            ignoreBOM = Boolean.valueOf(props.getProperty(CSVStoreSchema.IGNORE_BOM));
         }
         // padding char
         if (props.getProperty(CSVStoreSchema.PADDING_CHAR) != null) {
            String propPaddingChar = props.getProperty(CSVStoreSchema.PADDING_CHAR);
            if (propPaddingChar.isEmpty())
               paddingChar = ' ';
            else
               paddingChar = props.getProperty(CSVStoreSchema.PADDING_CHAR).charAt(0);
         }
         // default column type
         if (props.getProperty(CSVStoreSchema.DEFAULT_COLUMN_TYPE) != null) {
            defaultColumnType = props.getProperty(CSVStoreSchema.DEFAULT_COLUMN_TYPE);
         }
         // rows that should be ignored
         if (props.getProperty(CSVStoreSchema.IGNORE_ROWS) != null) {
            String ignoreRowsString = props.getProperty(CSVStoreSchema.IGNORE_ROWS);
            ignoreRowsExpression = new NumberRangeExpression(ignoreRowsString);
         }
      }
      // end of the properties analysis
   }

   public Date refreshModificationDate() throws StoreException {
      try {
         fileModificationDate = dir.getFileModificationDate(tableName);
      } catch (Exception ex) {
         throw new StoreException(ex);
      }

      return fileModificationDate;
   }

   public String getName() {
      return tableName;
   }

   public StoreTableReaderIF getReader() throws StoreException {
      return new CSVTableReader(this);
   }

   public void create(StoreFieldIF[] fields, IndexTableIF[] indexTables) throws StoreException {
      if (tableName.equalsIgnoreCase("csv.txt") ||
              tableName.equalsIgnoreCase("txt.txt") ||
              tableName.equalsIgnoreCase("csv.csv") ||
              tableName.equalsIgnoreCase("txt.csv"))
         throw new StoreException("Bad table name. Check your table name in a SQL query. " +
                 "A table name with a file extension must be in double quotes, " +
                 "i.e.: SELECT * FROM \"test.csv\", but not SELECT * FROM test.csv");

      if (dir.exists(tableName))
         throw new StoreException("File '" + tableName +
                 "' already exists");
      try {
         // create a file
         dir.createFile(tableName);
      } catch (Exception ex) {
         throw new StoreException("Can't create file '" + tableName + "'", ex);
      }

      CSVTableWriter writer = new CSVTableWriter(this, fields);
      writer.writeHeader();
      writer.close();
   }

   public StoreTableWriterIF getWriter(StoreFieldIF[] fields) throws
           StoreException {
      return new CSVTableWriter(this, fields);
   }

   public boolean isReadOnly() {
      return false;
   }

   public Properties getTableProperties() {
      if (tableDescr != null)
         return tableDescr.getLocalProps();
      else
         return new Properties();
   }

   public void setCheckLastEOL(boolean checkLastEOL) {
      this.checkLastEOL = checkLastEOL;
   }

   public boolean isCheckLastEOL() {
      return checkLastEOL;
   }

}
