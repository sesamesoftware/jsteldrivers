package com.relationaljunction.jdbc.mdb;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import com.relationaljunction.jdbc.common.h2.CommonConnection2;
import com.relationaljunction.jdbc.common.h2.CommonDataSource2;
import com.relationaljunction.jdbc.common.h2.CommonDriver2;
import com.relationaljunction.jdbc.mdb.h2.*;

public class MDBDataSource2 extends CommonDataSource2 {
   public MDBDataSource2() {
      super(new MDBDriver2());
   }

   public Connection getConnection() throws SQLException {
      if (getPath() == null)
         throw new SQLException(
                 "You must set a path to a MDB/ACCDB file! Use setPath() method.");
      driver.writeLog("Data Source " + getProperties() + ", java.sql.Driver=[" +
              driver.toString() + "] -> getConnection()");
      return driver.connect(driver.getURLPrefix() + getPath(), props);
   }

   public Reference getReference() throws NamingException {
      Reference ref = new Reference(this.getClass().getName(),
              "com.relationaljunction.jdbc.mdb.MDBObjectFactory2", null);
      ref.add(new StringRefAddr(CommonDriver2.PATH, getPath()));
      ref.add(new StringRefAddr(CommonDriver2.CHARSET, getCharset()));
      ref.add(new StringRefAddr(MDBSchema2.CREATE, String.valueOf(isCreate())));
      ref.add(new StringRefAddr(MDBSchema2.FORMAT_STRING, getFormat()));

      ref.add(new StringRefAddr(CommonConnection2.IGNORE_CASE,
              String.valueOf(isIgnoreCase())));
      ref.add(new StringRefAddr(CommonDriver2.LOG_PATH, getLogPath()));
      ref.add(new StringRefAddr(CommonDriver2.USE_WEB_PARAM, getWebParameterName()));
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
      return value == null ? MDBDriver2.DEFAULT_CHARSET : value;
   }

   public void setCreate(boolean create) {
      props.setProperty(MDBSchema2.CREATE,
              String.valueOf(create));
   }

   public boolean isCreate() {
      String value = props.getProperty(MDBSchema2.CREATE);
      return value != null && Boolean.valueOf(value);
   }

   public void setFormat(String format) {
      if (format != null)
         props.setProperty(MDBSchema2.FORMAT_STRING, format);
   }

   public String getFormat() {
      String value = props.getProperty(MDBSchema2.FORMAT_STRING);
      return value == null ? MDBSchema2.DEFAULT_FORMAT_STRING :
              value;
   }

   public void setLogPath(String logPath) {
      if (logPath != null)
         props.setProperty(CommonDriver2.LOG_PATH, logPath);
   }

   public String getLogPath() {
       return props.getProperty(CommonDriver2.LOG_PATH);
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
