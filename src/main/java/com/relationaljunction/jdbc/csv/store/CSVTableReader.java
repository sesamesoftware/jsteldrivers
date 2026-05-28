package com.relationaljunction.jdbc.csv.store;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.h2.index.Cursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.DefaultStoreField;
import com.relationaljunction.database.DefaultStoreRecord;
import com.relationaljunction.database.StoreDataType;
import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreRecordIF;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.StoreTableReaderIF;
import com.relationaljunction.database.io.FileManager;
import com.relationaljunction.jdbc.csv.schema.CSVColumnDescr;
import com.relationaljunction.jdbc.csv.schema.CSVComment;
import com.relationaljunction.utils.DateFormatter;
import com.relationaljunction.utils.DecimalFormatter;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.StringUtils;
import com.relationaljunction.utils.UnexpectedException;

/**
 * read and process a CSV file
 * there are 3 stages:
 * 1) extracting a CSV row depending on the 'rowDelimiter' property set
 * 2) parsing a row depending on the 'separator' property
 * 3) converting parsed string values to data types (INT, VARCHAR, DATE) specified in the schema file
 * or in a CSV file itself
 */
public class CSVTableReader implements StoreTableReaderIF {
   public final static char[] QUOTE_CHARS = new char[]{
           '"', '`', '\''};

   private final Logger log = LoggerFactory.getLogger("CSVTableReader");

   public static int bufferSize = 8192;

   private CSVTable table = null;
   private boolean isCommentEnabled = false;
   private BufferedReader input;
   private String[] columnNames;
   private String[] values;
   private String firstNonCommentedLine = null;
   private int recCount = 0;
   private FileManager fm = null;
   // very rare CSV files starting with BOM bytes
   private final byte[] bomSignatureArray = new byte[]{(byte) 0xEF, (byte) 0xBB,
           (byte) 0xBF};
   private int currentPosition = 0;
   private DateFormatter[] dateFormatters;
   private DecimalFormatter[] decimalFormatters;

   public CSVTableReader(CSVTable table) throws StoreException {
      this.table = table;

      // if a comment line is defined, turn on comment support option
      if (table.comment != null && !table.comment.isEmpty())
         isCommentEnabled = true;

      // check for file existing
      if (!table.dir.exists(table.tableName)) {
         String exceptionMessage = "File '" + table.tableName + "' doesn't exist!";
         if (table.tableName.equalsIgnoreCase("csv.txt") ||
                 table.tableName.equalsIgnoreCase("txt.txt") ||
                 table.tableName.equalsIgnoreCase("csv.csv") ||
                 table.tableName.equalsIgnoreCase("txt.csv"))
            exceptionMessage += " Check your table name in a SQL query. " +
                    "A table name with a file extension must be in double quotes, " +
                    "i.e.: SELECT * FROM \"test.csv\", but not SELECT * FROM test.csv";
         throw new StoreException(exceptionMessage);
      }

      try {
         fm = table.dir.getFileManager(table.tableName);

         if (table.lockFiles) {
            InputStream is = new BufferedInputStream(fm.getInputStream(true));

            // check and ignore byte order mark (BOM)
            if (table.ignoreBOM)
               checkBOM(is);

            // get a file stream using charset parameter
            // file lock is shared
            if (table.charset != null) {
               input = new BufferedReader(new InputStreamReader(is, table.charset),
                       bufferSize);
            } else {
               input = new BufferedReader(new InputStreamReader(is), bufferSize);
            }
         } else {
            InputStream is = new BufferedInputStream(fm.getInputStream());

            // check and ignore byte order mark (BOM)
            if (table.ignoreBOM)
               checkBOM(is);

            // get a file stream using charset parameter
            // no file locks
            if (table.charset != null) {
               input = new BufferedReader(new InputStreamReader(is, table.charset),
                       bufferSize);
            } else {
               input = new BufferedReader(new InputStreamReader(is), bufferSize);
            }
         }

//      table.fileModificationDate = fm.getModificationTime();

         // load columns from a file
         loadColumns();
         initFields();
         prepareUtilClasses();
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException(ex);
      }
   }

