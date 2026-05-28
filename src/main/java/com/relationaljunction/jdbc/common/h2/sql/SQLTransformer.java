package com.relationaljunction.jdbc.common.h2.sql;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class SQLTransformer {
   public static class SQLTransformerException extends Exception {
      public SQLTransformerException(String message, Exception ex) {
         super(message, ex);
      }
   }

   public static String doubleQuoteColumnNames(String sqlText) throws Exception {
      if (sqlText == null) throw new NullPointerException();

      SqlParser parser = new SqlParser(sqlText);
      SQLNode root = parser.QueryStatement();

//      root.printTreeInfo(System.out);

      return doubleQuoteColumnNames(sqlText, root);
   }

   private static String doubleQuoteColumnNames(String sqlText, SQLNode root) throws Exception {
      List<SQLNode> columnNodes = new ArrayList<SQLNode>();
      StringBuilder result;

      try {
         root.getNodesByName("COLUMN_DESCR", columnNodes);

         result = new StringBuilder();
         BufferedReader br = new BufferedReader(new StringReader(sqlText));

         String line = br.readLine();
         int lineCount = 1;
         int lineOffset = 0;

         for (SQLNode columnNode : columnNodes) {
            boolean isQuoted = columnNode.getProperty("COLUMN_NAME_TYPE").equals("QUOTED");

            // column name is already quoted
            if (isQuoted) continue;

            String columnName = columnNode.getProperty("COLUMN_NAME");

            // ignore wildcard columns
            if (columnName.equals("*")) continue;

            int begLine = Integer.parseInt(columnNode.getProperty("COLUMN_NAME_BEG_LINE"));
            int begCol = Integer.parseInt(columnNode.getProperty("COLUMN_NAME_BEG_COLUMN"));
            int endCol = Integer.parseInt(columnNode.getProperty("COLUMN_NAME_END_COLUMN"));

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
            result.append(lineLeftPart).append("\"").append(columnName).append("\"");
         }

         if (!line.isEmpty()) result.append(line);

         while ((line = br.readLine()) != null) {
            result.append("\n").append(line);
         }

         br.close();
      } catch (Exception ex) {
         throw new SQLTransformerException("Can't quote column names. " +
                 "Error was: " + ex.getMessage(), ex);
      }

      return result.toString();
   }

   public static void main(String[] args) throws Exception {
      String sql = "  SELECT col1\r\n,prices.*, prices.[MIN_price], prices.max_price, \r\n prices.curr_price, prices.\"avg_price\" \r\nFROM prices where a=b+a";

      System.out.println(doubleQuoteColumnNames(sql));
   }
}
