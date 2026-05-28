package com.relationaljunction.jdbc.common.h2;

import static com.relationaljunction.utils.OtherUtils.writeLogInfo;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.h2.StoreSchemaIF2;
import com.relationaljunction.jdbc.common.h2.sql.SQLCommand;
import com.relationaljunction.utils.OtherUtils;

public abstract class CommonConnection2 implements Connection {
    
   private final Logger log = LoggerFactory.getLogger("CommonConnection2");

   public final static String LOAD_INDEXES = "loadIndexes";
   public final static boolean DEFAULT_LOAD_INDEXES = true;
   public final static String IGNORE_CASE = "ignoreCase";
   public final static boolean DEFAULT_IGNORE_CASE = true;
   public final static String CREATE_VIEWS_FOR_QUERIES = "createViewsForQueries";
   public final static boolean DEFAULT_CREATE_VIEWS_FOR_QUERIES = false;

   // syncbase database modes
   public final static String DB_PATH = "dbPath";
   public final static String DB_IN_MEMORY = "dbInMemory";
   public static final boolean DEFAULT_DB_IN_MEMORY = true;
   public final static String DB_CACHING = "dbCaching";
   public static boolean DEFAULT_DB_CACHING = true;

   public final static String PRE_SQL = "preSQL";
   public final static String PRESERVE_COLUMN_NAMES = "preserveColumnNames";

   // ### case settings for identifiers
   // preserve column names in H2. All columns will be quoted to do that.
   public final static boolean DEFAULT_PRESERVE_COLUMN_NAMES = false;
   // all identifiers (table and columns) to upper case
   public final static String IDENTIFIERS_TO_UPPER_CASE = "identifiersToUpperCase";
   public final static boolean DEFAULT_IDENTIFIERS_TO_UPPER_CASE = false;
   public final static String TABLE_IDENTIFIERS_TO_UPPER_CASE = "tableIdentifiersToUpperCase";
   public final static boolean DEFAULT_TABLE_IDENTIFIERS_TO_UPPER_CASE = false;
   public final static String COLUMN_IDENTIFIERS_TO_UPPER_CASE = "columnIdentifiersToUpperCase";
   public final static boolean DEFAULT_COLUMN_IDENTIFIERS_TO_UPPER_CASE = false;

   public final static String READ_ONLY = "readOnly";
   public final static String LOGGING_PATH = "logingPath";
   public final static String LOGGING_CONFIG_PATH = "logingConfigPath";
   public final static boolean DEFAULT_READ_ONLY = false;

   public final static String MULTI_THREADED = "multiThreaded";
   public final static boolean DEFAULT_MULTI_THREADED = true;
   public final static String WATCH_FILE_MODIFICATIONS =
           "watchFileModifications";
   public final static boolean DEFAULT_WATCH_FILE_MODIFICATIONS = false;
   public final static String CHECK_PERIOD = "checkPeriod";
   public final static int DEFAULT_CHECK_PERIOD = 1000; // 1 second
   public final static int DEFAULT_LOCK_TIME_OUT = 100000; // 100 seconds


   public CommonDriver2 driver = null;
   public boolean loadIndexes = DEFAULT_LOAD_INDEXES;

   protected org.h2.jdbc.JdbcConnection h2Conn = null;
   protected Properties props = null;
   protected boolean persistentMode = false;
   protected boolean dbInMemory = true;
   protected boolean dbCaching = DEFAULT_DB_CACHING;
   protected String url = null;
   protected boolean multiThreaded = DEFAULT_MULTI_THREADED;
   protected WatchModificationsThread watchThread = null;
   protected boolean watchMods = DEFAULT_WATCH_FILE_MODIFICATIONS;
   protected CacheTableManager cacheTableManager = null;
   protected boolean autoCommit = true;

   private StoreSchemaIF2 schema = null;
   private Views views = null;
   private SQLQueriesCache sqlQueriesCache = null;
   private boolean closed = false;
   private String tempPath = null;
   private String dbPath = null;
   private boolean ignoreCase = DEFAULT_IGNORE_CASE;
   private boolean readOnly = DEFAULT_READ_ONLY;
   private String preSQL = null;
   private String loggingPath = null;
   private String loggingConfigPath = null;
   private int checkPeriod = DEFAULT_CHECK_PERIOD;
   private DatabaseMetaData meta = null;
   private int transactionIsolationLevel = TRANSACTION_READ_COMMITTED;
   private boolean preserveColumnNames = DEFAULT_PRESERVE_COLUMN_NAMES;
   private boolean columnIdentifiersToUpperCase = DEFAULT_COLUMN_IDENTIFIERS_TO_UPPER_CASE;
   private boolean tableIdentifiersToUpperCase = DEFAULT_TABLE_IDENTIFIERS_TO_UPPER_CASE;

