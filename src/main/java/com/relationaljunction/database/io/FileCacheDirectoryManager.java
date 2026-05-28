package com.relationaljunction.database.io;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.utils.FileUtils;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.UnexpectedException;
import com.relationaljunction.utils.concurrency.FutureConcurrentHashMap;

public class FileCacheDirectoryManager extends DirectoryManager {
	   private final Logger log = LoggerFactory.getLogger("FileCacheDirectoryManager");

   public static boolean UPLOAD_USING_TEMP_FILE = true;

   DirectoryManager dir;
   FutureConcurrentHashMap<String, FileObjectInfo> fileCache =
           new FutureConcurrentHashMap<String, FileObjectInfo>();
   String cacheFilesPath = null;

   public FileCacheDirectoryManager(DirectoryManager dir, String tempPath) {
      this(dir, tempPath, true);
   }

   public FileCacheDirectoryManager(DirectoryManager dir, String tempPath, boolean createRandomTempSubDir) {
      this.dir = dir;

      // create random temporary sub directory
      if (createRandomTempSubDir)
         cacheFilesPath = FileUtils.CreateRandomSubDir(tempPath);
      else
         cacheFilesPath = tempPath;

      OtherUtils.writeLogInfo(log,
              "FileCacheDirectoryManager: the temporary directory '"
                      + cacheFilesPath + "' was created for caching files in '" + dir.getPath() + "'");
   }

   @Override
   public boolean isReadOnly() {
      return dir.isReadOnly();
   }

   @Override
   public void createFile(String file) throws Exception {
      dir.createFile(file);
   }

   @Override
   public void dropFile(String file) throws Exception {
      // delete an original file
      dir.dropFile(file);
      // delete a cache file
      removeFileFromCache(file);
   }

   @Override
   public void rename(String oldFile, String newFile) throws Exception {
      // delete from cache as well
      dropFile(oldFile);
      // new file is not required to be uploaded into the cache. getFileManager() will do that.
      dir.rename(oldFile, newFile);
   }

   /**
    * delete only a cache file and remove a reference to it from the cache
    *
    * @param fileName
    */
   public void removeFileFromCache(String fileName) {
      try {
         FileObjectInfo info = fileCache.get(fileName.toUpperCase());
         if (info != null) {
            info.getFileManager().delete();
            fileCache.remove(fileName.toUpperCase());
         }
      } catch (Exception e) {
         log.warn("FileCacheDirectoryManager: error while deleting '" +
                 fileName + "' from the cache. ", e);
      }

      OtherUtils.writeLogInfo(log,
              "FileCacheDirectoryManager: the cache file '" + fileName +
                      "' was deleted and removed from the cache.");

      OtherUtils.writeTraceInfo(log, "FileCache elements: " + fileCache.toString());
   }

   public boolean supportsAppending() {
      return true;
   }

   public boolean exists(String file) {
      try {
         return dir.getFileManager(file).exists();
      } catch (Exception ex) {
         return false;
      }
   }

   public java.util.Date getFileModificationDate(String fileName) throws Exception {
      return dir.getFileManager(fileName).getModificationTime();
   }

