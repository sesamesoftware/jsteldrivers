package com.relationaljunction.jdbc.xml.jdbc2xml;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;

public class XMLOutputContext {
   private HashMap<String, ResultSet> resultSetHashMap = new HashMap<String, ResultSet>();
   private Connection conn;
   private XMLParameterHandler parameterHandler;

   public XMLOutputContext(Connection conn) throws SQLException {
      this.conn = conn;
      parameterHandler = new XMLParameterHandler(this);
   }

   public void addResultSet(String alias, String query) throws SQLException {
      Statement st = conn.createStatement();
      resultSetHashMap.put(alias, st.executeQuery(query));
   }

   public ResultSet getResultSet(String alias) {
      if (resultSetHashMap.get(alias) == null)
         throw new IllegalArgumentException("Can't find a result set for '" + alias + "'");

      return resultSetHashMap.get(alias);
   }

   public XMLParameterHandler getParameterHandler() {
      return parameterHandler;
   }

   public void close() throws SQLException {
      // close ResultSets
      Collection<ResultSet> resultSets = resultSetHashMap.values();
      for (ResultSet rs : resultSets) {
         Statement st = rs.getStatement();
         rs.close();
         st.close();
      }


      resultSetHashMap.clear();
      resultSetHashMap = null;
      conn = null;
      parameterHandler = null;
   }
}
