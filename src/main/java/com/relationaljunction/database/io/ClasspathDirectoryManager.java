package com.relationaljunction.database.io;

public class ClasspathDirectoryManager extends DirectoryManager{
  String classpath = null;

  ClasspathDirectoryManager(String path) throws Exception{
    // ######## check if filepath is a correct path. #############
    if (path.trim().isEmpty())
      throw new Exception("Empty path is incorrect");

    if (path.startsWith("classpath://"))
      classpath = path.substring("classpath://".length());
    else
      classpath = path;

    if (!classpath.trim().isEmpty() && !classpath.endsWith("/") &&
        !classpath.endsWith("\\"))
        this.classpath = this.classpath + "/";

//    getClass().getClassLoader().getSystemResourceAsStream(path);
  }

  public FileManager getFileManager(String fileName) throws Exception{
    return new ClasspathFileManager(this, fileName);
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

  public FileManager[] listFiles(String extension, String templateName){
    return null;
  }

  public String getPath() {
    return "classpath://" + classpath;
  }

  public boolean isReadOnly(){
    return true;
  }

  public boolean supportsAppending(){
    return false;
  }

  public boolean supportsRandomAccess(){
    return false;
  }

  public static void main(String[] args) throws Throwable {
//    java.io.InputStream is = ClassLoader.getSystemResourceAsStream(
//        "res\\schema.xml");


    ClasspathDirectoryManager dir = new ClasspathDirectoryManager(
        "classpath://res");
    FileManager fm = dir.getFileManager("prices.txt");

    System.out.println("Exists :" + fm.exists());
    java.io.InputStream is = fm.getInputStream();

    int b;
    while ( (b = is.read()) != -1) {
      char c = (char) b;
      System.out.print("" + (char) b); //This prints out content that is unreadable.
      //Isn't it supposed to print out html tag?
    }

  }


}
