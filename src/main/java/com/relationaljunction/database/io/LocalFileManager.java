package com.relationaljunction.database.io;

import java.io.*;
import java.nio.channels.FileLock;

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

public class LocalFileManager
        extends FileManager {
   private static final boolean CHECK_LOWER_AND_UPPER_FILE_CASES = true;
   String name;
   private File f;
   private FileLock lock = null;

   protected LocalFileManager() {
   }

   public LocalFileManager(LocalFileDirectoryManager dir, String name) {
      this.name = name;

      String dirPath = dir.getPath();

      if (dirPath.isEmpty() || dirPath.equals(".") || dirPath.equals("./") ||
              dirPath.equals(".\\"))
         // file in the current directory
         this.f = new File(name);
      else
         this.f = new File(dirPath + name);

      if (CHECK_LOWER_AND_UPPER_FILE_CASES)
         checkLowerAndUpperFileCases();
   }

   private void checkLowerAndUpperFileCases() {
      if (!f.exists()) {
         String dir = f.getParent() == null ? "" : f.getParent() + File.separator;
         String fileName = f.getName();
         File lowerFileName = new File(dir + fileName.toLowerCase());
         File upperFileName = new File(dir + fileName.toUpperCase());

         if (lowerFileName.exists())
            this.f = lowerFileName;
         else if (upperFileName.exists())
            this.f = upperFileName;
      }
   }

/*
  public LocalFileManager(String dirPath, String name) {
    if (dirPath.equals("") || dirPath.equals(".") || dirPath.equals("./") ||
        dirPath.equals(".\\"))
      // file in the current directory
      this.f = new File(name);
    else
      this.f = new File(new File(dirPath).getPath() + File.separator + name);
  }
*/

   // absolute path to the file
   public LocalFileManager(String filePath) {
      this.f = new File(filePath);

      if (CHECK_LOWER_AND_UPPER_FILE_CASES)
         checkLowerAndUpperFileCases();
   }

   // absolute path to the file
   public LocalFileManager(File f) {
      this.f = f;

      if (CHECK_LOWER_AND_UPPER_FILE_CASES)
         checkLowerAndUpperFileCases();
   }

   public RandomAccessFile getRandomAccess(String mode) throws Exception {
      return new RandomAccessFile(f, mode);
   }

   public OutputStream getOutputStream(boolean append, boolean sharedLock) throws Exception {
      FileOutputStream fos = new FileOutputStream(f, append);
      int time = 0;

      if (sharedLock) {
         // shared lock
         while ((lock = fos.getChannel().tryLock(0, Long.MAX_VALUE, true)) == null) {
            Thread.sleep(FileManager.LOCK_CHECK_PERIOD);
            //	System.out.println("waiting... time=" + time);
            time += FileManager.LOCK_CHECK_PERIOD;
            if (FileManager.LOCK_TIME_OUT != 0 && time > FileManager.LOCK_TIME_OUT)
               throw new Exception("Was trying to set a " + (sharedLock ?
                       "shared" : "exclusive") + " lock for a file " +
                       f.getName() + ". But lock time is out!");
         }
      } else {
         // exclusive lock
         while ((lock = fos.getChannel().tryLock()) == null) {
            Thread.sleep(FileManager.LOCK_CHECK_PERIOD);
            //	System.out.println("waiting... time=" + time);
            time += FileManager.LOCK_CHECK_PERIOD;
            if (FileManager.LOCK_TIME_OUT != 0 && time > FileManager.LOCK_TIME_OUT)
               throw new Exception("Was trying to set a " + (sharedLock ?
                       "shared" : "exclusive") + " lock for a file " +
                       f.getName() + ". But lock time is out!");
         }
      }

      return fos;
   }

   public OutputStream getOutputStream(boolean append) throws Exception {
      return new FileOutputStream(f, append);
   }

   public InputStream getInputStream(boolean sharedLock) throws Exception {
      FileInputStream fis = new FileInputStream(f);
      int time = 0;

      if (sharedLock) {
         // shared lock
         while ((lock = fis.getChannel().tryLock(0, Long.MAX_VALUE, true)) == null) {
            Thread.sleep(FileManager.LOCK_CHECK_PERIOD);
            //	System.out.println("waiting... time=" + time);
            time += FileManager.LOCK_CHECK_PERIOD;
            if (FileManager.LOCK_TIME_OUT != 0 && time > FileManager.LOCK_TIME_OUT)
               throw new Exception("Was trying to set a " + (sharedLock ?
                       "shared" : "exclusive") + " lock for a file " +
                       f.getName() + ". But lock time is out!");
         }
      } else {
         // exclusive lock
         while ((lock = fis.getChannel().tryLock()) == null) {
            Thread.sleep(FileManager.LOCK_CHECK_PERIOD);
            //	System.out.println("waiting... time=" + time);
            time += FileManager.LOCK_CHECK_PERIOD;
            if (FileManager.LOCK_TIME_OUT != 0 && time > FileManager.LOCK_TIME_OUT)
               throw new Exception("Was trying to set a " + (sharedLock ?
                       "shared" : "exclusive") + " lock for a file " +
                       f.getName() + ". But lock time is out!");
         }
      }

      return fis;
   }

   public java.util.Date getModificationTime() {
      return new java.util.Date(f.lastModified());
   }

   public InputStream getInputStream() throws Exception {
      return new FileInputStream(f);
   }

   public File getFile() {
      return f;
   }

   public String getPath() {
      return f.getPath();
   }

   public String getDirPath() {
      return getDir().getPath();
   }

   public DirectoryManager getDir() {
      return new LocalFileDirectoryManager(f.getParent() == null ? "." :
              f.getParent(), tempPath);
   }

   public String getName() {
      if (name == null) {
         return f.getName();
      } else {
         return name;
      }
   }

   public boolean exists() {
      return f.exists();
   }

   public boolean isReadOnly() {
      return !f.canWrite();
   }

   public boolean isDirectory() {
      return f.isDirectory();
   }

   @Override
   public void create() throws Exception {
      boolean result = f.createNewFile();
      if (!result) throw new UnexpectedException("can't create the file '" + f.getPath() + "'");
   }

   public void delete() throws Exception {
      boolean result = f.delete();
//      if (!result) f.deleteOnExit();
      if (!result) throw new UnexpectedException("can't delete file '" + f.getPath() +
              File.separator + f.getName() + "'");
   }

   public void unlock() throws Exception {
      if (lock != null) {
         lock.release();
         lock = null;
      }
   }

   public void close() throws Exception {
      unlock();
      this.f = null;
      this.dir = null;
   }

   public static void main(String[] args) {
      try {
         File f = new File("c:/a.txt");
         System.out.println(f.getParent());

         f = new File("c:/dir/a.txt");
         System.out.println(f.getParent());

         f = new File("a.txt");
         System.out.println(f.getParent());

         f = new File("./a.txt");
         System.out.println(f.getParent());

         f = new File("../a.txt");
         System.out.println(f.getParent());

         LocalFileManager lf = new LocalFileManager("c:/java/test/files/test.dbf");
         System.out.println("path = " + lf.getPath());
         System.out.println("dir path = " + lf.getDirPath());
         System.out.println("file name = " + lf.getName());

         lf = new LocalFileManager("test.dbf");
         System.out.println("\npath = " + lf.getPath());
         System.out.println("dir path = " + lf.getDirPath());
         System.out.println("file name = " + lf.getName());

         lf = new LocalFileManager("dbffiles/test.dbf");
         System.out.println("\npath = " + lf.getPath());
         System.out.println("dir path = " + lf.getDirPath());
         System.out.println("file name = " + lf.getName());

         LocalFileDirectoryManager ld = new LocalFileDirectoryManager(
                 "c:/java/test/files");
         lf = (LocalFileManager) ld.getFileManager("test.dbf");
         System.out.println("\npath = " + lf.getPath());
         System.out.println("dir path = " + lf.getDirPath());
         System.out.println("file name = " + lf.getName());

         lf = (LocalFileManager) ld.getFileManager("foxpro/test.dbf");
         System.out.println("\npath = " + lf.getPath());
         System.out.println("dir path = " + lf.getDirPath());
         System.out.println("file name = " + lf.getName());

//      ld = new LocalFileDirectoryManager("");
//      lf = (LocalFileManager) ld.getFileManager("test.dbf");
//      System.out.println("\npath = " + lf.getPath());
//      System.out.println("dir path = " + lf.getDirPath());
//      System.out.println("file name = " + lf.getName());

         ld = new LocalFileDirectoryManager(".");
         lf = (LocalFileManager) ld.getFileManager("test.dbf");
         System.out.println("\npath = " + lf.getPath());
         System.out.println("dir path = " + lf.getDirPath());
         System.out.println("file name = " + lf.getName());

         ld = new LocalFileDirectoryManager("dbffiles");
         lf = (LocalFileManager) ld.getFileManager("test.dbf");
         System.out.println("\npath = " + lf.getPath());
         System.out.println("dir path = " + lf.getDirPath());
         System.out.println("file name = " + lf.getName());

         ld = new LocalFileDirectoryManager("./dbffiles./");
         lf = (LocalFileManager) ld.getFileManager("test.dbf");
         System.out.println("\npath = " + lf.getPath());
         System.out.println("dir path = " + lf.getDirPath());
         System.out.println("file name = " + lf.getName());
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }
}
