package com.relationaljunction.database.io;

import java.io.*;
import java.net.URL;

import com.relationaljunction.utils.UnexpectedException;

import jcifs.smb.*;

public class SMBFileManager extends FileManager {
   private SmbFile smbFile;
   private String filePath;
   private final String fileName;

   public SMBFileManager(String dirPath, String name) throws Exception {
      this.filePath = dirPath + name;
      this.fileName = name;
      this.smbFile = new SmbFile(this.filePath);
   }

   // absolute path to the file
   public SMBFileManager(String filePath) throws Exception {
      this.filePath = filePath;
      if (!filePath.startsWith("smb://"))
         this.filePath = "smb://" + filePath;

      this.smbFile = new SmbFile(this.filePath);
      this.fileName = smbFile.getName();
   }

   // absolute path to the file
//  public SMBFileManager(SmbFile smbFile) {
//    this.smbFile = smbFile;
//  }

   public void lock(int lockType) throws Exception {
      this.smbFile = new SmbFile(this.filePath,
              new NtlmPasswordAuthentication(new URL(filePath).getUserInfo()),
              SmbFile.FILE_SHARE_WRITE);
   }

   public java.util.Date getModificationTime() {
      return new java.util.Date(smbFile.getDate());
   }

   public RandomAccessFile getRandomAccess(String mode) throws Exception {
      throw new UnsupportedOperationException("getRandomAccess() is not supported for " +
              this.getClass());
   }

   // not tested
   public OutputStream getOutputStream(boolean append,  boolean sharedLock) throws Exception {
      SmbFileOutputStream smbOut;

      int time = 0;

      while (true) {
         try {
            smbOut = new SmbFileOutputStream(filePath, SmbFile.FILE_NO_SHARE);
            break;
         } catch (SmbException e) {
            Thread.sleep(FileManager.LOCK_CHECK_PERIOD);
//            System.out.println("waiting... time=" + time);
            time += FileManager.LOCK_CHECK_PERIOD;
            if (FileManager.LOCK_TIME_OUT != 0 && time > FileManager.LOCK_TIME_OUT)
               throw new UnexpectedException("[SMBFileManager] Can't access the file " +
                       filePath + ". But lock time is out!", e);
         }
      }

      return smbOut;
   }

   public OutputStream getOutputStream(boolean append) throws Exception {
//      return new SmbFileOutputStream(smbFile, append);
      SmbFileOutputStream smbOut;

      int time = 0;

      while (true) {
         try {
            smbOut = new SmbFileOutputStream(smbFile, append);
            break;
         } catch (SmbException e) {
            Thread.sleep(FileManager.LOCK_CHECK_PERIOD);
//            System.out.println("waiting... time=" + time);
            time += FileManager.LOCK_CHECK_PERIOD;
            if (FileManager.LOCK_TIME_OUT != 0 && time > FileManager.LOCK_TIME_OUT)
               throw new UnexpectedException("[SMBFileManager] Can't access the file " +
                       filePath + ". But lock time is out!", e);
         }
      }

      return smbOut;
   }

   public InputStream getInputStream() throws Exception {
      return new SmbFileInputStream(smbFile);
   }

//  public SmbFile getFile(){
//    return smbFile;
//  }

   public String getPath() {
      return filePath;
   }

   public String getDirPath() {
      return (smbFile.getParent() == null) ? "." : smbFile.getParent();
   }

   public DirectoryManager getDir() {
      try {
         return new SMBDirectoryManager(getDirPath());
      } catch (Exception ex) {
         throw new UnexpectedException(ex);
      }
   }

   public String getName() {
      return fileName;
   }

   public boolean exists() throws Exception {
      return smbFile.exists();
   }

   public boolean isReadOnly() {
      try {
         return !smbFile.canWrite();
      } catch (SmbException ex) {
         throw new UnexpectedException(ex);
      }
   }

   public boolean isDirectory() {
      try {
         return smbFile.isDirectory();
      } catch (SmbException ex) {
         throw new UnexpectedException(ex);
      }
   }

   public void delete() throws Exception {
      try {
         smbFile.delete();
      } catch (SmbException e) {
         throw new UnexpectedException("SMBFileManager.delete(). Can't delete an smb file " + filePath, e);
      }
   }

   @Override
   public void create() throws Exception {
      smbFile.createNewFile();
   }

   public void close() {
      smbFile = null;
   }
}
