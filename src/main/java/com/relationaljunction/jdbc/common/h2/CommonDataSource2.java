package com.relationaljunction.jdbc.common.h2;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.*;
import javax.sql.DataSource;


abstract public class CommonDataSource2 implements DataSource, java.io.Serializable, Referenceable {
  private String description = null;
  protected Properties props = new Properties();
  public CommonDriver2 driver = null;

  protected CommonDataSource2(CommonDriver2 driver) {
    this.driver = driver;
  }

  public abstract Connection getConnection() throws SQLException;

  public Connection getConnection(String username, String password) throws
      SQLException {
    return getConnection();
  }

  public PrintWriter getLogWriter() throws SQLException {
   return driver.getLogWriter();
  }

  public void setLogWriter(PrintWriter out) throws SQLException {
    driver.setLogWriter(out);
  }

  public void setLoginTimeout(int seconds) throws SQLException {
  }

  public int getLoginTimeout() throws SQLException {
    return 0;
  }

  abstract public Reference getReference() throws NamingException;

  //  ######### Other methods #################

  public Properties getProperties(){
    return props;
  }

  public void reset(){
    props.clear();
  }

  //  ######### Description #################

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  //  ######### Path #################

  public String getPath() {
    return props.getProperty(CommonDriver2.PATH);
  }

  public void setPath(String path) throws Exception{
    if (path == null)throw new Exception("Path must not be null!");
    props.setProperty(CommonDriver2.PATH, path);
    setURL();
  }

  private void setURL(){
    props.setProperty(CommonDriver2.URL, driver.getURLPrefix() + getPath());
  }

  //############## User functions ################

  public void setUserFunction(String name, String handler) {
    props.setProperty(CommonDriver2.FUNCTION_PREFIX + name, handler);
  }

  public String getUserFunction(String name) {
      return props.getProperty(CommonDriver2.FUNCTION_PREFIX + name);
  }

  //############## Connection properties ################
  public void setIgnoreCase(boolean ignoreCase) {
    props.setProperty(CommonConnection2.IGNORE_CASE, Boolean.toString(ignoreCase));
  }

  public boolean isIgnoreCase() {
    String value = props.getProperty(CommonConnection2.IGNORE_CASE);
    return value == null ? CommonConnection2.DEFAULT_IGNORE_CASE :
	Boolean.valueOf(value).booleanValue();
  }

  public void setDbInMemory(boolean caching) {
    props.setProperty(CommonConnection2.DB_IN_MEMORY, Boolean.toString(caching));
  }

  public boolean isDbInMemory() {
    String value = props.getProperty(CommonConnection2.DB_IN_MEMORY);
    return value == null ? CommonConnection2.DEFAULT_DB_IN_MEMORY :
	Boolean.valueOf(value).booleanValue();
  }

  public void setTempPath(String tempPath) {
    if (tempPath != null)
      props.setProperty(CommonDriver2.TEMP_PATH, tempPath);
  }

  public String getTempPath() {
    String value = props.getProperty(CommonDriver2.TEMP_PATH);
    return value == null ? CommonDriver2.DEFAULT_TEMP_PATH : value;
  }

}
