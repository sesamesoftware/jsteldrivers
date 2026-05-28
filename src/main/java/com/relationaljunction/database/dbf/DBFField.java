package com.relationaljunction.database.dbf;

import java.io.*;

import com.relationaljunction.utils.UnexpectedException;

public class DBFField {

   // standard DBase III/IV data types
   public static final byte FIELD_TYPE_CHAR = (byte) 'C';
   public static final byte FIELD_TYPE_LOGICAL = (byte) 'L';
   public static final byte FIELD_TYPE_NUMERIC = (byte) 'N';
   public static final byte FIELD_TYPE_FLOAT = (byte) 'F';
   public static final byte FIELD_TYPE_DATE = (byte) 'D';
   public static final byte FIELD_TYPE_MEMO = (byte) 'M';
   // additional data types (Visual FoxPro, DBase7 level, etc)
   public static final byte FIELD_TYPE_PICTURE = (byte) 'P';
   public static final byte FIELD_TYPE_GENERAL = (byte) 'G';
   public static final byte FIELD_TYPE_DOUBLE = (byte) 'B';
   public static final byte FIELD_TYPE_INTEGER = (byte) 'I';
   public static final byte FIELD_TYPE_CURRENCY = (byte) 'Y';
   public static final byte FIELD_TYPE_DATETIME = (byte) 'T';
   public static final byte FIELD_TYPE_SYSTEM = 0x30;

   String fieldNameString;
   byte[] fieldName = new byte[11]; /* 0-10*/
   byte dataType; /* 11 */
   int offset; /* 12-15 */
   int fieldLength; /* 16 */
   byte decimalCount; /* 17 */
   short reserv2; /* 18-19 */
   byte workAreaId; /* 20 */
   short reserv3; /* 21-22 */
   byte setFieldsFlag; /* 23 */
   byte[] reserv4 = new byte[7]; /* 24-30 */
   byte indexFieldFlag; /* 31 */

   int nameNullIndex = 0;
   String charset = DBFBase.DEFAULT_CHARSET;

   public DBFField() {
   }

   public DBFField(String charset) {
      this.charset = charset;
   }

   protected static DBFField createField(DataInput in, String charset) throws
           IOException {
      DBFField field = new DBFField(charset);

      byte t_byte = in.readByte(); /* 0 */
      if (t_byte == (byte) 0x0d) {

         //System.out.println( "End of header found");
         return null;
      }

      in.readFully(field.fieldName, 1, 10); /* 1-10 */
      field.fieldName[0] = t_byte;
      field.fieldNameString = new String(field.fieldName);

      for (int i = 0; i < field.fieldName.length; i++) {

         if (field.fieldName[i] == (byte) 0) {

            field.nameNullIndex = i;
            break;
         }
      }

      field.dataType = in.readByte(); /* 11 */

      // if a field has an unknown type convert it to the CHAR type
      if (field.dataType == 'W' ||
              field.dataType == 'w' ||
              field.dataType == 0)
         field.dataType = FIELD_TYPE_CHAR;

      field.offset = DBFUtils.readLittleEndianInt(in); /* 12-15 */
      field.fieldLength = in.readUnsignedByte(); /* 16 */
      field.decimalCount = in.readByte(); /* 17 */
      field.reserv2 = DBFUtils.readLittleEndianShort(in); /* 18-19 */
      field.workAreaId = in.readByte(); /* 20 */
      field.reserv2 = DBFUtils.readLittleEndianShort(in); /* 21-22 */
      field.setFieldsFlag = in.readByte(); /* 23 */
      in.readFully(field.reserv4); /* 24-30 */
      field.indexFieldFlag = in.readByte(); /* 31 */

      return field;
   }

