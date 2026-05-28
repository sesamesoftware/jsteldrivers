# Sesame Software XML JDBC Driver

> **Archived Repository** — This is a stable, read-only open source release. No active development, bug fixes, or security patches are planned. The driver is provided as-is for reference and integration use. See [SECURITY.md](SECURITY.md) for known dependency advisories.

A JDBC Type 4 driver that enables SQL queries and standard JDBC operations directly against XML files, using XPath expressions to define table and column mappings.

Originally developed as **StelsXML** by [J-Stels Software](http://www.csv-jdbc.com) (Sergey Kutsygin). Acquired by [Sesame Software](https://www.sesamesoftware.com) on December 31, 2015 and released as the Sesame Software XML JDBC Driver.

---

## Table of Contents

- [Getting Started](#getting-started)
- [Schema File](#schema-file)
- [Driver Properties](#driver-properties)
- [Data Types](#data-types)
- [Supported SQL Syntax](#supported-sql-syntax)
- [Connection Example](#connection-example)
- [Handling XML Namespaces](#handling-xml-namespaces)
- [XPath Engines](#xpath-engines)
- [Driver Modes](#driver-modes)
- [User-Defined SQL Functions](#user-defined-sql-functions)
- [Performance and Other Hints](#performance-and-other-hints)
- [Advanced Topics](#advanced-topics)

---

## Getting Started

1. Add `xmldriver.jar` and any required third-party libraries to your classpath.

2. Create a schema file defining your table specifications. See [Schema File](#schema-file) for details.

3. Register the driver in your Java code:

```java
Class.forName("com.relationaljunction.jdbc.xml.XMLDriver2");
```

4. Connect using `java.sql.Connection`:

```java
Connection conn = DriverManager.getConnection("jdbc:relationaljunction:xml:c:/xmlfiles/schema.xml");
```

The connection URL format is `jdbc:relationaljunction:xml:<schema_path>`, where `schema_path` may be any of the following:

```
// Absolute or relative file path
jdbc:relationaljunction:xml:c:/xmlfiles/schema.xml
jdbc:relationaljunction:xml:xmlfiles/schema.xml

// ZIP / JAR archive
jdbc:relationaljunction:xml:zip://c:/archive.zip/schema.xml

// CLASSPATH resource
jdbc:relationaljunction:xml:classpath://resources/schema.xml

// FTP URL
jdbc:relationaljunction:xml:ftp://login:password@somesite.com:21/xmlfiles/schema.xml

// HTTP URL
jdbc:relationaljunction:xml:http://www.somesite.com/xmlfiles/schema.xml

// HTTP dynamic page (separate driver props from page params with '??')
jdbc:relationaljunction:xml:http://www.somesite.com/out.jsp?param1=value1??dbInMemory=false

// SMB/CIFS share
jdbc:relationaljunction:xml:smb://your_server/your_share/your_folder/schema.xml
```

5. Execute an SQL query using `java.sql.Statement`:

```java
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery("SELECT * FROM test");
```

---

## Schema File

The schema file defines the table specifications used when executing SQL queries. Each table is described using XPath expressions that map XML elements and attributes to SQL columns.

### Example XML document

```xml
<?xml version="1.0" encoding="UTF-8"?>
<employees>
    <document_name>Employees doc</document_name>
    <employee id="1">
        <first_name>Bill</first_name>
        <last_name>Adams</last_name>
        <age>25</age>
        <hire_date>12-06-1995</hire_date>
        <title>Java programmer</title>
    </employee>
    <employee id="2">
        <first_name>Mary</first_name>
        <last_name>Jones</last_name>
        <age>32</age>
        <hire_date>22-09-2001</hire_date>
        <title>Sales manager</title>
    </employee>
</employees>
```

### Corresponding schema file

```xml
<?xml version="1.0" encoding="UTF-8"?>
<schema>

  <table name="employees"
         file="employees.xml"
         path="/employees/employee"
         constraint="PRIMARY KEY(id), UNIQUE(lastname), CHECK age > 18">

    <!-- 'file' may be absolute, relative, or a URL:
         HTTP:      http://www.example.com/rss_feed.xml
         FTP:       ftp://user:passw@www.sample.com:21/test/test.xml
         CLASSPATH: classpath://resources/test.xml
         ZIP:       zip://c:/archive.zip/test.xml
         SMB/CIFS:  smb://your_server/your_share/your_folder/test.xml
         SFTP:      sftp://login:password@somesite.com:22/test/test.xml -->

    <!-- 'path' is the XPath base for table rows; each /employee element = one row -->

    <!-- Absolute XPath from the XML root: -->
    <column name="documentname" type="VARCHAR"  path="/employees/document_name"/>

    <!-- Relative paths (resolved from 'path' /employees/employee/): -->
    <column name="firstname"    type="VARCHAR"  size="15" path="first_name"/>
    <column name="lastname"     type="VARCHAR"  size="25" path="last_name"/>
    <column name="title"        type="VARCHAR"  size="15" path="title"/>
    <column name="id"           type="INTEGER"            path="@id"/>
    <column name="age"          type="INTEGER"            path="age"/>
    <column name="hiredate"     type="DATETIME"           path="hire_date"/>
  </table>

  <!-- Local properties on a table override global connection properties -->
  <table name="advanced_test"
         file="advanced_test.xml"
         path="/table/rec"
         decimalFormatInput="$###,###.##|###,###.##"
         dateFormat="yyyy-MM-dd HH:mm:ss | yyyy-MM-dd | HH:mm:ss"
         readAPI="XOM">
    <column name="int_col"  type="integer" path="@int"/>
    <column name="year_col" type="date"    path="year_col" dateFormat="dd/MM/yyyy"/>
    <column name="time_col" type="time"    path="time_col" dateFormat="HH:mm"/>
    <column name="dec_col"  type="numeric" path="dec_col"
            decimalFormatInput="$###,###.##|###,###.##$;"
            decimalFormatOutput="$###,###.##"/>
  </table>

</schema>
```

### Schema notes

- A table may omit the `name` attribute, in which case specify the full file path as the table name in SQL: `SELECT * FROM "c:/xmlfiles/employees.xml"`
- Wildcard file patterns are supported: `<table file="c:/xmlfiles/??employees*.xml" path="/employees/employee">` — use `*` for any string, `?` for any single character.
- Local driver properties set directly on a `<table>` element override global connection properties for that table only.

---

## Driver Properties

### Standard Properties

| Property | Description | Default |
|---|---|---|
| `charset` | Character encoding for output. Set input encoding in the XML declaration: `<?xml version="1.0" encoding="some_charset"?>` | `UTF-8` |
| `dateFormat` | Date/time format(s) separated by `\|`, e.g. `"dd.MM.yy \| dd.MM \| dd"`. See `java.util.SimpleDateFormat`. | `"yyyy-MM-dd HH:mm:ss.SSS \| yyyy-MM-dd HH:mm:ss \| yyyy-MM-dd \| HH:mm:ss.SSS \| HH:mm:ss"` |
| `decimalFormatInput` | Input format(s) for numbers, e.g. `"###,###.##$"`. Multiple formats separated by `\|`. May be set per column. | — |
| `decimalFormatOutput` | Output format for numbers, e.g. `"###,###.##$"`. May be set per column. | — |
| `dbInMemory`, `dbPath` | Set the driver mode. See [Driver Modes](#driver-modes). | — |
| `ignoreCase` | If `true`, string comparisons are case-insensitive. | `true` |
| `namespaceAware` | Enables namespace support when parsing XML. Not supported with XOM engine. | `true` |
| `namespaces` | Defines XML namespaces for XPath expressions. See [Handling XML Namespaces](#handling-xml-namespaces). | — |

### Advanced Properties

| Property | Description | Default |
|---|---|---|
| `emptyStringAsNull` | Treats strings containing only whitespace as NULL. | `true` |
| `ignoreRows` | Row numbers to skip during parsing, e.g. `1,3,5-10,100+`. Useful for header rows or bad records. | — |
| `nullStringInput` | String treated as NULL on input. | `(?i)null` (regex) |
| `nullStringOutput` | String written to XML to represent NULL values. | `"NULL"` |
| `parameter_xxx` | Passes a named parameter value into the schema. See [Advanced Topics](#advanced-topics). | — |
| `preSQL` | Path to a SQL script executed after the connection is created, e.g. `c:/sql_script.txt`. | — |
| `propertiesFile` | Path to an external properties file for driver settings. | — |
| `readAPI` | Sets the XPath engine: `"SAX"` or `"XOM"`. See [XPath Engines](#xpath-engines). | `"SAX"` |
| `singletonConnection` | If `true`, one `Connection` instance per unique JDBC URL, shared across threads. Recommended for app servers. Issue `SHUTDOWN` on undeploy. | `false` |
| `trimBlanks` | Trims leading/trailing whitespace from string values. | `false` |

### Setting Driver Properties

**1) Properties object:**

```java
java.util.Properties props = new java.util.Properties();
props.put("dateFormat",      "MM.dd.yyyy");
props.put("namespaceAware",  "false");
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:xml:schema.xml", props);
```

**2) Appended to the URL** (separate multiple with `&` or `!`):

```java
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:xml:schema.xml?dateFormat=MM.dd.yyyy&namespaceAware=false");
```

**3) External properties file:**

```java
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:xml:schema.xml?propertiesFile=config.properties");
```

**4)** Local properties defined per-table in the schema file (see [Schema File](#schema-file)).

---

## Data Types

| RJ Type | JDBC Type (`java.sql.Types.*`) | Java Class |
|---|---|---|
| `AUTOINCREMENT`, `IDENTITY` | `BIGINT` | `java.lang.Long` |
| `Integer`, `INT` | `INTEGER` | `java.lang.Integer` |
| `BIGINT`, `LONG` | `BIGINT` | `java.lang.Long` |
| `FLOAT` | `FLOAT` | `java.lang.Float` |
| `DOUBLE` | `DOUBLE` | `java.lang.Double` |
| `BIGDECIMAL`, `DECIMAL`, `NUMERIC`, `MONEY`, `CURRENCY` | `NUMERIC` | `java.math.BigDecimal` |
| `STRING`, `VARCHAR` | `VARCHAR` | `java.lang.String` |
| `DATETIME`, `TIMESTAMP` | `TIMESTAMP` | `java.util.Date` |
| `DATE`, `YEAR` | `DATE` | `java.sql.Date` |
| `TIME` | `TIME` | `java.sql.Time` |
| `BOOLEAN` | `BOOLEAN` | `java.lang.Boolean` |

> **Note:** Do not use `DOUBLE` or `FLOAT` for currency values — use `BIGDECIMAL` instead to avoid rounding errors. `BIGDECIMAL` is slower and uses more storage than floating-point types.

---

## Supported SQL Syntax

Version 7.0 uses H2 as its SQL engine and supports the majority of ANSI/ISO SQL grammar, including `SELECT`, `INSERT`, `UPDATE`, and `DELETE`.

### Requirements

- Column names that are SQL reserved words, or contain spaces or special characters, must be enclosed in double quotes: `SELECT "Date", "My integer-column" FROM test`
- To include a single quote in a string constant, duplicate it: `SELECT 'a"bcd"efgh'`

### Examples

```sql
-- SELECT
SELECT * FROM employees WHERE title = 'Java programmer' ORDER BY last_name

SELECT name FROM salesreps
    WHERE (rep_office IN (22, 11, 12))
       OR (manager IS NULL
           AND hire_date >= parsedatetime('01-05-2002','dd-MM-yyyy')
       OR (sales > quota AND NOT sales > 600000.0))

-- JOINs
SELECT * FROM prices ps
    JOIN regions regs ON ps.regionid = regs.id
    JOIN products prod ON prod.prodid = ps.prodid

-- INSERT / UPDATE / DELETE
INSERT INTO employees (firstname, lastname, title, id, hiredate)
    VALUES('John', 'Doe', 'Web admin', 3, parsedatetime('07:02:2007','dd:MM:yyyy'))

DELETE FROM employees WHERE lastname LIKE 'Henry%'

UPDATE "c:/xmlfiles/customers.xml"
    SET credit_limit = 50000.00 WHERE company = 'Acme Mfg.'

-- CREATE INDEX
CREATE INDEX i_1 ON new_table (int_col);
```

> **Note:** `INSERT`, `UPDATE`, and `DELETE` are supported only for XML documents with a simple element hierarchy.

For full syntax reference see [H2 SQL Grammar](https://h2database.com/html/grammar.html).

---

## Connection Example

```java
import java.sql.*;

public class DriverTest {

    public static void main(String[] args) {
        try {
            // Load the driver
            Class.forName("com.relationaljunction.jdbc.xml.XMLDriver2");

            // Connect — first arg is the path to the schema file
            Connection conn = DriverManager.getConnection(
                "jdbc:relationaljunction:xml:" + args[0]);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM employees");

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

Schema file for this example:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<schema>
  <table name="employees" file="employees.xml"
         path="/employees/employee" dateFormat="dd-MM-yyyy">
    <column name="documentname" type="string"   path="/employees/document_name"/>
    <column name="firstname"    type="string"   path="first_name"/>
    <column name="lastname"     type="string"   path="last_name"/>
    <column name="title"        type="string"   path="title"/>
    <column name="id"           type="integer"  path="@id"/>
    <column name="age"          type="integer"  path="age"/>
    <column name="hiredate"     type="datetime" path="hire_date"/>
  </table>
</schema>
```

---

## Handling XML Namespaces

By default the driver is namespace-aware. To disable this, set `namespaceAware=false` (SAX mode only).

When working with namespaced XML, declare each namespace using the `namespaces` property in the format:

```
prefix1:namespace_uri|prefix2:namespace_uri|...
```

Where `namespace_uri` is the URI from the XML document and `prefix` is an arbitrary identifier used in XPath expressions in the schema.

> **Note:** The reserved `xml:` namespace (`http://www.w3.org/XML/1998/namespace`) must **not** be listed in `namespaces` — it is always available automatically. Reference it directly in XPath: `path="@xml:id"`.

### Example schema for a namespaced document

```xml
<?xml version="1.0" encoding="UTF-8"?>
<schema>
  <table name="employees" file="employees.xml"
         path="/empl:employees/empl:employee"
         dateFormat="dd-MM-yyyy"
         namespaces="empl:http://www.example.com/employees">
    <!-- All XPath expressions must use the 'empl:' prefix -->
    <column name="firstname" type="string"   path="empl:first_name"/>
    <column name="lastname"  type="string"   path="empl:last_name"/>
    <column name="title"     type="string"   path="empl:title"/>
    <column name="id"        type="integer"  path="@id"/>
    <column name="age"       type="integer"  path="empl:age"/>
    <column name="hiredate"  type="datetime" path="empl:hire_date"/>
  </table>
</schema>
```

---

## XPath Engines

The driver supports two XPath engines, selectable per table via the `readAPI` attribute.

| Engine | Model | Memory Usage | XPath Support | Best For |
|---|---|---|---|---|
| `SAX` *(default)* | Simple API for XML | Minimal — streams the file | Partial — no preceding axes | Simple XPath, large files, low-memory environments |
| `XOM` | XML Object Model | Higher — loads full tree | Full XPath syntax | Complex XPath, preceding axes, relational lookups |

> Use `XOM` when your XPath requires preceding axes or other advanced syntax. Use `SAX` for simple expressions and maximum memory efficiency.

### Example — 1:n XML structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<catalogue>
  <author id="1">
    <name>Isaac Asimov</name>
    <books>
      <book><name>The Stars, Like Dust</name><genre>Science fiction</genre><price>44.95</price></book>
      <book><name>Pebble in the Sky</name>   <genre>Science fiction</genre><price>24.95</price></book>
    </books>
  </author>
  <author id="2">
    <name>Stanislaw Lem</name>
    <books>
      <book><name>Solaris</name>              <genre>Science fiction</genre><price>39.95</price></book>
      <book><name>Return from the Stars</name><genre>Science fiction</genre><price>34.95</price></book>
    </books>
  </author>
</catalogue>
```

Schema using XOM to traverse the preceding `author` element:

```xml
<schema>
  <table name="books" file="books.xml"
         path="/catalogue/author/books/book" readAPI="XOM">
    <column name="author_id" path="../../@id" type="INT"/>
    <column name="name"      path="name"       type="VARCHAR" size="50"/>
    <column name="genre"     path="genre"      type="VARCHAR" size="30"/>
    <column name="price"     path="price"      type="BIGDECIMAL"/>
  </table>
  <table name="authors" file="books.xml"
         path="/catalogue/author" readAPI="SAX">
    <column name="id"   path="@id"  type="INT"/>
    <column name="name" path="name" type="VARCHAR" size="50"/>
  </table>
</schema>
```

`SELECT * FROM books` result:

| AUTHOR_ID | NAME | GENRE | PRICE |
|---|---|---|---|
| 1 | The Stars, Like Dust | Science fiction | 44.95 |
| 1 | Pebble in the Sky | Science fiction | 24.95 |
| 2 | Solaris | Science fiction | 39.95 |
| 2 | Return from the Stars | Science fiction | 34.95 |

**SAX workaround** — if you can't use XOM, use absolute XPaths for elements above the base path:

```xml
<table name="books" file="books.xml"
       path="/catalogue/author/books/book" readAPI="SAX">
  <column name="author_id" path="/catalogue/author/@id" type="INT"/>
  <column name="name"      path="name"                  type="VARCHAR" size="50"/>
</table>
```

---

## Driver Modes

The driver loads XML data into an intermediate H2 database called the **synchrobase**, which handles SQL queries and synchronizes changes back to the source files.

### Mode 1: Temporary synchrobase in RAM *(default)*

Created in RAM, removed when the connection is closed. Best performance.

> The JVM must have enough heap for large tables. Use `-Xms` / `-Xmx` JVM options. Use `DROP TABLE <name> FROM CACHE` to release specific tables.

### Mode 2: Temporary synchrobase on disk

Written to disk on connect, deleted on close. Use for large files that exceed available RAM.

```java
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:xml:c:/xml/schema.xml?dbInMemory=false&tempPath=c:/tempfiles");
```

### Mode 3: Persistent synchrobase on disk

Created once, reused across connections. Best for production where startup latency matters.

```java
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:xml:c:/xml/schema.xml" +
    "?dbPath=c:/synchrobases/syncro_db_name&tempPath=c:/tempfiles");
```

`tempPath` — directory for temporary files. Defaults to the OS temp dir (`java.io.tmpdir`). A fast SSD is recommended.

```java
Properties props = new java.util.Properties();
props.setProperty("dbInMemory", "false"); // Mode 2: disk-based synchrobase
props.setProperty("tempPath",   "c:/temp");
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:xml:c:/xml/schema.xml", props);
```

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
- Arguments must match the Java types listed in the [Data Types](#data-types) table.
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
    "jdbc:relationaljunction:xml:c:/xmlfiles/schema.xml", props);

// Or appended to URL:
Connection conn2 = DriverManager.getConnection(
    "jdbc:relationaljunction:xml:c:/xmlfiles/schema.xml"
    + "?function:format_date=my_pack.MyFuncs.format_date");
```

### Step 3: Call the function in SQL

```java
Statement st = conn.createStatement();
st.execute("SELECT format_date(date_column, 'yyyy-MM-dd') FROM test");
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

- Create indexes using `CREATE INDEX` and/or the `constraint` attribute on the `<table>` element.
- Use `EXPLAIN` to verify index usage:

```java
ResultSet rs = connection.createStatement()
    .executeQuery("EXPLAIN SELECT * FROM test WHERE id = 5");
// Print the result set to review the query execution plan
```

- Reuse the same `java.sql.Connection` across multiple threads.
- Set `singletonConnection=true` for application servers (Tomcat, GlassFish, WebSphere, etc.).
- For tables larger than ~100 MB, use a disk-based synchrobase. See [Driver Modes](#driver-modes).
- Use `org.hibernate.dialect.H2Dialect` in Hibernate configurations.
- Always close `ResultSet`, `Statement`, and `Connection` when finished.
- Use a current JVM. JDK 17+ offers significant performance improvements over older runtimes.

---

## Advanced Topics

### 1) Client/Server Mode via RMI (JDBC Type 3)

The driver can operate in a client/server configuration over RMI, allowing remote clients to query XML files without a local copy of the files or the JAR. See the Sesame Software XML RMI documentation for setup details.

### 2) External Parameters in the Schema

Named parameters can be used in table descriptions within the schema file. Reference them as `{@parameter_name}` in the schema, then pass values via the driver property `parameter_xxx`.

```
jdbc:relationaljunction:xml:c:/xmlfiles/schema.xml
    &parameter_filePath=c:/xmlfiles/products.xml
    &parameter_productsPath=/products/product
    &parameter_columnName1=id
    &parameter_columnName2=name
```

Corresponding schema entry:

```xml
<table name="products"
       file="{@filePath}"
       path="{@productsPath}">
    <column name="{@columnName1}" type="IDENTITY" path="id"/>
    <column name="{@columnName2}" type="VARCHAR"  path="name"/>
    <column name="price"          type="INTEGER"  path="price"/>
</table>
```

### See Also

- [H2 Database — SQL Grammar](https://h2database.com/html/grammar.html)
- [H2 Database — SQL Functions](https://h2database.com/html/functions.html)
- [H2 Database — Full Documentation](https://h2database.com/html/main.html)
- [java.util.SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html)
- [java.text.DecimalFormat](https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html)
