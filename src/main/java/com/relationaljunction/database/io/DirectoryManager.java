package com.relationaljunction.database.io;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 * @author not attributable
 * @version 2.2
 */

import java.io.*;

/**
 * <p>Title: StelsCSV JDBC driver</p>
 * <p>Copyright: Copyright (c) J-Stels Software 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author J-Stels Software
 * @version 2.0
 */


abstract public class DirectoryManager {
   protected String tempPath = null;

   public static DirectoryManager buildDirectoryManager(String path) throws
           Exception {
      return buildDirectoryManager(path, null, null);
   }

   public static DirectoryManager buildDirectoryManager(String path,
                                                        String tempPath) throws
           Exception {
      return buildDirectoryManager(path, tempPath, null);
   }

   public static DirectoryManager buildDirectoryManager(String path,
                                                        String tempPath, String useWebParam) throws Exception {
      // validate argument(s)
      if (path == null || path.isEmpty()) {
         throw new IllegalArgumentException(
                 "'path' argument may not be empty or null");
      }

      DirectoryManager dir = null;

      // test for direct http url and server page
      if (path.startsWith("directhttp://"))
         dir = new HTTPDirectoryManager(path.substring("direct".length()),
                 useWebParam);

         // test for direct ftp url
      else if (path.startsWith("directftp://"))
         dir = new FTPDirectoryManager(path.substring("direct".length()));

         // test for cache http url and server page
      else if (path.startsWith("http://"))
         dir = new FileCacheDirectoryManager(new HTTPDirectoryManager(path,
                 useWebParam), tempPath);

         // test for cache ftp
      else if (path.startsWith("ftp://"))
         dir = new FileCacheDirectoryManager(new FTPDirectoryManager(path),
                 tempPath);

         // test for URL via Commons VFS library
      else if (path.startsWith("commons:"))
         dir = new FileCacheDirectoryManager(new CommonsVFSDirectoryManager(path.
                 substring("commons:".length())), tempPath);

         // test for URL via Commons VFS 2 library
      else if (path.startsWith("commons2:"))
         dir = new FileCacheDirectoryManager(new CommonsVFS2DirectoryManager(path.
                 substring("commons2:".length())), tempPath);

         // test for cache sftp
      else if (path.startsWith("sftp://"))
         dir = new FileCacheDirectoryManager(new CommonsVFS2DirectoryManager(path),
                 tempPath);

         // test for cache
      else if (path.startsWith("cache://"))
         dir = new FileCacheDirectoryManager(buildDirectoryManager(path.
                 substring("cache://".length()), tempPath), tempPath);

         // test for zip of jar
      else if (path.startsWith("zip://") || path.startsWith("jar://"))
         dir = new ZipDirectoryManager(path, tempPath);

         // test for resources in the classpath
      else if (path.startsWith("classpath://"))
         dir = new ClasspathDirectoryManager(path);

         // test for smb
      else if (path.startsWith("smb://"))
         dir = new SMBDirectoryManager(path);

         // test for directory with the protocol file://
      else if (path.startsWith("file://"))
         dir = new LocalFileDirectoryManager(path.substring("file://".length()));

         // test for file
      else if (new File(path).isFile())
         throw new IllegalArgumentException("Path must not be a file: " + path);

         //test for directory
      else if (new File(path).isDirectory())
         dir = new LocalFileDirectoryManager(path);

      else throw new IllegalArgumentException("Invalid path or path not found: " + path);

      dir.setTempPath(tempPath);
      return dir;
   }

   // get path to the directory.
   // File separator should be added to the end of the path.
   abstract public String getPath();

   public java.util.Date getFileModificationDate(String fileName) throws Exception {
      return getFileManager(fileName).getModificationTime();
   }

   abstract public FileManager getFileManager(String fileName) throws Exception;

   public boolean exists(String file) {
      try {
         return getFileManager(file).exists();
      } catch (Exception ex) {
         return false;
      }
   }

   abstract public void dropFile(String file) throws Exception;

   /*
      rename file with creating a new one and droping an old one
    */
   abstract public void rename(String oldFile, String newFile) throws Exception;

   abstract public void createFile(String file) throws Exception;

   public void upload(FileManager fm) throws Exception {
   }

   public FileManager[] listFiles(String extension, String templateName, boolean recursive) {
      return listFiles(extension, templateName);
   }

   // extension must contain dot. e.g.: '.txt'
   // templateName may contain the following wildcards: '%', '_'
   abstract public FileManager[] listFiles(String extension, String templateName);

   abstract public boolean isReadOnly();

   // returns whether the file supports appending to itself
   abstract public boolean supportsAppending();

   // returns whether the file supports random access
   abstract public boolean supportsRandomAccess();

   public void close() {
   }

   public void setTempPath(String tempPath) {
      this.tempPath = tempPath;
   }

   public String getTempPath() {
      return tempPath;
   }
}
