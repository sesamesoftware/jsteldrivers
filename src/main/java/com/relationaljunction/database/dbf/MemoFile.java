package com.relationaljunction.database.dbf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.utils.FileUtils;

public class MemoFile {
   private final Logger log = LoggerFactory.getLogger("MemoFile");

   public final static int MEMO_PICTURE_TYPE = 0;
   public final static int MEMO_TEXT_TYPE = 1;
   public final static int MEMO_OBJECT_TYPE = 2;

   public static short DEFAULT_MEMO_BLOCK_SIZE = 512;
   public static int DEFAULT_MEMO_MAX_SIZE = 4 * 1024 * 1024;
   public static short HEADER_OFFSET = 512;

   private final static long FREE_BLOCK_POINTER_POSITION = 0;
   private final static long MEMO_BLOCK_SIZE_POSITION = 6;
   private final static byte BYTEZERO = (byte) '0';
   private final static byte BYTESPACE = (byte) ' ';

   private FileLock fileLock = null;
   private RandomAccessFile raf;
   private short memoBlockSize = DEFAULT_MEMO_BLOCK_SIZE;
   private int memoMaxSizeInBytes = DEFAULT_MEMO_MAX_SIZE;
   private int nextFreeBlock = 1;
   private boolean exist = true;
   private boolean readOnly = true;


   // creates a memo file
   public MemoFile(String fileName, short memoBlockSize) throws DBFException {
      this(new File(fileName), memoBlockSize);
   }

/*
  // creates a memo file
  public MemoFile(File f, short memoBlockSize) throws DBFException {
    try {
      FileOutputStream fos = new FileOutputStream(f, false);
      fos.close();

      this.memoBlockSize = memoBlockSize;
      this.raf = new RandomAccessFile(f, "rw");

      // 00-03 write next free block pointer
      nextFreeBlock = 1;
      raf.writeInt(nextFreeBlock);
      // 04-05 not used
      raf.writeShort(0);
      // 06-07 write memo block size
      raf.writeShort(memoBlockSize);

      // 08 - memoBlockSize not used
      raf.write(new byte[memoBlockSize - 8]);
    }
    catch (Exception ex) {
      throw new DBFException("Can't create a memo file " + f.getName() +
                             " Error was: " + ex.getMessage());
    }
  }
*/

   // creates a memo file
   public MemoFile(File f, short memoBlockSize) throws DBFException {
      try {
         FileOutputStream fos = new FileOutputStream(f, false);
         fos.close();

         this.memoBlockSize = memoBlockSize;
         this.raf = new RandomAccessFile(f, "rw");

         // 00-03 write next free block pointer
         if (memoBlockSize > HEADER_OFFSET)
            // memo block size = 1024, 2048, etc
            nextFreeBlock = 1;
         else
            nextFreeBlock = HEADER_OFFSET / memoBlockSize;

         raf.writeInt(nextFreeBlock);
         // 04-05 not used
         raf.writeShort(0);
         // 06-07 write memo block size
         raf.writeShort(memoBlockSize);

         // 08 - 511 unused
         if (memoBlockSize > HEADER_OFFSET)
            // memo block size = 1024, 2048, etc
            raf.write(new byte[memoBlockSize - 8]);
         else
            raf.write(new byte[HEADER_OFFSET - 8]);
      } catch (Exception ex) {
         throw new DBFException("Can't create a memo file " + f.getName() +
                 " Error was: " + ex.getMessage());
      }
   }

   // opens a memo file
   public MemoFile(String fileName, boolean readOnly, boolean lock) throws Exception {
      this(new File(fileName), readOnly, lock);
   }

   // opens a memo file
   public MemoFile(File f, boolean readOnly, boolean lock) throws DBFException {
      this.readOnly = readOnly;

      exist = f.exists();

      if (DBFBase.ignoreMemoFile == DBFBase.IGNORE_MEMO_FILE_ALWAYS) return;
      else if (!exist &&
              DBFBase.ignoreMemoFile != DBFBase.IGNORE_MEMO_FILE_IF_NOT_EXIST)
         throw new DBFException(
                 "can't find a memo file: " + f.getPath());
      else if (!exist &&
              DBFBase.ignoreMemoFile == DBFBase.IGNORE_MEMO_FILE_IF_NOT_EXIST)
         return;

      try {
         if (lock) {
            Vector<RandomAccessFile> rafResult = new Vector<RandomAccessFile>();
            fileLock = FileUtils.lockFile(f, readOnly,
                    DBFBase.LOCK_CHECK_PERIOD, DBFBase.LOCK_TIME_OUT, rafResult);
            raf = rafResult.get(0);
         } else {
            if (readOnly)
               raf = new RandomAccessFile(f, "r");
            else
               raf = new RandomAccessFile(f, "rw");
         }

         // read next free block pointer
         raf.seek(FREE_BLOCK_POINTER_POSITION);
         nextFreeBlock = raf.readInt();
         // read memo block size
         raf.seek(MEMO_BLOCK_SIZE_POSITION);
         memoBlockSize = raf.readShort();
      } catch (Exception ex) {
         throw new DBFException("Can't read a memo file " + f.getName() +
                 ". Error was: " + ex.getMessage());
      }
   }

