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

package sun.module.osgi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.main.AutoActivator;
import org.apache.felix.main.Main;

/**
 * OSGi runtime provides the API to start the container and
 * access the OSGi representations wrapping the container implementation
 * specific API.
 *
 * XXX: Use reflection API to support different OSGi container implementation
 *
 */
class OSGiRuntime implements BundleActivator {

    private static final OSGiRuntime runtime =
        new OSGiRuntime();

    private static URI bundleSourceLocation;
    private static Felix felix;
    private static boolean debug = System.getProperty("java.module.debug") != null;

    private BundleContext context = null;
    private PackageAdmin packageAdmin = null;

    // Default system property to configure the directory for OSGi bundle caching
    private static final String CACHE_DIR_PROP = "java.module.osgi.repository.cache";

    private OSGiRuntime() {
    }

    public void start(BundleContext context) {
        this.context = context;
        this.packageAdmin = (PackageAdmin)
            context.getService(
                context.getServiceReference(PackageAdmin.class.getName()));
    }

    public void stop(BundleContext context) {
        // the framework is being stopped
        //
        // XXX: OSGi module system will stop working.
        context = null;
    }

    BundleContext getContext() {
        return context;
    }

    PackageAdmin getPackageAdmin() {
        return packageAdmin;
    }

    /**
     * Start the OSGi container.
     */
    static void start(URI source, String osgiContainer) throws IOException {
        if (debug) {
           System.out.println("Starting the OSGi runtime at source = " +
               source + " container = " + osgiContainer);
        }

        bundleSourceLocation = source;

        File container = null;
        if (osgiContainer != null) {
            container = new File(URI.create(osgiContainer));
        } else {
            // Determine whether the OSGi container is in the class path
            String classpath = System.getProperty("java.class.path");
            int index = classpath.toLowerCase().indexOf("felix.jar");
            int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
            if (index >= start)
            {
                // Get the path of the felix.jar file.
                String jarLocation = classpath.substring(start, index + 9);
                container = new File(jarLocation);
            }
        }
        if (container == null || !container.exists()) {
            // No OSGi container
            throw new IOException("Felix container doesn't exist");
        }

        // Read configuration properties.
        Properties configProps = initConfigProperties(container);

        try
        {
            // Create a list for custom framework activators and
            // add an instance of the auto-activator it for processing
            // auto-install and auto-start properties.
            List<BundleActivator> list = new ArrayList<BundleActivator>();
            list.add(new AutoActivator(configProps));
            list.add(runtime);

            // Create a case-insensitive property map.
            Map configMap = new StringMap(configProps, false);
            // Create an instance of the framework.
            felix = new Felix(configMap, list);
            felix.start();
        } catch (BundleException e) {
            System.err.println("Failed to create the Felix framework");
            throw new IOException(e);
        }
    }

    // Initialize the configuration properties for the OSGi container
    private static Properties initConfigProperties(File container) throws IOException {
        // (1) Load system properties.
        Main.loadSystemProperties();

        // Workaround: Felix implementation assumes that felix.jar is on
        // the classpath ("java.class.path" system property) for locating
        // the default configuration properties file.
        // So set the "felix.config.properties" property to workaround it
        if (System.getProperty("felix.config.properties") == null) {
            File confDir = new File(container.getParentFile().getParentFile(), "conf");
            File f = new File(confDir, "config.properties");
            if (f.exists()) {
                System.setProperty("felix.config.properties", f.toURI().toString());
            }
        }

        // (2) Read configuration properties.
        Properties configProps = Main.loadConfigProperties();

        // (3) Copy framework properties from the system properties.
        Main.copySystemProperties(configProps);

        // (4) See if the profile name and directory property was specified.
        String profileName = configProps.getProperty(BundleCache.CACHE_PROFILE_PROP);
        String profileDirName = configProps.getProperty(BundleCache.CACHE_PROFILE_DIR_PROP);

        if ((profileName == null || profileName.length() == 0) &&
            (profileDirName == null || profileDirName.length() == 0)) {
            String cacheDir = System.getProperty(CACHE_DIR_PROP);
            if (cacheDir != null) {
                configProps.setProperty(BundleCache.CACHE_PROFILE_DIR_PROP, cacheDir);
            } else {
                throw new IOException("Fail to start the OSGi container: " +
                    "Profile name or directory property is not specified");
            }
        }

        // Configure the Felix instance to be embedded.
        configProps.put(FelixConstants.EMBEDDED_EXECUTION_PROP, "true");

        // Auto start the bundles listed in the source location
        StringBuilder sb = new StringBuilder();
        File p = new File(bundleSourceLocation);
        for (File file : p.listFiles()) {
            sb.append(file.toURI());
            sb.append(" ");
        }
        if (sb.length() > 0) {
            configProps.put(AutoActivator.AUTO_INSTALL_PROP + ".1", sb.toString());
        }

        if (debug) {
            configProps.put("felix.log.level", "4");
            StringBuilder autostart = new StringBuilder();
            File dir = new File(container.getParentFile().getParentFile(), "bundle");
            for (File file : dir.listFiles()) {
                System.out.println(file);
                autostart.append(file.toURI());
                autostart.append(" ");
            }
            if (autostart.length() > 0) {
                configProps.put(AutoActivator.AUTO_START_PROP + ".1", autostart.toString());
            }
        }

        return configProps;
    }

