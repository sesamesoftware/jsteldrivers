package com.relationaljunction.database.view;

import java.util.Set;

public interface ViewTableIF {

   String getName();

   String getQuery();

   Set<String> getRealTablesUsedInView();

}
