package com.relationaljunction.database.io;

import java.io.*;
import java.util.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

abstract public class FileManager {
   static int LOCK_CHECK_PERIOD = 500;
   static int LOCK_TIME_OUT = 30000;

   final static int FILE = 0;
   final static int DIRECT_HTTP = 1;
   final static int DIRECT_FTP = 2;
   final static int HTTP = 3;
   final static int FTP = 4;
   final static int ZIP = 5;
   final static int SMB = 6;
   final static int CLASSPATH = 7;
   final static int CACHE = 8;
   final static int ABSOLUTE_FILE = 9;
   final static int RELATIVE = 10;
   final static int SFTP = 11;

   protected String tempPath = null;
   protected DirectoryManager dir = null;

   public static FileManager buildFileManager(DirectoryManager baseDir,
                                              String filePath) throws Exception {
      return buildFileManager(baseDir, filePath, null, false);
   }

   public static FileManager buildFileManager(DirectoryManager baseDir,
                                              String filePath,
                                              String tempPath,
                                              boolean createTempSubDir
   ) throws Exception {
      int pathType = checkPath(filePath);
      return buildFileManager(baseDir, filePath, tempPath, createTempSubDir, pathType);
   }

   static FileManager buildFileManager(DirectoryManager baseDir,
                                       String filePath,
                                       String tempPath,
                                       int pathType) throws Exception {
      return buildFileManager(baseDir, filePath, tempPath, false, pathType);
   }

   static FileManager buildFileManager(DirectoryManager baseDir,
                                       String filePath,
                                       String tempPath,
                                       boolean createTempSubDir,
                                       int pathType) throws Exception {
      switch (pathType) {

         // check for http path
         case HTTP:
            return new HTTPFileManager(filePath);

         // check for ftp path
         case FTP:
            return new FTPFileManager(filePath);

         // check for sftp path
         case SFTP:
            return new CommonsVFSFileManager(filePath);

         // check for absolute zip path
         case ZIP:
            return new ZipFileManager(filePath);

         // check class path
         case CLASSPATH:
            return new ClasspathFileManager(filePath);

         // check smb
         case SMB:
            return new SMBFileManager(filePath);

         // check for absolute file path with the protocol file://
         case FILE:
            return new LocalFileManager(filePath.substring("file://".length()));

         // check for cache
         case CACHE:
            return new FileCacheManager(buildFileManager(baseDir, filePath.
                    substring("cache://".length()),
                    tempPath, createTempSubDir),
                    tempPath, false, createTempSubDir);

         // check for absolute file path
         case ABSOLUTE_FILE:
            return new LocalFileManager(filePath);

         default: {
            FileManager fm;

            // check for file path relative to the dir
            if (baseDir == null)
               // file path relative to the current dir
               fm = new LocalFileDirectoryManager(".").getFileManager(filePath);
            else
               // file path relative to the specified baseDir
               fm = baseDir.getFileManager(filePath);

            if (fm.isDirectory()) throw new IllegalArgumentException("File '" + filePath +
                    "' must not be a directory!");
            return fm;
         }
      } //switch
   }

   // Creates a FileManager instance. baseDir - dir
   /*
    public static FileManager buildFileManager(DirectoryManager baseDir,
   String filePath, String tempPath) throws Exception {
      FileManager fm = null;

      // check for http path
      if (filePath.startsWith("http://"))
        fm = new HTTPFileManager(filePath);

      // check for ftp path
      else if (filePath.startsWith("ftp://"))
        fm = new FTPFileManager(filePath);

      // check for absolute zip path
      else if (filePath.startsWith("zip://") || filePath.startsWith("jar://"))
        fm = new ZipFileManager(filePath);

      // check class path
      else if (filePath.startsWith("classpath://"))
        fm = new ClasspathFileManager(filePath);

      // check smb
      else if (filePath.startsWith("smb://"))
        fm = new SMBFileManager(filePath);

      // check for absolute file path with the protocol file://
      else if (filePath.startsWith("file://"))
      fm = new LocalFileManager(filePath.substring("file://".length()));

      // check for cache
      else if (filePath.startsWith("cache://"))
        fm = new FileCacheManager(buildFileManager(baseDir, filePath.
   substring("cache://".length()),
                                                   tempPath), tempPath);

      // check for the file directory
      else if (new File(filePath).isDirectory())
      throw new Exception("The file '" + filePath +
                          "' must not be a directory");

      // check for absolute file path
      else if (new File(filePath).isAbsolute()) {
        fm = new LocalFileManager(filePath);
      }

      // check for file path relative to the dir
      else if (baseDir == null)
        // file path relative to the current dir
        fm = new LocalFileDirectoryManager(".").getFileManager(filePath);
      else
        // file path relative to the specified baseDir
        fm = baseDir.getFileManager(filePath);

      if (fm.isDirectory())throw new Exception("File '" + filePath +
                                               "' must not be a directory!");

      return fm;
    }
   */

