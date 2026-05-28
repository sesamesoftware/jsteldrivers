package com.relationaljunction.utils;

import com.relationaljunction.database.dbf.DBFUtils;

import java.util.regex.*;
import java.io.*;
import java.util.*;

/**
 * <p>Title: StelsCSV JDBC driver</p>
 * <p>Copyright: Copyright (c) J-Stels Software 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author J-Stels Software
 * @version 2.0
 */

public class StringUtils {
   // pattern {@somestring.somestring.somestring}
   private final static Pattern PARAMETER_PATTERN = Pattern.compile("\\{@([\\w|\\.]+)\\}");
   private final static Pattern DUP_DOUBLE_QUOTE_PATTERN = Pattern.compile("\"\"");
   private static final Random RANDOM = new Random();
   private static final char[] HEX = "0123456789abcdef".toCharArray();
   private final static char[] DOUBLE_QUOTE = {
           '"'};
   public final static char[] QUOTE_CHARS = new char[]{
           '"', '!', '`'};
   public final static char[] SQL_RESERVED_CHARS = new char[]{
           ' ', '.', ',', ';', ':',
           '@', '"', '\'', '%', '$', '!', '\t', '\n', '\r', '(',
           ')', '{', '}', '+', '-', '*', '/',
           '>', '<', '=', '/', '#'};
   private final static String[] SQL_RESERVED_KEYWORDS = new String[]{
           "EXISTS", "MINUS", "NATURAL", "FOREIGN", "ON", "CONSTRAINT", "ORDER",
           "SELECT", "UNION", "DISTINCT", "PRIMARY", "CHECK", "NULL", "HAVING",
           "UNIQUE", "WHERE", "INNER", "EXCEPT", "FULL", "GROUP", "JOIN", "FROM",
           "IS", "LIKE", "CROSS", "NOT", "LIMIT", "OFFSET", "DELETE", "UPDATE",
           "TODAY", "INDEX", "ASC", "DESC"
   };
   private static Set<String> SQL_RESERVED_KEYWORDS_HASH = null;

   static {
      SQL_RESERVED_KEYWORDS_HASH = Collections.synchronizedSet(
              new HashSet<String>(SQL_RESERVED_KEYWORDS.length));
      Collections.addAll(SQL_RESERVED_KEYWORDS_HASH, SQL_RESERVED_KEYWORDS);
   }

   public StringUtils() {
   }

   public static String getListValuesString(List<?> values, String delimiter) {
      StringBuilder result = new StringBuilder();

      for (int i = 0; i < values.size(); i++) {
         result.append(values.get(i).toString());
         if (i < values.size() - 1) result.append(delimiter);
      }

      return result.toString();
   }

   public static String generateRandomString(int length) {
      return convertBytesToString(generateRandomBytes(length));
   }

   private static byte[] generateRandomBytes(int length) {
      if (length <= 0) {
         length = 1;
      }
      byte[] buff = new byte[length];
      RANDOM.nextBytes(buff);
      return buff;
   }

   private static String convertBytesToString(byte[] value) {
      char[] buff = new char[value.length + value.length];
      char[] hex = HEX;
      for (int i = 0; i < value.length; i++) {
         int c = value[i] & 0xff;
         buff[i + i] = hex[c >> 4];
         buff[i + i + 1] = hex[c & 0xf];
      }
      return new String(buff);
   }

   public static String substring(String str,
                                  int begLineIndex,
                                  int begColumnIndex,
                                  int endLineIndex,
                                  int endColumnIndex) throws IOException {
      int curLineIndex = 1;

      StringBuilder result = new StringBuilder();
      BufferedReader br = new BufferedReader(new StringReader(str));

      /*
         while (curLineIndex <= endLineIndex) {
           String line = br.readLine();

           if (curLineIndex == begLineIndex)
             result.append(line.substring(begColumnIndex - 1)+" ");
           else if (curLineIndex == endLineIndex)
             result.append(line.substring(0, endColumnIndex) + " ");
      else if (curLineIndex > begLineIndex && curLineIndex < endLineIndex) {
             result.append(line);
           }

           curLineIndex++;
         }
      */

      while (curLineIndex < begLineIndex) {
         br.readLine();
         curLineIndex++;
      }

      String line = br.readLine();
      curLineIndex++;

      if (begLineIndex == endLineIndex) {
         return line.substring(begColumnIndex - 1, endColumnIndex);
      }

      result.append(line.substring(begColumnIndex - 1)).append("\n");

      while (curLineIndex < endLineIndex) {
         line = br.readLine();
         result.append(line).append("\n");
         curLineIndex++;
      }

      line = br.readLine();
      result.append(line, 0, endColumnIndex);

      br.close();
      return result.toString();
   }