    static Bundle installBundle(URI uri) throws IOException {
        try {
            return runtime.getContext().installBundle(uri.toString());
        } catch (BundleException e) {
            throw new IOException(e);
        }
    }

    static void uninstallBundle(Bundle bundle) throws IOException {
        try {
            bundle.uninstall();
        } catch (BundleException e) {
            throw new IOException(e);
        }
    }

    static Set<Bundle> getInstalledBundles() {
        Set<Bundle> installedBundles = new HashSet<Bundle>();
        Bundle[] bundles = runtime.getContext().getBundles();

        for (Bundle bundle : bundles) {
            if (bundle.getBundleId() == 0) {
                // Ignore system bundle.
                continue;
            }
            if (bundle.getState() == Bundle.UNINSTALLED) {
                // Ignore bundle that is uninstalled.
                continue;
            }
            installedBundles.add(bundle);
        }

        return installedBundles;
    }

    static Bundle getExportingBundle(Bundle bundle, String packageName) {
        ExportedPackage[] epkgs = runtime.getPackageAdmin().getExportedPackages(packageName);
        if (epkgs != null) {
            for (ExportedPackage ep : epkgs) {
                Bundle[] importingBundles = ep.getImportingBundles();
                if (importingBundles == null) {
                    return null;
                }
                for (Bundle b : importingBundles) {
                    if (b.getBundleId() == bundle.getBundleId()) {
                        return ep.getExportingBundle();
                    }
                }
            }
        }
        return null;
    }

    static Bundle getRequiredBundle(Bundle bundle, String bundleSymbolicName) {
        /*
         * TODO: Felix has not implemented getRequiredBundles() which just returns null
         *
        RequiredBundle[] reqBundles = runtime.getPackageAdmin().getRequiredBundles(bundleSymbolicName);
        if (reqBundles != null) {
            for (RequiredBundle rb : reqBundles) {
                Bundle[] requiringBundles = rb.getRequiringBundles();
                if (requiringBundles == null) {
                    return null;
                }
                for (Bundle b : requiringBundles) {
                    if (b.getBundleId() == bundle.getBundleId()) {
                        return rb.getBundle();
                    }
                }
            }
        }
        */
        Bundle[] bundles = runtime.getPackageAdmin().getBundles(bundleSymbolicName, null);
        if (bundles != null && bundles.length >= 1) {
            if (bundles.length == 1) {
                return bundles[0];
            }
            throw new AssertionError("Required-Bundle not supported yet");
        }
        return null;
    }

    static Set<ExportedPackage> getExportedPackages(Bundle bundle) {
        Set<ExportedPackage> exports = new HashSet<ExportedPackage>();
        ExportedPackage[] epkgs = runtime.getPackageAdmin().getExportedPackages(bundle);
        if (epkgs != null) {
            for (ExportedPackage ep : epkgs) {
                String packageName = ep.getName();
                if (packageName.equals(".") ||
                        packageName.startsWith("META-INF.") ||
                        packageName.startsWith("license")) {
                    continue;
                }
                exports.add(ep);
            }
        }
        return exports;
    }
}
