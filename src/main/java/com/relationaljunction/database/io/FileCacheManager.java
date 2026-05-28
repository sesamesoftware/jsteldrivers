package com.relationaljunction.database.io;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.utils.FileUtils;

// it should extend LocalFileManager
public class FileCacheManager
        extends LocalFileManager {
   private final boolean createRandomTempSubDir;

   private final Logger log = LoggerFactory.getLogger("FileCacheManager");

   private FileManager originalFileManager = null;
   private LocalFileManager cacheFileManager = null;

   public FileCacheManager(FileManager fm, String tempPath) throws Exception {
      this(fm, tempPath, false, false);
   }

   /**
    * @param fm                     - FileManager to be cached
    * @param tempPath               - path to a temporary directory
    * @param createEmptyFile        - create an empty file on both cache and source sides
    * @param createRandomTempSubDir - create subdir in a temporary directory.
    *                               It will then be deleted after closing.
    * @throws Exception
    */
   public FileCacheManager(FileManager fm, String tempPath,
                           boolean createEmptyFile,
                           boolean createRandomTempSubDir)
           throws Exception {
      this.createRandomTempSubDir = createRandomTempSubDir;

      if (createRandomTempSubDir)
         this.tempPath = FileUtils.CreateRandomSubDir(tempPath);
      else
         this.tempPath = tempPath;

      originalFileManager = fm;

      String cacheFilePath = this.tempPath + File.separator + fm.getName();

      com.relationaljunction.utils.OtherUtils.writeLogInfo(log,
              "FileCacheManager: the temporary file '"
                      + cacheFilePath + "' was created for caching the file '" + fm.getPath() + "'");

      cacheFileManager = new LocalFileManager(cacheFilePath);

      OutputStream os;

      // create temp subdirs if it is required
      if (new File(fm.getName()).getParentFile() != null)
         new File(cacheFilePath).getParentFile().mkdirs();

      // just create an empty temp file, not copy data.
      if (createEmptyFile) {
         // create an empty temp file
         cacheFileManager.getFile().delete();
         cacheFileManager.getFile().createNewFile();
         // create the same file on the source side
         flush();
         return;
      }

      // copy data from an original file to a temp file
      InputStream is = originalFileManager.getInputStream();
      os = cacheFileManager.getOutputStream(false);

      if (is == null)
         throw new Exception("File to be cached '" + originalFileManager.getPath() +
                 "' is not found");

      FileUtils.copyStream(is, os);
   }

   public void delete() throws Exception {
      originalFileManager.delete();
      cacheFileManager.delete();
   }

   public boolean exists() {
      return true;
//      try {
//         return originalFileManager.exists();
//      } catch (Exception e) {
//         throw new UnexpectedException("Error in FileCacheManager.exists(). Error was: " + e.getMessage(), e);
//      }
   }

   public java.util.Date getModificationTime() {
      return originalFileManager.getModificationTime();
   }

   public DirectoryManager getDir() {
      return new FileCacheDirectoryManager(originalFileManager.getDir(), tempPath, false);
   }

   public String getDirPath() {
      return cacheFileManager.getDirPath();
   }

   public InputStream getInputStream() throws Exception {
      return cacheFileManager.getInputStream();
   }

   public String getName() {
      return originalFileManager.getName();
   }

   public OutputStream getOutputStream(boolean append) throws Exception {
      if (originalFileManager.exists() && originalFileManager.isReadOnly())
         throw new Exception("File '" + originalFileManager.getPath() +
                 "' is read-only!");
//    updated = true;
      return cacheFileManager.getOutputStream(append);
   }

   public File getFile() {
      return cacheFileManager.getFile();
   }

   public String getPath() {
      return cacheFileManager.getPath();
   }

   public RandomAccessFile getRandomAccess(String mode) throws Exception {
      if (originalFileManager.exists() && originalFileManager.isReadOnly())
         throw new Exception("File '" + originalFileManager.getPath() +
                 "' is read-only!");
      return cacheFileManager.getRandomAccess(mode);
   }

   public boolean isReadOnly() {
      return originalFileManager.isReadOnly();
   }

   public boolean isDirectory() {
      return originalFileManager.isDirectory();
   }

   public void flush() throws Exception {
//    if (updated) {
      InputStream is = cacheFileManager.getInputStream();
      OutputStream os = originalFileManager.getOutputStream(false);
      if (is == null)
         throw new Exception("File to be cached '" + originalFileManager.getName() +
                 "' is not found");
      FileUtils.copyStream(is, os);

      com.relationaljunction.utils.OtherUtils.writeLogInfo(log,
              "FileCacheDirectoryManager: the cache file '" + cacheFileManager.getPath() +
                      "' was uploaded.");
//    }

//    updated = false;
   }

   protected void finalize() throws Throwable {
      try {
         super.finalize();
         close();
      } catch (Exception ex) {
         log.warn(ex.getMessage(), ex);
      }
   }

   public void close() throws Exception {
      if (cacheFileManager == null || originalFileManager == null)
         return;

//    flush();
      cacheFileManager.delete();

      cacheFileManager.close();
      originalFileManager.close();

      if (this.createRandomTempSubDir)
         FileUtils.clearAllDir(new File(tempPath));

      cacheFileManager = null;
      originalFileManager = null;
   }

}
