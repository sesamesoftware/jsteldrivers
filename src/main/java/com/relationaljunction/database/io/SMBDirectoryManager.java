package com.relationaljunction.database.io;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.utils.UnexpectedException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class SMBDirectoryManager extends DirectoryManager {
	   private final Logger log = LoggerFactory.getLogger("CommonsVFS2DirectoryManager");

   private String dirPath;
   private final SmbFile smbContext;

   public SMBDirectoryManager(String path) throws Exception {
      // ######## check if filepath is a correct path. #############
      if (path.trim().isEmpty())
         throw new IllegalArgumentException("Empty path is incorrect");

      if (!path.startsWith("smb://"))
         this.dirPath = "smb://" + path;
      else
         this.dirPath = path;

      smbContext = new SmbFile(dirPath);

      if (!smbContext.exists()) {
         throw new IllegalArgumentException("Specified path '" + path +
                 "' is not found.");
      }
      if (!smbContext.isDirectory()) {
         throw new IllegalArgumentException("Specified path '" + path +
                 "' is not a directory.");
      }

      if (!this.dirPath.trim().isEmpty() && !this.dirPath.endsWith("/") &&
              !this.dirPath.endsWith("\\"))
         this.dirPath = this.dirPath + "/";
   }

   public FileManager getFileManager(String fileName) throws Exception {
      int pathType = FileManager.checkPath(fileName);

      // relative to directory file path
      if (pathType == FileManager.RELATIVE) {
         FileManager fm = new SMBFileManager(getPath(), fileName);
         fm.setTempPath(tempPath);
         return fm;
      }
      // absolute file path
      else
         return FileManager.buildFileManager(this, fileName, tempPath, pathType);
   }

   public void createFile(String fileName) throws Exception {
      try {
         new SmbFile(getPath() + fileName).createNewFile();
      } catch (SmbException e) {
         throw new UnexpectedException("SMBDirectoryManager.dropFile(): can't create an smb file " +
                 getPath() + fileName, e);
      }
   }

   public void dropFile(String fileName) throws Exception {
      try {
         new SmbFile(getPath() + fileName).delete();
      } catch (Exception e) {
         throw new UnexpectedException("SMBDirectoryManager.dropFile(): can't drop an smb file " +
                 getPath() + fileName, e);
      }
   }

   public void rename(String oldFile, String newFile) throws Exception {
      try {
         if (exists(newFile)) {
            dropFile(newFile);
         }
      } catch (Exception e) {
         throw new UnexpectedException("SMBDirectoryManager.rename(): can't drop an smb file " +
                 getPath() + newFile + " to rename", e);
      }

      try {
         new SmbFile(getPath() + oldFile).renameTo(new SmbFile(getPath() + newFile));
      } catch (Exception e) {
         throw new UnexpectedException("SMBDirectoryManager.rename(): can't rename an smb file " +
                 getPath() + oldFile + " to " + getPath() + newFile, e);
      }
   }

   public FileManager[] listFiles(String extension, String templateName) {
      List<FileManager> filteredFiles = new LinkedList<FileManager>();

      try {
         SmbFile[] smbFiles = new SmbFile(dirPath).listFiles();
         com.relationaljunction.utils.FileExtensionFilter fileFilter = new com.relationaljunction.utils.
                 FileExtensionFilter(extension,
                 templateName);

         for (SmbFile smbFile : smbFiles) {
            if (!smbFile.isFile())
               continue;

            String fileName = smbFile.getName();

            if (fileFilter.accept(fileName))
               filteredFiles.add(new SMBFileManager(getPath(), smbFile.getName()));
         }
      } catch (Exception e) {
         throw new UnexpectedException("Error in SMBDirectoryManager.listFiles() while getting a list of files for the URL '" +
                 dirPath + "'", e);
      }

      return filteredFiles.toArray(new FileManager[0]);
   }

   public String getPath() {
      return dirPath;
   }

   public boolean isReadOnly() {
      return false;
   }

   public boolean supportsAppending() {
      return true;
   }

   public boolean supportsRandomAccess() {
      return false;
   }

   SmbFile getSMBContext() {
      return smbContext;
   }

   public static void main(String[] args) {
      try {
         SMBDirectoryManager smbDir = new SMBDirectoryManager("smb://serg10/temp");
         SMBFileManager smbFm = (SMBFileManager) smbDir.getFileManager("test2.dbf");

         InputStream is = smbFm.getInputStream();

         byte[] b = new byte[8192];
         int n;
         while ((n = is.read(b)) > 0) {
            System.out.write(b, 0, n);
         }

      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }
}
