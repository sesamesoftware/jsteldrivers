package com.relationaljunction.database.io;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class HTTPFileManager
        extends FileManager {
   private String filePath = null;
   private String fileName = null;
   private String dirPath = null;
   private URL url = null;
   private String userInfo = null;

   // example - http://test:test@www.csv-jdbc.com/test2/atom2.xml
   public HTTPFileManager(String path) throws Exception {
      this.filePath = path;
      if (!path.startsWith("http://"))
         this.filePath = "http://" + path;

      this.fileName = new File(path).getName();
      this.url = new URL(filePath);
      this.dirPath = filePath.substring(0, filePath.indexOf(fileName));
      this.dir = new HTTPDirectoryManager(dirPath);
      this.userInfo = url.getUserInfo();

//    try {
//      urlConn.getInputStream();
//    }
//    catch (IOException ex) {
//      throw new Exception("URL is unknown or not found: " +
//                          ex.getMessage());
//    }
   }

   public HTTPFileManager(HTTPDirectoryManager dir, String fileName) throws Exception {
      this.dir = dir;
      this.filePath = dir.getPath() + fileName;
      this.fileName = fileName;
      this.dirPath = dir.getPath();

      this.url = new URL(filePath);
      this.userInfo = url.getUserInfo();
   }

   public HTTPFileManager(HTTPDirectoryManager dir, String fileName, String webParamName) throws
           Exception {
      this.dir = dir;
      this.fileName = fileName;

      // form URL for web-server page
      dirPath = dir.getPath();
      if (dirPath.indexOf("?") == -1) {
         this.filePath = dirPath + "?" + webParamName + "=" + fileName;
         this.dirPath = dirPath + "?" + webParamName + "=";
      } else if (!dirPath.endsWith("&")) {
         this.filePath = dirPath + "&" + webParamName + "=" + fileName;
         this.dirPath = dirPath + "&" + webParamName + "=";
      } else {
         this.filePath = dirPath + webParamName + "=" + fileName;
         this.dirPath = dirPath + webParamName + "=";
      }

      this.url = new URL(filePath);
      this.userInfo = url.getUserInfo();
   }

//  public HTTPFileManager(String dirPath, String fileName) throws Exception{
//    this.filePath = dirPath + fileName;
//    this.fileName = fileName;
//    url = new URL(filePath);
//    userInfo = url.getUserInfo();
//  }

//  public HTTPFileManager(String dirPath, String fileName, String webParamName) throws
//      Exception {
//    this.fileName = fileName;
//
//    // form URL for web-server page
//    if (dirPath.indexOf("?") == -1)
//      this.filePath = dirPath + "?" + webParamName + "=" + fileName;
//    else if (!dirPath.endsWith("?") && !dirPath.endsWith("&"))
//      this.filePath = dirPath + "&" + webParamName + "=" + fileName;
//    else
//      this.filePath = dirPath + webParamName + "=" + fileName;
//
//    url = new URL(filePath);
//    userInfo = url.getUserInfo();
//  }

   public RandomAccessFile getRandomAccess(String mode) throws Exception {
      /**@todo Implement this com.relationaljunction.jdbc.csv.store.FileManager abstract method*/
      throw new UnsupportedOperationException(
              "Method getRandomAccess() not yet implemented.");
   }

   public boolean exists() {
      try {
         InputStream is = getInputStream();
         is.close();
      } catch (Exception ex) {
         return false;
      }
      return true;
   }

   public String getName() {
      return fileName;
   }

   public boolean isReadOnly() {
      return true;
   }

   public String getPath() {
      return filePath;
   }

   public String getDirPath() {
      return dir.getPath();
   }

   public DirectoryManager getDir() {
      return dir;
   }

   public OutputStream getOutputStream(boolean append) throws
           Exception {
      throw new UnsupportedOperationException("Write operations are not supported!");
   }

//  public InputStream getInputStream() throws Exception {
//    return getInputStream(this.filePath);
//  }

   public InputStream getInputStream() throws Exception {
      InputStream is = null;

      try {
         is = getInputStream(this.filePath);
      } catch (Exception ex) {
         try {
            String lowerCasePath = dirPath + fileName.toLowerCase();
            is = getInputStream(lowerCasePath);
            this.filePath = lowerCasePath;
         } catch (Exception ex2) {
            try {
               String upperCasePath = dirPath + fileName.toUpperCase();
               is = getInputStream(upperCasePath);
               this.filePath = upperCasePath;
            } catch (Exception ex3) {
               throw new Exception("Can't open a stream for the file '" +
                       this.filePath + "'. Error was: " +
                       ex.getMessage());
            }
         }
      }

      return is;
   }

   private InputStream getInputStream(String _filePath) throws Exception {
      URL _url = new URL(_filePath);
      URLConnection urlConn = _url.openConnection();

      if (userInfo != null) {
         String userInfoEncoding = Base64.getEncoder()
                 .encodeToString(userInfo.getBytes(StandardCharsets.UTF_8));

         urlConn.setRequestProperty(
                 "Authorization",
                 "Basic " + userInfoEncoding
         );
      }

      try {
         return urlConn.getInputStream();
      } catch (Exception ex) {
         throw new Exception("Can't open a stream for the file '" +
                 url.toExternalForm() + "'. Error was: " +
                 ex.getMessage());
      }
   }

   public void delete() throws Exception {
      throw new UnsupportedOperationException("Write operations are not supported!");
   }

   @Override
   public void create() throws Exception {
      throw new UnsupportedOperationException("Write operations are not supported!");
   }
}
