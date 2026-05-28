package com.relationaljunction.database.view;

import java.util.Set;

public class DefaultViewTable
        implements ViewTableIF {
   private final String name;
    private final String query;
   // real tables (not nother views) used in a view
   private Set<String> realTablesUsedInView;

   public DefaultViewTable(String name, String query) {
      this.name = name;
      this.query = query;
   }

   public DefaultViewTable(String name, String query, Set<String> realTablesUsed) {
      this.name = name;
      this.query = query;
      this.realTablesUsedInView = realTablesUsed;
   }

   public String getName() {
      return name;
   }

   public String getQuery() {
      return query;
   }

   public Set<String> getRealTablesUsedInView() {
      return realTablesUsedInView;
   }
}
