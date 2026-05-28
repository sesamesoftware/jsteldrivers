package com.relationaljunction.jdbc.common.h2.sql;

import java.util.HashSet;

public class DeleteSQLCommand extends SQLCommand {
  private String baseTableName = null;

  protected DeleteSQLCommand(String sqlText,
			     HashSet<String> tablesUsed, SQLNode rootNode) {
    super(SQLCommand.DELETE, sqlText, tablesUsed, rootNode);

    SQLNode tableNode = rootNode.getUniqueChildWithName("TABLE_DESCR");
    baseTableName = tableNode.getProperty("TABLE_NAME");
  }

  public String getBaseTable() {
    return baseTableName;
  }

}
