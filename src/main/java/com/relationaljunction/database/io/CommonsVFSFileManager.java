package com.relationaljunction.database.io;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.provider.sftp.SftpFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.utils.OtherUtils;

public class CommonsVFSFileManager
        extends FileManager {
	   private final Logger log = LoggerFactory.getLogger("CommonsVFSFileManager");

   private final String path;
   private final String name;
   private FileObject fileObject;
   private CommonsVFSDirectoryManager dir;
   private FileSystemManager fileSystemManager = null;
   private static final boolean CHECK_LOWER_AND_UPPER_FILE_CASES = true;

   public CommonsVFSFileManager(CommonsVFSDirectoryManager dir, String name) throws
           Exception {
      this.dir = dir;
      this.name = name;
      this.path = dir.getPath() + name;

      fileSystemManager = dir.fileSystemManager;
      fileObject = fileSystemManager.resolveFile(this.path,
              CommonsVFSDirectoryManager.
                      SFTP_FILE_SYSTEM_OPTIONS);
   }

   // absolute path to the file
   public CommonsVFSFileManager(String filePath) throws
           Exception {
      this.path = filePath;
      this.name = new File(filePath).getName();

      fileSystemManager = VFS.getManager();
      fileObject = fileSystemManager.resolveFile(this.path,
              CommonsVFSDirectoryManager.
                      SFTP_FILE_SYSTEM_OPTIONS);
   }

//  private void checkLowerAndUpperFileCases() {
//    if (!fileObject.exists()) {
//
//    }
//  }

   private void reconnectSFTP() {
      try {
         OtherUtils.writeTraceInfo(log, "CommonsVFSFileManager: SFTP " + path + " is reconnecting...");

         ((SftpFileSystem) fileObject.getFileSystem()).closeCommunicationLink();
         fileObject = fileSystemManager.resolveFile(this.path,
                 CommonsVFSDirectoryManager.
                         SFTP_FILE_SYSTEM_OPTIONS);
      } catch (Exception ex) {
         log.warn("CommonsVFSFileManager: SFTP error while reconnecting: " +
                 ex.getMessage(), ex);
      }
   }

   public java.util.Date getModificationTime() {
      try {
         return new java.util.Date(fileObject.getContent().getLastModifiedTime());
      } catch (Exception ex) {
         if (fileObject.getFileSystem() instanceof SftpFileSystem) {
            try {
               // reconnect for SFTP
               reconnectSFTP();
               return new java.util.Date(fileObject.getContent().getLastModifiedTime());
            } catch (Exception ex2) {
               log.warn("CommonsVFSFileManager: could not return a file modification date", ex);
               return null;
            }
         }

         return null;
      }
   }

   public RandomAccessFile getRandomAccess(String mode) throws Exception {
      throw new UnsupportedOperationException(
              "Method getRandomAccess() is not supported.");
   }

   public OutputStream getOutputStream(boolean append) throws Exception {
      try {
         return fileObject.getContent().getOutputStream(append);
      } catch (Exception ex) {
         // reconnect for SFTP
         if (fileObject.getFileSystem() instanceof SftpFileSystem) {
            reconnectSFTP();
            return fileObject.getContent().getOutputStream(append);
         } else
            throw new Exception(ex);
      }
   }

   public InputStream getInputStream() throws Exception {
      try {
         return fileObject.getContent().getInputStream();
      } catch (Exception ex) {
         if (fileObject.getFileSystem() instanceof SftpFileSystem) {
            // reconnect for SFTP
            reconnectSFTP();
            return fileObject.getContent().getInputStream();
         } else
            throw new Exception(ex);
      }
   }

   public void create() throws Exception {
      try {
         fileObject.createFile();
      } catch (Exception ex) {
         if (fileObject.getFileSystem() instanceof SftpFileSystem) {
            // reconnect for SFTP
            reconnectSFTP();
            fileObject.createFile();
         } else
            throw new Exception(ex);
      }
   }

   public void rename(CommonsVFSFileManager newFileManager) throws Exception {
      try {
         fileObject.moveTo(newFileManager.fileObject);
      } catch (Exception ex) {
         if (fileObject.getFileSystem() instanceof SftpFileSystem) {
            // reconnect for SFTP
            reconnectSFTP();
            fileObject.moveTo(newFileManager.fileObject);
         } else
            throw new Exception(ex);
      }
   }

   public void delete() throws Exception {
      try {
         fileObject.delete();
      } catch (Exception ex) {
         if (fileObject.getFileSystem() instanceof SftpFileSystem) {
            // reconnect for SFTP
            reconnectSFTP();
            fileObject.delete();
         } else
            throw new Exception(ex);
      }
   }

   public String getPath() {
      try {
         return fileObject.getURL().toString();
      } catch (FileSystemException ex) {
         return null;
      }
   }

   public String getDirPath() {
      return getDir().getPath();
   }

   public DirectoryManager getDir() {
      try {
         return new CommonsVFSDirectoryManager(fileObject.getParent().getURL().
                 toString(), tempPath);
      } catch (Exception ex) {
         return null;
      }
   }

   public String getName() {
      return name;
   }

   public boolean exists() {
      try {
         return fileObject.exists();
      } catch (FileSystemException ex) {
         return false;
      }
   }

   public boolean isReadOnly() {
      try {
         return !fileObject.isWriteable();
      } catch (FileSystemException ex) {
         return false;
      }
   }

   public boolean isDirectory() {
      try {
         return fileObject.getType().getName().equals("folder");
      } catch (FileSystemException ex) {
         return false;
      }
   }

   public void close() throws Exception {
      this.dir = null;

      if (fileObject != null) {
         fileObject.close();
         FileSystem fs = fileObject.getFileSystem();

         if (fileSystemManager != null)
            fileSystemManager.closeFileSystem(fs);
      }

      fileObject = null;
      fileSystemManager = null;
   }


   public static void main(String[] args) {
   }
}
