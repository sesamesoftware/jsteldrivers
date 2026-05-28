package com.relationaljunction.jdbc.common.h2.sql;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.jdbc.common.h2.CacheTableManager;

public class SQLCommand {
   private static final Logger log = LoggerFactory.getLogger("SQLCommand");

   public static final int SELECT = 0;
   public static final int INSERT = 1;
   public static final int UPDATE = 2;
   public static final int DELETE = 3;
   public static final int CREATE_TABLE = 4;
   public static final int DROP_TABLE = 5;
   public static final int DROP_TABLE_FROM_CACHE = 7;
   public static final int CREATE_INDEX = 8;
   public static final int DROP_INDEX = 9;
   public static final int OTHER = 10;
   public static final int SAVE = 11;
   public static final int CREATE_VIEW = 12;
   public static final int DROP_VIEW = 14;
   public static final int EXPLAIN = 15;
   public static final int RELOAD_TABLE = 16;
   public static final int RELOAD_CACHE = 17;
   public static final int SHUTDOWN = 18;
   public static final int LOCK_DATABASE = 19;
   public static final int UNLOCK_DATABASE = 20;
   private int type = SELECT;
   private String sqlText = null;
   protected SQLNode rootNode = null;
   protected HashSet<String> tablesUsed = new HashSet<String>();
   private final static char[] WHITE_SPACE_CHARS = new char[]{'\n', '\t', '\r'};

//  private Hashtable functionsMap = new Hashtable();


//  protected SQLCommand(int commandType, String sqlText) {
//    this(commandType, sqlText, null);
//  }

   protected SQLCommand(int commandType, String sqlText,
                        HashSet<String> tablesUsed, SQLNode rootNode) {
      this.type = commandType;
      this.sqlText = sqlText;
      this.tablesUsed = tablesUsed;
      this.rootNode = rootNode;
   }

   public Set<String> getTablesUsed() {
      return tablesUsed;
   }

   public String getBaseTable() {
      // by default return a first table. That's correct for CREATE and DROP
      Iterator<String> iter = tablesUsed.iterator();
      return iter.next();
   }

   public int getType() {
      return type;
   }

   public String getSqlText() {
      return sqlText;
   }

   public void setSqlText(String newSqlText) {
      sqlText = newSqlText;
   }

   public static SQLCommand parseSQLCommand(String sql)
           throws Exception {
      SQLCommand command;
      HashSet<String> tablesUsed = new HashSet<String>();
      SQLNode rootNode;

      String sqlText = sql.trim();

      try {
         // parse an sql text
         SqlParser parser = new SqlParser(sqlText);
         rootNode = parser.QueryStatement();
      } catch (Exception ex) {
         com.relationaljunction.utils.OtherUtils.writeWarnInfo(log,
                 "Can't preparse the query: " + sql +
                         ". Error was: " + ex.getMessage(), true);
         throw ex;
      }

      // get tables being used in a query
      Vector tableNodes = new Vector();
      rootNode.getNodesByName("TABLE_DESCR", tableNodes);

      for (Object tableNode : tableNodes)
         tablesUsed.add(((SQLNode) tableNode).getProperty("TABLE_NAME"));

      if (rootNode.isSelectQuery()) {
         command = new SelectCommand(sqlText, tablesUsed, rootNode);
      } else if (rootNode.isInsert()) {
         command = new InsertSQLCommand(sqlText, tablesUsed, rootNode);
      } else if (rootNode.isUpdate()) {
         command = new UpdateSQLCommand(sqlText, tablesUsed, rootNode);
      } else if (rootNode.isDelete()) {
         command = new DeleteSQLCommand(sqlText, tablesUsed, rootNode);
      } else if (rootNode.isCreateTable()) {
         command = new CreateTableSQLCommand(sqlText, tablesUsed, rootNode);
      } else if (rootNode.isDropTable()) {
         command = new SQLCommand(SQLCommand.DROP_TABLE, sqlText, tablesUsed, rootNode);
      } else if (rootNode.isCreateIndex()) {
         command = new SQLCommand(SQLCommand.CREATE_INDEX, sqlText, tablesUsed, rootNode);
      } else if (rootNode.isCreateView()) {
         command = new CreateViewSQLCommand(sqlText, tablesUsed, rootNode);
      } else if (rootNode.isDropView()) {
         command = new SQLCommand(SQLCommand.DROP_VIEW, sqlText, tablesUsed, rootNode);
      } else if (rootNode.isDropTableFromCache()) {
         command = new SQLCommand(SQLCommand.DROP_TABLE_FROM_CACHE, sqlText,
                 tablesUsed, rootNode);
      } else if (rootNode.isReloadTable()) {
         command = new SQLCommand(SQLCommand.RELOAD_TABLE, sqlText,
                 tablesUsed, rootNode);
      } else if (rootNode.isReloadCache()) {
         command = new SQLCommand(SQLCommand.RELOAD_CACHE, sqlText,
                 tablesUsed, rootNode);
      } else if (rootNode.isShutdown()) {
         command = new SQLCommand(SQLCommand.SHUTDOWN, sqlText,
                 tablesUsed, rootNode);
      } else if (rootNode.isLockDatabase()) {
         command = new SQLCommand(SQLCommand.LOCK_DATABASE, sqlText,
                 tablesUsed, rootNode);
      } else if (rootNode.isUnlockDatabase()) {
         command = new SQLCommand(SQLCommand.UNLOCK_DATABASE, sqlText,
                 tablesUsed, rootNode);
      } else if (rootNode.isSave()) {
         command = new SaveCommand(sqlText, tablesUsed, rootNode);
      } else if (rootNode.isExplain()) {
         command = new SQLCommand(SQLCommand.EXPLAIN, sqlText, tablesUsed,
                 rootNode);
      } else
         command = new SQLCommand(SQLCommand.OTHER, sqlText, tablesUsed, rootNode);

      return command;
   }

