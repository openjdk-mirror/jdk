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
import java.io.Reader;
import java.io.StreamTokenizer;
import java.module.ImportDependency;
import java.module.ImportOverridePolicy;
import java.module.ModuleDefinition;
import java.module.ModuleDependency;
import java.module.PackageDependency;
import java.module.Version;
import java.module.VersionConstraint;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sun.module.Debug;

/**
 * This class represents an import override policy file. It consists of zero
 * or more entries in the following format:
 *
 *   module <importer-module-name> [@ <version-constraint>] {
 *        import <type> <imported-name> @ <overridden-version-constraint>;
 *        ...
 *   };
 *
 * For example,
 *   module x.y.z @ [1.0, 1.1) {
 *        import module a.b.c @ 1.7.1;
 *        import module p.q.r @ 2.3.4;
 *   };
 *   ....
 *
 * Each entry must begin with "module", followed by the module name and the
 * version constraint for the importer. If the version constraint is not
 * present, the default version constraint is assumed.
 *
 * Inside each entry, it contains zero or more sub-entry. Each sub-entry begins
 * with the import type, name, followed by the overridden version constraint.
 *
 * The importer module name may be set to the wildcard value, "*", which allows
 * it to match any importer module. In addition, the module name may be suffixed
 * to the wildcard value, ".*", which allows it to match any importer module that
 * has the module name's prefix.
 *
 * For example,
 *   module * {
 *        import module a.b.c @ [1.7, 1.8);
 *        import module p.q.r @ [2.3, 2.4);
 *   };
 *
 *   module x.y.* @ [1.0, 1.1) {
 *        import module a.b.c @ [1.7, 1.8);
 *        import module p.q.r @ [2.3, 2.4);
 *   };
 *
 * When setting the <importer-module-name>, <version-constraint>, <type>,
 * <imported-name>, or <overridden-version-constraint>, do not surround
 * it with quotes.
 *
 * When determining the import overridden constraints for a module, each entry
 * in the import override policy is looped through. If the importer module
 * name's and version match an entry, the specified overridden constraints
 * in the entry is used. If a module matches no entry in the import overridden
 * constraints, the original import constraints are assumed.
 *
 * @see java.module.ImportOverridePolicy
 * @since 1.7
 */
public final class ImportOverridePolicyFile implements ImportOverridePolicy {

    /**
     * This class represents an entry in the import override policy file.
     */
    private static class Entry {
        private final String name;
        private final VersionConstraint constraint;
        private final Map<String, VersionConstraint> overriddenConstraints;

        /**
         * Constructs a <code>Entry</code> object.
         *
         * @param name importing module's name to match.
         * @param constraint version constraint for matching importer's version.
         * @pararm overriddenConstraints overridden version constraints for the imports.
         */
        Entry(String name, VersionConstraint constraint, Map<String, VersionConstraint> overriddenConstraints) {
            this.name = name;
            this.constraint = constraint;
            this.overriddenConstraints = overriddenConstraints;
        }

        /**
         * Checks if the importing module's name and version is contained within this entry.
         *
         * @param moduleName importing module's name.
         * @param version importing module's version.
         * @return true if the importing module's name and version is contained
         *         within this entry; otherwise, false is returned.
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
         * @return the overridden version constraints for the imports in the
         *         importing module.
         */
        Map<String, VersionConstraint> getOverriddenVersionConstraint() {
            return overriddenConstraints;
        }
    }

    /* Are we debugging? -- for developers */
    private static final Debug mdebug = Debug.getInstance("properties");

    private final List<Entry> entries;

    ImportOverridePolicyFile() {
        this.entries = new ArrayList<Entry>();
    }

    /**
     * Add new import override policy info.
     *
     * @param name importing module's name to match.
     * @param constraint version constraint for matching importing module's version.
     * @pararm overriddenConstraints overridden version constraints for the imports.
     */
    private void add(String name, VersionConstraint constraint, Map<String, VersionConstraint> overriddenConstraints) {
        entries.add(new Entry(name, constraint, overriddenConstraints));
    }

