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

import java.io.File;

/**
 * Abstract base class that eases development and maintenance of command line
 * programs.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 * @author $Author: hoschek3 $
 * @version $Revision: 1.6 $, $Date: 2005/07/18 06:58:05 $
 */
abstract class CmdLineProgram { // not a public class!

	/** Executes the program with the given command line arguments. */
	protected void doMain(String[] args) {
		try {
			if (parseArguments(args) < 0) return;
			run();
		} catch (UsageException e) {
			System.err.println(getProgramName() + ": " + e.getMessage());
			System.err.println(getShortUsage());
			System.exit(-1);
		} catch (Throwable t) {
			System.err.print("Oopsla: ");
			t.printStackTrace(System.err);
			System.exit(-1);
		}	
	}
	
	/** Parses and validates the given command line arguments. */
	protected abstract int parseArguments(String[] args) throws Throwable;
	
	/** Runs the program (after previously having parsed arguments. */
	protected abstract void run() throws Throwable;
	
	/** Returns the author's email for displaying program info. */
	protected abstract String getMailAddress();
	
	/** Returns the program's name for displaying program info. */
	protected abstract String getProgramName();
	
	/** Returns the program's version for displaying program info. */
	protected abstract String getVersion();
	
	/** Returns the program's home page for displaying program info. */
	protected abstract String getHomepage();
		
	/** Returns the program's overall options. */
	protected String getOptions() {
		return "[OPTION]... [FILE]...";
	}
	
	/** Returns the program's overall usage message. */
	protected String getShortUsage() {
		return 	
		"\nUsage: " + getProgramName() + " " + getOptions() +"\n\n"+
		"Try `" + getProgramName() + " --help' for detailed options.";
	}
	
	/** Returns the program's version info. */
	protected String getVersionInfo() {
		return	
		"\n" + getProgramName() + " " + getVersion() + 
		" [Java " + 
		System.getProperty("java.version") + ", " + 
		System.getProperty("java.vm.name") + 
		"]" + "\n" +
		"See " + getHomepage() + " for the latest version.\n\n"+
		
		getLicense() + "\n\n"+
	
		"Originally written by " + getMailAddress() + ".";
	}
	
	/** Returns the program's copyright and licene. */
	protected String getLicense() {
		return "Copyright (c) 2005, The Regents of the University of California, through\n" + 
		"Lawrence Berkeley National Laboratory (subject to receipt of any required\n" + 
		"approvals from the U.S. Dept. of Energy).  All rights reserved.\n" +
		"See the license file for more details.";
	}
	
	/** 
	 * Utility parse method for illegal/invalid option checking.
	 */ 
	protected int parseIntGreaterThanZero(String arg, String optionName) {
		int val; 
		try {
			val = Integer.parseInt(arg);
			if (val <= 0) throw new NumberFormatException();
		}
		catch (NumberFormatException exc) {
			throw new UsageException("Invalid option '"+optionName+"="+arg+"'. " + 
					"Argument must be integer > 0.", exc);
		}
		return val;
	}
	
	/** 
	 * Utility parse method for illegal/invalid option checking.
	 */ 
	protected int parseInt(String arg, String optionName) {
		int val; 
		try {
			val = Integer.parseInt(arg);
		}
		catch (NumberFormatException exc) {
			throw new UsageException("Invalid option '"+optionName+"="+arg+"'. " + 
					"Argument must be integer.", exc);
		}
		return val;
	}
	
	/**
	 * Yet another utility parse method for illegal/invalid option checking.
	 */ 
	protected void checkValidity(String arg, String[] permitted, String optionName) {
		StringBuffer str = new StringBuffer();
		for (int i=0; i < permitted.length; i++) {
			if (arg.equalsIgnoreCase(permitted[i])) return;
			
			str.append("'").append(permitted[i]).append("'");
			if (i < permitted.length-1) str.append("|");
		}
		throw new UsageException("Invalid option '" + optionName + "=" + arg + "'. " + 
				"Argument must be one of " + str + ".");
	}

	/**
	 * Parse non-option command line arguments (option arguments have already been parsed).
	 */	
	protected String[] parseNonOptionArguments(String[] args, int getOptind, boolean checkFile, 
			int minArgs, int maxArgs) {
		
		if (minArgs < 0) throw new IllegalArgumentException("minArgs must be >= 0");
		if (maxArgs < 0) maxArgs = Integer.MAX_VALUE;
		
		for (int i = getOptind; i < args.length; i++) {
			//log.debug("Non-option argv element: " + args[i] + "\n");
		}
		if (getOptind > args.length - minArgs) {
			throw new UsageException("Further non-option arguments required but missing.");
		}
		if (getOptind < args.length - maxArgs) {
			throw new UsageException("Too many non-option arguments given; at most " 
					+ maxArgs + " allowed.");
		}
			
		String[] nonOptionArgs = new String[args.length - getOptind];
		for (int i = getOptind, j=0; i < args.length; i++, j++) {
			String nonOptionArg = args[i];
			nonOptionArgs[j] = nonOptionArg;
			if (checkFile && (! nonOptionArg.equals("-")) && (! new File(nonOptionArg).exists())) {
				throw new UsageException("File '"+nonOptionArg+"' not found.");
			}
		}
		return nonOptionArgs;
	}
	
	/**
	 * An exception used when a user defines invalid or illegal command line arguments.
	 */
	static final class UsageException extends RuntimeException {

		public UsageException() {
			super();
		}
		
		public UsageException(String s) {
			super(s);
		}
		
		public UsageException(String message, Throwable cause) {
			super(message, cause);
		}
		
		public UsageException(Throwable cause) {
			super(cause);
		}
	}
	
}
