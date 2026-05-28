package com.relationaljunction.database.index;

public class TablesRelationship {
  String fromTable, toTable, fromColumn, toColumn;
  String fromRelationshipName;
  String toRelationshipName;

  public TablesRelationship(String pFromTable, String toTable,
                            String pFromColumn,
                            String pToColumn) {
    this.fromTable = pFromTable;
    this.toTable = toTable;
    this.fromColumn = pFromColumn;
    this.toColumn = pToColumn;

    fromRelationshipName = "EXPORTED_RELATIONSHIP_" +
                           com.relationaljunction.utils.StringUtils.
                           replaceReservedChars(com.relationaljunction.utils.StringUtils.
                                   unDoubleQuote(pFromTable).toUpperCase()) +
                           "_" +
                           com.relationaljunction.utils.StringUtils.replaceReservedChars(com.relationaljunction.utils.StringUtils.
                                   unDoubleQuote(toTable).toUpperCase()) + "_" +
                           com.relationaljunction.utils.StringUtils.replaceReservedChars(com.relationaljunction.utils.StringUtils.
                                   unDoubleQuote(pFromColumn).toUpperCase());
    toRelationshipName = "IMPORTED_RELATIONSHIP_" +
                         com.relationaljunction.utils.StringUtils.
                         replaceReservedChars(com.relationaljunction.utils.StringUtils.unDoubleQuote(
                                 pFromTable).toUpperCase()) +
                         "_" +
                         com.relationaljunction.utils.StringUtils.replaceReservedChars(com.relationaljunction.utils.StringUtils.unDoubleQuote(toTable).toUpperCase()) + "_" +
                         com.relationaljunction.utils.StringUtils.replaceReservedChars(com.relationaljunction.utils.StringUtils.unDoubleQuote(pFromColumn).toUpperCase());
  }

  public String getFromRelationshipName() {
    return fromRelationshipName;
  }

  public String getToRelationshipName() {
    return toRelationshipName;
  }

  public String getFromColumn() {
    return fromColumn;
  }

  public String getFromTable() {
    return fromTable;
  }

  public String getToColumn() {
    return toColumn;
  }

  public String getToTable() {
    return toTable;
  }
}
