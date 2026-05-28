package com.relationaljunction.jdbc.common.h2.sql;

import java.io.*;
import java.util.*;

public class TestParser {
   public static void main(String[] args) throws Exception {
      SqlParser p = null;
      String file_name = args[0];

      FileReader fr = new FileReader(file_name);
      String sql = com.relationaljunction.utils.FileUtils.fileContentToString(fr);
      fr.close();
      PrintStream ps = new PrintStream(new FileOutputStream("root_info.txt"));

      StringTokenizer st = new StringTokenizer(sql, ";");

      while (st.hasMoreElements()) {
         String query = st.nextToken();
         System.out.println(query);

         if (query.trim().startsWith("//")) continue;

//    sql="SELECT '\u0166' FROM \"test\"";
//    p = new SqlParser(sql);
         p = new SqlParser(query.trim());

         SQLNode root = p.QueryStatement();

         if (root.getName().equals("SELECT_QUERY")) {
            System.out.println("####### SELECT #########");
         } else if (root.getName().equals("UPDATE_QUERY")) {
            System.out.println("####### UPDATE #########");
         } else if (root.getName().equals("DELETE_QUERY")) {
            System.out.println("####### DELETE #########");
         } else if (root.getName().equals("CREATE_QUERY")) {
            System.out.println("####### CREATE #########");
         } else if (root.getName().equals("DROP_QUERY")) {
            System.out.println("####### DROP #########");
         }

         Vector v = new Vector();
         root.getNodesByName("TABLE_DESCR", v);

         HashSet<String> tablesUsed = new HashSet<String>();
         for (int i = 0; i < v.size(); i++)
            tablesUsed.add(((SQLNode) v.get(i)).getProperty("TABLE_NAME"));

         System.out.print("------ tables used: ");
         for (String tableName : tablesUsed) {
            System.out.print(tableName + ",");
         }
         System.out.println();

         ps.println("#####################################");
         root.printTreeInfo(ps);

//         System.out.println(quoteTables(query.trim(), v));
//        Vector v = root.getChildsWithType("TABLE_NAME");
      }

      ps.close();
   }

   static String quoteTables(String sql, Vector nodes) throws
           Exception {
      StringBuilder result = new StringBuilder();
      BufferedReader br = new BufferedReader(new StringReader(sql));
      String line = br.readLine();
      if (line == null) return sql;
      int lineCount = 1;
      int lineOffset = 0;

      for (int i = 0; i < nodes.size(); i++) {
         SQLNode node = (SQLNode) nodes.get(i);
         boolean isQuoted = node.getProperty("TABLE_NAME_TYPE").equals("QUOTED");
         if (isQuoted) continue;
         String tableName = node.getProperty("TABLE_NAME");
         int begLine = Integer.parseInt(node.getProperty("TABLE_NAME_BEG_LINE"));
         int begCol = Integer.parseInt(node.getProperty("TABLE_NAME_BEG_COLUMN"));
         int endCol = Integer.parseInt(node.getProperty("TABLE_NAME_END_COLUMN"));

         if (lineCount < begLine) {
            result.append(line).append('\n');
            line = br.readLine();
            if (line == null) throw new Exception("Unexpected EOF of an SQL query");
            lineCount++;
            lineOffset = 0;
         }

         String lineLeftPart = line.substring(0, begCol - lineOffset - 1);
         line = line.substring(endCol - lineOffset);
         lineOffset += endCol - lineOffset;
         result.append(lineLeftPart).append("\"").append(tableName).append("\"");
      }

      if (!line.isEmpty()) result.append(line);
      br.close();
      return result.toString();
   }

}
