package com.relationaljunction.database.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.utils.UnexpectedException;

import cz.dhl.ftp.FtpFile;
import cz.dhl.ftp.FtpInputStream;
import cz.dhl.ftp.FtpOutputStream;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2006</p>
 * <p>Company: J-Stels Software</p>
 * @author not attributable
 * @version 2.5
 */

public class FTPFileManager
    extends FileManager {
  private final Logger log = LoggerFactory.getLogger("FTPFileManager");

  private FTPDirectoryManager ftpDir;
  private String ftpFileName;
  private final String ftpPath;
  private FtpFile ftpFile = null;
  private static final boolean CHECK_LOWER_AND_UPPER_FILE_CASES = true;

  public FTPFileManager(FTPDirectoryManager ftpDir, String ftpFileName) {
    this.ftpDir = ftpDir;
    this.ftpPath = ftpDir.getPath();
    this.ftpFileName = ftpFileName;
    this.ftpFile = new FtpFile(ftpFileName, ftpDir.ftp);
  }

  public FTPFileManager(String ftpFileNamePath) throws Exception{
    // ftp file syntax: ftp://user:password@hostname[:port]/[dirpath/]filename
    URL urlPath = new URL(ftpFileNamePath);
    this.ftpFileName = new File(urlPath.getFile()).getName();
    ftpPath = ftpFileNamePath.substring(0, ftpFileNamePath.indexOf(
        ftpFileName));
    this.ftpDir = new FTPDirectoryManager(ftpPath);
    this.ftpFile = new FtpFile(ftpFileName, ftpDir.ftp);
  }

  public String getDirPath() {
    return ftpDir.getPath();
  }

  public java.util.Date getModificationTime() {
    try {
      return new java.util.Date(ftpFile.lastModified());
    } catch (Exception ex) {
      try {
        ftpDir.reconnect();
        return new java.util.Date(ftpFile.lastModified());
      } catch (Exception ex2) {
        log.warn(
            "FTPFileManager: could not return a file modification date",
            ex2);
        return null;
      }
    }
  }

  public RandomAccessFile getRandomAccess(String mode) throws
          Exception {
    throw new UnsupportedOperationException(
        "Method getRandomAccess() is not supported.");
  }

  public boolean isReadOnly() {
    return false;
  }

  public DirectoryManager getDir() {
    return ftpDir;
  }

  public boolean exists() throws Exception {
    InputStream is = getInputStream();

    if (is == null)
      return false;

    try {
      // it may throw an exception
      is.close();
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }

    return true;
  }

  public String getName() {
    return ftpFileName;
  }

  public String getPath() {
    return getDirPath() + getName();
  }

  public OutputStream getOutputStream(boolean append) throws
          Exception {
    try {
      return new FtpOutputStream(ftpFile, append);
    }
    catch (Exception ex) {
      try {
        ftpDir.reconnect();
        this.ftpFile = new FtpFile(ftpFileName, ftpDir.ftp);
        return new FtpOutputStream(ftpFile, append);
      }
      catch (Exception ex1) {
        throw new Exception("Unknown error '" + ex1.getMessage() +
                            "' while writing the file '" + getName() +
                            "' to the FTP server '" + getDirPath() + "'");
      }
    }
  }

/*
  public InputStream getInputStream() throws java.lang.Exception {
    try {
      return new FtpInputStream(ftpFile);
    }
    catch (IOException ex) {
//      System.out.println("#####"+ex.getMessage());

      if (ex.getMessage().indexOf("No such file") > 0 ||
          ex.getMessage().indexOf("File not found") > 0 ||
          ex.getMessage().indexOf("550") > 0)
        return null; // file not found
      try {
        ftpDir.reconnect();
        this.ftpFile = new FtpFile(ftpFileName, ftpDir.ftp);
        return new FtpInputStream(ftpFile);
      }
      catch (Exception ex1) {
        ex1.printStackTrace();
        if (ex.getMessage().indexOf("No such file") > 0 ||
            ex.getMessage().indexOf("File not found") > 0 ||
            ex.getMessage().indexOf("550") > 0)
          return null; // file not found
        throw new UnexpectedException("Unknown error '" + ex.getMessage() +
                            "' while reading the file '" + getName() +
                            "' from the FTP server: '" + getDirPath() + "'");
      }
    }
  }
*/


  public InputStream getInputStream() throws Exception {
    InputStream is;

    is = _getInputStream();

    if (CHECK_LOWER_AND_UPPER_FILE_CASES && is == null) {
      this.ftpFileName = ftpFileName.toLowerCase();
      this.ftpFile = new FtpFile(this.ftpFileName, ftpDir.ftp);
      is = _getInputStream();

      if (is == null) {
        this.ftpFileName = ftpFileName.toUpperCase();
        this.ftpFile = new FtpFile(this.ftpFileName, ftpDir.ftp);
        is = _getInputStream();
      }
    }

    return is;
  }

  private InputStream _getInputStream() throws Exception {
    try {
      return new FtpInputStream(this.ftpFile);
    }
    catch (IOException ex) {
//      System.out.println("#####"+ex.getMessage());

      if (ex.getMessage().indexOf("No such file") > 0 ||
          ex.getMessage().indexOf("File not found") > 0 ||
          ex.getMessage().indexOf("550") > 0)
        return null; // file not found
      try {
        ftpDir.reconnect();
        this.ftpFile = new FtpFile(ftpFileName, ftpDir.ftp);
        return new FtpInputStream(this.ftpFile);
      }
      catch (Exception ex1) {
//        ex1.printStackTrace();
        if (ex.getMessage().indexOf("No such file") > 0 ||
            ex.getMessage().indexOf("File not found") > 0 ||
            ex.getMessage().indexOf("550") > 0)
          return null; // file not found
        throw new UnexpectedException("Unknown error '" + ex.getMessage() +
                            "' while reading the file '" + getName() +
                            "' from the FTP server: '" + getDirPath() + "'");
      }
    }
  }

  public static void main(String[] args) {
    try {
      FTPFileManager FTPFileManager1 = new FTPFileManager(
          "ftp://csvjdbcc:ShrFk23NFb@csv-jdbc.com:21/home/csvjdbcc/public_html/order.htm");
      InputStream is = FTPFileManager1.getInputStream();
      if (is == null)
        System.out.println("File not found");
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

   public void create() throws Exception{
     ftpDir.createFile(ftpFileName);
   }

  public void delete() throws Exception{
    ftpDir.dropFile(ftpFileName);
  }

  public void close(){
    try {
      if (ftpDir != null) {
        ftpDir.disconnect();
        ftpDir.close();
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    ftpFile = null;
    ftpDir = null;
    ftpFileName = null;
  }

}