   /**
    * initialize utility classes for multi-thread environment,
    * because DateFormatter and DecimalFormatter are not thread safe
    * and they cannot be used as shared objects
    */
   private void prepareUtilClasses() {
      dateFormatters = new DateFormatter[table.storeFields.length];
      decimalFormatters = new DecimalFormatter[table.storeFields.length];

      for (int i = 0; i < table.storeFields.length; i++) {
         StoreFieldIF storeField = table.storeFields[i];

         // init date formatters
         if (storeField.getType().isDateType()) {
            if (table.tableDescr != null) {
               CSVColumnDescr columnDescr =
                       table.tableDescr.findColumnDescrByName(storeField.getName());

               if (columnDescr != null && columnDescr.getDateFormatString() != null) {
                  // decimal format is set separately for the specific column
                  dateFormatters[i] = new DateFormatter(columnDescr.getDateFormatString(), table.locale);
               }
            }

            // date format is inherited from a table or a schema property.
            if (dateFormatters[i] == null && table.dateFormatString != null) {
               dateFormatters[i] = new DateFormatter(table.dateFormatString, table.locale);
            }
         }
         // init decimal formatters
         else if (storeField.getType().isNumberType()) {
            if (table.tableDescr != null) {
               CSVColumnDescr columnDescr = table.tableDescr.findColumnDescrByName(storeField.getName());

               if (columnDescr != null && columnDescr.getDecimalFormatInput() != null) {
                  // decimal format is set separately for the specific column
                  decimalFormatters[i] = new DecimalFormatter(columnDescr.getDecimalFormatInput(),
                          columnDescr.getDecimalFormatOutput(), table.locale);
               }
            }

            // decimal format is inherited from a table or a schema property.
            if (decimalFormatters[i] == null && table.decimalFormatInput != null) {
               decimalFormatters[i] = new DecimalFormatter(table.decimalFormatInput,
                       table.decimalFormatOut, table.locale);
            }

            // set BigDecimal parsing for NUMERIC (BIGDECIMAL) types
            if (decimalFormatters[i] != null && storeField.getType() == StoreDataType.NUMERIC) {
               decimalFormatters[i].setParseBigDecimal(true);
            }
         }
      }
   }

   private void checkBOM(InputStream is) throws IOException {
      is.mark(10);
      byte[] bArray = new byte[3];
      is.read(bArray);

      if (!Arrays.equals(bomSignatureArray, bArray))
         is.reset();
   }

   public StoreTableIF getStoreTable() {
      return table;
   }

   public Cursor getIndexCursor(List<String> columnNames) {
      return null;
   }

