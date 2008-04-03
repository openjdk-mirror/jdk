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

package sun.module.repository;

import java.io.File;
import java.io.IOException;
import java.module.ModuleArchiveInfo;
import java.module.ModuleDefinition;
import java.module.Modules;
import java.module.Query;
import java.module.Version;
import java.module.annotation.JarLibraryPath;
import java.module.annotation.NativeLibraryPath;
import java.module.annotation.NativeLibraryPaths;
import java.module.annotation.PlatformBinding;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import sun.module.JamUtils;
import sun.security.action.GetPropertyAction;

/**
 * Utility methods for repository-related classes.
 */
public class RepositoryUtils {
    /** Current software platform. */
    private static String platform = null;

    /** Current processor architecture. */
    private static String arch = null;

    private RepositoryUtils() {
    }

    /**
     * Return a value for the key from system properties.
     *
     * @param key used for lookup in {@link #java.lang.System.getProperties()}
     */
    static String getProperty(String key) {
        return AccessController.doPrivileged(new GetPropertyAction(key));
    }

    /**
     * Returns the current software platform.
     *
     * @return the current software platform
     */
    public static String getPlatform() {
        if (platform == null) {
            platform = getProperty("os.platform");
        }
        return platform;
    }

    /**
     * Returns the current processor architecture.
     *
     * @return the current processor architecture
     */
    public static String getArch() {
        if (arch == null) {
            arch = getProperty("os.arch");
            if (arch.endsWith("86")) {
                arch = "x86";
            }
        }
        return arch;
    }

    /** Default path to JARs embedded in a JAM (so '/', not File.separator). */
    static String getJarLibraryPathDefault() {
        return JamUtils.MODULE_INF + "/lib";
    }

    /** Default path to native libs in a JAM (so '/', not File.separator). */
    static String getNativeLibraryPathDefault() {
        return JamUtils.MODULE_INF + "/bin/" + getPlatform() + "/" + getArch();
    }

    // Determines if the original file should be shadow copied into the cache
    // before opening it. This is to avoid locking the original file on certain
    // operating systems, e.g. Windows.
    public static boolean shouldShadowCopyFiles() {
        Boolean shadowCopyFiles = java.security.AccessController.doPrivileged(
              new sun.security.action.GetBooleanAction("java.module.repository.shadowcopyfiles"));
        return shadowCopyFiles == Boolean.TRUE;
    }

    /**
     * Provides a means of extracting entries from JAM files
     */
    static abstract class Extractor {
        protected boolean dirChecked = false;
        protected final File extractDir;

        Extractor(File extractDir) {
            this.extractDir = extractDir;
        }

        void mkdir() throws IOException {
            // Make directory only if not already done.
            if (!dirChecked) {
                if (!extractDir.isDirectory() && !extractDir.mkdirs()) {
                    throw new IOException(
                        "Cannot create extraction directory " + extractDir);
                }
                dirChecked = true;
            }
        }

        /**
         * Subclasses implement this to extract the given entry if appropriate.
         */
        abstract File extract(JarEntry je) throws IOException;
    }

    /**
     * Extracts both legacy JARs and native libraries from {@code jamFile}.
     * Legacy JAR files are placed into {@code baseDir/lib}, native libraries
     * are placed into {#code baseDir/bin}.  The two paths are used to locate
     * entries in the JAM file.
     *
     * @param jamFile JAM possibly containing legacy JARs and/or native
     * libraries
     * @param jarLibraryPath path in JAM file at which legacy JAR files reside
     * @param legacyJarDir directory into which legacy JAR files are placed
     * @param nativeLibraryPath path in JAM file at which legacy native
     * library files reside
     * @param nativeLibraryDir directory into which native libraries are placed
     * @return array of legacy JAR files found.
     * @throws IOException if there are errors accessing the JAM file or in
     * writing jar/lib files.
     */
    public static File[] extractJarsAndLibs(final JarFile jamFile,
            final String jarLibraryPath, final File legacyJarDir,
            final String nativeLibraryPath, final File nativeLibraryDir) throws IOException {

        // Legacy JAR files found in jamFile
        List<File> rc = new ArrayList<File>();

        Extractor libExtractor = new Extractor(nativeLibraryDir) {
            File extract(JarEntry je) throws IOException {
                File rc = null;
                String name = je.getName();
                if (name.startsWith(nativeLibraryPath) && !je.isDirectory()) {
                    mkdir();
                    // Save in nativeLibraryDir using only the last component of
                    // the JAM file's entry name.
                    rc = new File(nativeLibraryDir, new File(name).getName());
                    nativeLibraryDir.mkdirs();
                    JamUtils.saveStreamAsFile(jamFile.getInputStream(je), rc);
                }
                return rc;
            }
        };

        Extractor jarExtractor = new Extractor(legacyJarDir) {
            File extract(JarEntry je) throws IOException {
                String name = je.getName();
                File rc = null;
                if (name.startsWith(jarLibraryPath) && name.endsWith(".jar")) {
                    mkdir();
                    int start = name.lastIndexOf("/");
                    if (start > 0) {
                        name = name.substring(start + 1);
                    }
                    File f = new File(legacyJarDir, name);
                    legacyJarDir.mkdirs();
                    JamUtils.saveStreamAsFile(jamFile.getInputStream(je), f);
                    JarFile jf = null;
                    boolean ok;
                    try {
                        jf = new JarFile(f, false);
                        ok = (jf.getEntry(JamUtils.MODULE_INF_METADATA) == null);
                    } finally {
                        JamUtils.close(jf);
                    }
                    if (ok) {
                        rc = f;
                    } else {
                        // Per 5.24, a legacy JAR with
                        // MODULE-INF/MODULE.METADATA, is ignored.
                        f.delete();
                    }
                }
                return rc;
            }
        };

        // True if any native libraries are extracted.
        boolean haveNativeLibs = false;
        for (JarEntry je : Collections.list(jamFile.entries())) {
            File f = jarExtractor.extract(je);
            if (f != null) {
                rc.add(f);
            } else if (libExtractor.extract(je) != null) {
                haveNativeLibs = true;
            }
        }

        // Verify that if client-specified paths are used, some results were found
        if (!jarLibraryPath.equals(getJarLibraryPathDefault()) && rc.size() == 0) {
            throw new IOException("No legacy JAR files found at " + jarLibraryPath);
        }
        if (!nativeLibraryPath.equals(getNativeLibraryPathDefault()) && !haveNativeLibs) {
            throw new IOException("No native libraries found at " + nativeLibraryPath);
        }
        return rc.toArray(new File[0]);
    }
}