   private final Set<String> systemTableSet = new HashSet<String>();

   static {
      try {
         Class.forName("org.h2.Driver");
      } catch (ClassNotFoundException ex) {
         throw new RuntimeException("org.h2.Driver class was not found!");
      }
   }

   public CommonConnection2(CommonDriver2 driver, Properties props) throws
           SQLException {
      this.driver = driver;
      this.props = props;

      // init schema
      this.schema = createSchema(props);

      this.views = new Views();

      writeLogInfo(log,
              "opening a JDBC connection with properties= " +
                      props);

      loadCommmonPropertiesInVariables(props);
      loadSpecificPropertiesInVariables(props);

      openH2Connection();

      // register old functions used in previuos versions
      registerOldFuncs();

      // seek and register user-defined functions
      seekUserFuncs(props);

      // init operation (synchro) tables
      initCacheTableManager();

      // init meta data
      meta = new CommonMetaData2(this);

      // init cache of parsed SQL queries
      sqlQueriesCache = new SQLQueriesCache();

      // start a thread that looks for file modifications
      runWatchingModificationsThread();

      executePredefinedSQL();

      initSystemTableSet();
   }

   private void initSystemTableSet() {
      systemTableSet.add("SYSTEM_RANGE");
   }

   public boolean isSystemTable(String tableName) {
      return systemTableSet.contains(tableName);
   }

   private void runWatchingModificationsThread() {
      try {
         if (watchMods) {
            if (!isCachingMode()) throw new UnsupportedOperationException(
                    "The 'watchFileModifications' property can't be " +
                            "used together with disabled caching ('dbCaching = false')");

            watchThread = createWatchingModificationsThread(checkPeriod);
            watchThread.start();
            watchThread.setPriority(2);
         }
      } catch (Exception ex) {
         log.warn("Can't run WatchModificationsThread. Error was " +
                 ex.getMessage(), ex);
      }
   }

   public WatchModificationsThread createWatchingModificationsThread(int checkPeriod) {
      return new WatchModificationsThread(this, checkPeriod);
   }

