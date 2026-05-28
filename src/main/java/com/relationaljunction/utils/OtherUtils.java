package com.relationaljunction.utils;

import java.util.*;

import org.slf4j.Logger;

import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;

public class OtherUtils {
   public static boolean TRACE_ENABLED = false;
   public static boolean DEBUG_ENABLED_FOR_DRIVER_MANAGER = true;

   // copys values of one list to another
   public static void copyList(List l1, List l2) {
      for (int i = 0; i < l2.size(); i++) {
         l1.add(l2.get(i));
      }
   }

   // copys unique values of one list to another
   public static void copyDistinctList(List l1, List l2) {
      for (int i = 0; i < l2.size(); i++) {
         if (!l1.contains(l2.get(i)))
            l1.add(l2.get(i));
      }
   }

   // immutable copying of two lists
   public static ArrayList createCommonList(List l1, List l2) {
      ArrayList result = new ArrayList();
      for (int i = 0; i < l1.size(); i++) {
         result.add(l1.get(i));
      }
      for (int i = 0; i < l2.size(); i++) {
         result.add(l2.get(i));
      }
      return result;
   }

   // removes common values of one list from another
   public static void removeList(List l1, List l2) {
      for (int i = 0; i < l2.size(); i++) {
         l1.remove(l2.get(i));
      }
   }

   public static ArrayList createArrayList(Object[] objs) {
      ArrayList al = new ArrayList();
       Collections.addAll(al, objs);
      return al;
   }

   public static <T> SortedSet<T> getSortedSet(Set<T> set) {
      return new TreeSet<T>(set);
   }

   public static int compare(Object[] objs1, Object[] objs2) {
      int compareResult = 0;

      if (objs1.length != objs2.length)
         throw new IllegalArgumentException("Sizes are not the same");

      for (int i = 0; i < objs1.length; i++) {
         if (!objs1[i].getClass().equals(objs2[i].getClass()))
            throw new IllegalArgumentException("Classes are not the same. " +
                    objs1[i].getClass() + " and " + objs2[i].getClass() +
                    " for values: " + objs1[i] + " and " + objs2[i]);

         Comparable comp1 = (Comparable) objs1[i];
         Comparable comp2 = (Comparable) objs2[i];
         if (comp1.compareTo(comp2) > 0) return 1;
         else if (comp1.compareTo(comp2) < 0) return -1;
      }

      return compareResult;
   }

   public static int containsStringIgnoringCase(String[] array, String str) {
      for (int i = 0; i < array.length; i++) {
         if (array[i].equalsIgnoreCase(str))
            return i;
      }

      return -1;
   }

   public static String getTempFilePath(String tempPath, String tempFileName) {
      String tempFile;
      String dirPath;

      if (tempPath == null)
         // use the default temporary path in OS
         dirPath = new File(System.getProperty("java.io.tmpdir")).getPath();
      else
         dirPath = new File(tempPath).getPath();

      tempFile = dirPath + File.separator + tempFileName;
      return tempFile;
   }

   public static String formatDate(java.util.Date d) {
      if (d == null)
         return "null";

      SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
      return sdf.format(d);
   }

   public static Exception createAndLogException(Logger
                                                         log, String message, boolean out2DriverManager) {
      Exception e = new Exception(message);
      log.error(message, e);

      if (out2DriverManager)
         DriverManager.println(message);

      return e;
   }

   public static SQLException createAndLogSQLException(Logger
                                                               log, String message, boolean out2DriverManager) {
      SQLException sqlException = new SQLException(message);
      log.error(message, sqlException);

      if (out2DriverManager)
         DriverManager.println(message);

      return sqlException;
   }

   public static void writeTraceInfo(Logger log, String... messages) {
      if (!TRACE_ENABLED) return;

      if (log.isTraceEnabled()) {
         StringBuilder messageBuilder = new StringBuilder();

         for (String message : messages) {
            messageBuilder.append(message);
         }

         log.trace(messageBuilder.toString());
      }
   }

   public static void writeLogInfo(Logger log, String... messages) {
      writeLogInfo(log, DEBUG_ENABLED_FOR_DRIVER_MANAGER, messages);
   }

   public static void writeLogInfo(Logger log, boolean out2DriverManager,
                                   String... messages) {
      StringBuilder messageBuilder = null;

      if (log.isDebugEnabled()) {
         messageBuilder = initMessageBuilder(messageBuilder, messages);
         log.debug(messageBuilder.toString());
      }

      if (out2DriverManager && DriverManager.getLogWriter() != null) {
         java.util.Date time = new java.util.Date(System.currentTimeMillis());
         messageBuilder = initMessageBuilder(messageBuilder, messages);
         DriverManager.println(time + " " + messageBuilder);
      }
   }

   private static StringBuilder initMessageBuilder(StringBuilder messageBuilder, String... messages) {
      if (messageBuilder == null) {
         messageBuilder = new StringBuilder();

         for (String message : messages) {
            messageBuilder.append(message);
         }
      }

      return messageBuilder;
   }

   public static void writeWarnInfo(Logger log, String message,
                                    boolean out2DriverManager) {
      log.warn(message);

      if (out2DriverManager) {
         java.util.Date time = new java.util.Date(System.currentTimeMillis());
         DriverManager.println(time + " " + message);
      }
   }

   public static void writeWarnInfo(Logger log, String message, Exception ex,
                                    boolean out2DriverManager) {
      log.warn(message, ex);

      if (out2DriverManager) {
         java.util.Date time = new java.util.Date(System.currentTimeMillis());
         DriverManager.println(time + " " + message);
      }
   }
}
