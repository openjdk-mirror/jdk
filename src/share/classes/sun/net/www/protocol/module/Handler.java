/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package sun.net.www.protocol.module;

import java.io.*;
import java.module.*;
import java.net.*;
import java.util.*;

/**
 * This class represents the handler for the module: protocol in
 * JSR 277: Java Module System.
 *
 * The format of the module URL is as follows:
 *
 *    module:<repository-type>[/<source>]!/<module-name>/<version-constraint>
 *
 * where
 *    repository-type:    Type of the repository.
 *    source:             Source location of the repository.
 *    module-name:        Name of the module definition.
 *    version-constraint: Version constraint that matches the module definition.
 *
 * @author Stanley M. Ho
 * @since 1.7.0
 */
public class Handler extends java.net.URLStreamHandler {

    @Override
    protected java.net.URLConnection openConnection(URL u) throws IOException {
        throw new UnknownServiceException("Cannot open connection to a module URL.");
    }

    /**
     * This method is called to parse the string spec into URL for a
     * module protocol.
     *
     * @param url URL to receive the result after the spec is parsed.
     * @param spec URL string to parse.
     * @param start the character position to start parsing at. This is just
     *              past the ':'
     * @param limit the character position to stop parsing at.
     */
    @Override
    protected void parseURL(URL url, String spec, int start, int limit) {

        // protocol must be "module"
        String protocol = url.getProtocol();
        if (protocol.equals("module") == false) {
            throw new RuntimeException("Unexpected protocol: " + protocol);
        }

        String host = "";
        int port = url.getPort();
        String file = "";
        if (start < limit) {
            file = spec.substring(start, limit);
        }

        // string to be parsed must not be empty.
        if (file.equals("")) {
            throw new RuntimeException("Spec is empty in the module URL.");
        }

        // string to be parsed must not have whitespaces.
        for (int i=0; i < file.length(); i++) {
            if (Character.isWhitespace(file.charAt(i)))
                throw new RuntimeException("Whitespace is not allowed in the module URL.");
        }

        // spec must have a "!/"
        int bangSlash = file.indexOf("!/");
        if (bangSlash == -1) {
            throw new RuntimeException("!/ is missing in the module URL.");
        }

        // extracts <repository-type>[/<source>] from the spec.
        String rinfo = file.substring(0, bangSlash);

        // <repository-type>[/<source>] must not be empty.
        if (rinfo == null || rinfo.equals("")) {
            throw new RuntimeException("<repository-type> is missing in the module URL.");
        }

        // <repository-type> must not be empty.
        int slash = rinfo.indexOf('/');
        if (slash == 0) {
            throw new RuntimeException("<repository-type> is missing in the module URL.");
        }

        // if <source> exists, it must be a well-formed URL.
        if (slash > 0) {
            String sourceLocation = rinfo.substring(slash + 1);
            try {
                URL sourceURL = new URL(sourceLocation);
            }
            catch (MalformedURLException e) {
                throw new RuntimeException("<source> is a malformed URL: " + sourceLocation, e);
            }
        }

        // extracts <module-name>/<version-constraint> from the spec.
        String minfo = file.substring(bangSlash + 2);

        // <module-name>/<version-constraint> must be non-empty
        if (minfo == null || minfo.equals("")) {
            throw new RuntimeException("<module-name> is missing in the module URL.");
        }

        // <module-name> must not be empty.
        slash = minfo.indexOf('/');
        if (slash == 0) {
            throw new RuntimeException("<module-name> is missing in the module URL.");
        }

        // <version-constraint> must not be empty.
        if (slash == -1) {
            throw new RuntimeException("<version-constraint> is missing in the module URL.");
        }

        // <version-constraint> must follow the version constraint format.
        String vc = minfo.substring(slash + 1);
        try {
            VersionConstraint.valueOf(vc);
        }
        catch (IllegalArgumentException iae) {
            throw new RuntimeException("Cannot parse version constraint in the module URL: " + vc, iae);
        }

        setURL(url, protocol, host, port, file, null);
    }
}
