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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Comment;
import nu.xom.DocType;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.ParsingException;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;
import nu.xom.xinclude.XIncludeException;
import nu.xom.xinclude.XIncluder;
import nux.xom.binary.BinaryXMLCodec;
import nux.xom.pool.BuilderFactory;
import nux.xom.pool.BuilderPool;
import nux.xom.pool.DocumentFactory;
import nux.xom.pool.DocumentMap;
import nux.xom.pool.DocumentPool;
import nux.xom.pool.DocumentURIResolver;
import nux.xom.pool.FileUtil;
import nux.xom.pool.PoolConfig;
import nux.xom.pool.XOMUtil;
import nux.xom.pool.XQueryFactory;
import nux.xom.pool.XQueryPool;
import nux.xom.xquery.ResultSequenceSerializer;
import nux.xom.xquery.StreamingPathFilter;
import nux.xom.xquery.StreamingPathFilterException;
import nux.xom.xquery.StreamingTransform;
import nux.xom.xquery.XQuery;
import nux.xom.xquery.XQueryUtil;

import org.xml.sax.EntityResolver;
import org.xml.sax.XMLReader;

/**
 * Nux XQuery test tool with optional schema validation, XInclude and update
 * facility; See the <a href="doc-files/fire-xquery-usage.txt ">online help</a>
 * for a description of all available options, plus examples.
 * <p>
 * Somewhat complex implementation due to the large number of flexible options,
 * and robust handling of all potential issues. For a much simpler example see
 * SimpleXQueryCommand.java.
 * <p>
 * Can also be used as a simple benchmark, measuring XML parsing, XQuery
 * execution and XML serialization, either individually, or in combination (via
 * --iterations, --runs, --out=/dev/null), with and without pooling.
 * <p>
 * For best parsing results, make sure to run with the latest stable Xerces
 * release. (For JDK 1.5, copy the xerces jars into nux/lib/. For JDK 1.4, set
 * environment variable via export
 * JAVA_OPTS='-Djava.endorsed.dirs=/path/to/xerces/lib').
 * <p>
 * When using W3C XML Schema, RelaxNG and TagSoup, make sure to put the xerces
 * jar, MSV jars and tagsoup jar onto the classpath, respectively.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.68 $, $Date: 2006/05/01 22:53:36 $
 */
public final class XQueryCommand extends CmdLineProgram {
	
	/**
	 * Main entry point; run this program without any arguments to get help
	 * (including descriptions of all options).
	 */
	public static void main(String[] args) {
		new XQueryCommand().doMain(args);
	}
	
	private XQueryCommand() {
		initOptions();
	}
		
