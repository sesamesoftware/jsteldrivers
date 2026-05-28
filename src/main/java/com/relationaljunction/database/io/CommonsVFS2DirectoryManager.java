package com.relationaljunction.database.io;

import java.io.InputStream;
import java.util.Vector;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystem;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.utils.OtherUtils;

public class CommonsVFS2DirectoryManager extends DirectoryManager {

   private final Logger log = LoggerFactory.getLogger("CommonsVFS2DirectoryManager");

   FileSystemManager fileSystemManager = null;

   static FileSystemOptions SFTP_FILE_SYSTEM_OPTIONS = new
           FileSystemOptions();

   static {
      try {
         SftpFileSystemConfigBuilder config = SftpFileSystemConfigBuilder.
                 getInstance();
         config.setStrictHostKeyChecking(SFTP_FILE_SYSTEM_OPTIONS, "no");
         config.setTimeout(SFTP_FILE_SYSTEM_OPTIONS, Integer.valueOf(1000));
      } catch (FileSystemException ex) {
      }
   }

   private String path;
   private FileObject fileObject;


   public CommonsVFS2DirectoryManager(String path) throws Exception {
      this(path, null);
   }

   public CommonsVFS2DirectoryManager(String path, String tempPath) throws
           Exception {
      this.tempPath = tempPath;

      // ######## check if filepath is a correct path. #############
      if (path.trim().isEmpty())
         throw new IllegalArgumentException("Empty path is incorrect");

      this.path = path;

      if (!this.path.endsWith("\\") || this.path.endsWith("/"))
         this.path += "/";

      if (log.isDebugEnabled())
         log.debug("connecting to " + path);

      fileSystemManager = VFS.getManager();
      fileObject = fileSystemManager.resolveFile(this.path, SFTP_FILE_SYSTEM_OPTIONS);

      if (!fileObject.exists()) {
         log.error("Specified directory '" + path + "' is not found.");
         throw new IllegalArgumentException("Specified directory '" + path +
                 "' is not found.");
      }

      if (fileObject.getType() == FileType.FILE) {
         log.error("Specified path '" + path + "' is not directory.");
         throw new IllegalArgumentException("Specified path '" + path +
                 "' is not directory.");
      }

      if (log.isDebugEnabled())
         log.debug("Succesfully connected to " + path);
   }


   public FileManager getFileManager(String fileName) throws Exception {
//      if (log.isTraceEnabled())
//         log.trace("calling CommonsVFS2DirectoryManager.getFileManager(" + fileName + ")");

      int pathType = FileManager.checkPath(fileName);

      // relative to directory file path
      if (pathType == FileManager.RELATIVE) {
         FileManager fm = new CommonsVFS2FileManager(this, fileName);
         fm.setTempPath(tempPath);
         return fm;
      }
      // absolute file path
      else
         return FileManager.buildFileManager(this, fileName, tempPath, pathType);
   }

   public void createFile(String fileName) throws Exception {
      CommonsVFS2FileManager fm = new CommonsVFS2FileManager(this, fileName);
      fm.create();
//    fm.close();
   }

   public void dropFile(String fileName) throws Exception {
      CommonsVFS2FileManager fm = new CommonsVFS2FileManager(this, fileName);
      fm.delete();
//    fm.close();
   }

   public void rename(String oldFile, String newFile) throws Exception {
      CommonsVFS2FileManager fmOld = new CommonsVFS2FileManager(this, oldFile);
      CommonsVFS2FileManager fmNew = new CommonsVFS2FileManager(this, newFile);

      try {
         fmNew.delete();
      } catch (Exception ex) {
      }

      fmOld.rename(fmNew);
   }

   public FileManager[] listFiles(String extension, String templateName) {
      Vector<FileManager> filteredFiles = new Vector<FileManager>();

      if (log.isDebugEnabled())
         log.debug("calling CommonsVFS2DirectoryManager.listFiles(" + extension +
                 ", " + templateName + ")");

      try {
         com.relationaljunction.utils.FileExtensionFilter fileFilter = new com.relationaljunction.utils.
                 FileExtensionFilter(extension, templateName);

         FileObject[] children = null;
         try {
            children = fileObject.getChildren();

            if (log.isDebugEnabled())
               log.debug("getting list of files in the directory " + path +
                       ". Found " + children.length + " elements");
         } catch (Exception ex) {
            if (fileObject.getFileSystem() instanceof SftpFileSystem) {
               if (log.isDebugEnabled())
                  log.warn("error while getting list of files in the directory " +
                          path);

               // reconnect for SFTP
               reconnectSFTP();
               children = fileObject.getChildren();
            } else
               throw new Exception(ex);
         }

         for (int i = 0; i < children.length; i++) {
            if (children[i].getType() != FileType.FILE)
               continue;

            String fileName = children[i].getName().getBaseName();

            if (fileFilter.accept(fileName))
               filteredFiles.add(new CommonsVFS2FileManager(this, fileName));
         }
      } catch (Exception ex) {
         throw new com.relationaljunction.utils.UnexpectedException(
                 "Unexpected exception in CommonsVFS2DirectoryManager.listFiles()", ex);
      }

      log.debug("returning list of files in the directory " + path +
              ". There are " + filteredFiles.size() + " files");

      return filteredFiles.toArray(new FileManager[0]);
   }

   private void reconnectSFTP() {
      try {
         OtherUtils.writeTraceInfo(log, "CommonsVFSFileManager: SFTP " + path + " is reconnecting...");

         ((SftpFileSystem) fileObject.getFileSystem()).closeCommunicationLink();
         fileObject = fileSystemManager.resolveFile(this.path, SFTP_FILE_SYSTEM_OPTIONS);
      } catch (Exception ex) {
         log.warn("CommonsVFSFileManager: SFTP error while reconnecting: " +
                 ex.getMessage(), ex);
      }
   }

   public String getPath() {
      return path;
   }

   public boolean isReadOnly() {
      return false;
   }

   public boolean supportsAppending() {
      return false;
   }

   public boolean supportsRandomAccess() {
      return false;
   }

   protected void finalize() throws Throwable {
      try {
         super.finalize();
         close();
      } catch (Exception ex) {
      }
   }

   public void close() {
      try {
//      if (fileSystem!=null) fileSystem.close();

         if (fileObject != null) {
            fileObject.close();
            FileSystem fs = fileObject.getFileSystem();

            if (fileSystemManager != null)
               fileSystemManager.closeFileSystem(fs);

            if (log.isDebugEnabled())
               log.debug("closing connection to " + path);
         }

         fileObject = null;
         fileSystemManager = null;


//      not works for repeated connection. Throws "Unknown scheme "sftp" in URI"
//      ((DefaultFileSystemManager)fileSystemManager).close();
      } catch (Exception ex) {
//      ex.printStackTrace();
      }
   }

   public static void main(String[] args) {
      try {
         Vector<String> v = new Vector<String>();
         String[] array = v.toArray(new String[0]);


         CommonsVFS2DirectoryManager commonsvfsdirectorymanager = new
                 CommonsVFS2DirectoryManager(
                 "sftp://serg:derfli@127.0.0.1:22/pmode.bak");

         FileManager fm = commonsvfsdirectorymanager.getFileManager(
                 "test/prices.txt");

         InputStream is = fm.getInputStream();

         int b;
         while ((b = is.read()) != -1) {
            char c = (char) b;
            System.out.print("" + (char) b); //This prints out content that is unreadable.
            //Isn't it supposed to print out html tag?
         }

//      fm.close();
         commonsvfsdirectorymanager.close();
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }
}