   private void loadColumns() throws Exception {
      if (table.suppressHeaders && table.tableDescr == null) {
         // No column names available.
         // No table description available.
         recCount++;
         firstNonCommentedLine = readNoCommentedLine();
         // read the first line to determine a number of columns
         columnNames = new String[parseCustomLine(firstNonCommentedLine).length];
      } else if (table.suppressHeaders && table.tableDescr != null &&
              !table.fixedLengthType) {
         // No column names available.
         // Get columns from the table description
         recCount++;
         firstNonCommentedLine = readNoCommentedLine();

         // read the first line to determine a number of columns
         columnNames = new String[parseCustomLine(firstNonCommentedLine).length];

         List<CSVColumnDescr> describedCols = table.tableDescr.getDescribedColumns();

         if (describedCols.size() <= columnNames.length) {
            // First case... The number of described columns less or equal to
            // quantity of columns in the file
            for (CSVColumnDescr colDescr : describedCols) {

               if (colDescr.getPos() == -1)
                  throw new Exception(
                          "Can't find the 'position' attribute in the schema for the column: " +
                                  colDescr.getName() +
                                  ". It is required if you set 'suppressHeaders' to 'true'");

               if (colDescr.getPos() < 1 || colDescr.getPos() > columnNames.length)
                  throw new Exception(
                          "Incorrect 'position' attribute in the schema for the column: " +
                                  colDescr.getName() + ". Position =" + colDescr.getPos());

               columnNames[colDescr.getPos() - 1] = colDescr.getName();
            }
         } else {
            // Second case... The number of described columns more then
            // number of columns in a file
            if (describedCols.isEmpty())
               throw new Exception(
                       "Can't find column definitions in the schema for the file " +
                               table.tableName);

            // seek the maximum value of columns position specified in the schema
            int maxPos = 1;

            for (Object describedCol : describedCols) {
               CSVColumnDescr colDescr = (CSVColumnDescr) describedCol;

               if (colDescr.getPos() == -1)
                  throw new Exception(
                          "Can't find the position attribute in the schema for the column: " +
                                  colDescr.getName() +
                                  ". It is required if you set 'suppressHeaders' to 'true'");

               if (colDescr.getPos() < 1)
                  throw new Exception(
                          "Incorrect position attribute in the schema for the column: " +
                                  colDescr.getName() + ". Position =" + colDescr.getPos());

               if (colDescr.getPos() > 1)
                  maxPos = colDescr.getPos();
            }

            columnNames = new String[maxPos];
            for (Object describedCol : describedCols) {
               CSVColumnDescr colDescr = (CSVColumnDescr) describedCol;
               columnNames[colDescr.getPos() - 1] = colDescr.getName();
            }
         }
      } else if (table.suppressHeaders && table.tableDescr != null &&
              table.fixedLengthType) {
         // fixed-length file.
         // No header.
         List<CSVColumnDescr> describedCols = table.tableDescr.getDescribedColumns();
         columnNames = new String[describedCols.size()];
         for (int i = 0; i < describedCols.size(); i++) {
            columnNames[i] = describedCols.get(i).getName();
         }
      } else {
         // column names are available.
         // read the first line (header) to determine the columns
         String headerLine = readNoCommentedLine();
         String[] parsedColumns = parseCustomLine(headerLine);
         columnNames = new String[parsedColumns.length];

         for (int i = 0; i < columnNames.length; i++) {
            columnNames[i] = (String) getJDBCObject(StoreDataType.VARCHAR, parsedColumns[i], null, null);
         }

         if (table.supportsColumnDetailsRow) {
            readColumnDetailsRow();
         }
      }
   }

   /**
    * set data types for columns using the schema file
    *
    * @throws Exception
    */
   void initFields() throws Exception {
      table.storeFields = new StoreFieldIF[columnNames.length];

      for (int i = 0; i < columnNames.length; i++) {
         String colName = columnNames[i];

         int size = -1;
         int decimalCount = -1;
         StoreDataType sqlType = table.defaultColumnJDBCCode;
         String sourceTypeName = null;

         if (table.tableDescr != null) {
            CSVColumnDescr colDescr;

            // first try to find a column by position
            colDescr = table.tableDescr.findColumnDescrByPos(i + 1);

            if (colDescr == null && colName != null) {
               // a column description is not found. Try to find it by name.
               colDescr = table.tableDescr.findColumnDescrByName(colName);
            }

            if (colDescr != null) {
               // override a column name by the name in a column description
               if (colDescr.getName() != null && !colDescr.getName().equalsIgnoreCase(colName)) {
                  colName = colDescr.getName();
               } else if (colDescr.getName() == null) {
                  colDescr.setName(colName);
               }

               if (colDescr.getAlias() != null) {
                  colName = colDescr.getAlias();
               }

               sqlType = colDescr.getColType();
               size = colDescr.getSize();
               decimalCount = colDescr.getDecimalCount();
               sourceTypeName = colDescr.getSourceTypeName();
            }
         }

         // if a column name is NULL, define column as COLUMN1,COLUMN2 etc
         if (colName == null)
            colName = "COLUMN" + (i + 1);

         DefaultStoreField storeField = new DefaultStoreField(colName, sqlType);
         // ####added 3-Sep-2007
         storeField.setSourceTypeName(sourceTypeName);
         // ####added 6-Nov-2006
         // set size for the column
         if (size > 0)
            storeField.setLength(size);
         // ####added 11-Dec-2009
         // set decimal count for the column
         if (decimalCount > 0)
            storeField.setDecimalCount(decimalCount);

         table.storeFields[i] = storeField;
      }

      // apply internal column details row if exists
      if (!table.columnDetails.isEmpty()) {
         int minSize = table.storeFields.length >= table.columnDetails.size() ?
                 table.columnDetails.size() : table.storeFields.length;

         for (int i = 0; i < minSize; i++) {
            table.storeFields[i].setType(table.columnDetails.get(i));
         }
      }
   }

