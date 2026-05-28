package com.relationaljunction.utils;

import java.util.*;
import java.text.*;

/**
 * <p>Title: StelsCSV JDBC driver</p>
 * <p>Copyright: Copyright (c) J-Stels Software 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author J-Stels Software
 * @version 2.0
 */

public class DateFormatter {
   private final SimpleDateFormat[] formats;
   private String formatStr = null;
   public static String DEFAULT_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss.SSS | " +
           "yyyy-MM-dd HH:mm:ss | " + "yyyy-MM-dd | " + "HH:mm:ss.SSS | " +
           "HH:mm:ss";

   public DateFormatter() {
      this(DEFAULT_FORMAT_STRING, Locale.getDefault());
   }

   public DateFormatter(String formatStr, Locale locale) {
      this.formatStr = formatStr;

      StringTokenizer tokenizer = new StringTokenizer(formatStr, "|");
      this.formats = new SimpleDateFormat[tokenizer.countTokens()];
      if (formats.length == 0) throw new IllegalArgumentException(
              "No tokens found while parsing the date format = '" + formatStr + "'");

      for (int i = 0; i < this.formats.length; i++) {
         formats[i] = new SimpleDateFormat(tokenizer.nextToken().trim(), locale);
      }
   }

   public Date parseDate(String dateStr) throws Exception {
      if (dateStr == null) return null;

      Date date = null;
      Exception lastException = null;

      for (SimpleDateFormat format : formats) {
         try {
            date = format.parse(dateStr);
            break;
         } catch (Exception ex) {
            lastException = ex;
         }
      }
      if (date == null)
         throw new Exception("Can't parse the date '" + dateStr +
                 "' by using the date format = '" + formatStr + "'." +
                 (lastException != null ? (" The last error was: " + lastException.getMessage()) : ""));
      return date;
   }

   public String format(Date obj) {
      return formats[0].format(obj);
   }

   public String getDateFormatString() {
      return formatStr;
   }

   public static void main(String[] args) throws Exception {
      DateFormatter cdf = new DateFormatter();
      Date date = cdf.parseDate("2003-08-20 15:21:31.123");
      //Date date = cdf.parseDate("2003-08-20");
      //Date date = cdf.parseDate("15:21:31.123");
      System.out.println("The data: " + date.toString());
   }
}
