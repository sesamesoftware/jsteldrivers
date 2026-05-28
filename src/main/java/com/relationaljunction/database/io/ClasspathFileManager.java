package com.relationaljunction.database.io;

import java.io.*;

public class ClasspathFileManager
    extends FileManager {
  private String resourceName = null;
  private String resourceNamePath = null;

  ClasspathFileManager(ClasspathDirectoryManager dir, String resourceName) {
    this.dir = dir;
    this.resourceName = resourceName;
    this.resourceNamePath = dir.classpath + resourceName;

//    this.is = this.getClass().getClassLoader().getResourceAsStream(
//        resourceNamePath);
//    this.is = ClassLoader.getSystemResourceAsStream(
//        resourceNamePath);
  }

  public ClasspathFileManager(String resourceName) throws Exception {
    File f = new File(resourceName);
    this.resourceName = f.getName();

    if (resourceName.startsWith("classpath://"))
      this.resourceNamePath = resourceName.substring("classpath://".length());
    else
      this.resourceNamePath = resourceName;

    String dirPath = resourceName.substring(0, resourceName.indexOf(f.getName()));
    this.dir = new ClasspathDirectoryManager(dirPath);

//    this.dir = new ClasspathDirectoryManager(f.getParentFile().getPath());
  }

  public RandomAccessFile getRandomAccess(String mode) throws Exception {
    return null;
  }

  public OutputStream getOutputStream(boolean append) throws Exception {
    throw new Exception("Write operations for URL '" + getPath() +
                        "' are not supported!");
  }

  public InputStream getInputStream() throws Exception {
    return this.getClass().getClassLoader().getResourceAsStream(resourceNamePath);
  }

  public String getPath(){
    return "classpath://" + resourceNamePath;
  }

  public String getDirPath(){
    return dir.getPath();
  }

  public DirectoryManager getDir(){
    return dir;
  }

  public String getName(){
    return resourceName;
  }

  public boolean exists() throws Exception {
    InputStream is = getInputStream();
    if (is == null)
      return false;
    is.close();
    return true;
  }

  public boolean isReadOnly(){
    return true;
  }

  public void delete() throws Exception{
     throw new Exception("Write operations for URL '" + getPath() +
                         "' are not supported!");
  }

   @Override
   public void create() throws Exception {
      throw new Exception("Write operations for URL '" + getPath() +
                          "' are not supported!");
   }
}