    /**
     * Returns a map of import dependencies and overridden version constraints
     * for the module definition. The returned map contains the same set of
     * import dependencies as in the given map.
     *
     * @param importer the importing module definition.
     * @param constraints an unmodifiable map of import dependencies and
     *        overridden version constraints.
     * @return the map of import dependencies and overridden version
     *         constraints. It contains the same set of import dependencies as
     *         in the given map.
     */
    public Map<ImportDependency,VersionConstraint> narrow(ModuleDefinition importer, Map<ImportDependency,VersionConstraint> originalConstraints) {
        String name = importer.getName();
        Version version = importer.getVersion();

        // Check if the importer's name and version is contained within
        // one of the entries in the import override policy.
        for (Entry entry : entries) {

            // Skip to next entry if the current entry does not match
            if (entry.contains(name, version) == false)
                continue;

            Map<ImportDependency,VersionConstraint> result = null;
            Map<String,VersionConstraint> newConstraintsInPolicyFile = entry.getOverriddenVersionConstraint();

            for (Map.Entry<ImportDependency,VersionConstraint> originalEntry : originalConstraints.entrySet()) {
                ImportDependency dep = originalEntry.getKey();
                String key;

                if (dep instanceof ModuleDependency) {
                    key = "module:" + dep.getName();
                } else if (dep instanceof PackageDependency) {
                    key = "package:" + dep.getName();
                } else {
                    // import dependency is not recognized
                    throw new IllegalArgumentException("The type of import dependency is not supported: " + dep);
                }

                // Original version constraint
                VersionConstraint originalvcs = originalEntry.getValue();

                // Overridden version constraint
                VersionConstraint overriddenvcs = newConstraintsInPolicyFile.get(key);

                // The overridden version constraint must be within the known range of the
                // original version constraint.
                if (overriddenvcs != null) {

                    if (originalvcs.contains(overriddenvcs) == false)
                        throw new IllegalArgumentException("The overridden version constraint " + overriddenvcs
                                + " is outside the known range of the original version constraint " + originalvcs);

                    if (result == null) {
                        result = new HashMap<ImportDependency,VersionConstraint>(originalConstraints);
                    }

                    result.put(dep, overriddenvcs);
                }
            }

            // Returns original constraints if nothing has been changed.
            if (result != null)
                return result;
            else
                return originalConstraints;
        }
        return originalConstraints;
    }

    /**
     * Parses the import override policy file from the url and returns a
     * <code>ImportOverridePolicy</code> object that represents the
     * information in the import override policy file.
     *
     * @param url URL of import override policy file
     * @return <code>ImportOverridePolicy</code> object that represents the
     *         information in the visibility policy file.
     * @throws IOException if any I/O error occurs.
     */
    public static ImportOverridePolicy parse(URL url) throws IOException {

        BufferedReader br = null;
        try {
            // read in import override policy using UTF-8 by default
            br = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));