   /**
    * reads an internal row that holds info about column types.
    * It should be on the second row and start with '#' char
    */
   private void readColumnDetailsRow() {
      try {
         // looks for column details row starting with '#' char
         input.mark(1);
         char readChar = (char) input.read();

         input.reset();

         if (readChar == -1) {
            // EOF
            return;
         }

         if (readChar == '#') {
            // get column details
            String line = readCustomLine();
            if (line == null) throw new UnexpectedException("Unexpected end of file");
            table.columnDetailsRow = line.substring(1);
            StringTokenizer stringTokenizer = new StringTokenizer(table.columnDetailsRow, ",");

            OtherUtils.writeLogInfo(log, "found the column details row: " + table.columnDetailsRow);

            while (stringTokenizer.hasMoreElements()) {
               // parse a data type
               String dataType = stringTokenizer.nextToken();
               if (!dataType.trim().isEmpty())
                  table.columnDetails.add(StoreDataType.getDataTypeByName(dataType));
            }
         }
      } catch (Exception e) {
         throw new UnexpectedException("Error while reading column details row", e);
      }
   }

   public int getRecordCount() {
      if (recCount > 0) return recCount;

      int size = 0;
      try {
         while ((readNoCommentedLine()) != null)
            size++;
//      System.out.println(size);
      } catch (IOException ex) {
         throw new RuntimeException(
                 "[CSV File] Can't read a record with the index " + size +
                         " from the file '" + table.getName() + "'");
      }
      return size;
   }

   boolean nextLine() throws StoreException {
      values = new String[columnNames.length];
      String dataLine;

      try {
         if (table.suppressHeaders && firstNonCommentedLine != null) {
            // table has no column header and the first line is a data line
            dataLine = firstNonCommentedLine;
            // set to null to prevent a perpetual loop
            firstNonCommentedLine = null;
         } else {
            // read a new line of data from an input stream.
            recCount++;
            dataLine = readNoCommentedLine();
         }
      } catch (IOException ex) {
         throw new StoreException("Error while reading " + recCount +
                 " record in the file" + fm.getPath() +
                 ". Error was: " + ex.getMessage(), ex);
      }

      // end of file
      if (dataLine == null)
         return false;

      String[] parsedValues;
      try {
         parsedValues = parseCustomLine(dataLine);
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new StoreException(ex);
      }

       System.arraycopy(parsedValues, 0, values, 0, ((parsedValues.length > columnNames.length) ?
               columnNames.length : parsedValues.length));

      return true;
   }

   private String readNoCommentedLine() throws IOException {
      String line;

      if (isCommentEnabled) {
         while ((line = readCustomLine()) != null) {
            // ignore comments
            boolean commentFound = false;

            for (String comment : table.commentTokens) {
               if (line.startsWith(comment)) {
                  table.comments.add(new CSVComment(line, currentPosition));
                  commentFound = true;
                  break;
               }
            }

            if (!commentFound) break;
         }

         return line;
      } else
         return readCustomLine();
   }

   private String readLineUsingCustomDelimiter() throws IOException {
      StringBuilder result = new StringBuilder();
      boolean inQuoted = false;

      for (; ; ) {

         int charReaded = input.read();
         if (charReaded == -1) {
            if (result.length() == 0)
               return null;
            break;
         }
         char ch = (char) charReaded;

         if (!inQuoted && ch == table.rowDelimiter.charAt(0)) {
            StringBuilder nextChars = new StringBuilder();
            nextChars.append(ch);
            boolean eof = false;
            int delimCount = 1;

            while (delimCount < table.rowDelimiter.length()) {
               input.mark(1);
               int nextCharReaded = input.read();
               if (nextCharReaded == -1) {
                  result.append(nextChars);
                  eof = true;
                  break;
               }
               char nextChar = (char) nextCharReaded;
               if (nextChar != table.rowDelimiter.charAt(delimCount)) {
                  result.append(nextChars);
                  input.reset();
                  break;
               } else {
                  nextChars.append(nextChar);
                  delimCount++;
               }
            }
            // check for EOF or EOL
            if (eof || delimCount == table.rowDelimiter.length())
               break;
         } else {
            result.append(ch);

            if (ch == '"') {
               if (!inQuoted) {
                  inQuoted = true;
               } else {
                  input.mark(1);
                  int nextCharReaded = input.read();
                  if (nextCharReaded == -1) {
                     if (result.length() == 0)
                        return null;
                     break;
                  }
                  char nextChar = (char) nextCharReaded;

                  if (nextChar == '"') {
                     result.append(nextChar);
                  } else {
                     inQuoted = false;
                     input.reset();
                  }
               }
            } // if
         }
      } // for

      return result.toString().trim();
   }

