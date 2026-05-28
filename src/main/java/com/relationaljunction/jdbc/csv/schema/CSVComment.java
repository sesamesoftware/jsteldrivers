package com.relationaljunction.jdbc.csv.schema;

public class CSVComment {
   private final String comment;
   private int pos = -1;

   public CSVComment(String comment, int pos) {
      this.comment = comment;
      this.pos = pos;
   }

   public String getComment() {
      return comment;
   }

   public int getPos() {
      return pos;
   }
}
