package com.relationaljunction.jdbc.common.h2;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.utils.UnexpectedException;
import com.relationaljunction.utils.concurrency.FutureConcurrentHashMap;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

abstract public class CommonDriver2 implements Driver {
   private final Logger log = LoggerFactory.getLogger("CommonDriver2");

   public final static String LOG_PATH = "logPath";
   public final static String FUNCTION_PREFIX = "function:";

   public final static String SINGLETON_CONNECTION = "singletonConnection";
   public final static boolean DEFAULT_SINGLETON_CONNECTION = false;

   // deprecated. Replaced to 'dbInMemory' property
   public final static String CACHING = "caching";
   public static final boolean DEFAULT_CACHING = true;

   public final static String DATE_FORMAT = "dateFormat";
   public final static String URL = "url";
   // StelsDBF file extension property
   public final static String FILE_EXTENSION = "extension";
   // StelsCSV file extension property
   public final static String FILE_EXTENSION2 = "fileExtension";
   public final static String SCHEMA = "schema";
   public final static String PATH = "path";
   public final static String CHARSET = "charset";
   public final static boolean DEFAULT_READ_ONLY_SUB_MODE = true;
   public final static String TEMP_PATH = "tempPath";
   public static final String DEFAULT_TEMP_PATH = System.getProperty(
           "java.io.tmpdir");
   public final static String USE_WEB_PARAM = "useWebParam";
   public static final String DEFAULT_USE_WEB_PARAM = null;
   // ##### lock properties #####
   public final static String LOCK = "lock";
   public static final String DEFAULT_LOCK = "os_lock";
   public final static String LOCK_TIMEOUT = "lockTimeout";
   public static final int DEFAULT_LOCK_TIMEOUT = 0;
   public final static String LOCK_CHECK_TIME = "lockCheckTime";
   public static final int DEFAULT_LOCK_CHECK_TIME = 100;
   public static final int ADAPTING_TABLE_NAMES = 1;
   public static final int NON_ADAPTING_TABLE_NAMES = 2;
   public static final int TABLE_NAMING_TYPE = ADAPTING_TABLE_NAMES;

   private PrintWriter pwLog = null;
   private static final PrintStream debugLog = null;
   //   private static Hashtable<String,
//           Connection> hashConnections = new Hashtable<String, Connection>();
   private static final FutureConcurrentHashMap<String, Connection> hashConnections =
           new FutureConcurrentHashMap<String, Connection>();


   abstract public String getURLPrefix();

   abstract public String getDriverName();

   abstract public String getH2TriggerClassName();

   // returns a connection without "close()" method implemented.
   // The feature is intended for Java servers like Tomcat, Glassfish, etc
   // See also "singletonConnection" property
   protected abstract Connection getPersistentConnectionForJavaServers(Properties props)
           throws SQLException;

   // additional properties
   public boolean supportsInsertOnRecord() {
      return true;
   }

   public boolean supportsUpdateOnRecord() {
      return false;
   }

   public boolean supportsDeleteOnRecord() {
      return false;
   }
   // end of additional properties

//   public SQLException createException(String message, Exception ex) {
//      return createException(message,
//              ex instanceof SQLException ? ((SQLException) ex).getSQLState() : null, ex);
//   }

   public SQLException createException(String message) {
//    writeLog(message);
      return new SQLException("[" + getDriverName() + "] " + message);
   }

   public SQLException createException(Exception ex) {
//    writeLog(message);
      return new SQLException("[" + getDriverName() + "] " + ex.getMessage(), ex);
   }

   public SQLException createException(String message, Exception ex) {
//    writeLog(message);
      return createException(message, "", ex);
   }

   public SQLException createException(String message, String sqlState, Exception ex) {
//    writeLog(message);
      return new SQLException("[" + getDriverName() + "] " + message, sqlState, ex);
   }

   protected abstract Connection getConnection(Properties props) throws
           SQLException;

   public void writeLog(String message) {
      if (pwLog == null)
         DriverManager.println(getDriverName() + " -> " + message);
      else
         pwLog.println(getDriverName() + " -> " + message);
   }

