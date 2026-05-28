package com.relationaljunction.database.dbf;

import java.io.*;
import java.nio.channels.FileLock;
import java.util.Vector;

import com.relationaljunction.utils.FileUtils;

public class DBFInfo {
   private static final int RECORD_COUNT = 4;
   private static final int HEADER_LENGTH = 8;
   private static final int RECORD_LENGTH = 10;

   private static final int RECORD_LENGTH_POSITION = 4;

   private RandomAccessFile raf = null;

   private final byte signature; /* 0 */
   private final byte year; /* 1 */
   private final byte month; /* 2 */
   private final byte day; /* 3 */
   private final int numberOfRecords; /* 4-7 */
   private final int headerLength; /* 8-9 */
   private final short recordLength; /* 10-11 */

   private FileLock fileLock = null;

   public DBFInfo(String fileName, boolean readOnly, boolean lock) throws Exception {
      if (lock) {
         Vector<RandomAccessFile> rafResult = new Vector<RandomAccessFile>();
         fileLock = FileUtils.lockFile(new File(fileName), readOnly,
                 DBFBase.LOCK_CHECK_PERIOD, DBFBase.LOCK_TIME_OUT, rafResult);
         raf = rafResult.get(0);
      } else {
         if (readOnly)
            raf = new RandomAccessFile(fileName, "r");
         else
            raf = new RandomAccessFile(fileName, "rw");
      }

      signature = raf.readByte(); /* 0 */
      year = raf.readByte(); /* 1 */
      month = raf.readByte(); /* 2 */
      day = raf.readByte(); /* 3 */
      numberOfRecords = DBFUtils.readLittleEndianInt(raf); /* 4-7 */

      headerLength = DBFUtils.readLittleEndianShortToInt(raf); /* 8-9 */
      recordLength = DBFUtils.readLittleEndianShort(raf); /* 10-11 */
   }

   public byte getSignature() {
      return signature;
   }

   public long getSize() throws Exception {
      return raf.length();
   }

//  public long calculateTrueSize(int recordCount) throws Exception{
//    raf.seek(HEADER_LENGTH);
//    int headerLength = DBFUtils.readLittleEndianShortToInt(raf);
//    short recLength = DBFUtils.readLittleEndianShort(raf);
//    return headerLength + (recLength * recordCount) + 1;
//  }

   public long getTrueSize() throws Exception {
      return headerLength + ((long) recordLength * numberOfRecords) + 1;
   }

   public void clearRecords() throws Exception {
      // write modified date and record count
      raf.seek(RECORD_LENGTH_POSITION);
      DBFUtils.writeLittleEndianInteger(raf, 0); /* 4-7 */
      raf.setLength(headerLength);
   }

   public void trimToSize(long size) throws Exception {
      raf.setLength(size);
   }

   public void close() throws Exception {
      // release a lock
      if (fileLock != null) fileLock.release();

      raf.close();

      raf = null;
      fileLock = null;
   }

}
