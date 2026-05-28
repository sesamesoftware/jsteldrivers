# Sesame Software MDB JDBC Driver

> **Archived Repository** — This is a stable, read-only open source release. No active development, bug fixes, or security patches are planned. The driver is provided as-is for reference and integration use. See [SECURITY.md](../SECURITY.md) for known dependency advisories.

A JDBC Type 4 driver that enables SQL queries and standard JDBC operations directly against Microsoft Access database files (MDB and ACCDB formats). The driver is fully platform-independent and does not require Microsoft Access or any Office components to be installed.

Originally developed as **StelsMDB** by [J-Stels Software](http://www.csv-jdbc.com) (Sergey Kutsygin). Acquired by [Sesame Software](https://www.sesamesoftware.com) on December 31, 2015 and released as the Sesame Software MDB JDBC Driver.

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
- [Third-Party Components](#third-party-components)

---

## Installation

Add `mdbdriver.jar` and any required third-party library JARs to your classpath, or extract these JARs into the directory of your application.

---

## Driver Classes

| Description | Class Name |
|---|---|
| Driver class (JDBC API v1.0) | `com.relationaljunction.jdbc.mdb.MDBDriver2` |
| DataSource class (JDBC API v2.0) | `com.relationaljunction.jdbc.mdb.MDBDataSource2` |

---

## URL Syntax

The connection URL is `jdbc:relationaljunction:mdb:<mdbfile>`, where `mdbfile` is the path to an MDB or ACCDB file.

### Local paths

```
// Absolute or relative path to an Access database file
jdbc:relationaljunction:mdb:c:/mydir/database.mdb
jdbc:relationaljunction:mdb:c:/mydir/database.accdb
jdbc:relationaljunction:mdb:mydir/database.mdb

// ZIP / JAR archive
jdbc:relationaljunction:mdb:cache://zip://c:/mydir/databases.zip/database.mdb

// CLASSPATH resource
jdbc:relationaljunction:mdb:cache://classpath://resources/database.mdb
```

### Remote paths

```
// FTP
jdbc:relationaljunction:mdb:ftp://login:password@somesite.com:21/databases/database.mdb

// FTP + ZIP archive
jdbc:relationaljunction:mdb:cache://zip://ftp://login:password@somesite.com:21/archives/databases.zip/database.mdb

// SFTP (requires Commons VFS, Commons Logging, and JSch)
jdbc:relationaljunction:mdb:sftp://login:password@somesite.com:22/databases/database.mdb

// HTTP
jdbc:relationaljunction:mdb:http://www.somesite.com/databases/database.mdb

// SMB/CIFS share
jdbc:relationaljunction:mdb:cache://smb://your_server/your_share/your_folder/database.mdb
jdbc:relationaljunction:mdb:cache://smb://login:password@your_server/your_share/database.mdb
jdbc:relationaljunction:mdb:cache://smb://domain;login:password@your_server/your_share/database.mdb
```

An RMI server can also be used as an alternative to FTP/SFTP/HTTP. See [Advanced Topics](#advanced-topics).

---

## Driver Properties

### Standard Properties

| Property | Description | Default |
|---|---|---|
| `charset` | Character encoding for string data. Accepts any encoding supported by Java. | System default |
| `create` | If `true`, creates a new Access database file at the specified path if one does not already exist. | `false` |
| `dbInMemory`, `dbPath` | Set the driver mode. See [Driver Modes](#driver-modes). | — |
| `format` | Access file format for newly created databases. Use `access2007` for ACCDB (Access 2007+). | Default Access format |
| `ignoreCase` | If `true`, string comparisons are case-insensitive. | `false` |
| `logPath` | File path for the driver log. | — |

### Advanced Properties

| Property | Description | Default |
|---|---|---|
| `preSQL` | Path to a SQL script executed after the connection is created, e.g. `c:/sql_script.txt`. | — |
| `singletonConnection` | If `true`, one `Connection` instance per unique JDBC URL, shared across threads. Recommended for app servers. Issue `SHUTDOWN` on undeploy. | `false` |
| `useWebParam` | Name of the URL parameter used to pass the database file name to a dynamic server page. | — |

### Setting Driver Properties

**1) Properties object:**

```java
java.util.Properties props = new java.util.Properties();
props.put("charset",    "UTF-8");
props.put("ignoreCase", "true");

Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:mdb:c:/mydir/database.mdb", props);
```

**2) DataSource class:**

```java
MDBDataSource2 mdbDS = new MDBDataSource2();
mdbDS.setPath("c:/mydir/database.mdb");
mdbDS.setCharset("UTF-8");

Connection conn = mdbDS.getConnection();
```

**3) Appended to the URL** (separate multiple with `&` or `!`):

```java
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:mdb:c:/mydir/database.mdb?charset=UTF-8&ignoreCase=true");
```

---

## Connection Example

```java
import java.sql.*;

public class MDBDriverTest {

    public static void main(String[] args) {
        try {
            // Load the driver
            Class.forName("com.relationaljunction.jdbc.mdb.MDBDriver2");

            // Connect — first arg is the path to the MDB/ACCDB file
            Connection conn = DriverManager.getConnection(
                "jdbc:relationaljunction:mdb:" + args[0]);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM \"Customers\"");

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

The driver loads Access data into an intermediate H2 database called the **synchrobase**, which handles SQL queries and synchronizes changes back to the source file.

### Mode 1: Temporary synchrobase in RAM *(default)*

Created in RAM, removed when the connection is closed. Best performance.

> The JVM must have enough heap for large databases. Use `-Xms` / `-Xmx` JVM options. Use `DROP TABLE <name> FROM CACHE` to release specific tables.

### Mode 2: Temporary synchrobase on disk

Written to disk on connect, deleted on close. Use for large databases that exceed available RAM.

```java
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:mdb:c:/mydir/database.mdb?dbInMemory=false&tempPath=c:/tempfiles");
```

### Mode 3: Persistent synchrobase on disk

Created once, reused across connections. Best for production where startup latency matters.

```java
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:mdb:c:/mydir/database.mdb" +
    "?dbPath=c:/synchrobases/syncro_db_name&tempPath=c:/tempfiles");
