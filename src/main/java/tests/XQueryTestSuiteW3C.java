/*
 * Copyright (c) 2005, The Regents of the University of California, through
 * Lawrence Berkeley National Laboratory (subject to receipt of any required
 * approvals from the U.S. Dept. of Energy). All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * (1) Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * (2) Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * (3) Neither the name of the University of California, Lawrence Berkeley
 * National Laboratory, U.S. Dept. of Energy nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * You are under no obligation whatsoever to provide any bug fixes, patches, or
 * upgrades to the features, functionality or performance of the source code
 * ("Enhancements") to anyone; however, if you choose to make your Enhancements
 * available either publicly, or directly to Lawrence Berkeley National
 * Laboratory, without imposing a separate written license agreement for such
 * Enhancements, then you hereby grant the following license: a non-exclusive,
 * royalty-free perpetual license to install, use, modify, prepare derivative
 * works, incorporate into other computer software, distribute, and sublicense
 * such enhancements or derivative works thereof, in binary and source code
 * form.
 */
package tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nux.xom.pool.BuilderPool;
import nux.xom.pool.FileUtil;
import nux.xom.pool.XOMUtil;
import nux.xom.xquery.ResultSequenceSerializer;
import nux.xom.xquery.XQuery;
import nux.xom.xquery.XQueryException;
import nux.xom.xquery.XQueryUtil;

/**
 * Runs the <a target="_blank" href="http://www.w3.org/XML/Query/test-suite/">
 * Official W3C XQuery Test Suite</a> (XQTS) against Nux, looking for potential
 * standards conformance bugs; The test suite contains some 15000 test cases; it
 * must be downloaded separately from the W3C site.
 * <p>
 * Example usage:
 * 
 * <pre>
 * export CLASSPATH=lib/nux.jar:lib/saxon8.jar:lib/xom.jar
 * java nux.xom.tests.XQueryTestSuiteW3C ../xqts-0.9.4
 * </pre>
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.33 $, $Date: 2006/06/14 08:05:17 $
 */
public class XQueryTestSuiteW3C {

	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final boolean DISABLE_URI_CHECK = true;
	
	private XQueryTestSuiteW3C() {}
	
	/**
	 * Runs the test suite; the first argument indicates the test suite's root
	 * directory.
	 */
	public static void main(String[] args) throws Throwable {
		new XQueryTestSuiteW3C().run(args);
	}
	
	private void run(String[] args) throws Throwable {
		if (DISABLE_URI_CHECK) System.setProperty("nu.xom.Verifier.checkURI", "false");

		if (args.length == 0) args = new String[] { "../xqts-0.9.4" };
		File rootDir = new File(args[0]);
		if (!rootDir.exists() || !rootDir.isDirectory()) {
			throw new IllegalArgumentException("xqts dir does not exist: " + rootDir);
		}
		Document catalog = buildDocument(new File(rootDir, "XQTSCatalog.xml"));
		
		String ns = "declare namespace ns = 'http://www.w3.org/2005/02/query-test-XQTSCatalog'; ";
//		String version = XQueryUtil.xquery(catalog, ns + "ns:test-suite/@version").get(0).getValue();
		File expectedDir = new File(rootDir, 
			XQueryUtil.xquery(catalog, ns + "ns:test-suite/@ResultOffsetPath").get(0).getValue());
		File queryDir = new File(rootDir, 
			XQueryUtil.xquery(catalog, ns + "ns:test-suite/@XQueryQueryOffsetPath").get(0).getValue());
		File testSourcesDir = new File(rootDir, "TestSources");		
		Nodes testCases = XQueryUtil.xquery(catalog, ns + "//ns:test-case");
		
		for (int i=0; i < testCases.size(); i++) {
			Node testCase = testCases.get(i);

//			String groupTitle = XQueryUtil.xquery(testCase, ns + "../ns:GroupInfo/ns:title").get(0).getValue();
			String path = XQueryUtil.xquery(testCase, "@FilePath").get(0).getValue();
			File query = new File(new File(queryDir, path), 
				XQueryUtil.xquery(testCase, ns + "ns:query/@name").get(0).getValue() + ".xq");
			String squery = readQuery(query); 
			System.out.println(i + ": " + query + " ...");
			
			if (XQueryUtil.xquery(testCase, ns + "ns:spec-citation[@section-pointer='id-validate']").size() > 0) {
				System.out.println("		************* IGNORED SCHEMA AWARE FUNCTIONALITY *****");
				continue; // ignore validate() function (nux is not schema aware)
			}
					
			if (squery == null) {
				System.out.println("		************* IGNORED *****");
				continue;
			}
						
			Nodes inputs = XQueryUtil.xquery(testCase, ns + "ns:input-file");
			Map vars = new HashMap();
			for (int j=0; j < inputs.size(); j++) {
				File input = new File(testSourcesDir, inputs.get(j).getValue() + ".xml");
				String varName = ((Element) inputs.get(j)).getAttributeValue("variable");
				Document inputDoc = buildDocument(input);
//				System.out.println(inputDoc.getBaseURI());
				if (true) XOMUtil.Normalizer.STRIP.normalize(inputDoc);
				vars.put(varName, inputDoc);
			}

			Nodes expectedErrors = XQueryUtil.xquery(testCase, ns + "ns:expected-error");					
			Nodes expectedOutputs = XQueryUtil.xquery(testCase, ns + "ns:output-file");
			boolean inspect = false;
			for (int k=0; !inspect && k < expectedOutputs.size(); k++) {
				String compare = ((Element)expectedOutputs.get(k)).getAttributeValue("compare");
				if ("Inspect".equals(compare)) inspect = true;
			}
				
			Nodes results = null;
			try { // here's where the query is actually executed
				XQuery xquery = new XQuery(squery, testSourcesDir.toURI());
//				XQuery xquery = new XQuery(squery, query.toURI());
//				XQuery xquery = XQueryPool.GLOBAL_POOL.getXQuery(squery, query.toURI());
				results = xquery.execute(null, null, vars).toNodes();
			} catch (Throwable t) {
				if (!inspect && expectedErrors.size() == 0) {
					System.out.println(XOMUtil.toPrettyXML(testCase));
					throw t;
				}
				if (!(t instanceof XQueryException)) throw t;
//				System.out.println("expected error:" + t);
				continue;
			}
			
			for (int k=0; k < expectedOutputs.size(); k++) {
				File expectedOutput = new File(
						new File(expectedDir, path), expectedOutputs.get(k).getValue());			
				String compare = ((Element)expectedOutputs.get(k)).getAttributeValue("compare");
				if ("Text".equals(compare)) compare = "Fragment"; // see http://www.w3.org/Bugs/Public/show_bug.cgi?id=2476
				
				try {
					if (compare.equals("Text")) {
						String expected = FileUtil.toString(
								new FileInputStream(expectedOutput), UTF8);
						String actual = serialize(results);
						assertEquals(expected, actual);
					} else if (compare.equals("XML")) {
						Document expected = buildDocument(expectedOutput);
						if (query.toString().indexOf("XQuery/UseCase/") >= 0) {
							// input doc should not have whitespace
							XOMUtil.Normalizer.STRIP.normalize(expected); // ???
						}
						Document actual = XOMUtil.toDocument(serialize(results));
						assertEquals(expected, actual);				
					} else if (compare.equals("Fragment")) {				
						Document expected = buildFromSequence(expectedOutput);
						Document actual = buildFromSequence(results);
						assertEquals(expected, actual);
					} else if (compare.equals("Ignore")) {
                        // nothing to do
					} else if (compare.equals("Inspect")) { 
						System.out.println("****************** Inspect output?");
					} else {
						throw new RuntimeException(
							"Unrecognized comparison operator: " + compare);
					}			
					break; // found a match; break out of "for" loop
				} catch (ConformanceException e) {
					if (k == expectedOutputs.size()-1) { // done trying all expected outputs?
						System.out.println(XOMUtil.toPrettyXML(testCase));
						throw e;				
					}
				}
			}		
//			System.out.println("Passed.");
		}
		System.out.println("\nFinished testing. Good bye.");
	}
	