   public void setMemoMaxSizeInBytes(int memoMaxSizeInBytes) {
      this.memoMaxSizeInBytes = memoMaxSizeInBytes;
   }

   public int addMemo(byte[] memoBytes) throws DBFException {
      return addMemo(memoBytes, MEMO_TEXT_TYPE);
   }

   public int addMemo(byte[] memoBytes, int memoSignature) throws DBFException {
      int curFreeBlock = nextFreeBlock;

      if (memoBytes.length == 0)
         return 0;

      try {
         int specifiedBlock = memoBlockSize * nextFreeBlock;
         // go to free block
         raf.seek(specifiedBlock);

         // write memo signature
         raf.writeInt(memoSignature);
         // write memo length
         raf.writeInt(memoBytes.length);
         // write memo data
         raf.write(memoBytes);

         // calculate free block pointer and rest bytes to fill block
         int blockCountEmployed = getBlockCountEmployed(memoBytes.length);

         int restBytesLength = (blockCountEmployed * memoBlockSize) -
                 (memoBytes.length + 8);
         raf.write(new byte[restBytesLength]);

         // write free block pointer
         nextFreeBlock += blockCountEmployed;
         raf.seek(FREE_BLOCK_POINTER_POSITION);
         raf.writeInt(nextFreeBlock);
      } catch (IOException ex) {
         throw new DBFException("Can't write a memo record. Error was: " +
                 ex.getMessage());
      }

      return curFreeBlock;
   }

   public int updateMemo(int pointer, byte[] memoBytes) throws DBFException {
      return updateMemo(pointer, memoBytes, MEMO_TEXT_TYPE);
   }

   public int updateMemo(int pointer, byte[] memoBytes, int memoSignature) throws DBFException {

       if (memoBytes.length == 0)
         return 0;

      try {
         int specifiedBlock = memoBlockSize * pointer;
         // go to free block
         raf.seek(specifiedBlock);

         // read memo signature
         int oldMemoSignature = raf.readInt();
         // read memo length
         int oldMemoLength = raf.readInt();

         // calculate free block pointer and rest bytes to fill block
         int oldBlockCountEmployed = getBlockCountEmployed(oldMemoLength);
         int newBlockCountEmployed = getBlockCountEmployed(memoBytes.length);

         if (oldBlockCountEmployed >= newBlockCountEmployed) {
            // go to free block
            raf.seek(specifiedBlock);

            // write memo signature
            raf.writeInt(memoSignature);
            // write memo length
            raf.writeInt(memoBytes.length);
            // write memo data
            raf.write(memoBytes);

            // write rest bytes
            int restBytesLength = (oldBlockCountEmployed * memoBlockSize) -
                    (memoBytes.length + 8);
            raf.write(new byte[restBytesLength]);
         } else {
            return addMemo(memoBytes, memoSignature);
         }
      } catch (IOException ex) {
         throw new DBFException("Can't update a memo record. Error was: " +
                 ex.getMessage());
      }
      return pointer;
   }

   private int getBlockCountEmployed(int bytesCount) {
      int blockCountEmployed = (bytesCount + 8) / memoBlockSize;
      if ((bytesCount + 8) > (blockCountEmployed * memoBlockSize))
         blockCountEmployed++;
      return blockCountEmployed;
   }

   public byte[] readMemoData(int memoPointer) throws DBFException {
      try {
         // go to specified block
         raf.seek((long) memoBlockSize * memoPointer);
         // read memo signature
         int memoSignature = raf.readInt();

         // read memo length
         int memoLength = raf.readInt();

         // test for maximum memo size
         if (memoLength > memoMaxSizeInBytes) {
            log.warn("Memo element size (" + memoLength + ") with pointer " + memoPointer +
                    " exceeds maximum size allowed (" + memoMaxSizeInBytes +
                    "). It can mean that memo file is corrupted.");
            memoLength = memoMaxSizeInBytes;
         }

         // read memo data
         byte[] memoBytes = new byte[memoLength];
         raf.read(memoBytes);

         return memoBytes;
      } catch (IOException ex) {
         throw new DBFException("Can't read a memo record. Error was: " +
                 ex.getMessage());
      }
   }

   public void close() throws Exception {
      // release a lock
      if (fileLock != null) fileLock.release();

      if (raf != null)
         raf.close();

      raf = null;
      fileLock = null;
   }

   public boolean exists() {
      return !exist;
   }

   public short getMemoBlockSize() {
      return memoBlockSize;
   }

   public static void main(String[] args) throws Throwable {
//    MemoFile memo = new MemoFile("dbffiles/vfp_with_memo.fpt");
//    byte[] memoData = memo.readMemoData(5);
//    System.out.println(new String(memoData));

      MemoFile memo = new MemoFile(new File("c:/temp/test_memo.fpt"), (short) 32);
      int pointer = memo.addMemo("aaaaaaaa".getBytes());
      int pointer2 = memo.addMemo("aaaaaaaaaaaaaaaaaaaaaaaa".getBytes());
      memo.addMemo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes());
      memo.addMemo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes());

      memo.updateMemo(pointer, "updated".getBytes());
      memo.updateMemo(pointer2, "updaaaaaaaaaaaaaaaaaaaaa".getBytes());
      memo.updateMemo(pointer2, "updaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes());
   }
}