   private void loadCommmonPropertiesInVariables(Properties props) throws
           SQLException {
      if (props != null) {
         // caching. Deprecated. Replaced to 'dbInMemory' property
         if (props.getProperty(CommonDriver2.CACHING) != null) {
            dbInMemory = Boolean.valueOf(props.getProperty(
                    CommonDriver2.CACHING));
         }

         // dbInMemory
         if (props.getProperty(DB_IN_MEMORY) != null) {
            dbInMemory = Boolean.valueOf(props.getProperty(
                    DB_IN_MEMORY));
         }

         // url
         if (props.getProperty(CommonDriver2.URL) != null) {
            url = props.getProperty(CommonDriver2.URL);
         }

         // ignoreCase
         if (props.getProperty(IGNORE_CASE) != null) {
            ignoreCase = Boolean.valueOf(props.getProperty(IGNORE_CASE));
         }

         // preserveColumnNames
         if (props.getProperty(PRESERVE_COLUMN_NAMES) != null) {
            preserveColumnNames =
                    Boolean.valueOf(props.getProperty(PRESERVE_COLUMN_NAMES));
            columnIdentifiersToUpperCase = !preserveColumnNames;
            tableIdentifiersToUpperCase = !preserveColumnNames;
         }

         // identifiersToUpperCase
         if (props.getProperty(IDENTIFIERS_TO_UPPER_CASE) != null) {
            boolean identifiersToUpperCase =
                    Boolean.valueOf(props.getProperty(IDENTIFIERS_TO_UPPER_CASE));
            columnIdentifiersToUpperCase = identifiersToUpperCase;
            tableIdentifiersToUpperCase = identifiersToUpperCase;
         }

         // columnIdentifiersToUpperCase
         if (props.getProperty(COLUMN_IDENTIFIERS_TO_UPPER_CASE) != null) {
            columnIdentifiersToUpperCase =
                    Boolean.valueOf(props.getProperty(COLUMN_IDENTIFIERS_TO_UPPER_CASE));

         }

         // tableIdentifiersToUpperCase
         if (props.getProperty(TABLE_IDENTIFIERS_TO_UPPER_CASE) != null) {
            tableIdentifiersToUpperCase =
                    Boolean.valueOf(props.getProperty(TABLE_IDENTIFIERS_TO_UPPER_CASE));
         }

         // read only mode
         if (props.getProperty(READ_ONLY) != null) {
            readOnly = Boolean.valueOf(props.getProperty(READ_ONLY));
         }

         // multi threaded
         if (props.getProperty(MULTI_THREADED) != null) {
            multiThreaded = Boolean.valueOf(props.getProperty(MULTI_THREADED));
         }

         // temp path
         if (props.getProperty(CommonDriver2.TEMP_PATH) != null) {
            tempPath = props.getProperty(CommonDriver2.TEMP_PATH);
            if (!new File(tempPath).exists())
               throw new SQLException("Temporary path '" + tempPath +
                       "' doesn't exist");
         }

         // path to logging.properties
         if (props.getProperty(LOGGING_CONFIG_PATH) != null) {
            OtherUtils.TRACE_ENABLED = true;

            loggingConfigPath = props.getProperty(LOGGING_CONFIG_PATH);
            if (!new File(loggingConfigPath).exists())
               throw new SQLException("Log4J configuration path '" + loggingConfigPath +
                       "' doesn't exist");
         }

         // logging path
         if (props.getProperty(LOGGING_PATH) != null) {
            OtherUtils.TRACE_ENABLED = true;

            loggingPath = props.getProperty(LOGGING_PATH);

            if (loggingConfigPath == null) {
                
                
                
                
                
                
                
                
                
                
                
                
                
//               ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
//               builder.setStatusLevel(Level.DEBUG);
//               builder.setConfigurationName("DefaultLogger");
//               AppenderComponentBuilder appenderBuilder = builder.newAppender("RJ_FILE", "FileAppender").
//                   addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
//               appenderBuilder.add(builder.newLayout("PatternLayout").
//                   addAttribute("pattern", "%d{MM/dd HH:mm:ss} %5p %t - %m%n"));
//               builder.add(appenderBuilder);
//               builder.add(builder.newLogger("org.apache.logging.log4j", Level.DEBUG).
//                   add(builder.newAppenderRef("Stdout")).
//                   addAttribute("additivity", false));
//               builder.add(builder.newRootLogger(Level.TRACE).add(builder.newAppenderRef("Stdout")));
//               Configurator.reconfigure(builder.build());
//            } else {
//                try (InputStream is = LoadLogPropertiesFile.class.getClassLoader().
//                        getResourceAsStream("logging.properties")) {
//                    LogManager.getLogManager().readConfiguration(is);
//                } catch (IOException e) {
//                    throw new SQLException("Error reading logging configuration file at path '" + loggingConfigPath, e);
//                }
//                
//                
//                
//                
//            	ConfigurationSource source;
//				try {
//					source = new ConfigurationSource(new FileInputStream(new File(loggingConfigPath)));
//	            	Configurator.initialize(null, source);
//				} catch (FileNotFoundException e) {
//					throw new SQLException("logging configuration file '" + loggingConfigPath +
//		                       "' doesn't exist");
//				} catch (IOException e) {
//					throw new SQLException("Error reading logging configuration file at path '" + loggingConfigPath);
//				}
            }
         }

         // preSQL
         if (props.getProperty(PRE_SQL) != null) {
            String sqlFilePath = props.getProperty(PRE_SQL);
            FileReader fr;
            try {
               fr = new FileReader(sqlFilePath);
               preSQL = com.relationaljunction.utils.FileUtils.fileContentToString(fr);
               fr.close();
            } catch (IOException ex) {
               throw new SQLException("Error while reading a predefined SQL file '" +
                       sqlFilePath +
                       "'. Error was: " + ex.getMessage());
            }
         }

         // watch modifications
         if (props.getProperty(WATCH_FILE_MODIFICATIONS) != null) {
            watchMods = Boolean.valueOf(props.getProperty(WATCH_FILE_MODIFICATIONS));
         }

         // check period
         if (props.getProperty(CHECK_PERIOD) != null) {
            checkPeriod = Integer.parseInt(props.getProperty(CHECK_PERIOD));
         }

         // db path
         if (props.getProperty(DB_PATH) != null) {
            dbPath = props.getProperty(DB_PATH);
            persistentMode = true;
         }

         // dbCaching
         if (props.getProperty(DB_CACHING) != null) {
            dbCaching = Boolean.valueOf(props.getProperty(
                    DB_CACHING));
         }
      }
   }