	private void assertEquals(String expected, String actual) {
		if (!normalize(expected).equals(normalize(actual))) {
//			fail(new Text(expected), new Text(actual));
			fail(expected, actual);
		}
	}
	
	private void assertEquals(Document expected, Document actual) throws UnsupportedEncodingException {
		int window = 20;
		byte[] e = XOMUtil.toCanonicalXML(expected);
		byte[] a = XOMUtil.toCanonicalXML(actual);
		if (!Arrays.equals(e, a)) {
			// print snippet of the offending area to gain some debugging clues
			if (e.length != a.length) {
				System.out.println("e.length="+ e.length + ", a.length=" + a.length);
			}
			int size = Math.min(e.length, a.length);
			for (int i=0; i < size; i++) {
				if (e[i] != a[i]) {
					System.out.println("diff at i=" + i + ", e[i]=" + e[i] + 
							", a[i]=" + a[i]);
					int off = Math.max(0, i-window);
					int len1 = Math.min(2*window, e.length-off);
					int len2 = Math.min(2*window, a.length-off);
					System.out.println("e='"+ new String(e, off, len1, StandardCharsets.UTF_8) + "'");
					System.out.println("a='"+ new String(a, off, len2, StandardCharsets.UTF_8) + "'");
//					System.out.println("e1='"+ new String(e, "UTF-8") + "'");
//					System.out.println("a1='"+ new String(a, "UTF-8") + "'");
					break;
				}
			}
//			fail(expected, actual);
			fail(expected.toXML(), actual.toXML());
		}
	}
	
	private void fail(Object expected, Object actual) {
		throw new ConformanceException(
			"\nexpected='" + expected + "', \nactual  ='" + actual + "'");		
	}
	
	private String normalize(String text) {
		Element wrapper = new Element("dummy");
		wrapper.appendChild(text);
		XOMUtil.Normalizer.COLLAPSE.normalize(wrapper); // ???
		return wrapper.getValue();
	}
	
	private Document buildDocument(File file) throws Exception {
		return BuilderPool.GLOBAL_POOL.getBuilder(false).build(file);
	}
	
	private Document buildFromSequence(File file) {
		String xml = "<!DOCTYPE doc [<!ENTITY e SYSTEM '" + file.toURI() + 
				"'>]><doc>&e;</doc>";
		Document doc = XOMUtil.toDocument(xml);
		if (true) XOMUtil.Normalizer.COLLAPSE.normalize(doc); // needed by SeqUnion/fn-union-node-args-004.xq et al
		return doc;
	}
	
	private Document buildFromSequence(Nodes nodes) throws IOException {
		File tmp = new File(System.getProperty("java.io.tmpdir"));
		tmp = new File(tmp, "xqts-tmp.out");
		Writer out = new OutputStreamWriter(new FileOutputStream(tmp), UTF8);
		out.write(serialize(nodes));
		out.flush();
		out.close();	
		Document doc = buildFromSequence(tmp); // reparse
		tmp.delete();
		return doc;
	}
	
	private String serialize(Nodes nodes) {
		ResultSequenceSerializer serializer = new ResultSequenceSerializer();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		String xml;
		try {
			serializer.write(nodes, out);
			xml = out.toString("UTF-8"); // safe: UTF-8 support is required by JDK spec
		} catch (IOException e) {
			throw new RuntimeException("should never happen", e);
		}
		
		// remove XML declaration header <?xml version="1.0" encoding="UTF-8"?>\r\n
		// remove trailing line break, if any
		xml = xml.substring(xml.indexOf('>') + 1);
		if (xml.startsWith("\r\n")) xml = xml.substring(2);
		int j = xml.length();
		if (xml.endsWith("\r\n")) j = j - 2;
		else if (xml.endsWith("\n")) j = j - 1;
		return xml.substring(0, j);
	}

	private String readQuery(File query) throws IOException {
		if (ignore(query)) return null;		
		String squery = FileUtil.toString(new FileInputStream(query), UTF8);	
		return substituteSchema(squery);		
	}
	
