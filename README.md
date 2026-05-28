# jsteldrivers

JDBC drivers for querying CSV, DBF, MDB, and XML data sources using standard SQL — no database installation required.

Provided by [Sesame Software](https://www.sesamesoftware.com).

> **Note:** This project is archived and no longer actively developed. No security patches are planned. See [SECURITY.md](docs/SECURITY.md) for known vulnerabilities.

---

## Maven Dependency

```xml
<dependency>
    <groupId>com.sesamesoftware</groupId>
    <artifactId>jsteldrivers</artifactId>
    <version>1.0</version>
</dependency>
```

---

## Overview

jsteldrivers provides four JDBC Type 4 drivers that allow you to query and modify non-relational file-based data sources using standard SQL. All drivers use an embedded H2 database as the SQL engine and support the full H2 SQL grammar including JOINs, subqueries, GROUP BY, aggregates, and user-defined functions.

| Driver | Data Source | Read | Write |
|--------|-------------|------|-------|
| CSV | Delimited text files (.txt, .csv, .tsv) | SELECT | INSERT |
| DBF | dBASE III/IV and Visual FoxPro files | SELECT | INSERT, UPDATE, DELETE |
| MDB | Microsoft Access databases (.mdb, .accdb) | SELECT | INSERT, UPDATE, DELETE |
| XML | XML files with XPath schema mapping | SELECT | Limited |

All drivers support accessing files over local disk, FTP, SFTP, HTTP, SMB/CIFS, ZIP archives, and CLASSPATH resources.

**Requires Java 8 or higher.**

---

## CSV Driver

**Driver class:** `com.relationaljunction.jdbc.csv.CsvDriver2`

Queries delimited text files. Each file in a directory is treated as a database table. The first row is used as column headers by default.

### Connection String

```
jdbc:relationaljunction:csv:<path>
```

### Examples

```
jdbc:relationaljunction:csv:c:/mydir/csvfiles
jdbc:relationaljunction:csv:cache://zip://c:/csvfiles.zip
jdbc:relationaljunction:csv:ftp://user:password@host:21/csvfiles
jdbc:relationaljunction:csv:sftp://user:password@host:22/csvfiles
jdbc:relationaljunction:csv:http://www.example.com/csvfiles
jdbc:relationaljunction:csv:cache://smb://server/share/folder
```

### Key Properties

| Property | Default | Description |
|----------|---------|-------------|
| `separator` | tab | Field delimiter character |
| `fileExtension` | `.txt` | File extension to treat as tables |
| `suppressHeaders` | `false` | Treat first row as data instead of headers |
| `trimBlanks` | `false` | Trim whitespace from field values |
| `charset` | system default | Character encoding |
| `ignoreCase` | `true` | Case-insensitive string comparisons |

For full documentation see [docs/csv/README.md](docs/csv/README.md).

---

## DBF Driver

**Driver class:** `com.relationaljunction.jdbc.dbf.DBFDriver2`

Queries dBASE III, dBASE IV, and Visual FoxPro .dbf files. Supports Visual FoxPro database containers (.dbc) for extended column name support.

### Connection String

```
jdbc:relationaljunction:dbf:<path>
```

### Examples

```
jdbc:relationaljunction:dbf:c:/mydir/dbffiles
jdbc:relationaljunction:dbf:c:/mydir/database.dbc
jdbc:relationaljunction:dbf:ftp://user:password@host:21/dbffiles
jdbc:relationaljunction:dbf:sftp://user:password@host:22/dbffiles
jdbc:relationaljunction:dbf:http://www.example.com/dbffiles
```

### Key Properties

| Property | Default | Description |
|----------|---------|-------------|
| `extension` | `.dbf` | File extension for DBF files |
| `format` | `DBASEIII` | Create format: `DBASEIII` or `VFP` |
| `useBigDecimalType` | `false` | Use BigDecimal for floating-point (recommended for currency) |
| `watchFileModifications` | `false` | Poll for external file changes |
| `charset` | system default | Character encoding |

For full documentation see [docs/dbf/README.md](docs/dbf/README.md).

---

## MDB Driver

**Driver class:** `com.relationaljunction.jdbc.mdb.MDBDriver2`

Queries Microsoft Access .mdb and .accdb files without requiring Microsoft Access or Office to be installed.

### Connection String

```
jdbc:relationaljunction:mdb:<path-to-file>
```

### Examples

```
jdbc:relationaljunction:mdb:c:/mydir/database.mdb
jdbc:relationaljunction:mdb:c:/mydir/database.accdb
jdbc:relationaljunction:mdb:cache://zip://c:/archive.zip/database.mdb
jdbc:relationaljunction:mdb:ftp://user:password@host:21/database.mdb
```

### Key Properties

| Property | Default | Description |
|----------|---------|-------------|
| `create` | `false` | Create new Access file if it doesn't exist |
| `format` | `access2000` | `access2007` for .accdb format |
| `ignoreCase` | `false` | Case-insensitive string comparisons |
| `charset` | system default | Character encoding |

For full documentation see [docs/mdb/README.md](docs/mdb/README.md).

---

## XML Driver

**Driver class:** `com.relationaljunction.jdbc.xml.XMLDriver2`

Queries XML files using XPath expressions to define table and column mappings. Requires a schema file that maps XPath expressions to SQL table and column names.

### Connection String

```
jdbc:relationaljunction:xml:<path-to-schema-file>
```

### Examples

```
jdbc:relationaljunction:xml:c:/xmlfiles/schema.xml
jdbc:relationaljunction:xml:cache://zip://c:/archive.zip/schema.xml
jdbc:relationaljunction:xml:ftp://user:password@host:21/schema.xml
jdbc:relationaljunction:xml:http://www.example.com/xmlfiles/schema.xml
```

### Key Properties

| Property | Default | Description |
|----------|---------|-------------|
| `readAPI` | `SAX` | XPath engine: `SAX` (memory-efficient) or `XOM` (full XPath) |
| `namespaceAware` | `false` | Enable XML namespace support |
| `namespaces` | — | Namespace prefix-to-URI mappings |
| `ignoreCase` | `true` | Case-insensitive string comparisons |

For full documentation and schema file format see [docs/xml/README.md](docs/xml/README.md).

---

## Common Properties

These properties apply to all four drivers.

| Property | Default | Description |
|----------|---------|-------------|
| `dbInMemory` | `true` | Load data into RAM (fastest) |
| `dbPath` | — | Persist synchrobase to this directory |
| `tempPath` | system temp | Directory for temporary files |
| `singletonConnection` | `false` | Reuse a single connection per URL (recommended for app servers) |
| `preSQL` | — | Path to a SQL script executed on connection |
| `logPath` | — | Path for driver log output |

### Passing Properties

Properties can be passed three ways:

**1. Properties object:**
```java
Properties props = new Properties();
props.setProperty("separator", ",");
Connection conn = DriverManager.getConnection(
    "jdbc:relationaljunction:csv:c:/data", props);
```

**2. URL parameters (using `&` or `!` as separator):**
```
jdbc:relationaljunction:csv:c:/data&separator=,&fileExtension=.csv
```

**3. DataSource class:**
```java
CsvDataSource2 ds = new CsvDataSource2();
ds.setUrl("c:/data");
ds.setSeparator(",");
Connection conn = ds.getConnection();
```

---

## SQL Support

All drivers support standard ANSI SQL via the embedded H2 engine:

- `SELECT`, `INSERT`, `UPDATE`, `DELETE`
- `JOIN` (INNER, LEFT, RIGHT, FULL)
- Subqueries and correlated subqueries
- `GROUP BY`, `HAVING`, `ORDER BY`
- Aggregate functions: `SUM`, `AVG`, `MAX`, `MIN`, `COUNT`
- `CREATE INDEX` for query optimization
- `CREATE ALIAS` for user-defined functions
- Full [H2 SQL grammar](https://h2database.com/html/grammar.html)

---

## License

Apache License 2.0 — see [LICENSE.md](LICENSE.md).