   protected void loadSpecificPropertiesInVariables(Properties props) {

   }

   protected void initCacheTableManager() {
      this.cacheTableManager = new CacheTableManager(this);

      // should be called outside, due to initializing triggers
      if (isCachingMode() && isPersistentMode())
         this.cacheTableManager.initTablesInPersistentMode();
   }

   private void openH2Connection() throws SQLException {
//    h2Conn = (org.h2.jdbc.JdbcConnection) DriverManager.getConnection(
//        "jdbc:h2:other/temp/db", "sa", "");

      Properties h2Properties = new Properties();

      // H2 file lock for persistent mode only
      if (!isPersistentMode())
         h2Properties.setProperty("FILE_LOCK", "NO");

//      h2Properties.setProperty("MULTI_THREADED", "1");
      h2Properties.setProperty("MODE", "RJ");
      h2Properties.setProperty("PAGE_SIZE", "4096");
      h2Properties.setProperty("CACHE_SIZE", "65536");
      h2Properties.setProperty("DEFAULT_LOCK_TIMEOUT", String.valueOf(DEFAULT_LOCK_TIME_OUT));
      h2Properties.setProperty("IGNORECASE", String.valueOf(ignoreCase));

      // add driver specific properties
      addSpecificH2Properties(h2Properties);

      initModes(h2Properties);
   }

   /**
    * initialize modes
    *
    * @param h2Properties
    * @throws SQLException
    */
   private void initModes(Properties h2Properties) throws SQLException {
      if (!isCachingMode()) {
         if (isPersistentMode())
            throw new IllegalArgumentException("Disabled H2 cache can't be used " +
                    "with a persistence synchrobase");

         // non caching mode
         writeLogInfo(log, "H2 cache is disabled");
//         h2Properties.setProperty("h2.check", "false");
      }

      if (isPersistentMode()) {
         // persistent H2 syncbase on a hard drive
         writeLogInfo(log, "the driver mode is now with a persistent synchrobase: '", dbPath, "'");
         h2Conn = (org.h2.jdbc.JdbcConnection) DriverManager.getConnection(
                 "jdbc:h2_custom:" + dbPath, h2Properties);
      } else if (isDBInMemory()) {
         // temporary in-memory H2 syncbase
         writeLogInfo(log, "the driver mode is now with a temporary synchrobase in memory");
         h2Conn = (org.h2.jdbc.JdbcConnection) DriverManager.getConnection(
                 "jdbc:h2_custom:mem:", h2Properties);
      } else {
         // temporary H2 syncbase on a hard drive
         String tempDbPath = getTempH2FilePath(tempPath, "h2db");
         writeLogInfo(log, "the driver mode is now with a temporary synchrobase: '", tempDbPath, "'");
         h2Conn = (org.h2.jdbc.JdbcConnection) DriverManager.getConnection(
                 "jdbc:h2_custom:" + tempDbPath, h2Properties);
      }

      ((org.h2.engine.Session) h2Conn.getSession()).setWrapperConnection(this);

      // create relationaljunction schema for collateral operation tables
      Statement st = h2Conn.createStatement();
      st.execute("CREATE SCHEMA IF NOT EXISTS RJ_SCHEMA");

      st.executeUpdate("CREATE TABLE IF NOT EXISTS RJ_SCHEMA.TABLE_STAT(stat INT, name VARCHAR(10), info VARCHAR(20))");
      st.executeUpdate("INSERT INTO RJ_SCHEMA.TABLE_STAT(stat, name, info) VALUES(0, 'table', 'not loaded')");

      if (persistentMode)
         // create a table that contains info about loaded external tables (store files)
         st.executeUpdate("CREATE TABLE IF NOT EXISTS RJ_SCHEMA.TABLES_INFO(table_name VARCHAR(255) PRIMARY KEY, insert_oper_table_sql VARCHAR(16348), create_oper_table_sql VARCHAR(16348), create_oper_index_sql VARCHAR(16348), fields OTHER, sql_table_name VARCHAR(255), mod_date TIMESTAMP)");

      // H2 in transaction mode
      h2Conn.setAutoCommit(false);

      st.close();
   }

   private void initExternalEngineMode(Properties h2Properties) {

   }

   protected void addSpecificH2Properties(Properties h2Properties) {
   }