```

`tempPath` — directory for temporary files. Defaults to the OS temp dir (`java.io.tmpdir`). A fast SSD is recommended.

```java
Properties props = new java.util.Properties();
props.setProperty("dbInMemory", "false"); // Mode 2: disk-based synchrobase
props.setProperty("tempPath",   "c:/temp");
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:mdb:c:/mydir/database.mdb", props);
```

---

## Data Type Mapping

Microsoft Access data types are mapped to JDBC types as follows:

| MS Access Type | RJ MDB Type | JDBC Type (`java.sql.Types.*`) | Java Class |
|---|---|---|---|
| AutoNumber | `IDENTITY`, `BIGINT` | `BIGINT` | `java.lang.Long` |
| Long Integer | `INTEGER` | `INTEGER` | `java.lang.Integer` |
| Integer | `SMALLINT` | `SMALLINT` | `java.lang.Short` |
| Byte | `TINYINT` | `TINYINT` | `java.lang.Byte` |
| Single | `FLOAT` | `FLOAT` | `java.lang.Float` |
| Double | `DOUBLE` | `DOUBLE` | `java.lang.Double` |
| Currency | `NUMERIC` | `NUMERIC` | `java.math.BigDecimal` |
| Text | `VARCHAR` | `VARCHAR` | `java.lang.String` |
| Memo | `VARCHAR` (large) | `VARCHAR` | `java.lang.String` |
| Date/Time | `DATETIME` | `TIMESTAMP` | `java.util.Date` |
| Yes/No | `BOOLEAN` | `BOOLEAN` | `java.lang.Boolean` |
| OLE Object | `BLOB` | `BLOB` | `java.sql.Blob` |

> **Note:** Use `NUMERIC` rather than `DOUBLE` or `FLOAT` for currency values to avoid floating-point rounding errors.

Example `CREATE TABLE`:

```sql
CREATE TABLE Customers (
    id          IDENTITY,
    name        VARCHAR(100),
    balance     NUMERIC(15,2),
    signup_date DATETIME,
    active      BOOLEAN
);
```

---

## Supported SQL Syntax

The driver uses H2 as its SQL engine and supports the majority of ANSI/ISO SQL grammar, including `SELECT`, `INSERT`, `UPDATE`, `DELETE`, and `CREATE`.

### Requirements

- Table names and column names that are SQL reserved words, or contain spaces or special characters, must be enclosed in double quotes: `SELECT "Date", "Customer Name" FROM "Order Details"`
- To include a single quote in a string constant, duplicate it: `SELECT 'it''s fine'`

### Examples

```sql
-- SELECT
SELECT SUM(balance) AS total, MAX(signup_date) AS latest
    FROM "Customers" GROUP BY region HAVING AVG(balance) > 1000

