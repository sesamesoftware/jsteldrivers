# Sesame Software DBF JDBC Driver

> **Archived Repository** — This is a stable, read-only open source release. No active development, bug fixes, or security patches are planned. The driver is provided as-is for reference and integration use. See [SECURITY.md](SECURITY.md) for known dependency advisories.

A JDBC Type 4 driver that enables SQL queries and standard JDBC operations directly against DBF (dBASE III/IV) and Visual FoxPro (VFP) files.

Originally developed as **StelsDBF** by [J-Stels Software](http://www.csv-jdbc.com) (Sergey Kutsygin). Acquired by [Sesame Software](https://www.sesamesoftware.com) on December 31, 2015 and released as the Sesame Software DBF JDBC Driver.

> **Note:** Earlier versions of the Sesame Software website labeled this as the "CSV File JDBC Driver." These are structured binary database files (dBASE III/IV, Visual FoxPro), not plain-text CSV files.

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
- [Unsupported Features](#unsupported-features)
- [User-Defined SQL Functions](#user-defined-sql-functions)
- [Performance and Other Hints](#performance-and-other-hints)
- [Advanced Topics](#advanced-topics)

---

## Installation

Add `dbfdriver.jar` to your classpath, or extract the JAR into the directory of your application.

---

## Driver Classes

| Description | Class Name |
|---|---|
| Driver class (JDBC API v1.0) | `com.relationaljunction.jdbc.dbf.DBFDriver2` |
| DataSource class (JDBC API v2.0) | `com.relationaljunction.jdbc.dbf.DBFDataSource2` |

---

## URL Syntax

The connection URL is `jdbc:relationaljunction:dbf:<dbfdir>`, where `dbfdir` may be any of the following:

### Local paths

```
// Absolute or relative path to a directory containing DBF files
jdbc:relationaljunction:dbf:c:/mydir/dbffiles
jdbc:relationaljunction:dbf:dbffiles

// CLASSPATH resource directory
jdbc:relationaljunction:dbf:cache://classpath://resources

// ZIP / JAR archive
jdbc:relationaljunction:dbf:cache://zip://c:/mydir/dbffiles.zip
jdbc:relationaljunction:dbf:cache://zip://myApp.jar/dbffiles
```

### Remote paths

```
// FTP directory
jdbc:relationaljunction:dbf:ftp://login:password@somesite.com:21/dbffiles

// FTP + ZIP archive
jdbc:relationaljunction:dbf:cache://zip://ftp://login:password@somesite.com:21/archives/dbffiles.zip

// SFTP (requires Commons VFS, Commons Logging, and JSch)
jdbc:relationaljunction:dbf:sftp://login:password@somesite.com:22/dbffiles

// HTTP directory
jdbc:relationaljunction:dbf:http://www.somesite.com/dbffiles

// HTTP dynamic page (returns a DBF file — see useWebParam property)
jdbc:relationaljunction:dbf:http://www.somesite.com/out.jsp

// SMB/CIFS share
jdbc:relationaljunction:dbf:cache://smb://your_server/your_share/your_folder
jdbc:relationaljunction:dbf:cache://smb://login:password@your_server/your_share/your_folder
jdbc:relationaljunction:dbf:cache://smb://domain;login:password@your_server/your_share/your_folder
```

An RMI server can also be used as an alternative to FTP/SFTP/HTTP. See [Advanced Topics](#advanced-topics).

### Visual FoxPro database container (.dbc)

For VFP files, you can point to a `.dbc` database container file, which allows column names longer than 10 characters:

```
jdbc:relationaljunction:dbf:c:/mydir/database.dbc
jdbc:relationaljunction:dbf:dbffiles/database.dbc
```

### Inline file path in SQL

A specific DBF file path can be provided directly in a query:

```sql
SELECT * FROM "c:/dbffiles/test.dbf"
SELECT * FROM "subfolder/test.dbf"
SELECT * FROM "http://www.somesite.com/dbffiles/test.dbf"
SELECT * FROM "cache://zip://c:/dbffiles.zip/dir/test.dbf"
SELECT * FROM "ftp://login:password@somesite.com:21/dbffiles/test.dbf"
```

---

## Driver Properties

### Standard Properties

| Property | Description | Default |
|---|---|---|
| `charset` | Character encoding for DBF/VFP files. Accepts any encoding supported by Java. | `8859_1` |
| `dbInMemory`, `dbPath` | Set the driver mode. See [Driver Modes](#driver-modes). | — |
| `extension` | File extension for DBF files. If set to `.dbf`, both `myTable.dbf` and `myTable` are valid table names. | `.dbf` |
| `ignoreCase` | If `true`, string comparisons are case-insensitive. | `true` |
| `format` | DBF format for tables created by `CREATE TABLE`. Values: `DBASEIII` or `VFP`. | `DBASEIII` |
| `logPath` | File path for the driver log. | — |
| `watchFileModifications` | Starts a background thread that polls for external file changes every `checkPeriod` ms, refreshing the cache on change. Recommended when files are modified by external processes. | `false` |
| `checkPeriod` | Polling interval for `watchFileModifications`, in milliseconds. | `1000` |

> **Note on `extension`:** Stick to one table naming format within your SQL queries — either include the extension (`myTable.dbf`) or omit it (`myTable`). Do not mix both.

Example with `watchFileModifications`:

```
jdbc:relationaljunction:dbf:c:/dbffiles?watchFileModifications=true&checkPeriod=3600000
```

### Advanced Properties

| Property | Description | Default |
|---|---|---|
| `emptyStringAsNull` | If `true`, empty strings (`""`) are treated as NULL. | `true` |
| `lockFiles` | If `true`, uses exclusive file locks during write operations and shared locks during reads. VFP files only. | `false` |
| `memoExtension` | File extension for memo files. | `.dbt` (dBASE III/IV), `.fpt` (FoxPro/VFP) |
| `preSQL` | Path to a SQL script executed after the connection is created, e.g. `c:/sql_script.txt`. | — |
| `singletonConnection` | If `true`, one `Connection` instance per unique JDBC URL, shared across threads. Recommended for app servers. Issue `SHUTDOWN` on undeploy. | `false` |
| `trimBlanks` | If `true`, trims leading/trailing spaces from string values when reading. | `true` |
| `useBigDecimalType` | If `true`, uses `BIGDECIMAL` for all floating-point columns (`FLOAT`, `DOUBLE`, `CURRENCY`, `NUMERIC`). Recommended for currency values. See [Data Type Mapping](#data-type-mapping). | `false` |
| `useWebParam` | Name of the URL parameter used to pass the DBF table name to a dynamic server page. E.g. `tablename` causes the driver to build: `http://www.site.com/out.jsp?tablename=sometable` | — |

> **Note on `useWebParam`:** If driver properties are in the URL and the server page also has its own parameters, separate them with `??`: `jdbc:relationaljunction:dbf:http://www.site.com/out.jsp?param1=value1??useWebParam=tablename&dbInMemory=false`

### Setting Driver Properties

**1) Properties object:**

```java
java.util.Properties props = new java.util.Properties();
props.put("extension", ".db");       // file extension is .db
props.put("charset",   "ISO-8859-2"); // file encoding

Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:dbf:c:/dbffiles", props);
```

**2) DataSource class:**

```java
DBFDataSource2 dbfDS = new DBFDataSource2();
dbfDS.setPath("c:/dbffiles");      // path to DBF directory
dbfDS.setFileExtension(".db");     // file extension is .db
dbfDS.setCharset("ISO-8859-2");    // file encoding

Connection conn = dbfDS.getConnection();
```

**3) Appended to the URL** (separate multiple with `&` or `!`):

```java
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:dbf:c:/dbffiles?charset=ISO-8859-2&dbInMemory=false");
```

---

## Connection Example

```java
import java.sql.*;

public class DBFDriverTest {

    public static void main(String[] args) {
        try {
            // Load the driver
            Class.forName("com.relationaljunction.jdbc.dbf.DBFDriver2");

            // Connect — first arg is the directory containing .dbf files
            Connection conn = DriverManager.getConnection(
                "jdbc:relationaljunction:dbf:" + args[0]);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM \"test.dbf\"");

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

The driver loads DBF data into an intermediate H2 database called the **synchrobase**, which handles SQL queries and synchronizes changes back to the source files.

### Mode 1: Temporary synchrobase in RAM *(default)*

Created in RAM, removed when the connection is closed. Best performance.

> The JVM must have enough heap for large tables. Use `-Xms` / `-Xmx` JVM options. Use `DROP TABLE <name> FROM CACHE` to release specific tables. Enable `watchFileModifications` if external processes modify your DBF files.

### Mode 2: Temporary synchrobase on disk

Written to disk on connect, deleted on close. Use for large files that exceed available RAM.

```java
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:dbf:c:/dbffiles?dbInMemory=false&tempPath=c:/tempfiles");
```

### Mode 3: Persistent synchrobase on disk

Created once, reused across connections. Best for production where startup latency matters.

```java
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:dbf:c:/dbffiles" +
    "?dbPath=c:/synchrobases/syncro_db_name&tempPath=c:/tempfiles");
```

`tempPath` — directory for temporary files. Defaults to the OS temp dir (`java.io.tmpdir`). A fast SSD is recommended.

```java
Properties props = new java.util.Properties();
props.setProperty("dbInMemory", "false"); // Mode 2: disk-based synchrobase
props.setProperty("tempPath",   "c:/temp");
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:dbf:c:/dbffiles", props);
```

---

## Data Type Mapping

| RJ DBF Type | DBF / VFP Type | JDBC Type (`java.sql.Types.*`) | Java Class |
|---|---|---|---|
| `Integer` | `NUMERIC` (length ≤ 9, decimal count = 0), `INTEGER` | `INTEGER` | `java.lang.Integer` |
| `Bigint` | `NUMERIC` (length > 9, decimal count = 0) | `BIGINT` | `java.lang.Long` |
| `Double` *(default)* | `NUMERIC` (decimal count > 0), `FLOAT`, `CURRENCY`, `DOUBLE` | `DOUBLE` | `java.lang.Double` |
| `BIGDECIMAL` *(useBigDecimalType=true)* | `NUMERIC` (decimal count > 0), `FLOAT`, `CURRENCY`, `DOUBLE` | `NUMERIC` | `java.math.BigDecimal` |
| `VARCHAR` | `CHARACTER` | `VARCHAR` | `java.lang.String` |
| `DATETIME`, `DATE` | `DATE` (dBASE III/IV), `DATETIME` (VFP) | `TIMESTAMP` | `java.util.Date` |
| `BOOLEAN` | `LOGICAL` | `BOOLEAN` | `java.lang.Boolean` |
| `MEMO` | `MEMO`, `GENERAL`, `PICTURE` | `VARCHAR` | `java.lang.String` |
| `BLOB` | `GENERAL`, `PICTURE` | `BLOB` | `java.sql.Blob` |

> **Note:** When creating VFP files (`format=VFP`), the `INTEGER`, `DOUBLE`, and `DATETIME` types map to native VFP types.

Example `CREATE TABLE`:

```sql
CREATE TABLE test (
    int_col   INTEGER(5),
    long_col  BIGINT(12),
    float_col NUMERIC(15,2),
    str_col   VARCHAR(10),
    dat_col   DATETIME,
    bool_col  BOOLEAN
);
```

---

## Supported SQL Syntax

Version 5.x uses H2 as its SQL engine and supports the majority of ANSI/ISO SQL grammar, including `SELECT`, `INSERT`, `UPDATE`, `DELETE`, and `CREATE`.

### Requirements

- Column names that are SQL reserved words, or contain spaces or special characters, must be enclosed in double quotes: `SELECT "Date", "My integer-column" FROM "test.dbf"`
- To include a single quote in a string constant, duplicate it: `SELECT 'a"bcd"efgh'`

### Examples

```sql
-- SELECT
SELECT SUM(a) AS col1, MAX(b) / MAX(c) AS col2
    FROM "test.dbf" GROUP BY a HAVING AVG(a) > 30

SELECT name FROM "salesreps.dbf"
    WHERE (rep_office IN (22, 11, 12))
       OR (manager IS NULL AND hire_date >= to_date('01-05-2002','dd-MM-yyyy')
       OR (sales > quota AND NOT sales > 600000.0))

-- JOINs
SELECT * FROM "prices.dbf" ps
    JOIN "regions.dbf"  regs ON ps.regionid = regs.id
    JOIN "products.dbf" prod ON prod.prodid  = ps.prodid

-- INSERT / UPDATE / DELETE
INSERT INTO "salesreps.dbf" (name, age, empl_num, sales, title)
    VALUES ('Henry Smith', 35, 111, NULL, 'Sales Mgr')

DELETE FROM "salesreps.dbf" WHERE NAME LIKE 'Henry%'

UPDATE "customers.dbf"
    SET credit_limit = 50000.00 WHERE company = 'Acme Mfg.'

-- CREATE TABLE and INDEX
CREATE TABLE "new_table.dbf" (
    int_col INT, long_col LONG, float_col REAL, double_col DOUBLE,
    str_col VARCHAR(20), date_col DATETIME, bool_col BOOLEAN,
    num_col DECIMAL(15,2));

CREATE INDEX i_1 ON "new_table.dbf" (int_col);
```

For full syntax reference see [H2 SQL Grammar](https://h2database.com/html/grammar.html).

---

## Unsupported Features

- CDX and IDX index files are not supported.

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
    "jdbc:relationaljunction:dbf:c:/dbffiles", props);

// Or appended to the URL:
Connection conn2 = DriverManager.getConnection(
    "jdbc:relationaljunction:dbf:c:/dbffiles"
    + "?function:format_date=my_pack.MyFuncs.format_date");
```

### Step 3: Call the function in SQL

```java
Statement st = connection.createStatement();
st.execute("SELECT format_date(date_column, 'yyyy-MM-dd') FROM \"test.dbf\"");
```

---

## Performance and Other Hints

- Use `java.sql.PreparedStatement` for all `SELECT`, `INSERT`, `UPDATE`, and `DELETE` operations wherever possible.
- Use batch operations for bulk inserts:

```java
PreparedStatement pst = conn.prepareStatement(
    "INSERT INTO test(id, str) VALUES(?,?)");
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

The driver supports client/server operation over RMI, enabling remote access to DBF files without copying them to the client machine. See the Sesame Software DBF RMI documentation for full setup instructions.

### See Also

- [H2 Database — SQL Grammar](https://h2database.com/html/grammar.html)
- [H2 Database — SQL Functions](https://h2database.com/html/functions.html)
- [H2 Database — Full Documentation](https://h2database.com/html/main.html)
