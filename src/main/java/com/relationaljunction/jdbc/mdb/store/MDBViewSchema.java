package com.relationaljunction.jdbc.mdb.store;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.healthmarketscience.jackcess.query.Query;
import com.relationaljunction.database.view.DefaultViewTable;
import com.relationaljunction.database.view.ViewSchemaIF;
import com.relationaljunction.database.view.ViewTableIF;
import com.relationaljunction.jdbc.common.h2.sql.SQLTransformer;
import com.relationaljunction.utils.StringUtils;

public class MDBViewSchema implements ViewSchemaIF {
	
   private final Logger log = LoggerFactory.getLogger("MDBViewSchema");

   private final Map<String,
           ViewTableIF> viewsMap = new HashMap<String, ViewTableIF>();

   MDBViewSchema(MDBSchema schema) {
      initViews(schema);
   }

   public ViewTableIF[] getStoreViews() {
      ViewTableIF[] result = new ViewTableIF[viewsMap.size()];
      Iterator iter = viewsMap.values().iterator();
      int index = 0;

      while (iter.hasNext()) {
         result[index] = (ViewTableIF) iter.next();
         index++;
      }

      return result;
   }

   public void initViews(MDBSchema schema) {
      List<Query> queries;

      try {
         queries = schema.getDatabase().getQueries();
      } catch (Exception ex) {
         log.warn("MDBViewSchema: could not initialize MDB queries." +
                 " Error was " + ex.getMessage(), ex);
         return;
      }

      for (Query query : queries) {
         if (query.getType() == Query.Type.SELECT || query.getType() == Query.Type.UNION) {
            String queryText;

            try {
               queryText = query.toSQLString();
            } catch (Exception ex) {
               log.warn("MDBViewSchema: could not initialize the query '" + query.getName() +
                       "' . Error was " + ex.getMessage(), ex);
               continue;
            }

            // ignore temp views
            if (query.getName().startsWith("~")) continue;

            if (queryText.endsWith(";")) queryText = queryText.substring(0,
                    queryText.length() - 1);

            try {
               if (schema.preserveColumnNames)
                  queryText = SQLTransformer.doubleQuoteColumnNames(queryText);

               queryText = StringUtils.removeChar(queryText, "[");
               queryText = StringUtils.removeChar(queryText, "]");
            } catch (Exception ex) {
               log.warn("MDBViewSchema: error while transforming a view query '" + query.getName() +
                       "'. Error was " + ex.getMessage());
               continue;
            }

            if (log.isDebugEnabled())
               log.debug("MDBViewSchema: query '" + query.getName() + "' is initialized.");

            viewsMap.put(query.getName().toUpperCase(),
                    new DefaultViewTable(query.getName(), queryText));
         }
      }
   }

   public ViewTableIF getViewByName(String viewName) {
      return viewsMap.get(viewName.toUpperCase());
   }

   public static void main(String[] args) {
   }
}