   public FileManager getFileManager(final String fileName) throws Exception {
      int pathType = FileManager.checkPath(fileName);

      // ##### absolute file path
      if (pathType != FileManager.RELATIVE)
         return FileManager.buildFileManager(this, fileName, tempPath, pathType);

      // ##### relative file path
      FileManager fm = fileCache.putIfAbsent(fileName.toUpperCase(), new Callable<FileObjectInfo>() {
         public FileObjectInfo call() throws Exception {
            // get a path to a local cache file
            String tempFilePath = OtherUtils.getTempFilePath(cacheFilesPath,
                    fileName);
            File tempFile = new File(tempFilePath);

            // create temp subdirs if it is required
            if (new File(fileName).getParentFile() != null) {
               boolean result = tempFile.getParentFile().mkdirs();
               // don't throw an exception if result = false
//               if (!result) throw new UnexpectedException("File cache: can't create subdirs");
            }

            // open a temp file for writing
            FileManager fmOut = new LocalFileManager(tempFile);
            fmOut.setTempPath(tempPath);

            // ##### copy an original file to a local cache file
            FileManager fm = dir.getFileManager(fileName);

            InputStream is = fm.getInputStream();
            if (is == null)
               throw new Exception("File to be cached '" + fm.getPath() +
                       "' does not exist!");

            OutputStream os = fmOut.getOutputStream(false);
            FileUtils.copyStream(is, os);

            OtherUtils.writeLogInfo(log,
                    "FileCacheDirectoryManager: the initial file '" + fm.getPath() +
                            "' was cached to '" + tempFile.getAbsolutePath() + "'");
            // #####

            // add new object to the cache
            FileObjectInfo newFileInfo = new FileObjectInfo();
            newFileInfo.setFileManager(fmOut);

            return newFileInfo;
         }
      }).getFileManager();

      OtherUtils.writeTraceInfo(log, "FileCache elements: " + fileCache.toString());

      return fm;
/*
      if (fileInfo == null) {
         // get a path to a local cache file
         String tempFilePath = com.relationaljunction.utils.OtherUtils.getTempFilePath(cacheFilesPath,
                 fileName);
         File tempFile = new File(tempFilePath);

         // create temp subdirs if it is required
         if (new File(fileName).getParentFile() != null)
            tempFile.getParentFile().mkdirs();

         // open a temp file for writing
         fmOut = new LocalFileManager(tempFile);
         fmOut.setTempPath(tempPath);

         // copy an original file to a local cache file
         // not cache hit
         FileManager fm = dir.getFileManager(fileName);
//      if (!fm.exists())throw new Exception("File to be cached '" + fm.getPath() +
//                                           "' does not exist!");

         InputStream is = fm.getInputStream();
         if (is == null)
            throw new Exception("File to be cached '" + fm.getPath() +
                    "' does not exist!");

         OutputStream os = fmOut.getOutputStream(false);
         FileUtils.copyStream(is, os);

         // add new object to the cache
         FileObjectInfo newFileInfo = new FileObjectInfo();
         fmOut.addListener(this);
         newFileInfo.setFileManager(fmOut);
         fileCache.set(fileName, newFileInfo);
      } else {
         //cache hit
         return fileInfo.getFileManager();
      }

      return fmOut;
*/
   }

//  public File getFile(String fileName) throws java.lang.Exception {
//    LocalFileManager fmOut = (LocalFileManager) getFileManager(fileName);
//    return fmOut.getFile();
//  }

   @Override
   public void upload(FileManager fm) throws Exception {
      if (UPLOAD_USING_TEMP_FILE) {
         // copy a cache file to a temp file on original server
         // and then rename a temp file to a destination file
         try {
            // previously delete a .tmp file, if it exists
            if (dir.exists(fm.getName() + ".tmp")) {
               dir.dropFile(fm.getName() + ".tmp");
            }
         } catch (Exception e) {
            throw new UnexpectedException("File cache: can't drop a temp file '" + fm.getName() + ".tmp'", e);
         }

         // create a temp file on cached server
         try {
            dir.createFile(fm.getName() + ".tmp");
         } catch (Exception e) {
            throw new UnexpectedException("File cache: can't create a temp file '" + fm.getName() + ".tmp'", e);
         }

         // copy an updated data to a temp file
         FileUtils.copyStream(fm.getInputStream(),
                 dir.getFileManager(fm.getName() + ".tmp").
                         getOutputStream(false));

         // rename a temp file to a destination file
         dir.rename(fm.getName() + ".tmp", fm.getName());
      } else {
         // uploading by directly overwriting a destination file
         FileManager fmDest = dir.getFileManager(fm.getName());
         try {
            FileUtils.copyStream(fm.getInputStream(), fmDest.getOutputStream(false));
         } catch (Exception e) {
            throw new UnexpectedException("File cache: can't overwrite an original file '" +
                    fm.getName() + "' by a cache file", e);
         }
      }

      OtherUtils.writeLogInfo(log,
              "FileCacheDirectoryManager: the cache file '" + fm.getPath() +
                      "' was uploaded.");
   }

   public boolean supportsRandomAccess() {
      return true;
   }

   public String getPath() {
      return dir.getPath();
   }

   public void close() {
      try {
         if (fileCache != null) {
//            flush();

            Enumeration<FileObjectInfo> fileInfoArray = fileCache.elements();

            while (fileInfoArray.hasMoreElements()) {
               FileObjectInfo fileObjectInfo = fileInfoArray.nextElement();
               fileObjectInfo.getFileManager().delete();
            }

            // clear cache
            fileCache.clear();
            dir.close();

            FileUtils.clearAllDir(new File(cacheFilesPath));

            fileCache = null;
            dir = null;
         }
      } catch (Exception ex) {
//      ex.printStackTrace();
      }
   }

   protected void finalize() throws Throwable {
      try {
         super.finalize();
         close();
      } catch (Exception ex) {
         log.warn(ex.getMessage(), ex);
      }
   }

   public FileManager[] listFiles(String extension, String templateName) {
      return dir.listFiles(extension, templateName);
   }

}
