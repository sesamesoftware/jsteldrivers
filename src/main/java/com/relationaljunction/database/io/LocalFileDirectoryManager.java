package com.relationaljunction.database.io;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

import com.relationaljunction.utils.CallbackHandler;
import com.relationaljunction.utils.FileUtils;
import com.relationaljunction.utils.UnexpectedException;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 1.0
 */

public class LocalFileDirectoryManager
        extends DirectoryManager {
   private String path;

   public LocalFileDirectoryManager(String path) {
      this(path, null);
   }

   public LocalFileDirectoryManager(String path, String tempPath) {
      this.tempPath = tempPath;

      // ######## check if filepath is a correct path. #############
      if (path.trim().isEmpty())
         throw new IllegalArgumentException("Empty path is incorrect");

      File checkPath = new File(path);
      if (!checkPath.exists()) {
         throw new IllegalArgumentException("Specified path '" + path +
                 "' is not found.");
      }
      if (!checkPath.isDirectory()) {
         throw new IllegalArgumentException("Specified path '" + path +
                 "' is not a directory.");
      }

      this.path = new File(path).getPath();

      if (!this.path.endsWith("\\") || this.path.endsWith("/"))
         this.path += File.separator;
   }


   public FileManager getFileManager(String fileName) throws Exception {
      int pathType = FileManager.checkPath(fileName);

      // relative to directory file path
      if (pathType == FileManager.RELATIVE) {
         FileManager fm = new LocalFileManager(this, fileName);
         fm.setTempPath(tempPath);
         return fm;
      }
      // absolute file path
      else
         return FileManager.buildFileManager(this, fileName, tempPath, pathType);
   }

   public void createFile(String fileName) throws Exception {
      boolean result = new File(getPath() + fileName).createNewFile();

      if (!result) {
         throw new UnexpectedException("[LocalFileDirectoryManager] Can't create the file. " +
                 "Method File.createNewFile() returned 'false'. File '" +
                 getPath() + fileName + "' may already exist.");
      }
   }

   public void dropFile(String fileName) throws Exception {
      File fileToDelete = new File(getPath() + fileName);
      if (!fileToDelete.exists()) return;

      boolean result = fileToDelete.delete();

      if (!result) {
         throw new UnexpectedException("[LocalFileDirectoryManager] Can't delete the file '" +
                 getPath() + fileName + "'. Maybe it is already used by another stream or process.");
      }
   }

   @Override
   public void rename(String oldFile, String newFile) throws Exception {
      dropFile(newFile);

      boolean result = new File((getPath() + oldFile)).renameTo(new File((getPath() + newFile)));

      if (!result) throw new UnexpectedException("[LocalFileDirectoryManager] Can't rename the file '" +
              getPath() + oldFile + "' to '" + getPath() + oldFile +
              "'. Maybe it is already used by another stream or process.");
   }

/*
   public FileManager[] listFiles(String extension, String templateName) {
      File[] files = new File(path).listFiles(new com.relationaljunction.utils.FileExtensionFilter(
              extension, templateName));

      FileManager[] localFiles = new FileManager[files.length];
      for (int i = 0; i < files.length; i++)
         localFiles[i] = new LocalFileManager(this, files[i].getName());
      return localFiles;
   }
*/

   public FileManager[] listFiles(final String extension, String templateName, boolean recursive) {
      final List<FileManager> fileManagers = new LinkedList<FileManager>();

      final com.relationaljunction.utils.FileExtensionFilter fileFilter = new com.relationaljunction.utils.
              FileExtensionFilter(extension, templateName);

      final File dirPath = new File(path);

      FileUtils.listFilesForFolder(dirPath, recursive, new CallbackHandler<File>() {
         public void process(File file) {
            if (file.getParentFile().equals(dirPath) && fileFilter.accept(file.getName())) {
               // file from the same directory
               fileManagers.add(new LocalFileManager(LocalFileDirectoryManager.this, file.getName()));
            } else {
               // file from a subdirectory
               String relativeFilePath = dirPath.toURI().relativize(file.toURI()).getPath().replace('/', '\\');

               if (fileFilter.accept(relativeFilePath)) {
                  fileManagers.add(new LocalFileManager(LocalFileDirectoryManager.this, relativeFilePath));
               }
            }
         }
      }

      );

      return fileManagers.toArray(new FileManager[0]);
   }

   public FileManager[] listFiles(final String extension, String templateName) {
      return listFiles(extension, templateName, false);
   }

   public String getPath() {
      return path;
   }

   public boolean isReadOnly() {
      return false;
   }

   public boolean supportsAppending() {
      return true;
   }

   public boolean supportsRandomAccess() {
      return true;
   }

   public static void main(String[] args) {

   }
}