   private String readLineEscapingEOL() throws IOException {
      StringBuilder result = new StringBuilder();
      boolean inQuoted = false;

      for (; ; ) {

         // check EOF
         int charReaded = input.read();
         if (charReaded == -1) {
            if (result.length() == 0)
               return null;
            break;
         }

         // current char
         char ch = (char) charReaded;

         if (!inQuoted && ch == '\r') {
            input.mark(1);
            int nextCharReaded = input.read();
            if (nextCharReaded == -1) {
               if (result.length() == 0)
                  return null;
               break;
            }

            char nextChar = (char) nextCharReaded;

            if (nextChar != '\n')
               input.reset();
            break;
         } else if (!inQuoted && ch == '\n') {
            // EOL encountered
            break;
         } else {
            result.append(ch);

            if (ch == '"') {
               if (!inQuoted) {
                  inQuoted = true;
               } else {
                  input.mark(1);
                  int nextCharReaded = input.read();

                  // check EOF
                  if (nextCharReaded == -1) {
                     if (result.length() == 0)
                        return null;
                     break;
                  }

                  char nextChar = (char) nextCharReaded;

                  // check for the next double quote
                  if (nextChar == '"') {
                     result.append(nextChar);
                  } else {
                     inQuoted = false;
                     input.reset();
                  }
               }
            } // if
         }
      } // for

      String line = result.toString();

      OtherUtils.writeTraceInfo(log, "##### reading line: '", line, "'");

      return line;
   }

   private String readCustomLine() throws IOException {
//      System.out.println("cur pos" + currentPosition);
      String line;

      while (true) {
         currentPosition++;

         if (table.rowDelimiter.equals(CSVStoreSchema.DEFAULT_ROW_DELIMITER)) {
            // row delimiter is default
            if (!table.escapeEOLInQuotes) {
               line = input.readLine();
            } else {
               line = readLineEscapingEOL();
            }
         } else {
            // row delimiter is custom
            line = readLineUsingCustomDelimiter();
         }

         // EOF
         if (line == null) break;

         boolean isRowIgnored = table.ignoreRowsExpression != null &&
                 table.ignoreRowsExpression.matches(currentPosition);

         // row is not ignored
         if (!isRowIgnored) break;

         // check if all records below are ignored too
         if (table.ignoreRowsExpression.isNumberInRangeToMaximum(currentPosition)) {
            // all records below should be ignored, so it can be treated as EOF
            line = null;
            break;
         }
      }
/*
      do {
         currentPosition++;

         if (table.rowDelimiter.equals(CSVStoreSchema.DEFAULT_ROW_DELIMITER)) {
            // row delimiter is default
            if (!table.escapeEOLInQuotes) {
               line = input.readLine();
            } else {
               line = readLineEscapingEOL();
            }
         } else {
            // row delimiter is custom
            line = readLineUsingCustomDelimiter();
         }
      } while (isRowIgnored(line, currentPosition));
*/

      return line;
   }

   /**
    * check if a record should be ignored
    *
    * @param line
    * @param currentPosition
    * @return
    */
   private boolean isRowIgnored(String line, int currentPosition) {
      return table.ignoreRowsExpression != null &&
              line != null &&
              table.ignoreRowsExpression.matches(currentPosition);
   }

   private String[] parseCustomLine(String line) throws Exception {
      if (table.fixedLengthType)
         return parseFixedLengthLine(line);
      else
         return parseCsvLine(line);
   }

