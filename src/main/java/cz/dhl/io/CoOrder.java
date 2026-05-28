/*
 * Visit url for update: http://sourceforge.net/projects/jvftp
 * 
 * JvFTP was developed by http://sourceforge.net/users/bpetrovi
 * The sources was donated to sourceforge.net under the terms 
 * of GNU Lesser General Public License (LGPL). Redistribution of any 
 * part of JvFTP or any derivative works must include this notice.
 */
package cz.dhl.io;

/**
 * Allows to compare CoFile objects.
 * 
 * @version 0.72 08/10/2003
 * @author Bea Petrovicova   
 * @see cz.dhl.io.CoFile
 */
public interface CoOrder extends CoComparable {
	/** Compares two abstract pathnames lexicographically by name. 
	 * @exception ClassCastException */
    int compareNameToIgnoreCase(CoOrder file);

	/** Compares two abstract pathnames lexicographically by extension. 
	 * @exception ClassCastException */
    int compareExtToIgnoreCase(CoOrder file);

	/** Tests this abstract pathname whether 
	 * name starts with the given character. */
    boolean startsWithIgnoreCase(char ch);

	/** Tests this abstract pathname for equality with the given extension.
	 * @param filter must be uppercase string with a leading '.' sign; 
	 * example: ".TXT" or ".HTM" or ".HTML" etc ... */
    boolean equalsExtTo(String filter);

	/** Tests this abstract pathname for equality with one of the given extensions.
	 * @param filter must be array of uppercase strings with a leading '.' sign; 
	 * example: { ".TXT", ".HTM", ".HTML", etc ... } */
    boolean equalsExtTo(String[] filter);

	/** Compares two abstract pathnames lexicographically (by pathname). */
    int compareTo(Object o);

	/** Tests if corresponding connection to remote host is active. */
    boolean isConnected();
}
