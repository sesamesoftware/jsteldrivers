package com.relationaljunction.jdbc.csv;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.jdbc.common.h2.CommonConnection2;
import com.relationaljunction.jdbc.csv.store.CSVStoreSchema;

public class CsvDataSource2
//    extends CsvDriverProperties
        implements DataSource, java.io.Serializable, Referenceable {
   private final Logger log = LoggerFactory.getLogger("CsvDataSource2");

   private String description = null;
   private String path = null;
   private final CsvDriverProperties csvProps = new CsvDriverProperties();
   CsvDriver2 driver = null;

   public CsvDataSource2() {
      this.driver = new CsvDriver2();
   }

   public Connection getConnection() throws SQLException {
      if (getPath() == null)
         throw new SQLException("You must set a path to CSV directory! Use setPath() method.");
      if (log.isDebugEnabled())
         log.debug("Relational Junction CSV Driver -> Data Source " + getProperties() +
                 ", java.sql.Driver=[" + driver.toString() +
                 "] -> getConnection()");
      return driver.connect(driver.getURLPrefix() + getPath(), csvProps);
   }

   public Connection getConnection(String userName, String passw) throws SQLException {
      return getConnection();
   }

   public PrintWriter getLogWriter() throws SQLException {
      return DriverManager.getLogWriter();
   }

   public void setLogWriter(PrintWriter parm1) throws SQLException {
      DriverManager.setLogWriter(parm1);
   }

   public void setLoginTimeout(int parm1) throws SQLException {
   }

   public int getLoginTimeout() throws SQLException {
      return 0;
   }

   public Reference getReference() throws javax.naming.NamingException {
      Reference ref = new Reference(this.getClass().getName(),
              "com.relationaljunction.jdbc.csv.CsvObjectFactory2", null);
      ref.add(new StringRefAddr(CSVStoreSchema.PATH, getPath()));
      ref.add(new StringRefAddr(CSVStoreSchema.CHARSET, getCharset()));
      ref.add(new StringRefAddr(CSVStoreSchema.COMMENT_LINE, getCommentLine()));
      ref.add(new StringRefAddr(CSVStoreSchema.DECIMAL_FORMAT_INPUT, getDecimalFormatInput()));
      ref.add(new StringRefAddr(CSVStoreSchema.DECIMAL_FORMAT_OUTPUT, getDecimalFormatOutput()));
      ref.add(new StringRefAddr(CSVStoreSchema.DATE_FORMAT, getDateFormat()));
      ref.add(new StringRefAddr(CSVStoreSchema.SEPARATOR, getSeparator()));
      ref.add(new StringRefAddr(CSVStoreSchema.FILE_EXTENSION, getFileExtension()));
      ref.add(new StringRefAddr(CSVStoreSchema.ROW_DELIMITER, getRowDelimiter()));
      ref.add(new StringRefAddr(CSVStoreSchema.SCHEMA, getSchema()));
      ref.add(new StringRefAddr(CSVStoreSchema.ESCAPE_EOL_IN_QUOTES,
              String.valueOf(isEscapeEOLInQuotes())));
      ref.add(new StringRefAddr(CSVStoreSchema.EMPTY_STRING_AS_NULL,
              String.valueOf(isEmptyStringIsNull())));
      ref.add(new StringRefAddr(CSVStoreSchema.LOCALE, getLocale()));
      ref.add(new StringRefAddr(CSVStoreSchema.LOG_PATH, getLogPath()));
      ref.add(new StringRefAddr(CSVStoreSchema.NULL_STRING, getNullString()));
      ref.add(new StringRefAddr(CSVStoreSchema.PADDING_CHAR, getPaddingChar()));
      ref.add(new StringRefAddr(CSVStoreSchema.SUPPRESS_HEADERS,
              String.valueOf(isSuppressHeaders())));
      ref.add(new StringRefAddr(CSVStoreSchema.TRIM_BLANKS, String.valueOf(isTrimBlanks())));
      ref.add(new StringRefAddr(CSVStoreSchema.USE_WEB_PARAM, getWebParameterName()));
      ref.add(new StringRefAddr(CSVStoreSchema.QUOTE_STRING, String.valueOf(isQuoteString())));
      ref.add(new StringRefAddr(CommonConnection2.DB_IN_MEMORY, String.valueOf(isDbInMemory())));
      ref.add(new StringRefAddr(CSVStoreSchema.TEMP_PATH, getTempPath()));

      return ref;
   }

   //  ######### Other methods #################

   public Properties getProperties() {
      return csvProps;
   }

   public void reset() {
      path = null;
      csvProps.clear();
   }

   //  ######### Description #################

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   //  ######### Path #################

   public String getPath() {
      return csvProps.getPath();
//    return path;
   }

   public void setPath(String path) throws Exception {
      if (path == null || path.trim().isEmpty()) throw new Exception(
              "Path must not be null or empty!");
      csvProps.setPath(path);

//    this.path = path;
   }

   // ######### Properties #################

   public void setCharset(String charset) {
      csvProps.setCharset(charset);
   }

   public String getCharset() {
      return csvProps.getCharset();
   }

   public void setCommentLine(String commentLine) {
      csvProps.setCommentLine(commentLine);
   }

   public String getCommentLine() {
      return csvProps.getCommentLine();
   }

   public void setDecimalFormatInput(String decFormat) {
      csvProps.setDecimalFormatInput(decFormat);
   }

   public String getDecimalFormatInput() {
      return csvProps.getDecimalFormatInput();
   }

   public void setDecimalFormatOutput(String decFormat) {
      csvProps.setDecimalFormatOutput(decFormat);
   }

   public String getDecimalFormatOutput() {
      return csvProps.getDecimalFormatOutput();
   }

   public void setDateFormat(String dateFormat) {
      csvProps.setDateFormat(dateFormat);
   }

   public String getDateFormat() {
      return csvProps.getDateFormat();
   }

   public void setFixedLengthSeparator(boolean fixed) {
      csvProps.setFixedLengthSeparator(fixed);
   }

   public boolean isFixedLengthSeparator() {
      return csvProps.isFixedLengthSeparator();
   }

   public void setFileExtension(String fileExtension) {
      csvProps.setFileExtension(fileExtension);
   }

   public String getFileExtension() {
      return csvProps.getFileExtension();
   }

   public void setRowDelimiter(String rowDelimiter) {
      csvProps.setRowDelimiter(rowDelimiter);
   }

   public String getRowDelimiter() {
      return csvProps.getRowDelimiter();
   }

   public void setSchema(String schema) {
      csvProps.setSchema(schema);
   }

   public String getSchema() {
      return csvProps.getSchema();
   }

   public void setSeparator(String separator) {
      csvProps.setSeparator(separator);
   }

   public String getSeparator() {
      return csvProps.getSeparator();
   }

   public void setSuppressHeaders(boolean suppressHeaders) {
      csvProps.setSuppressHeaders(suppressHeaders);
   }

   public boolean isSuppressHeaders() {
      return csvProps.isSuppressHeaders();
   }

   //############## Advanced Properties ################

   public void setEscapeEOLInQuotes(boolean escape) {
      csvProps.setEscapeEOLInQuotes(escape);
   }

   public boolean isEscapeEOLInQuotes() {
      return csvProps.isEscapeEOLInQuotes();
   }

   public void setEmptyStringIsNull(boolean yes) {
      csvProps.setEmptyStringIsNull(yes);
   }

   public boolean isEmptyStringIsNull() {
      return csvProps.isEmptyStringIsNull();
   }

   public void setLocale(String locale) {
      csvProps.setLocale(locale);
   }

   public String getLocale() {
      return csvProps.getLocale();
   }

   public void setLogPath(String logPath) {
      csvProps.setLogPath(logPath);
   }

   public String getLogPath() {
      return csvProps.getLogPath();
   }

   public void setNullString(String nullString) {
      csvProps.setNullString(nullString);
   }

   public String getNullString() {
      return csvProps.getNullString();
   }

   public void setPaddingChar(String paddingChar) {
      csvProps.setPaddingChar(paddingChar);
   }

   public String getPaddingChar() {
      return csvProps.getPaddingChar();
   }

   public void setTrimBlanks(boolean trim) {
      csvProps.setTrimBlanks(trim);
   }

   public boolean isTrimBlanks() {
      return csvProps.isTrimBlanks();
   }

   public void setWebParameterName(String webParam) {
      csvProps.setWebParameterName(webParam);
   }

   public String getWebParameterName() {
      return csvProps.getWebParameterName();
   }

   public void setQuoteString(boolean quoteString) {
      csvProps.setQuoteString(quoteString);
   }

   public boolean isQuoteString() {
      return csvProps.isQuoteString();
   }

   //############## User functions ################

   public void setUserFunction(String name, String handler) {
      csvProps.setUserFunction(name, handler);
   }

   public String getUserFunction(String name) {
      return csvProps.getUserFunction(name);
   }

   //############## Swap Properties ################
   public void setDbInMemory(boolean caching) {
      csvProps.setDbInMemory(caching);
   }

   public boolean isDbInMemory() {
      return csvProps.dbInMemory();
   }

   public void setTempPath(String tempPath) {
      csvProps.setTempPath(tempPath);
   }

   public String getTempPath() {
      return csvProps.getTempPath();
   }

   // --- JDK 1.6 ---

   public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new UnsupportedOperationException();
   }

   public boolean isWrapperFor(Class<?> iface) throws SQLException {
      throw new UnsupportedOperationException();
   }

@Override
public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
	// TODO Auto-generated method stub
	return null;
}
}