   // execute an predefined SQL script
   private void executePredefinedSQL() throws SQLException {
      if (preSQL != null) {
         try {
            Statement st = createStatement();

            writeLogInfo(log,
                    "######## executing predefined SQL");

            // queries separated by ";"
            StringTokenizer sqlTokens = new StringTokenizer(preSQL, ";");

            while (sqlTokens.hasMoreElements()) {
               String sqlToken = sqlTokens.nextToken();
               if (sqlToken.trim().isEmpty() ||
                       // comment
                       sqlToken.trim().startsWith("//"))
                  continue;

               st.execute(sqlToken);
            }

            writeLogInfo(log,
                    "######## end of executing predefined SQL\n");

            st.close();
         } catch (Exception ex) {
            ex.printStackTrace();
            throw OtherUtils.createAndLogSQLException(log,
                    "Can't execute preliminary SQL using the property 'preSQL'. Error was: " +
                            ex.getMessage(), true);
         }
      }
   }

   SQLCommand parseSQLQuery(String query) throws Exception {
      return sqlQueriesCache.parseSQLQuery(query);
   }

   public org.h2.jdbc.JdbcConnection getH2Connection() {
      return h2Conn;
   }

   public void reloadCache() throws Exception {
      // get sorted table names
      Collection<CacheTable> tablesSorted = getCacheTableManager().getSortedCacheTables();

      writeLogInfo(log, "Connection(" + this +
              ") -> reloadCache() -> start. Tables to reload: " + tablesSorted);

      try {
         for (CacheTable cacheTable : tablesSorted) {
            // it seems to be never called
//            if (!table.isAlreadyLoaded())
//               throw new Exception("The table '" + tableName + "' is not loaded yet");

            if (cacheTable.isInTransaction())
               throw new Exception("The table '" + cacheTable.getTableName() + "' is in transaction now");

//            table.writeLock();
            boolean lockSuccess = cacheTable.writeLock(DEFAULT_LOCK_TIME_OUT - 10000);
            if (!lockSuccess) throw new Exception("lock timeout in reloadCache()");
         }

         // reload schema with exclusive locking
         try {
            if (getSchemaIF2().requiresLockingForWritingOperations())
               getSchemaIF2().lockForWriting();

            getSchemaIF2().reload();
         } finally {
            if (getSchemaIF2().requiresLockingForWritingOperations())
               getSchemaIF2().unlock();
         }

         // clear views
         views.clear();

         // reload tables
         for (CacheTable cacheTable : tablesSorted) {
            cacheTable.reloadData();
         }

         writeLogInfo(log, "Connection(" + this +
                 ") -> reloadCache() -> successful");
      } catch (Exception e) {
         e.printStackTrace();
         throw new Exception("Can't reload the cache. Error was: " + e.getMessage(), e);
      } finally {
         for (CacheTable cacheTable : tablesSorted) {
            cacheTable.writeUnlock();
         }
      }
   }

   private void closeH2Connection() throws SQLException {
      try {
         if (!persistentMode) {
            Statement st = h2Conn.createStatement();
            st.execute("DROP ALL OBJECTS DELETE FILES");
            st.close();
         }

         ((org.h2.engine.Session) h2Conn.getSession()).clearWrapperConnection();
         h2Conn.close();
         h2Conn = null;
      } catch (SQLException ex) {
         throw driver.createException(
                 "[H2] Can't delete temporary H2 files. Error was: " +
                         ex.getMessage(), ex.getSQLState(), ex);
      }
   }

   private static String getTempH2FilePath(String tempPath,
                                           String tempFilePrefix) {
      String tempFilePath;
      try {
         File tempFile;
         if (tempPath != null)
            tempFile = File.createTempFile(tempFilePrefix, "", new File(tempPath));
         else
            tempFile = File.createTempFile(tempFilePrefix, "", null);

         tempFilePath = tempFile.getAbsolutePath();
         tempFile.delete();
      } catch (IOException ex) {
         throw new IllegalArgumentException(
                 "Unexpected error while creating a temp file: " +
                         ex.getMessage());
      }
      return tempFilePath;
   }

   abstract protected StoreSchemaIF2 createSchema(Properties props) throws
           SQLException;

