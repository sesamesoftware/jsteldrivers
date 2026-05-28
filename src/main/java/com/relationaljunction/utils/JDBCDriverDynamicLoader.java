package com.relationaljunction.utils;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;
import java.net.*;
import java.io.File;

public class JDBCDriverDynamicLoader implements Driver {
  private final Driver driver;

  public JDBCDriverDynamicLoader(String driverFilePath, String className) {
    try {
      URL u;

      if (driverFilePath.startsWith("file://"))
        u = new URL(driverFilePath);
      else{
        File f = new File(driverFilePath);
        if (!f.exists())
          throw new Exception(driverFilePath + " does not exist");
        u = f.toURL();
      }

      URLClassLoader ucl = new URLClassLoader(new URL[] {u});
      driver = (Driver) Class.forName(className, true, ucl).newInstance();
    } catch (Exception ex) {
      throw new RuntimeException("Can't load the driver. Error was: " +
                                 ex.getMessage());
    }
  }

  public void register() throws SQLException{
    DriverManager.registerDriver(this);
  }

  public JDBCDriverDynamicLoader(Driver d) {
    this.driver = d;
  }

  public boolean acceptsURL(String u) throws SQLException {
    return this.driver.acceptsURL(u);
  }

  public Connection connect(String u, Properties p) throws SQLException {
    return this.driver.connect(u, p);
  }

  public int getMajorVersion() {
    return this.driver.getMajorVersion();
  }

  public int getMinorVersion() {
    return this.driver.getMinorVersion();
  }

  public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws
      SQLException {
    return this.driver.getPropertyInfo(u, p);
  }

  public boolean jdbcCompliant() {
    return this.driver.jdbcCompliant();
  }

@Override
public Logger getParentLogger() throws SQLFeatureNotSupportedException {
	// TODO Auto-generated method stub
	return null;
}
}
