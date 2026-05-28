package com.relationaljunction.jdbc.csv.store;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.AbstractStoreTableWriter;
import com.relationaljunction.database.StoreDataType;
import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreRecordIF;
import com.relationaljunction.database.StoreRecordsIF;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.io.FileManager;
import com.relationaljunction.jdbc.csv.schema.CSVColumnDescr;
import com.relationaljunction.jdbc.csv.schema.CSVComment;
import com.relationaljunction.utils.DateFormatter;
import com.relationaljunction.utils.DecimalFormatter;
import com.relationaljunction.utils.StringUtils;
import com.relationaljunction.utils.UnexpectedException;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class CSVTableWriter
        extends AbstractStoreTableWriter {
   private final Logger log = LoggerFactory.getLogger("CSVTableWriter");

   private CSVTable table = null;
   private BufferedWriter output = null;
   private FileManager fm = null;
   private DateFormatter[] dateFormatters;
   private DecimalFormatter[] decimalFormatters;
   private int currentRowPosition = 1;

   public CSVTableWriter(CSVTable table, StoreFieldIF[] fields) throws
           StoreException {
      this.table = table;

      if (fields == null && !table.dir.exists(table.tableName))
         throw new StoreException("File " + table.tableName +
                 " doesn't exist");

      // open a stream
      try {
         fm = table.dir.getFileManager(table.tableName);

         if (table.lockFiles) {
            if (table.charset != null) {
               output = new BufferedWriter(new OutputStreamWriter(table.dir.
                       getFileManager(table.tableName).getOutputStream(fields == null, false),
                       table.charset));
            } else {
               output = new BufferedWriter(new OutputStreamWriter(table.dir.
                       getFileManager(table.tableName).getOutputStream(fields == null, false)));
            }
         } else {
            if (table.charset != null) {
               output = new BufferedWriter(new OutputStreamWriter(table.dir.
                       getFileManager(
                               table.tableName).getOutputStream(fields == null), table.charset));
            } else {
               output = new BufferedWriter(new OutputStreamWriter(table.dir.
                       getFileManager(table.tableName).getOutputStream(fields == null)));
            }
         }

         if (fields != null) {
            // table is empty
            table.checkLastEOL = false;
            table.storeFields = fields;
         }

         prepareUtilClasses();
      } catch (Exception ex) {
         throw new StoreException(ex);
      }
   }

   /**
    * initialize utility classes for multi-thread environment
    */
   private void prepareUtilClasses() {
      dateFormatters = new DateFormatter[table.storeFields.length];
      decimalFormatters = new DecimalFormatter[table.storeFields.length];

      for (int i = 0; i < table.storeFields.length; i++) {
         StoreFieldIF storeField = table.storeFields[i];

         // init date formatters
         if (storeField.getType().isDateType()) {
            if (table.tableDescr != null) {
               CSVColumnDescr columnDescr = table.tableDescr.findColumnDescrByName(storeField.getName());

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

   public void writeHeader() throws StoreException {
      try {
         if (!table.suppressHeaders)
            insertColumns(table.storeFields);
      } catch (Exception e) {
         throw new StoreException("[CSV file] Can't write the header. Error was: " + e.getMessage(), e);
      }
   }

   public StoreTableIF getStoreTable() {
      return table;
   }

   public void clearRecords() throws StoreException {
   }

   // checks for last EOL existing at the end of the file
   private void checkForLastEOL() throws Exception {
      boolean addNewLine = false;
      RandomAccessFile raf = table.dir.getFileManager(table.tableName).
              getRandomAccess("r");
      long fileLength = raf.length();

      // check for a line separator at the end of the file
      if (fileLength >= table.rowDelimiter.length()) {
         for (int i = 0; i < table.rowDelimiter.length(); i++) {
            raf.seek(fileLength - table.rowDelimiter.length() + i);
            if (raf.read() != table.rowDelimiter.charAt(i)) {
               addNewLine = true;
               break;
            } else
               addNewLine = false;
         }
      } else if (fileLength > 0 && fileLength < table.rowDelimiter.length())
         addNewLine = true;

      raf.close();
      // add a new line before inserting new records
      if (addNewLine)
         output.newLine();

      // EOL is checked, so we can disable it.
      table.checkLastEOL = false;
   }

   private void insertColumns(StoreFieldIF[] fields) throws Exception {
      if (table.fixedLengthType) {
         // ### fixed-length file type ###
         insertColumnsInFixed(fields);
      } else
         // ### CSV file type ###
         insertColumnsInCSV(fields);
   }

   // insert columns in the text file
   private void insertColumnsInCSV(StoreFieldIF[] fields) throws Exception {
      StringBuilder result = new StringBuilder();
      for (int i = 0; i < fields.length; i++) {
         String fieldName = fields[i].getName();
         if (fieldName.contains("\""))
            fieldName = "\"" +
                    StringUtils.duplicateDoubleQuote2(fieldName) + "\"";
         result.append(fieldName);
         if (i < fields.length - 1)
            result.append(table.separator);
      }
      output.write(result + table.rowDelimiter);
      currentRowPosition++;

      // if a CSV file has own column details row
      if (table.columnDetailsRow != null) {
         output.write("#" + table.columnDetailsRow + table.rowDelimiter);
         currentRowPosition++;
      }
   }

   // insert a columns row in a fixed-length file
   private void insertColumnsInFixed(StoreFieldIF[] fields) throws
           Exception {
      List<CSVColumnDescr> fixedCols = table.tableDescr.getDescribedColumns();
      String result = "";

      for (CSVColumnDescr colDescr : fixedCols) {
         int begPos = colDescr.getBeginPos();
         int endPos = colDescr.getEndPos();
         String colName = colDescr.getName();
         int pos = colDescr.getPos();

         if (colName == null)
            colName = fields[pos - 1].getName();

         result = StringUtils.replace(result, begPos - 1,
                 endPos - 1, colName, table.paddingChar);
      }

      output.write(result + table.rowDelimiter);

      currentRowPosition++;
   }


   // insert a data row in a text file
   private void insertRecordInCSV(StoreRecordIF rec) throws Exception {
      boolean nullRow = true;

      if (table.nullRowAsBlankLine) {
         for (int i = 0; i < rec.getSize(); i++) {
            if (rec.getObject(i) != null) {
               nullRow = false;
               break;
            }
         }

         if (nullRow) {
            output.write(table.rowDelimiter);
            currentRowPosition++;
         }
      }

      if (!table.nullRowAsBlankLine || !nullRow) {
         appendCSVRecord(rec);
         currentRowPosition++;
      }
   }

   private void appendCSVRecord(StoreRecordIF rec) throws StoreException, IOException {
      StringBuilder result = new StringBuilder();

      for (int i = 0; i < rec.getSize(); i++) {
         result.append(getCSVString(rec.getFields()[i], rec.getObject(i),
                 table.quoteString, table.nullStringToWrite, dateFormatters[i], decimalFormatters[i]));

         if (i < rec.getSize() - 1)
            result.append(table.separator);
      }

      output.write(result + table.rowDelimiter);
   }

   // insert a data row in a fixed-length file
   private void insertRecordInFixed(StoreRecordIF rec) throws
           Exception {
      List<CSVColumnDescr> fixedCols = table.tableDescr.getDescribedColumns();
      String result = "";

      for (int i = 0; i < fixedCols.size(); i++) {
         CSVColumnDescr colDescr = fixedCols.get(i);
         int begPos = colDescr.getBeginPos();
         int endPos = colDescr.getEndPos();
//         int pos = colDescr.getPos();

         String value = getCSVString(rec.getFields()[i], rec.getObject(i),
                 false, "", dateFormatters[i], decimalFormatters[i]);

         if (value.length() > (endPos - begPos + 1))
            throw new StoreException(
                    "Value size is too big for a fixed-length file: '" + value +
                            "'");
         result = StringUtils.replace(result, begPos - 1,
                 endPos - 1, value, table.paddingChar);
      }
      output.write(result + table.rowDelimiter);
      currentRowPosition++;
   }

   // JDBC -> CSV
   String getCSVString(StoreFieldIF storeField,
                       Object obj,
                       boolean quoteAllString,
                       String nullStringToWrite,
                       DateFormatter dateFormatter,
                       DecimalFormatter decimalFormatter) throws
           StoreException {
      // NULL value
      if (obj == null) {
         return nullStringToWrite;
      }
      // date types (TIMESTAMP, TIME, DATE)
      else if (storeField.getType().isDateType()) {
         return dateFormatter.format((java.util.Date) obj);
      }
      // VARCHAR type
      else if (storeField.getType() == StoreDataType.VARCHAR) {
         String str = (String) obj;
         if (quoteAllString || str.contains("\""))
            return "\"" + StringUtils.duplicateDoubleQuote2(str) + "\"";
         else
            return str;
      }
      // any number (INTEGER, BIGINT, FLOAT, DOUBLE, BIGDECIMAL) and decimalFormatter!=null
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

   // inserts record
   public void insertRecords(StoreRecordsIF recs) throws StoreException {
      try {
         // check the end of the file. Is it a line separator or no?
         if (table.dir.supportsRandomAccess() && table.checkLastEOL) {
            output.flush();
            checkForLastEOL();
         }
         // loop on records
         recs.beforeFirst();
         while (recs.hasNext()) {
            insertRecord(recs.nextRecord());
         }
      } catch (Exception ex) {
         throw new StoreException("[CSV File] Can't insert a record. " + ex.getMessage(), ex);
      }
   }

   private void insertRecord(StoreRecordIF rec) throws Exception {
      if (table.fixedLengthType) {
         // ### fixed-length file type ###
         insertRecordInFixed(rec);
      } else
         // ### CSV file type ###
         insertRecordInCSV(rec);
   }

   public void rewrite(StoreRecordsIF recs) throws StoreException {
      try {
         if (!table.comments.isEmpty())
            rewriteWithComments(recs);
         else {
            rewriteWithNoComments(recs);
         }
      } catch (Exception ex) {
         throw new StoreException("[CSV File] Can't rewrite CSV file '" + table.tableName + "':"
                 + ex.getMessage(), ex);
      }
   }

   /**
    * write a file that has no comments
    *
    * @param recs
    * @throws StoreException
    */
   private void rewriteWithNoComments(StoreRecordsIF recs) throws StoreException {
      writeHeader();
      insertRecords(recs);
   }

   /**
    * write a file that has comments
    *
    * @param recs
    * @throws StoreException
    */
   private void rewriteWithComments(StoreRecordsIF recs) throws Exception {
      recs.beforeFirst();
      Iterator<CSVComment> commentIterator = table.comments.iterator();
      CSVComment comment = null;

      if (commentIterator.hasNext()) {
         comment = commentIterator.next();
      }

      // check for comments before a header
      while (comment != null && currentRowPosition == comment.getPos()) {
         output.write(comment.getComment() + table.rowDelimiter);
         currentRowPosition++;

         if (commentIterator.hasNext()) {
            comment = commentIterator.next();
         } else {
            comment = null;
         }
      }

      // insert a header
      writeHeader();

      // loop on records
      while (recs.hasNext()) {
         // check for comments before the next row
         while (comment != null && currentRowPosition == comment.getPos()) {
            output.write(comment.getComment() + table.rowDelimiter);
            currentRowPosition++;

            if (commentIterator.hasNext()) {
               comment = commentIterator.next();
            } else {
               comment = null;
            }
         }

         insertRecord(recs.nextRecord());
      }


      // inserts rest comments
      if (comment != null) {
         output.write(comment.getComment() + table.rowDelimiter);

         while (commentIterator.hasNext()) {
            comment = commentIterator.next();
            output.write(comment.getComment() + table.rowDelimiter);
         }
      }
   }

   // updates record
   public void updateRecords(StoreRecordsIF recs) throws StoreException {
      throw new UnsupportedOperationException(
              "Method updateRecords() not yet implemented.");
   }

   // deletes record
   public void deleteRecords(StoreRecordsIF recs) throws StoreException {
      throw new UnsupportedOperationException(
              "Method deleteRecords() not yet implemented.");
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
         output.close();

         // directly upload a text file for FTP, SFTP, etc protocols
         table.dir.upload(fm);

      } catch (Exception ex) {
         throw new UnexpectedException(ex.getMessage(), ex);
      }

      output = null;
      table = null;
   }

}
