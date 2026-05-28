package com.relationaljunction.jdbc.xml.jdbc2xml;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;

import com.relationaljunction.utils.StringUtils;

public class XMLQueryNode implements XMLOutputNodeIF {
   private boolean containsPattern = false;
   private final String alias;
    private final String sql;

   public XMLQueryNode(String alias, String sql) {
      this.alias = alias;
      this.sql = sql;
      this.containsPattern = StringUtils.containsPattern(sql);
   }

   public void execute(XMLOutputContext context, Writer writer) throws SQLException, IOException {
      String queryWithReplacement;

      if (containsPattern)
         queryWithReplacement = StringUtils.replaceParameters(sql, context.getParameterHandler());
      else
         queryWithReplacement = sql;

      context.addResultSet(alias, queryWithReplacement);
   }

   public void reset() {
   }
}
