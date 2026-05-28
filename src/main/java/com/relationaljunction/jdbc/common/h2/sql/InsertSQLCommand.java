package com.relationaljunction.jdbc.common.h2.sql;

import java.util.HashSet;

public class InsertSQLCommand extends SQLCommand {
  private String baseTableName = null;
  private boolean insertSelectCommand = false;

  protected InsertSQLCommand(String sqlText,
                             HashSet<String> tablesUsed, SQLNode rootNode) {
    super(SQLCommand.INSERT, sqlText, tablesUsed, rootNode);

    SQLNode tableNode = rootNode.getUniqueChildWithName("TABLE_DESCR");
    baseTableName = tableNode.getProperty("TABLE_NAME");
    insertSelectCommand = rootNode.getUniqueChildWithName("SELECT_QUERY") != null;
  }

  public String getBaseTable() {
    return baseTableName;
  }

  public boolean isInsertSelectCommand(){
    return insertSelectCommand;
  }

}