   /**
    * registers old SQL functions for compatibility with previous versions
    */
   private void registerOldFuncs() {
      try {
         Statement st = h2Conn.createStatement();

         // replace_string
         st.execute(
                 "CREATE ALIAS IF NOT EXISTS REPLACE_STRING FOR \"com.relationaljunction.jdbc.common.h2.sql.SimpleFuncs.replace_string(java.lang.String, java.lang.String, java.lang.String)\"");

         // to_date
         st.execute(
                 "CREATE ALIAS IF NOT EXISTS TO_DATE FOR \"com.relationaljunction.jdbc.common.h2.sql.SimpleFuncs.to_date(java.lang.String, java.lang.String)\"");

         // to_int
         st.execute(
                 "CREATE ALIAS IF NOT EXISTS TO_INT FOR \"com.relationaljunction.jdbc.common.h2.sql.SimpleFuncs.to_int(java.lang.Integer)\"");

         // to_long
         st.execute(
                 "CREATE ALIAS IF NOT EXISTS TO_LONG FOR \"com.relationaljunction.jdbc.common.h2.sql.SimpleFuncs.to_long(java.lang.Long)\"");

         // to_float
         st.execute(
                 "CREATE ALIAS IF NOT EXISTS TO_FLOAT FOR \"com.relationaljunction.jdbc.common.h2.sql.SimpleFuncs.to_float(java.lang.Float)\"");

         // to_double
         st.execute(
                 "CREATE ALIAS IF NOT EXISTS TO_DOUBLE FOR \"com.relationaljunction.jdbc.common.h2.sql.SimpleFuncs.to_double(java.lang.Double)\"");

         // to_big_decimal
//      st.execute(
//	  "CREATE ALIAS IF NOT EXISTS TO_BIG_DECIMAL FOR \"com.relationaljunction.jdbc.common.h2.sql.SimpleFuncs.to_big_decimal(java.math.BigDecimal)\"");

         // to_string
         st.execute(
                 "CREATE ALIAS IF NOT EXISTS TO_STRING FOR \"com.relationaljunction.jdbc.common.h2.sql.SimpleFuncs.to_string(java.lang.String)\"");

         // matches
         st.execute(
                 "CREATE ALIAS IF NOT EXISTS MATCHES FOR \"com.relationaljunction.jdbc.common.h2.sql.SimpleFuncs.matches(java.lang.String, java.lang.String)\"");

         // val
         st.execute(
                 "CREATE ALIAS IF NOT EXISTS VAL FOR \"com.relationaljunction.jdbc.common.h2.sql.SimpleFuncs.val(java.lang.String)\"");

         st.execute(
                 "CREATE ALIAS IF NOT EXISTS RJ_GET_RECORDS_LOADED FOR \"com.relationaljunction.jdbc.common.h2.sql.SimpleFuncs.swap2()\"");
         st.close();
      } catch (Exception ex) {
         log.warn("Can't register an old function. Error was:" + ex.getMessage(),
                 ex);
      }
   }

   /**
    * seek and register user-defined functions passed to the driver
    *
    * @param props Properties
    */
   private void seekUserFuncs(Properties props) {
      Enumeration enumer = props.keys();
      while (enumer.hasMoreElements()) {
         String property = (String) enumer.nextElement();
         if (property.startsWith(CommonDriver2.FUNCTION_PREFIX)) {
            String funcName = property.substring(CommonDriver2.FUNCTION_PREFIX.
                    length());
            String handler = props.getProperty(property);
            try {
               Statement st = h2Conn.createStatement();
               st.execute("CREATE ALIAS IF NOT EXISTS " + funcName + " FOR \"" +
                       handler + "\"");
               st.close();
            } catch (Exception ex) {
               log.error(
                       "Connection: [Error] Can't register the user-defined function '" +
                               funcName + "'. Error was:" + ex.getMessage());
            }
         }
      }
   }

   /**
    * check if the connection is already closed
    *
    * @throws SQLException
    */
   protected void checkClosed() throws SQLException {
      if (isClosed())
         throw new SQLException("The connection is already closed");
   }

   public CacheTableManager getCacheTableManager() {
      return cacheTableManager;
   }

   /**
    * get all loaded/created views to the driver
    *
    * @return Views
    */
   public Views getViews() {
      return views;
   }

   public StoreSchemaIF2 getSchemaIF2() {
      return schema;
   }

   public boolean preserveColumnNames() {
      return preserveColumnNames;
   }

   public boolean columnsIdentifiersToUpperCase() {
      return columnIdentifiersToUpperCase;
   }

   public boolean tableIdentifiersToUpperCase() {
      return tableIdentifiersToUpperCase;
   }

   public boolean isDBInMemory() {
      return dbInMemory;
   }

   public boolean isPersistentMode() {
      return persistentMode;
   }

   public boolean isCachingMode() {
      return dbCaching;
   }

