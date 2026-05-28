package com.relationaljunction.database.dbf;

import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;

public final class DBFUtils {

   private static final DecimalFormatSymbols dcs = new DecimalFormatSymbols();

   public static final int ALIGN_LEFT = 10;
   public static final int ALIGN_RIGHT = 12;

   static {
      dcs.setDecimalSeparator('.');
   }

   private DBFUtils() {
   }

   public static long readLittleEndianLong(DataInputStream f) throws IOException {
      long n, n1, n2, n3, n4, n5, n6, n7, n8;

      n1 = f.read();
      n2 = f.read();
      n3 = f.read();
      n4 = f.read();
      n5 = f.read();
      n6 = f.read();
      n7 = f.read();
      n8 = f.read();

      n = ((n8 << 56) + (n7 << 48) + (n6 << 40) + (n5 << 32) +
              (n4 << 24) + (n3 << 16) + (n2 << 8) + n1);
      return (n);
   }

   public static long readLittleEndianLong(RandomAccessFile f) throws IOException {
      long n, n1, n2, n3, n4, n5, n6, n7, n8;

      n1 = f.read();
      n2 = f.read();
      n3 = f.read();
      n4 = f.read();
      n5 = f.read();
      n6 = f.read();
      n7 = f.read();
      n8 = f.read();

      n = ((n8 << 56) + (n7 << 48) + (n6 << 40) + (n5 << 32) +
              (n4 << 24) + (n3 << 16) + (n2 << 8) + n1);
      return (n);
   }

   public static double readLittleEndianDouble(DataInputStream f) throws IOException {
      long l;
      double d;

      l = readLittleEndianLong(f);
      d = Double.longBitsToDouble(l);
      return (d);
   }

   public static double readLittleEndianDouble(RandomAccessFile f) throws IOException {
      long l;
      double d;

      l = readLittleEndianLong(f);
      d = Double.longBitsToDouble(l);
      return (d);
   }

   public static int readLittleEndianInt3(DataInput f) throws IOException {
      int n, n1, n2, n3, n4;

      n1 = f.readByte();
      n2 = f.readByte();
      n3 = f.readByte();
      n4 = f.readByte();
      n = ((n4 << 24) + (n3 << 16) + (n2 << 8) + n1);

      return (n);
   }

   public static int readLittleEndianInt2(DataInputStream f) throws IOException {
      int n, n1, n2, n3, n4;

      n1 = f.read();
      n2 = f.read();
      n3 = f.read();
      n4 = f.read();
      n = ((n4 << 24) + (n3 << 16) + (n2 << 8) + n1);

      return (n);
   }

   public static int readLittleEndianInt2(RandomAccessFile raf) throws IOException {
      int n, n1, n2, n3, n4;

      n1 = raf.read();
      n2 = raf.read();
      n3 = raf.read();
      n4 = raf.read();
      n = ((n4 << 24) + (n3 << 16) + (n2 << 8) + n1);

      return (n);
   }

   public static int readLittleEndianInt(DataInput in) throws IOException {

      int bigEndian = 0;
      for (int shiftBy = 0; shiftBy < 32; shiftBy += 8) {

         bigEndian |= (in.readUnsignedByte() & 0xff) << shiftBy;
      }

      return bigEndian;
   }

   public static short readLittleEndianShort(DataInput in) throws IOException {

      int low = in.readUnsignedByte() & 0xff;
      int high = in.readUnsignedByte();

      return (short) (high << 8 | low);
   }

   public static int readLittleEndianShortToInt(DataInput in) throws IOException {

      int low = in.readUnsignedByte() & 0xff;
      int high = in.readUnsignedByte();

      return high << 8 | low;
   }

   public static byte[] trimLeftSpaces(byte[] arr) {

      StringBuffer t_sb = new StringBuffer(arr.length);

      for (int i = 0; i < arr.length; i++) {

         if (arr[i] != ' ') {

            t_sb.append((char) arr[i]);
         }
      }

      return t_sb.toString().getBytes();
   }

   public static void writeLittleEndianShort(DataOutput out, short x) throws
           IOException {
      out.write((byte) (x & 0xFF));
      out.write((byte) ((x >>> 8) & 0xFF));
   }

   public static void writeLittleEndianShort(DataOutput out, int x) throws
           IOException {
      out.write((byte) (x & 0xFF));
      out.write((byte) ((x >>> 8) & 0xFF));
   }

   public static void writeLittleEndianInteger(DataOutput out, int x) throws
           IOException {
      out.write((byte) (x & 0xFF));
      out.write((byte) ((x >>> 8) & 0xFF));
      out.write((byte) ((x >>> 16) & 0xFF));
      out.write((byte) ((x >>> 24) & 0xFF));
   }

   public static void writeLittleEndianDouble(DataOutput out, double d) throws IOException {
      long l = Double.doubleToLongBits(d);
      writeLittleEndianLong(out, l);
   }