   public static String removeChar(String str, String ch) {
      return str.replace(ch, "");
   }

   public static String removeChars(String str, String pattern) {
      return str.replaceAll(pattern, "");
   }

   public static boolean isLike(String str, String template, char anyStringChar,
                                char anySingleChar) {
      boolean result = false;
      boolean[] lastRecord = new boolean[str.length()];

      for (int j = 0; j < template.length(); j++) {
         boolean[] curRecord = new boolean[str.length()];
         char templChar = template.charAt(j);
         for (int i = 0; i < str.length(); i++) {
            boolean diagonalCond = (i == 0 && j == 0) || (i != 0 && lastRecord[i - 1]);
            boolean verticalCond = (j != 0) && lastRecord[i];
            boolean horizontalCond = (i != 0) && curRecord[i - 1];
            boolean localResult;

            char strChar = str.charAt(i);
            if (templChar == anySingleChar)
               localResult = diagonalCond;
            else if (templChar == anyStringChar)
               localResult = horizontalCond || verticalCond || diagonalCond;
            else
               localResult = (strChar == templChar) &&
                       (diagonalCond ||
                               (j != 0 && template.charAt(j - 1) == anyStringChar));
            curRecord[i] = localResult;
         }
         boolean check = false;
         for (boolean aCurRecord : curRecord) check |= aCurRecord;
         if (!check)
            return false;

         lastRecord = curRecord;
      }
      return lastRecord[str.length() - 1];
   }

   public static boolean equalsIgnoreCase(Collection<String> coll1, Collection<String> coll2) {
      boolean result = true;

      if (coll1.size() != coll2.size()) throw new
              IllegalArgumentException("Collections sizes are not the same!");

      Iterator<String> coll2Iterator = coll2.iterator();

      for (String elem : coll1) {
         if (!elem.equalsIgnoreCase(coll2Iterator.next())) return false;
      }

      return result;
   }

   public static String replaceChars(String text, char[] chars, char newchar) {
      String result = text;
      for (char aChar : chars) {
         result = result.replace(aChar, newchar);
      }
      return result;
   }

   public static String replaceReservedChars(String text) {
      return replaceChars(text, SQL_RESERVED_CHARS, '_');
   }

   public static String replaceSubString(String text, String oldStr,
                                         String newStr) {
      int pos = text.indexOf(oldStr);
      if (pos < 0)
         return text;
      return text.substring(0, pos) + newStr + replaceSubString(
              text.substring(pos + oldStr.length()), oldStr, newStr);
   }

   public static String replace(String srcText, int begPos, int endPos,
                                String text2replace, char paddingChar) throws
           Exception {
      if (begPos < 0)
         throw new Exception("Out of bound access. Index=" + begPos);
      if (endPos < begPos)
         throw new Exception("end position < begin position");

      int size = Math.max(srcText.length(), endPos + 1);

      char[] chars = new char[size];
      if (paddingChar != '\0')
         Arrays.fill(chars, paddingChar);

      srcText.getChars(0, srcText.length(), chars, 0);

      for (int i = begPos; (i <= endPos) && ((i - begPos) < text2replace.length());
           i++)
         chars[i] = text2replace.charAt(i - begPos);

//    for (int i = endPos - text2replace.length(); i <= endPos; i++)
//      chars[i] = text2replace.charAt(endPos - text2replace.length() - i);

      return new String(chars);
   }

   public static String replaceParameters(String text, Properties parameterProperties) {
      return replaceParameters(text, PARAMETER_PATTERN, parameterProperties);
   }

   public static String replaceParameters(String text, Pattern pattern, Properties parameterProperties) {
      Matcher matcher = pattern.matcher(text);

      StringBuffer sb = new StringBuffer();
      while (matcher.find()) {
         String parameter = matcher.group();
         String value = parameterProperties.getProperty(parameter);
         if (value == null) throw new NullPointerException("No value is set for the parameter "
                 + parameter);

         matcher.appendReplacement(sb, value);
      }
      matcher.appendTail(sb);


      return sb.toString();
   }

   public static String replaceParameters(String text, Map<String, String> parameterMap) {
      return replaceParameters(text, PARAMETER_PATTERN, new ParameterReplacementHandler(parameterMap));
   }

   public static String replaceParameters(String text,
                                          ParameterReplacementHandler handler) {
      return replaceParameters(text, PARAMETER_PATTERN, handler);
   }

