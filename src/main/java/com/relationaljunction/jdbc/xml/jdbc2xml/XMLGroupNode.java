package com.relationaljunction.jdbc.xml.jdbc2xml;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import com.relationaljunction.utils.StringUtils;

public class XMLGroupNode implements XMLOutputNodeIF {
   private final List<XMLOutputNodeIF> childNodes;
   private StringBuilder key = new StringBuilder();
   private final List<String> groupColumns = new LinkedList<String>();

   public XMLGroupNode(List<XMLOutputNodeIF> childNodes, String groupColumnsString) {
      this.childNodes = childNodes;

      StringTokenizer st = new StringTokenizer(groupColumnsString, ",");
      while (st.hasMoreElements()) {
         String groupColumn = st.nextToken();
         if (!StringUtils.containsPattern(groupColumn)) {
            throw new IllegalArgumentException("group column must contain column " +
                    "pattern with parameters like {@alias.columnName} '" + groupColumn + "'");
         }

         groupColumns.add(groupColumn);
      }

      if (groupColumns.isEmpty())
         throw new IllegalArgumentException("Invalid group columns '" + groupColumnsString + "'");

   }

   public void execute(XMLOutputContext context, Writer writer) throws SQLException, IOException {
      StringBuilder currentKey = new StringBuilder();

      // get a current key for a current row
      for (int i = 0; i < groupColumns.size(); i++) {
         String groupColumn = groupColumns.get(i);

         // resolve parameters
         currentKey.append(StringUtils.replaceParameters(groupColumn, context.getParameterHandler()));
         if (i < groupColumns.size() - 1) {
            currentKey.append(",");
         }
      }

      // keys are different
      if (!key.toString().equalsIgnoreCase(currentKey.toString())) {
         key = currentKey;

         // get child nodes executed
         for (XMLOutputNodeIF node : childNodes) {
            node.execute(context, writer);
         }
      }
   }

   public void reset() {
      for (XMLOutputNodeIF node : childNodes) {
         node.reset();
      }

      key = new StringBuilder();
   }
}
