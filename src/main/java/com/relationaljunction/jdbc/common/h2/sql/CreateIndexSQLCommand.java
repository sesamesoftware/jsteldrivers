package com.relationaljunction.jdbc.common.h2.sql;

import java.util.HashSet;

public class CreateIndexSQLCommand extends SQLCommand {

  protected CreateIndexSQLCommand(String sqlText,
			     HashSet<String> tablesUsed, SQLNode rootNode) {
    super(SQLCommand.CREATE_INDEX, sqlText, tablesUsed, rootNode);
  }

}