   public static String replaceParameters(String text,
                                          Pattern pattern,
                                          ParameterReplacementHandler handler) {
      Matcher matcher = pattern.matcher(text);

      StringBuffer sb = new StringBuffer();
      while (matcher.find()) {
         String parameterName = matcher.group();

         // be sure to escape any $ signs with
         // literal $ signs so that they are not evaluated are
         // regular expression groups.
         // so all '$' chars will be replaced to '\$' chars
         String value = handler.getStringValueForParameter(parameterName);

         if (value == null)
            throw new NullPointerException("Value of parameter '" + parameterName + "' is null");

         String escapedValue = value.replaceAll("\\$", "\\\\\\$");

         matcher.appendReplacement(sb, escapedValue);
      }
      matcher.appendTail(sb);

      return sb.toString();
   }

   public static String replaceString(String text, String patternString,
                                      String newString) {
      Pattern pattern = Pattern.compile(patternString);
      return changeAllWords(text, pattern, newString);
   }

   public static String changeAllWords(String text, Pattern pattern,
                                       String newString) {
      Matcher m = pattern.matcher(text);
      StringBuffer sb = new StringBuffer();

      while (m.find()) {
         m.appendReplacement(sb, newString);
      }

      m.appendTail(sb);
      return sb.toString();
   }

   public static boolean containsPattern(String text) {
      return containsPattern(text, PARAMETER_PATTERN);
   }

   public static boolean containsPattern(String text, Pattern pattern) {
      Matcher matcher = pattern.matcher(text);
      return matcher.find();
   }

   public static String duplicateSingleQuote(String text) {
      return replaceString(text, "'", "''");
   }

   public static String escapeChars(String text, char[] charsToEscape) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
         char ch = text.charAt(i);

         // loop on escape chars
         for (char aCharsToEscape : charsToEscape)
            if (ch == aCharsToEscape) {
               sb.append('\\');
               break;
            }