	private String getLongUsage() {
		String text = 
	
		"\n" + getProgramName() + " - Nux XQuery test tool with optional schema validation.\n\n" +
	
		"Usage: " + getProgramName() + " " + getOptions() + "\n\n"+
	
		"Option names can be abbreviated as long as they remain unambigous.\n"+
		"Option cardinalities: '?' = 0..1, '*' = 0..N, '+' = 1..N, 'def' = default.\n\n" +
	
		"Help options:\n"+
		"  ? --version             Display the version of this program and exit.\n"+
		"  ? --help                Print this help page and exit.\n\n"+
		
		"Query options:\n"+
		"  + --query={STRING}|FILE The XQuery to execute.\n"+	
		"  ? --base=FILE           Resolve relative URIs found in the XQuery (def='.').\n"+	
		"  * --var=NAME:VALUE      Pass external variables to XQuery (def=none).\n\n"+	
	
		"Output options:\n"+
		"  * --out=FILE|/dev/null  File(s) to serialize to (def=stdout).\n"+	
		"  ? --algo=w3c|wrap       Result sequence serialization algorithm (def=w3c).\n"+	
		"  ? --encoding=STRING     Character encoding to serialize with (def=UTF-8).\n"+	
		"  ? --indent=INT          Insert prettyprint indentation; disable=0 (def=4).\n\n"+	
	
		"Validation options for input documents:\n"+
		"  ? --validate=wf|dtd|schema|relaxng|html  Set validation language (def=wf).\n"+	
		"  ? --schema=FILE         e.g. foo.dtd|foo.xsd|foo.rng (def=undefined).\n"+	
		"  ? --namespace=URI       Namespace of schema (def=undefined).\n\n"+	
		
		"Misc options:\n"+
		"  ? --update={STRING}|FILE Apply update XQuery to each item in result sequence.\n"+	
		"  ? --xinclude            Perform W3C XInclude resolution on input files.\n"+	
		"  ? --strip               Remove whitespace-only text nodes from input files.\n"+	
		"  ? --noexternal          Disallow Java extension functions in XQuery.\n" +
		"  ? --filterpath=STRING   Streaming path filter, e.g. '/a/b/c' (def=none).\n" +
		"  ? --filterquery={STRING}|FILE XQuery transforming each filter match (def=.).\n" +
		"  ? --debug               Print full stack trace on exception.\n\n"+
	
		"Benchmarking options:\n"+
		"  ? --runs=INT            Repeat outer loop N times (def=1).\n"+
		"  ? --iterations=INT      Repeat inner loop M times (def=1).\n"+
		"  ? --docpoolcapacity=INT Allow at most N MB memory for document pool (def=0).\n" +
		"  ? --docpoolcompression=-1..9    Use document ZLIB compression level (def=-1).\n" +
		"  ? --nobuilderpool       Disable caching of SAX XMLReaders.\n" +
		"  ? --explain             Print description of optimized XQuery plan.\n"+
		"  ? --xomxpath            Use XOM's XPath engine instead of Nux's XPath engine.\n\n" +
	
//		"Logging:\n"+
//		"  --loglevel=all|trace|debug|info|warn|error|fatal|off (default='info').\n\n"+

		"Examples:\n"+
		"  " + "cd samples/data\n"+
		"  " + getProgramName() + " --query='{doc(\"periodic.xml\")/PERIODIC_TABLE/ATOM[NAME=\"Zinc\"]}'\n"+
		"  " + getProgramName() + " --query='{declare namespace atom = \"http://www.w3.org/2005/Atom\"; doc(\"http://www.tbray.org/ongoing/ongoing.atom\")/atom:feed/atom:entry/atom:title}'\n"+
		"  " + getProgramName() + " --query='{count(//*)}' *.xml\n"+
		"  " + getProgramName() + " --query='{count(//*)}' *.xml.bnux\n"+
		"  " + getProgramName() + " --algo=wrap --query='{//node(), //@*, \"Hello World!\"}' p2pio.xml\n"+ 
		"  " + getProgramName() + " --query=../xmark/q09.xq ../xmark/auction-0.01.xml --out=/tmp/results.out\n"+
		"  " + getProgramName() + " --var=x:2 --var=y:5 --query='{declare variable $x external; declare variable $y external; $x * $y}'\n"+
		"  " + getProgramName() + " --query='{/receive/timeout}' --update='{declare namespace system = \"java:java.lang.System\"; system:currentTimeMillis() + 10000}' p2pio.xml\n"+
		"  " + getProgramName() + " --xinclude --query='{.}' xinclude.xml\n"+
//		"  " + getProgramName() + " --xinclude --query='{//@xml:base}' --update='{()}' xinclude.xml\n"+
		"  " + getProgramName() + " --query=../fulltext/q2-06.xq ../fulltext/full-text.xml\n"+
		"  " + getProgramName() + " --validate=html --query='{//*:img/string(@src)}' ../../doc/index.html\n"+
		"  " + getProgramName() + " --query='{.}' --validate=relaxng --debug --schema=../data-atom/atom.rng ../data-atom/ongoing.xml\n"+
		"  " + getProgramName() + " --query='{.}' --validate=schema --namespace='http://openuri.org/easypo' --schema=ns-order.xsd ns-order.xml\n"+
		"  " + getProgramName() + " --query='{declare namespace util = \"java:nux.xom.pool.FileUtil\"; <files> {for $uri in util:listFiles(\"../shakespeare\", false(), \"*.xml\", \"\") let $kills := count(saxon:discard-document(doc(string($uri)))//LINE[contains(., \"kill\")]) order by $kills return <file><name>{$uri}</name> <killCount>{$kills}</killCount></file> }</files>}'\n" +
		"  " + getProgramName() + " --query='{.}' --validate=schema --namespace='http://openuri.org/easypo' --schema=ns-order.xsd ns-order.xml --out=/dev/null --iter=0 --runs=100000\n"+
		"  " + getProgramName() + " --query='{count(doc(\"periodic.xml\")//*)}' --out=/dev/null --indent=0 --iter=5000 --runs=5 --docpoolcapacity=100 --explain\n";
		
		boolean isWindows = System.getProperty("os.name").startsWith("Windows");
		if (isWindows) { // swap escape apostrophes so examples work also on Windows command line:
			char z = (char) 0;
			text = text.replace('\'', z);    // ' --> 0
			text = text.replace('\"', '\''); // " --> '
			text = text.replace(z, '\"');    // 0 --> "	
		}
		return text;
	}
	
