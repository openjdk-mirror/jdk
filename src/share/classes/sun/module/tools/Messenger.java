/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.module.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.security.AccessController;
import java.text.MessageFormat;
import sun.security.action.GetPropertyAction;

/**
 * Provides a means for tools to emit messages.
 */
class Messenger {
    public static final boolean DEBUG;

    static {
        DEBUG = (AccessController.doPrivileged(new GetPropertyAction("sun.module.tools.debug")) != null);
    }

    protected final String program;
    protected final PrintStream out;
    protected final PrintStream err;
    protected final BufferedReader reader;

    Messenger(String program, PrintStream out, PrintStream err, BufferedReader reader) {
        this.program = program;
        this.out = out;
        this.err = err;
        this.reader = reader;
    }

    String getMsg(String key) {
        return key;
        // XXX i18n
//      try {
//          return (rsrc.getString(key));
//      } catch (MissingResourceException e) {
//          throw new Error("Error in message file");
//      }
    }

    String formatMsg(String key, String arg) {
        String msg = getMsg(key);
        String[] args = new String[1];
        args[0] = arg;
        return MessageFormat.format(msg, (Object[]) args);
    }

    /*
     * A fatal exception has been caught.  No recovery possible
     */
    void fatalError(Exception e) {
        e.printStackTrace(err);
        if (DEBUG) e.printStackTrace(System.err);
    }

    /*
     * A fatal condition has been detected; message is "s".
     * No recovery possible
     */
    void fatalError(String s) {
        error(program + ": " + s);
    }

    /**
     * Print an output message; like verbose output and the like; end with a newline.
     */
    void println(String s) {
        out.println(s);
        if (DEBUG) System.out.println(s);
    }

    /**
     * Print an output message; like verbose output and the like.
     */
    void print(String s) {
        out.print(s);
        if (DEBUG) System.out.print(s);
    }

    /**
     * Print an error mesage; like something is broken
     */
    void error(String s) {
        err.println(s);
        if (DEBUG) System.err.println(s);
    }

    void debug(String s) {
        if (DEBUG) System.err.println(s);
    }

    String readLine() throws IOException {
        String rc = reader.readLine();
        if (DEBUG) System.out.println(rc);
        return rc;
    }
}
