package com.relationaljunction.database.dbf;

import java.io.*;
import java.util.*;

public class DBFHeader {

   public static final byte SIG_FOX_BASE = (byte) 0x02;
   public static final byte SIG_DBASE_III_WITHOUT_MEMO = (byte) 0x03;
   public static final byte SIG_DBASE_IV_WITHOUT_MEMO = (byte) 0x04;
   public static final byte SIG_DBASE_V_WITHOUT_MEMO = (byte) 0x05;
   public static final byte SIG_VFP = (byte) 0x30;
   public static final byte SIG_VFP_WITH_AUTOINCREMENT = (byte) 0x31;
   public static final byte SIG_DBV_MEMO_FLAG = (byte) 0x43;
   public static final byte SIG_DBASE_IV_WITH_MEMO = (byte) 0x7B;
   public static final byte SIG_DBASE_III_WITH_MEMO = (byte) 0x83;
   public static final byte SIG_DBASE_IV_WITH_MEMO_2 = (byte) 0x8B;
   public static final byte SIG_DBASE_IV_WITH_SQL_TABLE = (byte) 0x8E;
   public static final byte SIG_DBV_AND_DBT_FLAG = (byte) 0xB3;
   public static final byte SIG_CLIPPER_WITH_SMT_MEMO = (byte) 0xB3;
   public static final byte SIG_FOXPRO_WITH_MEMO = (byte) 0xF5;
   public static final byte SIG_FOXBASE_2 = (byte) 0xFB;

   public static final byte CONTAINS_MEMO_FIELD = 0x2;
   public static final byte NOT_CONTAINS_MEMO_FIELD = 0;

   public static final byte LANGUAGE_WINDOWS_ANSI_1252 = 0x3;

   byte signature; /* 0 */
   byte year; /* 1 */
   byte month; /* 2 */
   byte day; /* 3 */
   int numberOfRecords; /* 4-7 */
   short headerLength; /* 8-9 */
   short recordLength; /* 10-11 */
   short reserv1; /* 12-13 */
   byte incompleteTransaction; /* 14 */
   byte encryptionFlag; /* 15 */
   int freeRecordThread; /* 16-19 */
   int reserv2; /* 20-23 */
   int reserv3; /* 24-27 */
   byte mdxFlag = NOT_CONTAINS_MEMO_FIELD; /* 28 */
   byte languageDriver = LANGUAGE_WINDOWS_ANSI_1252; /* 29 Windows ANSI */
   short reserv4; /* 30-31 */
   DBFField[] fieldArray; /* each 32 bytes */
   byte terminator1 = 0x0D; /* n+1 */

   //byte[] databaseContainer; /* 263 bytes */
   /* DBF structure ends here */
   String charset = DBFBase.DEFAULT_CHARSET;

   boolean containsMemoField = false;

   DBFHeader(String charset) {
      this.charset = charset;
      this.signature = SIG_DBASE_III_WITHOUT_MEMO;
   }

   DBFHeader(byte signature, String charset) {
      this.charset = charset;
      this.signature = signature;
   }

   DBFHeader(byte signature, String charset, byte languageDriver) {
      this.charset = charset;
      this.signature = signature;
      this.languageDriver = languageDriver;
   }

   void read(DataInput dataInput) throws IOException {
      signature = dataInput.readByte(); /* 0 */
      year = dataInput.readByte(); /* 1 */
      month = dataInput.readByte(); /* 2 */
      day = dataInput.readByte(); /* 3 */
      numberOfRecords = DBFUtils.readLittleEndianInt(dataInput); /* 4-7 */

      headerLength = DBFUtils.readLittleEndianShort(dataInput); /* 8-9 */
      recordLength = DBFUtils.readLittleEndianShort(dataInput); /* 10-11 */

      reserv1 = DBFUtils.readLittleEndianShort(dataInput); /* 12-13 */
      incompleteTransaction = dataInput.readByte(); /* 14 */
      encryptionFlag = dataInput.readByte(); /* 15 */
      freeRecordThread = DBFUtils.readLittleEndianInt(dataInput); /* 16-19 */
      reserv2 = dataInput.readInt(); /* 20-23 */
      reserv3 = dataInput.readInt(); /* 24-27 */
      mdxFlag = dataInput.readByte(); /* 28 */
      languageDriver = dataInput.readByte(); /* 29 */
      reserv4 = DBFUtils.readLittleEndianShort(dataInput); /* 30-31 */


      /* ###### read fields #####*/

      Vector<DBFField> v_fields = new Vector<DBFField>();

      DBFField field = DBFField.createField(dataInput, charset); /* 32 each */
      while (field != null) {
         v_fields.addElement(field);
         field = DBFField.createField(dataInput, charset);
      }

      fieldArray = new DBFField[v_fields.size()];

      for (int i = 0; i < fieldArray.length; i++) {
         fieldArray[i] = v_fields.elementAt(i);
         if (isFieldMemo(fieldArray[i])) containsMemoField = true;
      }
      //System.out.println( "Number of fields: " + fieldArray.length);

   }

