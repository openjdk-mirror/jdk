/*
 * Copyright 2000-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package java.util.prefs;
import java.util.*;
import java.io.*;
import java.util.logging.Logger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;


/**
 * Preferences implementation for Haiku!  
 * Preferences are stored in the file system, under 
 * B_USER_SETTINGS_DIRECTORY + java/prefs. Each node is a Directory within it's
 * parent. Keys and values are stored in attributes.
 * 
 * This is a very basic implementation of the SPI methods, with plenty
 * of room for improvement in the future. IE Caching. All methods are currently
 * implemented atomically.
 * 
 * @author Bryan Varner
 * @version 1.0 2/23/2003
 * @see	 Preferences
 * @since 1.4
 */
class HaikuPreferences extends AbstractPreferences {
	/** 
	 * Logger for error messages. Backing store exception are logged at
	 * WARNING level.
	 */
	private static final Logger logger = Logger.getLogger("java.util.prefs"); 
	
	// The Node we Update!
	private final File prefsFile;
	private boolean fileError = false;
	
	static HaikuPreferences userRoot   = new HaikuPreferences(true);
	static HaikuPreferences systemRoot = new HaikuPreferences(false);

	// User dir
	static native String getUserPrefsDir0();
	// System dir
	static native String getSystemPrefsDir0();

	/**
	 * Special constructor for roots (both user and system).  This constructor
	 * will only be called twice, by the static intializer.
	 */
	private HaikuPreferences(boolean user) {
		super(null, "");
		prefsFile = new File(user ? getUserPrefsDir0(): getSystemPrefsDir0());
	}
	
	/**
	 * Construct a new HaikuPreferences instance with the specified
	 * parent node and name.  This constructor, called from childSpi,
	 * is used to make every node except for the two //roots.
	 */
	private HaikuPreferences(HaikuPreferences parent, String name) {
		super(parent, name);
		prefsFile = new File(parent.prefsFile, dirName(name));
		checkFileOk();
	}
	
	// Checks to make sure the file is A OK.
	private boolean checkFileOk() {
		return (
				AccessController.doPrivileged( new PrivilegedAction() {
					public Object run() {				
						if (! prefsFile.exists()) {
							if (! prefsFile.mkdirs()) {
								logger.warning("Could not create node: " + prefsFile.getName());
								return null;
							}
						}
						return new Object();
					}
				}) != null);
	}

	// -- Put -- 
	protected void putSpi(String key, String value) {
		try {
			if (checkFileOk())
				putSpi0(prefsFile.getCanonicalPath(), key, value);
			else
				fileError = true;
		} catch (IOException e) {
			logger.warning(e.toString());
			fileError = true;
		}
	}
	
	private static native void putSpi0(String fileName, String key, String value);
	

	// -- Get -- 
	protected String getSpi(String key) {
		checkFileOk();
		try {
			return getSpi0(prefsFile.getCanonicalPath(), key);
		} catch (IOException e) {
			logger.warning(e.toString());
			return key;
		}
	}
	
	private static native String getSpi0(String fileName, String key);
	
	// -- Remove Key/Value--
	protected void removeSpi(String key) {
		try {
			if (checkFileOk())
				removeSpi0(prefsFile.getCanonicalPath(), key);
			else
				fileError = true;
		} catch (IOException e) {
			logger.warning(e.toString());
			fileError = true;
		}
	}
	
	private static native void removeSpi0(String fileName, String key);
	
	
	// -- String[] keys --

	protected String[] keysSpi() {
		checkFileOk();
		try {
			return keysSpi0(prefsFile.getCanonicalPath());
		} catch (IOException e) {
			logger.warning(e.toString());
			return new String[0];
		}
	}
	
	private static native String[] keysSpi0(String fileName);
	
	protected String[] childrenNamesSpi() {
		checkFileOk();
		return (String[]) 
			AccessController.doPrivileged( new PrivilegedAction() {
				public Object run() {	
					List result = new ArrayList();
					File[] dirContents = prefsFile.listFiles();
					if (dirContents != null) {
						for (int i = 0; i < dirContents.length; i++)
							if (dirContents[i].isDirectory())
								result.add(nodeName(dirContents[i].getName()));
					}
					return result.toArray(new String[0]);
			   }
			});
	}
	
	protected AbstractPreferences childSpi(String name) {
		return new HaikuPreferences(this, name);
	}
	
	public void removeNodeSpi() throws BackingStoreException {
		try {
			AccessController.doPrivileged( new PrivilegedExceptionAction() {
				public Object run() throws BackingStoreException {
					if (! prefsFile.delete()) {
						throw new BackingStoreException(prefsFile.getName() + 
							"Failed to delete");
					}
					return null;
				}
			});
		} catch (PrivilegedActionException pae) {
			throw (BackingStoreException) pae.getException();
		}
	}

	// We're atomic, nothing to sync
	public synchronized void syncSpi() throws BackingStoreException {
		if (fileError) {
			fileError = false; // reset, throw, 'faa-getaboudit' as The Don would say
			throw new BackingStoreException("At some point in time or another" +
					" an error occurred attempting to access your node." + 
					" Sorry I can't be more specific.");
		}
		return;
	}
	
	// We're atomic, nothing to flush
	public void flushSpi() throws BackingStoreException {
		return;
	}
	

	/**
	 * Returns true if the specified character is appropriate for use in
	 * Unix directory names.  A character is appropriate if it's a printable
	 * ASCII character (> 0x1f && < 0x7f) and unequal to slash ('/', 0x2f),
	 * dot ('.', 0x2e), or underscore ('_', 0x5f).
	 */
	private static boolean isDirChar(char ch) {
		return ch > 0x1f && ch < 0x7f && ch != '/' && ch != '.' && ch != '_';
	}
	
	/**
	 * Returns the directory name corresponding to the specified node name.
	 * Generally, this is just the node name.  If the node name includes
	 * inappropriate characters (as per isDirChar) it is translated to Base64.
	 * with the underscore  character ('_', 0x5f) prepended. 
	 */
	private static String dirName(String nodeName) {
		for (int i=0, n=nodeName.length(); i < n; i++)
			if (!isDirChar(nodeName.charAt(i)))
				return "_" + Base64.byteArrayToAltBase64(byteArray(nodeName));
		return nodeName;
	}

	/**
	 * Translate a string into a byte array by translating each character
	 * into two bytes, high-byte first ("big-endian").
	 */
	private static byte[] byteArray(String s) {
		int len = s.length();
		byte[] result = new byte[2*len];
		for (int i=0, j=0; i<len; i++) {
			char c = s.charAt(i);
			result[j++] = (byte) (c>>8);
			result[j++] = (byte) c;
		}
		return result;
	}
	
	/**
	 * Returns the node name corresponding to the specified directory name.
	 * (Inverts the transformation of dirName(String).
	 */
	private static String nodeName(String dirName) {
		if (dirName.charAt(0) != '_')
			return dirName;
		byte a[] = Base64.altBase64ToByteArray(dirName.substring(1));
		StringBuffer result = new StringBuffer(a.length/2);
		for (int i = 0; i < a.length; ) {
			int highByte = a[i++] & 0xff;
			int lowByte =  a[i++] & 0xff;
			result.append((char) ((highByte << 8) | lowByte));
		}
		return result.toString();
	}
	
	static synchronized Preferences getUserRoot() {
		return userRoot;
	}
	
	static synchronized Preferences getSystemRoot() {
		return systemRoot;
	}
}