   public String replaceTableNames(String tableName2replace, String newTableName) throws
           Exception {
      StringBuilder result = new StringBuilder();
      BufferedReader br = new BufferedReader(new StringReader(sqlText));
      String line = br.readLine();
      if (line == null) return sqlText;
      int lineCount = 1;
      int lineOffset = 0;

      Vector<SQLNode> nodes = new Vector<SQLNode>();
      rootNode.getNodesByName("TABLE_DESCR", nodes);

      for (int i = 0; i < nodes.size(); i++) {
         SQLNode node = nodes.get(i);
         String tableName = node.getProperty("TABLE_NAME");

         if (!com.relationaljunction.utils.StringUtils.
                 unquote(tableName, CacheTableManager.QUOTE_CHARS).equals
                 (com.relationaljunction.utils.StringUtils.unquote(newTableName,
                         CacheTableManager.QUOTE_CHARS)))
            continue;

         int begLine = Integer.parseInt(node.getProperty("TABLE_NAME_BEG_LINE"));
         int begCol = Integer.parseInt(node.getProperty("TABLE_NAME_BEG_COLUMN"));
         int endCol = Integer.parseInt(node.getProperty("TABLE_NAME_END_COLUMN"));

         while (lineCount < begLine) {
            result.append(line).append('\n');
            line = br.readLine();
            if (line == null) throw new Exception("Unexpected EOF of an SQL query");
            lineCount++;
            lineOffset = 0;
         }

         String lineLeftPart = line.substring(0, begCol - lineOffset - 1);
         line = line.substring(endCol - lineOffset);
         lineOffset += endCol - lineOffset;

//      node.setProperty("TABLE_NAME", name2replace);
//      tablesUsed.add(name2replace);

         result.append(newTableName);
      }

      if (!line.isEmpty()) result.append(line);
      br.close();

      return result.toString();
   }

/*
  public static SQLCommand parseSQLCommand(CommonConnection2 conn, String sql) throws
      SQLException {
    SQLCommand command = null;
    HashSet<String> tablesUsed = new HashSet<String>();
    SQLNode rootNode = null;
    String sqlText = null;

    String sqlWithoutWhiteSpace = com.relationaljunction.utils.StringUtils.replaceChars(sql.trim(),
        WHITE_SPACE_CHARS, ' ');

    try {
      // parse an sql text
      SqlParser parser = new SqlParser(sqlWithoutWhiteSpace);
      rootNode = parser.QueryStatement();
    } catch (Exception ex) {
      conn.driver.writeLog("Can't preparse the query: " + sql);
      return null;
    }

    try {
      // quote tables in an sql text and nodes, fill tables used
      sqlText = quoteTables(conn, sqlWithoutWhiteSpace, rootNode, tablesUsed);
    } catch (Exception ex) {
      throw conn.driver.createException("Can't quote tables in a query: " + sql);
    }

    if (rootNode.isSelectQuery()) {
      command = new SQLCommand(SQLCommand.SELECT, sqlText, tablesUsed);
    }
    else if (rootNode.isInsert()) {
      command = new InsertSQLCommand(sqlText, tablesUsed, rootNode);
    } else if (rootNode.isUpdate()) {
      command = new SQLCommand(SQLCommand.UPDATE, sqlText, tablesUsed);
    } else if (rootNode.isDelete()) {
      command = new SQLCommand(SQLCommand.DELETE, sqlText, tablesUsed);
    } else if (rootNode.isCreateTable()) {
      command = new CreateTableSQLCommand(sqlText, tablesUsed, rootNode);
    } else if (rootNode.isDropTable()) {
      command = new SQLCommand(SQLCommand.DROP_TABLE, sqlText, tablesUsed);
    }
    else if (rootNode.isDropTableFromCache()) {
      command = new SQLCommand(SQLCommand.DROP_TABLE_FROM_CACHE, sqlText,
                               tablesUsed);
    }
    return command;
  }
*/

/*
  private static String quoteTables(CommonConnection2 conn, String sql,
                                    SQLNode rootNode,
                                    HashSet<String> tablesUsed) throws
      Exception {
    StringBuilder result = new StringBuilder();
    BufferedReader br = new BufferedReader(new StringReader(sql));
    String line = br.readLine();
    if (line == null)return sql;
    int lineCount = 1;
    int lineOffset = 0;

    Vector<SQLNode> nodes = new Vector<SQLNode>();
    rootNode.getNodesByName("TABLE_DESCR", nodes);

    for (int i = 0; i < nodes.size(); i++) {
      SQLNode node = nodes.get(i);
      boolean isQuoted = node.getProperty("TABLE_NAME_TYPE").equals("QUOTED");
//      if (isQuoted)continue;
      String tableName = node.getProperty("TABLE_NAME");
      int begLine = Integer.parseInt(node.getProperty("TABLE_NAME_BEG_LINE"));
      int begCol = Integer.parseInt(node.getProperty("TABLE_NAME_BEG_COLUMN"));
      int endCol = Integer.parseInt(node.getProperty("TABLE_NAME_END_COLUMN"));

      while (lineCount < begLine) {
	result.append(line + '\n');
	line = br.readLine();
	if (line == null)throw new Exception("Unexpected EOF of an SQL query");
	lineCount++;
	lineOffset = 0;
      }

      String lineLeftPart = line.substring(0, begCol - lineOffset - 1);
      line = line.substring(endCol - lineOffset);
      lineOffset += endCol - lineOffset;

      String fullTableName =  conn.getFileTableName(tableName);

      node.setProperty("TABLE_NAME", fullTableName);
      tablesUsed.add(fullTableName);

      result.append(lineLeftPart + "\"" + fullTableName.toUpperCase() + "\"");
    }

    if (line.length() > 0) result.append(line);
    br.close();


    return result.toString();
  }

  private static String quoteTablesInSQLText(CommonConnection2 conn, String sql,
                                    Vector<SQLNode> nodes) throws
      Exception {
    StringBuilder result = new StringBuilder();
    BufferedReader br = new BufferedReader(new StringReader(sql));
    String line = br.readLine();
    if (line == null)return sql;
    int lineCount = 1;
    int lineOffset = 0;

    for (int i = 0; i < nodes.size(); i++) {
      SQLNode node = nodes.get(i);
      boolean isQuoted = node.getProperty("TABLE_NAME_TYPE").equals("QUOTED");
//      if (isQuoted)continue;
      String tableName = node.getProperty("TABLE_NAME");
      int begLine = Integer.parseInt(node.getProperty("TABLE_NAME_BEG_LINE"));
      int begCol = Integer.parseInt(node.getProperty("TABLE_NAME_BEG_COLUMN"));
      int endCol = Integer.parseInt(node.getProperty("TABLE_NAME_END_COLUMN"));

      if (lineCount < begLine) {
	result.append(line + '\n');
	line = br.readLine();
	if (line == null)throw new Exception("Unexpected EOF of an SQL query");
	lineCount++;
	lineOffset = 0;
      }

      String lineLeftPart = line.substring(0, begCol - lineOffset - 1);
      line = line.substring(endCol - lineOffset);
      lineOffset += endCol - lineOffset;

      String fullTableName =  conn.getFileTableName(tableName);
//      node.setProperty("TABLE_NAME", fullTableName);
      result.append(lineLeftPart + "\"" + fullTableName + "\"");
    }

    if (line.length() > 0) result.append(line);
    br.close();
    return result.toString();
  }
*/

}