	/** Defines command line options. */
	private void initOptions() {
		// TODO: ???
		// additional input docs via --dir --includes --excludes --recursive
		// --loglevel=debug
		// --optionally validate output as well?
		
		this.sb = new StringBuffer();
		ArrayList options = new ArrayList();
		options.add( new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h') );
		options.add( new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'v') );		
		options.add( new LongOpt("query", LongOpt.REQUIRED_ARGUMENT, sb, 'q') );
		options.add( new LongOpt("base", LongOpt.REQUIRED_ARGUMENT, sb, 'b') );
		options.add( new LongOpt("var", LongOpt.REQUIRED_ARGUMENT, sb, 'P') );
		options.add( new LongOpt("out", LongOpt.REQUIRED_ARGUMENT, sb, 'o') );
		options.add( new LongOpt("algo", LongOpt.REQUIRED_ARGUMENT, sb, 'S') );
		options.add( new LongOpt("encoding", LongOpt.REQUIRED_ARGUMENT, sb, 'E') );
		options.add( new LongOpt("indent", LongOpt.REQUIRED_ARGUMENT, sb, 'I') );	
		options.add( new LongOpt("strip", LongOpt.NO_ARGUMENT, null, 's') );		
		options.add( new LongOpt("update", LongOpt.REQUIRED_ARGUMENT, sb, 'u') );	
		options.add( new LongOpt("xinclude", LongOpt.NO_ARGUMENT, null, 'x') );		
		options.add( new LongOpt("explain", LongOpt.NO_ARGUMENT, null, 'e') );
		options.add( new LongOpt("noexternal", LongOpt.NO_ARGUMENT, null, 'n') );		
		options.add( new LongOpt("runs", LongOpt.REQUIRED_ARGUMENT, sb, 'r') );
		options.add( new LongOpt("iterations", LongOpt.REQUIRED_ARGUMENT, sb, 'i') );
		options.add( new LongOpt("docpoolcapacity", LongOpt.REQUIRED_ARGUMENT, sb, 'C') );
		options.add( new LongOpt("docpoolcompression", LongOpt.REQUIRED_ARGUMENT, sb, 'D') );
		options.add( new LongOpt("nobuilderpool", LongOpt.NO_ARGUMENT, null, 'p') );
		options.add( new LongOpt("debug", LongOpt.NO_ARGUMENT, null, 'd') );		
		options.add( new LongOpt("validate", LongOpt.REQUIRED_ARGUMENT, sb, 'V') ); 
		options.add( new LongOpt("namespace", LongOpt.REQUIRED_ARGUMENT, sb, 'W') ); 
		options.add( new LongOpt("schema", LongOpt.REQUIRED_ARGUMENT, sb, 'w') );
		options.add( new LongOpt("filterpath", LongOpt.REQUIRED_ARGUMENT, sb, 'f') );
		options.add( new LongOpt("filterquery", LongOpt.REQUIRED_ARGUMENT, sb, 'F') );
		options.add( new LongOpt("xomxpath", LongOpt.NO_ARGUMENT, null, 'N') );		
		
////		options.add( new LongOpt("loglevel", LongOpt.REQUIRED_ARGUMENT, sb, 'l') ); setLogLevels(Level.INFO);
			
		this.longOpts = new LongOpt[options.size()];
		options.toArray(this.longOpts);		
	}
	
	private LongOpt[] longOpts = null;
	private StringBuffer sb = null;
	
	// parsed command line options, initialized with defaults:
	private String[] inputFiles = null;
	private final Map variables = new HashMap(); // String -> Object
	private final List queries = new ArrayList(); // String or File
	private URI baseURI = null;
	
