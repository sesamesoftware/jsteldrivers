package com.relationaljunction.database.dbf;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.channels.FileLock;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.utils.FileUtils;
import com.relationaljunction.utils.OtherUtils;

public class VFPFileReader
        extends DBFBase {
   private final Logger log = LoggerFactory.getLogger("VFPFileReader");

   private File dbfFile;
   protected DBFHeader header;
   protected String memoFilePath = null;
   protected MemoFile memoFile = null;

   protected boolean ignoreMemo = false;
   protected boolean readMemoInBytes = false;
   protected boolean isClosed = true;
   protected FileLock fileLock = null;

   // properties
   private DataInputStream dataInputStream;
   private boolean containsDeletedRecords = false;
   protected int recordPos = 0;

   protected VFPFileReader() {
   }

   public VFPFileReader(File dbfFile,
                        String memoFileExtension,
                        String charset)
           throws DBFException {
      this(dbfFile, memoFileExtension, charset, false, false);
   }

//   public VFPFileReader(File dbfFile,
//                        String charset,
//                        boolean ignoreMemo)
//           throws DBFException {
//      this(dbfFile, null, charset, ignoreMemo, false);
//   }

   public VFPFileReader(File dbfFile,
                        String memoFileExtension,
                        String charset,
                        boolean ignoreMemo,
                        boolean lock)
           throws DBFException {
      if (!dbfFile.exists()) throw new DBFException("Can't find the file '" +
              dbfFile.getName() + "'");

      this.dbfFile = dbfFile;
      this.ignoreMemo = ignoreMemo;
      this.charset = charset;

      try {
         if (lock) {
            Vector<InputStream> isResult = new Vector<InputStream>();
            fileLock = FileUtils.lockFile(dbfFile, DBFBase.LOCK_CHECK_PERIOD, DBFBase.LOCK_TIME_OUT,
                    isResult);
            this.dataInputStream = new DataInputStream(
                    new BufferedInputStream(isResult.get(0)));

            OtherUtils.writeLogInfo(log, "File '" + dbfFile.getName() + "' is locked shared");
         } else {
            this.dataInputStream = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(dbfFile)));
         }


         this.isClosed = false;
         this.header = new DBFHeader(charset);
         this.header.read(this.dataInputStream);

         /* it might be required to leap to the start of records at times */
         int t_dataStartIndex = this.header.headerLength -
                 (32 + (32 * this.header.fieldArray.length)) - 1;

         if (t_dataStartIndex > 0) {
            byte[] b_skip = new byte[t_dataStartIndex];
            dataInputStream.read(b_skip);
         }

         /* skip to data block*/
//      if (t_dataStartIndex > 0)
//        dataInputStream.skip(t_dataStartIndex);

      } catch (Exception ex) {
//         ex.printStackTrace();
         throw new DBFException("Can't read a header of DBF. Error was:" + ex.getMessage(), ex);
      }

      if (!ignoreMemo)
         loadMemo(dbfFile, memoFileExtension, true, lock);
   }

   public boolean isReadMemoInBytes() {
      return readMemoInBytes;
   }

   public void setReadMemoInBytes(boolean readMemoInBytes) {
      this.readMemoInBytes = readMemoInBytes;
   }

   public void setMaxMemoSizeInBytes(int maxMemoSizeInBytes) {
      if (memoFile != null)
         memoFile.setMemoMaxSizeInBytes(maxMemoSizeInBytes);
   }

   protected void loadMemo(File dbfFile, String memoFileExtension, boolean readOnly, boolean lock)
           throws DBFException {
      try {
         if (this.header.containsMemoField) {
            memoFilePath = com.relationaljunction.utils.StringUtils.getFileNameWithoutExtension(
                    dbfFile.getPath());

            if (memoFileExtension == null) {
               // resolve memo extension
               if (this.header.signature == DBFHeader.SIG_VFP ||
                       this.header.signature == DBFHeader.SIG_VFP_WITH_AUTOINCREMENT ||
                       this.header.signature == DBFHeader.SIG_FOXPRO_WITH_MEMO) {
                  memoFilePath += ".fpt";
               } else {
                  memoFilePath += ".dbt";
               }
            } else {
               // use user-defined memo extensions
               memoFilePath += memoFileExtension;
            }

            // open an existing memo file
            memoFile = new MemoFile(memoFilePath, readOnly, lock);
         }
      } catch (Exception ex) {
         throw new DBFException("Can't read a memo file. Error was:" + ex.getMessage());
      }
   }

   public int getRecordCount() {
      return this.header.numberOfRecords;
   }

   public DBFField getField(int index) throws DBFException {
      return this.header.fieldArray[index];
   }

   public int getFieldCount() throws DBFException {
      if (this.header.fieldArray != null) {
         return this.header.fieldArray.length;
      }
      return -1;
   }

   public int getSignature() {
      return this.header.signature;
   }

   protected int readRecordInBytes(byte[] recordBytes) throws IOException {
      return dataInputStream.read(recordBytes);
   }

   /**
    * gets the next non-deleted record
    *
    * @return
    */
   public Object[] nextRecord() throws DBFException {
      try {
         byte[] recordBytes;

         while (true) {
            // read a record to the buffer
            recordBytes = new byte[header.recordLength];
            int bytesRead = readRecordInBytes(recordBytes);

            if (bytesRead <= 1) {
               // EOF encountered
               return null;
            } else if (bytesRead < recordBytes.length) {
               // the record length is less than expected
               // don't process the file further
               log.warn("VFPFileReader: record has less bytes (" +
                       bytesRead + ") than expected (" + recordBytes.length + ")");
               return null;
            }

            // check the record status (deleted or non-deleted)
            byte recordStatus = recordBytes[0];

            if (recordStatus != DELETED_BYTE) {
               // non-deleted record
               break;
            } else {
               // deleted record
               containsDeletedRecords = true;
            }
         }

         recordPos++;

         // process bytes with data
         return extractRecord(new DataInputStream(
                 new ByteArrayInputStream(recordBytes, 1, header.recordLength)));
      } catch (EOFException e) {
         log.warn("VFPFileReader: unexpected EOF", e);
         return null;
      } catch (IOException e) {
         //      e.printStackTrace();
         throw new DBFException(e);
      }
   }

   protected Object[] extractRecord(DataInputStream recordStream) throws IOException {
      Object[] recordObjects = new Object[this.header.fieldArray.length];

      for (int i = 0; i < this.header.fieldArray.length; i++) {

         switch (this.header.fieldArray[i].getDataType()) {

            case DBFField.FIELD_TYPE_CHAR:

               byte[] b_array = new byte[this.header.fieldArray[i].getFieldLength()];
               recordStream.read(b_array);
               recordObjects[i] = new String(b_array, charset);
               break;

            case DBFField.FIELD_TYPE_INTEGER:

               try {
                  int num = DBFUtils.readLittleEndianInt2(recordStream);
                  recordObjects[i] = num;
               } catch (IOException ex) {
                  recordObjects[i] = null;
               }
               break;

            case DBFField.FIELD_TYPE_DOUBLE:

               try {
                  double d = DBFUtils.readLittleEndianDouble(recordStream);
                  recordObjects[i] = d;
               } catch (IOException ex) {
                  recordObjects[i] = null;
               }
               break;

            case DBFField.FIELD_TYPE_CURRENCY:
               try {
                  double curr = ((double) DBFUtils.readLittleEndianLong(
                          recordStream)) / 10000;
                  recordObjects[i] = curr;
               } catch (IOException ex) {
                  recordObjects[i] = null;
               }
               break;

            case DBFField.FIELD_TYPE_DATETIME:

               int julianDayNumber = DBFUtils.readLittleEndianInt2(recordStream);
               int milisec = DBFUtils.readLittleEndianInt2(recordStream);

               if (julianDayNumber <= 0) {
                  recordObjects[i] = null;
                  break;
               }

               // converts to Gregorian date
               // The algorithm is taken from http://www.hermetic.ch/cal_stud/jdn.htm#comp
               int L = julianDayNumber + 68569;
               int n = (4 * L) / 146097;
               L = L - (146097 * n + 3) / 4;
               int i3 = (4000 * (L + 1)) / 1461001;
               L = L - (1461 * i3) / 4 + 31;
               int j = (80 * L) / 2447;
               int day = L - (2447 * j) / 80;
               L = j / 11;
               int month = j + 2 - (12 * L);
               int year = 100 * (n - 49) + i3 + L;

               int hours = milisec / 3600000;
               int milisec_rest = milisec - (hours * 3600000);
               int min = milisec_rest / 60000;
               int sec = (milisec_rest - (min * 60000)) / 1000;

               GregorianCalendar calendar2 = new GregorianCalendar(
                       year, month - 1, day, hours, min, sec);

               recordObjects[i] = calendar2.getTime();

               break;

            case DBFField.FIELD_TYPE_DATE:

               byte[] t_byte_year = new byte[4];
               recordStream.read(t_byte_year);

               byte[] t_byte_month = new byte[2];
               recordStream.read(t_byte_month);

               byte[] t_byte_day = new byte[2];
               recordStream.read(t_byte_day);

               try {
                  GregorianCalendar calendar = new GregorianCalendar(
                          Integer.parseInt(new String(t_byte_year)),
                          Integer.parseInt(new String(t_byte_month)) - 1,
                          Integer.parseInt(new String(t_byte_day)));

                  recordObjects[i] = calendar.getTime();
               } catch (NumberFormatException e) {
                  /* this field may be empty or may have improper value set */
                  recordObjects[i] = null;
               }

               break;

            case DBFField.FIELD_TYPE_FLOAT:

               byte[] t_float = new byte[this.header.fieldArray[i].
                       getFieldLength()];
               recordStream.read(t_float);
               t_float = DBFUtils.trimLeftSpaces(t_float);

               if (t_float.length > 0 && DBFUtils.contains(t_float, (byte) '?')) {
                  String numberString = new String(t_float);
                  if (!numberString.trim().isEmpty()) {
                     if (numberString.indexOf(',') > 0)
                        numberString = numberString.replace(",", ".");

                     recordObjects[i] = numberString;
                  } else
                     recordObjects[i] = null;
               } else
                  recordObjects[i] = null;

               break;

            case DBFField.FIELD_TYPE_NUMERIC:

               byte[] t_numeric = new byte[this.header.fieldArray[i].
                       getFieldLength()];
               recordStream.read(t_numeric);
               t_numeric = DBFUtils.trimLeftSpaces(t_numeric);

               if (t_numeric.length > 0 && DBFUtils.contains(t_numeric, (byte) '?')) {
                  String numberString = new String(t_numeric);
                  if (!numberString.trim().isEmpty()) {
                     if (numberString.indexOf(',') > 0)
                        numberString = numberString.replace(",", ".");
                     recordObjects[i] = numberString;
                  } else
                     recordObjects[i] = null;
               } else
                  recordObjects[i] = null;

               break;

            case DBFField.FIELD_TYPE_LOGICAL:

               byte t_logical = recordStream.readByte();

               if (t_logical == 'Y' || t_logical == 'T' ||
                       t_logical == 't' || t_logical == '1')
                  recordObjects[i] = Boolean.TRUE;
               else
                  recordObjects[i] = Boolean.FALSE;

               break;

            case DBFField.FIELD_TYPE_MEMO:
            case DBFField.FIELD_TYPE_GENERAL:
            case DBFField.FIELD_TYPE_PICTURE:

               int memoPointer = DBFUtils.readLittleEndianInt2(recordStream);

               if (ignoreMemo ||
                       DBFBase.ignoreMemoFile == DBFBase.IGNORE_MEMO_FILE_ALWAYS ||
                       memoFile.exists()) {
                  recordObjects[i] = null;
                  break;
               }

               try {
                  if (memoFile == null || memoPointer == 0) {
                     recordObjects[i] = null;
                  } else {
                     byte[] memoBytes = memoFile.readMemoData(memoPointer);

                     // MEMO text
                     if (this.header.fieldArray[i].getDataType() == DBFField.FIELD_TYPE_MEMO &&
                             !readMemoInBytes)
                        try {
                           recordObjects[i] = new String(memoBytes, charset);
                        } catch (Throwable e) {
                           e.printStackTrace();
                        }
                        // GENERAL or PICTURE bytes
                     else
                        recordObjects[i] = memoBytes;
                  }
               } catch (Exception ex) {
                  recordObjects[i] = null;
               }

               break;

            case DBFField.FIELD_TYPE_SYSTEM: {
               recordStream.read(new byte[this.header.fieldArray[i].getFieldLength()]);
               recordObjects[i] = null;
               break;
            }

            default:
               throw new DBFException("Unsupported data type: '" +
                       this.header.fieldArray[i].getDataType() + "'");
         }
      }

      return recordObjects;
   }

   public void close() throws Exception {
      // release a lock
      if (fileLock != null) {
         fileLock.release();
         OtherUtils.writeLogInfo(log, "File '" + dbfFile.getName() + "' is unlocked");
      }

      if (dataInputStream != null)
         dataInputStream.close();
      if (memoFile != null)
         memoFile.close();

      this.fileLock = null;
      this.dataInputStream = null;
      this.memoFile = null;
      this.header = null;
   }

   public String toString() {
      StringBuilder sb = new StringBuilder(this.header.year + "/" +
              this.header.month + "/" +
              this.header.day + "\n"
              + "Total records: " +
              this.header.numberOfRecords +
              "\nHEader length: " +
              this.header.headerLength);

      for (int i = 0; i < this.header.fieldArray.length; i++) {

         sb.append(this.header.fieldArray[i].getName());
         sb.append("\n");
      }
      return sb.toString();
   }

   public boolean containsDeletedRecords() {
      return containsDeletedRecords;
   }

   public static void main(String[] args) throws Throwable {
//    PrintStream ps = new PrintStream(new FileOutputStream("other/temp/result.txt"));
//
//    VFPFileWriter writer = new VFPFileWriter(new File(
//      "c:/java/test/files/e_inven_vfp.dbf","unicode"), "fpt");
//
//    for (int i = 0; i < writer.getRecordCount(); i++) {
//      System.out.println("Record i");
//
//      boolean deleted = writer.gotoRecord(i);
//      Object[] objs = null;
//      if (deleted) continue;
//
//      objs = writer.readRecord();
//
//      printObjects(ps, objs);
//    }

//    ps.close();
   }

   private static void printObjects(PrintStream ps, Object[] objs) {
      for (int j = 0; j < objs.length; j++) {
         ps.print(objs[j]);
         ps.print("\t");
      }
      ps.println();
   }
}
