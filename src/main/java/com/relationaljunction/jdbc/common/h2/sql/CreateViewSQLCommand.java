package com.relationaljunction.jdbc.common.h2.sql;

import java.util.HashSet;

public class CreateViewSQLCommand extends SQLCommand{
  private String viewName = null;

  public CreateViewSQLCommand(String sqlText, HashSet<String> tablesUsed,
                              SQLNode rootNode) {
    super(SQLCommand.CREATE_VIEW, sqlText, tablesUsed, rootNode);

    SQLNode viewClauseNode = rootNode.getUniqueChildWithName("VIEW_CLAUSE");
    viewName = viewClauseNode.getUniqueChildWithName("TABLE_DESCR").getProperty(
        "TABLE_NAME");
    tablesUsed.remove(viewName);
  }

  public String getBaseTable() {
    return viewName;
  }
}
