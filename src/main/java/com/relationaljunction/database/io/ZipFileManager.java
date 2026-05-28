package com.relationaljunction.database.io;

import java.io.*;
import java.util.zip.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 * @author not attributable
 * @version 2.2
 */

public class ZipFileManager
    extends FileManager {
  ZipFile zf = null;
  ZipEntry zipEntry = null;

  // urlPath - absolute url path to the zip file
  // e.g. zip://c:/temp/csvarchive.zip/schema.xml
  ZipFileManager(String urlPath) throws Exception{
    String path = null;
    if (urlPath.startsWith("zip://") || urlPath.startsWith("jar://"))
      path = urlPath.substring("zip://".length());
    else
      path = urlPath;

    int entryIndex = path.indexOf(".zip");
    if (entryIndex == -1){
      entryIndex = path.indexOf(".jar");
      if (entryIndex == -1)
        throw new IllegalArgumentException (
            "Path '" + urlPath +
            "' doesn't contain a zip file with the extension '.zip' or '.jar'");
    }

    String zipFilePath = path.substring(0, entryIndex + 4);

    // file path within zip (entry path)
    String entryPath = path.substring(entryIndex + 4).trim();

    if (!new File(zipFilePath).exists())
      throw new IllegalArgumentException (
          "Zip file '" + zipFilePath + "' not found");

    if (entryPath.isEmpty())
      throw new IllegalArgumentException (
          "Path in zip file '" + zipFilePath + "' must not be a null");

    if (!entryPath.isEmpty()) {
      // remove separator from the begining
      if (entryPath.startsWith("/"))
        entryPath = entryPath.substring(1);
    }

    zf = new ZipFile(zipFilePath);

    setZipEntry(entryPath);

    File f = new File(urlPath);
    String dirPath = urlPath.substring(0, urlPath.indexOf(f.getName()));
    dir = new ZipDirectoryManager(dirPath, tempPath);
  }

  private void setZipEntry(String path) throws Exception {
    zipEntry = zf.getEntry(path);

    // since ZipEntry is case-sensitive, try to read an entry in other cases.
    if (zipEntry == null){
      File f = new File(path);
      String filePart = f.getName();

      String dirPart = null;
      if (f.getParent() != null)
        dirPart = path.substring(0, path.indexOf(filePart));

      // try to read with a file name in upper-case
      zipEntry = zf.getEntry(dirPart == null ?
                             f.getName().toUpperCase() :
                             dirPart + f.getName().toUpperCase());

      // try to read with a file name in lower-case
      if (zipEntry == null) {
        zipEntry = zf.getEntry(dirPart == null ?
                               f.getName().toLowerCase() :
                               dirPart + f.getName().toLowerCase());

        if (zipEntry == null)
          throw new Exception("Zip entry '" + path +
                              "' is not found in the zip file '" + zf.getName() + "'");
      }
    }

    if (zipEntry.isDirectory())
      throw new Exception("Zip entry '" + path +
                          "' can not be a directory in the zip file '" +
                          zf.getName() + "'");
  }

  ZipFileManager(ZipDirectoryManager dir, ZipFile zf, ZipEntry entry) {
    this.dir = dir;
    this.zf = zf;

    this.zipEntry = entry;
  }

  ZipFileManager(ZipDirectoryManager dir, ZipFile zf, String entryPath, String fileName) throws
      Exception {
    this.dir = dir;
    this.zf = zf;

    setZipEntry(entryPath + fileName);
  }

  public RandomAccessFile getRandomAccess(String mode) throws Exception {
    /**@todo Implement this com.relationaljunction.jdbc.csv.store.CommonFile abstract method*/
    throw new UnsupportedOperationException(
        "Method getRandomAccess() not yet implemented.");
  }

  public boolean exists(){
    return zipEntry != null;
  }

  public String getName() {
    return new File(zipEntry.getName()).getName();
  }

  public boolean isReadOnly(){
    return true;
  }

  public String getPath() {
    return "zip://" + zf.getName() + "/" + zipEntry.getName();
  }

  public String getDirPath(){
    return dir.getPath();
  }

  public DirectoryManager getDir(){
    return dir;
  }

  public OutputStream getOutputStream(boolean append) throws Exception {
    throw new UnsupportedOperationException(
        "Write operations for zip protocol are not supported!");
  }

  public InputStream getInputStream() throws Exception {
    return zf.getInputStream(zipEntry);
  }

  public void delete() throws Exception{
     throw new UnsupportedOperationException("Write operations are not supported!");
  }

   @Override
   public void create() throws Exception {
      throw new UnsupportedOperationException("Write operations are not supported!");
   }

   public static void main(String[] args) {
    try {
      ZipFileManager zf = new ZipFileManager(
          "zip://c:/java/test/files/archive.zip/Folder1/Folder2/FOOD.DBF");
      System.out.println("path = " + zf.getPath());
      System.out.println("dir path = " + zf.getDirPath());
      System.out.println("file name = " + zf.getName());


      zf = new ZipFileManager(
          "zip://c:/java/test/files/archive.zip/FOOD.DBF");
      System.out.println("\npath = " + zf.getPath());
      System.out.println("dir path = " + zf.getDirPath());
      System.out.println("file name = " + zf.getName());

      ZipDirectoryManager zd = new ZipDirectoryManager(
          "zip://c:/java/test/files/archive.zip");
      zf = (ZipFileManager) zd.getFileManager("food.dbf");
      System.out.println("\npath = " + zf.getPath());
      System.out.println("dir path = " + zf.getDirPath());
      System.out.println("file name = " + zf.getName());

    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