         sb.append(ch);
      }

      return sb.toString();
   }

   public static String unduplicateDoubleQuote(String text) {
      return changeAllWords(text, DUP_DOUBLE_QUOTE_PATTERN, "\"");
   }

   public static String duplicateDoubleQuote(String text) {
      return replaceString(text, "\"", "\"\"");
   }

   public static String duplicateDoubleQuote2(String text) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < text.length(); i++) {
         char ch = text.charAt(i);
         if (ch == '\"')
            sb.append("\"\"");
         else
            sb.append(ch);
      }

      return sb.toString();
   }

   public static String unduplicateChar(String text, char quoteChar) {
      return replaceString(text, "" + quoteChar + quoteChar, "" + quoteChar);
   }

   public static String getFileExtension(String fileName) {
      int dotPos = fileName.lastIndexOf('.');
      if (dotPos < 0)
         return "";
      return fileName.substring(dotPos + 1);
   }

   public static String getFileNameWithoutExtension(String fileName) {
       return DBFUtils.getFileNameWithoutExtension(fileName);
   }

   public static int getTextMaximumLineLength(String text) {
      StringTokenizer str_tok = new StringTokenizer(text, "\r\n");
      int max_len = 0;
      while (str_tok.hasMoreTokens()) {
         String line = str_tok.nextToken();
         if (line == null)
            break;
         int len = line.length();
         if (max_len < len)
            max_len = len;
      }
      return max_len;
   }

   public static String rightTrim(String str) {
      if (str == null || str.isEmpty())
         return str;

      int pos = str.length() - 1;
      while (pos >= 0 && str.charAt(pos) <= ' ')
         pos--;

      return str.substring(0, pos + 1);
   }

   public static boolean isDoubleQuoted(String str) {
      return isQuoted(str, DOUBLE_QUOTE);
   }

   public static boolean isQuoted(String str) {
      return isQuoted(str, QUOTE_CHARS);
   }

   public static boolean isQuoted(String str, char[] quoteChars) {
      if (str.length() <= 1) return false;

      boolean quoted = false;

      for (char quoteChar : quoteChars) {
         if (str.startsWith(String.valueOf(quoteChar)) &&
                 str.endsWith(String.valueOf(quoteChar))) {
            quoted = true;
            break;
         }
      }

      return quoted;
   }

   public static String unDoubleQuote(String str) {
      return unDoubleQuote(str, true);
   }

   public static String unDoubleQuote(String str, boolean unduplicateQuotes) {
      return unquote(str, DOUBLE_QUOTE, unduplicateQuotes);
   }

   public static String unquote(String str) {
      return unquote(str, true);
   }

   public static String unquote(String str, boolean unduplicateQuotes) {
      return unquote(str, QUOTE_CHARS, unduplicateQuotes);
   }

   public static String unquote(String str, char[] quoteChars) {
      return unquote(str, quoteChars, true);
   }

   public static String unquote(String str, char[] quoteChars, boolean unduplicateQuotes) {
      String result = str;

      for (char quoteChar : quoteChars) {
         if (result.startsWith(String.valueOf(quoteChar)) &&
                 result.endsWith(String.valueOf(quoteChar))) {
            result = result.substring(1);
            result = result.substring(0, result.length() - 1);

            if (unduplicateQuotes)
               result = unduplicateChar(result, quoteChar);
         }
      }

      return result;
   }

   public static String quote(String str, char[] reservedChars) {
      boolean shouldBeQuoted = false;

      for (char quoteChar : reservedChars) {
         if (str.indexOf(quoteChar) != -1) {
            shouldBeQuoted = true;
            break;
         }
      }

      if (shouldBeQuoted)
         return "\"" + duplicateDoubleQuote2(str) + "\"";
      else
         return str;
   }

   public static String quoteFieldAndTableName(String fieldName, boolean caseSensivity) {
      if (caseSensivity)
         return "\"" + StringUtils.duplicateDoubleQuote2(fieldName) + "\"";
      else
         return StringUtils.quoteReservedFieldAndTableName(fieldName);
   }

   public static String quoteReservedFieldAndTableName(String str) {
      return quoteReservedFieldAndTableName(str, SQL_RESERVED_CHARS, true);
   }

   private static String quoteReservedFieldAndTableName(String str, char[] reservedChars,
                                                        boolean checkSQLReservedWords) {
      boolean shouldBeQuoted = false;

      if (checkSQLReservedWords &&
              SQL_RESERVED_KEYWORDS_HASH.contains(str.toUpperCase()))
         return "\"" + str + "\"";

      // if it contains reserved chars
      for (char quoteChar : reservedChars) {
         if (str.indexOf(quoteChar) != -1) {
            shouldBeQuoted = true;
            break;
         }
      }

      // if it starts with number
      if (!shouldBeQuoted && str.charAt(0) >= '0' && str.charAt(0) <= '9')
         shouldBeQuoted = true;

      if (shouldBeQuoted)
         return "\"" + StringUtils.duplicateDoubleQuote2(str) + "\"";
      else
         return str.toUpperCase();
   }

   // if name in quotes -> unquote
   // if name not in quotes -> to upper case
   public static String toUpperCaseIfNotQuoted(String name) {
      if (isDoubleQuoted(name))
         return unquote(name, QUOTE_CHARS);
      else
         return unquote(name, QUOTE_CHARS).toUpperCase();
   }

   /**
    * leaves SQL reserved words as is and other converts to upper case
    */
   public static String toUpperCaseIfNotReserved(String str) {
      return toUpperCaseIfNotReserved(str, SQL_RESERVED_CHARS, true);
   }

   /**
    * leaves SQL reserved words as is and other converts to upper case
    */
   public static String toUpperCaseIfNotReserved(String str,
                                                 char[] reservedChars,
                                                 boolean checkSQLReservedWords) {
      if (checkSQLReservedWords &&
              SQL_RESERVED_KEYWORDS_HASH.contains(str.toUpperCase()))
         return str;

      // if it contains reserved chars
      for (char quoteChar : reservedChars) {
         if (str.indexOf(quoteChar) != -1) {
            return str;
         }
      }

      // if it starts with number
      if (str.charAt(0) >= '0' && str.charAt(0) <= '9')
         return str;

      return str.toUpperCase();
   }

   /**
    * double quottes SQL reserved words and other converts to upper case
    */
   public static String toUpperCaseIfNotReserved2(String str, char[] reservedChars,
                                                  boolean checkSQLReservedWords) {
      boolean shouldBeQuoted = false;

      if (checkSQLReservedWords &&
              SQL_RESERVED_KEYWORDS_HASH.contains(str.toUpperCase()))
         return "\"" + str + "\"";

      // if it contains reserved chars
      for (char quoteChar : reservedChars) {
         if (str.indexOf(quoteChar) != -1) {
            shouldBeQuoted = true;
            break;
         }
      }

      // if it starts with number
      if (!shouldBeQuoted && str.charAt(0) >= '0' && str.charAt(0) <= '9')
         shouldBeQuoted = true;

      if (shouldBeQuoted)
         return "\"" + StringUtils.duplicateDoubleQuote2(str) + "\"";
      else
         return str.toUpperCase();
   }

   public static void main(String[] args) throws Throwable {
      String text = "\" \"\"value1\"\"\"\"\"\"\"";

      System.out.println(unDoubleQuote(text));

   }

   public static class ParameterReplacementHandler {
      Map<String, String> parameters;

      public ParameterReplacementHandler() {
      }

      public ParameterReplacementHandler(Map<String, String> parameters) {
         this.parameters = parameters;
      }

      public String getStringValueForParameter(String parameterName) throws RuntimeException {
         String value = parameters.get(parameterName);

         if (value == null) return "";
         else return value;
      }
   }
}

