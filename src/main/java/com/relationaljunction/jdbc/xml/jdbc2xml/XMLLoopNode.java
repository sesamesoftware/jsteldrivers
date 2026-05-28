package com.relationaljunction.jdbc.xml.jdbc2xml;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class XMLLoopNode implements XMLOutputNodeIF {
   private final String queryAlias;
   private final List<XMLOutputNodeIF> childNodes;

   public XMLLoopNode(List<XMLOutputNodeIF> childNodes, String queryAlias) {
      this.queryAlias = queryAlias;
      this.childNodes = childNodes;
   }

   public String getQuery() {
      return queryAlias;
   }

   public void execute(XMLOutputContext context, Writer writer) throws SQLException, IOException {
      ResultSet rs = context.getResultSet(queryAlias);
      rs.beforeFirst();

      // loop on ResultSet
      while (rs.next()) {
         for (XMLOutputNodeIF node : childNodes) {
            node.execute(context, writer);
         }
      }

      // ResultSet should not be closed.
//      rs.close();
   }

   public void reset() {
      for (XMLOutputNodeIF node : childNodes) {
         node.reset();
      }
   }
}