	private final List outputFiles = new ArrayList(); // File
	private String algorithm = ResultSequenceSerializer.W3C_ALGORITHM;
	private String encoding = "UTF-8";
	private int indent = 4;
	private boolean stripWhitespace = false;
	private boolean explain = false;
	private int runs = 1;
	private int iterations = 1;
	private long docPoolCapacity = 0;
	private int docPoolCompression = -1;
	private boolean noBuilderPool = false;
	private boolean debug = false;
	private String validate = "wf";
	private String namespace = null;
	private File schema = null;
	private boolean xinclude = false;
	private Object update = null;
	private StreamingPathFilter filter = null;
	private String filterQuery = null;
	private boolean xomXPath = false;
	
	protected int parseArguments(String[] args) {
		if (args.length == 0) {
			System.out.println(getLongUsage());
			return -1;
		}
		
		Getopt getopt = new Getopt(getProgramName(), args, ":", longOpts, true);
		//Getopt getopt = new Getopt(getCommandString(), argv, "-:h:vf:b:pc:u:t:y:w:drq", longOpts, true);
		//getopt.setOpterr(false); // We'll do our own error handling
	
		int c;
		while ((c = getopt.getopt()) != -1) {
//			log.trace("longind="+g.getLongind());
			switch (c) {
				case 'h' : // --help
					System.out.println(getLongUsage());
					return -1;
				case 'v' : // --version
					System.out.println(getVersionInfo());
					return -1;
				case 's' : // --strip
					stripWhitespace = true;
					break;
				case 'x' : // --xinclude
					xinclude = true;
					break;
				case 'e' : // --explain
					explain = true;
					break;
				case 'n' : // --noexternal
					System.setProperty("nux.xom.xquery.XQuery.allowExternalFunctions", "false");
					break;
				case 'd' : // --debug
					debug = true;
					break;
				case 'p' : // --nobuilderpool
					noBuilderPool = true;
					break;
				case 'N' : // --xomxpath
					xomXPath = true;
					break;
				case 0 :  
					String arg = getopt.getOptarg();
					char val = (char) (Integer.valueOf(sb.toString())).intValue();
					String optionName = longOpts[getopt.getLongind()].getName();
//					log.trace("Got long option with value '" + val + "' with argument " + ((arg != null) ? arg : "null"));
					switch (val) {
						case 'q' : // --query
							arg = arg.trim();
							if (arg.startsWith("{") && arg.endsWith("}")) { 
								// query is given inline between curly brackets, ala Saxon command line tool
								queries.add(arg.substring(1, arg.length()-1));
							} else {
								if (arg.equals("nop")) 
									queries.add(null); // disable xquery for benchmarking
								else 
									queries.add(parsePath(arg));
							}
							break;
						case 'u' : // --update
							arg = arg.trim();
							if (arg.startsWith("{") && arg.endsWith("}")) { 
								// update query is given inline between curly brackets, ala Saxon command line tool
								update = arg.substring(1, arg.length()-1);
							} else {
								update = parsePath(arg);
							}
							break;
						case 'b' : // --base
							baseURI = parsePath(arg).toURI();
							break;
						case 'P' : { // --var
							int i = arg.indexOf(':');
							if (i < 0) throw new UsageException("Missing name:value pair");
							String name = arg.substring(0, i).trim();
							String value = arg.substring(i+1);
							if (false) {
								try {
									value = value.substring("doc(".length()-1);
									value = value.substring(1, value.length()-1);
									variables.put(name, new Builder().build(new File(value)));
								} catch (Exception e) {
									throw new UsageException(e);
								}
							} else {
								variables.put(name, value);
							}
							break;
						}
						case 'o' : // --out
							outputFiles.add(parsePath(arg));
							break;
						case 'S' : // --algo
							arg = arg.trim();
							checkValidity(arg, new String[] {
								ResultSequenceSerializer.W3C_ALGORITHM, 
								ResultSequenceSerializer.WRAP_ALGORITHM}, optionName);
							algorithm = arg;
							break;
						case 'E' : // --encoding
							encoding = arg.trim();
							break;
						case 'I' : // --indent
							indent = Math.max(0, parseInt(arg, optionName));
							break;
						case 'r' : // --runs
							runs = parseIntGreaterThanZero(arg, optionName);
							break;
						case 'i' : // --iterations
							iterations = Math.max(0, parseInt(arg, optionName));
							break;
						case 'C' : // --docpoolcapacity
							docPoolCapacity = 1024L * 1024L * parseInt(arg, optionName);
							break;					
						case 'D' : // --docpoolcompression
							docPoolCompression = parseInt(arg, optionName);
							break;					
						case 'V' :  // --validate
							arg = arg.trim();
							checkValidity(arg, new String[] {
								"wf", "dtd", "schema", "relaxng", "html"}, optionName);
							validate = arg;
							break;	
						case 'W' :  // --namespace
							namespace = arg.trim();
							break;	
						case 'w' : { // --schema
							// if the schema file location is a relative path, 
							// xerces interprets it relative to the XML instance document file, 
							// not the current working directory.
							// This may be surprising and errorprone, so we convert it to an absolute path 
							// Also, there are some obscure work arounds to make this work both on Unix and Windows, in all cases...
							schema = parsePath(arg).getAbsoluteFile();
							break;
						}
						case 'f' : // --filterpath
							try {
								filter = new StreamingPathFilter(arg, null);
							} catch (StreamingPathFilterException e) {
								throw new UsageException(e);
							}
							break;	
						case 'F' : // --filterquery
							arg = arg.trim();
							if (arg.startsWith("{") && arg.endsWith("}")) { 
								// query is given inline between curly brackets, ala Saxon command line tool
								filterQuery = arg.substring(1, arg.length()-1);
							} else {
								try {
									filterQuery = FileUtil.toString(
											new FileInputStream(parsePath(arg)), null);
								} catch (IOException e) {
									throw new UsageException(e);
								}
							}
							break;
////						case 'l' : // --loglevel
////							setLogLevels(toLevel(arg));
////							break;
						default :
							throw new InternalError("Oops. Should never reach here. val='" + val + "'");
					}				
					break;
				case ':' :
					throw new UsageException("Option '" + longOpts[getopt.getLongind()].getName() + "' requires an argument");
					//throw new UsageException("Argument missing for option " + (char) g.getOptopt() + ", errname=" + longopts[g.getLongind()].getName());
				case '?' :
					System.err.println(getLongUsage());
					return -1;
//					throw new UsageException("The option '" + (char) g.getOptopt() + "' is not valid");
				default :
					throw new InternalError("Oops. Should never reach here. getopt() returned '" + (char) c + "'");
			}
		}
	
		if (queries.isEmpty()) throw new UsageException("Missing required argument --query");
		inputFiles = parseNonOptionArguments(args, getopt.getOptind(), true, 0, Integer.MAX_VALUE);
		if (inputFiles.length == 0) inputFiles = new String[] { null };
		

		// fill in default, if necessary
		File file = null;
		if (!outputFiles.isEmpty()) file = (File) outputFiles.get(0);
		while (outputFiles.size() < inputFiles.length) outputFiles.add(file);
		
		if (filterQuery == null) filterQuery = ".";
		if (update != null) docPoolCompression = Math.max(0, docPoolCompression);
		
		if (xomXPath) {
			for (int i=0; i < queries.size(); i++) {
				Object query = queries.get(i);
				if (query instanceof File) {
					try {
						query = FileUtil.toString(new FileInputStream((File)query), null);
					} catch (IOException e) {
						throw new UsageException(e);
					}
				}
				queries.set(i, query);
			}
		}
		
		return 0;
	}
		
