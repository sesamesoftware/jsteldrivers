package com.relationaljunction.database.io;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.relationaljunction.utils.FileUtils;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 1.0
 */

public class ZipDirectoryManager extends DirectoryManager {
   String entryPath = null;
   String zipFilePath = null;
   ZipFile zf = null;
   ZipEntry zipEntry = null;
   String cacheFilePath = null;

   /*
   arg path - path to the zip file
   e.g. c:/temp/arch.zip/entry1/entry2
   zipFilePath = c:/temp/arch.zip
   entryPath = entry1/entry2/
   */
   ZipDirectoryManager(String urlPath) throws Exception {
      this(urlPath, null);
   }

   ZipDirectoryManager(String urlPath, String tempPath) throws Exception {
      this.tempPath = tempPath;

      String path = null;

      if (urlPath.startsWith("zip://") || urlPath.startsWith("jar://"))
         path = urlPath.substring("zip://".length());
      else
         path = urlPath;

      // check if a file path of another protocol
      if (path.startsWith("ftp://")) {
         cacheFilePath = copyOriginalFileToTempDir(path, tempPath);
         path = cacheFilePath;
      }

      int entryIndex = path.indexOf(".zip");
      if (entryIndex == -1) {
         entryIndex = path.indexOf(".jar");
         if (entryIndex == -1)
            throw new Exception(
                    "Path '" + urlPath +
                            "' doesn't contain a zip file with the extension '.zip' or '.jar'");
      }

      zipFilePath = path.substring(0, entryIndex + 4);
      entryPath = path.substring(entryIndex + 4).trim();

      if (!new File(zipFilePath).exists())
         throw new Exception(
                 "Zip file '" + zipFilePath + "' not found");

      zf = new ZipFile(zipFilePath);

      if (!entryPath.trim().isEmpty()) {
         // add separator to the end
         if (!entryPath.endsWith("/"))
            entryPath += "/";
         // remove separator from the begining
         if (entryPath.startsWith("/"))
            entryPath = entryPath.substring(1);
      }

      if (!entryPath.isEmpty()) {
         zipEntry = zf.getEntry(entryPath);
         if (zipEntry == null)
            throw new Exception("Zip entry '" + entryPath +
                    "' not found in the zip file '" + zipFilePath + "'");
         if (!zipEntry.isDirectory())
            throw new Exception("Zip entry '" + entryPath +
                    "' is a not directory in the zip file '" +
                    zipFilePath + "'");
      }
   }

   private String copyOriginalFileToTempDir(String filePath, String tempPath) throws Exception {
      FileManager originalFileManager = FileManager.buildFileManager(null, filePath);
      String tempFilePath = com.relationaljunction.utils.OtherUtils.getTempFilePath(tempPath, new File(filePath).getName());
      FileManager cacheFileManager = new LocalFileManager(tempFilePath);

      InputStream is = originalFileManager.getInputStream();
      OutputStream os = cacheFileManager.getOutputStream(false);
      if (is == null)
         throw new Exception("File '" + filePath + "' is not found");
      FileUtils.copyStream(is, os);

      originalFileManager.close();
      cacheFileManager.close();
      return tempFilePath;
   }

   public FileManager getFileManager(String fileName) throws Exception {
      int pathType = FileManager.checkPath(fileName);

      // relative to directory file path
      if (pathType == FileManager.RELATIVE) {
         FileManager fm = new ZipFileManager(this, zf, entryPath, fileName);
         fm.setTempPath(tempPath);
         return fm;
      }
      // absolute file path
      else
         return FileManager.buildFileManager(this, fileName, tempPath, pathType);
   }

   @Override
   public void createFile(String file) throws Exception {
      throw new UnsupportedOperationException("Write operations are not supported!");
   }

   @Override
   public void dropFile(String file) throws Exception {
      throw new UnsupportedOperationException("Write operations are not supported!");
   }

   @Override
   public void rename(String oldFile, String newFile) {
      throw new UnsupportedOperationException("Write operations are not supported!");
   }

   public String getPath() {
      return "zip://" + zf.getName() + "/" + entryPath;
   }

   public boolean isReadOnly() {
      return true;
   }

   public boolean supportsAppending() {
      return false;
   }

