# Sesame Software CSV JDBC Driver

> **Archived Repository** — This is a stable, read-only open source release. No active development, bug fixes, or security patches are planned. The driver is provided as-is for reference and integration use. See [SECURITY.md](../SECURITY.md) for known dependency advisories.

A JDBC Type 4 driver that enables SQL queries and standard JDBC operations directly against CSV, TSV, and other delimited text files.

Originally developed as **StelsCSV** by [J-Stels Software](http://www.csv-jdbc.com) (Sergey Kutsygin). Acquired by [Sesame Software](https://www.sesamesoftware.com) on December 31, 2015 and released as the Sesame Software CSV JDBC Driver.

---

## Table of Contents

- [Installation](#installation)
- [Driver Classes](#driver-classes)
- [URL Syntax](#url-syntax)
- [Driver Properties](#driver-properties)
- [Connection Example](#connection-example)
- [Driver Modes](#driver-modes)
- [Data Type Mapping](#data-type-mapping)
- [Supported SQL Syntax](#supported-sql-syntax)
- [User-Defined SQL Functions](#user-defined-sql-functions)
- [Performance and Other Hints](#performance-and-other-hints)
- [Advanced Topics](#advanced-topics)
- [Third-Party Components](#third-party-components)

---

## Installation

Add `csvdriver.jar` to your classpath, or extract the JAR into the directory of your application.

---

## Driver Classes

| Description | Class Name |
|---|---|
| Driver class (JDBC API v1.0) | `com.relationaljunction.jdbc.csv.CsvDriver2` |
| DataSource class (JDBC API v2.0) | `com.relationaljunction.jdbc.csv.CsvDataSource2` |

---

## URL Syntax

The connection URL is `jdbc:relationaljunction:csv:<csvdir>`, where `csvdir` is the path to a directory containing your delimited text files.

### Local paths

```
// Absolute or relative path to a directory containing CSV/text files
jdbc:relationaljunction:csv:c:/mydir/csvfiles
jdbc:relationaljunction:csv:csvfiles

// CLASSPATH resource directory
jdbc:relationaljunction:csv:cache://classpath://resources

// ZIP / JAR archive
jdbc:relationaljunction:csv:cache://zip://c:/mydir/csvfiles.zip
jdbc:relationaljunction:csv:cache://zip://myApp.jar/csvfiles
```

### Remote paths

```
// FTP directory
jdbc:relationaljunction:csv:ftp://login:password@somesite.com:21/csvfiles

// FTP + ZIP archive
jdbc:relationaljunction:csv:cache://zip://ftp://login:password@somesite.com:21/archives/csvfiles.zip

// SFTP (requires Commons VFS, Commons Logging, and JSch)
jdbc:relationaljunction:csv:sftp://login:password@somesite.com:22/csvfiles

// HTTP directory
jdbc:relationaljunction:csv:http://www.somesite.com/csvfiles

// SMB/CIFS share
jdbc:relationaljunction:csv:cache://smb://your_server/your_share/your_folder
jdbc:relationaljunction:csv:cache://smb://login:password@your_server/your_share/your_folder
jdbc:relationaljunction:csv:cache://smb://domain;login:password@your_server/your_share/your_folder
```

An RMI server can also be used as an alternative to FTP/SFTP/HTTP. See [Advanced Topics](#advanced-topics).

### Inline file path in SQL

A specific file path can be provided directly in a query:

```sql
SELECT * FROM "c:/csvfiles/test.csv"
SELECT * FROM "subfolder/test.txt"
SELECT * FROM "http://www.somesite.com/csvfiles/data.csv"
SELECT * FROM "ftp://login:password@somesite.com:21/csvfiles/data.csv"
```

---

## Driver Properties

### Standard Properties

| Property | Description | Default |
|---|---|---|
| `charset` | Character encoding for text files. Accepts any encoding supported by Java. | System default |
| `separator` | Field delimiter character. Use `\t` for tab-separated files. | `\t` (tab) |
| `fileExtension` | File extension for text files. If set to `.csv`, both `myTable.csv` and `myTable` are valid table names. | `.txt` |
| `suppressHeaders` | If `true`, the first row is treated as data rather than column names. Columns are named `COLUMN1`, `COLUMN2`, etc. | `false` |
| `dbInMemory`, `dbPath` | Set the driver mode. See [Driver Modes](#driver-modes). | — |
| `ignoreCase` | If `true`, string comparisons are case-insensitive. | `true` |
| `dateFormat` | Date/time format(s) separated by `\|`, e.g. `"dd.MM.yy \| dd.MM \| dd"`. See `java.util.SimpleDateFormat`. | `"yyyy-MM-dd HH:mm:ss.SSS \| yyyy-MM-dd HH:mm:ss \| yyyy-MM-dd \| HH:mm:ss.SSS \| HH:mm:ss"` |
| `logPath` | File path for the driver log. | — |

### Advanced Properties

| Property              | Description                                                                                                                                 | Default               |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------|-----------------------|
| `commentLine`         | Lines beginning with this string are skipped during parsing.                                                                                | —                     |
| `decimalFormatInput`  | Input format(s) for numbers, e.g. `"###,###.##$"`. Multiple formats separated by `\|`.                                                      | —                     |
| `decimalFormatOutput` | Output format for numbers, e.g. `"###,###.##$"`.                                                                                            | —                     |
| `emptyStringAsNull`   | If `true`, empty fields are treated as NULL.                                                                                                | `true`                |
| `escapeEOLInQuotes`   | If `true`, newlines within quoted fields are preserved correctly. Enable when your data contains multi-line values.                         | `false`               |
| `locale`              | Locale used for number and date formatting, e.g. `en_US`.                                                                                   | System default        |
| `nullString`          | Regex pattern representing NULL values in the file.                                                                                         | `(?i)null`            |
| `preSQL`              | Path to a SQL script executed after the connection is created, e.g. `c:/sql_script.txt`.                                                    | —                     |
| `rowDelimiter`        | Line ending used in output files.                                                                                                           | System line separator |
| `singletonConnection` | If `true`, one `Connection` instance per unique JDBC URL, shared across threads. Recommended for app servers. Issue `SHUTDOWN` on undeploy. | `false`               |
| `trimBlanks`          | If `true`, trims leading/trailing whitespace from field values when reading.                                                                | `true`                |

### Setting Driver Properties

**1) Properties object:**

```java
java.util.Properties props = new java.util.Properties();
props.put("separator",  ",");          // comma-separated
props.put("charset",    "UTF-8");      // file encoding
props.put("fileExtension", ".csv");    // file extension

Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:csv:c:/csvfiles", props);
```

**2) DataSource class:**

```java
CsvDataSource2 csvDS = new CsvDataSource2();
csvDS.setPath("c:/csvfiles");
csvDS.setSeparator(",");
csvDS.setCharset("UTF-8");

Connection conn = csvDS.getConnection();
```

**3) Appended to the URL** (separate multiple with `&` or `!`):

```java
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:csv:c:/csvfiles?separator=,&charset=UTF-8&fileExtension=.csv");
```

---

## Connection Example

```java
import java.sql.*;

public class CsvDriverTest {

    public static void main(String[] args) {
        try {
            // Load the driver
            Class.forName("com.relationaljunction.jdbc.csv.CsvDriver2");

            // Connect — first arg is the directory containing text/CSV files
            Connection conn = DriverManager.getConnection(
                "jdbc:relationaljunction:csv:" + args[0] + "?separator=,&fileExtension=.csv");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM \"test.csv\"");

            // Print column headers
            for (int j = 1; j <= rs.getMetaData().getColumnCount(); j++) {
                System.out.print(rs.getMetaData().getColumnName(j) + "\t");
            }
            System.out.println();

            // Print rows
            while (rs.next()) {
                for (int j = 1; j <= rs.getMetaData().getColumnCount(); j++) {
                    System.out.print(rs.getObject(j) + "\t");
                }
                System.out.println();
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## Driver Modes

The driver loads CSV data into an intermediate H2 database called the **synchrobase**, which handles SQL queries and synchronizes changes back to the source files.

### Mode 1: Temporary synchrobase in RAM *(default)*

Created in RAM, removed when the connection is closed. Best performance.

> The JVM must have enough heap for large tables. Use `-Xms` / `-Xmx` JVM options. Use `DROP TABLE <name> FROM CACHE` to release specific tables. Enable `watchFileModifications` if external processes modify your CSV files.

### Mode 2: Temporary synchrobase on disk

Written to disk on connect, deleted on close. Use for large files that exceed available RAM.

```java
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:csv:c:/csvfiles?dbInMemory=false&tempPath=c:/tempfiles");
```

### Mode 3: Persistent synchrobase on disk

Created once, reused across connections. Best for production where startup latency matters.

```java
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:csv:c:/csvfiles" +
    "?dbPath=c:/synchrobases/syncro_db_name&tempPath=c:/tempfiles");
```

`tempPath` — directory for temporary files. Defaults to the OS temp dir (`java.io.tmpdir`). A fast SSD is recommended.

```java
Properties props = new java.util.Properties();
props.setProperty("dbInMemory", "false"); // Mode 2: disk-based synchrobase
props.setProperty("tempPath",   "c:/temp");
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:csv:c:/csvfiles", props);
```

---

## Data Type Mapping

Column types in CSV files are defined when creating a table or inferred from the data. The driver uses H2 as its SQL engine and supports the following types:

| RJ CSV Type | JDBC Type (`java.sql.Types.*`) | Java Class |
|---|---|---|
| `INTEGER`, `INT` | `INTEGER` | `java.lang.Integer` |
| `BIGINT`, `LONG` | `BIGINT` | `java.lang.Long` |
| `FLOAT` | `FLOAT` | `java.lang.Float` |
| `DOUBLE` | `DOUBLE` | `java.lang.Double` |
| `NUMERIC`, `DECIMAL` | `NUMERIC` | `java.math.BigDecimal` |
| `VARCHAR`, `STRING` | `VARCHAR` | `java.lang.String` |
| `DATETIME`, `TIMESTAMP` | `TIMESTAMP` | `java.util.Date` |
| `DATE` | `DATE` | `java.sql.Date` |
| `TIME` | `TIME` | `java.sql.Time` |
| `BOOLEAN` | `BOOLEAN` | `java.lang.Boolean` |

> **Note:** Use `NUMERIC`/`DECIMAL` rather than `DOUBLE` or `FLOAT` for currency values to avoid floating-point rounding errors.

Example `CREATE TABLE`:

```sql
CREATE TABLE test (
    id        INTEGER,
    name      VARCHAR(50),
    amount    NUMERIC(15,2),
    hire_date DATETIME,
    active    BOOLEAN
);
```

---

## Supported SQL Syntax

The driver uses H2 as its SQL engine and supports the majority of ANSI/ISO SQL grammar, including `SELECT`, `INSERT`, `UPDATE`, `DELETE`, and `CREATE`.

### Requirements

- Column names that are SQL reserved words, or contain spaces or special characters, must be enclosed in double quotes: `SELECT "Date", "My column" FROM "test.csv"`
- To include a single quote in a string constant, duplicate it: `SELECT 'it''s fine'`

### Examples

```sql
-- SELECT
SELECT SUM(amount) AS total, MAX(hire_date) AS latest
    FROM "employees.csv" GROUP BY department HAVING AVG(amount) > 30

SELECT name FROM "salesreps.csv"
    WHERE (rep_office IN (22, 11, 12))
       OR (manager IS NULL AND hire_date >= to_date('01-05-2002','dd-MM-yyyy'))

-- JOINs
SELECT * FROM "prices.csv" ps
    JOIN "regions.csv"  regs ON ps.regionid = regs.id
    JOIN "products.csv" prod ON prod.prodid  = ps.prodid

-- INSERT / UPDATE / DELETE
INSERT INTO "salesreps.csv" (name, age, sales, title)
    VALUES ('Henry Smith', 35, NULL, 'Sales Mgr')

DELETE FROM "salesreps.csv" WHERE name LIKE 'Henry%'

UPDATE "customers.csv"
    SET credit_limit = 50000.00 WHERE company = 'Acme Mfg.'

-- CREATE TABLE and INDEX
CREATE TABLE "new_table.csv" (
    id        INTEGER,
    name      VARCHAR(50),
    amount    NUMERIC(15,2),
    hire_date DATETIME,
    active    BOOLEAN
);

CREATE INDEX i_1 ON "new_table.csv" (id);
```

For full syntax reference see [H2 SQL Grammar](https://h2database.com/html/grammar.html).

---

## User-Defined SQL Functions

You can register custom Java methods as SQL functions callable within queries.

### Step 1: Create a static method

```java
package my_pack;

public class MyFuncs {

    // Formats a Date into a string using the specified format pattern
    public static String format_date(java.util.Date d, String format) {
        if (d == null || format == null) return null;
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format);
        return sdf.format(d);
    }
}
```

Rules:
- Both the class and method must be `public`.
- Arguments must match the Java types listed in the [Data Type Mapping](#data-type-mapping) table.
- Handle `null` arguments explicitly. The method may also return `null`.

### Step 2: Register the function

```sql
CREATE ALIAS IF NOT EXISTS format_date
    FOR "my_pack.MyFuncs.format_date(java.util.Date, java.lang.String)";
```

Or via driver property:

```java
Properties props = new java.util.Properties();
props.put("function:format_date", "my_pack.MyFuncs.format_date");
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:csv:c:/csvfiles", props);

// Or appended to the URL:
Connection conn2 = DriverManager.getConnection(
    "jdbc:relationaljunction:csv:c:/csvfiles"
    + "?function:format_date=my_pack.MyFuncs.format_date");
```

### Step 3: Call the function in SQL

```java
Statement st = connection.createStatement();
st.execute("SELECT format_date(hire_date, 'yyyy-MM-dd') FROM \"employees.csv\"");
```

---

## Performance and Other Hints

- Use `java.sql.PreparedStatement` for all `SELECT`, `INSERT`, `UPDATE`, and `DELETE` operations wherever possible.
- Use batch operations for bulk inserts:

```java
PreparedStatement pst = conn.prepareStatement(
    "INSERT INTO test(id, name) VALUES(?,?)");
for (int i = 0; i < 10000; i++) {
    pst.setInt(1, i);
    pst.setString(2, "string " + i);
    pst.addBatch();
}
pst.executeBatch();
```

- Create indexes using `CREATE INDEX` to improve query performance.
- Reuse the same `java.sql.Connection` across multiple threads.
- Set `singletonConnection=true` for application servers (Tomcat, GlassFish, WebSphere, etc.).
- For tables larger than ~100 MB, use a disk-based synchrobase. See [Driver Modes](#driver-modes).
- Use `org.hibernate.dialect.H2Dialect` in Hibernate configurations.
- Always close `ResultSet`, `Statement`, and `Connection` when finished.
- Use a current JVM. JDK 17+ offers significant performance improvements over older runtimes.

---

## Advanced Topics

### Client/Server Mode via RMI (JDBC Type 3)

The driver supports client/server operation over RMI, enabling remote access to CSV files without copying them to the client machine. See the Sesame Software CSV RMI documentation for full setup instructions.

### See Also

- [H2 Database — SQL Grammar](https://h2database.com/html/grammar.html)
- [H2 Database — SQL Functions](https://h2database.com/html/functions.html)
- [H2 Database — Full Documentation](https://h2database.com/html/main.html)
- [java.util.SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html)
- [java.text.DecimalFormat](https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html)

---

## Third-Party Components

This driver bundles the following open-source components:

| Component | License | Notes |
|---|---|---|
| H2 Database | MPL 2.0 / EPL 1.0 | SQL engine and synchrobase |
| JCIFS | LGPL 2.1 | SMB/CIFS network access |
| JSch | BSD | SFTP support |
| Apache Commons Net | Apache 2.0 | FTP support |
| Apache Commons VFS | Apache 2.0 | Virtual file system |
| Apache Commons Logging | Apache 2.0 | Logging |