	/**
	 * Execute the query.
	 */	
	protected void run() throws Exception {		
		try {
			final boolean isBench2 = (runs > 1);
			final boolean isBench = (runs > 1 && iterations > 0);			
			final DocumentPool docPool = createDocumentPool(isBench);

			DocumentURIResolver resolver = new DocumentURIResolver() {
				public Document resolve(String href, String baseURI) throws ParsingException, IOException, TransformerException {
					String systemID = new net.sf.saxon.StandardURIResolver(null).
						resolve(href, baseURI).getSystemId();
//					System.err.println(systemID);
					return docPool.getDocument(URI.create(systemID));
				}
			};

			// prepare XQuery pool
			XQueryPool queryPool = new XQueryPool(
					new PoolConfig(), new XQueryFactory(null, resolver));
			
			ResultSequenceSerializer serializer = new ResultSequenceSerializer();
			serializer.setAlgorithm(algorithm);
			serializer.setEncoding(encoding);
			serializer.setIndent(indent);
			
			// now do the real work
			long runsStart = System.currentTimeMillis();
			for (int run=0; run < runs; run++) {
				if (isBench) {
					System.out.println("\n\n******************************************");
					System.out.println("run = " + run + ":");
				}
				for (int i=0; i < queries.size(); i++) {
					long start = System.currentTimeMillis();
					long serializationTime = 0;
					Object query = queries.get(i);
					XQuery xquery;
					if (query instanceof String) {
						xquery = queryPool.getXQuery((String)query, baseURI);
					} else if (query instanceof File) {
						xquery = queryPool.getXQuery((File)query, baseURI);
					} else {
						xquery = null; // disable XQuery for benchmarking
					}
					
					if (isBench) {
						System.out.println("query = " +query);
					}
					if (explain && run == 0 && xquery != null) {
						System.out.println("explain = \n" + xquery.explain());
					}
					
					XQuery morpher;
					if (update instanceof String) {
						morpher = queryPool.getXQuery((String)update, null);
					} else if (update instanceof File) {
						morpher = queryPool.getXQuery((File)update, null);
					} else {
						morpher = null;
					}
					
					int numSerials = 0;
					for (int j=0; j < inputFiles.length; j++) {
						Document doc = null;
						if (inputFiles[j] != null) {
							doc = docPool.getDocument(new File(inputFiles[j]));
						}
						if (explain && doc != null) {
							System.out.println("stats=" + toStatisticsString(doc));
						}

						for (int iter=0; iter < iterations; iter++) {
							Document doc2 = doc;
							if (morpher != null && doc2 != null) { 
								doc2 = new Document(doc2); // immutable for multiple iterations
							}
							
							// run the query
							Nodes results;
							if (xomXPath) {
								if (doc2 == null) throw new UsageException(
									"A context node is required by XOM's XPath engine, but missing.");
								results = doc2.query((String)query);
							} else if (xquery != null) {
								results = xquery.execute(doc2, null, variables).toNodes();
							} else {
								results = new Nodes(); // disable XQuery for benchmarking
								results.append(doc2);
							}
							
							if (morpher != null) {
								// interpret --query as select, interpret --update as morpher
								for (int k=0; doc2 == null && k < results.size(); k++) {
									doc2 = results.get(k).getDocument();
								}
								XQueryUtil.update(results, morpher, null);
								
								// serialize modified document if there is one
								results = new Nodes();
								if (doc2 != null) results.append(doc2);
							}
							
							// serialize results onto output, if any
							File f = (File) outputFiles.get(j);
							OutputStream out = System.out;
							if (f != null) {
								if (f.getAbsolutePath().equals("/dev/null")) continue;
								out = new FileOutputStream(f);
							}
							
							long serializationStart = System.currentTimeMillis();
							serializer.write(results, out);
							if (out != System.out && out != System.err) out.close();
							serializationTime += System.currentTimeMillis() - serializationStart;
							numSerials++;
						}
					}
					if (isBench && iterations > 0) {
						long end = System.currentTimeMillis();
						System.out.println("\nsecs = " + ((end-start) / 1000.0f));
						System.out.println("queries/sec = " + (inputFiles.length * iterations / ((end-start) / 1000.0f)));
						if (numSerials > 0) {
							System.out.println("\nserialization secs = " + (serializationTime / 1000.0f));
							System.out.println("serializations/sec = " + (numSerials / (serializationTime / 1000.0f)));
						}
					}
				}
			}
			if (isBench2) {
				long runsEnd = System.currentTimeMillis();
				System.out.println("\n\n******************************************");
				System.out.println("total secs = " + ((runsEnd-runsStart) / 1000.0f));
				System.out.println("runs/sec = " + (runs / ((runsEnd-runsStart) / 1000.0f)));
			}
		} catch (RuntimeException e) { // report stack trace only if requested
			if (debug) {
				if (e instanceof UsageException) e = new RuntimeException(e);
				throw e;
			}
			throw new UsageException(e); 	
		} catch (Exception e) {
			if (debug) throw e; 
			throw new UsageException(e); 	
		}
	}