	/** Nux is not schema aware: replace schema refs as suggested by W3C */
	private String substituteSchema(String query) {
		String toFind = "import schema default element namespace";
		int i = query.indexOf(toFind);
		if (i >= 0) { // ForExprType?
			System.out.println("************* Substituting 'import schema default element namespace' with 'declare default element namespace'.");
			System.out.println("before='" + query + "'");
			query = query.substring(0, i) + "declare default element namespace" + query.substring(i + toFind.length());
			System.out.println("after='" + query + "'");
		}
		
		toFind = "import schema namespace";
		i = query.indexOf(toFind);
		if (i >= 0) { 
			System.out.println("************* Substituting 'import schema namespace' with 'declare namespace'.");
			System.out.println("before='" + query + "'");
			query = query.substring(0, i) + "declare namespace" + query.substring(i + toFind.length());
			System.out.println("after='" + query + "'");
		}
		
		toFind = "import schema";
		i = query.indexOf(toFind);
		if (i >= 0) {
			System.out.println("************* Substituting 'import schema' with 'declare default element namespace'.");
			System.out.println("before='" + query + "'");
			if (query.indexOf("declare default element namespace") < 0) {
				query = query.substring(0, i) + "declare default element namespace" + query.substring(i + toFind.length());
			} else { // there is already a default element namespace; remove import schema "xyz"; 
				int j = query.indexOf(';', i);
				query = query.substring(0, i) + query.substring(j+1);
			}
			System.out.println("after='" + query + "'");
		}
		return query;
	}
	
