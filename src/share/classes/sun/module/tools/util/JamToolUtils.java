/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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
package sun.module.tools.util;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import sun.module.JamUtils;

public class JamToolUtils {
    /**
     * This class provides a set of static utility methods for the jam and
     * jar tools. The directory or filename utilities will convert from
     * platform specific file separators to '/', the '/' being the specified
     * separator for zip entries.
     */

    // a singleton utility class for the tools
    private JamToolUtils() {}

    /**
     * @return the module-inf directory with the trailing slash, like the
     * way zip expects for directory names.
     */
    public static String getModuleInfDir() {
        return JamUtils.MODULE_INF + "/";
    }

    /**
     * Tests whether input string is, a module inf directory, after converting
     * file separator to /.
     * @param in a String denoting a directory name
     * @return true or false
     */
    public static boolean isModuleInfDir(String in) {
        return in.toUpperCase(java.util.Locale.ROOT).equals(getModuleInfDir());
    }

    /**
     * Tests whether the input string  is a module inf entry, after converting
     * file separator is replaced with /, noting that entry
     * could be preceded by other path elements. Note that a trailing
     * "/" will be added if the input string does not contain one.
     * @param in a String denoting a directory name could have have prefix dirs.
     * @return true or false
     */
    public static boolean isModuleInfDirEntry(String in) {
        String s = (in.endsWith("/") ? in : in.concat("/")).replace('\\', '/');
        return s.toUpperCase(java.util.Locale.ROOT).endsWith(getModuleInfDir());
    }

    /**
     * Tests whether the input string is a metadata file, after converting
     * the input from file separators to /.
     * @param in a String denoting an entry name
     * @return true or false
     */
    public static boolean isModuleInfMetaData(String in) {
        return in.replace('\\', '/').toUpperCase(
                java.util.Locale.ROOT).equals(JamUtils.MODULE_INF_METADATA);
    }

    /**
     * Tests whether the string is a module-info.class
     * @param in a String denoting an entry name
     * @return true or false
     */
    public static boolean isModuleInfoClass(String in) {
        return in.toLowerCase(java.util.Locale.ROOT).equals(JamUtils.MODULE_INFO_CLASS);
    }

    /**
     * Tests whether the entry (after converting file separator to /) is
     * module-info.class noting that an entry could be preceded by directory
     * path elements.
     * @param in a String denoting an entry name
     * @return true or false
     */
    public static boolean isModuleInfoClassEntry(String in) {
        return in.replace('\\', '/').toLowerCase(
                java.util.Locale.ROOT).endsWith(JamUtils.MODULE_INFO_CLASS);
    }
}