   public PrintWriter getLogWriter() throws SQLException {
      return pwLog;
   }

   public void setLogWriter(PrintWriter out) throws SQLException {
      this.pwLog = out;
   }

   private void loadPropertiesFromURL(String url, Properties props) throws
           SQLException {
      // ############ get path and parameters from url ##############
      String path;
      String urlParams = null;

      // ##### begin position of url parameters
      int paramsPos = url.indexOf("??");
      // check for server page path
      if (paramsPos != -1) {
         path = url.substring(getURLPrefix().length(), paramsPos);
         urlParams = url.substring(paramsPos + 2);
      }
      // standard path
      else {
         paramsPos = url.indexOf("?");
         if (paramsPos == -1)
            path = url.substring(getURLPrefix().length());
         else {
            path = url.substring(getURLPrefix().length(), paramsPos);
            urlParams = url.substring(paramsPos + 1);
         }
      }

      // ############ get parameters from url ##############
      if (paramsPos != -1) {
         try {
            StringTokenizer st = new StringTokenizer(urlParams, "&!");
            while (st.hasMoreElements()) {
               String set = st.nextToken();
               String param = set.substring(0, set.indexOf("=")).trim();
               String value = set.substring(set.indexOf("=") + 1);
               props.setProperty(param, value);
            }
         } catch (Exception ex1) {
            throw createException("Incorrect driver parameters in the URL");
         }
      }

      // ############ set the path property ##############
      props.setProperty(CommonDriver2.PATH, path);
   }

   public Connection connect(String url, Properties info) throws SQLException {
      // ############ check for correct url ############
      if (!url.startsWith(getURLPrefix())) return null;


      com.relationaljunction.utils.OtherUtils.writeLogInfo(log, this +
              " -> connect(): url= " + url);

      final Properties props = new Properties();
      if (info != null) props.putAll(info);

      props.put(CommonDriver2.URL, url);

      loadPropertiesFromURL(url, props);

      // if "singletonConnection" = true all connections with the same URL will be the same (singleton)
      // it is especially effective for servers
      boolean singletonConnection = DEFAULT_SINGLETON_CONNECTION;

      if (props.getProperty(SINGLETON_CONNECTION) != null)
         singletonConnection = Boolean.valueOf(props.getProperty(
                 SINGLETON_CONNECTION));

      if (singletonConnection) {
         try {
            return hashConnections.putIfAbsent(url, new Callable<Connection>() {
               public Connection call() throws Exception {
                  Connection connNew = getPersistentConnectionForJavaServers(props);

                  com.relationaljunction.utils.OtherUtils.writeLogInfo(log, CommonDriver2.super.toString() +
                          " -> creating a new singleton connection = " +
                          connNew);

                  return connNew;
               }
            });
         } catch (Exception e) {
            throw new UnexpectedException("Error while creating a JDBC connection " +
                    "(singletonConnection = true) for the URL " + url, e);
         }
      } else
         return getConnection(props);
   }

   public void removeConnection(String url) {
      if (url == null) return;

      hashConnections.remove(url);

      com.relationaljunction.utils.OtherUtils.writeLogInfo(log, this +
              " -> connection '" + url + "' was removed from the cache");
   }

   public boolean acceptsURL(String url) throws SQLException {
      writeLog("acceptURL(): url= " + url);
      return url.startsWith(getURLPrefix());
   }

   public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws
           SQLException {
      return new DriverPropertyInfo[0];
   }

   public abstract int getMajorVersion();

   public abstract int getMinorVersion();

   public boolean jdbcCompliant() {
      return true;
   }

   /*
   @Override
   public void finalize() throws Throwable {
      super.finalize();

      try {
         OtherUtils.writeLogInfo(log, this + " finalize()");

         // close singletonConnection's if they are exist
         if (hashConnections == null || hashConnections.size() == 0) return;

         Enumeration<Connection> conns = hashConnections.elements();

         while (conns.hasMoreElements()) {
            conns.nextElement().close();
         }

         hashConnections.clear();
      } catch (Exception e) {
      }
   }
   */
}
