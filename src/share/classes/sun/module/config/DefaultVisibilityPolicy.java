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
import java.module.VisibilityPolicy;
import java.module.Version;
import java.module.VersionConstraint;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.ArrayList;
import sun.net.www.ParseUtil;
import sun.module.Debug;
import sun.security.util.PropertyExpander;


/**
 * This class represents a default implementation for
 * <code>java.module.VisibilityPolicy</code>.
 *
 * <p> This object stores the policy for entire Java runtime,
 * and is the amalgamation of multiple visiblity policies
 * that resides in files.
 * The algorithm for locating the policy file(s) and reading their
 * information is:
 *
 * <ol>
 * <li>
 *   Loop through the <code>sun.module.config.ModuleSystemConfig</code> properties,
 *   <i>visibility.policy.url.1</i>,
 *   <i>visibility.policy.url.2</i>, ...,
 *   <i>visibility.policy.X</i>".  These properties are set
 *   in the module system properties file, which is located in the file named
 *   &lt;JAVA_HOME&gt;/lib/module/module.properties.
 *   &lt;JAVA_HOME&gt; refers to the value of the java.home system property,
 *   and specifies the directory where the JRE is installed.
 *   Each property value specifies a <code>URL</code> pointing to a
 *   policy file to be loaded.  Read in and load each policy.
 *
 * <li>
 *   The <code>java.lang.System</code> property <i>java.module.visibility.policy.file</i>
 *   may also be set to a <code>URL</code> pointing to another policy file
 *   (which is the case when a user uses the -D switch at runtime).
 *   If this property is defined, and its use is allowed by the
 *   module system property file (the Module System property,
 *   <i>visibility.policy.allowSystemProperty</i> is set to <i>true</i>),
 *   also load that policy.
 *
 *   If the  <i>java.module.visibility.policy</i> property is defined using
 *   "==" (rather than "="), then ignore all other specified
 *   policies and only load this policy.
 * </ol>
 *
 * @see java.module.VisibilityPolicy
 * @since 1.7
 */
public class DefaultVisibilityPolicy extends DefaultPolicy<java.module.VisibilityPolicy>
                                     implements java.module.VisibilityPolicy {

    private List<VisibilityPolicy> policies = new ArrayList<VisibilityPolicy>();

    /**
     * Initializes the DefaultVisibilityPolicy object and reads the default
     * visibility policies.
     */
    public DefaultVisibilityPolicy() {
        refresh();
    }

    /**
     * Refreshes the DefaultVisibilityPolicy object and loads the visibility
     * policy configuration file(s).
     *
     * The algorithm for locating the standard visibility policy file(s) is
     * as follows:
     * <pre>
     *   loop through the Module System Properties named
     *   "visibility.policy.url.1", "visibility.policy.url.2", etc, until
     *   no more is found. Each of these specify a visibility policy file.
     *
     *   if the system property "java.module.visibility.policy.file" is defined
     *     (which is the case when the user uses the -D switch at runtime), and
     *     its use is allowed by the module system property file, also load it.
     * </pre>
     *
     * @return true if the visibility policy configuration file(s) are loaded
     *         properly; returns false otherwise.
     */
    public boolean refresh() {
        final ArrayList<VisibilityPolicy> newPolicies = new ArrayList<VisibilityPolicy>();

        // Loads visibility policy configuration files.
        //
        boolean loadedPolicy = loadPolicy("java.module.visibility.policy.file",
                                          ModuleSystemConfig.VISIBILITY_POLICY_ALLOW_SYSTEMPROPERTY,
                                          ModuleSystemConfig.VISIBILITY_POLICY_URL_PREFIX,
                                          newPolicies);

        // Replace existing policies with the new ones only if the refresh has
        // been successful, thus the state should never be inconsistent.
        if (loadedPolicy)
            policies = newPolicies;

        return loadedPolicy;
    }

    /**
     * Loads the visibility policy configuration file and adds the information
     * to the list of policies.
     *
     * @param policyURL URL of the visibility policy configuration file.
     * @param policies list of policies
     * @return true if the information in the import override policy
     *         configuration file has been added successfully; otherwise,
     *         returns false.
     */
    protected boolean addNewPolicy(final URL url, List<VisibilityPolicy> policies) {
        try {
            policies.add(VisibilityPolicyFile.parse(url));
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
     * Returns true if the module definition should be visible in the
     * repository of the module system. Otherwise, returns false.
     * <p>
     *
     * @param moduleDef the module definition.
     * @return true if the module definition should be visible in the
     *         module system; false otherwise.
     */
    public boolean isVisible(ModuleDefinition moduleDef) {
        for (VisibilityPolicy policy : policies)   {
            if (policy.isVisible(moduleDef) == false)
                return false;
        }

        // Returns true only if the module definition is considered visible in
        // *all* visibility policies.
        return true;
    }
}