	private DocumentPool createDocumentPool(final boolean isBench) {
		// prepare BuilderPool
		PoolConfig config = new PoolConfig();
		if (noBuilderPool) config.setMaxEntries(0);
		final BuilderPool builderPool;
		
		if (filter == null) {
			builderPool = new BuilderPool(config, new BuilderFactory());
		} else {
			BuilderFactory builderFactory = new BuilderFactory() {
				protected Builder newBuilder(XMLReader parser, boolean validate) {
					StreamingTransform myTransform = new StreamingTransform() {
						public Nodes transform(Element subtree) {
							return XQueryUtil.xquery(subtree, filterQuery);
						}
					};
					return new Builder(parser, validate, filter.createNodeFactory(null, myTransform)); 		
				}
			};
			builderPool = new BuilderPool(config, builderFactory);
		}

		// prepare DocumentFactory and DocumentPool
		DocumentFactory docFactory = new DocumentFactory() {
			public Document createDocument(InputStream input, URI baseURI) 
					throws ParsingException, IOException {
				long start = System.currentTimeMillis();
				Document doc;
				if (baseURI != null && baseURI.getPath().endsWith(".bnux")) {
					if (filter == null) {
						doc = getBinaryXMLFactory().createDocument(input, baseURI);
					} else {
						StreamingTransform myTransform = new StreamingTransform() {
							public Nodes transform(Element subtree) {
								return XQueryUtil.xquery(subtree, filterQuery);
							}
						};
	
						if (input == null && baseURI == null) 
							throw new IllegalArgumentException("input and baseURI must not both be null");
						if (input == null) input = baseURI.toURL().openStream();
						try {
							doc = new BinaryXMLCodec().deserialize(input, filter.createNodeFactory(null, myTransform));
							if (baseURI != null) doc.setBaseURI(baseURI.toASCIIString());
						} finally {
							input.close(); // do what SAX XML parsers do
						}
					}
				} else {
					doc = super.createDocument(input, baseURI);
				}
				if (xinclude) {
					try {
						XIncluder.resolveInPlace(doc, newBuilder());
					} catch (XIncludeException e) {
						throw new ParsingException(e.getMessage(), e);
					}
				}
				if (stripWhitespace) XOMUtil.Normalizer.STRIP.normalize(doc);
				long end = System.currentTimeMillis();
				if (isBench || explain) System.out.println(baseURI + " parse [ms]=" + (end-start));
				return doc;
			}
			
			protected Builder newBuilder() {
				if (validate.equals("wf")) {
					return builderPool.getBuilder(false);
				} else if (validate.equals("dtd")) {
					if (schema == null) return builderPool.getBuilder(true);
					EntityResolver resolver;
					try {
						resolver = new BuilderFactory().createResolver(
									new FileInputStream(schema));
					} catch (IOException e) {
						throw new UsageException(e);
					}
					return builderPool.getDTDBuilder(resolver);
				} else if (validate.equals("schema")) {
					HashMap map = new HashMap();
					if (schema != null) map.put(schema, namespace);
//					return new BuilderFactory().createW3CBuilder(map);
					return builderPool.getW3CBuilder(map);
				} else if (validate.equals("relaxng")) { 
					if (schema == null) throw new UsageException(
							"Missing required argument --schema");
					return builderPool.getMSVBuilder(schema.toURI());
				} else if (validate.equals("html")) { 
					XMLReader parser;
					try {
						parser = (XMLReader) Class.forName("org.ccil.cowan.tagsoup.Parser").newInstance();
					} catch (Exception e) {
						throw new UsageException(e);
					}
					return new Builder(parser);
				} else {
					throw new UsageException("Illegal validate option: " + validate);
				}
			}
		};
		
		return new DocumentPool(
			new DocumentMap(
				new PoolConfig().
					setCompressionLevel(docPoolCompression).
					setCapacity(docPoolCapacity)), 
				docFactory);
	}
	
