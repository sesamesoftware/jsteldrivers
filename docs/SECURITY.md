# Security Policy

## Maintenance status

This repository is an archived release. **No security patches will be issued.** The driver is provided as-is and is not actively monitored for new vulnerabilities.

---

## Known dependency vulnerabilities
A list of known vulnerabilities at release. Sesame Software is not responsible for any attacks as the result of these vulnerabilities.  

Apache POI Scratchpad(3.10-FINAL): Apache POI in versions prior to release 3.17 are vulnerable to Denial of Service Attacks: 1) Infinite Loops while parsing crafted WMF, EMF, MSG and macros (POI bugs 61338 and 61294), and 2) Out of Memory Exceptions while parsing crafted DOC, PPT and XLS (POI bugs 52372 and 61295).  
  
Commons Lang(2.3): Uncontrolled Recursion vulnerability in Apache Commons Lang. This issue affects Apache Commons Lang: Starting with commons-lang:commons-lang 2.0 to 2.6, and, from org.apache.commons:commons-lang3 3.0 before 3.18.0. The methods ClassUtils.getClass(...) can throw StackOverflowError on very long inputs. Because an Error is usually not handled by applications and libraries, a StackOverflowError could cause an application to stop. Users are recommended to upgrade to version 3.18.0, which fixes the issue. Mend Note: The description of this vulnerability differs from MITRE.  
  
Commons HTTP Client(3.1): Apache Commons HttpClient 3.x, as used in Amazon Flexible Payments Service (FPS) merchant Java SDK and other products, does not verify that the server hostname matches a domain name in the subject's Common Name (CN) or subjectAltName field of the X.509 certificate, which allows man-in-the-middle attackers to spoof SSL servers via an arbitrary valid certificate.  
  
Commons Net(2.2):  Prior to Apache Commons Net 3.9.0, Net's FTP client trusts the host from PASV response by default. A malicious server can redirect the Commons Net code to use a different host, but the user has to connect to the malicious server in the first place. This may lead to leakage of information about services running on the private network of the client. The default in version 3.9.0 is now false to ignore such hosts, as cURL does. See https://issues.apache.org/jira/browse/NET-711.  

Commons VFS(2.2): Relative Path Traversal vulnerability in Apache Commons VFS before 2.10.0. The FileObject API in Commons VFS has a 'resolveFile' method that takes a 'scope' parameter. Specifying 'NameScope.DESCENDENT' promises that "an exception is thrown if the resolved file is not a descendent of the base file". However, when the path contains encoded ".." characters (for example, "%2E%2E/bar.txt"), it might return file objects that are not a descendent of the base file, without throwing an exception. This issue affects Apache Commons VFS: before 2.10.0. Users are recommended to upgrade to version 2.10.0, which fixes the issue.  

JDOM(1.1): An XXE issue in SAXBuilder in JDOM through 2.0.6 allows attackers to cause a denial of service via a crafted HTTP request.     

DOM4J(1.6.1): dom4j before 2.0.3 and 2.1.x before 2.1.3 allows external DTDs and External Entities by default, which might enable XXE attacks. However, there is popular external documentation from OWASP showing how to enable the safe, non-default behavior in any application that uses dom4j.

UCANACCESS(5.0.1): Those using java.sql.Statement or java.sql.PreparedStatement in hsqldb (HyperSQL DataBase) to process untrusted input may be vulnerable to a remote code execution attack. By default it is allowed to call any static method of any Java class in the classpath resulting in code execution. The issue can be prevented by updating to 2.7.1 or by setting the system property "hsqldb.method_class_names" to classes which are allowed to be called. For example, System.setProperty("hsqldb.method_class_names", "abc") or Java argument -Dhsqldb.method_class_names="abc" can be used. From version 2.7.1 all classes by default are not accessible except those in java.lang.Math and need to be manually enabled.




---

## Reporting a security concern

This repository is not actively monitored. If you have a security concern that requires direct attention from Sesame Software, please reach out via the [Sesame Software website](https://www.sesamesoftware.com). Do not open a public GitHub issue for security-sensitive matters.
