package com.relationaljunction.jdbc.dbf.h2;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.naming.Reference;
import javax.naming.StringRefAddr;

import com.relationaljunction.jdbc.common.h2.CommonConnection2;
import com.relationaljunction.jdbc.common.h2.CommonDataSource2;
import com.relationaljunction.jdbc.common.h2.CommonDriver2;
import com.relationaljunction.jdbc.dbf.DBFDriver2;

public class DBFDataSource2 extends CommonDataSource2 {
   public DBFDataSource2() {
      super(new DBFDriver2());
   }

   public Connection getConnection() throws SQLException {
      if (getPath() == null)
         throw new SQLException(
                 "You must set a path to DBF directory! Use setPath() method.");
      driver.writeLog("Data Source " + getProperties() + ", java.sql.Driver=[" +
              driver.toString() + "] -> getConnection()");
      return driver.connect(driver.getURLPrefix() + getPath(), props);
   }

   public Reference getReference() throws javax.naming.NamingException {
      Reference ref = new Reference(this.getClass().getName(),
              "com.relationaljunction.jdbc.dbf.h2.DBFObjectFactory2", null);
      ref.add(new StringRefAddr(CommonDriver2.PATH, getPath()));
      ref.add(new StringRefAddr(CommonDriver2.CHARSET, getCharset()));
      ref.add(new StringRefAddr(DBFSchema2.EMPTY_STRING_AS_NULL,
              String.valueOf(isEmptyStringIsNull())));
      ref.add(new StringRefAddr(CommonDriver2.FILE_EXTENSION, getFileExtension()));
      ref.add(new StringRefAddr(DBFSchema2.FORMAT, getFormat()));

      ref.add(new StringRefAddr(CommonConnection2.IGNORE_CASE,
              String.valueOf(isIgnoreCase())));
      ref.add(new StringRefAddr(CommonDriver2.LOG_PATH, getLogPath()));
      ref.add(new StringRefAddr(CommonDriver2.USE_WEB_PARAM, getWebParameterName()));
      ref.add(new StringRefAddr(DBFSchema2.TRIM_BLANKS,
              String.valueOf(isTrimBlanks())));
      ref.add(new StringRefAddr(CommonConnection2.DB_IN_MEMORY, String.valueOf(isDbInMemory())));
      ref.add(new StringRefAddr(CommonDriver2.TEMP_PATH, getTempPath()));
      return ref;
   }

   // ######### Properties #################

   public void setCharset(String charset) {
      if (charset != null)
         props.setProperty(CommonDriver2.CHARSET, charset);
   }

   public String getCharset() {
      String value = props.getProperty(CommonDriver2.CHARSET);
      return value == null ? DBFDriver2.DEFAULT_CHARSET : value;
   }

   public void setEmptyStringIsNull(boolean emptyStringIsNull) {
      props.setProperty(DBFSchema2.EMPTY_STRING_AS_NULL,
              String.valueOf(emptyStringIsNull));
   }

   public boolean isEmptyStringIsNull() {
      String value = props.getProperty(DBFSchema2.EMPTY_STRING_AS_NULL);
      return value == null ? DBFSchema2.DEFAULT_EMPTY_STRING_AS_NULL :
              Boolean.valueOf(value).booleanValue();
   }

   public void setFormat(String format) {
      if (format != null)
         props.setProperty(DBFSchema2.FORMAT, format);
   }

   public String getFormat() {
      String value = props.getProperty(DBFSchema2.FORMAT);
      return value == null ? DBFSchema2.DEFAULT_FORMAT_STRING :
              value;
   }

   public void setFileExtension(String fileExtension) {
      if (fileExtension != null)
         props.setProperty(CommonDriver2.FILE_EXTENSION, fileExtension);
   }

   public String getFileExtension() {
      String value = props.getProperty(CommonDriver2.FILE_EXTENSION);
      return value == null ? DBFDriver2.DEFAULT_EXTENSION : value;
   }

   public void setLogPath(String logPath) {
      if (logPath != null)
         props.setProperty(CommonDriver2.LOG_PATH, logPath);
   }

   public String getLogPath() {
       return props.getProperty(CommonDriver2.LOG_PATH);
   }

   public void setTrimBlanks(boolean trimBlanks) {
      props.setProperty(DBFSchema2.TRIM_BLANKS, String.valueOf(trimBlanks));
   }

   public boolean isTrimBlanks() {
      String value = props.getProperty(DBFSchema2.TRIM_BLANKS);
      return value == null ? DBFSchema2.DEFAULT_TRIM_BLANKS :
              Boolean.valueOf(value).booleanValue();
   }

   public void setWebParameterName(String webParam) {
      if (webParam != null)
         props.setProperty(CommonDriver2.USE_WEB_PARAM, webParam);
   }

   public String getWebParameterName() {
      String value = props.getProperty(CommonDriver2.USE_WEB_PARAM);
      return value == null ? CommonDriver2.DEFAULT_USE_WEB_PARAM : value;
   }

   // --- JDK 1.6 ---

   public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new UnsupportedOperationException();
   }

   public boolean isWrapperFor(Class<?> iface) throws SQLException {
      throw new UnsupportedOperationException();
   }

@Override
public Logger getParentLogger() throws SQLFeatureNotSupportedException {
	// TODO Auto-generated method stub
	return null;
}
}
