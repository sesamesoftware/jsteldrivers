package com.relationaljunction.jdbc.csv;

import java.util.Properties;

import com.relationaljunction.jdbc.common.h2.CommonConnection2;
import com.relationaljunction.jdbc.csv.store.CSVStoreSchema;

/**
 * <p>Title: StelsCSV JDBC Driver</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author not attributable
 * @version 2.2
 */

public class CsvDriverProperties
        extends Properties {
//  Properties props = new Properties();

//  public CsvDriverProperties(){
//  }
//
//  private void setProperty(String propertyName, String value){
//    props.setProperty(propertyName, value);
//  }
//
//  private String getProperty(String propertyName){
//    return props.getProperty(propertyName);
//  }

   public void setPath(String path) {
      if (path != null)
         setProperty(CSVStoreSchema.PATH, path);
   }

   public String getPath() {
      return getProperty(CSVStoreSchema.PATH);
   }

   public String getTempPath() {
      return getProperty(CSVStoreSchema.TEMP_PATH);
   }

   public void setCharset(String charset) {
      if (charset != null)
         setProperty(CSVStoreSchema.CHARSET, charset);
   }

   public String getCharset() {
      String value = getProperty(CSVStoreSchema.CHARSET);
      return value == null ? CSVStoreSchema.DEFAULT_CHARSET : value;
   }

   public void setCommentLine(String commentLine) {
      if (commentLine != null)
         setProperty(CSVStoreSchema.COMMENT_LINE, commentLine);
   }

   public String getCommentLine() {
      String value = getProperty(CSVStoreSchema.COMMENT_LINE);
      return value == null ? CSVStoreSchema.DEFAULT_COMMENT_LINE : value;
   }

   public void setDecimalFormatInput(String decFormat) {
      if (decFormat != null)
         setProperty(CSVStoreSchema.DECIMAL_FORMAT_INPUT, decFormat);
   }

   public String getDecimalFormatInput() {
       return getProperty(CSVStoreSchema.DECIMAL_FORMAT_INPUT);
   }

   public void setDecimalFormatOutput(String decFormat) {
      if (decFormat != null)
         setProperty(CSVStoreSchema.DECIMAL_FORMAT_OUTPUT, decFormat);
   }

   public String getDecimalFormatOutput() {
      String value = getProperty(CSVStoreSchema.DECIMAL_FORMAT_OUTPUT);
      return value == null ? CSVStoreSchema.DEFAULT_DECIMAL_FORMAT_OUTPUT : value;
   }

   public void setDateFormat(String dateFormat) {
      if (dateFormat != null)
         setProperty(CSVStoreSchema.DATE_FORMAT, dateFormat);
   }

   public String getDateFormat() {
      String value = getProperty(CSVStoreSchema.DATE_FORMAT);
      return value == null ? CSVStoreSchema.DEFAULT_DATE_FORMAT : value;
   }

   public void setFixedLengthSeparator(boolean fixed) {
      if (fixed)
         setProperty(CSVStoreSchema.SEPARATOR, "fixed");
   }

   public boolean isFixedLengthSeparator() {
      String value = getProperty(CSVStoreSchema.SEPARATOR);
       return value != null && value.equals("fixed");
   }

   public void setFileExtension(String fileExtension) {
      if (fileExtension != null)
         setProperty(CSVStoreSchema.FILE_EXTENSION, fileExtension);
   }

   public String getFileExtension() {
      String value = getProperty(CSVStoreSchema.FILE_EXTENSION);
      return value == null ? CSVStoreSchema.DEFAULT_EXTENSION : value;
   }

   public void setRowDelimiter(String rowDelimiter) {
      if (rowDelimiter != null)
         setProperty(CSVStoreSchema.ROW_DELIMITER, rowDelimiter);
   }

   public String getRowDelimiter() {
      String value = getProperty(CSVStoreSchema.ROW_DELIMITER);
      return value == null ? CSVStoreSchema.DEFAULT_ROW_DELIMITER : value;
   }

   public void setSchema(String schema) {
      if (schema != null)
         setProperty(CSVStoreSchema.SCHEMA, schema);
   }

   public String getSchema() {
      String value = getProperty(CSVStoreSchema.SCHEMA);
      return value == null ? CSVStoreSchema.DEFAULT_SCHEMA : value;
   }

   public void setSeparator(String separator) {
      if (separator != null)
         setProperty(CSVStoreSchema.SEPARATOR, separator);
   }

   public String getSeparator() {
      String value = getProperty(CSVStoreSchema.SEPARATOR);
      return value == null ? CSVStoreSchema.DEFAULT_SEPARATOR : value;
   }

   public void setSuppressHeaders(boolean suppressHeaders) {
      setProperty(CSVStoreSchema.SUPPRESS_HEADERS, Boolean.toString(suppressHeaders));
   }

   public boolean isSuppressHeaders() {
      String value = getProperty(CSVStoreSchema.SUPPRESS_HEADERS);
      return value == null ? CSVStoreSchema.DEFAULT_SUPPRESS :
              Boolean.valueOf(value).booleanValue();
   }

   //############## Advanced Properties ################

   public void setDefaultColumnType(String type) {
      if (type != null)
         setProperty(CSVStoreSchema.DEFAULT_COLUMN_TYPE, type);
   }

   public String getDefaultColumnType() {
      String value = getProperty(CSVStoreSchema.DEFAULT_COLUMN_TYPE);
      return value == null ? String.valueOf(CSVStoreSchema.DEFAULT_DEFAULT_COLUMN_TYPE) :
              value;
   }

   public void setEscapeEOLInQuotes(boolean escape) {
      setProperty(CSVStoreSchema.ESCAPE_EOL_IN_QUOTES, Boolean.toString(escape));
   }

   public boolean isEscapeEOLInQuotes() {
      String value = getProperty(CSVStoreSchema.ESCAPE_EOL_IN_QUOTES);
      return value == null ? CSVStoreSchema.DEFAULT_ESCAPE_EOL_IN_QUOTES :
              Boolean.valueOf(value).booleanValue();
   }

   public void setEmptyStringIsNull(boolean yes) {
      setProperty(CSVStoreSchema.EMPTY_STRING_AS_NULL, Boolean.toString(yes));
   }

   public boolean isEmptyStringIsNull() {
      String value = getProperty(CSVStoreSchema.EMPTY_STRING_AS_NULL);
      return value == null ? CSVStoreSchema.DEFAULT_EMPTY_STRING_AS_NULL :
              Boolean.valueOf(value).booleanValue();
   }

   public void setLocale(String locale) {
      if (locale != null)
         setProperty(CSVStoreSchema.LOCALE, locale);
   }

   public String getLocale() {
      String value = getProperty(CSVStoreSchema.LOCALE);
      return value == null ? CSVStoreSchema.DEFAULT_LOCALE.toString() : value;
   }

   public void setLogPath(String logPath) {
      if (logPath != null)
         setProperty(CSVStoreSchema.LOG_PATH, logPath);
   }

   public String getLogPath() {
       return getProperty(CSVStoreSchema.LOG_PATH);
   }

   public void setNullString(String nullString) {
      if (nullString != null)
         setProperty(CSVStoreSchema.NULL_STRING, nullString);
   }

   public String getNullString() {
      String value = getProperty(CSVStoreSchema.NULL_STRING);
      return value == null ? CSVStoreSchema.DEFAULT_NULL_STRING : value;
   }

   public void setPaddingChar(String paddingChar) {
      if (paddingChar != null)
         setProperty(CSVStoreSchema.PADDING_CHAR, paddingChar);
   }

   public String getPaddingChar() {
      String value = getProperty(CSVStoreSchema.PADDING_CHAR);
      return value == null ? String.valueOf(CSVStoreSchema.DEFAULT_PADDING_CHAR) :
              value;
   }

   public void setTrimBlanks(boolean trim) {
      setProperty(CSVStoreSchema.TRIM_BLANKS, Boolean.toString(trim));
   }

   public boolean isTrimBlanks() {
      String value = getProperty(CSVStoreSchema.TRIM_BLANKS);
      return value == null ? CSVStoreSchema.DEFAULT_TRIM_BLANKS :
              Boolean.valueOf(value).booleanValue();
   }

   public void setWebParameterName(String webParam) {
      if (webParam != null)
         setProperty(CSVStoreSchema.USE_WEB_PARAM, webParam);
   }

   public String getWebParameterName() {
      String value = getProperty(CSVStoreSchema.USE_WEB_PARAM);
      return value == null ? CSVStoreSchema.DEFAULT_USE_WEB_PARAM : value;
   }

   public void setQuoteString(boolean quoteString) {
      setProperty(CSVStoreSchema.QUOTE_STRING, Boolean.toString(quoteString));
   }

   public boolean isQuoteString() {
      String value = getProperty(CSVStoreSchema.QUOTE_STRING);
      return value == null ? CSVStoreSchema.DEFAULT_QUOTE_STRING :
              Boolean.valueOf(value).booleanValue();
   }

   //############## User functions ################

   public void setUserFunction(String name, String handler) {
      setProperty(CSVStoreSchema.FUNCTION_PREFIX + name, handler);
   }

   public String getUserFunction(String name) {
       return getProperty(CSVStoreSchema.FUNCTION_PREFIX + name);
   }

   //############## Swap Properties ################
   public void setDbInMemory(boolean caching) {
      setProperty(CommonConnection2.DB_IN_MEMORY, Boolean.toString(caching));
   }

   public boolean dbInMemory() {
      String value = getProperty(CommonConnection2.DB_IN_MEMORY);
      return value == null ? CommonConnection2.DEFAULT_DB_IN_MEMORY :
              Boolean.valueOf(value).booleanValue();
   }

   public void setTempPath(String tempPath) {
      if (tempPath != null)
         setProperty(CSVStoreSchema.TEMP_PATH, tempPath);
   }

}
