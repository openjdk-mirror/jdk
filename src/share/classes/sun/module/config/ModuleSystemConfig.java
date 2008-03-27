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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecurityPermission;
import java.util.Properties;
import sun.module.Debug;
import sun.security.util.PropertyExpander;

/**
 * <p>This class centralizes all module system properties and common module
 * system configuration methods.
 *
 * @since 1.7
 */

public final class ModuleSystemConfig {

    // Constants for property names.
    public static final String VISIBILITY_POLICY_CLASSNAME = "visibility.policy.classname";
    public static final String VISIBILITY_POLICY_URL_PREFIX = "visibility.policy.url.";
    public static final String VISIBILITY_POLICY_ALLOW_SYSTEMPROPERTY = "visibility.policy.allowSystemProperty";

    public static final String IMPORT_OVERRIDE_POLICY_CLASSNAME = "import.override.policy.classname";
    public static final String IMPORT_OVERRIDE_POLICY_URL_PREFIX = "import.override.policy.url.";
    public static final String IMPORT_OVERRIDE_POLICY_ALLOW_SYSTEMPROPERTY = "import.override.policy.allowSystemProperty";

    public static final String MODULE_SYSTEM_OVERRIDE_PROPERTIESFILE = "module.system.overridePropertiesFile";

    public static final String REPOSITORY_PROPERTIES_FILE = "java.module.repository.properties.file";
    public static final String REPOSITORY_PROPERTIES_ALLOW_SYSTEMPROPERTY = "repository.properties.allowSystemProperty";

    /* Are we debugging? -- for developers */
    private static final Debug mdebug = Debug.getInstance("properties");

    /* module.system.properties */
    private static Properties props;

