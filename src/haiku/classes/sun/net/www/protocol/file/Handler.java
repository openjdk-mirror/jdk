/*
 * Copyright 1994-2003 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.net.www.protocol.file;

import java.net.InetAddress;
import java.net.URLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLStreamHandler;
import java.io.InputStream;
import java.io.IOException;
import sun.net.www.ParseUtil;
import java.io.File;

/**
 * Open an file input stream given a URL.
 * @author	James Gosling
 * @version 	1.44, 01/12/03
 */
public class Handler extends URLStreamHandler {

    private String getHost(URL url) {
	String host = url.getHost();
	if (host == null)
	    host = "";
	return host;
    }


    protected void parseURL(URL u, String spec, int start, int limit) {
	/*
	 * Ugly backwards compatibility. Flip any file separator
	 * characters to be forward slashes. This is a nop on Unix
	 * and "fixes" win32 file paths. According to RFC 2396,
	 * only forward slashes may be used to represent hierarchy
	 * separation in a URL but previous releases unfortunately
	 * performed this "fixup" behavior in the file URL parsing code
	 * rather than forcing this to be fixed in the caller of the URL
	 * class where it belongs. Since backslash is an "unwise"
	 * character that would normally be encoded if literally intended
	 * as a non-seperator character the damage of veering away from the
	 * specification is presumably limited.
	 */
	super.parseURL(u, spec.replace(File.separatorChar, '/'), start, limit);
    }

    public synchronized URLConnection openConnection(URL u)
           throws IOException {
	String host = u.getHost();
	if (host == null || host.equals("") || host.equals("~") ||
	    host.equals("localhost")) {
	    File file = new File(ParseUtil.decode(u.getPath()));
	    return createFileURLConnection(u, file);
	} 

	/* If you reach here, it implies that you have a hostname
	   so attempt an ftp connection.
	 */
	URLConnection uc;
	URL ru;

	try {
	    ru = new URL("ftp", host, u.getFile() +
                             (u.getRef() == null ? "": "#" + u.getRef()));
	    uc = ru.openConnection();
	} catch (IOException e) {
	    uc = null;
	}
	if (uc == null) {
	    throw new IOException("Unable to connect to: " +
                                                       u.toExternalForm());
	}
	return uc;
    }

    // Template method to be overriden by Java Plug-in. [stanleyh]
    //
    protected URLConnection createFileURLConnection(URL u, File file)
    {
       return new FileURLConnection(u, file);
    }
}
