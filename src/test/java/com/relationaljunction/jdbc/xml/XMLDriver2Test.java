package com.relationaljunction.jdbc.xml;

import java.sql.*;
import java.util.logging.Logger;

import com.relationaljunction.jdbc.common.h2.CommonDriver2;
import com.relationaljunction.jdbc.xml.h2.*;

public class XMLDriver2Test extends CommonDriver2 {
   public static final String DEFAULT_CHARSET = null;
   private static final String PRODUCT_NAME = "Relational Junction XML JDBC driver";
   private static final int PRODUCT_MAJOR_VERSION = 7;
   private static final int PRODUCT_MINOR_VERSION = 0;
   private static final String PRODUCT_VERSION = PRODUCT_MAJOR_VERSION + "." +
           PRODUCT_MINOR_VERSION;
   private static final String URL_PREFIX = "jdbc:relationaljunction:xml:";
   public static final String DEFAULT_EXTENSION = "";
   public static final String H2_TRIGGER_CLASS_NAME = "com.relationaljunction.jdbc.common.h2.sql.DefaultH2Trigger";

   public XMLDriver2Test() {
   }

   // additional properties
   public boolean supportsInsertOnRecord() {
      return false;
   }

   public boolean supportsUpdateOnRecord() {
      return false;
   }

   public boolean supportsDeleteOnRecord() {
      return false;
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
      return new XMLConnection2(this, props);
   }

   protected Connection getPersistentConnectionForJavaServers(java.util.Properties props)
           throws SQLException {
      return new PersistentXMLConnection2(this, props);
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
         java.sql.DriverManager.registerDriver(new XMLDriver2());
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
//        String d = "01.10.2010";
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
                 "[Relational Junction CSV JDBC Driver] FATAL ERROR: Could not initialize CSV driver. Message was: "
                         + e.getMessage());
      }
   }

@Override
public Logger getParentLogger() throws SQLFeatureNotSupportedException {
	// TODO Auto-generated method stub
	return null;
}

}