   public static void writeLittleEndianLong(DataOutput out, long l) throws IOException {
      out.write((byte) (l & 0xFF));
      out.write((byte) ((l >>> 8) & 0xFF));
      out.write((byte) ((l >>> 16) & 0xFF));
      out.write((byte) ((l >>> 24) & 0xFF));
      out.write((byte) ((l >>> 32) & 0xFF));
      out.write((byte) ((l >>> 40) & 0xFF));
      out.write((byte) ((l >>> 48) & 0xFF));
      out.write((byte) ((l >>> 56) & 0xFF));
   }

   public static byte[] textPadding(String text, String characterSetName,
                                    int length) throws Exception {

      return textPadding(text, characterSetName, length, DBFUtils.ALIGN_LEFT);
   }

   public static byte[] textPadding(String text, String characterSetName,
                                    int length, int alignment) throws Exception {

      return textPaddingSupportingUTF8(text, characterSetName, length, alignment, (byte) ' ');
   }

   public static byte[] textPadding(String text, String characterSetName,
                                    int length, int alignment,
                                    byte paddingByte) throws Exception {

      if (text.length() >= length) {
         return text.substring(0, length).getBytes(characterSetName);
      }

//   if (text.length() > length)
//     throw new Exception("Value '" + text +
//                         "' is too long for a column. Column size = " + length +
//                         ", value size = " + text.length());

      byte[] byte_array = new byte[length];
      Arrays.fill(byte_array, paddingByte);

      switch (alignment) {

         case ALIGN_LEFT:
            System.arraycopy(text.getBytes(characterSetName), 0, byte_array, 0,
                    text.length());
            break;

         case ALIGN_RIGHT:
            int t_offset = length - text.length();
            System.arraycopy(text.getBytes(characterSetName), 0, byte_array,
                    t_offset, text.length());
            break;
      }

      return byte_array;
   }

   public static byte[] textPaddingSupportingUTF8(String text, String characterSetName,
                                                  int length, int alignment,
                                                  byte paddingByte) throws Exception {
      byte[] encodedText = text.getBytes(characterSetName);

//      if (encodedText.length > length) {
//         throw new Exception("Value '" + text +
//                 "' is too long for a column. Column size = " + length +
//                 ", encoded value size = " + encodedText.length);
//      }

      if (encodedText.length > length) {
      // our encoded size is more than field length
      // so trim it
         return Arrays.copyOf(encodedText, length);
      }

      byte[] byte_array = new byte[length];
      Arrays.fill(byte_array, paddingByte);

      switch (alignment) {

         case ALIGN_LEFT:
            System.arraycopy(encodedText, 0, byte_array, 0,
                    encodedText.length);
            break;

         case ALIGN_RIGHT:
            int t_offset = length - text.length();
            System.arraycopy(encodedText, 0, byte_array,
                    t_offset, encodedText.length);
            break;
      }

      return byte_array;
   }

   public static byte[] bigDecimalFormating(BigDecimal bg,
                                            String characterSetName, int
           length) throws Exception {

      return textPadding(bg.toString(), characterSetName, length, ALIGN_RIGHT,
              (byte) ' ');
   }


   public static byte[] doubleFormating(double doubleNum,
                                        String characterSetName, int fieldLength,
                                        int sizeDecimalPart) throws Exception {

      int sizeWholePart = fieldLength -
              (sizeDecimalPart > 0 ? (sizeDecimalPart + 1) : 0);

      StringBuffer format = new StringBuffer(fieldLength);

      for (int i = 0; i < sizeWholePart; i++) {
         format.append("#");
      }

      if (sizeDecimalPart > 0) {
         format.append(".");
         for (int i = 0; i < sizeDecimalPart; i++) {
            format.append("0");
         }
      }

      DecimalFormat df = new DecimalFormat(format.toString(), dcs);

      return textPadding(df.format(doubleNum),
              characterSetName, fieldLength, ALIGN_RIGHT);
   }

   public static boolean contains(byte[] arr, byte value) {

      boolean found = false;
      for (int i = 0; i < arr.length; i++) {

         if (arr[i] == value) {

            found = true;
            break;
         }
      }

      return !found;
   }

   public static String getFileNameWithoutExtension(String fileName) {
      int dotPos = fileName.lastIndexOf('.');
      if (dotPos < 0)
         return fileName;
      return fileName.substring(0, dotPos);
   }

   public static void main(String[] args) throws Throwable {
      BigDecimal bg1 = new BigDecimal("1.0100");
      BigDecimal bg2 = new BigDecimal("1.01");
      System.out.println(bg1);
      System.out.println(bg2);
      System.out.println(bg1.compareTo(bg2));

      DecimalFormat df = new DecimalFormat("#.####", dcs);
      System.out.println(df.format(new BigDecimal("1.2345678901234552E14")));
   }

}
