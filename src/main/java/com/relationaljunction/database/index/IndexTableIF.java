package com.relationaljunction.database.index;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public interface IndexTableIF {

   String getIndexedTable();

   IndexTableIF getReferencedIndex();

   String getIndexName();

   String getSQLString(boolean preserveColumnNames);

   String getFieldsCommaListString(boolean preserveColumnNames);

   IndexFieldIF[] getIndexFields();

   int[] getIndexFieldsPositions();

   boolean isPrimaryKey();

   boolean isForeignKey();

   boolean isUnique();

   boolean isAutonumber();

   boolean isAllFieldsAscending();

   boolean allowNulls();
}
