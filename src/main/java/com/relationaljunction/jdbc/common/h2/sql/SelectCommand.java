package com.relationaljunction.jdbc.common.h2.sql;

import java.util.HashSet;

public class SelectCommand extends SQLCommand{
  protected SelectCommand(String sqlText,
                          HashSet<String> tablesUsed, SQLNode rootNode) {
    super(SQLCommand.SELECT, sqlText, tablesUsed, rootNode);
  }

  // if a query like "SELECT * FROM tbl"
  public boolean isSimpleSelect(){
    SQLNode selectBlock = rootNode.getUniqueChildWithName("SELECT_BLOCK");
      return selectBlock.getProperty("LIST_TYPE").equals("*") &&
              getTablesUsed().size() == 1;
  }
}