    static {
        // doPrivileged here because there are multiple
        // things in initialize that might require privs.
        // (the FileInputStream call and the File.exists call,
        // the moduleSystemPropFile call, etc)
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                initialize();
                return null;
            }
        });
    }

    /**
     * Don't let anyone instantiate this.
     */
    private ModuleSystemConfig() {
    }

    private static void initialize() {
        props = new Properties();
        boolean loadedProps = false;
        boolean overrideAll = false;

        // first load the module system properties file
        // to determine the value of module.system.overridePropertiesFile
        File propFile = new File(System.getProperty("java.home") + File.separator
                        + "lib" + File.separator + "module" + File.separator
                        + "module.properties");

        if (propFile.exists()) {
            InputStream is = null;
            try {
                FileInputStream fis = new FileInputStream(propFile);
                is = new BufferedInputStream(fis);
                props.load(is);
                loadedProps = true;

                if (mdebug != null) {
                    mdebug.println("reading module system properties file: " +
                                propFile);
                }
            } catch (IOException e) {
                if (mdebug != null) {
                    mdebug.println("unable to load module system properties from " +
                                propFile);
                    e.printStackTrace();
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ioe) {
                        if (mdebug != null) {
                            mdebug.println("unable to close input stream");
                        }
                    }
                }
            }
        }

        if ("true".equalsIgnoreCase(props.getProperty(MODULE_SYSTEM_OVERRIDE_PROPERTIESFILE))) {

            String extraPropFile = System.getProperty
                                        ("java.module.system.properties");
            if (extraPropFile != null && extraPropFile.startsWith("=")) {
                overrideAll = true;
                extraPropFile = extraPropFile.substring(1);
            }

            if (overrideAll) {
                props = new Properties();
                if (mdebug != null) {
                    mdebug.println
                        ("overriding other module system properties files!");
                }
            }

            // now load the user-specified file so its values
            // will win if they conflict with the earlier values
            if (extraPropFile != null) {
                BufferedInputStream bis = null;
                try {
                    URL propURL;

                    extraPropFile = PropertyExpander.expand(extraPropFile);
                    propFile = new File(extraPropFile);
                    if (propFile.exists()) {
                        propURL = new URL
                                ("file:" + propFile.getCanonicalPath());
                    } else {
                        propURL = new URL(extraPropFile);
                    }
                    bis = new BufferedInputStream(propURL.openStream());
                    props.load(bis);
                    loadedProps = true;

                    if (mdebug != null) {
                        mdebug.println("reading module system properties file: " +
                                        propURL);
                        if (overrideAll) {
                            mdebug.println
                                ("overriding other module system properties files!");
                        }
                    }
                } catch (Exception e) {
                    if (mdebug != null) {
                        mdebug.println
                                ("unable to load module system properties from " +
                                extraPropFile);
                        e.printStackTrace();
                    }
                } finally {
                    if (bis != null) {
                        try {
                            bis.close();
                        } catch (IOException ioe) {
                            if (mdebug != null) {
                                mdebug.println("unable to close input stream");
                            }
                        }
                    }
                }
            }
        }

        if (!loadedProps) {
            initializeStatic();
            if (mdebug != null) {
                mdebug.println("unable to load module system properties " +
                        "-- using defaults");
            }
        }

    }

    /*
     * Initialize to default values, if <java.home>/lib/module/java.module.system
     * is not found.
     */
    private static void initializeStatic() {
        props.put(VISIBILITY_POLICY_CLASSNAME, sun.module.config.DefaultVisibilityPolicy.class.getName());
        props.put(VISIBILITY_POLICY_URL_PREFIX + "1", "file:${java.home}/lib/module/visibility.policy");
        props.put(VISIBILITY_POLICY_URL_PREFIX + "2", "file:${user.home}/.visibility.policy");
        props.put(VISIBILITY_POLICY_ALLOW_SYSTEMPROPERTY, "true");
        props.put(IMPORT_OVERRIDE_POLICY_CLASSNAME, sun.module.config.DefaultImportOverridePolicy.class.getName());
        props.put(IMPORT_OVERRIDE_POLICY_URL_PREFIX + "1", "file:${java.home}/lib/module/import.override.policy");
        props.put(IMPORT_OVERRIDE_POLICY_URL_PREFIX + "2", "file:${user.home}/.import.override.policy");
        props.put(IMPORT_OVERRIDE_POLICY_ALLOW_SYSTEMPROPERTY, "true");
        props.put(MODULE_SYSTEM_OVERRIDE_PROPERTIESFILE, "true");
        props.put(REPOSITORY_PROPERTIES_FILE, "file:///${java.home}/lib/module/repository.properties");
        props.put(REPOSITORY_PROPERTIES_ALLOW_SYSTEMPROPERTY, "true");
    }

    /**
     * Gets a module system property value.
     *
     * @param key the key of the property being retrieved.
     * @return the value of the module system property corresponding to key.
     * @throws  NullPointerException is key is null
     */
    static String getProperty(String key) {
        String value = props.getProperty(key);
        if (value != null)
            value = value.trim();       // could be a class name with trailing ws
        return value;
    }

    /**
     * Sets a module system property value.
     *
     * @param key the name of the property to be set.
     * @param datum the value of the property to be set.
     * @throws  NullPointerException if key or datum is null
     */
    public static void setProperty(String key, String datum) {
        props.put(key, datum);
    }

    /**
     * Returns the default class name for the visibility policy.
     */
    public static String getVisibilityPolicyDefaultClassName() {
        String value = getProperty(VISIBILITY_POLICY_CLASSNAME);
        return ((value == null) ? sun.module.config.DefaultVisibilityPolicy.class.getName() : value);
    }

    /**
     * Returns the default class name for the import override policy.
     */
    public static String getImportOverridePolicyDefaultClassName() {
        String value = getProperty(IMPORT_OVERRIDE_POLICY_CLASSNAME);
        return ((value == null) ? sun.module.config.DefaultImportOverridePolicy.class.getName() : value);
    }

    /**
     * Returns the name of the file for loading repository properties.
     */
    public static String getRepositoryPropertiesFileName() {
        String rc = rc = getProperty(REPOSITORY_PROPERTIES_FILE);
        boolean allowOverride = Boolean.parseBoolean(
            getProperty(REPOSITORY_PROPERTIES_ALLOW_SYSTEMPROPERTY));
        if (allowOverride) {
            String s = System.getProperty(REPOSITORY_PROPERTIES_FILE);
            if (s != null) {
                rc = s;
            }
        }
        return rc;
    }
}