   void write(DataOutput dataOutput) throws IOException {
      // does it contains memo field?
      for (DBFField aFieldArray : fieldArray)
         if (isFieldMemo(aFieldArray)) containsMemoField = true;

      dataOutput.writeByte(signature); /* 0 */

      writeModifiedDate(dataOutput); /*1-3*/
      writeRecordCount(dataOutput); /*4-7*/

      headerLength = findHeaderLength();
      DBFUtils.writeLittleEndianShort(dataOutput, headerLength); /* 8-9 */

      recordLength = findRecordLength();
      DBFUtils.writeLittleEndianShort(dataOutput, recordLength); /* 10-11 */

      DBFUtils.writeLittleEndianShort(dataOutput, reserv1); /* 12-13 */
      dataOutput.writeByte(incompleteTransaction); /* 14 */
      dataOutput.writeByte(encryptionFlag); /* 15 */
      DBFUtils.writeLittleEndianInteger(dataOutput, freeRecordThread); /* 16-19 */
      DBFUtils.writeLittleEndianInteger(dataOutput, reserv2); /* 20-23 */
      DBFUtils.writeLittleEndianInteger(dataOutput, reserv3); /* 24-27 */

      if (containsMemoField) /* 28 */
         dataOutput.writeByte(CONTAINS_MEMO_FIELD);
      else
         dataOutput.writeByte(NOT_CONTAINS_MEMO_FIELD);

      dataOutput.writeByte(languageDriver); /* 29 */
      DBFUtils.writeLittleEndianShort(dataOutput, reserv4); /* 30-31 */


      int offset = 1;
      for (DBFField field : fieldArray) {
         // some checks
         if (field.getDataType() == DBFField.FIELD_TYPE_CHAR &&
                 field.getFieldLength() > 255)
            throw new IOException("Invalid field '" +
                    field.getName() +
                    "'. Maximum length for CHARACTER fields in the DBF format is 255 characters.");
         field.setOffset(offset);
         field.write(dataOutput);
         offset += field.getFieldLength();
      }

      dataOutput.writeByte(terminator1); /* n+1 */

      // if file is VFP add a 263-byte range
      if (signature == SIG_VFP || signature == SIG_VFP_WITH_AUTOINCREMENT)
         dataOutput.write(new byte[263]);
   }

   private boolean isFieldMemo(DBFField field) {
      return (field.getDataType() == DBFField.FIELD_TYPE_MEMO ||
              field.getDataType() == DBFField.FIELD_TYPE_GENERAL ||
              field.getDataType() == DBFField.FIELD_TYPE_PICTURE);
   }

   void writeRecordCount(DataOutput dataOutput) throws IOException {
      writeRecordCount(dataOutput, numberOfRecords); /* 4-7 */
   }

   void writeRecordCount(DataOutput dataOutput, int recordCount) throws IOException {
      DBFUtils.writeLittleEndianInteger(dataOutput, recordCount); /* 4-7 */
   }

   void writeModifiedDate(DataOutput dataOutput) throws IOException {
      GregorianCalendar calendar = new GregorianCalendar();
      String lastYears = String.valueOf(calendar.get(Calendar.YEAR)).substring(2, 4);
      year = Byte.parseByte(lastYears);
      month = (byte) (calendar.get(Calendar.MONTH) + 1);
      day = (byte) (calendar.get(Calendar.DAY_OF_MONTH));

      dataOutput.writeByte(year); /* 1 */
      dataOutput.writeByte(month); /* 2 */
      dataOutput.writeByte(day); /* 3 */
   }

   private short findHeaderLength() {

      short length = (short) (
              1 +
                      3 +
                      4 +
                      2 +
                      2 +
                      2 +
                      1 +
                      1 +
                      4 +
                      4 +
                      4 +
                      1 +
                      1 +
                      2 +
                      (32 * fieldArray.length) +
                      1
      );

      // if file is VFP add a 263-byte range
      if (signature == SIG_VFP || signature == SIG_VFP_WITH_AUTOINCREMENT)
         length += 263;

      return length;
   }

   private short findRecordLength() {

      int recordLength = 0;
      for (DBFField field : fieldArray) {
         recordLength += field.getFieldLength();
      }

      return (short) (recordLength + 1);
   }

   public boolean isContainsMemoField() {
      return containsMemoField;
   }
}
