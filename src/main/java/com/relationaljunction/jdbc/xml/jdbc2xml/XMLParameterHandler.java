package com.relationaljunction.jdbc.xml.jdbc2xml;

import java.sql.SQLException;

import com.relationaljunction.utils.StringUtils;


public class XMLParameterHandler extends StringUtils.ParameterReplacementHandler {
   private final XMLOutputContext context;

   public XMLParameterHandler(XMLOutputContext context) {
      this.context = context;
   }

   @Override
   public String getStringValueForParameter(String parameterName) throws RuntimeException {
//      StringTokenizer st = new StringTokenizer(parameterName, ".");
//
//      if (st.countTokens() < 2)
//         throw new IllegalArgumentException("Bad parameter name '" + parameterName +
//                 "'. It must be 'queryAlias.columnName'");
//
//      String queryAlias = st.nextToken().substring(2);
//      String columnName = st.nextToken();
//      columnName = columnName.substring(0, columnName.length() - 1);

      String[] delimiters = parameterName.split("\\.", 2);

      if (delimiters.length < 2) {
         throw new IllegalArgumentException("Bad parameter name '" + parameterName +
                 "'. It must be like '{@queryAlias.[columnAlias.]columnName}'");
      }

      String queryAlias = delimiters[0].substring(2);
      String columnName = delimiters[1].substring(0, delimiters[1].length() - 1);

      String result;
      try {
         result = context.getResultSet(queryAlias).getString(columnName);
      } catch (SQLException e) {
         throw new RuntimeException("Error while resolving parameter '" + parameterName +
                 "'. Error was: " + e.getMessage(), e);
      }

      return result != null ? result : "";
   }
}
