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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import sun.module.Debug;
import sun.net.www.ParseUtil;
import sun.security.util.PropertyExpander;


/**
 * This class represents the base class for configuration policies.
 *
 * @since 1.7
 */
abstract class DefaultPolicy<T> {

    protected static final Debug mdebug = Debug.getInstance("properties");

    /**
     * Loads the policy configuration file(s).
     *
     * The algorithm for locating the standard policy configuration file(s) is
     * as follows:
     * <pre>
     *   loop through the module system properties named
     *   "<configFilePropertyPrefix>1", "<configFilePropertyPrefix>2", etc, until
     *   no more is found. Each of these specify a policy file.
     *
     *   if the system property "<policyFileProperty>" is defined (which is the
     *     case when the user uses the -D switch at runtime), and its use is
     *     allowed by the module system property file, also load it.
     * </pre>
     *
     * @param policyFileProperty property that represents the location of an
     *        extra policy file
     * @param allowExtraPolicyProperty property indicates if an extra policy
     *        file is allowed.
     * @param configFilePropertyPrefix policy file URL prefix in module
     *        system properties
     * @param policies list of policies
     * @return true if the policy configuration file(s) are loaded properly;
     *         returns false otherwise.
     */
    final boolean loadPolicy(final String policyFileProperty,
                             final String allowExtraPolicyProperty,
                             final String configFilePropertyPrefix,
                             final List<T> policies) {

        Boolean loadedPolicy =
            AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                boolean loaded_policy = false;

                String value = ModuleSystemConfig.getProperty(allowExtraPolicyProperty);
                boolean extra_policy_allowed = false;
                if (value != null)
                    extra_policy_allowed = "true".equalsIgnoreCase(value);
                else
                    extra_policy_allowed = false;

                if (extra_policy_allowed) {
                    String extra_policy = System.getProperty(policyFileProperty);
                    if (extra_policy != null) {
                        boolean overrideAll = false;
                        if (extra_policy.startsWith("=")) {
                            overrideAll = true;
                            extra_policy = extra_policy.substring(1);
                        }
                        try {
                            extra_policy =
                                PropertyExpander.expand(extra_policy);
                            URL policyURL;

                            // If the extra_policy is an actual file (e.g. "/home/userXYZ/...",
                            // then it will be converted into a URL. Otherwise, extra_policy is
                            // directly treated as a URL.
                            File policyFile = new File(extra_policy);
                            if (policyFile.exists()) {
                                policyURL = ParseUtil.fileToEncodedURL
                                    (new File(policyFile.getCanonicalPath()));
                            } else {
                                policyURL = new URL(extra_policy);
                            }
                            if (mdebug != null)
                                mdebug.println("reading "+policyURL);
                            if (addNewPolicy(policyURL, policies))
                                loaded_policy = true;
                        } catch (Exception e) {
                            // ignore.
                            if (mdebug != null) {
                                mdebug.println("caught exception: "+e);
                            }
                        }
                        if (overrideAll) {
                            if (mdebug != null) {
                                mdebug.println("overriding other policies!");
                            }
                            return Boolean.valueOf(loaded_policy);
                        }
                    }
                }

                int n = 1;
                String policy_uri;

                while ((policy_uri = ModuleSystemConfig.getProperty(configFilePropertyPrefix+n)) != null) {
                    try {
                        URL policy_url = null;
                        String expanded_uri = PropertyExpander.expand
                                (policy_uri).replace(File.separatorChar, '/');

                        if (policy_uri.startsWith("file:${java.home}/") ||
                            policy_uri.startsWith("file:${user.home}/")) {

                            // this special case accommodates
                            // the situation java.home/user.home
                            // expand to a single slash, resulting in
                            // a file://foo URI
                            policy_url = new File
                                (expanded_uri.substring(5)).toURI().toURL();
                        } else {
                            policy_url = new URI(expanded_uri).toURL();
                        }

                        if (mdebug != null)
                            mdebug.println("reading "+policy_url);
                        if (addNewPolicy(policy_url, policies))
                            loaded_policy = true;
                    } catch (Exception e) {
                        if (mdebug != null) {
                            mdebug.println("error reading policy "+e);
                            e.printStackTrace();
                        }
                        // ignore that policy
                    }
                    n++;
                }
                return Boolean.valueOf(loaded_policy);
            }
        });

        return loadedPolicy.booleanValue();
    }

    /**
     * Loads the policy configuration file and adds the information to
     * the policies.
     *
     * @param policyURL URL of the policy configuration file.
     * @param policies list of policies
     * @return true if the information in the policy configuration file has
     *         been added successfully; otherwise, returns false.
     */
    protected abstract boolean addNewPolicy(URL policyURL, List<T> policies);
}