	protected String getMailAddress() { return "wolfgang.DOT.hoschek.AT.mac.DOT.com"; }
	protected String getProgramName() { return "fire-xquery"; }
	protected String getHomepage()    { return "http://dsd.lbl.gov/nux"; }
	protected String getVersion() { 
		String s = "[";
		if (Package.getPackage("nux.xom.xquery") != null) {
			String version = Package.getPackage("nux.xom.xquery").getImplementationVersion();
			if (version != null) s += "nux-" + version + ", ";
		}
		s += "saxon-" +  net.sf.saxon.Version.getProductVersion();
		if (Package.getPackage("nu.xom") != null) {
			String version = Package.getPackage("nu.xom").getImplementationVersion();
			if (version != null) s += ", xom-" + version;
		}
		s += "]";
		return s;
	}
	
	/** Parses OS insensitive file path, stripping off leading URI scheme, if any */
	private static File parsePath(String path) {
		path = (path == null ? "" : path.trim());
		if (path.startsWith("file://"))  {
			path = path.substring("file://".length());
		} else if (path.startsWith("file:")) { 
			path = path.substring("file:".length());	
		}
		
		if (path.isEmpty() || path.equals(".")) {
			path = System.getProperty("user.dir", "."); // CWD
		} else {	
			// convert separators to native format
			path = path.replace('\\', File.separatorChar);
			path = path.replace('/',  File.separatorChar);
			
			if (path.startsWith("~")) {
				// substitute Unix style home dir: ~ --> user.home
				String home = System.getProperty("user.home", "~");
				path = home + path.substring(1);
			}
		}
		
		return new File(path);
	}