   public boolean supportsRandomAccess() {
      return false;
   }

   public FileManager[] listFiles(String extension, String templateName) {
      Vector files = new Vector();

      Enumeration enumer = zf.entries();
      while (enumer.hasMoreElements()) {
         ZipEntry curEntry = (ZipEntry) enumer.nextElement();
         if (!curEntry.isDirectory()) {
            // use File class for parsing paths
            File fCurEntry = new File(curEntry.getName());
            File fEntryPath = entryPath.isEmpty() ? null : new File(entryPath);
            File fDirEntry = fCurEntry.getParentFile();

            if ((fEntryPath == null && fDirEntry == null) ||
                    (fEntryPath != null && fEntryPath.equals(fDirEntry))) {
               // compare dir entries
               String fileXtension = "." +
                       com.relationaljunction.utils.StringUtils.getFileExtension(fCurEntry.getName());
               // compare extensions and check for conformity with the template
               if ((extension == null || fileXtension.equalsIgnoreCase(extension)) &&
                       (templateName == null || templateName.trim().isEmpty() ||
                               com.relationaljunction.utils.StringUtils.isLike(com.relationaljunction.utils.StringUtils.
                                       getFileNameWithoutExtension(fCurEntry.getName().toLowerCase()),
                                       templateName.toLowerCase(), '%', '_')))
                  files.add(curEntry);
            }
         }
      }
      FileManager[] fms = new FileManager[files.size()];
      for (int i = 0; i < files.size(); i++)
         fms[i] = new ZipFileManager(this, zf, (ZipEntry) files.get(i));
      return fms;
   }

   public void close() {
      entryPath = null;
      zipFilePath = null;
      zipEntry = null;

      try {
         if (zf != null) {
            zf.close();
         }
      } catch (IOException ex) {
      }
      zf = null;

      if (cacheFilePath != null)
         new File(cacheFilePath).delete();

      cacheFilePath = null;
   }

   public static void main(String[] args) throws Throwable {
      ZipDirectoryManager zipDir = new ZipDirectoryManager("arch.zip");
      System.out.println("zipFilePath=" + zipDir.zipFilePath);
      System.out.println("entryPath=" + zipDir.entryPath);

      zipDir = new ZipDirectoryManager("arch.zip/");
      System.out.println("zipFilePath=" + zipDir.zipFilePath);
      System.out.println("entryPath=" + zipDir.entryPath);

      zipDir = new ZipDirectoryManager("arch.zip/a");
      System.out.println("zipFilePath=" + zipDir.zipFilePath);
      System.out.println("entryPath=" + zipDir.entryPath);

      zipDir = new ZipDirectoryManager("arch.zip/a/");
      System.out.println("zipFilePath=" + zipDir.zipFilePath);
      System.out.println("entryPath=" + zipDir.entryPath);

      try {
         zipDir = new ZipDirectoryManager("c:/temp/arch");
      } catch (Exception ex) {
         System.out.println("Error: " + ex.getMessage());
      }
      try {
         zipDir = new ZipDirectoryManager("c:/temp/arch.zip");
      } catch (Exception ex) {
         System.out.println("Error: " + ex.getMessage());
      }
      try {
         zipDir = new ZipDirectoryManager("arch.zip/none");
      } catch (Exception ex) {
         System.out.println("Error: " + ex.getMessage());
      }

      zipDir = new ZipDirectoryManager("arch.zip");
      FileManager[] fms = zipDir.listFiles(null, null);
      System.out.println("Files in the dir " + zipDir.getPath());
      for (int i = 0; i < fms.length; i++)
         System.out.println(fms[i].getName());

      zipDir = new ZipDirectoryManager("arch.zip/a");
      fms = zipDir.listFiles(".txt", null);
      System.out.println("Files in the dir " + zipDir.getPath());
      for (int i = 0; i < fms.length; i++)
         System.out.println(fms[i].getName());

      zipDir = new ZipDirectoryManager("arch.zip/a/b");
      fms = zipDir.listFiles(null, "crea%");
      System.out.println("Files in the dir " + zipDir.getPath());
      for (int i = 0; i < fms.length; i++)
         System.out.println(fms[i].getName());
   }

}
