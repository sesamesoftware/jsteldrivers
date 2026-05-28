package com.relationaljunction.database.dbf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Blob;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.utils.FileUtils;
import com.relationaljunction.utils.OtherUtils;

public class VFPFileWriter
        extends VFPFileReader {
   private final Logger log = LoggerFactory.getLogger("VFPFileWriter");

   private final static long MODIFIED_DATE_POSITION = 1;

   private RandomAccessFile raf = null;
   private final File dbfFile;
   private String writeEOF = DBFBase.WRITE_EOF_ON_RECORDS_NUMBER;

   // creates a new DBF file
   public VFPFileWriter(File dbfFile, DBFField[] fields, String charset,
                        byte dbfType, String memoFileExtension,
                        short memoBlockSize, byte dbfCodepage) throws
           Exception {
      // create or clear a file if it exists
      try {
         this.charset = charset;
         this.header = new DBFHeader(dbfType, charset, dbfCodepage);
         this.dbfFile = dbfFile;

         FileOutputStream fos = new FileOutputStream(dbfFile, false);
         fos.close();

         this.raf = new RandomAccessFile(dbfFile, "rw");
         setFields(fields);
      } catch (IOException ex) {
         throw new DBFException("Can't create a DBF: " + dbfFile + ". Exception was: " +
                 ex.getMessage());
      }

      try {
         if (this.header.isContainsMemoField()) {
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

            // create an empty memo file
            memoFile = new MemoFile(memoFilePath, memoBlockSize);
         }
      } catch (DBFException ex) {
         throw new DBFException("Can't create a memo file: " + memoFilePath + ". Exception was " +
                 ex.getMessage());
      }
   }

   public VFPFileWriter(File dbfFile, String memoFileExtension, String charset) throws DBFException {
      this(dbfFile, memoFileExtension, charset, false);
   }

   // open an existing file
   public VFPFileWriter(File dbfFile, String memoFileExtension, String charset, boolean lock)
           throws DBFException {
      if (!dbfFile.exists()) {
         throw new DBFException("Specified file '" + dbfFile.getName() +
                 "' is not found. ");
      }

      this.charset = charset;
      this.dbfFile = dbfFile;

      try {
         if (lock) {
            Vector<RandomAccessFile> rafResult = new Vector<RandomAccessFile>();
            fileLock = FileUtils.lockFile(dbfFile, false,
                    DBFBase.LOCK_CHECK_PERIOD, DBFBase.LOCK_TIME_OUT, rafResult);
            this.raf = rafResult.get(0);

            OtherUtils.writeLogInfo(log, "File '" + dbfFile.getName() + "' is locked exlusively");
         } else {
            this.raf = new RandomAccessFile(dbfFile, "rw");
         }

         header = new DBFHeader(charset);
         this.header.read(raf);
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new DBFException("Error while reading header: " + ex.getMessage());
      }

      loadMemo(dbfFile, memoFileExtension, false, lock);
   }

   public void setWriteEOF(String writeEOF) {
      this.writeEOF = writeEOF;
   }

   public void clearRecords() throws Exception {
      // write modified date and record count
      this.raf.seek(MODIFIED_DATE_POSITION);
      this.header.writeModifiedDate(raf);
      this.header.writeRecordCount(raf, 0);
      this.header.numberOfRecords = 0;
      this.raf.setLength(this.header.headerLength);


      if (this.header.isContainsMemoField()) {
         short memoBlockSize = memoFile.getMemoBlockSize();
         memoFile.close();
         // create an empty memo file
         memoFile = new MemoFile(memoFilePath, memoBlockSize);
      }
   }

   /**
    * go to a position of EOF. EOF is calculated on basis of numberOfRecords value.
    *
    * @throws IOException
    */
   private void gotoEOFOnRecordsNumber() throws IOException {
      int eofPos = this.header.numberOfRecords * this.header.recordLength +
              this.header.headerLength;

      if (eofPos > raf.length()) {
         throw new IOException("Expected EOF postion (" + eofPos + ") > length file (" + raf.length()
                 + "). DBF file '" + dbfFile.getName() + "' may be corrupted.");
      } else if (eofPos < (raf.length() - 1)) {
//         System.out.println("Expected EOF position (" + eofPos + ") < length file - 1 (" + (raf.length() - 1)
//                          + "). DBF file '" + dbfFile.getName() + "' may be corrupted.");
         log.warn("Expected EOF position (" + eofPos + ") < length file - 1 (" + (raf.length() - 1)
                 + "). DBF file '" + dbfFile.getName() + "' may be corrupted.");
      }

      this.raf.seek(eofPos);
   }

   protected int readRecordInBytes(byte[] recordBytes) throws IOException {
      return raf.read(recordBytes);
   }

   public boolean gotoRecord(int recordNumber) throws IOException {
      if (recordNumber > this.header.numberOfRecords) throw new
              ArrayIndexOutOfBoundsException("Specified record number = " +
              recordNumber + " > number of records");

      int recordPos = recordNumber * this.header.recordLength +
              this.header.headerLength;

      if (recordPos > raf.length()) throw new IOException(
              "Expected record position " + recordPos + " for the record number =" +
                      recordNumber + " > length file. DBF file may be corrupted.");
      this.raf.seek(recordPos);

      // read "deleted" byte
      int t_byte = raf.readByte();
      // return back to the recordPos
      this.raf.seek(recordPos);

      return t_byte == DELETED_BYTE;
   }

   public boolean gotoPreviousRecord() throws IOException {
      recordPos--;
      return gotoRecord(recordPos);
   }

   public void beforeFirst() throws IOException {
      gotoRecord(0);
   }

   private void setFields(DBFField[] fields) throws DBFException {
      if (this.header.fieldArray != null) {
         throw new DBFException("Fields has already been set");
      }

      if (fields == null || fields.length == 0) {
         throw new DBFException("Should have at least one field");
      }

      for (int i = 0; i < fields.length; i++) {
         if (fields[i] == null) {
            throw new DBFException("Field " + (i + 1) + " is null");
         }
      }

      this.header.fieldArray = fields;

      try {
         // writes a new/non-existent file. So write header before proceeding
         this.header.write(this.raf);
      } catch (IOException e) {
         throw new DBFException("Error writing fields: " + e.getMessage());
      }
   }

   public void insertRecord(Object[] objectArray) throws Exception {
      gotoEOFOnRecordsNumber();

      writeRecord(objectArray, true);

      // increase record number
      this.header.numberOfRecords++;
   }

   public void updateRecord(Object[] objectArray) throws Exception {
      writeRecord(objectArray, false);
   }

   public void deleteRecord() throws IOException {
      // write "deleted" byte
      raf.write(DELETED_BYTE);
   }

   private void writeRecord(Object[] objectArray, boolean isNewRecord) throws Exception {
      // write "deleted" byte
      raf.write(NON_DELETED_BYTE);

      recordPos++;

      for (int i = 0; i < this.header.fieldArray.length; i++) {
         /* iterate throught fields */

         switch (this.header.fieldArray[i].getDataType()) {

            case DBFField.FIELD_TYPE_CHAR:
               if (objectArray[i] != null) {

                  String str_value = objectArray[i].toString();
                  raf.write(DBFUtils.textPadding(str_value, charset,
                          this.header.fieldArray[i].
                                  getFieldLength()));
               } else {

                  raf.write(DBFUtils.textPadding("", this.charset,
                          this.header.fieldArray[i].
                                  getFieldLength()));
               }

               break;

            case DBFField.FIELD_TYPE_INTEGER:
               if (objectArray[i] != null) {
                  int int_value = ((Number) objectArray[i]).intValue();
                  DBFUtils.writeLittleEndianInteger(raf, int_value);
               } else {
//            raf.write("        ".getBytes());
                  raf.writeInt(0);
               }
               break;

            case DBFField.FIELD_TYPE_DOUBLE:
               if (objectArray[i] != null) {
                  double d = ((Number) objectArray[i]).doubleValue();
                  DBFUtils.writeLittleEndianDouble(raf, d);
               } else {
//            raf.write("        ".getBytes());
                  DBFUtils.writeLittleEndianDouble(raf, 0);
               }
               break;

            case DBFField.FIELD_TYPE_CURRENCY:
               if (objectArray[i] != null) {
                  double d = ((Number) objectArray[i]).doubleValue();
                  DBFUtils.writeLittleEndianLong(raf, (long) (d * 10000));
               } else {
//            raf.write("        ".getBytes());
                  DBFUtils.writeLittleEndianLong(raf, 0);
               }
               break;

            case DBFField.FIELD_TYPE_DATE:
               if (objectArray[i] != null) {

                  GregorianCalendar calendar = new GregorianCalendar();
                  calendar.setTime((java.util.Date) objectArray[i]);
                  raf.write(String.valueOf(calendar.get(Calendar.YEAR)).
                          getBytes());
                  raf.write(DBFUtils.textPadding(String.valueOf(calendar.get(
                          Calendar.MONTH) + 1), this.charset, 2,
                          DBFUtils.ALIGN_RIGHT, (byte) '0'));
                  raf.write(DBFUtils.textPadding(String.valueOf(calendar.get(
                          Calendar.DAY_OF_MONTH)), this.charset, 2,
                          DBFUtils.ALIGN_RIGHT, (byte) '0'));
               } else {
                  raf.write("        ".getBytes());
               }
               break;

            case DBFField.FIELD_TYPE_DATETIME:
               if (objectArray[i] != null) {
                  GregorianCalendar calendar = new GregorianCalendar();
                  calendar.setTime((java.util.Date) objectArray[i]);
                  int year = calendar.get(Calendar.YEAR);
                  int month = calendar.get(Calendar.MONTH) + 1;
                  int day = calendar.get(Calendar.DAY_OF_MONTH);
                  int hour = calendar.get(Calendar.HOUR_OF_DAY);
                  int min = calendar.get(Calendar.MINUTE);
                  int sec = calendar.get(Calendar.SECOND);
                  int jd = (1461 * (year + 4800 + (month - 14) / 12)) / 4 +
                          (367 * (month - 2 - 12 * ((month - 14) / 12))) / 12 -
                          (3 * ((year + 4900 + (month - 14) / 12) / 100)) / 4 +
                          day - 32075;
                  int milisec = (hour * 3600000) + (min * 60000) + (sec * 1000);
                  DBFUtils.writeLittleEndianInteger(raf, jd);
                  DBFUtils.writeLittleEndianInteger(raf, milisec);
               } else {
                  raf.writeInt(0);
                  raf.writeInt(0);
               }
               break;

            case DBFField.FIELD_TYPE_FLOAT:

               if (objectArray[i] != null) {
                  raf.write(DBFUtils.doubleFormating(((Number) objectArray[i]).doubleValue(),
                          this.charset,
                          this.header.fieldArray[i].
                                  getFieldLength(),
                          this.header.fieldArray[i].
                                  getDecimalCount()));
               } else {
                  raf.write(DBFUtils.textPadding(" ", this.charset,
                          this.header.fieldArray[i].
                                  getFieldLength(),
                          DBFUtils.ALIGN_RIGHT));
               }

               break;

            case DBFField.FIELD_TYPE_NUMERIC:

               if (objectArray[i] != null) {
//            System.out.println(new String(Utils.doubleFormating( ( (Number) objectArray[j]).
//                                                     doubleValue(),
//                                                     this.characterSetName,
//                                                     this.header.fieldArray[j].
//                                                     getFieldLength(),
//                                                     this.header.fieldArray[j].
//                                                     getDecimalCount())));

                  if (objectArray[i] instanceof java.math.BigDecimal)
                     raf.write(DBFUtils.textPadding(objectArray[i].toString(), charset,
                             this.header.fieldArray[i].getFieldLength(),
                             DBFUtils.ALIGN_RIGHT,
                             (byte) ' '));
                  else
                     raf.write(DBFUtils.doubleFormating(((Number) objectArray[i]).doubleValue(),
                             this.charset,
                             this.header.fieldArray[i].getFieldLength(),
                             this.header.fieldArray[i].getDecimalCount()));
               } else {
                  raf.write(
                          DBFUtils.textPadding(" ", this.charset,
                                  this.header.fieldArray[i].getFieldLength(),
                                  DBFUtils.ALIGN_RIGHT));
               }
               break;

            case DBFField.FIELD_TYPE_LOGICAL:

               if (objectArray[i] != null) {
                  if ((Boolean) objectArray[i]) {
                     raf.write((byte) 'T');
                  } else {
                     raf.write((byte) 'F');
                  }
               } else {
//            raf.write( (byte) '?');
                  raf.write((byte) 'F');
               }
               break;

            case DBFField.FIELD_TYPE_MEMO:
            case DBFField.FIELD_TYPE_GENERAL:
            case DBFField.FIELD_TYPE_PICTURE:
               if (DBFBase.ignoreMemoFile == DBFBase.IGNORE_MEMO_FILE_ALWAYS ||
                       memoFile.exists()) {
                  raf.writeInt(0);
                  break;
               }

               if (objectArray[i] == null || objectArray[i].toString().isEmpty())
                  // if value to insert (update) is NULL then write NULL
                  raf.writeInt(0);

               else if (isNewRecord) {
                  // inserts a memo
                  int pointer;

                  if (this.header.fieldArray[i].getDataType() == DBFField.FIELD_TYPE_MEMO && !readMemoInBytes)
                     pointer = memoFile.addMemo(objectArray[i].toString().getBytes(this.charset));
                  else if (objectArray[i] instanceof byte[])
                     pointer = memoFile.addMemo((byte[]) objectArray[i], MemoFile.MEMO_OBJECT_TYPE);
                  else if (objectArray[i] instanceof Blob) {
                     Blob blob = (Blob) objectArray[i];
                     pointer = memoFile.addMemo(blob.getBytes(1, (int) blob.length()), MemoFile.MEMO_OBJECT_TYPE);
                  } else
                     throw new IllegalArgumentException("VFPWileWriter: invalid object for BLOB " + objectArray[i]);

                  DBFUtils.writeLittleEndianInteger(raf, pointer);
               } else {
                  // updates a memo
                  long offset = raf.getFilePointer();
                  int oldPointer = DBFUtils.readLittleEndianInt(raf);
                  int newPointer = 0;

                  if (oldPointer == 0) {
                     // memo value was NULL;
                     if (this.header.fieldArray[i].getDataType() == DBFField.FIELD_TYPE_MEMO && !readMemoInBytes)
                        newPointer = memoFile.addMemo(objectArray[i].toString().
                                getBytes(this.charset));
                     else if (objectArray[i] instanceof byte[])
                        newPointer = memoFile.addMemo((byte[]) objectArray[i], MemoFile.MEMO_OBJECT_TYPE);
                     else if (objectArray[i] instanceof Blob) {
                        Blob blob = (Blob) objectArray[i];
                        newPointer = memoFile.addMemo(blob.getBytes(1, (int) blob.length()), MemoFile.MEMO_OBJECT_TYPE);
                     } else
                        throw new IllegalArgumentException("VFPWileWriter: invalid object for BLOB " + objectArray[i]);
                  }
                  // memo values was not null
                  else {
                     if (this.header.fieldArray[i].getDataType() == DBFField.FIELD_TYPE_MEMO && !readMemoInBytes)
                        newPointer = memoFile.updateMemo(oldPointer, objectArray[i].toString().getBytes(this.charset));
                     else if (objectArray[i] instanceof byte[])
                        newPointer = memoFile.updateMemo(oldPointer, (byte[]) objectArray[i], MemoFile.MEMO_OBJECT_TYPE);
                     else if (objectArray[i] instanceof Blob) {
                        Blob blob = (Blob) objectArray[i];
                        newPointer = memoFile.updateMemo(oldPointer, blob.getBytes(1, (int) blob.length()),
                                MemoFile.MEMO_OBJECT_TYPE);
                     } else
                        throw new IllegalArgumentException("VFPWileWriter: invalid object for BLOB " + objectArray[i]);
                  }

                  if (newPointer != oldPointer) {
                     // update a memo pointer
                     raf.seek(offset);
                     DBFUtils.writeLittleEndianInteger(raf, newPointer);
                  }
               }

               break;

            case DBFField.FIELD_TYPE_SYSTEM: {
               raf.write(new byte[this.header.fieldArray[i].getFieldLength()]);
               break;
            }

            default:
               throw new DBFException("Unknown field type " +
                       this.header.fieldArray[i].getDataType());
         }
      }
   }

   public void close() throws Exception {
      // should be first
      writeEOF();

      // write modified date and record count
      this.raf.seek(MODIFIED_DATE_POSITION);
      this.header.writeModifiedDate(raf);
      this.header.writeRecordCount(raf);

      // release a lock
      if (fileLock != null) {
         fileLock.release();
         OtherUtils.writeLogInfo(log, "File '" + dbfFile.getName() + "' is unlocked");
      }

      // close a file
      this.raf.close();

      if (this.memoFile != null)
         this.memoFile.close();

      this.raf = null;
      this.memoFile = null;
      this.header = null;
      this.fileLock = null;
   }

   private void writeEOF() throws IOException {
      // write EOF on basis of numberOfRecords value
      if (writeEOF.equals(DBFBase.WRITE_EOF_ON_RECORDS_NUMBER)) {
         gotoEOFOnRecordsNumber();
         this.raf.writeByte(END_OF_DATA);

         // trim a file size to the size specified in the header.
         // sometimes files may be corrupted and have the size more than required one
         raf.setLength((long) this.header.numberOfRecords * this.header.recordLength +
                 this.header.headerLength + 1);
      }
      // write EOF simply at the end of file
      else if (writeEOF.equals(DBFBase.WRITE_EOF_AT_END)) {
         this.raf.writeByte(END_OF_DATA);
      }
      // else don't write EOF at all
   }


}
