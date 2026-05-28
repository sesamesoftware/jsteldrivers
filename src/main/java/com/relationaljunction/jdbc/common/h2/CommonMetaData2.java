package com.relationaljunction.jdbc.common.h2;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;

import org.h2.jdbc.JdbcDatabaseMetaData;
import org.h2.tools.SimpleResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.StoreException;
import com.relationaljunction.database.StoreFieldIF;
import com.relationaljunction.database.StoreTableIF;
import com.relationaljunction.database.StoreTableReaderIF;
import com.relationaljunction.database.index.IndexFieldIF;
import com.relationaljunction.database.index.IndexSchemaIF;
import com.relationaljunction.database.index.IndexTableIF;
import com.relationaljunction.database.index.TablesRelationship;
import com.relationaljunction.database.view.ViewSchemaIF;
import com.relationaljunction.database.view.ViewTableIF;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.StringUtils;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class CommonMetaData2 implements DatabaseMetaData {
   private final Logger log = LoggerFactory.getLogger("CommonMetaData2");

   protected CommonConnection2 conn = null;
   protected JdbcDatabaseMetaData h2ConnectionMetaData = null;

//  public CommonMetaData2(CommonConnection2 conn,
//                         JdbcDatabaseMetaData h2ConnectionMetaData) {
//    this.conn = conn;
//    this.h2ConnectionMetaData = h2ConnectionMetaData;
//  }

   public CommonMetaData2(CommonConnection2 conn) throws SQLException {
      this.conn = conn;
      this.h2ConnectionMetaData = (JdbcDatabaseMetaData) conn.getH2Connection().
              getMetaData();
   }

   private String getSchemaName() {
      return "PUBLIC";
   }

   private String getTableIdentifierString(String name) {
      return getIdentifierString(name, conn.tableIdentifiersToUpperCase());
   }

   private String getColumnIdentifierString(String name) {
      return getIdentifierString(name, conn.columnsIdentifiersToUpperCase());
   }

   private String getIdentifierString(String name, boolean upperCase) {
      if (upperCase)
         // if a name contains reserved words and chars -> leave it without changes
         // if a name does not contain reserved words and chars -> to upper case
         // for example, table name should be case sensitive due to case-sensitivity in Linux
         return StringUtils.toUpperCaseIfNotReserved(name);
      else
         // leave name without changes
         return name;
   }

   private SimpleResultSet createSimpleResultSet(ResultSet rs) throws
           SQLException {
      SimpleResultSet simpleResultSet = new SimpleResultSet();
      ResultSetMetaData rsmd = rs.getMetaData();

      for (int i = 0; i < rsmd.getColumnCount(); i++)
         simpleResultSet.addColumn(rsmd.getColumnName(i + 1),
                 rsmd.getColumnType(i + 1),
                 rsmd.getPrecision(i + 1),
                 rsmd.getScale(i + 1));

      while (rs.next()) {
         Object[] objs = new Object[simpleResultSet.getColumnCount()];
         for (int i = 0; i < simpleResultSet.getColumnCount(); i++)
            objs[i] = rs.getObject(i + 1);
         simpleResultSet.addRow(objs);
      }

      return simpleResultSet;
   }

   private void addNonUniqueResultSet(PreparedStatement pstInsert, ResultSet rs) throws
           SQLException {
      int columnCount = pstInsert.getParameterMetaData().getParameterCount();

      while (rs.next()) {
         for (int i = 0; i < columnCount; i++)
            pstInsert.setObject(i + 1, rs.getObject(i + 1));

         try {
            pstInsert.execute();
         } catch (SQLException ex) {
//            ex.printStackTrace();
         }
      }
   }

   public boolean allProceduresAreCallable() throws SQLException {
      return h2ConnectionMetaData.allProceduresAreCallable();
   }

   public boolean allTablesAreSelectable() throws SQLException {
      return h2ConnectionMetaData.allTablesAreSelectable();
   }

   public String getURL() throws SQLException {
      return conn.url;
   }

   public String getUserName() throws SQLException {
      return "";
   }

   // to override
   public boolean isReadOnly() throws SQLException {
      return h2ConnectionMetaData.isReadOnly();
   }

   public boolean nullsAreSortedHigh() throws SQLException {
      return h2ConnectionMetaData.nullsAreSortedHigh();
   }

   public boolean nullsAreSortedLow() throws SQLException {
      return h2ConnectionMetaData.nullsAreSortedLow();
   }

   public boolean nullsAreSortedAtStart() throws SQLException {
      return h2ConnectionMetaData.nullsAreSortedAtStart();
   }

   public boolean nullsAreSortedAtEnd() throws SQLException {
      return h2ConnectionMetaData.nullsAreSortedAtEnd();
   }

   public String getDatabaseProductName() throws SQLException {
      return conn.driver.getDriverName();
   }

   public String getDatabaseProductVersion() throws SQLException {
      return conn.driver.getMajorVersion() + "." + conn.driver.getMinorVersion();
   }

   public String getDriverName() throws SQLException {
      return conn.driver.getDriverName();
   }

   public String getDriverVersion() throws SQLException {
      return conn.driver.getMajorVersion() + "." + conn.driver.getMinorVersion();
   }

   public int getDriverMajorVersion() {
      return conn.driver.getMajorVersion();
   }

   public int getDriverMinorVersion() {
      return conn.driver.getMinorVersion();
   }

   public boolean usesLocalFiles() throws SQLException {
      return true;
   }

   // to override
   public boolean usesLocalFilePerTable() throws SQLException {
      return true;
   }

   public boolean supportsMixedCaseIdentifiers() throws SQLException {
//      return h2ConnectionMetaData.supportsMixedCaseIdentifiers();
      // by default meta data returns table and column names in mixed case
      return !conn.columnsIdentifiersToUpperCase();
   }

   public boolean storesUpperCaseIdentifiers() throws SQLException {
//      return h2ConnectionMetaData.storesUpperCaseIdentifiers();
      // by default meta data returns table and column names in mixed case
      return conn.columnsIdentifiersToUpperCase();
   }

   public boolean storesLowerCaseIdentifiers() throws SQLException {
      return h2ConnectionMetaData.storesLowerCaseIdentifiers();
   }

   public boolean storesMixedCaseIdentifiers() throws SQLException {
//      return h2ConnectionMetaData.storesMixedCaseIdentifiers();
      return !conn.columnsIdentifiersToUpperCase();
   }

   public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
      return h2ConnectionMetaData.supportsMixedCaseQuotedIdentifiers();
   }

   public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
      return h2ConnectionMetaData.storesUpperCaseQuotedIdentifiers();
   }

   public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
      return h2ConnectionMetaData.storesLowerCaseQuotedIdentifiers();
   }

   public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
      return h2ConnectionMetaData.storesMixedCaseQuotedIdentifiers();
   }

   public String getIdentifierQuoteString() throws SQLException {
      return h2ConnectionMetaData.getIdentifierQuoteString();
   }

   public String getSQLKeywords() throws SQLException {
      return h2ConnectionMetaData.getSQLKeywords();
   }

   public String getNumericFunctions() throws SQLException {
      return h2ConnectionMetaData.getNumericFunctions();
   }

   public String getStringFunctions() throws SQLException {
      return h2ConnectionMetaData.getStringFunctions();
   }

   public String getSystemFunctions() throws SQLException {
      return h2ConnectionMetaData.getSystemFunctions();
   }

   public String getTimeDateFunctions() throws SQLException {
      return h2ConnectionMetaData.getTimeDateFunctions();
   }

   public String getSearchStringEscape() throws SQLException {
      return h2ConnectionMetaData.getSearchStringEscape();
   }

   public String getExtraNameCharacters() throws SQLException {
      return h2ConnectionMetaData.getExtraNameCharacters();
   }

   public boolean supportsAlterTableWithAddColumn() throws SQLException {
      return false;
   }

   public boolean supportsAlterTableWithDropColumn() throws SQLException {
      return false;
   }

   public boolean supportsColumnAliasing() throws SQLException {
      return h2ConnectionMetaData.supportsColumnAliasing();
   }

   public boolean nullPlusNonNullIsNull() throws SQLException {
      return h2ConnectionMetaData.nullPlusNonNullIsNull();
   }

   public boolean supportsConvert() throws SQLException {
      return h2ConnectionMetaData.supportsConvert();
   }

   public boolean supportsConvert(int fromType, int toType) throws SQLException {
      return h2ConnectionMetaData.supportsConvert(fromType, toType);
   }

   public boolean supportsTableCorrelationNames() throws SQLException {
      return h2ConnectionMetaData.supportsTableCorrelationNames();
   }

   public boolean supportsDifferentTableCorrelationNames() throws SQLException {
      return h2ConnectionMetaData.supportsDifferentTableCorrelationNames();
   }

   public boolean supportsExpressionsInOrderBy() throws SQLException {
      return h2ConnectionMetaData.supportsExpressionsInOrderBy();
   }

   public boolean supportsOrderByUnrelated() throws SQLException {
      return h2ConnectionMetaData.supportsOrderByUnrelated();
   }

   public boolean supportsGroupBy() throws SQLException {
      return h2ConnectionMetaData.supportsGroupBy();
   }

   public boolean supportsGroupByUnrelated() throws SQLException {
      return h2ConnectionMetaData.supportsGroupByUnrelated();
   }

   public boolean supportsGroupByBeyondSelect() throws SQLException {
      return h2ConnectionMetaData.supportsGroupByBeyondSelect();
   }

   public boolean supportsLikeEscapeClause() throws SQLException {
      return h2ConnectionMetaData.supportsGroupByBeyondSelect();
   }

   public boolean supportsMultipleResultSets() throws SQLException {
      return h2ConnectionMetaData.supportsMultipleResultSets();
   }

   public boolean supportsMultipleTransactions() throws SQLException {
      // ?
      return h2ConnectionMetaData.supportsMultipleTransactions();
   }

   public boolean supportsNonNullableColumns() throws SQLException {
      return h2ConnectionMetaData.supportsNonNullableColumns();
   }

   public boolean supportsMinimumSQLGrammar() throws SQLException {
      return h2ConnectionMetaData.supportsMinimumSQLGrammar();
   }

   public boolean supportsCoreSQLGrammar() throws SQLException {
      return h2ConnectionMetaData.supportsCoreSQLGrammar();
   }

   public boolean supportsExtendedSQLGrammar() throws SQLException {
      return h2ConnectionMetaData.supportsExtendedSQLGrammar();
   }

   public boolean supportsANSI92EntryLevelSQL() throws SQLException {
      return h2ConnectionMetaData.supportsANSI92EntryLevelSQL();
   }

   public boolean supportsANSI92IntermediateSQL() throws SQLException {
      return h2ConnectionMetaData.supportsANSI92IntermediateSQL();
   }

   public boolean supportsANSI92FullSQL() throws SQLException {
      return h2ConnectionMetaData.supportsANSI92FullSQL();
   }

   public boolean supportsIntegrityEnhancementFacility() throws SQLException {
      return h2ConnectionMetaData.supportsIntegrityEnhancementFacility();
   }

   public boolean supportsOuterJoins() throws SQLException {
      return h2ConnectionMetaData.supportsOuterJoins();
   }

   public boolean supportsFullOuterJoins() throws SQLException {
      return h2ConnectionMetaData.supportsFullOuterJoins();
   }

   public boolean supportsLimitedOuterJoins() throws SQLException {
      return h2ConnectionMetaData.supportsLimitedOuterJoins();
   }

   public String getSchemaTerm() throws SQLException {
      return h2ConnectionMetaData.getSchemaTerm();
   }

   public String getProcedureTerm() throws SQLException {
      return h2ConnectionMetaData.getProcedureTerm();
   }

   public String getCatalogTerm() throws SQLException {
      return "directory";
   }

   public boolean isCatalogAtStart() throws SQLException {
      return h2ConnectionMetaData.isCatalogAtStart();
   }

   public String getCatalogSeparator() throws SQLException {
      return h2ConnectionMetaData.getCatalogSeparator();
   }

   public boolean supportsSchemasInDataManipulation() throws SQLException {
      return h2ConnectionMetaData.supportsSchemasInDataManipulation();
   }

   public boolean supportsSchemasInProcedureCalls() throws SQLException {
      return h2ConnectionMetaData.supportsSchemasInProcedureCalls();
   }

   public boolean supportsSchemasInTableDefinitions() throws SQLException {
      return h2ConnectionMetaData.supportsSchemasInTableDefinitions();
   }

   public boolean supportsSchemasInIndexDefinitions() throws SQLException {
      return h2ConnectionMetaData.supportsSchemasInIndexDefinitions();
   }

   public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
      return h2ConnectionMetaData.supportsSchemasInPrivilegeDefinitions();
   }

   public boolean supportsCatalogsInDataManipulation() throws SQLException {
      return h2ConnectionMetaData.supportsCatalogsInDataManipulation();
   }

   public boolean supportsCatalogsInProcedureCalls() throws SQLException {
      return h2ConnectionMetaData.supportsCatalogsInDataManipulation();
   }

   public boolean supportsCatalogsInTableDefinitions() throws SQLException {
      return h2ConnectionMetaData.supportsCatalogsInTableDefinitions();
   }

   public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
      return h2ConnectionMetaData.supportsCatalogsInIndexDefinitions();
   }

   public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
      return h2ConnectionMetaData.supportsCatalogsInPrivilegeDefinitions();
   }

   public boolean supportsPositionedDelete() throws SQLException {
      return h2ConnectionMetaData.supportsPositionedDelete();
   }

   public boolean supportsPositionedUpdate() throws SQLException {
      return h2ConnectionMetaData.supportsPositionedUpdate();
   }

   public boolean supportsSelectForUpdate() throws SQLException {
      return h2ConnectionMetaData.supportsSelectForUpdate();
   }

   public boolean supportsStoredProcedures() throws SQLException {
      return h2ConnectionMetaData.supportsStoredProcedures();
   }

   public boolean supportsSubqueriesInComparisons() throws SQLException {
      return h2ConnectionMetaData.supportsSubqueriesInComparisons();
   }

   public boolean supportsSubqueriesInExists() throws SQLException {
      return h2ConnectionMetaData.supportsSubqueriesInExists();
   }

   public boolean supportsSubqueriesInIns() throws SQLException {
      return h2ConnectionMetaData.supportsSubqueriesInIns();
   }

   public boolean supportsSubqueriesInQuantifieds() throws SQLException {
      return h2ConnectionMetaData.supportsSubqueriesInQuantifieds();
   }

   public boolean supportsCorrelatedSubqueries() throws SQLException {
      return h2ConnectionMetaData.supportsCorrelatedSubqueries();
   }

   public boolean supportsUnion() throws SQLException {
      return h2ConnectionMetaData.supportsUnion();
   }

   public boolean supportsUnionAll() throws SQLException {
      return h2ConnectionMetaData.supportsUnionAll();
   }

   public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
      return h2ConnectionMetaData.supportsOpenCursorsAcrossCommit();
   }

   public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
      return h2ConnectionMetaData.supportsOpenCursorsAcrossRollback();
   }

   public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
      return h2ConnectionMetaData.supportsOpenStatementsAcrossCommit();
   }

   public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
      return h2ConnectionMetaData.supportsOpenStatementsAcrossRollback();
   }

   public int getMaxBinaryLiteralLength() throws SQLException {
      return h2ConnectionMetaData.getMaxBinaryLiteralLength();
   }

   public int getMaxCharLiteralLength() throws SQLException {
      return h2ConnectionMetaData.getMaxCharLiteralLength();
   }

   public int getMaxColumnNameLength() throws SQLException {
      return h2ConnectionMetaData.getMaxColumnNameLength();
   }

   public int getMaxColumnsInGroupBy() throws SQLException {
      return h2ConnectionMetaData.getMaxColumnsInGroupBy();
   }

   public int getMaxColumnsInIndex() throws SQLException {
      return h2ConnectionMetaData.getMaxColumnsInIndex();
   }

   public int getMaxColumnsInOrderBy() throws SQLException {
      return h2ConnectionMetaData.getMaxColumnsInOrderBy();
   }

   public int getMaxColumnsInSelect() throws SQLException {
      return h2ConnectionMetaData.getMaxColumnsInSelect();
   }

   public int getMaxColumnsInTable() throws SQLException {
      return h2ConnectionMetaData.getMaxColumnsInTable();
   }

   // to override
   public int getMaxConnections() throws SQLException {
      return h2ConnectionMetaData.getMaxConnections();
   }

   public int getMaxCursorNameLength() throws SQLException {
      return h2ConnectionMetaData.getMaxCursorNameLength();
   }

   public int getMaxIndexLength() throws SQLException {
      return h2ConnectionMetaData.getMaxIndexLength();
   }

   public int getMaxSchemaNameLength() throws SQLException {
      return h2ConnectionMetaData.getMaxSchemaNameLength();
   }

   public int getMaxProcedureNameLength() throws SQLException {
      return h2ConnectionMetaData.getMaxProcedureNameLength();
   }

   public int getMaxCatalogNameLength() throws SQLException {
      return h2ConnectionMetaData.getMaxCatalogNameLength();
   }

   public int getMaxRowSize() throws SQLException {
      return h2ConnectionMetaData.getMaxRowSize();
   }

   public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
      return h2ConnectionMetaData.doesMaxRowSizeIncludeBlobs();
   }

   public int getMaxStatementLength() throws SQLException {
      return h2ConnectionMetaData.getMaxStatementLength();
   }

   public int getMaxStatements() throws SQLException {
      return h2ConnectionMetaData.getMaxStatements();
   }

   public int getMaxTableNameLength() throws SQLException {
      return h2ConnectionMetaData.getMaxTableNameLength();
   }

   public int getMaxTablesInSelect() throws SQLException {
      return h2ConnectionMetaData.getMaxTablesInSelect();
   }

   public int getMaxUserNameLength() throws SQLException {
      return h2ConnectionMetaData.getMaxUserNameLength();
   }

   public int getDefaultTransactionIsolation() throws SQLException {
      return h2ConnectionMetaData.getDefaultTransactionIsolation();
   }

   public boolean supportsTransactions() throws SQLException {
      return h2ConnectionMetaData.supportsTransactions();
   }

   public boolean supportsTransactionIsolationLevel(int level) throws
           SQLException {
      return h2ConnectionMetaData.supportsTransactionIsolationLevel(level);
   }

   public boolean supportsDataDefinitionAndDataManipulationTransactions() throws
           SQLException {
      return h2ConnectionMetaData.
              supportsDataDefinitionAndDataManipulationTransactions();
   }

   public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
      return h2ConnectionMetaData.supportsDataManipulationTransactionsOnly();
   }

   public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
      return h2ConnectionMetaData.dataDefinitionCausesTransactionCommit();
   }

   public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
      return h2ConnectionMetaData.dataDefinitionIgnoredInTransactions();
   }

   public ResultSet getProcedures(String catalog, String schemaPattern,
                                  String procedureNamePattern) throws
           SQLException {
      return h2ConnectionMetaData.getProcedures(catalog, schemaPattern,
              procedureNamePattern);
   }

   public ResultSet getProcedureColumns(String catalog, String schemaPattern,
                                        String procedureNamePattern,
                                        String columnNamePattern) throws
           SQLException {
      return h2ConnectionMetaData.getProcedureColumns(catalog, schemaPattern,
              procedureNamePattern,
              columnNamePattern);
   }

   // Retrieves the schema names available in this database.
   // The results are ordered by schema name.
   // to override
   public ResultSet getSchemas() throws SQLException {
      OtherUtils.writeLogInfo(log,
              "DatabaseMetaData -> getSchemas()");

      SimpleResultSet simpleResultSet = new SimpleResultSet();
      simpleResultSet.addColumn("TABLE_SCHEM", Types.VARCHAR, 255, 0);
      simpleResultSet.addColumn("TABLE_CATALOG", Types.VARCHAR, 255, 0);
      simpleResultSet.addRow(getSchemaName(), conn.getCatalog());
      return simpleResultSet;

      /*
         SQLTable t = null;
         try {
           Columns cols = new Columns();
           cols.add(new Column("TABLE_SCHEM"));
           cols.add(new Column("TABLE_CATALOG"));
           t = new SQLTable(cols);
 //      t.insert(cols, new Object[] {conn.getSchema().getName(), conn.getCatalog()});
           t.insert(cols, new Object[] {getSchemaName(), conn.getCatalog()});
         } catch (Exception ex) {
           return null;
         }
         return t.getResultSet();
      */
   }

   // Retrieves the catalog names available in this database.
   // The results are ordered by catalog name.
   // to override
   public ResultSet getCatalogs() throws SQLException {
      OtherUtils.writeLogInfo(log,
              "DatabaseMetaData -> getCatalogs()");

      SimpleResultSet simpleResultSet = new SimpleResultSet();
      simpleResultSet.addColumn("TABLE_CAT", Types.VARCHAR, 255, 0);
      simpleResultSet.addRow(conn.getCatalog());
      return simpleResultSet;

      /*
         SQLTable t = null;
         try {
           Columns cols = new Columns();
           cols.add(new Column("TABLE_CAT"));
           t = new SQLTable(cols);
           t.insert(cols, new Object[] {conn.getCatalog()});
         } catch (Exception ex) {
           return null;
         }
         return t.getResultSet();
      */
   }

   // Retrieves the table types available in this database.
   // The results are ordered by table type.
   public ResultSet getTableTypes() throws SQLException {
      SimpleResultSet simpleResultSet = new SimpleResultSet();
      simpleResultSet.addColumn("TABLE_TYPE", Types.VARCHAR, 255, 0);
      simpleResultSet.addRow("TABLE");

      if (conn.getSchemaIF2().getViewSchema() != null)
         simpleResultSet.addRow("VIEW");

      return simpleResultSet;
//    return h2ConnectionMetaData.getTableTypes();
   }

   // to override
   public synchronized ResultSet getTables(String catalog, String schemaPattern,
                                           String tableNamePattern,
                                           String[] types) throws
           SQLException {
      OtherUtils.writeLogInfo(log,
              "DatabaseMetaData -> getTables(catalog = " +
                      catalog +
                      ", schemaPattern = " + schemaPattern +
                      ", tableNamePattern = " + tableNamePattern +
                      ", types = " + Arrays.toString(types) +
                      ")");

      Statement st = conn.getH2Connection().createStatement();

      st.execute("CREATE MEMORY TABLE IF NOT EXISTS RJ_SCHEMA.RJ_TABLES(" +
              "TABLE_CAT VARCHAR(255)," +
              "TABLE_SCHEM VARCHAR(255)," +
              "TABLE_NAME VARCHAR(255)," +
              "TABLE_TYPE VARCHAR(255)," +
              "REMARKS VARCHAR(255)," +
              "TYPE_CAT VARCHAR(255)," +
              "TYPE_SCHEM VARCHAR(255)," +
              "TYPE_NAME VARCHAR(255)," +
              "SELF_REFERENCING_COL_NAME VARCHAR(255)," +
              "REF_GENERATION VARCHAR(255)," +
              "PRIMARY KEY (TABLE_NAME))"
      );

      st.execute("DELETE FROM RJ_SCHEMA.RJ_TABLES");

      PreparedStatement pstInsert = conn.getH2Connection().prepareStatement(
              "INSERT INTO RJ_SCHEMA.RJ_TABLES VALUES(?,?,?,?,?,?,?,?,?,?)");

      // add tables externally stored
      try {
         StoreTableIF[] tables = conn.getSchemaIF2().getStoreTables(tableNamePattern);
         for (StoreTableIF table : tables) {
            pstInsert.setString(1, conn.getCatalog());
            pstInsert.setString(2, getSchemaName());
            // table names
            pstInsert.setString(3, getTableIdentifierString(StringUtils.
                    getFileNameWithoutExtension(table.getName())));
            pstInsert.setString(4, "TABLE");
            pstInsert.setNull(5, Types.VARCHAR);
            pstInsert.setNull(6, Types.VARCHAR);
            pstInsert.setNull(7, Types.VARCHAR);
            pstInsert.setNull(8, Types.VARCHAR);
            pstInsert.setNull(9, Types.VARCHAR);
            pstInsert.setNull(10, Types.VARCHAR);
            try {
               pstInsert.execute();
            } catch (SQLException ex) {
               log.warn(ex.getMessage(), ex);
            }
         }
      } catch (Exception ex) {
         throw new SQLException(ex.getMessage());
      }

      // add views externally stored
      ViewSchemaIF viewSchema = conn.getSchemaIF2().getViewSchema();

      if (viewSchema != null) {
         ViewTableIF[] views = viewSchema.getStoreViews();

         for (ViewTableIF view : views) {
            pstInsert.setString(1, conn.getCatalog());
            pstInsert.setString(2, getSchemaName());
            pstInsert.setString(3, getTableIdentifierString(view.getName()));
            pstInsert.setString(4, "VIEW");
            pstInsert.setNull(5, Types.VARCHAR);
            pstInsert.setNull(6, Types.VARCHAR);
            pstInsert.setNull(7, Types.VARCHAR);
            pstInsert.setNull(8, Types.VARCHAR);
            pstInsert.setNull(9, Types.VARCHAR);
            pstInsert.setNull(10, Types.VARCHAR);
            try {
               pstInsert.execute();
            } catch (SQLException ex) {
                log.warn(ex.getMessage(), ex);
            }
         }
      }

      // add views already loaded to H2
      ResultSet rsViewLoaded = h2ConnectionMetaData.getTables(conn.getCatalog(), null, null,
              new String[]{"VIEW"});

      int columnCount = pstInsert.getParameterMetaData().getParameterCount();

      while (rsViewLoaded.next()) {
         String viewLoadedName = rsViewLoaded.getString("TABLE_NAME");

         // if view is already added
         if (viewSchema != null && viewSchema.getViewByName(viewLoadedName) != null)
            continue;

         for (int i = 0; i < columnCount; i++)
            pstInsert.setObject(i + 1, rsViewLoaded.getObject(i + 1));

         try {
            pstInsert.execute();
         } catch (SQLException ex) {
            log.warn(ex.getMessage(), ex);
         }
      }

      rsViewLoaded.close();

      String tableType;
      if (types != null && types.length > 0) {
         
         StringBuffer buff = new StringBuffer("TABLE_TYPE IN(");
         String separator = "";
         for (String type : types) {
            buff.append(separator);
            buff.append("'").append(type).append("'");
            separator = ", ";
         }
         tableType = buff.append(')').toString();
      } else {
         tableType = "TRUE";
      }

      PreparedStatement pstResult = conn.getH2Connection().prepareStatement(
              "SELECT * FROM RJ_SCHEMA.RJ_TABLES WHERE " +
                      "TABLE_NAME LIKE ? ESCAPE '' AND " + tableType +
                      " ORDER BY TABLE_TYPE, TABLE_NAME");

      pstResult.setString(1,
              (tableNamePattern == null ||
                      tableNamePattern.trim().isEmpty()) ?
                      "%" :
                      StringUtils.getFileNameWithoutExtension(
                              tableNamePattern));
      ResultSet rsTables = pstResult.executeQuery();

      SimpleResultSet simpleResultSet = createSimpleResultSet(rsTables);

      rsTables.close();
      st.close();
      pstInsert.close();
      pstResult.close();

      if (log.isDebugEnabled())
         log.debug("DatabaseMetaData.getTables(). Returning tables.");

      return simpleResultSet;

      /*
         // add tables externally stored
         if (types == null ||
      types.length == 0 ||
      com.relationaljunction.utils.OtherUtils.containsStringIgnoringCase(types, "TABLE") != -1) {
           try {
      StoreTableIF[] tables = conn.getSchema().getStoreTables(tableNamePattern);
      for (StoreTableIF table : tables) {
        simpleResultSet.addRow(new Object[] {conn.getCatalog(), getSchemaName(),
          CacheTable.quoteName(com.relationaljunction.utils.StringUtils.
            getFileNameWithoutExtension(
         table.getName()), CacheTable.SQL_RESERVED_CHARS),
          new String("TABLE"), null, null, null, null, null, null});
      }
           } catch (Exception ex) {
      return null;
           }
         }

         // add views externally stored
         if (types == null ||
      types.length == 0 ||
      com.relationaljunction.utils.OtherUtils.containsStringIgnoringCase(types, "VIEW") != -1) {

           ViewSchemaIF viewSchema = conn.getSchema().getViewSchema();

           if (viewSchema != null) {
      ViewTableIF[] views = viewSchema.getStoreViews();

      for (ViewTableIF view : views) {
        simpleResultSet.addRow(new Object[] {conn.getCatalog(), getSchemaName(),
          CacheTable.quoteName(view.getName(),
            CacheTable.SQL_RESERVED_CHARS),
          new String("VIEW"), null, null, null, null, null, null});
      }
           }

           // add views already loaded to H2
           ResultSet rsViewLoaded = h2ConnectionMetaData.getTables(conn.getCatalog(), null, null,
        new String[] {"VIEW"});
           ResultSetMetaData rsViewLoadedMetaData = rsViewLoaded.getMetaData();

           while (rsViewLoaded.next()) {
      String viewLoadedName = rsViewLoaded.getString("TABLE_NAME");

 // if view is already added
      if (viewSchema != null && viewSchema.getViewByName(viewLoadedName) != null)
        continue;

      Object[] objs = new Object[rsViewLoadedMetaData.getColumnCount()];
      for (int i = 0; i < objs.length; i++)
        objs[i] = rsViewLoaded.getObject(i + 1);

      simpleResultSet.addRow(objs);
           }
         }

         return simpleResultSet;
      */
   }

   // Retrieves a description of table columns available in the specified catalog.
   // to override
   public synchronized ResultSet getColumns(String catalog, String schemaPattern,
                                            String tableNamePattern,
                                            String columnNamePattern) throws
           SQLException {
      OtherUtils.writeLogInfo(log,
              "DatabaseMetaData -> getColumns(catalog = " +
                      catalog +
                      ", schemaPattern = " + schemaPattern +
                      ", tableNamePattern = " + tableNamePattern +
                      ", columnNamePattern = " + columnNamePattern +
                      ")");

      Statement st = conn.getH2Connection().createStatement();

      st.execute(
              "CREATE MEMORY TABLE IF NOT EXISTS RJ_SCHEMA.RJ_COLUMNS(" +
                      "TABLE_CAT VARCHAR(255)," +
                      "TABLE_SCHEM VARCHAR(255)," +
                      "TABLE_NAME VARCHAR(255)," +
                      "COLUMN_NAME VARCHAR(255)," +
                      "DATA_TYPE INTEGER," +
                      "TYPE_NAME VARCHAR(255)," +

                      "COLUMN_SIZE INTEGER," +
                      "BUFFER_LENGTH INTEGER," +
                      "DECIMAL_DIGITS INTEGER," +
                      "NUM_PREC_RADIX INTEGER," +
                      "NULLABLE INTEGER," +
                      "REMARKS VARCHAR(255)," +
                      "COLUMN_DEF VARCHAR(255)," +
                      "SQL_DATA_TYPE INTEGER," +
                      "SQL_DATETIME_SUB INTEGER," +
                      "CHAR_OCTET_LENGTH INTEGER," +
                      "ORDINAL_POSITION INTEGER," +
                      "IS_NULLABLE VARCHAR(255)," +
                      "SCOPE_CATALOG VARCHAR(255)," +
                      "SCOPE_SCHEMA VARCHAR(255)," +
                      "SCOPE_TABLE VARCHAR(255)," +
                      "SOURCE_DATA_TYPE INTEGER," +
                      "PRIMARY KEY (TABLE_NAME, COLUMN_NAME))"
      );

      st.execute("DELETE FROM RJ_SCHEMA.RJ_COLUMNS");

      PreparedStatement pstInsert = conn.getH2Connection().prepareStatement(
              "INSERT INTO RJ_SCHEMA.RJ_COLUMNS " +
                      "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

      try {
         StoreTableIF[] tables = conn.getSchemaIF2().getStoreTables(tableNamePattern);
         for (StoreTableIF table : tables) {
            StoreTableReaderIF reader;

            try {
               reader = table.getReader();
            } catch (StoreException e) {
               e.printStackTrace();
               log.warn("[MetaData] Can't read the table '" + table.getName() + "'", e);
               continue;
            }

            StoreFieldIF[] allFields = reader.getFields();

            for (int i = 0; i < allFields.length; i++) {
               pstInsert.setString(1, conn.getCatalog());
               pstInsert.setString(2, getSchemaName());
               // table names
               pstInsert.setString(3, getTableIdentifierString(StringUtils.
                       getFileNameWithoutExtension(table.getName())));
               // column names
               pstInsert.setString(4, getColumnIdentifierString(allFields[i].getName()));

               pstInsert.setInt(5, allFields[i].getType().getJdbcType());
               pstInsert.setString(6, allFields[i].getType().getName());
               pstInsert.setInt(7, allFields[i].getLength());
               pstInsert.setNull(8, Types.VARCHAR);
               pstInsert.setInt(9, allFields[i].getDecimalCount());
               pstInsert.setInt(10, 2);
               pstInsert.setInt(11, ResultSetMetaData.columnNullable);
               pstInsert.setNull(12, Types.VARCHAR);
               pstInsert.setNull(13, Types.VARCHAR);
               pstInsert.setNull(14, Types.VARCHAR);
               pstInsert.setNull(15, Types.VARCHAR);
               pstInsert.setNull(16, Types.VARCHAR);
               pstInsert.setInt(17, (i + 1));
               pstInsert.setString(18, "YES");
               pstInsert.setNull(19, Types.VARCHAR);
               pstInsert.setNull(20, Types.VARCHAR);
               pstInsert.setNull(21, Types.VARCHAR);
               pstInsert.setNull(22, Types.VARCHAR);

               try {
                  pstInsert.execute();
               } catch (SQLException ex) {
                  ex.printStackTrace();
               }

            } // for fields

            reader.close();
         } // for tables
      } catch (Exception ex) {
         ex.printStackTrace();
         return null;
      }

//      try {
//         TestUtils.printQueryOut(st, "SELECT * FROM RJ_SCHEMA.RJ_COLUMNS", System.out);
//      } catch (SQLException e) {
//         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//      }


      PreparedStatement pstResult = conn.getH2Connection().prepareStatement(
              "SELECT * FROM RJ_SCHEMA.RJ_COLUMNS WHERE " +
                      "TABLE_NAME LIKE ? ESCAPE '' AND " +
                      "COLUMN_NAME LIKE ? ESCAPE '' " +
                      " ORDER BY TABLE_SCHEM, TABLE_NAME, ORDINAL_POSITION");

      pstResult.setString(1,
              (tableNamePattern == null ||
                      tableNamePattern.trim().isEmpty()) ?
                      "%" :
                      StringUtils.getFileNameWithoutExtension(
                              tableNamePattern));
      pstResult.setString(2,
              (columnNamePattern == null ||
                      columnNamePattern.trim().isEmpty()) ?
                      "%" : columnNamePattern);

      ResultSet rsTables = pstResult.executeQuery();

//      try {
//         TestUtils.printColumnsOut(rsTables, System.out);
//         TestUtils.printResultSetOut(rsTables, System.out);
//      } catch (SQLException e) {
//         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//      }


      SimpleResultSet simpleResultSet = createSimpleResultSet(rsTables);

      rsTables.close();
      st.close();
      pstInsert.close();
      pstResult.close();

      return simpleResultSet;
      /*
         try{
           StoreTableReaderIF reader = conn.getSchema().getStoreTable(conn.
        getCacheTableManager().getFileTableName(tableNamePattern)).getReader();

         StoreFieldIF[] fields = null;

         StoreFieldIF[] allFields = reader.getFields();

         if (columnNamePattern != null && !columnNamePattern.equals("")) {
           java.util.Vector patternFields = new java.util.Vector();
           for (int i = 0; i < allFields.length; i++) {
      if (com.relationaljunction.utils.StringUtils.isLike(allFields[i].getName().toUpperCase(),
              columnNamePattern.toUpperCase(),
              '%', '_'))
        patternFields.add(allFields[i]);
           }
           fields = new StoreFieldIF[patternFields.size()];
           for (int i = 0; i < patternFields.size(); i++)
      fields[i] = (StoreFieldIF) patternFields.get(i);
         } else
           fields = allFields;

         for (int i = 0; i < fields.length; i++)
      simpleResultSet.addRow(new Object[] {conn.getCatalog(), getSchemaName(),
             tableNamePattern, fields[i].getName(),
             fields[i].getSQLType(), fields[i].getSQLTypeName(),
             fields[i].getLength(), null, fields[i].getDecimalCount(),
             2, ResultSetMetaData.columnNullable, null, null, null, null, null,
             i + 1, "YES", null, null, null, null});
         reader.close();
       } catch (Exception ex) {
         ex.printStackTrace();
         return null;
       }

         return simpleResultSet;
      */
   }

   public ResultSet getColumnPrivileges(String catalog, String schema,
                                        String table, String columnNamePattern) throws
           SQLException {
      return h2ConnectionMetaData.getColumnPrivileges(catalog, schema,
              table, columnNamePattern);
   }

   public ResultSet getTablePrivileges(String catalog, String schemaPattern,
                                       String tableNamePattern) throws
           SQLException {
      return h2ConnectionMetaData.getTablePrivileges(catalog, schemaPattern,
              tableNamePattern);
   }

   public ResultSet getBestRowIdentifier(String catalog, String schema,
                                         String table, int scope,
                                         boolean nullable) throws SQLException {
      return h2ConnectionMetaData.getBestRowIdentifier(catalog, schema, table,
              scope, nullable);
   }

   public ResultSet getVersionColumns(String catalog, String schema,
                                      String table) throws SQLException {
      // always empty
      return h2ConnectionMetaData.getVersionColumns(catalog, schema, table);
   }

   // should be overrided for StelsMDB
   public synchronized ResultSet getPrimaryKeys(String catalog, String schema,
                                                String table) throws
           SQLException {
      OtherUtils.writeLogInfo(log,
              "DatabaseMetaData -> getPrimaryKeys(catalog = " +
                      catalog +
                      ", schema = " + schema +
                      ", table = " + table +
                      ")");

      // load tables preliminarily to the cache to get index data available,
      // e.g. for StelsCSV, StelsXML indexes described in schema.
      loadTable(table);

      Statement st = conn.getH2Connection().createStatement();


      st.execute(
              "CREATE MEMORY TABLE IF NOT EXISTS RJ_SCHEMA.RJ_PRIMARY_KEYS(" +
                      "TABLE_CAT VARCHAR(255)," +
                      "TABLE_SCHEM VARCHAR(255)," +
                      "TABLE_NAME VARCHAR(255)," +
                      "COLUMN_NAME VARCHAR(255)," +
                      "KEY_SEQ INTEGER," +
                      "PK_NAME VARCHAR(255)," +
                      "PRIMARY KEY (PK_NAME, COLUMN_NAME))"
      );

      st.execute("DELETE FROM RJ_SCHEMA.RJ_PRIMARY_KEYS");

      PreparedStatement pstInsert = conn.getH2Connection().prepareStatement(
              "INSERT INTO RJ_SCHEMA.RJ_PRIMARY_KEYS VALUES(?,?,?,?,?,?)");

      // add external indexes
      try {
         IndexSchemaIF indexSchema = conn.getSchemaIF2().getIndexSchema();

         if (indexSchema != null) {
            IndexTableIF[] indexTables = indexSchema.getStoreIndexes(table);

            for (IndexTableIF indexTable : indexTables) {
               if (!indexTable.isPrimaryKey())
                  continue;

               int pos = 1;
               for (IndexFieldIF indexField : indexTable.getIndexFields()) {
                  pstInsert.setString(1, conn.getCatalog());
                  pstInsert.setString(2, getSchemaName());
                  // table names
                  pstInsert.setString(3, getTableIdentifierString(StringUtils.
                          getFileNameWithoutExtension(table)));
                  // column names
                  pstInsert.setString(4, getColumnIdentifierString(indexField.getStoreField().getName()));

                  pstInsert.setInt(5, pos);
                  pstInsert.setString(6, indexTable.getIndexName());

                  try {
                     pstInsert.execute();
                  } catch (SQLException ex) {
                     log.warn(ex.getMessage(), ex);
                  }

                  pos++;
               }
            }

         } else {
//          ResultSet rs = h2ConnectionMetaData.getPrimaryKeys("","","test_pm.dbf");
//          ResultSet rs = st.executeQuery("SELECT * FROM INFORMATION_SCHEMA.INDEXES");
//	  com.relationaljunction.utils.TestUtils.printColumnsOut(rs,System.out);
//          com.relationaljunction.utils.TestUtils.printResultSetOut(rs, System.out);

            // add indexes already loaded to H2
            // InformationSchema in H2 are case sensetive!!!
            ResultSet rsIndexLoaded = h2ConnectionMetaData.getPrimaryKeys(catalog,
                    schema,
                    CacheTableManager.getCacheTableName(table));
            addNonUniqueResultSet(pstInsert, rsIndexLoaded);
            rsIndexLoaded.close();
         }
      } catch (Exception ex) {
         throw new SQLException(ex.getMessage());
      }

      ResultSet rsTables = st.executeQuery(
              "SELECT * FROM RJ_SCHEMA.RJ_PRIMARY_KEYS ORDER BY COLUMN_NAME");

      SimpleResultSet simpleResultSet = createSimpleResultSet(rsTables);

      rsTables.close();
      st.close();
      pstInsert.close();

      return simpleResultSet;

      /*
         Columns cols = new Columns();
         cols.add(new Column("TABLE_CAT"));
         cols.add(new Column("TABLE_SCHEM"));
         cols.add(new Column("TABLE_NAME"));
         cols.add(new Column("COLUMN_NAME"));
         cols.add(new Column("KEY_SEQ"));
         cols.add(new Column("PK_NAME"));
         SQLTable t = new SQLTable(cols);

         IndexTableIF[] indexTables = conn.getSchema().getIndexSchema().
          getStoreIndexes(table);

         if (indexTables == null)throw new Exception("Table '" + table +
           "' does not exist!");


           for (int i = 0; i < indexTables.length; i++) {
      IndexTableIF indexTable = indexTables[i];
      if (!indexTable.isPrimaryKey())continue;

      int pos = 1;
      for (IndexFieldIF indexField : indexTable.getIndexFields()) {
        t.insert(cols, new Object[] {conn.getCatalog(),
          conn.getSchema().getName(),
          table, indexField.getStoreField().getName(), pos,
          indexTable.getIndexName()});
        pos++;
      }
           }

         return t.getResultSet();
       }
       catch (Exception ex) {
         throw new SQLException(ex.getMessage());
       }
      */
   }


   /**
    * loads tables preliminarily to the cache to get index data available,
    * e.g. for StelsCSV, StelsXML indexes described in the schema.
    *
    * @param table
    * @throws SQLException
    */
   private void loadTable(String table) throws SQLException {
      if (conn.getSchemaIF2().requiresLoadingTablesInMetaData()) {
         if (log.isDebugEnabled()) log.debug("loading table '" + table + "' in the MetaData interface");

         Statement st = conn.createStatement();
         st.executeQuery("SELECT * FROM " + StringUtils.quoteReservedFieldAndTableName(table)
                 + " WHERE 1=0");
         st.close();
      }
   }


   public synchronized ResultSet getImportedKeys(String catalog, String schema,
                                                 String table) throws
           SQLException {
      OtherUtils.writeLogInfo(log,
              "DatabaseMetaData -> getImportedKeys(catalog = " +
                      catalog +
                      ", schema = " + schema +
                      ", table = " + table +
                      ")");

      Statement st = conn.getH2Connection().createStatement();

      st.execute(
              "CREATE MEMORY TABLE IF NOT EXISTS RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO(" +
                      "PKTABLE_CAT VARCHAR(255)," +
                      "PKTABLE_SCHEM VARCHAR(255)," +
                      "PKTABLE_NAME VARCHAR(255)," +
                      "PKCOLUMN_NAME VARCHAR(255)," +
                      "FKTABLE_CAT VARCHAR(255)," +
                      "FKTABLE_SCHEM VARCHAR(255)," +
                      "FKTABLE_NAME VARCHAR(255)," +
                      "FKCOLUMN_NAME VARCHAR(255)," +
                      "KEY_SEQ SMALLINT," +
                      "UPDATE_RULE SMALLINT," +
                      "DELETE_RULE SMALLINT," +
                      "FK_NAME VARCHAR(255)," +
                      "PK_NAME VARCHAR(255)," +
                      "DEFERRABILITY SMALLINT," +
                      "PRIMARY KEY (PKTABLE_NAME, PKCOLUMN_NAME, FKTABLE_NAME, FKCOLUMN_NAME))"
      );

      st.execute("DELETE FROM RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO");

      PreparedStatement pstInsert = conn.getH2Connection().prepareStatement(
              "INSERT INTO RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

      try {
         IndexSchemaIF indexSchema = conn.getSchemaIF2().getIndexSchema();

         if (indexSchema != null) {
            // add native external indexes
            IndexTableIF[] indexTables = indexSchema.getStoreIndexes(table);

            for (IndexTableIF indexTable : indexTables) {
               if (!indexTable.isForeignKey() || indexTable.isUnique())
                  continue;

               pstInsert.setString(1, conn.getCatalog());
               pstInsert.setString(2, getSchemaName());
               pstInsert.setString(3, getTableIdentifierString(
                       indexTable.getReferencedIndex().getIndexedTable()));
               pstInsert.setString(5, conn.getCatalog());
               pstInsert.setString(6, getSchemaName());
               pstInsert.setString(7, getTableIdentifierString(indexTable.getIndexedTable()));
               pstInsert.setInt(10, importedKeyRestrict);
               pstInsert.setInt(11, importedKeyRestrict);
               pstInsert.setString(12, indexTable.getReferencedIndex().getIndexName());
               pstInsert.setString(13,
                       indexTable.getIndexName());
               pstInsert.setInt(14, importedKeyNotDeferrable);

               for (int j = 0; j < indexTable.getIndexFields().length; j++) {
                  pstInsert.setString(4,
                          getColumnIdentifierString(indexTable.getReferencedIndex().
                                  getIndexFields()[j].getStoreField().getName()));
                  pstInsert.setString(8, getColumnIdentifierString(indexTable.getIndexFields()[j].
                          getStoreField().getName()));
                  pstInsert.setInt(9, j + 1);
                  try {
                     pstInsert.execute();
                  } catch (SQLException ex) {
                     log.warn(ex.getMessage(), ex);
                  }

               }
            }

            // add additional data from the relationship table (this data is required for linked tables in StelsMDB)
            // some data may be duplicated regarding data above and it may cause primary key violation.
            // but it is OK.
            addRelationships(pstInsert, indexSchema);
         } else {
            // add internal indexes created in H2 directly
            return h2ConnectionMetaData.getImportedKeys(catalog, schema,
                    CacheTableManager.getCacheTableName(table));
         }
      } catch (Exception ex) {
         throw new SQLException(ex.getMessage());
      }

//    ResultSet rs = st.executeQuery(
//        "SELECT * FROM RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO");
//    com.relationaljunction.utils.TestUtils.printColumnsOut(rs, System.out);
//    com.relationaljunction.utils.TestUtils.printResultSetOut(rs, System.out);

      PreparedStatement pstQuery = conn.getH2Connection().prepareStatement("SELECT * FROM " +
              "RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO WHERE FKTABLE_NAME=? " +
              "ORDER BY PKTABLE_CAT, PKTABLE_SCHEM, PKTABLE_NAME, FK_NAME, KEY_SEQ");

      pstQuery.setString(1, getTableIdentifierString(table));

      ResultSet rsTables = pstQuery.executeQuery();

      SimpleResultSet simpleResultSet = createSimpleResultSet(rsTables);

      rsTables.close();
      st.close();
      pstQuery.close();
      pstInsert.close();

      return simpleResultSet;
   }


   public synchronized ResultSet getExportedKeys(String catalog, String schema,
                                                 String table) throws
           SQLException {
      OtherUtils.writeLogInfo(log,
              "DatabaseMetaData -> getExportedKeys(catalog = " +
                      catalog +
                      ", schema = " + schema +
                      ", table = " + table +
                      ")");

      Statement st = conn.getH2Connection().createStatement();

      st.execute(
              "CREATE MEMORY TABLE IF NOT EXISTS RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO(" +
                      "PKTABLE_CAT VARCHAR(255)," +
                      "PKTABLE_SCHEM VARCHAR(255)," +
                      "PKTABLE_NAME VARCHAR(255)," +
                      "PKCOLUMN_NAME VARCHAR(255)," +
                      "FKTABLE_CAT VARCHAR(255)," +
                      "FKTABLE_SCHEM VARCHAR(255)," +
                      "FKTABLE_NAME VARCHAR(255)," +
                      "FKCOLUMN_NAME VARCHAR(255)," +
                      "KEY_SEQ SMALLINT," +
                      "UPDATE_RULE SMALLINT," +
                      "DELETE_RULE SMALLINT," +
                      "FK_NAME VARCHAR(255)," +
                      "PK_NAME VARCHAR(255)," +
                      "DEFERRABILITY SMALLINT," +
                      "PRIMARY KEY (PKTABLE_NAME, PKCOLUMN_NAME, FKTABLE_NAME, FKCOLUMN_NAME))"
      );

      st.execute("DELETE FROM RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO");

      PreparedStatement pstInsert = conn.getH2Connection().prepareStatement(
              "INSERT INTO RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

      try {
         IndexSchemaIF indexSchema = conn.getSchemaIF2().getIndexSchema();

         if (indexSchema != null) {
            // add native external indexes
            IndexTableIF[] indexTables = indexSchema.getStoreIndexes(table);

            for (IndexTableIF indexTable : indexTables) {
               if (!indexTable.isForeignKey() || !indexTable.isUnique())
                  continue;

               pstInsert.setString(1, conn.getCatalog());
               pstInsert.setString(2, getSchemaName());
               pstInsert.setString(3, getTableIdentifierString(indexTable.getIndexedTable()));
               pstInsert.setString(5, conn.getCatalog());
               pstInsert.setString(6, getSchemaName());
               pstInsert.setString(7, getTableIdentifierString(
                       indexTable.getReferencedIndex().getIndexedTable()));
               pstInsert.setInt(10, importedKeyRestrict);
               pstInsert.setInt(11, importedKeyRestrict);
               pstInsert.setString(12, indexTable.getIndexName());
               pstInsert.setString(13,
                       indexTable.getReferencedIndex().getIndexName());
               pstInsert.setInt(14, importedKeyNotDeferrable);

               for (int j = 0; j < indexTable.getIndexFields().length; j++) {
                  pstInsert.setString(4,
                          getColumnIdentifierString(indexTable.getIndexFields()[j].getStoreField().
                                  getName()));
                  pstInsert.setString(8, getColumnIdentifierString(indexTable.getReferencedIndex()
                          .getIndexFields()[j].getStoreField().getName()));
                  pstInsert.setInt(9, j + 1);
                  try {
                     pstInsert.execute();
                  } catch (SQLException ex) {
                     log.warn(ex.getMessage(), ex);
                  }
               }
            }

            // add additional data from the relationship table (this data is required for linked tables in StelsMDB)
            // some data may be duplicated regarding data above and it may cause primary key violation.
            // but it is OK.
            addRelationships(pstInsert, indexSchema);
         } else {
            // add internal indexes created in H2 directly
            return h2ConnectionMetaData.getExportedKeys(catalog, schema,
                    CacheTableManager.
                            getCacheTableName(table));
         }
      } catch (Exception ex) {
         throw new SQLException(ex.getMessage());
      }

      PreparedStatement pstQuery = conn.getH2Connection().prepareStatement("SELECT * FROM " +
              "RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO WHERE PKTABLE_NAME=? " +
              "ORDER BY FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME, FK_NAME, KEY_SEQ");

      pstQuery.setString(1,
              getTableIdentifierString(table));

      ResultSet rsTables = pstQuery.executeQuery();

      SimpleResultSet simpleResultSet = createSimpleResultSet(rsTables);

      rsTables.close();
      st.close();
      pstQuery.close();
      pstInsert.close();

      return simpleResultSet;
   }

   /*
   public synchronized ResultSet getImportedKeys(String catalog, String schema,
        String table) throws
        SQLException {
      OtherUtils.writeLogInfo(log,
         "DatabaseMetaData -> getImportedKeys(catalog = " +
         catalog +
         ", schema = " + schema +
         ", table = " + table +
         ")", true);

      Statement st = conn.getH2Connection().createStatement();

      st.execute(
   "CREATE MEMORY TABLE IF NOT EXISTS RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO(" +
   "PKTABLE_CAT VARCHAR(255)," +
   "PKTABLE_SCHEM VARCHAR(255)," +
   "PKTABLE_NAME VARCHAR(255)," +
   "PKCOLUMN_NAME VARCHAR(255)," +
   "FKTABLE_CAT VARCHAR(255)," +
   "FKTABLE_SCHEM VARCHAR(255)," +
   "FKTABLE_NAME VARCHAR(255)," +
   "FKCOLUMN_NAME VARCHAR(255)," +
   "KEY_SEQ SMALLINT," +
   "UPDATE_RULE SMALLINT," +
   "DELETE_RULE SMALLINT," +
   "FK_NAME VARCHAR(255)," +
   "PK_NAME VARCHAR(255)," +
   "DEFERRABILITY SMALLINT," +
   "PRIMARY KEY (PKTABLE_NAME, PKCOLUMN_NAME, FKTABLE_NAME, FKCOLUMN_NAME))"
   );

      st.execute("DELETE FROM RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO");

      PreparedStatement pstInsert = conn.getH2Connection().prepareStatement(
   "INSERT INTO RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

      // add external indexes
      try {
        IndexSchemaIF indexSchema = conn.getSchema().getIndexSchema();

        if (indexSchema != null) {
   addRelationships(pstInsert, indexSchema);
        }
        else {
   return h2ConnectionMetaData.getImportedKeys(catalog, schema,
            CacheTableManager.
            getCacheTableName(table));
        }
      } catch (Exception ex) {
        throw new SQLException(ex.getMessage());
      }

      PreparedStatement pstQuery = conn.getH2Connection().prepareStatement("SELECT * FROM RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO WHERE FKTABLE_NAME=? ORDER BY PKTABLE_CAT, PKTABLE_SCHEM, PKTABLE_NAME, FK_NAME, KEY_SEQ");

      pstQuery.setString(1,
      StringUtils.getMetaName(StringUtils.unquote(table)));

      ResultSet rsTables = pstQuery.executeQuery();

      SimpleResultSet simpleResultSet = createSimpleResultSet(rsTables);

      rsTables.close();
      st.close();
      pstQuery.close();
      pstInsert.close();

      return simpleResultSet;
    }
   */
   /*
   public synchronized ResultSet getExportedKeys(String catalog, String schema,
                                                  String table) throws
        SQLException {
      OtherUtils.writeLogInfo(log,
   "DatabaseMetaData -> getExportedKeys(catalog = " +
                              catalog +
                              ", schema = " + schema +
                              ", table = " + table +
                              ")", true);

      Statement st = conn.getH2Connection().createStatement();

      st.execute(
   "CREATE MEMORY TABLE IF NOT EXISTS RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO(" +
          "PKTABLE_CAT VARCHAR(255)," +
          "PKTABLE_SCHEM VARCHAR(255)," +
          "PKTABLE_NAME VARCHAR(255)," +
          "PKCOLUMN_NAME VARCHAR(255)," +
          "FKTABLE_CAT VARCHAR(255)," +
          "FKTABLE_SCHEM VARCHAR(255)," +
          "FKTABLE_NAME VARCHAR(255)," +
          "FKCOLUMN_NAME VARCHAR(255)," +
          "KEY_SEQ SMALLINT," +
          "UPDATE_RULE SMALLINT," +
          "DELETE_RULE SMALLINT," +
          "FK_NAME VARCHAR(255)," +
          "PK_NAME VARCHAR(255)," +
          "DEFERRABILITY SMALLINT," +
   "PRIMARY KEY (PKTABLE_NAME, PKCOLUMN_NAME, FKTABLE_NAME, FKCOLUMN_NAME))"
          );

      st.execute("DELETE FROM RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO");

      PreparedStatement pstInsert = conn.getH2Connection().prepareStatement(
          "INSERT INTO RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

      // add external indexes
      try {
        IndexSchemaIF indexSchema = conn.getSchema().getIndexSchema();

        if (indexSchema != null) {
          fillRelationshipsTable(pstInsert, indexSchema);
        }
        else {
          return h2ConnectionMetaData.getExportedKeys(catalog, schema,
                                                      CacheTableManager.
   getCacheTableName(table));
        }
      } catch (Exception ex) {
        throw new SQLException(ex.getMessage());
      }

      PreparedStatement pstQuery = conn.getH2Connection().prepareStatement("SELECT * FROM RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO WHERE PKTABLE_NAME=? ORDER BY FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME, FK_NAME, KEY_SEQ");

      pstQuery.setString(1,
   StringUtils.getMetaName(StringUtils.unquote(table)));

      ResultSet rsTables = pstQuery.executeQuery();

      SimpleResultSet simpleResultSet = createSimpleResultSet(rsTables);

      rsTables.close();
      st.close();
      pstQuery.close();
      pstInsert.close();

      return simpleResultSet;
    }
   */

   /*
    public synchronized ResultSet getCrossReference(String primaryCatalog,
                                                    String primarySchema,
                                                    String primaryTable,
                                                    String foreignCatalog,
                                                    String foreignSchema,
                                                    String foreignTable) throws
        SQLException {
      OtherUtils.writeLogInfo(log,
   "DatabaseMetaData -> getCrossReference(primaryCatalog = " +
                              primaryCatalog +
                              ", primarySchema = " + primarySchema +
                              ", primaryTable = " + primaryTable +
                              ", foreignCatalog = " + foreignCatalog +
                              ", foreignSchema = " + foreignSchema +
                              ", foreignTable = " + foreignTable +
                              ")", true);

      Statement st = conn.getH2Connection().createStatement();

      st.execute(
   "CREATE MEMORY TABLE IF NOT EXISTS RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO(" +
          "PKTABLE_CAT VARCHAR(255)," +
          "PKTABLE_SCHEM VARCHAR(255)," +
          "PKTABLE_NAME VARCHAR(255)," +
          "PKCOLUMN_NAME VARCHAR(255)," +
          "FKTABLE_CAT VARCHAR(255)," +
          "FKTABLE_SCHEM VARCHAR(255)," +
          "FKTABLE_NAME VARCHAR(255)," +
          "FKCOLUMN_NAME VARCHAR(255)," +
          "KEY_SEQ SMALLINT," +
          "UPDATE_RULE SMALLINT," +
          "DELETE_RULE SMALLINT," +
          "FK_NAME VARCHAR(255)," +
          "PK_NAME VARCHAR(255)," +
          "DEFERRABILITY SMALLINT," +
   "PRIMARY KEY (PKTABLE_NAME, PKCOLUMN_NAME, FKTABLE_NAME, FKCOLUMN_NAME))"
          );

      st.execute("DELETE FROM RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO");

      PreparedStatement pstInsert = conn.getH2Connection().prepareStatement(
          "INSERT INTO RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

      // add external indexes
      try {
        IndexSchemaIF indexSchema = conn.getSchema().getIndexSchema();

        if (indexSchema != null) {
          addRelationships(pstInsert, indexSchema);
        } else {
          // InformationSchema in H2 are case sensetive!!!
          return h2ConnectionMetaData.getCrossReference(primaryCatalog,
              primarySchema, CacheTableManager.getCacheTableName(primaryTable),
              foreignCatalog, foreignSchema,
              CacheTableManager.getCacheTableName(foreignTable));
        }
      } catch (Exception ex) {
        throw new SQLException(ex.getMessage());
      }

      PreparedStatement pstQuery = conn.getH2Connection().prepareStatement("SELECT * FROM RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO WHERE PKTABLE_NAME=? AND FKTABLE_NAME=? ORDER BY FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME, FK_NAME, KEY_SEQ");

      pstQuery.setString(1,
   StringUtils.getMetaName(StringUtils.unquote(
                             primaryTable)));
      pstQuery.setString(2,
   StringUtils.getMetaName(StringUtils.unquote(
                             foreignTable)));

      ResultSet rsTables = pstQuery.executeQuery();

      SimpleResultSet simpleResultSet = createSimpleResultSet(rsTables);

      rsTables.close();
      st.close();
      pstQuery.close();
      pstInsert.close();

      return simpleResultSet;
    }
   */

   private void addRelationships(PreparedStatement pstInsert,
                                 IndexSchemaIF indexSchema) throws
           SQLException, StoreException {
      TablesRelationship[] relationshipArray = indexSchema.getRelationships();

      for (int i = 0; i < relationshipArray.length; i++) {
         TablesRelationship relationship = relationshipArray[i];

         pstInsert.setString(1, conn.getCatalog());
         pstInsert.setString(2, getSchemaName());
         pstInsert.setString(3, getTableIdentifierString(relationship.getFromTable()));
         pstInsert.setString(4, getColumnIdentifierString(relationship.getFromColumn()));
         pstInsert.setString(5, conn.getCatalog());
         pstInsert.setString(6, getSchemaName());
         pstInsert.setString(7, getTableIdentifierString(relationship.getToTable()));
         pstInsert.setString(8, getColumnIdentifierString(relationship.getToColumn()));
         pstInsert.setInt(9, i + 1);
         pstInsert.setInt(10, importedKeyRestrict);
         pstInsert.setInt(11, importedKeyRestrict);
         pstInsert.setString(12, relationship.getFromRelationshipName());
         pstInsert.setString(13,
                 relationship.getToRelationshipName());
         pstInsert.setInt(14, importedKeyNotDeferrable);

         try {
            pstInsert.execute();
         } catch (SQLException ex) {
            log.warn(ex.getMessage(), ex);
         }
      }
   }


   public synchronized ResultSet getCrossReference(String primaryCatalog,
                                                   String primarySchema,
                                                   String primaryTable,
                                                   String foreignCatalog,
                                                   String foreignSchema,
                                                   String foreignTable) throws
           SQLException {
      OtherUtils.writeLogInfo(log,
              "DatabaseMetaData -> getCrossReference(primaryCatalog = " +
                      primaryCatalog +
                      ", primarySchema = " + primarySchema +
                      ", primaryTable = " + primaryTable +
                      ", foreignCatalog = " + foreignCatalog +
                      ", foreignSchema = " + foreignSchema +
                      ", foreignTable = " + foreignTable +
                      ")");

      Statement st = conn.getH2Connection().createStatement();

      st.execute(
              "CREATE MEMORY TABLE IF NOT EXISTS RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO(" +
                      "PKTABLE_CAT VARCHAR(255)," +
                      "PKTABLE_SCHEM VARCHAR(255)," +
                      "PKTABLE_NAME VARCHAR(255)," +
                      "PKCOLUMN_NAME VARCHAR(255)," +
                      "FKTABLE_CAT VARCHAR(255)," +
                      "FKTABLE_SCHEM VARCHAR(255)," +
                      "FKTABLE_NAME VARCHAR(255)," +
                      "FKCOLUMN_NAME VARCHAR(255)," +
                      "KEY_SEQ SMALLINT," +
                      "UPDATE_RULE SMALLINT," +
                      "DELETE_RULE SMALLINT," +
                      "FK_NAME VARCHAR(255)," +
                      "PK_NAME VARCHAR(255)," +
                      "DEFERRABILITY SMALLINT," +
                      "PRIMARY KEY (PKTABLE_NAME, PKCOLUMN_NAME, FKTABLE_NAME, FKCOLUMN_NAME))"
      );

      st.execute("DELETE FROM RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO");

      PreparedStatement pstInsert = conn.getH2Connection().prepareStatement(
              "INSERT INTO RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

      try {
         IndexSchemaIF indexSchema = conn.getSchemaIF2().getIndexSchema();

         if (indexSchema != null) {
            // add native external indexes
            IndexTableIF[] indexTables = indexSchema.getStoreIndexes(primaryTable);

            for (IndexTableIF indexTable : indexTables) {
               if (!indexTable.isForeignKey() || !indexTable.isUnique())
                  continue;

               if (!indexTable.getReferencedIndex().getIndexedTable().
                       equalsIgnoreCase(
                               foreignTable))
                  continue;

               pstInsert.setString(1, conn.getCatalog());
               pstInsert.setString(2, getSchemaName());
               pstInsert.setString(3, getTableIdentifierString(indexTable.getIndexedTable()));
               pstInsert.setString(5, conn.getCatalog());
               pstInsert.setString(6, getSchemaName());
               pstInsert.setString(7, getTableIdentifierString(indexTable.getReferencedIndex().
                       getIndexedTable()));
               pstInsert.setInt(10, importedKeyRestrict);
               pstInsert.setInt(11, importedKeyRestrict);
               pstInsert.setString(12, indexTable.getIndexName());
               pstInsert.setString(13,
                       indexTable.getReferencedIndex().getIndexName());
               pstInsert.setInt(14, importedKeyNotDeferrable);

               for (int j = 0; j < indexTable.getIndexFields().length; j++) {
                  pstInsert.setString(4, getColumnIdentifierString(indexTable.getIndexFields()[j].
                          getStoreField().getName()));
                  pstInsert.setString(8, getColumnIdentifierString(indexTable.getReferencedIndex().
                          getIndexFields()[j].getStoreField().getName()));
                  pstInsert.setInt(9, j + 1);
                  try {
                     pstInsert.execute();
                  } catch (SQLException ex) {
                     log.warn(ex.getMessage(), ex);
                  }
               }
            }

            // add additional data from the relationship table (this data is required for linked tables in StelsMDB)
            // some data may be duplicated regarding data above and it may cause primary key violation.
            // but it is OK.
            addRelationships(pstInsert, indexSchema);
         } else {
            // add internal indexes created in H2 directly

            // InformationSchema in H2 are case sensetive!!!
            return h2ConnectionMetaData.getCrossReference(primaryCatalog,
                    primarySchema,
                    CacheTableManager.
                            getCacheTableName(
                                    primaryTable),
                    foreignCatalog,
                    foreignSchema,
                    CacheTableManager.
                            getCacheTableName(
                                    foreignTable));
         }
      } catch (Exception ex) {
         throw new SQLException(ex.getMessage());
      }

      PreparedStatement pstQuery = conn.getH2Connection().prepareStatement("SELECT * FROM " +
              "RJ_SCHEMA.RJ_FOREIGN_KEYS_INFO WHERE PKTABLE_NAME=? " +
              "AND FKTABLE_NAME=? ORDER BY FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME, FK_NAME, KEY_SEQ");

      pstQuery.setString(1, getColumnIdentifierString(primaryTable));
      pstQuery.setString(2, getColumnIdentifierString(foreignTable));

      ResultSet rsTables = pstQuery.executeQuery();

      SimpleResultSet simpleResultSet = createSimpleResultSet(rsTables);

      rsTables.close();
      st.close();
      pstQuery.close();
      pstInsert.close();

      return simpleResultSet;
   }


   public ResultSet getTypeInfo() throws SQLException {
      PreparedStatement prep = conn.getH2Connection().prepareStatement("SELECT "
              + "TYPE_NAME, "
              + "DATA_TYPE, "
              + "PRECISION, "
              + "PREFIX LITERAL_PREFIX, "
              + "SUFFIX LITERAL_SUFFIX, "
              + "PARAMS CREATE_PARAMS, "
              + "NULLABLE, "
              + "CASE_SENSITIVE, "
              + "SEARCHABLE, "
              + "FALSE UNSIGNED_ATTRIBUTE, "
              + "FALSE FIXED_PREC_SCALE, "
              + "AUTO_INCREMENT, "
              + "TYPE_NAME LOCAL_TYPE_NAME, "
              + "MINIMUM_SCALE, "
              + "MAXIMUM_SCALE, "
              + "DATA_TYPE SQL_DATA_TYPE, "
              + "ZERO() SQL_DATETIME_SUB, "
              + "RADIX NUM_PREC_RADIX "
              + "FROM INFORMATION_SCHEMA.TYPE_INFO "
              +
              "WHERE TYPE_NAME='INTEGER' OR TYPE_NAME='BIGINT' OR TYPE_NAME='REAL' "
              +
              "OR TYPE_NAME='DOUBLE' OR TYPE_NAME='VARCHAR' OR TYPE_NAME='TIMESTAMP' "
              + "OR TYPE_NAME='DECIMAL' OR TYPE_NAME='BOOLEAN'"
              + "ORDER BY DATA_TYPE, POS");

      return prep.executeQuery();

      /*
         try {
           Columns cols = new Columns();
           cols.add(new Column("TYPE_NAME"));
           cols.add(new Column("DATA_TYPE"));
           cols.add(new Column("PRECISION"));
           cols.add(new Column("LITERAL_PREFIX"));
           cols.add(new Column("LITERAL_SUFFIX"));
           cols.add(new Column("CREATE_PARAMS"));
           cols.add(new Column("NULLABLE"));
           cols.add(new Column("CASE_SENSITIVE"));
           cols.add(new Column("SEARCHABLE"));
           cols.add(new Column("UNSIGNED_ATTRIBUTE"));
           cols.add(new Column("FIXED_PREC_SCALE"));
           cols.add(new Column("AUTO_INCREMENT"));
           cols.add(new Column("LOCAL_TYPE_NAME"));
           cols.add(new Column("MINIMUM_SCALE"));
           cols.add(new Column("MAXIMUM_SCALE"));
           cols.add(new Column("SQL_DATA_TYPE"));
           cols.add(new Column("SQL_DATETIME_SUB"));
           cols.add(new Column("NUM_PREC_RADIX"));
           SQLTable t = new SQLTable(cols);

           t.insert(cols, new Object[] {"INTEGER",
             "" + java.sql.Types.INTEGER,
             "10", null, null, null,
             "" + DatabaseMetaData.typeNullable,
             "false", "" + DatabaseMetaData.typeSearchable, "false", "false",
             "false",
             null, "0", "0", null, null, "2"});

           t.insert(cols, new Object[] {"BIGINT",
             "" + java.sql.Types.BIGINT,
             "19", null, null, null,
             "" + DatabaseMetaData.typeNullable,
             "false", "" + DatabaseMetaData.typeSearchable, "false", "false",
             "false",
             null, "0", "0", null, null, "2"});

           t.insert(cols, new Object[] {"FLOAT",
             "" + java.sql.Types.FLOAT,
             "8", null, null, null,
             "" + DatabaseMetaData.typeNullable,
             "false", "" + DatabaseMetaData.typeSearchable, "false", "false",
             "false",
             null, "0", "8", null, null, "2"});

           t.insert(cols, new Object[] {"DOUBLE",
             "" + java.sql.Types.DOUBLE,
             "17", null, null, null,
             "" + DatabaseMetaData.typeNullable,
             "false", "" + DatabaseMetaData.typeSearchable, "false", "false",
             "false",
             null, "0", "17", null, null, "2"});

           t.insert(cols, new Object[] {"DATETIME",
             "" + java.sql.Types.TIMESTAMP,
             "-1", null, null, null,
             "" + DatabaseMetaData.typeNullable,
             "false", "" + DatabaseMetaData.typeSearchable, "false", "false",
             "false",
             null, "0", "0", null, null, "2"});

           t.insert(cols, new Object[] {"STRING",
             "" + java.sql.Types.VARCHAR,
             "-1", null, null, null,
             "" + DatabaseMetaData.typeNullable,
             "false", "" + DatabaseMetaData.typeSearchable, "false", "false",
             "false",
             null, "0", "0", null, null, "2"});

           return t.getResultSet();
         } catch (Exception ex) {
           return null;
         }
      */
   }

   // should be overrided for StelsMDB
   public synchronized ResultSet getIndexInfo(String catalog, String schema,
                                              String table,
                                              boolean unique,
                                              boolean approximate) throws
           SQLException {
      OtherUtils.writeLogInfo(log,
              "DatabaseMetaData -> getIndexInfo(catalog = " +
                      catalog +
                      ", schema = " + schema +
                      ", table = " + table +
                      ", unique = " + unique +
                      ", approximate = " + approximate +
                      ")");

      // load tables preliminarily to the cache to get index data available,
      // e.g. for StelsCSV, StelsXML indexes described in schema.
      loadTable(table);

      Statement st = conn.getH2Connection().createStatement();

      st.execute(
              "CREATE MEMORY TABLE IF NOT EXISTS RJ_SCHEMA.RJ_INDEX_INFO(" +
                      "TABLE_CAT VARCHAR(255)," +
                      "TABLE_SCHEM VARCHAR(255)," +
                      "TABLE_NAME VARCHAR(255)," +
                      "NON_UNIQUE BOOLEAN," +
                      "INDEX_QUALIFIER VARCHAR(255)," +
                      "INDEX_NAME VARCHAR(255)," +
                      "TYPE INTEGER," +
                      "ORDINAL_POSITION INTEGER," +
                      "COLUMN_NAME VARCHAR(255)," +
                      "ASC_OR_DESC CHAR(1)," +
                      "CARDINALITY INTEGER," +
                      "PAGES INTEGER," +
                      "FILTER_CONDITION VARCHAR(255)," +
                      "PRIMARY KEY (INDEX_NAME, COLUMN_NAME))"
      );

      st.execute("DELETE FROM RJ_SCHEMA.RJ_INDEX_INFO");

      PreparedStatement pstInsert = conn.getH2Connection().prepareStatement(
              "INSERT INTO RJ_SCHEMA.RJ_INDEX_INFO VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)");

      // add external indexes
      try {
         IndexSchemaIF indexSchema = conn.getSchemaIF2().getIndexSchema();

         if (indexSchema != null) {
            IndexTableIF[] indexTables = indexSchema.getStoreIndexes(table);

            for (IndexTableIF indexTable : indexTables) {
               // filter unique / non-unique indexes
               if (unique && !indexTable.isUnique())
                  continue;

               // ignore foreign keys to avoid duplicating keys
               if (indexTable.isForeignKey())
                  continue;

               int pos = 1;
               for (IndexFieldIF indexField : indexTable.getIndexFields()) {
                  pstInsert.setString(1, conn.getCatalog());
                  pstInsert.setString(2, getSchemaName());
                  pstInsert.setString(3, getTableIdentifierString(StringUtils.
                          getFileNameWithoutExtension(table)));
                  pstInsert.setBoolean(4, !indexTable.isUnique());
                  pstInsert.setString(5, conn.getCatalog());
                  pstInsert.setString(6, indexTable.getIndexName());
                  pstInsert.setInt(7, tableIndexOther);
                  pstInsert.setInt(8, pos);
                  pstInsert.setString(9, getColumnIdentifierString(indexField.getStoreField().getName()));
                  pstInsert.setString(10, indexField.isAscending() ? "A" : "D");
                  pstInsert.setNull(11, Types.VARCHAR);
                  pstInsert.setNull(12, Types.VARCHAR);
                  pstInsert.setNull(13, Types.VARCHAR);

                  try {
                     pstInsert.execute();
                  } catch (SQLException ex) {
                     log.warn(ex.getMessage(), ex);
                  }

                  pos++;
               }
            }

         } else {
//          ResultSet rs = h2ConnectionMetaData.getIndexInfo(catalog,
//              schema, CacheTableManager.getCacheTableName(table), unique, approximate);
//          ResultSet rs = st.executeQuery("SELECT * FROM INFORMATION_SCHEMA.INDEXES");
//	  com.relationaljunction.utils.TestUtils.printColumnsOut(rs,System.out);
//          com.relationaljunction.utils.TestUtils.printResultSetOut(rs, System.out);

            // add indexes already loaded to H2
            // InformationSchema in H2 are case sensetive!!!
            ResultSet rsIndexLoaded = h2ConnectionMetaData.getIndexInfo(catalog,
                    schema, CacheTableManager.getCacheTableName(table), unique, approximate);

            addNonUniqueResultSet(pstInsert, rsIndexLoaded);
            rsIndexLoaded.close();
         }
      } catch (Exception ex) {
         throw new SQLException(ex.getMessage());
      }

      ResultSet rsTables = st.executeQuery(
              "SELECT * FROM RJ_SCHEMA.RJ_INDEX_INFO ORDER BY NON_UNIQUE, TYPE, TABLE_SCHEM, INDEX_NAME, ORDINAL_POSITION");

      SimpleResultSet simpleResultSet = createSimpleResultSet(rsTables);

      rsTables.close();
      st.close();
      pstInsert.close();

      return simpleResultSet;

      /*
         Columns cols = new Columns();
         cols.add(new Column("TABLE_CAT"));
         cols.add(new Column("TABLE_SCHEM"));
         cols.add(new Column("TABLE_NAME"));
         cols.add(new Column("NON_UNIQUE"));
         cols.add(new Column("INDEX_QUALIFIER"));
         cols.add(new Column("INDEX_NAME"));
         cols.add(new Column("TYPE"));
         cols.add(new Column("ORDINAL_POSITION"));
         cols.add(new Column("COLUMN_NAME"));
         cols.add(new Column("ASC_OR_DESC"));
         cols.add(new Column("CARDINALITY"));
         cols.add(new Column("PAGES"));
         cols.add(new Column("FILTER_CONDITION"));
         SQLTable t = new SQLTable(cols);

         IndexTableIF[] indexTables = conn.getSchema().getIndexSchema().
          getStoreIndexes(table);

         if (indexTables == null)throw new Exception("Table '" + table +
           "' does not exist!");

         for (int i = 0; i < indexTables.length; i++) {
           IndexTableIF indexTable = indexTables[i];
           if (unique && !indexTable.isUnique()) continue;

           int pos = 1;
           for (IndexFieldIF indexField : indexTable.getIndexFields()) {
      t.insert(cols, new Object[] {conn.getCatalog(),
        conn.getSchema().getName(),
        table,
        indexTable.isUnique() ? "false" : "true",
        conn.getCatalog(),
        indexTable.getIndexName(),
        tableIndexOther,
        pos,
        indexField.getStoreField().getName(),
        indexField.isAscending() ? "A" : "D", null, null, null
        });
      pos++;
           }
         }

         return t.getResultSet();
       }
       catch (Exception ex) {
         return null;
       }
      */
   }

   public boolean supportsResultSetType(int type) throws SQLException {
      return h2ConnectionMetaData.supportsResultSetType(type);
   }

   public boolean supportsResultSetConcurrency(int type, int concurrency) throws
           SQLException {
      return h2ConnectionMetaData.supportsResultSetConcurrency(type, concurrency);
   }

   public boolean ownUpdatesAreVisible(int type) throws SQLException {
      return h2ConnectionMetaData.ownUpdatesAreVisible(type);
   }

   public boolean ownDeletesAreVisible(int type) throws SQLException {
      return h2ConnectionMetaData.ownDeletesAreVisible(type);
   }

   public boolean ownInsertsAreVisible(int type) throws SQLException {
      return h2ConnectionMetaData.ownInsertsAreVisible(type);
   }

   public boolean othersUpdatesAreVisible(int type) throws SQLException {
      return h2ConnectionMetaData.othersUpdatesAreVisible(type);
   }

   public boolean othersDeletesAreVisible(int type) throws SQLException {
      return h2ConnectionMetaData.othersDeletesAreVisible(type);
   }

   public boolean othersInsertsAreVisible(int type) throws SQLException {
      return h2ConnectionMetaData.othersInsertsAreVisible(type);
   }

   public boolean updatesAreDetected(int type) throws SQLException {
      return h2ConnectionMetaData.updatesAreDetected(type);
   }

   public boolean deletesAreDetected(int type) throws SQLException {
      return h2ConnectionMetaData.deletesAreDetected(type);
   }

   public boolean insertsAreDetected(int type) throws SQLException {
      return h2ConnectionMetaData.insertsAreDetected(type);
   }

   public boolean supportsBatchUpdates() throws SQLException {
      return h2ConnectionMetaData.supportsBatchUpdates();
   }

   public ResultSet getUDTs(String catalog, String schemaPattern,
                            String typeNamePattern, int[] types) throws
           SQLException {
      // currently returns an empty result set
      return h2ConnectionMetaData.getUDTs(catalog, schemaPattern,
              typeNamePattern, types);

      /*
         try {
           Columns cols = new Columns();
           cols.add(new Column("TYPE_CAT"));
           cols.add(new Column("TYPE_SCHEM"));
           cols.add(new Column("TYPE_NAME"));
           cols.add(new Column("CLASS_NAME"));
           cols.add(new Column("DATA_TYPE", Column.INTEGER));
           cols.add(new Column("REMARKS"));
           cols.add(new Column("BASE_TYPE", Column.INTEGER));
           SQLTable t = new SQLTable(cols);
           return t.getResultSet();
         } catch (Exception ex) {
           return null;
         }
      */
   }

   public Connection getConnection() throws SQLException {
      return this.conn;
   }

   public boolean supportsSavepoints() throws SQLException {
      return false;
   }

   public boolean supportsNamedParameters() throws SQLException {
      return h2ConnectionMetaData.supportsNamedParameters();
   }

   public boolean supportsMultipleOpenResults() throws SQLException {
      return h2ConnectionMetaData.supportsMultipleOpenResults();
   }

   public boolean supportsGetGeneratedKeys() throws SQLException {
      return h2ConnectionMetaData.supportsGetGeneratedKeys();
   }

   public ResultSet getSuperTypes(String catalog, String schemaPattern,
                                  String typeNamePattern) throws SQLException {
      // not supported
      return h2ConnectionMetaData.getSuperTypes(catalog, schemaPattern,
              typeNamePattern);
   }

   public ResultSet getSuperTables(String catalog, String schemaPattern,
                                   String tableNamePattern) throws SQLException {
      // currently returns an empty result set
      return h2ConnectionMetaData.getSuperTables(catalog, schemaPattern,
              tableNamePattern);
   }

   public ResultSet getAttributes(String catalog, String schemaPattern,
                                  String typeNamePattern,
                                  String attributeNamePattern) throws
           SQLException {
      // not supported
      return h2ConnectionMetaData.getAttributes(catalog, schemaPattern,
              typeNamePattern,
              attributeNamePattern);
   }

   public boolean supportsResultSetHoldability(int holdability) throws
           SQLException {
      return h2ConnectionMetaData.supportsResultSetHoldability(holdability);
   }

   public int getResultSetHoldability() throws SQLException {
      return h2ConnectionMetaData.getResultSetHoldability();
   }

   public int getDatabaseMajorVersion() throws SQLException {
      return conn.driver.getMajorVersion();
   }

   public int getDatabaseMinorVersion() throws SQLException {
      return conn.driver.getMinorVersion();
   }

   public int getJDBCMajorVersion() throws SQLException {
      return conn.driver.getMajorVersion();
   }

   public int getJDBCMinorVersion() throws SQLException {
      return conn.driver.getMinorVersion();
   }

   public int getSQLStateType() throws SQLException {
      return h2ConnectionMetaData.getSQLStateType();
   }

   public boolean locatorsUpdateCopy() throws SQLException {
      return h2ConnectionMetaData.locatorsUpdateCopy();
   }

   public boolean supportsStatementPooling() throws SQLException {
      return h2ConnectionMetaData.supportsStatementPooling();
   }

   // ---- JDK 1.6 ---//

   public RowIdLifetime getRowIdLifetime() throws SQLException {
      throw new UnsupportedOperationException();
   }

   public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
      throw new UnsupportedOperationException();
   }

   public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
      return false;
   }

   public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
      return false;
   }

   public ResultSet getClientInfoProperties() throws SQLException {
      return h2ConnectionMetaData.getClientInfoProperties();
   }

   public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
      throw new UnsupportedOperationException();
   }

   public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
      throw new UnsupportedOperationException();
   }

   public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new UnsupportedOperationException();
   }

   public boolean isWrapperFor(Class<?> iface) throws SQLException {
      throw new UnsupportedOperationException();
   }

	@Override
	public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern,
			String columnNamePattern) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean generatedKeyAlwaysReturned() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
}

