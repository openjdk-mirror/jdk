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

import java.io.*;
import java.module.*;
import java.module.annotation.*;
import java.security.CodeSigner;
import java.util.*;
import java.util.jar.*;
import sun.module.*;

/**
 * /bin     -- native libraries
 * /lib     -- embedded JARs
 * /.module -- .module file
 * /.jam    -- original Jam file
 *
 * @since 1.7
 */
class LocalJamDefinitionContent extends ModuleDefinitionContent {
    /** JAM file corresponding to this content. */
    private final JarFile jamFile;

    /** LocalRepository containing the corresponding JAM. */
    private final Repository repository;

    /** Classpaths searched for classes & resources. */
    private final List<JarFile> classPaths = new ArrayList<JarFile>();

    /** Entry names of classes & resources. */
    private Set<String> entryNames = null;

    /** True if the JAM's embedded JAR files & native libs are expanded. */
    private boolean jarsAndLibsAreInstalled;

    /** True if the JAM's embedded JAR files & native libs have failed to be expanded. */
    private boolean jarsAndLibsInstallFailed = false;

    /**
     * Default path in JAM file where legacy JAR files reside.  Value is
     * changed if module uses @JarLibraryPath (see {@link #setJarLibraryPath}).
     */
    private String jarLibraryPath = RepositoryUtils.getJarLibraryPathDefault();

    /** Directory into which legacy JAR files are placed. */
    private File legacyJarDir;

    /**
     * Default path in JAM file where native library files reside.  Value is
     * changed if module uses @NativeLibraryPath (see {@link #setNativeLibraryPath}).
     */
    private static String nativeLibraryPath = RepositoryUtils.getNativeLibraryPathDefault();

    /** Directory into which native library files are placed. */
    private File nativeLibraryDir;

    /** Name used to identify this class in exceptions. */
    private static final String IDENT = "LocalJamDefinitionContent";

    LocalJamDefinitionContent(
            JarFile jamFile,
            Repository repository) throws IOException{
        this.jamFile = jamFile;
        this.repository = repository;
        classPaths.add(jamFile);
    }

    /**
     * Sets the path at which embedded JAR files are found in this content's
     * corresponding JAM file.
     */
    void setJarLibraryPath(String path) {
        jarLibraryPath = path;
    }

    /**
     * Sets the directory into which embedded JAR files will be extracted.
     */
    void setLegacyJarDir(File dir) {
        legacyJarDir = dir;
    }

    /**
     * Sets the path at which native library files are found in this content's
     * corresponding JAM file if the given {@code platform} and {@code arch}
     * match the current platform and arch.
     */
    void setNativeLibraryPath(String platform, String arch, String path) {
        if (RepositoryUtils.getPlatform().equals(platform)
                && RepositoryUtils.getArch().equals(arch)) {
            nativeLibraryPath = path;
        }
    }

    /**
     * Sets the directory into which native lib files will be extracted.
     */
    void setNativeLibraryDir(File dir) {
        nativeLibraryDir = dir;
    }

    /**
     * If not already done, extract legacy JAR and native library files.
     */
    private boolean getJarsAndLibs() {
        if (jarsAndLibsAreInstalled) {
            return true;
        }
        if (jarsAndLibsInstallFailed) {
            return false;
        }
        try {
            // Extract legacy/embedded JAR files and native libraries
            File[] jarFiles = RepositoryUtils.extractJarsAndLibs(
                jamFile, jarLibraryPath, legacyJarDir,
                nativeLibraryPath, nativeLibraryDir);
            for (File f : jarFiles) {
                classPaths.add(new JarFile(f, false));
            }

            jarsAndLibsAreInstalled = true;
            return true;
        } catch (IOException ex) {
            jarsAndLibsInstallFailed = true;
            // XXX Use logging
            System.err.println(IDENT + " for repository at " + repository.getSourceLocation()
                               + ": Failed while extracting native library or legacy JAR file: " + ex);

            return false;
        }
    }

    @Override
    public File getNativeLibrary(String libraryName) {
        if (getJarsAndLibs() == false) {
            return null;
        }
        File nativeLibrary = new File(nativeLibraryDir, libraryName);
        if (nativeLibrary.exists() && nativeLibrary.isFile()) {
            return nativeLibrary;
        } else {
            return null;
        }
    }

    @Override
    public boolean hasEntry(String name) {
        return getEntryNames().contains(name);
    }

    @Override
    public Set<String> getEntryNames() {
        if (getJarsAndLibs() == false) {
            return Collections.unmodifiableSet(new HashSet<String>());
        }
        if (entryNames == null) {
            Set<String> names = new HashSet<String>();
            boolean enclosingJam = true;
            for (JarFile jf : classPaths) {
                if (enclosingJam) {
                    // If it is the jam file, adds all entries
                    for (JarEntry je : Collections.list(jf.entries())) {
                        if (je.isDirectory())  {
                            continue;
                        }
                        names.add(je.getName());
                    }
                    enclosingJam = false;
                } else {
                    // If it is an embedded jar file, adds all entries except
                    // the ones under META-INF/ and MODULE-INF
                    for (JarEntry je : Collections.list(jf.entries())) {
                        if (je.isDirectory())  {
                            continue;
                        }
                        String s = je.getName();
                        if (s.startsWith("META-INF/") == false
                            && s.startsWith("MODULE-INF/") == false) {
                            names.add(s);
                        }
                    }
                }
            }
            entryNames = Collections.unmodifiableSet(names);
        }
        return entryNames;
    }

    @Override
    public InputStream getEntryAsStream(String name) throws IOException {
        if (getJarsAndLibs() == false)  {
            return null;
        }
        for (JarFile jf : classPaths)   {
            JarEntry je = jf.getJarEntry(name);
            if (je != null) {
                return jf.getInputStream(je);
            }
        }
        return null;
    }

    @Override
    public CodeSigner[] getCodeSigners() {
        // TODO Replace w/ correct implementation.
        return null;
    }

    @Override
    public boolean isDownloaded() {
        return true;
    }
}
