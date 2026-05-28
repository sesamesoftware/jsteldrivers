package com.relationaljunction.utils;

import java.text.*;
import java.util.*;

public class DecimalFormatter {
   private DecimalFormat[] formats = null;
   private DecimalFormat formatOut = null;
   private String formatStr = null;
   private String formatStrOut = null;


   public DecimalFormatter(String formatStr, String formatStrOut, Locale locale) {
      this.formatStr = formatStr;
      this.formatStrOut = formatStrOut;

      // parse an input format. It may be multiple formats separated by a bar.
      StringTokenizer tokenizer = new StringTokenizer(formatStr, "|");
      this.formats = new DecimalFormat[tokenizer.countTokens()];
      if (formats.length == 0) throw new IllegalArgumentException(
              "No tokens found while parsing the input decimal format = '" + formatStr + "'");

      for (int i = 0; i < this.formats.length; i++) {
         formats[i] = (DecimalFormat) NumberFormat.getNumberInstance(locale);
         formats[i].applyPattern(tokenizer.nextToken().trim());
      }

      // parse an output format. It should be single.
      if (formatStrOut != null && !formatStrOut.trim().isEmpty()) {
         this.formatOut = (DecimalFormat) NumberFormat.getNumberInstance(locale);
         this.formatOut.applyPattern(formatStrOut);
      } else {
         // the output format is not defined. So by default the output format will be the first input one.
         this.formatOut = formats[0];
      }
   }

   public Number parseDecimal(String doubleFormatted) throws Exception {
      if (doubleFormatted == null) return null;

      Number n = null;
      Exception lastException = null;

      // parse using specified formats
      for (DecimalFormat format : this.formats) {
         try {
            n = format.parse(doubleFormatted);
            break;
         } catch (Exception ex) {
            lastException = ex;
         }
      }

      if (n == null)
         throw new Exception("Can't parse the value {" + doubleFormatted +
                 "} by using the decimal format = '" + formatStr + "'." +
                 (lastException != null ? (" The last error was: " + lastException.getMessage()) : ""));
      return n;
   }

   public void setParseBigDecimal(boolean newValue) {
      for (DecimalFormat format : formats) {
         format.setParseBigDecimal(true);
      }
   }

   public String formatDecimal(Object obj) throws Exception {
      return formatOut.format(obj);
   }

   public String getDecimalFormat() {
      return formatStr;
   }

   public String getOutputDecimalFormat() {
      return formatStrOut;
   }

   public static void main(String[] args) throws Throwable {

//    System.out.println(new Locale());

      Locale localeUS = new Locale("en", "US");
      DecimalFormat dfUS = (DecimalFormat) NumberFormat.getNumberInstance(localeUS);
      dfUS.applyPattern("###,###.##'$'");

      DecimalFormatSymbols dfs = new DecimalFormatSymbols();
      dfs.setGroupingSeparator(' ');
      Locale localeRu = Locale.getDefault();
      DecimalFormat dfRU = (DecimalFormat) NumberFormat.getNumberInstance(localeRu);
//    DecimalFormat dfRU = new DecimalFormat("###,###.##'$'", dfs);
      dfRU.setDecimalFormatSymbols(dfs);
      dfRU.applyPattern("###,###.##'$'");

//    java.util.Currency  cur;
//    NumberFormat.getCurrencyInstance();

//    System.out.println(dfRU.getDecimalFormatSymbols().getGroupingSeparator() ==
//                       '\u00A0');
//    System.out.println(dfRU.getDecimalFormatSymbols().getCurrencySymbol());

      DecimalFormat df = new DecimalFormat("###,###.00'$'");
      String formattedStringRU = "321 345,54$";
      String formattedStringUS = "321,345.54$";
      try {
         System.out.println("US FORMAT: " + dfUS.format(321345.5457));
         System.out.println("US PARSING:" + dfUS.parse(formattedStringUS));
         System.out.println("RUS FORMAT: " + dfRU.format(321345.5457));
         System.out.println("RUS PARSING:" + dfRU.parse(formattedStringRU));
      } catch (Exception ex) {
         ex.printStackTrace();
      }

      DecimalFormatter decimalFormatter =
              new DecimalFormatter("###,###.##", null, new Locale("en", "US"));

      System.out.println(decimalFormatter.parseDecimal("-5.121224565711E10"));
      System.out.println(decimalFormatter.formatDecimal(-5.121224565711E10));

   }

}
