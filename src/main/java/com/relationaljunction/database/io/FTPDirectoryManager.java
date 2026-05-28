package com.relationaljunction.database.io;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.UnexpectedException;

import cz.dhl.ftp.Ftp;
import cz.dhl.ftp.FtpConnect;
import cz.dhl.ftp.FtpFile;
import cz.dhl.io.CoFile;
import cz.dhl.ui.CoConsole;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2006</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.5
 */

public class FTPDirectoryManager
        extends DirectoryManager {
   private final Logger log = LoggerFactory.getLogger("FTPDirectoryManager");

   private final URL urlPath;
   private FtpConnect ftpConn;
   private String internalFtpPath;
   Ftp ftp;

   public FTPDirectoryManager(String url) throws Exception {
      // ftp syntax: ftp://user:password@hostname[:port]/[dirpath/]
      String urlParam = url;
      if (urlParam.endsWith("\\"))
         urlParam = urlParam.substring(0, url.length() - 1);

      urlPath = new URL(urlParam);

      internalFtpPath = urlPath.getPath();
      if (!internalFtpPath.endsWith("/") && !internalFtpPath.endsWith("\\"))
         internalFtpPath = internalFtpPath + "/";

      // if the port is not specified in the URL the variable will be equal -1
      String port = urlPath.getPort() == -1 ? "" :
              ":" + urlPath.getPort();
      String host = urlPath.getProtocol() + "://" + urlPath.getHost() + port;

      // default user and password
      String user = "anonymous";
      String passw = "";

      if (urlPath.getUserInfo() != null &&
              urlPath.getUserInfo().contains(":")) {
         int pos = urlPath.getUserInfo().indexOf(":");
         user = urlPath.getUserInfo().substring(0, pos);
         passw = urlPath.getUserInfo().substring(pos + 1);
      }

      connect(host, user, passw, internalFtpPath);
   }

   private void connect(String host, String user, String passw, String internalPath) throws Exception {
      ftpConn = FtpConnect.newConnect(host);
      ftpConn.setPassWord(passw);
      ftpConn.setUserName(user);
      /* connect & login to host */
      connect(internalPath);
   }

   private void connect(String internalPath) throws Exception {
      ftp = new Ftp();

      if (log.isTraceEnabled())
         ftp.getContext().setConsole(new CoConsole() {
            public void print(String message) {
               OtherUtils.writeTraceInfo(log, "FTP Server: " + message);
            }
         });
      else
         ftp.getContext().setConsole(null);

      if (!ftp.connect(ftpConn))
         throw new Exception("Can't connect to the FTP server: '" +
                 ftpConn.getHostName() + "'");

      boolean result = ftp.cd(internalPath);
      if (!result)
         throw new Exception("Can't change the current directory on the FTP server to '" +
                 internalPath + "'. It may not exist!");
   }

   void reconnect() throws Exception {
      OtherUtils.writeTraceInfo(log, "FTPDirectoryManager: reconnecting...");

      if (ftp != null)
         disconnect();
      connect(internalFtpPath);
   }

   public boolean isReadOnly() {
      return false;
   }

   public void dropFile(String file) throws Exception {
      try {
         ftp.rm(internalFtpPath + file);
      } catch (Exception ex) {
         // error. try to reconnect
         reconnect();
         ftp.rm(internalFtpPath + file);
      }
   }

   public void rename(String oldFile, String newFile) throws Exception {
      try {
         dropFile(newFile);
      } catch (Exception ex) {
         throw new UnexpectedException("FTPDirectoryManager.rename(): can't drop a file " +
                 getPath() + newFile + " to rename", ex);
      }

      boolean result = ftp.mv(internalFtpPath + oldFile, internalFtpPath + newFile);
      if (!result)
         throw new Exception("Can't rename the file '" + internalFtpPath + oldFile +
                 "' to the '" + internalFtpPath + newFile + "'");
   }

   public boolean supportsAppending() {
      return true;
   }

   public FileManager getFileManager(String fileName) throws Exception {
      int pathType = FileManager.checkPath(fileName);

      // relative to directory file path
      if (pathType == FileManager.RELATIVE) {
         FileManager fm = new FTPFileManager(this, fileName);
         fm.setTempPath(tempPath);
         return fm;
      }
      // absolute file path
      else
         return FileManager.buildFileManager(this, fileName, tempPath, pathType);
   }

   public boolean supportsRandomAccess() {
      return false;
   }

   public String getPath() {
      String path = urlPath.toString();
      if (!path.endsWith("/") && !path.endsWith("\\"))
         path = path + "/";
      return path;
   }

   public FileManager[] listFiles(String extension, String templateName) {
      ArrayList<FileManager> filteredFiles = new ArrayList<FileManager>();

      try {
         CoFile curDir;

         try {
            curDir = new FtpFile(ftp.pwd(), ftp);
         } catch (IOException ex) {
            // error. try to reconnect
            reconnect();
            curDir = new FtpFile(ftp.pwd(), ftp);
         }

         com.relationaljunction.utils.FileExtensionFilter fileFilter = new com.relationaljunction.utils.
                 FileExtensionFilter(extension, templateName);

         CoFile[] files = curDir.listCoFiles();

         for (CoFile file : files) {
            if (!file.isFile())
               continue;

            if (fileFilter.accept(new File(file.getName()))) {
               filteredFiles.add(new FTPFileManager(this, file.getName()));
            }
         }
      } catch (Exception ex) {
         throw new UnexpectedException(
                 "Unexpected exception in FTPDirectoryManager.listFiles()", ex);
      }

      return filteredFiles.toArray(new FileManager[0]);
   }

   public void createFile(String file) throws Exception {
      FileManager fm = getFileManager(file);
      fm.getOutputStream(false).close();
   }

   protected void finalize() throws Throwable {
      super.finalize();
      
      try {
         disconnect();
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }

   void disconnect() {
      if (ftp != null)
         ftp.disconnect();
   }

   public void close() {
      try {
         disconnect();
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }

   public static void main(String[] args) {
      try {
         FTPDirectoryManager ftpDir = new FTPDirectoryManager(
                 "ftp://aaa:bbb@csv-jdbc.com:21/public_html/test");
//      System.out.println(ftpDir.getPath());
//      System.out.println(ftpFile.getPath());
//      System.out.println(ftpFile.getDirPath());
         ftpDir.listFiles(".txt", null);
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }

}
