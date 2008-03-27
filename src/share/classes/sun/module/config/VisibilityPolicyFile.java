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

package sun.module.config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.module.ModuleDefinition;
import java.module.Version;
import java.module.VersionConstraint;
import java.module.VisibilityPolicy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import sun.module.Debug;
import sun.module.JamUtils;

/**
 * This class represents a visibility policy file.
 *
 * @since 1.7
 */
public final class VisibilityPolicyFile implements VisibilityPolicy {

    /**
     * This class represents an entry in the visibility policy file.
     */
    private static class Entry {
        private final String name;
        private final VersionConstraint constraint;
        private final boolean visible;

        /**
         * Constructs a <code>Entry</code> object.
         *
         * @param name module's name to match.
         * @param constraint version constraint for matching module's version.
         * @pararm visible visibility of the module.
         */
        Entry(String name, VersionConstraint constraint, boolean visible) {
            this.name = name;
            this.constraint = constraint;
            this.visible = visible;
        }

        /**
         * Checks if the module's name and version is contained within this entry.
         *
         * @param moduleName module's name.
         * @param version module's version.
         * @return true if the module's name and version is contained within this entry;
         *         otherwise, false is returned.
         */
        boolean contains(String moduleName, Version version) {
            if (this.name.equals("*")) {
                return constraint.contains(version);
            }
            else if (this.name.endsWith(".*")) {
                // Only remove '*' but not '.'
                String s = this.name.substring(0, this.name.length() - 1);
                return (moduleName.indexOf(s) == 0 && constraint.contains(version));
            }
            else {
                // no wildcard
                return (this.name.equals(moduleName) && constraint.contains(version));
            }
        }

        /**
         * @return true if the module should be visible; otherwise, the module
         *         should be hidden and false is returned.
         */
        boolean isVisible() {
            return visible;
        }
    }

    /* Are we debugging? -- for developers */
    private static final Debug mdebug = Debug.getInstance("properties");

    private final List<Entry> entries;

    VisibilityPolicyFile() {
        this.entries = new ArrayList<Entry>();
    }

    /**
     * Add new visibility policy info.
     *
     * @param name module's name to match.
     * @param constraint version constraint for matching module's version.
     * @pararm visible visibility of the module.
     */
    private void add(String name, VersionConstraint constraint, boolean visible) {
        entries.add(new Entry(name, constraint, visible));
    }

    /**
     * Returns true if the module definition should be visible in the
     * repository of the module system. Otherwise, returns false.
     * <p>
     *
     * @param moduleDef the module definition.
     * @return true if the module definition should be visible in the
     *         module system; false otherwise.
     */
    public boolean isVisible(ModuleDefinition moduleDef) {
        String name = moduleDef.getName();
        Version version = moduleDef.getVersion();

        // Check if the module's name and version is contained within
        // one of the entries in the visibility policy.
        for (Entry entry : entries) {
            if (entry.contains(name, version))
                return entry.isVisible();
        }

        // If no entry is matched, return default visibility.
        return true;
    }

    /**
     * Parses the visibility policy file from the url and returns a
     * <code>VisibilityPolicy</code> object that represents the
     * information in the visibility policy file.
     *
     * @param url URL of visibility policy file
     * @return <code>VisibilityPolicy</code> object that represents the
     *         information in the visibility policy file.
     * @throws IOException if any I/O error occurs.
     */
    public static VisibilityPolicy parse(URL url) throws IOException {

        VisibilityPolicyFile result = new VisibilityPolicyFile();
        BufferedReader br = null;
        try {
            // read in visibility policy using UTF-8 by default
            br = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));

            String s = null;
            boolean inComment = false;

            do {
                s = br.readLine();

                if (s == null)
                    break;

                if (mdebug != null) {
                    mdebug.println("read visibility entry: " + s);
                }

                // trim whitespaces
                s = s.trim();

                // skip empty line
                if (s.equals(""))
                    continue;
                // skip various comments
                else if (s.indexOf("/*") == 0)
                    inComment = true;
                else if (inComment && s.indexOf("*") == 0)
                    continue;
                else if (inComment && s.indexOf("*/") == 0)
                    inComment = false;
                else if (s.indexOf("//") == 0)
                    continue;
                else {
                    // parse actual visibility policy entry:
                    // ['+'|'-'], <module-name>
                    // ['+'|'-'], <module-name>, <version-constraint>

                    int idx = s.indexOf(',');
                    if (idx <= 0)
                        throw new IOException("Invalid visibility policy entry: " + s);

                    // Parse visible/invisible field: ['+'|'-']
                    String token = s.substring(0, idx).trim();
                    boolean visible = false;
                    if (token.equals("+"))
                        visible = true;
                    else if (token.equals("-"))
                        visible = false;
                    else
                        throw new IOException("Invalid visibility policy entry: " + s);

                    String name = null;
                    VersionConstraint constraint = VersionConstraint.DEFAULT;

                    int idx2 = s.indexOf(',', idx + 1);

                    if (idx2 == -1) {
                        name = s.substring(idx+1).trim();
                        if (name.equals(""))
                            throw new IOException("Invalid visibility policy entry: " + s);
                    } else {
                        name = s.substring(idx+1, idx2).trim();
                        try {
                            constraint = VersionConstraint.valueOf(s.substring(idx2+1).trim());
                        } catch (IllegalArgumentException  iae) {
                            throw new IOException("Invalid visibility policy entry: " + s, iae);
                        }
                    }

                    // Count number of wildcard to determine if it is used correctly in
                    // the module's name
                    int wildcard = 0;
                    for (int i=0; i < name.length(); i++) {
                        char c = name.charAt(i);
                        if (c == '*')
                            wildcard++;
                    }
                    if (wildcard > 1 ||
                        (wildcard == 1 && !(name.equals("*") || (name.length() > 2 && name.endsWith(".*")))))
                        throw new IOException("Invalid visibility policy entry: " + s);


                    if (mdebug != null) {
                        mdebug.println("recognize visibility entry: module-name="
                                       + name + ", constraint=" + constraint
                                       + ", visible=" + visible);
                    }

                    result.add(name, constraint, visible);
                }
            }
            while (s != null);
        } catch (IOException ioe) {
            if (mdebug != null) {
                mdebug.println("unable to load visibility policy file from "
                                + url);
            }
            throw ioe;
    } finally {
            JamUtils.close(br);
        }

        return result;
    }
}