	/**
	 * Returns a statistical summary of the given node (subtree) for
	 * experimental/analytical purposes.
	 * 
	 * @param node
	 *            the node (subtree) for which to calculate statistics
	 * @return a summary representation
	 */
	private static String toStatisticsString(Node node) {
		Statistics stats = new Statistics();
		toStatisticsString(node, stats);
		
		NumberFormat f = NumberFormat.getPercentInstance();
		f.setMaximumFractionDigits(2);
		double nodes = stats.nodes * 1.0;
		double chars = stats.chars * 1.0;
		
		return 
		"[" + 
		"nodes=" + stats.nodes +
		", elements=" + f.format(stats.elements / nodes) +
		", attributes=" + f.format(stats.attributes / nodes) +
		", texts=" + f.format(stats.texts / nodes) +
		", comments=" + f.format(stats.comments / nodes) +
		", pis=" + f.format(stats.pis / nodes) +
		", docTypes=" + f.format(stats.docTypes / nodes) +
		
		", chars=" + stats.chars +
		", tagChars=" + f.format(stats.tagChars / chars) +
		", whitespaceChars=" + f.format(stats.whitespaceChars / chars) +
		", nonASCIIChars=" + f.format(stats.nonASCIIChars / chars) +
//		", memorySizeMB=" + (getMemorySize(node) / (1024.0f * 1024.0f)) +
		"]";
	}
	
	private static void toStatisticsString(Node node, Statistics stats) {
		stats.nodes++;
		String value = "";
		if (node instanceof ParentNode) {
			ParentNode parent = (ParentNode) node;
			for (int i=0; i < parent.getChildCount(); i++) {
				toStatisticsString(parent.getChild(i), stats);
			}
			if (node instanceof Element) {
				stats.elements++;
				Element elem = (Element) node;
				value = elem.getQualifiedName();
				stats.tagChars += value.length();
				for (int j=0; j < elem.getAttributeCount(); j++) {
					toStatisticsString(elem.getAttribute(j), stats);
				}
				// TODO: include additional namespace declarations?
			}
		}
		else {
			if (node instanceof Text) {
				stats.texts++;
			} else if (node instanceof Attribute) { 
				stats.attributes++;
				stats.tagChars += ((Attribute) node).getQualifiedName().length();
			} else if (node instanceof Comment) {
				stats.comments++;
			} else if (node instanceof ProcessingInstruction) {
				stats.pis++;
			} else if (node instanceof DocType) {
				stats.docTypes++;
			}
			value = node.toXML();
		}
		
		stats.chars += value.length();
		for (int i=0; i < value.length(); i++) {
			if (isWhitespace(value.charAt(i))) stats.whitespaceChars++;
			if (value.charAt(i) > 127 || value.charAt(i) < 0) stats.nonASCIIChars++;
		}
	}
	
	/** see XML spec */
	private static boolean isWhitespace(char c) {
		switch (c) {
			case '\t':
            case ' ' :
            case '\r':
            case '\n':
                return true;
            default  : return false;
		}
	}
	
	private static final class Statistics {
		private int nodes;
		private int elements;
		private int texts;
		private int comments;
		private int attributes;
		private int pis;
		private int docTypes;
//		private int namespaces;
		private long chars;
		private long tagChars;
		private long whitespaceChars;
		private long nonASCIIChars;
	}
	
}