	// these are 99% bugs in the *expected* results of the W3C test suite
	// rather than in the actual XQuery impl (!) 
	private boolean ignore(File query) {
		String s = query.toString();
//		if (!contains(s, "UseCaseR")) return true;
		
		// XPath -> XOM data model mismatches (these are the only things inherently non-fixable):
		if (endsWith(s, "ConDocNode/Constr-docnode-nested-1.xq")) return true; // Cannot add a nu.xom.Text to a Document
		if (endsWith(s, "ConDocNode/Constr-docnode-enclexpr-1.xq")) return true; // Missing document root element; A XOM document must have a root element
		if (endsWith(s, "ConDocNode/Constr-docnode-enclexpr-2.xq")) return true; // Cannot add a nu.xom.Text to a Document
		if (endsWith(s, "ConDocNode/Constr-docnode-enclexpr-3.xq")) return true; // Cannot add a nu.xom.Text to a Document
		if (endsWith(s, "ConDocNode/Constr-docnode-enclexpr-4.xq")) return true; // Cannot add a nu.xom.Text to a Document
		if (endsWith(s, "ConDocNode/Constr-docnode-doc-1.xq")) return true; // A XOM document must not have more than one root element
		if (endsWith(s, "ConText/Constr-text-nested-3.xq")) return true; // Cannot add a nu.xom.Text to a Document
		if (endsWith(s, "ComputeConComment/Constr-compcomment-nested-3.xq")) return true; // A XOM document must not have more than one root element

		
		// misc:
//		if (endsWith(s, "SeqUnion/fn-union-node-args-017.xq")) return true; // W3C bug (missing inscope namespaces)
//		if (contains(s, "DayTimeDurationDivideDTD")) return true; // same pattern 
		
//		if (endsWith(s, "DirectConElemContent/Constr-cont-constrmod-2.xq")) return true; // W3C bug
		if (endsWith(s, "DirectConElemContent/Constr-cont-constrmod-4.xq")) return true; // double -> int
//		if (endsWith(s, "DirectConElemContent/Constr-cont-constrmod-6.xq")) return true; // W3C bug
		if (endsWith(s, "DirectConElemContent/Constr-cont-constrmod-8.xq")) return true; // double -> int		
		if (endsWith(s, "ComputeConElem/Constr-compelem-compname-9.xq")) return true; // W3C bug: undeclared prefix
//		if (endsWith(s, "ComputeConElem/Constr-compelem-constrmod-2.xq")) return true; // text normalization bug
//		if (endsWith(s, "ComputeConElem/Constr-compelem-constrmod-4.xq")) return true; // double -> int
//		if (endsWith(s, "ComputeConElem/Constr-compelem-constrmod-6.xq")) return true; // W3C bug
//		if (endsWith(s, "ComputeConElem/Constr-compelem-constrmod-8.xq")) return true; // double -> int
	
		if (endsWith(s, "ComputeConAttr/Constr-compattr-compname-9.xq")) return true; // // W3C bug: undeclared prefix
				
//		if (endsWith(s, "ConDocNode/Constr-docnode-constrmod-2.xq")) return true; // W3C bug
//		if (endsWith(s, "ConDocNode/Constr-docnode-constrmod-4.xq")) return true; // double -> int
			
		if (endsWith(s, "ComputeConComment/Constr-compcomment-dash-4.xq")) return true; // file not found (should be emptydoc)
		if (endsWith(s, "ComputeConComment/Constr-compcomment-doubledash-4.xq")) return true; // file not found (should be emptydoc)
		
//		if (endsWith(s, "OrderbyExprWith/orderBy25.xq")) return true; // W3C bug number formatting: 100000000000000000 vs. 1.0E17
//		if (endsWith(s, "OrderbyExprWith/orderBy27.xq")) return true; // number formatting????
//		if (endsWith(s, "OrderbyExprWith/orderBy35.xq")) return true; // number formatting????
//		if (endsWith(s, "OrderbyExprWith/orderBy45.xq")) return true; // number formatting????
//		if (endsWith(s, "OrderbyExprWith/orderBy55.xq")) return true; // number formatting????
//		if (endsWith(s, "OrderbyExprWith/orderBy57.xq")) return true; // number formatting: 0 vs. -0 
		
//		if (endsWith(s, "SeqExprCast/casthcds9.xq")) return true; // number formatting
//		if (endsWith(s, "SeqExprCast/casthcds10.xq")) return true; // number formatting
//		if (endsWith(s, "SeqExprCast/casthcds13.xq")) return true; // number formatting
//		if (endsWith(s, "SeqExprCast/casthcds14.xq")) return true; // number formatting
		
		if (endsWith(s, "Catalog/Catalog001.xq")) return true; // file not found (should be ../xyz)
		if (endsWith(s, "Catalog/Catalog002.xq")) return true; // file not found (should be ../xyz)
		if (endsWith(s, "Catalog/Catalog003.xq")) return true; // file not found (should be ../xyz)
		
//		if (contains(s, "DurationDateTimeOp/gYearMonthEQ/op-gYearMonth-equalNew-2.xq")) return true; 
		
		
		// xqts-0.8.0:
//		if (contains(s, "PathExpr/Predicates")) return true; 
//		if (endsWith(s, "PathExpr/Predicates/predicates-1.xq")) return true; // W3C namespace bug
//		if (endsWith(s, "PathExpr/Predicates/predicates-3.xq")) return true; // W3C namespace bug
//		if (endsWith(s, "PathExpr/Predicates/predicates-10.xq")) return true; // W3C namespace bug
			
//		if (contains(s, "ForExprType")) return true; 		
//		if (endsWith(s, "ForExprType/ForExprType002.xq")) return true; // Required item type of value of variable $fileName is attribute(name, {http://www.w3.org/2005/xpath-datatypes}untypedAtomic); supplied value has item type attribute(name, {http://www.w3.org/2005/xpath-datatypes}untyped)
		if (endsWith(s, "ForExprType/ForExprType009.xq")) return true; // -0 vs. 0
		if (endsWith(s, "ForExprType/ForExprType009-1.xq")) return true; // -0 vs. 0
		if (endsWith(s, "ForExprType/ForExprType010.xq")) return true; // Required item type of value of variable $num is xs:decimal; supplied value has item type xdt:untypedAtomic
////		if (endsWith(s, "ForExprType/ForExprType022.xq")) return true; // W3C bug static errors
////		if (endsWith(s, "ForExprType/ForExprType023.xq")) return true; // W3C bug static errors
		if (endsWith(s, "ForExprType/ForExprType024.xq")) return true; // W3C bug static errors
		if (endsWith(s, "ForExprType/ForExprType025.xq")) return true; // Required item type of value of variable $test is attribute(*, xs:decimal); supplied value has item type attribute(integer)
		if (endsWith(s, "ForExprType/ForExprType026.xq")) return true; // No schema has been imported for namespace 'http://typedecl'
		if (endsWith(s, "ForExprType/ForExprType027.xq")) return true; // No schema has been imported for namespace 'http://typedecl'
		if (endsWith(s, "ForExprType/ForExprType038.xq")) return true; // No schema has been imported for namespace 'http://typedecl'
		if (endsWith(s, "ForExprType/ForExprType039.xq")) return true; // No schema has been imported for namespace 'http://typedecl'
		if (contains(s, "ForExprType/ForExprType04")) return true; 
		if (contains(s, "ForExprType/ForExprType05")) return true; 
		
//		if (endsWith(s, "TranslateFunc/fn-translate-15.xq")) return true; // number formatting	
//		if (endsWith(s, "EscapingFuncs/EscapeHTMLURIFunc/fn-escape-html-uri-20.xq")) return true; 
//		if (endsWith(s, "EscapingFuncs/EscapeHTMLURIFunc/fn-escape-html-uri-21.xq")) return true; 		
		if (DISABLE_URI_CHECK && endsWith(s, "QNameFunc/QNameConstructFunc/ExpandedQNameConstructFunc/ExpandedQNameConstructFunc018.xq")) return true; // Missing scheme in absolute URI reference
		if (contains(s, "QNameFunc/LocalNameFromQnameFunc/")) return true; // W3C typo: file not found
		if (contains(s, "QNameFunc/NamespaceURIFromQNameFunc/")) return true; // W3C typo: file not found

		// needed by sun-jdk-1.5.0-xerces-internal bug (begin/end entity)
		// bug appears not to be present in xerces-2.7.1 and crimson 
//		if (endsWith(s, "DirectConOther/Constr-pi-content-3.xq")) return true;
//		if (endsWith(s, "ComputeConPI/Constr-comppi-empty-1.xq")) return true;
//		if (endsWith(s, "ComputeConPI/Constr-comppi-empty-2.xq")) return true;	
		
		
		// xqts-0.8.2:
//		if (endsWith(s, "QNameOp/QNameEQ/op-qname-equal-15.xq")) return true; // W3C bug QName has null namespace but non-empty prefix
//		if (contains(s, "QNameOp/QNameEQ")) return true; // W3C bug QName has null namespace but non-empty prefix
//		if (endsWith(s, "DirectConElemAttr/Constr-attr-id-2.xq")) return true; // W3C bug Value of xml:id must be a valid NCName
//		if (endsWith(s, "DirectConElemContent/Constr-cont-nsmode-2.xq")) return true; // missing URI scheme
//		if (endsWith(s, "ComputeConAttr/Constr-compattr-id-2.xq")) return true; // W3C bug NCNames cannot start with the character 20 
//		if (endsWith(s, "OrderbyExprWith/orderbylocal-45.xq")) return true; // number formatting: 1.0E-6 vs. 0.000001 
//		if (endsWith(s, "OrderbyExprWith/orderbylocal-55.xq")) return true; // W3C bug: superfluous namespace
//		if (endsWith(s, "BaseURIProlog/base-URI-10.xq")) return true; // ??? fn:static-base-uri()
//		if (endsWith(s, "BaseURIProlog/base-URI-18.xq")) return true; // amphersand
		if (DISABLE_URI_CHECK && endsWith(s, "NamespaceProlog/namespaceDecl-17.xq")) return true; // nu.xom.MalformedURIException: Missing scheme in absolute URI reference
		if (DISABLE_URI_CHECK && endsWith(s, "NamespaceProlog/namespaceDecl-20.xq")) return true; // nu.xom.MalformedURIException: Missing scheme in absolute URI reference
		if (DISABLE_URI_CHECK && endsWith(s, "NamespaceProlog/namespaceDecl-21.xq")) return true; // nu.xom.MalformedURIException: Missing scheme in absolute URI reference
		if (DISABLE_URI_CHECK && endsWith(s, "NamespaceProlog/namespaceDecl-23.xq")) return true; // nu.xom.MalformedURIException: Missing scheme in absolute URI reference
//		if (endsWith(s, "BaseURIFunc/fn-base-uri-17.xq")) return true; // saxon bug?		
		if (endsWith(s, "DocumentURIFunc/fn-document-uri-12.xq")) return true; // saxon or nux bug: missing document URI with doc passed as variable rather than doc("xyz")		
		if (endsWith(s, "DocumentURIFunc/fn-document-uri-15.xq")) return true; // saxon or nux bug: missing document URI with doc passed as variable rather than doc("xyz")		
		if (endsWith(s, "DocumentURIFunc/fn-document-uri-16.xq")) return true; // saxon or nux bug: missing document URI with doc passed as variable rather than doc("xyz")		
		if (contains(s, "DocumentURIFunc")) return true; // saxon or nux bug: missing document URI with doc passed as variable rather than doc("xyz")
//		if (endsWith(s, "ABSFunc/fn-abs-more-args-001.xq")) return true; // W3C bug 0.0E0 vs 0	
//		if (endsWith(s, "ABSFunc/fn-abs-more-args-008.xq")) return true; // W3C bug 0.0 vs 0	
//		if (endsWith(s, "NormalizeUnicodeFunc/fn-normalize-unicode-1.xq")) return true; // ??? 
//		if (endsWith(s, "ResolveURIFunc/fn-resolve-uri-2.xq")) return true; // ???
//		if (endsWith(s, "NamespaceURIForPrefixFunc/fn-namespace-uri-for-prefix-4.xq")) return true; // saxon NPE
		if (endsWith(s, "SeqIDFunc/fn-id-5.xq")) return true; // W3C bug file not found
		if (contains(s, "NodeSeqFunc/SeqIDFunc/fn-id-")) return true; // W3C bug: typo: file not found
		
		
		// xqts-0.8.4:
		if (DISABLE_URI_CHECK && endsWith(s, "DirectConElemContent/Constr-cont-constrmod-1.xq")) return true; // W3C bug Missing scheme in absolute URI reference
		if (DISABLE_URI_CHECK && endsWith(s, "DirectConElemContent/Constr-cont-nsmode-1.xq")) return true; // W3C bug Missing scheme in absolute URI reference
		if (endsWith(s, "DirectConElem/DirectConElemContent/Constr-cont-nsmode-3.xq")) return true; // xmlns:inherit="inherit"
		if (endsWith(s, "DirectConElem/DirectConElemContent/Constr-cont-nsmode-4.xq")) return true; // xmlns:inherit="inherit" 
//		if (endsWith(s, "EmptyOrderProlog/emptyorderdecl-4.xq")) return true; // NaN sort order
//		if (endsWith(s, "EmptyOrderProlog/emptyorderdecl-5.xq")) return true; // NaN sort order
//		if (endsWith(s, "EmptyOrderProlog/emptyorderdecl-12.xq")) return true; // NaN sort order
//		if (endsWith(s, "EmptyOrderProlog/emptyorderdecl-13.xq")) return true; // NaN sort order
//		if (endsWith(s, "EmptyOrderProlog/emptyorderdecl-25.xq")) return true; // NaN sort order
//		if (endsWith(s, "EmptyOrderProlog/emptyorderdecl-27.xq")) return true; // NaN sort order
//		if (endsWith(s, "ExtensionExpression/extexpr-6.xq")) return true; // W3C bug empty file
//		if (endsWith(s, "RoundEvenFunc/fn-round-half-to-evendbl1args-1.xq")) return true; // numeric precision
//		if (endsWith(s, "RoundEvenFunc/fn-round-half-to-evendbl1args-3.xq")) return true; // numeric precision
//		if (endsWith(s, "NormalizeSpaceFunc/fn-normalize-space-2.xq")) return true; // saxon bug NPE
//		if (endsWith(s, "SeqAVGFunc/fn-avgdbl2args-2.xq")) return true; // numeric precision
//		if (endsWith(s, "ContextImplicitTimezoneFunc/fn-implicit-timezone-9.xq")) return true; // diff zone
//		if (endsWith(s, "FullAxis/precedingAxis/preceding-8.xq")) return true; // W3C bug: seq >> emptySeq
		
		
		// xqts-0.8.6:
//		if (endsWith(s, "NotationOp/NotationEQ/Comp-notation-2.xq")) return true; // ???
//		if (endsWith(s, "NotationOp/NotationEQ/Comp-notation-4.xq")) return true; // ???
		if (endsWith(s, "NotationOp/NotationEQ/Comp-notation-5.xq")) return true; // ???
		if (endsWith(s, "NotationOp/NotationEQ/Comp-notation-8.xq")) return true; // ???
		if (endsWith(s, "NotationOp/NotationEQ/Comp-notation-10.xq")) return true; // ???
		if (contains(s, "NotationOp/NotationEQ/")) return true; // ???
		if (endsWith(s, "SeqOp/SeqExcept/fn-except-node-args-003.xq")) return true; // W3C bug: whitespace
//		if (endsWith(s, "SeqExprCast/casthc14.xq")) return true; // numeric precision
		// FIXME: need to implement <input-query variable="x" name="extvardeclwithtypetobind-17" date="2006-02-09"/>
		// FIXME: need to implement module location hints
		if (endsWith(s, "VariableProlog/ExternalVariablesWithout/extvardeclwithouttype-1.xq")) return true; // DynamicError: No value supplied for required parameter $x
		if (endsWith(s, "VariableProlog/ExternalVariablesWithout/extvardeclwithouttype-2.xq")) return true; // DynamicError: No value supplied for required parameter $x
		if (contains(s, "VariableProlog/ExternalVariablesWithout/")) return true; // ???
		if (contains(s, "VariableProlog/ExternalVariablesWith/")) return true; // ???
//		if (endsWith(s, "BaseURIFunc/fn-base-uri-9.xq")) return true; // escaping
//		if (endsWith(s, "BaseURIFunc/fn-base-uri-11.xq")) return true; // ???
//		if (endsWith(s, "AllStringFunc/Surrogates/surrogates13.xq")) return true; // ???
		if (endsWith(s, "AllStringFunc/MatchStringFunc/MatchesFunc/caselessmatch04.xq")) return true; // ???
//		if (endsWith(s, "SeqBooleanFunc/fn-boolean-mixed-args-049.xq")) return true; // EBV undefined
//		if (endsWith(s, "SeqAVGFunc/fn-avgdbl2args-4.xq")) return true; // numeric precision
//		if (endsWith(s, "SeqAVGFunc/fn-avg-mix-args-009.xq")) return true; // numeric precision
//		if (endsWith(s, "SeqAVGFunc/fn-avg-mix-args-013.xq")) return true; // numeric precision
//		if (endsWith(s, "SeqAVGFunc/fn-avg-mix-args-015.xq")) return true; // numeric precision
//		if (contains(s, "/SeqIDREFFunc/")) return true; // An empty sequence is not allowed as the second argument of fn:idref()
		if (endsWith(s, "SeqIDREFFunc/fn-idref-4.xq")) return true; // W3C bug: file not found
		if (endsWith(s, "SeqIDREFFunc/fn-idref-5.xq")) return true; // W3C bug: file not found
		if (endsWith(s, "SeqIDREFFunc/fn-idref-6.xq")) return true; // W3C bug: file not found
		if (contains(s, "/SeqIDREFFunc/fn-idref-")) return true; // W3C bug: file not found
//		if (endsWith(s, "SeqDocFunc/fn-doc-5.xq")) return true; // DynamicError: invalid relative URI
//		if (endsWith(s, "SeqDocFunc/fn-doc-8.xq")) return true; // file not found
//		if (endsWith(s, "SeqDocFunc/fn-doc-9.xq")) return true; // file not found
//		if (contains(s, "SeqDocFunc/fn-doc-")) return true; // DynamicError: invalid relative URI
		if (endsWith(s, "ModuleImport/modules-two-import-ok.xq")) return true; // static error
		if (endsWith(s, "UseCaseSEQ/seq-queries-results-q5.xq")) return true; // W3C bug: whitespace
		
//		if (endsWith(s, "SeqExprCast/casthc23.xq")) return true; // numeric precision
//		if (endsWith(s, "SeqExprCast/casthcds2.xq")) return true; // numeric precision
//		if (endsWith(s, "SeqExprCast/casthcds7.xq")) return true; // numeric precision
//		if (endsWith(s, "SeqExprCast/casthcds8.xq")) return true; // numeric precision
//		if (endsWith(s, "SeqExprCast/casthcds23.xq")) return true; // numeric precision
//		if (endsWith(s, "VariableProlog/InternalVariablesWith/vardeclwithtype-6.xq")) return true; // numeric precision		
//		if (endsWith(s, "NodeNumberFunc/fn-numberintg1args-2.xq")) return true; // numeric precision
//		if (endsWith(s, "NodeNumberFunc/fn-numberdec1args-2.xq")) return true; // numeric precision
//		if (endsWith(s, "NodeNumberFunc/fn-numberflt1args-1.xq")) return true; // numeric precision
		if (endsWith(s, "NodeNumberFunc/fn-numberflt1args-3.xq")) return true; // numeric precision
//		if (endsWith(s, "NodeNumberFunc/fn-numberlng1args-2.xq")) return true; // numeric precision
////		if (endsWith(s, "NodeNumberFunc/fn-numbernint1args-2.xq")) return true; // numeric precision
//		if (endsWith(s, "NodeNumberFunc/fn-numberpint1args-2.xq")) return true; // numeric precision
//		if (endsWith(s, "NodeNumberFunc/fn-numberulng1args-2.xq")) return true; // numeric precision
//		if (endsWith(s, "NodeNumberFunc/fn-numberulng1args-3.xq")) return true; // numeric precision
		if (endsWith(s, "NodeNumberFunc/fn-numbernpi1args-2.xq")) return true; // numeric precision
//		if (endsWith(s, "AggregateSeqFunc/SeqAVGFunc/fn-avgdbl2args-1.xq")) return true; // numeric precision
		
		// saxonb-8.7, fixed in saxonb-8.7.1:
//		if (endsWith(s, "PathExpr/Steps/Axes/Axes014.xq")) return true; // numeric precision
//		if (endsWith(s, "PathExpr/Steps/Axes/Axes015.xq")) return true; // numeric precision
//		if (endsWith(s, "ForExprType/ForExprType032.xq")) return true; // numeric precision
//		if (endsWith(s, "ReturnExpr/ReturnExpr007.xq")) return true; // numeric precision
		
		// saxonb-8.7.1-final:
//		if (contains(s, "EscapingFuncs/EncodeURIfunc/fn-encode-for-uri")) return true; // escaping
		
		
		// xqts-0.9.0:
		if (endsWith(s, "FilterExpr/filterexpressionhc5.xq")) return true; // W3C bug: pretty printing
//		if (endsWith(s, "NumericComp/NumericEqual/value-comparison-3.xq")) return true; // import: missing hatSize() function?
//		if (endsWith(s, "NumericComp/NumericEqual/value-comparison-4.xq")) return true; // import: missing hatSize() function?
//		if (endsWith(s, "DurationDateTimeOp/DurationEQ/op-duration-equal-26.xq")) return true; // invalid duration value
//		if (endsWith(s, "DurationDateTimeOp/DurationEQ/op-duration-equal-27.xq")) return true; // invalid duration value
//		if (endsWith(s, "QNameOp/PrefixFromQName/fn-prefix-from-qname-2.xq")) return true; // Required item type of first argument of fn:prefix-from-QName() is xs:QName; supplied value has item type xs:integer
		if (endsWith(s, "QNameOp/PrefixFromQName/fn-prefix-from-qname-3.xq")) return true; // ???
//		if (endsWith(s, "DirectConOther/Constr-pi-content-3.xq")) return true; // org.xml.sax.SAXParseException: XML document structures must start and end within the same entity.
//		if (endsWith(s, "ComputeConPI/Constr-comppi-empty-1.xq")) return true; // org.xml.sax.SAXParseException: XML document structures must start and end within the same entity.
//		if (endsWith(s, "ComputeConPI/Constr-comppi-empty-2.xq")) return true; // org.xml.sax.SAXParseException: XML document structures must start and end within the same entity.
		if (endsWith(s, "FLWORExpr/ForExpr/ForExpr005.xq")) return true; // W3C bug: whitespace indentation
		if (endsWith(s, "FLWORExpr/ReturnExpr/ReturnExpr004.xq")) return true; // W3C bug: pretty printing
//		if (endsWith(s, "OrderbyExprWith/orderBy55.xq")) return true; // formatting -0 vs. 0
//		if (endsWith(s, "SeqExprCastWithinBranch/cast-within-3.xq")) return true; // cast: -10 vs. 10
//		if (endsWith(s, "SeqExprCastWithinBranch/cast-within-4.xq")) return true; // cast: -10 vs. 10
		if (endsWith(s, "SeqExprCast/qname-cast-1.xq")) return true; // static error: cast as qname
		if (endsWith(s, "SeqExprCast/qname-cast-2.xq")) return true; // static error: cast as qname
		if (endsWith(s, "SeqExprCast/qname-cast-3.xq")) return true; // static error: cast as qname
		if (endsWith(s, "SeqExprCast/qname-cast-4.xq")) return true; // static error: cast as qname
//		if (endsWith(s, "SeqExprCast/notation-cast-2.xq")) return true; // W3C bug: file not found
		if (endsWith(s, "SeqExprCast/notation-cast-3.xq")) return true; // There is no imported schema for namespace http://www.w3.org/XQueryTest/userDefinedTypes
//		if (endsWith(s, "BaseURIProlog/base-uri-25.xq")) return true; // escaping space
		if (endsWith(s, "FunctionDeclaration/function-declaration-008.xq")) return true; // W3C bug: whitespace indentation
		if (endsWith(s, "FunctionDeclaration/function-declaration-009.xq")) return true; // W3C bug: whitespace indentation
		if (endsWith(s, "NilledFunc/fn-nilled-5.xq")) return true; // ???
		if (endsWith(s, "NilledFunc/fn-nilled-7.xq")) return true; // ???
		if (endsWith(s, "NilledFunc/fn-nilled-12.xq")) return true; // ???
		if (endsWith(s, "NilledFunc/fn-nilled-15.xq")) return true; // ???
		if (endsWith(s, "NilledFunc/fn-nilled-16.xq")) return true; // ???
		if (endsWith(s, "NilledFunc/fn-nilled-17.xq")) return true; // ???
		if (endsWith(s, "NilledFunc/fn-nilled-23.xq")) return true; // ???
//		if (endsWith(s, "BaseURIFunc/fn-base-uri-23.xq")) return true; // escaping backslash
//		if (contains(s, "/ConstructFunc/UserDefined/user-defined-")) return true; // Cannot find a matching 1-argument function named {http://www.w3.org/XQueryTest/userDefinedTypes}sizeType()
//		if (endsWith(s, "CodepointEqualFunc/fn-codepoint-equal-14.xq")) return true; // ???
//		if (endsWith(s, "CodepointEqualFunc/fn-codepoint-equal-17.xq")) return true; // ???
//		if (contains(s, "/EscapingFuncs/IRIToURIfunc/fn-iri-to-uri-")) return true; // escaping
		if (endsWith(s, "DurationDateTimeFunc/SecondsFromDateTimeFunc/fn-seconds-from-dateTime-13.xq")) return true; // 0 vs. 00
		if (endsWith(s, "AggregateSeqFunc/SeqMAXFunc/fn-max-2.xq")) return true; // W3C bug: 5 vs. 5E0
		if (endsWith(s, "AggregateSeqFunc/SeqMINFunc/fn-min-2.xq")) return true; // W3C bug: 5 vs. 5E0
		if (endsWith(s, "TrivialEmbedding/trivial-1.xq")) return true; // W3C bug: useless garbage
		if (endsWith(s, "TrivialEmbedding/trivial-3.xq")) return true; // W3C bug: useless garbage
		
		
		// xqts-0.9.4:
		if (endsWith(s, "FilterExpr/K-FilterExpr-91.xq")) return true; // saxon NPE
		if (endsWith(s, "NumericAdd/K-NumericAdd-5.xq")) return true; // xs:double ???
		if (endsWith(s, "NumericAdd/K-NumericAdd-6.xq")) return true; // xs:double ???
//		if (endsWith(s, "NumericEqual/K-NumericEqual-16.xq")) return true; // NaN xs:double ???
//		if (endsWith(s, "NumericEqual/K-NumericEqual-17.xq")) return true; // NaN xs:double ???
//		if (endsWith(s, "NumericEqual/K-NumericEqual-18.xq")) return true; // NaN xs:double ???
//		if (endsWith(s, "NumericEqual/K-NumericEqual-30.xq")) return true; // NaN xs:double ???
//		if (endsWith(s, "NumericEqual/K-NumericEqual-31.xq")) return true; // NaN xs:double ???
//		if (endsWith(s, "NumericEqual/K-NumericEqual-32.xq")) return true; // NaN xs:double ???
		
		if (endsWith(s, "DurationDateTimeOp/TimeGT/op-time-greater-than-2.xq")) return true;
//		if (endsWith(s, "DurationDateTimeOp/gYearMonthEQ/K-gYearMonthEQ-1.xq")) return true;
//		if (endsWith(s, "DurationDateTimeOp/gYearEQ/K-gYearEQ-1.xq")) return true;
//		if (endsWith(s, "DurationDateTimeOp/gMonthDayEQ/K-gMonthDayEQ-1.xq")) return true;
//		if (endsWith(s, "DurationDateTimeOp/gMonthEQ/K-gMonthEQ-1.xq")) return true;
//		if (endsWith(s, "DurationDateTimeOp/gDayEQ/K-gDayEQ-1.xq")) return true;
		
		if (endsWith(s, "CompExpr/GenComprsn/GenCompLT/K-GenCompLT-4.xq")) return true; // saxon StringIndexOutoufBoundsException
		if (endsWith(s, "CompExpr/NodeComp/NodeBefore/K-NodeBefore-4.xq")) return true;
//		if (contains(s, "QuantExpr/QuantExprWith/K-QuantExprWith-")) return true; //  net.sf.saxon.trans.StaticError: Cardinality of range variable must be exactly one
		if (endsWith(s, "seqExprTypes/SeqExprCast/K-SeqExprCast-70.xq")) return true; // net.sf.saxon.trans.StaticError: The argument of a QName or NOTATION constructor must be a string literal
		if (endsWith(s, "seqExprTypes/SeqExprCast/K-SeqExprCast-394.xq")) return true; // casting a xs:date with UTC timezone to xs:gYear
		if (endsWith(s, "seqExprTypes/SeqExprCast/K-SeqExprCast-396.xq")) return true; // casting a xs:date with UTC timezone to xs:gYearMonth
		if (endsWith(s, "seqExprTypes/SeqExprCast/K-SeqExprCast-959.xq")) return true; // W3C Bug: whitespace???
//		if (endsWith(s, "seqExprTypes/SeqExprCast/K-SeqExprCast-1011.xq")) return true;// W3C Bug: whitespace???
//		if (endsWith(s, "seqExprTypes/SeqExprCast/K-SeqExprCast-1063.xq")) return true;// W3C Bug: whitespace???
//		if (endsWith(s, "seqExprTypes/SeqExprCast/K-SeqExprCast-1115.xq")) return true; // same as above
//		if (endsWith(s, "seqExprTypes/SeqExprCast/K-SeqExprCast-1167.xq")) return true; // same as above
//		if (endsWith(s, "seqExprTypes/SeqExprCast/K-SeqExprCast-1323.xq")) return true; // DynamicError: Invalid hexadecimal digit
		if (endsWith(s, "seqExprTypes/SeqExprCast/K-SeqExprCast-1427.xq")) return true; // DynamicError: Invalid QName {    ncname    }
		if (endsWith(s, "seqExprTypes/SeqExprCastable/K-SeqExprCastable-19.xq")) return true; // Casting an xs:string to xs:QName works
		if (endsWith(s, "seqExprTypes/SeqExprCastable/K-SeqExprCastable-21.xq")) return true; // Casting an empty sequence to xs:QName does not work	
		if (endsWith(s, "exprSeqTypes/SeqExprCastable/CastableAs647.xq")) return true; // Try string literal castable as xs:QName
		
		if (endsWith(s, "PrologExpr/FunctionProlog/K-FunctionProlog-1.xq")) return true; // The 'XPath Data Types' namespace is not reserved anymore
		if (endsWith(s, "PrologExpr/FunctionProlog/K-FunctionProlog-66.xq")) return true; // StaticError: A function call that reminds of the range expression 
		if (endsWith(s, "ExtensionExpression/K-ExtensionExpression-6.xq")) return true; // static error: pragma expression containing complex content
		if (endsWith(s, "ExtensionExpression/K-ExtensionExpression-8.xq")) return true; // StaticError: Invalid character '#' in expression
		if (endsWith(s, "DurationDateTimeFunc/SecondsFromDurationFunc/fn-seconds-from-duration-20.xq")) return true;
		if (endsWith(s, "SeqSubsequenceFunc/K-SeqSubsequenceFunc-21.xq")) return true; // saxon java.lang.NegativeArraySizeException
		if (endsWith(s, "SeqDeepEqualFunc/K-SeqDeepEqualFunc-8.xq")) return true; // deep-equal(xs:float("NaN"), xs:float("NaN"))
		if (endsWith(s, "SeqDeepEqualFunc/K-SeqDeepEqualFunc-9.xq")) return true; // deep-equal(xs:float("NaN"), xs:float("NaN"))
		if (endsWith(s, "SeqDeepEqualFunc/K-SeqDeepEqualFunc-10.xq")) return true; // deep-equal(xs:float("NaN"), xs:float("NaN"))
		if (endsWith(s, "SeqDeepEqualFunc/K-SeqDeepEqualFunc-11.xq")) return true; // deep-equal(xs:float("NaN"), xs:float("NaN"))
		if (endsWith(s, "SeqMAXFunc/K-SeqMAXFunc-13.xq")) return true; // max((1, xs:float(2), xs:untypedAtomic("3"))) instance of xs:double`
		if (endsWith(s, "SeqMAXFunc/K-SeqMAXFunc-15.xq")) return true; // max((1, xs:float(2), xs:untypedAtomic("3"))) instance of xs:double`
		if (endsWith(s, "SeqMINFunc/K-SeqMINFunc-13.xq")) return true;
		if (endsWith(s, "SeqMINFunc/K-SeqMINFunc-15.xq")) return true;
		if (endsWith(s, "SchemaImport/SchemaImportProlog/modules-schema-context.xq")) return true; // static error: library module that imports a schema
		if (endsWith(s, "Modules/ModuleImport/modules-simple.xq")) return true; // static error: Import simple library module.
		if (endsWith(s, "Modules/ModuleImport/modules-2.xq")) return true; // static error: namespace eval
		if (endsWith(s, "Modules/ModuleImport/modules-4.xq")) return true; // static error: Evaluation of actual usage of variable from imported module
		if (endsWith(s, "Modules/ModuleImport/modules-5.xq")) return true; // static error
		if (endsWith(s, "Modules/ModuleImport/modules-6.xq")) return true; // static error
		if (endsWith(s, "Modules/ModuleImport/modules-7.xq")) return true; // static error
		if (endsWith(s, "Modules/ModuleImport/modules-8.xq")) return true; // static error
		if (endsWith(s, "Modules/ModuleImport/modules-9.xq")) return true; // static error
		if (contains(s, "Modules/ModuleImport/modules-1")) return true; // static error
        return endsWith(s, "UseCase/UseCaseR/rdb-queries-results-q4.xq"); // static error
    }
	
	// operating system insensitive file name comparison
	protected static boolean endsWith(String x, String y) {
		x = x.replace('/', File.separatorChar);
		x = x.replace('\\', File.separatorChar);
		y = y.replace('/', File.separatorChar);
		y = y.replace('\\', File.separatorChar);
		
		return x.endsWith(y);
	}
	
	// operating system insensitive file name comparison
	protected static boolean contains(String x, String y) {
		x = x.replace('/', File.separatorChar);
		x = x.replace('\\', File.separatorChar);
		y = y.replace('/', File.separatorChar);
		y = y.replace('\\', File.separatorChar);
		
		return x.indexOf(y) >= 0;
	}

	private static final class ConformanceException extends RuntimeException {
		private ConformanceException(String msg) { 
			super(msg); 
		}
	}
	
}
