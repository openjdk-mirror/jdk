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

import java.io.File;
import java.io.IOException;
import java.module.ModuleDefinition;
import java.module.ImportDependency;
import java.module.ImportOverridePolicy;
import java.module.Version;
import java.module.VersionConstraint;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sun.net.www.ParseUtil;
import sun.module.Debug;
import sun.security.util.PropertyExpander;


/**
 * This class represents a default implementation for
 * <code>java.module.ImportOverridePolicy</code>.
 *
 * <p> This object stores the policy for entire Java runtime,
 * and is the amalgamation of multiple import override policies
 * that resides in files.
 *
 * The algorithm for locating the policy file(s) and reading their
 * information is:
 *
 * <ol>
 * <li>
 *   Loop through the <code>sun.module.config.ModuleSystemConfig</code> properties,
 *   <i>import.override.policy.url.1</i>,
 *   <i>import.override.policy.url.2</i>, ...,
 *   <i>import.override.policy.X</i>".  These properties are set
 *   in the module system properties file, which is located in the file named
 *   &lt;JAVA_HOME&gt;/lib/module/module.properties.
 *   &lt;JAVA_HOME&gt; refers to the value of the java.home system property,
 *   and specifies the directory where the JRE is installed.
 *   Each property value specifies a <code>URL</code> pointing to a
 *   policy file to be loaded.  Read in and load each policy.
 *
 * <li>
 *   The <code>java.lang.System</code> property <i>java.module.import.override.policy</i>
 *   may also be set to a <code>URL</code> pointing to another policy file
 *   (which is the case when a user uses the -D switch at runtime).
 *   If this property is defined, and its use is allowed by the
 *   module system property file (the Module System property,
 *   <i>import.override.policy.allowSystemProperty</i> is set to <i>true</i>),
 *   also load that policy.
 *
 *   If the  <i>java.module.import.override.policy</i> property is defined
 *   using "==" (rather than "="), then ignore all other specified
 *   policies and only load this policy.
 * </ol>
 *
 * @see java.module.ImportOverridePolicy
 * @since 1.7
 */
public class DefaultImportOverridePolicy extends DefaultPolicy<java.module.ImportOverridePolicy>
                                         implements java.module.ImportOverridePolicy {

    private List<ImportOverridePolicy> policies = new ArrayList<ImportOverridePolicy>();

    /**
     * Initializes the DefaultImportOverridePolicy object and reads the default
     * import override policies.
     */
    public DefaultImportOverridePolicy() {
        refresh();
    }

    /**
     * Refreshes the DefaultImportOverridePolicy object and loads the import
     * override policy configuration file(s).
     *
     * The algorithm for locating the standard import override policy file(s) is
     * as follows:
     * <pre>
     *   loop through the Module System Properties named
     *   "import.override.policy.url.1", "import.override.policy.url.2", etc, until
     *   no more is found. Each of these specify an import override policy file.
     *
     *   if the system property "java.module.import.override.policy.file" is defined
     *     (which is the case when the user uses the -D switch at runtime), and
     *     its use is allowed by the module system property file, also load it.
     * </pre>
     *
     * @return true if the import override policy configuration file(s) are
     *         loaded properly; returns false otherwise.
     */
    public boolean refresh() {
        final ArrayList<ImportOverridePolicy> newPolicies = new ArrayList<ImportOverridePolicy>();

        // Loads import override policy configuration files.
        //
        boolean loadedPolicy = loadPolicy("java.module.import.override.policy.file",
                                          ModuleSystemConfig.IMPORT_OVERRIDE_POLICY_ALLOW_SYSTEMPROPERTY,
                                          ModuleSystemConfig.IMPORT_OVERRIDE_POLICY_URL_PREFIX,
                                          newPolicies);

        // Replace existing policies with the new ones only if the refresh has
        // been successful, thus the state should never be inconsistent.
        if (loadedPolicy)
            policies = newPolicies;

        return loadedPolicy;
    }

    /**
     * Loads the import override policy configuration file and adds the
     * information to the list of policies.
     *
     * @param policyURL URL of the import override policy configuration file.
     * @param policies list of policies
     * @return true if the information in the import override policy
     *         configuration file has been added successfully; otherwise,
     *         returns false.
     */
    protected boolean addNewPolicy(final URL url, List<ImportOverridePolicy> policies) {
        try {
            policies.add(ImportOverridePolicyFile.parse(url));
            return true;
        }
        catch (IOException e) {
            if (mdebug != null) {
                mdebug.println("caught exception: " + e);
            }
        }

        return false;
    }

    /**
     * Returns a map of import dependencies and overridden version constraints
     * for the module definition. The returned map contains the same set of
     * import dependencies as the given map.
     *
     * @param importer the importing module definition.
     * @param originalConstraints an unmodifiable map of import dependencies
     *        and overridden version constraints.
     * @return the map of import dependencies and overridden version
     *         constraints. It contains the same set of import dependencies as
     *         in the given map.
     */
    public Map<ImportDependency,VersionConstraint> narrow(ModuleDefinition importer,
                                                Map<ImportDependency,VersionConstraint> originalConstraints) {

        Set<Map<ImportDependency,VersionConstraint> > newConstraintsSet = new HashSet<Map<ImportDependency,VersionConstraint> >();

        for (ImportOverridePolicy policy : policies)   {
            Map<ImportDependency,VersionConstraint> newConstraints = policy.narrow(importer, originalConstraints);
            if (newConstraints != originalConstraints) {
                // Added constraints only if they are different than the original ones.
                newConstraintsSet.add(newConstraints);
            }
        }

        // If there is no new overridden constraints, return the original
        // constraints.
        if (newConstraintsSet.size() == 0)
            return originalConstraints;

        // Otherwise, determine the intersected constraint from all policies
        Map<ImportDependency,VersionConstraint> result = null;

        for (Map.Entry<ImportDependency,VersionConstraint> entry: originalConstraints.entrySet()) {

            // Determines intersecting version constraint between
            // import override policies
            ImportDependency dep = entry.getKey();
            VersionConstraint originalvcs = entry.getValue();
            VersionConstraint vcs = originalvcs;

            for (Map<ImportDependency,VersionConstraint> constraints : newConstraintsSet) {

                vcs = vcs.intersection(constraints.get(dep));

                if (vcs == null)
                    throw new IllegalArgumentException("The overridden version constraint "
                        + " is outside the known range of the original version constraint "
                        + originalConstraints.get(dep));
            }

            if (originalvcs.equals(vcs) == false) {
                if (result == null) {
                    result = new HashMap<ImportDependency,VersionConstraint>(originalConstraints);
                }
                result.put(dep, vcs);
            }
        }

        // Returns original constraints if nothing has been changed.
        if (result != null)
            return result;
        else
            return originalConstraints;
    }
}