            Parser parser = new Parser();
            return parser.parse(br);

        } catch (IOException ioe) {
            if (mdebug != null) {
                mdebug.println("unable to load import override policy file from "
                    + url);
            }
            throw ioe;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * This class represents the parser for the import override policy file.
     */
    private static class Parser {

        private StreamTokenizer st;
        private int lookahead;
        private final static String STRING_TOKEN = "string";
        private ImportOverridePolicyFile policy;

        Parser() {
        }

        ImportOverridePolicy parse(Reader reader) throws IOException {

            policy = new ImportOverridePolicyFile();

            /**
             * Configure the stream tokenizer:
             *   Recognize strings between "..."
             *   Don't convert words to lowercase
             *   Recognize both C-style and C++-style comments
             *   Treat end-of-line as white space, not as a token
             */
            st   = new StreamTokenizer(reader);

            st.resetSyntax();
            st.wordChars('a', 'z');
            st.wordChars('A', 'Z');
            st.wordChars('0', '9');
            st.wordChars('[', '[');
            st.wordChars(']', ']');
            st.wordChars('(', '(');
            st.wordChars(')', ')');
            st.wordChars('+', '+');
            st.wordChars('-', '-');
            st.wordChars('*', '*');
            st.wordChars('.', '.');
            st.wordChars('_', '_');
            st.wordChars(',', ',');
            st.wordChars(128 + 32, 255);
            st.whitespaceChars(0, ' ');
            st.commentChar('/');
            st.quoteChar('\'');
            st.quoteChar('"');
            st.lowerCaseMode(false);
            st.ordinaryChar('/');
            st.slashSlashComments(true);
            st.slashStarComments(true);

            /**
             * The main parsing loop.  The loop is executed once
             * for each entry in the import override policy file.
             * The entries are delimited by semicolons.   Once we've read
             * in the information for an entry, go ahead and try to
             * add it to the policy entries.
             */
            lookahead = st.nextToken();
            while (lookahead != StreamTokenizer.TT_EOF) {
                parseImporterEntry();
                match(";");
            }

            return policy;
        }

        private boolean peek(String expect) {
            boolean found = false;
            switch (lookahead) {
                case StreamTokenizer.TT_NUMBER:
                    if (expect.equalsIgnoreCase(STRING_TOKEN))
                        found = true;
                    break;
                case StreamTokenizer.TT_WORD:
                    if (expect.equalsIgnoreCase(st.sval)
                        || expect.equalsIgnoreCase(STRING_TOKEN))
                        found = true;
                    break;
                case ';':
                    if (expect.equalsIgnoreCase(";"))
                        found = true;
                    break;
                case '{':
                    if (expect.equalsIgnoreCase("{"))
                        found = true;
                    break;
                case '}':
                    if (expect.equalsIgnoreCase("}"))
                        found = true;
                    break;
                case '@':
                    if (expect.equalsIgnoreCase("@"))
                        found = true;
                    break;
                default:

            }
            return found;
        }

        private String match(String expect)
            throws ParsingException, IOException {
            String value = null;

            switch (lookahead) {
                case StreamTokenizer.TT_NUMBER:
                    if (expect.equalsIgnoreCase(STRING_TOKEN)) {
                       value = String.valueOf(st.nval);
                       lookahead = st.nextToken();
                    }
                    else {
                       throw new ParsingException(st.lineno(), expect,
                        "number " + String.valueOf(st.nval));
                    }
                    break;
                case StreamTokenizer.TT_EOF:
                    throw new ParsingException("expected " + expect + ", read EOF");
                case StreamTokenizer.TT_WORD:
                    if (expect.equalsIgnoreCase(st.sval)) {
                        lookahead = st.nextToken();
                    } else if (expect.equalsIgnoreCase(STRING_TOKEN)) {
                        value = st.sval;
                        lookahead = st.nextToken();
                    } else {
                        throw new ParsingException(st.lineno(), expect, st.sval);
                    }
                    break;
                case '{':
                    if (expect.equalsIgnoreCase("{"))
                        lookahead = st.nextToken();
                    else
                        throw new ParsingException(st.lineno(), expect, "{");
                    break;
                case '}':
                    if (expect.equalsIgnoreCase("}"))
                        lookahead = st.nextToken();
                    else
                        throw new ParsingException(st.lineno(), expect, "}");
                    break;
                case ';':
                    if (expect.equalsIgnoreCase(";"))
                        lookahead = st.nextToken();
                    else
                        throw new ParsingException(st.lineno(), expect, ";");
                    break;
                case '@':
                    if (expect.equalsIgnoreCase("@"))
                        lookahead = st.nextToken();
                    else
                        throw new ParsingException(st.lineno(), expect, "@");
                    break;
                default:
                    throw new ParsingException(st.lineno(), expect,
                        new String(new char[] {(char)lookahead}));
            }
            return value;
        }

        /**
         * Parses an importer entry. Each entry is in the following format:
         *
         *   module <importer-name> [@ <importer-version-constraint>] {
         *        import <type> <imported-name> @ <overridden-version-constraint>;
         *        ...
         *   };
         */
        private void parseImporterEntry() throws IOException {

            // "module"
            match("module");

            // <importer-name>
            String name = match(STRING_TOKEN);
            VersionConstraint versionConstraint = VersionConstraint.DEFAULT;

            if (mdebug != null)
                mdebug.println("importer-module-name: " + name);

            // Count number of wildcard to determine if it is used correctly in
            // the importer module's name
            int wildcard = 0;
            for (int i=0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c == '*')
                    wildcard++;
            }
            if (wildcard > 1 ||
                (wildcard == 1 && !(name.equals("*") || (name.length() > 2 && name.endsWith(".*")))))
                throw new ParsingException(st.lineno(), "either \"*\", or \".*\" suffix", name);

            // Optional <version-constraint>
            if (peek("@")) {
                match("@");

                // <version-constraint>
                StringBuffer vcsBuffer = new StringBuffer();
                while (peek("{") == false) {
                    if (peek(STRING_TOKEN))
                        vcsBuffer.append(match(STRING_TOKEN));
                    else {
                        throw new ParsingException(st.lineno(), "invalid <importer-version-constraint>");
                    }
                }

                if (vcsBuffer.length() == 0)
                    throw new ParsingException(st.lineno(), "missing <importer-version-constraint>");

                versionConstraint = VersionConstraint.valueOf(vcsBuffer.toString());
            }

            if (mdebug != null)
                mdebug.println("version-constraint: " + versionConstraint);

            match("{");

            // Parse sub-entries
            Map<String, VersionConstraint> overriddenConstraints = new HashMap<String, VersionConstraint>();
            while(peek("}") == false) {

                // "import"
                match("import");

                // <type>
                String importedType = match(STRING_TOKEN);

                // <imported-name>
                String importedName = match(STRING_TOKEN);

                // No wildcard in <imported-module-name>
                if (importedName.indexOf("*") != -1)
                    throw new ParsingException(st.lineno(), "no wildcard in <imported-name>", importedName);

                match("@");

                // <overridden-version-constraint>
                StringBuffer vcsBuffer = new StringBuffer();
                while (peek(";") == false) {
                    if (peek(STRING_TOKEN))
                        vcsBuffer.append(match(STRING_TOKEN));
                    else
                        throw new ParsingException(st.lineno(), "invalid <overridden-version-constraint>");
                }

                if (vcsBuffer.length() == 0)
                    throw new ParsingException(st.lineno(), "missing <overridden-version-constraint>");

                VersionConstraint overriddenVersionConstraint = VersionConstraint.valueOf(vcsBuffer.toString());

                String key = importedType + ":" + importedName;
                overriddenConstraints.put(key, overriddenVersionConstraint);

                if (mdebug != null)
                    mdebug.println("   type=" + importedType + ", imported-name=" + importedName
                                   + ", overridden-version-constraint=" + overriddenVersionConstraint);

                match(";");
            }

            match("}");

            policy.add(name, versionConstraint, overriddenConstraints);
        }

        /**
         * This class represents a parsing exception in the import override policy
         * file.
         */
        private static class ParsingException extends IOException {

            private static final long serialVersionUID = -5112080612923544877L;

            /**
             * Constructs a ParsingException with the specified
             * detail message.
             *
             * @param msg the detail message.
             */
            public ParsingException(String msg) {
                super(msg);
            }

            public ParsingException(int line, String msg) {
                super("line " + line + ": " + msg);
            }

            public ParsingException(int line, String expect, String actual) {
                super("line " + line + ": expected [" + expect +
                    "], found [" + actual + "]");
            }
        }
    }
}
