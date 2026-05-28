package com.relationaljunction.jdbc.dbf;

import java.sql.*;
import java.util.logging.Logger;
import java.io.IOException;

import org.xBaseJ.Util;

import com.relationaljunction.jdbc.common.h2.CommonDriver2;
import com.relationaljunction.jdbc.dbf.h2.*;

public class DBFDriver2 extends CommonDriver2 {
  public static final String DEFAULT_CHARSET = "8859_1";
  private static final String PRODUCT_NAME = "Relational Junction DBF JDBC driver";
  private static final int PRODUCT_MAJOR_VERSION = 7;
  private static final int PRODUCT_MINOR_VERSION = 2;
  private static final String PRODUCT_VERSION = PRODUCT_MAJOR_VERSION + "." +
                                                PRODUCT_MINOR_VERSION;
  private static final String URL_PREFIX = "jdbc:relationaljunction:dbf:";
  public static final String DEFAULT_EXTENSION = ".dbf";
  public static final String H2_TRIGGER_CLASS_NAME =
      "com.relationaljunction.jdbc.common.h2.sql.DefaultH2Trigger";

  public DBFDriver2() {
  }

  // additional properties
  public boolean supportsInsertOnRecord() {
    return true;
  }

  public boolean supportsUpdateOnRecord() {
    return true;
  }

  public boolean supportsDeleteOnRecord() {
    return true;
  }

  // end of additional properties

  public String getURLPrefix() {
    return URL_PREFIX;
  }

  public String getDriverName() {
    return PRODUCT_NAME;
  }

  public String getDefaultFileExtension() {
    return DEFAULT_EXTENSION;
  }

  public String getH2TriggerClassName() {
    return H2_TRIGGER_CLASS_NAME;
  }

  protected Connection getConnection(java.util.Properties props) throws
      SQLException {
    return new DBFConnection2(this, props);
  }

  protected Connection getPersistentConnectionForJavaServers(java.util.
      Properties props) throws SQLException {
    return new PersistentDBFConnection2(this, props);
  }

  public int getMajorVersion() {
    return PRODUCT_MAJOR_VERSION;
  }

  public int getMinorVersion() {
    return PRODUCT_MINOR_VERSION;
  }

  // This static block inits the driver when the class is loaded by the JVM.
  static {
    try {
      DriverManager.registerDriver(new DBFDriver2());

      try {
        Util.setxBaseJProperty("otherValidCharactersInFieldNames",
                "%$# !.,;:@(){}+-*/><=" + '\\');
        Util.setxBaseJProperty("ignoreDBFLengthCheck", "true");
        Util.setxBaseJProperty("ignoreMissingMDX", "true");
      } catch (IOException ex) {
      }

      // ##############for registration####################
//      try {
//        System.out.println(PRODUCT_NAME + " " + PRODUCT_VERSION +
//                           " (Trial version)");
//        System.out.println("Trial limitations: ");
//        System.out.println(
//            "1) The trial version allows executing of not more than 50 queries at once.");
//        System.out.println(
//            "2) SELECT queries return the first 1000 records in the result set.");
//      } catch (Exception e1) {}
      // ##################################################

      //##############time license###################
//        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
//            "dd.MM.yyyy");
//        java.util.Date t1 = null;
//        String d = "31.12.2015";
//        System.out.println("This version is time-limited. It expires " + d);
//        try {
//          t1 = sdf.parse(d);
//        }
//        catch (Exception ex) {
//        }
//
//        if (new java.util.Date().compareTo(t1) > 0)
//          throw new RuntimeException(
//              "License has been expired");
      //##############################################

    } catch (SQLException e) {
      throw new RuntimeException(
          "[Relational Junstion DBF JDBC Driver] FATAL ERROR: Could not initialize the DBF driver. Message was: "
          + e.getMessage());
    }
  }

@Override
public Logger getParentLogger() throws SQLFeatureNotSupportedException {
	// TODO Auto-generated method stub
	return null;
}
}
