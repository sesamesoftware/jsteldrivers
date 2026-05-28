package com.relationaljunction.database.io;

import java.io.*;
import java.net.*;

import com.relationaljunction.database.io.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class HTTPDirectoryManager
        extends DirectoryManager {
   private String path = null;
   private String webParamName = null;


   /*
    arg path - HTTP URL
    e.g. http://www.somesite.com/csv/entry1/entry2
   */

   public HTTPDirectoryManager(String urlPath) throws Exception {
      this(urlPath, null);
   }

   public HTTPDirectoryManager(String urlPath, String webParamName) throws Exception {
      this.path = urlPath;
      this.webParamName = webParamName;

      if (!path.startsWith("http://"))
         this.path = "http://" + path;
      if (webParamName == null && !path.endsWith("/"))
         this.path += "/";
      else if (webParamName != null && path.endsWith("/"))
         this.path = urlPath.substring(0, urlPath.length() - 1);

      // check for URL existing
//    try {
//      checkURL(path);
//    }
//    catch (IOException ex) {
//      throw new Exception("URL is unknown or not found: " +
//                          ex.getMessage());
//    }
   }

   static InputStream checkURL(String path) throws IOException {
      URL url = new URL(path);
      URLConnection urlConn = url.openConnection();
       return urlConn.getInputStream();
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

   public boolean isReadOnly() {
      return true;
   }

   public boolean supportsAppending() {
      return false;
   }

   public FileManager getFileManager(String fileName) throws Exception {
      int pathType = FileManager.checkPath(fileName);

      // relative to directory file path
      if (pathType == FileManager.RELATIVE) {
         FileManager fm = null;

         if (webParamName != null)
            fm = new HTTPFileManager(this, fileName, webParamName);
         else
            fm = new HTTPFileManager(this, fileName);

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
      return path;
   }

   public FileManager[] listFiles(String extension, String templateName) {
      return new FileManager[0];
   }

   public static void main(String[] args) throws Throwable {
   }

}