   public Statement createStatement() throws SQLException {
      checkClosed();
      Statement st = new CommonStatement2(this, h2Conn.createStatement());

      writeLogInfo(log, "Connection(" + this +
              ") -> createStatement() -> Statement(" +
              st + ")");
      return st;
   }

   public Statement createStatement(int resultSetType, int resultSetConcurrency) throws
           SQLException {
      checkClosed();
      Statement st = new CommonStatement2(this,
              h2Conn.createStatement(resultSetType,
                      resultSetConcurrency));
      writeLogInfo(log, "Connection(" + this +
              ") -> createStatement() -> Statement(" +
              st + ")");
      return st;
   }

   public Statement createStatement(int resultSetType, int resultSetConcurrency,
                                    int resultSetHoldability) throws
           SQLException {
      checkClosed();
      Statement st = new CommonStatement2(this,
              h2Conn.createStatement(resultSetType,
                      resultSetConcurrency, resultSetHoldability));
      writeLogInfo(log, "Connection(" + this +
              ") -> createStatement() -> Statement(" +
              st + ")");
      return st;
   }

   public PreparedStatement prepareStatement(String sql) throws SQLException {
      checkClosed();
      writeLogInfo(log,
              "Connection -> prepareStatement(): connection = " + this);
      return new CommonPreparedStatement2(this, sql, -1, -1, -1, -1, null, null);
   }

   public PreparedStatement prepareStatement(String sql, int resultSetType,
                                             int resultSetConcurrency) throws
           SQLException {
      writeLogInfo(log,
              "Connection -> prepareStatement(): connection = " + this);
      return new CommonPreparedStatement2(this, sql, resultSetType,
              resultSetConcurrency, -1, -1, null, null);
   }

   public PreparedStatement prepareStatement(String sql, int resultSetType,
                                             int resultSetConcurrency,
                                             int resultSetHoldability) throws
           SQLException {
      writeLogInfo(log,
              "Connection -> prepareStatement(): connection = " + this);
      return new CommonPreparedStatement2(this, sql, resultSetType,
              resultSetConcurrency,
              resultSetHoldability, -1, null, null);
   }

   public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws
           SQLException {
      writeLogInfo(log,
              "Connection -> prepareStatement(): connection = " + this);
      return new CommonPreparedStatement2(this, sql, -1, -1, -1,
              autoGeneratedKeys, null, null);
   }

   public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws
           SQLException {
      writeLogInfo(log,
              "Connection -> prepareStatement(): connection = " + this);
      return new CommonPreparedStatement2(this, sql, -1, -1, -1, -1,
              columnIndexes, null);
   }

   public PreparedStatement prepareStatement(String sql, String[] columnNames) throws
           SQLException {
      writeLogInfo(log,
              "Connection -> prepareStatement(): connection = " + this);
      return new CommonPreparedStatement2(this, sql, -1, -1, -1, -1, null,
              columnNames);
   }

   public CallableStatement prepareCall(String sql) throws SQLException {
      return h2Conn.prepareCall(sql);
   }

   public CallableStatement prepareCall(String sql, int resultSetType,
                                        int resultSetConcurrency,
                                        int resultSetHoldability) throws
           SQLException {
      return h2Conn.prepareCall(sql, resultSetType, resultSetConcurrency,
              resultSetHoldability);
   }

   public CallableStatement prepareCall(String sql, int resultSetType,
                                        int resultSetConcurrency) throws
           SQLException {
      return h2Conn.prepareCall(sql, resultSetType, resultSetConcurrency);
   }

   public String nativeSQL(String sql) throws SQLException {
      return h2Conn.nativeSQL(sql);
   }

   public void setAutoCommit(boolean autoCommit) throws
           SQLException {
      checkClosed();

      writeLogInfo(log, "setting auto commit to '" + autoCommit + "'");

      // set autoCommit for wrapper connection (CommonConnection)
      // but H2 will remain in transact mode
      this.autoCommit = autoCommit;
//      if (!persistentMode && transactMode == 0)
//         getH2Connection().setAutoCommit(autoCommit);
   }

   public boolean getAutoCommit() throws SQLException {
      return autoCommit;
   }

   public void commit() throws SQLException {
      checkClosed();

      cacheTableManager.commit();
   }

   void commit(Set<String> tableNames) throws SQLException {
      cacheTableManager.commit(tableNames);
   }

   public synchronized void rollback() throws SQLException {
      cacheTableManager.rollback();
   }

   public void close() throws SQLException {
      closePhysically();
   }