SELECT name FROM "Customers"
    WHERE (region IN ('East', 'West', 'North'))
       OR (manager IS NULL AND signup_date >= to_date('01-05-2002','dd-MM-yyyy'))

-- JOINs
SELECT * FROM "Orders" o
    JOIN "Customers"  c ON o.customer_id = c.id
    JOIN "Products"   p ON p.id          = o.product_id

-- INSERT / UPDATE / DELETE
INSERT INTO "Customers" (name, balance, signup_date, active)
    VALUES ('Acme Corp', 5000.00, now(), true)

DELETE FROM "Customers" WHERE name LIKE 'Test%'

UPDATE "Customers"
    SET balance = 50000.00 WHERE name = 'Acme Corp'

-- CREATE TABLE and INDEX
CREATE TABLE "NewTable" (
    id          IDENTITY,
    name        VARCHAR(100),
    amount      NUMERIC(15,2),
    created_at  DATETIME,
    active      BOOLEAN
);

CREATE INDEX i_name ON "NewTable" (name);
```

For full syntax reference see [H2 SQL Grammar](https://h2database.com/html/grammar.html).

---

## Unsupported Features

- Access-specific query objects (stored queries) are not directly executable as stored procedures; use standard SQL instead.
- Access-specific data macros and VBA code are not executed.

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
    "jdbc:relationaljunction:mdb:c:/mydir/database.mdb", props);

// Or appended to the URL:
Connection conn2 = DriverManager.getConnection(
    "jdbc:relationaljunction:mdb:c:/mydir/database.mdb"
    + "?function:format_date=my_pack.MyFuncs.format_date");
```

### Step 3: Call the function in SQL

```java
Statement st = connection.createStatement();
st.execute("SELECT format_date(signup_date, 'yyyy-MM-dd') FROM \"Customers\"");
```

---

## Performance and Other Hints

- Use `java.sql.PreparedStatement` for all `SELECT`, `INSERT`, `UPDATE`, and `DELETE` operations wherever possible.
- Use batch operations for bulk inserts:

```java
PreparedStatement pst = conn.prepareStatement(
    "INSERT INTO Customers(name, balance) VALUES(?,?)");
for (int i = 0; i < 10000; i++) {
    pst.setString(1, "Customer " + i);
    pst.setBigDecimal(2, new java.math.BigDecimal("100.00"));
    pst.addBatch();
}
pst.executeBatch();
```

- Create indexes using `CREATE INDEX` to improve query performance.
- Reuse the same `java.sql.Connection` across multiple threads.
- Set `singletonConnection=true` for application servers (Tomcat, GlassFish, WebSphere, etc.).
- For databases larger than ~100 MB, use a disk-based synchrobase. See [Driver Modes](#driver-modes).
- Use `org.hibernate.dialect.H2Dialect` in Hibernate configurations.
- Always close `ResultSet`, `Statement`, and `Connection` when finished.
- Use a current JVM. JDK 17+ offers significant performance improvements over older runtimes.

---

## Advanced Topics

### Client/Server Mode via RMI (JDBC Type 3)

The driver supports client/server operation over RMI, enabling remote access to Access databases without copying the file to the client machine. See the Sesame Software MDB RMI documentation for full setup instructions.

### See Also

- [H2 Database — SQL Grammar](https://h2database.com/html/grammar.html)
- [H2 Database — SQL Functions](https://h2database.com/html/functions.html)
- [H2 Database — Full Documentation](https://h2database.com/html/main.html)

---

## Third-Party Components

This driver bundles the following open-source components:

| Component | License | Notes |
|---|---|---|
| H2 Database | MPL 2.0 / EPL 1.0 | SQL engine and synchrobase |
| Jackcess | LGPL 2.1 | Microsoft Access file read/write |
| JCIFS | LGPL 2.1 | SMB/CIFS network access |
| JSch | BSD | SFTP support |
| Apache Commons Net | Apache 2.0 | FTP support |
| Apache Commons VFS | Apache 2.0 | Virtual file system |
| Apache Commons Lang | Apache 2.0 | Utility library |
| Apache Commons Logging | Apache 2.0 | Logging |