   static int checkPath(String filePath) throws Exception {
      // validate argument(s)
      if (filePath == null || filePath.isEmpty()) {
         throw new IllegalArgumentException(
                 "'path' argument may not be empty or null");
      }

      // check for http path
      if (filePath.startsWith("http://"))
         return HTTP;

         // check for ftp path
      else if (filePath.startsWith("ftp://"))
         return FTP;

         // check for sftp path
      else if (filePath.startsWith("sftp://"))
         return SFTP;

         // check for absolute zip path
      else if (filePath.startsWith("zip://") || filePath.startsWith("jar://"))
         return ZIP;

         // check class path
      else if (filePath.startsWith("classpath://"))
         return CLASSPATH;

         // check smb
      else if (filePath.startsWith("smb://"))
         return SMB;

         // check for absolute file path with the protocol file://
      else if (filePath.startsWith("file://"))
         return FILE;

         // check cache
      else if (filePath.startsWith("cache://"))
         return CACHE;

         // check for the file directory
      else if (new File(filePath).isDirectory())
         throw new Exception("The file '" + filePath +
                 "' must not be a directory");

         // check for absolute file path
      else if (new File(filePath).isAbsolute())
         return ABSOLUTE_FILE;

      return RELATIVE;
   }

   public InputStream getInputStream(boolean sharedLock) throws Exception {
      return getInputStream();
   }

   public abstract InputStream getInputStream() throws Exception;

   public OutputStream getOutputStream(boolean append, boolean sharedLock) throws
           Exception {
      return getOutputStream(append);
   }

   public abstract OutputStream getOutputStream(boolean append) throws
           Exception;

   abstract public RandomAccessFile getRandomAccess(String mode) throws
           Exception;

   // return a full path to the file (dirPath + fileName).
   // note: fileName may contain parent dirs as well, e.g. fileName = subfolder/subfolder2/data.csv
   // e.g. c:/csvfiles/data.csv or ftp://site.com/data.csv
   abstract public String getPath();

   // return a full path to the directory (catalog) which contains the file
   // dirPath must contain separator in the end
   // e.g. c:/csvfiles/ or ftp://site.com/
   abstract public String getDirPath();

   // return DirectoryManager where is FileManager located
   abstract public DirectoryManager getDir();

   // return a File instance if it is possible
   public File getFile() {
      throw new UnsupportedOperationException("getFile() is not supported for " + this.getClass());
   }

   // return a name of the file only. Doesn't contain parent dirs.
   abstract public String getName();

   public Date getModificationTime() {
      return null;
   }

   abstract public boolean exists() throws Exception;

   abstract public boolean isReadOnly();

   public boolean isDirectory() {
      return false;
   }

   // for using by directories
   public abstract void create() throws Exception;

   // for using by directories
   public abstract void delete() throws Exception;

   public void flush() throws Exception {
   }

   public void close() throws Exception {
      this.dir = null;
   }

   public void unlock() throws Exception {
   }

   public void upload() throws Exception {
      getDir().upload(this);
   }

   public void setTempPath(String tempPath) {
      this.tempPath = tempPath;
   }

   public String getTempPath() {
      return tempPath;
   }

}