   synchronized public void closePhysically() throws SQLException {
      writeLogInfo(log, "Connection(" + this + ") -> close()");
      writeLogInfo(log, "closing connection to " + props + "\n\n");

      checkClosed();

      if (watchMods) {
         // stopping a watch modification thread
         try {
            watchThread.stopThread();
            watchThread.interrupt();

            com.relationaljunction.utils.concurrency.ConcurrentUtils.executeTaskForLimitedTime(new Callable<Void>() {
               public Void call() throws Exception {
                  while (watchThread.isAlive()) {
                     Thread.sleep(1000);
                  }

                  return null;
               }
            }, DEFAULT_LOCK_TIME_OUT + 5000);
         } catch (Exception ex) {
            // was not able to stop a watch modification thread
            ex.printStackTrace();
            log.warn("Error while stopping watching thread", ex);
         }
      }

      watchThread = null;

      closed = true;

      getCacheTableManager().clear();
      getSchemaIF2().close();
      closeH2Connection();

      driver.removeConnection(url);

      driver = null;
      cacheTableManager = null;
      schema = null;
      views = null;
      meta = null;
      h2Conn = null;
   }

   public boolean isClosed() throws SQLException {
      return closed;
   }

   public DatabaseMetaData getMetaData() throws SQLException {
//      if (meta == null)
//         meta = new CommonMetaData2(this);

      writeLogInfo(log, "Connection(" + this +
              ") -> getMetaData() -> DatabaseMetaData(" +
              meta.toString() + ")");

//    DatabaseMetaData meta = null;
//    try {
//      meta = new CommonMetaData2(this);
//      OtherUtils.writeLogInfo(log, "Connection(" + this.toString() +
//					   ") -> getMetaData() -> DatabaseMetaData(" +
//					   meta.toString() + ")", true);
//    } catch (SQLException ex) {
//      throw new SQLException(
//          "Can't initialize an DatabaseMetaData instance. Error was: " +
//          ex.getMessage());
//    }

      return meta;
   }

   public void setReadOnly(boolean readOnly) throws SQLException {
      this.readOnly = readOnly;
      h2Conn.setReadOnly(readOnly);
   }

   public boolean isReadOnly() throws SQLException {
      return readOnly;
   }

   public void setCatalog(String catalog) throws SQLException {
      // ignore this request
   }

   //to override
   public String getCatalog() throws SQLException {
      return h2Conn.getCatalog();
//    String path = getSchema().getSchemaProperties().getProperty(CommonDriver2.
//        PATH);
//    return path == null ? "" : path;
   }

   public void setTransactionIsolation(int level) throws SQLException {
      this.transactionIsolationLevel = level;
      h2Conn.setTransactionIsolation(level);
   }

   public int getTransactionIsolation() throws SQLException {
      return transactionIsolationLevel;
   }

   public SQLWarning getWarnings() throws SQLException {
      return h2Conn.getWarnings();
   }

   public void clearWarnings() throws SQLException {
      h2Conn.clearWarnings();
   }

   public Map getTypeMap() throws SQLException {
      return h2Conn.getTypeMap();
   }

   public void setTypeMap(Map<String, Class<?>> map) throws
           SQLException {
      h2Conn.setTypeMap(map);
   }

   public void setHoldability(int holdability) throws SQLException {
      h2Conn.setHoldability(holdability);
   }

   public int getHoldability() throws SQLException {
      return h2Conn.getHoldability();
   }

   // Savepoints are not supported.

   public Savepoint setSavepoint() throws SQLException {
      return null;
   }

   public Savepoint setSavepoint(String name) throws SQLException {
      return null;
   }

   public void rollback(Savepoint savepoint) throws SQLException {
   }

   public void releaseSavepoint(Savepoint savepoint) throws SQLException {
   }

   // --- JDK 1.6 ---

   public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new UnsupportedOperationException();
   }

   public boolean isWrapperFor(Class<?> iface) throws SQLException {
      throw new UnsupportedOperationException();
   }

   public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public Clob createClob() throws SQLException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public Blob createBlob() throws SQLException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public NClob createNClob() throws SQLException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public SQLXML createSQLXML() throws SQLException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public boolean isValid(int timeout) throws SQLException {
      return true;
   }

   public void setClientInfo(String name, String value) throws SQLClientInfoException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void setClientInfo(Properties properties) throws SQLClientInfoException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public String getClientInfo(String name) throws SQLException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public Properties getClientInfo() throws SQLException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }
}
