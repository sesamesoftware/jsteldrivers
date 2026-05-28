package com.relationaljunction.jdbc.common.h2.sql;

import java.util.HashSet;
import java.io.*;

import com.relationaljunction.utils.UnexpectedException;

public class SaveCommand extends SQLCommand {
  private String filePath = null;
  private String specTable = null;
  private String query = null;
  private String baseTableName = null;

  protected SaveCommand(String sqlText,
                        HashSet<String> tablesUsed, SQLNode rootNode) {
    super(SQLCommand.SAVE, sqlText, tablesUsed, rootNode);

    SQLNode tableNode = rootNode.getUniqueChildWithName("TABLE_DESCR");

    if (tableNode != null)
      // table is specified
      baseTableName = tableNode.getProperty("TABLE_NAME");
    else{
      // SELECT query is specified
      //      System.out.println(sqlText);
      int queryBegLineIndex = Integer.parseInt(rootNode.getProperty(
          "QUERY_BEG_LINE"));
      int queryEndLineIndex = Integer.parseInt(rootNode.getProperty(
	  "QUERY_END_LINE"));
      int queryBegColumnIndex = Integer.parseInt(rootNode.getProperty(
	  "QUERY_BEG_COLUMN"));
      int queryEndColumnIndex = Integer.parseInt(rootNode.getProperty(
	  "QUERY_END_COLUMN"));
      try {
        query = com.relationaljunction.utils.StringUtils.substring(sqlText, queryBegLineIndex,
            queryBegColumnIndex,
            queryEndLineIndex,
            queryEndColumnIndex);
      } catch (IOException ex) {
        throw new UnexpectedException("Unexpected error in SaveCommand(): "+ex.getMessage());
      }
    }

    if (!rootNode.getProperty("FILE_PATH").trim().isEmpty())
      filePath = com.relationaljunction.utils.StringUtils.unquote(rootNode.getProperty(
          "FILE_PATH"),
                                                  new char[] {'"'});

    if (!rootNode.getProperty("USING_SPECIFICATION").trim().isEmpty())
      specTable = com.relationaljunction.utils.StringUtils.unquote(rootNode.getProperty(
          "USING_SPECIFICATION"), new char[] {'"'});
  }

  public String getBaseTable() {
    return baseTableName;
  }

  public String getFilePath() {
    return filePath;
  }

  public String getQuery() {
    return query;
  }

  public String getSpecTable() {
    return specTable;
  }

}