   protected String[] parseFixedLengthLine(String line) throws Exception {
      ArrayList<String> values = new ArrayList<String>();
      List<CSVColumnDescr> fixedCols = table.tableDescr.getDescribedColumns();

      for (CSVColumnDescr colDescr : fixedCols) {
         int beginPos = colDescr.getBeginPos();
         if (beginPos <= -1)
            throw new Exception(
                    "Can't find the 'begin' attribute in the schema file for the column '" +
                            colDescr.getName() + "' of the table '" +
                            table.tableDescr.getTableName() +
                            "'");

         int endPos = colDescr.getEndPos();

         if (beginPos <= -1)
            throw new Exception(
                    "Can't find the 'end' attribute in the schema file for the column '" +
                            colDescr.getName() + "' of the table '" +
                            table.tableDescr.getTableName() +
                            "'");

         try {
            String value = null;

            if (beginPos <= line.length()) {
               if (beginPos > endPos)
                  throw new Exception(
                          "Error: begin position > end position in the schema for column '" +
                                  colDescr.getName() + "'");
               if (endPos <= line.length())
                  value = line.substring(beginPos - 1, endPos);
               else
                  value = line.substring(beginPos - 1);
            }

            if (value == null)
               values.add(null);
            else {
               if (table.trimBlanks)
                  value = value.trim();
               if (table.emptyStringAsNull && value.trim().isEmpty())
                  values.add(null);
               else
                  values.add(value);
            }

//        values.add( (value == null || value.length() == 0) ? null : value);
         } catch (StringIndexOutOfBoundsException ex) {
            throw new Exception("Can't extract the value from line '" + line +
                    "' with begin position=" +
                    beginPos + " and end position=" + endPos);
         }
      }

      return values.toArray(new String[0]);
   }

   private String[] parseCsvLine(String line) throws Exception {
      if (line == null) return new String[0];

      ArrayList<String> values;
      if (columnNames != null && columnNames.length > 10)
         values = new ArrayList<String>(columnNames.length + 3);
      else
         values = new ArrayList<String>(10);

      boolean inQuotedString = false;
      StringBuilder value = new StringBuilder();
      int currentPos = 0;

    if(log.isTraceEnabled())
      log.trace("Parsing CSV record: "+line);

      while (currentPos < line.length()) {
         char currentChar = line.charAt(currentPos);

         if (currentChar == '"' && table.escapeSeparatorInQuotes) {
               inQuotedString = !inQuotedString;

            // anyway append a quote to a value
            value.append(currentChar);
         } else if (currentPos + table.separator.length() - 1 < line.length() &&
                 line.substring(currentPos, currentPos + table.separator.length()).
                         equals(table.separator)) {
            // current char is separator!

            if (inQuotedString && table.escapeSeparatorInQuotes) {
               // separator inside quotes
               value.append(currentChar);
            } else {
               values.add(value.toString());
               value = new StringBuilder();
            }

            currentPos += table.separator.length() - 1;
         } else {
            // other char
            value.append(currentChar);
         }

         currentPos++;
      } // while

      // end of line
      values.add(value.toString());

      return values.toArray(new String[0]);
   }

   public StoreRecordIF nextRecord() throws StoreException {
      Object[] objs;

      objs = new Object[table.storeFields.length];

      try {
         if (!nextLine())
            return null;

         for (int i = 0; i < values.length; i++) {
            objs[i] = getJDBCObject(table.storeFields[i].getType(), values[i],
                    dateFormatters[i], decimalFormatters[i]);
         }

      } catch (Exception ex) {
//      ex.printStackTrace();
         throw new StoreException("[CSV File '" + table.getName() + "'] " +
                 ex.getMessage(), ex);
      }

      return new DefaultStoreRecord(table.storeFields, objs);
   }

