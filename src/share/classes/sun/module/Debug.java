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

package sun.module;

/**
 * A utility class for debuging.
 *
 * This class has be shamefully lifted from sun.security.util.Debug
 *
 * @since 1.7
 */
public class Debug {

    private String prefix;

    private static String args;

    static {
        args = java.security.AccessController.doPrivileged
                (new sun.security.action.GetPropertyAction
                ("java.module.debug"));

        if (args != null) {
            if (args.equals("help")) {
                help();
            }
        }
    }

    public static void help()
    {
        System.err.println();
        System.err.println("all           turn on all debugging");
        System.err.println("modinit       module initialization");
        System.err.println("properties    module system properties");
        System.err.println("repository    repository debugging");
        System.err.println();
        System.err.println("Note: Separate multiple options with a comma");
        System.exit(0);
    }

    /**
     * Get a Debug object corresponding to whether or not the given
     * option is set. Set the prefix to be the same as option.
     */

    public static Debug getInstance(String option)
    {
        return getInstance(option, option);
    }

    /**
     * Get a Debug object corresponding to whether or not the given
     * option is set. Set the prefix to be prefix.
     */
    public static Debug getInstance(String option, String prefix)
    {
        if (isOn(option)) {
            Debug d = new Debug();
            d.prefix = prefix;
            return d;
        } else {
            return null;
        }
    }

    /**
     * True if the system property "java.module.debug" contains the
     * string "option".
     */
    public static boolean isOn(String option)
    {
        if (args == null)
            return false;
        else {
            if (args.indexOf("all") != -1)
                return true;
            else
                return (args.indexOf(option) != -1);
        }
    }

    /**
     * print a message to stderr that is prefixed with the prefix
     * created from the call to getInstance.
     */

    public void println(String message)
    {
        System.err.println(prefix + ": "+message);
    }

    /**
     * print a blank line to stderr that is prefixed with the prefix.
     */

    public void println()
    {
        System.err.println(prefix + ":");
    }

    /**
     * print a message to stderr that is prefixed with the prefix.
     */

    public static void println(String prefix, String message)
    {
        System.err.println(prefix + ": "+message);
    }

    private final static char[] hexDigits = "0123456789abcdef".toCharArray();

    public static String toString(byte[] b) {
        if (b == null) {
            return "(null)";
        }
        StringBuilder sb = new StringBuilder(b.length * 3);
        for (int i = 0; i < b.length; i++) {
            int k = b[i] & 0xff;
            if (i != 0) {
                sb.append(':');
            }
            sb.append(hexDigits[k >>> 4]);
            sb.append(hexDigits[k & 0xf]);
        }
        return sb.toString();
    }
}
