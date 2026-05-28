/*
 * Visit url for update: http://sourceforge.net/projects/jvftp
 * 
 * JvFTP was developed by http://sourceforge.net/users/bpetrovi
 * The sources was donated to sourceforge.net under the terms 
 * of GNU Lesser General Public License (LGPL). Redistribution of any 
 * part of JvFTP or any derivative works must include this notice.
 */
package cz.dhl.io;

import cz.dhl.ui.CoConsole;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Defines interface to get input /
 * output stream on CoFile objects.
 * 
 * @version 0.72 08/10/2003
 * @author Bea Petrovicova   
 * @see CoFile
 */
public interface CoOpen {
	/** Get type of data transfer.
	 * @return One of following optional values:
	 * 'A'=ASCII, 'I'=BINARY; */
    char getDataType();
	/** Returns an input stream for this file. */
    InputStream getInputStream() throws IOException;
	/** Returns an output stream for this file. */
    OutputStream getOutputStream(boolean append)
		throws IOException;
	/** Returns an output stream for this file. */
    OutputStream getOutputStream() throws IOException;
	/** Creates a new file instance from this 
	 * abstract pathname and a child string. */
    CoFile newFileChild(String child);
	/** Creates a new file instance from a parent 
	 * of this abstract pathname and a name string. */
    CoFile newFileRename(String name);
	/** Gets console if implemented or null. */
    CoConsole getConsole();
}