   // CSV -> JDBC
   Object getJDBCObject(StoreDataType dataType,
                        String str,
                        DateFormatter dateFormatter,
                        DecimalFormatter decimalFormatter) throws
           StoreException {
      // NULL value excluding text types analysis
      // nullPattern should be here, for example if a user set "NULL" value (in double quotes) as NULL
      if (str == null ||
              table.nullPattern.matcher(str.trim()).matches() ||
              (!dataType.isTextType() && str.trim().isEmpty())) {
         return null;
      }
      // date types (TIMESTAMP, TIME, DATE)
      else if (dataType.isDateType()) {
         java.util.Date date;
         try {
            date = dateFormatter.parseDate(str.trim());
         } catch (Exception ex) {
            throw new StoreException("Can't convert {" + str +
                    "} to " + dataType.getName() + " type for the column '" +
                    dataType.getName() + "'", ex);
         }
         return date;
      }
      // any number (INTEGER, BIGINT, FLOAT, DOUBLE, BIGDECIMAL)
      else if (dataType.isNumberType()) {
         try {
            if (decimalFormatter != null) {
               // return java.lang.Long for integers, Double for floating point numbers,
               // BigDecimal if setParseBigDecimal() is set to true
               return decimalFormatter.parseDecimal(str.trim());
            } else {
               switch (dataType) {
                  case INTEGER: {
                     String result = str.trim();

                     if (StringUtils.isQuoted(result, QUOTE_CHARS)) {
                        // unquote string
                        result = StringUtils.unquote(result, QUOTE_CHARS, false);
                     }

                     try {
                        return Integer.valueOf(result);
                     } catch (NumberFormatException e) {
                        try {
                           return Double.valueOf(result).intValue();
                        } catch (NumberFormatException e1) {
                           throw new UnexpectedException("Can't convert '" + str + "' to INTEGER", e1);
                        }
                     }
                  }
                  case BIGINT: {
                     String result = str.trim();

                     if (StringUtils.isQuoted(result, QUOTE_CHARS)) {
                        // unquote string
                        result = StringUtils.unquote(result, QUOTE_CHARS, false);
                     }

                     try {
                        return Long.valueOf(result);
                     } catch (NumberFormatException e) {
                        try {
                           return Double.valueOf(result).longValue();
                        } catch (NumberFormatException e1) {
                           throw new UnexpectedException("Can't convert '" + str + "' to LONG", e1);
                        }
                     }
                  }
                  case FLOAT: {
                     String result = str.trim();

                     if (StringUtils.isQuoted(result, QUOTE_CHARS)) {
                        // unquote string
                        result = StringUtils.unquote(result, QUOTE_CHARS, false);
                     }

                     return Float.valueOf(result);
                  }
                  case DOUBLE: {
                     String result = str.trim();

                     if (StringUtils.isQuoted(result, QUOTE_CHARS)) {
                        // unquote string
                        result = StringUtils.unquote(result, QUOTE_CHARS, false);
                     }

                     return Double.valueOf(result);
                  }
                  case NUMERIC: {
                     String result = str.trim();

                     if (StringUtils.isQuoted(result, QUOTE_CHARS)) {
                        // unquote string
                        result = StringUtils.unquote(result, QUOTE_CHARS, false);
                     }

                     return new BigDecimal(result);
                  }
                  default:
                     throw new StoreException("Unknown type!");
               }
            }
         } catch (Exception ex) {
            String errorMsg = "Can't convert {" + str +
                    "} to " + dataType.getName() + " type for the column '" +
                    dataType.getName() + "'.";
            if (decimalFormatter != null) errorMsg += " Decimal format is '" +
                    decimalFormatter.getDecimalFormat() + "'";
            throw new StoreException(errorMsg, ex);
         }
      }
      // VARCHAR
      else if (dataType == StoreDataType.VARCHAR) {
         if (table.trimBlanks)
            str = str.trim();

         // unquote and unduplicate internal quotes
         if (StringUtils.isDoubleQuoted(str)) {
            str = StringUtils.unDoubleQuote(str);
         }
         // if it is not quoted, check for NULL
//         else if (table.nullPattern.matcher(str).matches()) {
//            str = null;
//         }
         // emptyStringAsNull for all empty strings including quoted empty strings
         if (table.emptyStringAsNull && str != null && str.isEmpty()) {
            str = null;
         }

         return str;
      }
      // BOOLEAN
      else if (dataType == StoreDataType.BOOLEAN) {
         if (table.trueValuePattern.matcher(str.trim()).matches())
            return Boolean.TRUE;
         else
            return Boolean.FALSE;
      }
      // other types
      else
         return str;
   }

   public StoreFieldIF[] getFields() {
      return table.storeFields;
   }

   public void close() {
      try {
         if (table.lockFiles) {
            fm.unlock();
         }
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new UnexpectedException("Unexpected error: can't unlock the file '" +
                 fm.getPath() + "'. Error was: " + ex.getMessage());
      }

      try {
         input.close();
      } catch (Exception ex) {
         ex.printStackTrace();
      }

      input = null;
      firstNonCommentedLine = null;
      table = null;
   }

}
