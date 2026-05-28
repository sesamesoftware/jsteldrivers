package com.relationaljunction.jdbc.common.h2.sql;

import java.util.HashSet;

public class UpdateSQLCommand extends SQLCommand {
  private String baseTableName = null;

  protected UpdateSQLCommand(String sqlText,
			     HashSet<String> tablesUsed, SQLNode rootNode) {
    super(SQLCommand.UPDATE, sqlText, tablesUsed, rootNode);

    SQLNode tableNode = rootNode.getUniqueChildWithName("TABLE_DESCR");
    baseTableName = tableNode.getProperty("TABLE_NAME");
  }

  public String getBaseTable() {
    return baseTableName;
  }

}
