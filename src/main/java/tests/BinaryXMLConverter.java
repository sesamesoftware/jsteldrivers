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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import nu.xom.Builder;
import nu.xom.NodeFactory;
import nux.xom.binary.BinaryXMLCodec;
import nux.xom.io.StreamingSerializer;
import nux.xom.io.StreamingSerializerFactory;
import nux.xom.pool.XOMUtil;

/**
 * Command-line demo that converts a set of standard textual XML files to and
 * from binary xml (bnux) files; Bnux files are indicated by a ".bnux" file name
 * extension. Output files are written in the same directory as the input files,
 * with the ".bnux" file extension added or stripped, respectively.
 * <p>
 * Example usage:
 * 
 * <pre>
 * export CLASSPATH=lib/nux.jar:lib/saxon8.jar:lib/xom.jar
 * 
 * # convert a set of XML files to bnux:
 * java -server nux.xom.tests.BinaryXMLConverter samples/shakespeare/*.xml
 * 
 * # convert a set of bnux files to XML:
 * java -server nux.xom.tests.BinaryXMLConverter samples/shakespeare/*.bnux
 * 
 * # redirecting System.in and System.out:
 * java nux.xom.tests.BinaryXMLConverter < samples/data/p2pio-receive.xml > p2pio-receive.xml.bnux
 * java nux.xom.tests.BinaryXMLConverter < samples/data/p2pio-receive.xml.bnux > p2pio-receive.xml
 * 
 * # display bnux file as textual XML to System.out:
 * java nux.xom.tests.BinaryXMLConverter < samples/data/p2pio-receive.xml.bnux
 * </pre>
 * 
 * @see BinaryXMLCodec
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.17 $, $Date: 2005/12/23 00:01:43 $
 */
public final class BinaryXMLConverter {
	
	private BinaryXMLConverter() {}

	/**
	 * Runs the demo program. 
	 */
	public static void main(String[] args) throws Exception {
		// some non-standard options for some more in-depth testing:
		int compressionLevel = Integer.getInteger("nux.xom.tests.BinaryXMLConverter.compressionLevel", 0).intValue();
		int runs = Integer.getInteger("nux.xom.tests.BinaryXMLConverter.runs", 1).intValue();
		boolean readOnly = Boolean.getBoolean("nux.xom.tests.BinaryXMLConverter.readOnly");
		
		StreamingSerializerFactory factory = new StreamingSerializerFactory();
		BinaryXMLCodec codec = new BinaryXMLCodec();

		if (args.length == 0) { // simply convert from System.in to System.out
			InputStream in = new BufferedInputStream(System.in);
			if (codec.isBnuxDocument(in)) {
				StreamingSerializer ser = factory.createXMLSerializer(System.out, "UTF-8");
				NodeFactory redirector = XOMUtil.getRedirectingNodeFactory(ser);
				codec.deserialize(in, redirector);
			} else { // it's an XML document (or rubbish)
				StreamingSerializer ser = factory.createBinaryXMLSerializer(System.out, compressionLevel);
				NodeFactory redirector = XOMUtil.getRedirectingNodeFactory(ser);
				new Builder(redirector).build(in);
			}
			return;
		}
		
		for (int run=0; run < runs; run++) {
			long s = System.currentTimeMillis();
			for (int i=0; i < args.length; i++) {
				long start, end;
				String fileName = args[i];
				File file = new File(fileName);
				if (file.isDirectory()) continue; // ignore
				System.out.print(fileName + " --> ");
				InputStream in = new FileInputStream(file);				
				OutputStream out = null;
				
				if (fileName.endsWith(".bnux")) {
					NodeFactory redirector = null; 
					if (readOnly) {
						redirector = XOMUtil.getNullNodeFactory();
					} else {
						String destFileName = fileName.substring(0, fileName.length() - ".bnux".length());
						System.out.print(destFileName);
						out = new FileOutputStream(destFileName);
						StreamingSerializer ser = factory.createXMLSerializer(out, "UTF-8");
						redirector = XOMUtil.getRedirectingNodeFactory(ser);
					}
					
					start = System.currentTimeMillis();
					
					codec.deserialize(in, redirector); // perform conversion				
				}
				else { // it's a textual XML document
					NodeFactory redirector = null; 
					if (readOnly) {
						redirector = XOMUtil.getNullNodeFactory();
					} else {
						String destFileName = fileName + ".bnux";
						System.out.print(destFileName);
						out = new FileOutputStream(destFileName);
						StreamingSerializer ser = codec.createStreamingSerializer(out, compressionLevel);
						redirector = XOMUtil.getRedirectingNodeFactory(ser);
					}

					start = System.currentTimeMillis();
					
					new Builder(redirector).build(file); // perform conversion				
				}
				
				end = System.currentTimeMillis();
				System.out.println(" [ms=" + (end-start) + "]. ");
				in.close();
				if (out != null) out.close();
			}
			long e = System.currentTimeMillis();
			System.out.println("Completed run=" + run + " [ms=" + (e-s) + "].");
			System.out.println("\n");
		}
	}

}