   protected void write(DataOutput out) throws IOException {

      //DataOutputStream out = new DataOutputStream( os);

      // Field Name
      out.write(fieldName); /* 0-10 */
      out.write(new byte[11 - fieldName.length]);

      // data type
      out.writeByte(dataType); /* 11 */
      DBFUtils.writeLittleEndianInteger(out, offset); /* field offset 12-15 */
      out.writeByte(fieldLength); /* 16 */
      out.writeByte(decimalCount); /* 17 */
      out.writeShort((short) 0x00); /* 18-19 */
      out.writeByte((byte) 0x00); /* 20 */
      out.writeShort((short) 0x00); /* 21-22 */
      out.writeByte((byte) 0x00); /* 23 */
      out.write(new byte[7]); /* 24-30*/
      out.writeByte((byte) 0x00); /* 31 */
   }

   public String getName() {
      try {
         return new String(this.fieldName, 0, nameNullIndex, charset);
      } catch (UnsupportedEncodingException ex) {
         throw new UnexpectedException("Unknown error in DBFField.getName()" +
                 ex.getMessage());
      }
   }

   public byte getDataType() {
      return dataType;
   }

   public int getFieldLength() {
      return fieldLength;
   }

   public int getDecimalCount() {
      return decimalCount;
   }

   // Setter methods

   public void setOffset(int offset) {
      this.offset = offset;
   }

   public void setName(String value) {

      if (value == null) {
         throw new IllegalArgumentException("Field name cannot be null");
      }

      if (value.isEmpty() || value.length() > 10) {
         throw new IllegalArgumentException("Invalid field '" + fieldName +
                 ". Field name should be of length 1-10");
      }

      if (dataType == FIELD_TYPE_CHAR && fieldLength > 255)
         throw new UnexpectedException("Invalid field '" + fieldName +
                 "'. The maximum length for CHARACTER fields in the DBF format is 255 characters.");


      try {
         this.fieldName = value.getBytes(charset);
      } catch (UnsupportedEncodingException ex) {
         throw new UnexpectedException("Unknown error in DBFField.setName()" +
                 ex.getMessage());
      }

      this.nameNullIndex = this.fieldName.length;
   }

   /**
    * Sets the data type of the field.
    *
    * @param type of the field. One of the following:<br>
    *             C, L, N, F, D, M
    */

   public void setDataType(byte value) {

      switch (value) {

         case FIELD_TYPE_DATE:
         case FIELD_TYPE_DATETIME:
         case FIELD_TYPE_CURRENCY:
            this.dataType = value;
            this.fieldLength = 8;
            break;
         case FIELD_TYPE_DOUBLE:
            this.dataType = value;
            this.fieldLength = 8;
            this.decimalCount = 4;
            break;
         case FIELD_TYPE_MEMO:
         case FIELD_TYPE_PICTURE:
         case FIELD_TYPE_GENERAL:
            // for VFP
            this.dataType = value;
            this.fieldLength = 4;
            break;
         case FIELD_TYPE_INTEGER:
            this.dataType = value;
            this.fieldLength = 4;
            break;
         case FIELD_TYPE_LOGICAL:
            this.dataType = value;
            this.fieldLength = 1;
            break;
         case FIELD_TYPE_CHAR:
         case FIELD_TYPE_NUMERIC:
         case FIELD_TYPE_FLOAT:
            this.dataType = value;
            break;

         default:
            throw new IllegalArgumentException("Unknown data type");
      }
   }

   public void setFieldLength(int value) {

      if (value <= 0) {

         throw new IllegalArgumentException(
                 "Field length should be a positive number");
      }

//    if (this.dataType == FIELD_TYPE_DATE) {
//      throw new UnsupportedOperationException("Cannot do this on a Date field");
//    }

      fieldLength = value;
   }

   public void setDecimalCount(int value) {

      if (value < 0) {

         throw new IllegalArgumentException(
                 "Decimal length should be a positive number");
      }

      if (value > fieldLength) {

         throw new IllegalArgumentException(
                 "Decimal length should be less than field length");
      }

      decimalCount = (byte) value;
   }

}
