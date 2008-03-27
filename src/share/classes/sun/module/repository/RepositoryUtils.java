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
import java.util.Collections;
import java.util.List;
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

    static File getLegacyJarDir(File dir, String name, Version version, String platform, String arch)    {
        return new File(getModuleDir(dir, name, version, platform, arch), "lib");
    }
    static File getNativeLibraryDir(File dir, String name, Version version, String platform, String arch)    {
        return new File(getModuleDir(dir, name, version, platform, arch), "bin");
    }

    /**
     * Creates a {@code ModuleDefinition} for a {@code LocalRepository}
     *
     * @param repository LocalRepository in which the module exists
     * @param file a JAM file
     * @param expansionDir directory into which JAR files & native libraries
     * file are expanded.
     * @return a {@code ModuleDefinition} corresponding to MODULE.METADATA
     *     from the JAM
     * @throws IOException if {@code downloadDir} isn't a directory, or if
     *     there's a problem downloading the module data, etc.
     */
    static ModuleDefinition createLocalModuleDefinition(
            LocalRepository repository,
            File file,
            File expansionDir) throws IOException {

        if ((repository == null)
            || (file  == null)
            || (expansionDir == null)) {
            throw new NullPointerException();
        }

        JarFile jamFile = new JarFile(file, false);

        LocalJamDefinitionContent content = new LocalJamDefinitionContent(
            jamFile, repository);

        // Extracts module metadata from JAM file.
        byte[] metadata = JamUtils.getMetadata(jamFile);

        // Constructs a superpackage from module metadata, for extracting the
        // stock annotations. Note that it is very important to go through
        // superpackage instead of ModuleDefinition to extract annotations
        // at this point, as the latter might trigger loading of the JAM file
        // in some rare cases.
        java.lang.reflect.Superpackage superPackage = sun.module.JamUtils.getSuperpackage(metadata);

        // Module name
        String moduleName = superPackage.getName();
        if (moduleName.startsWith("java.")) {
            throw new IOException("Non-bootstrap repository could not create " + moduleName + " module definition.");
        }

        // Module version
        java.module.annotation.Version aversion = superPackage.getAnnotation
            (java.module.annotation.Version.class);
        Version moduleVersion = Version.DEFAULT;
        if (aversion != null) {
            try {
                moduleVersion = Version.valueOf(aversion.value());
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        // Platform Binding
        java.module.annotation.PlatformBinding platformBinding = superPackage.getAnnotation
            (java.module.annotation.PlatformBinding.class);
        String platform = null;
        String arch = null;
        if (platformBinding != null) {
            platform = platformBinding.platform();
            arch = platformBinding.arch();
        }

        // JarLibraryPath
        JarLibraryPath jlp = superPackage.getAnnotation(JarLibraryPath.class);
        if (jlp != null) {
            content.setJarLibraryPath(jlp.value());
        }
        content.setLegacyJarDir(
            getLegacyJarDir(expansionDir, moduleName, moduleVersion, platform, arch));

        NativeLibraryPaths nlps = superPackage.getAnnotation(NativeLibraryPaths.class);
        if (nlps != null) {
            for (NativeLibraryPath nlp : nlps.value()) {
                content.setNativeLibraryPath(nlp.platform(), nlp.arch(), nlp.path());
            }
        }
        content.setNativeLibraryDir(
            getNativeLibraryDir(expansionDir, moduleName, moduleVersion, platform, arch));

        // TODO: moduleReleasable might be false in some cases
        return Modules.newJamModuleDefinition(metadata, content, repository, true);
    }

    /**
     * Creates a {@code ModuleDefinition} for a {@code URLRepository}
     *
     * @param repository URLRepository in which the module exists
     * @param downloadDir Directory into which JAM files are downloaded.  This
     * is the parent of each JAM-specific {@code baseDir}.
     * @param moduleInfo {@code ModuleInfo} describing this module
     * @param metadataFile MODULE.METADATA file for the {@code ModuleDefinition} to
     *     be returned
     * @return a {@code ModuleDefinition} corresponding to MODULE.METADATA
     *     from the JAM
     * @throws IOException if {@code downloadDir} isn't a directory, or if
     *     there's a problem downloading the module data, etc.
     */
    static ModuleDefinition createURLModuleDefinition(
            URLRepository repository,
            File downloadDir,
            ModuleInfo moduleInfo,
            File metadataFile) throws IOException {

        if ((repository == null)
            || (downloadDir == null)
            || (moduleInfo == null)
            || (metadataFile == null)) {
            throw new NullPointerException();
        }

        if (!downloadDir.isDirectory()) {
            throw new IOException(downloadDir.getName() + " is not a directory.");
        }
        File baseDir = getModuleDir(
            downloadDir, moduleInfo.getName(), moduleInfo.getVersion(),
            moduleInfo.getPlatform(), moduleInfo.getArch());
        if (!baseDir.isDirectory() && !baseDir.mkdirs()) {
            throw new IOException("Cannot create directory for module "
                + moduleInfo.getName() + " v" + moduleInfo.getVersion() + " at "
                + baseDir.getAbsolutePath());
        }

        URLModuleDefinitionContent content = new URLModuleDefinitionContent(
            baseDir, repository, moduleInfo, metadataFile);

        // Extracts module metadata from MODULE.METADATA file.
        byte[] metadata = JamUtils.readFile(metadataFile);

        // Constructs a superpackage from module metadata, for extracting the
        // stock annotations. Note that it is very important to go through
        // superpackage instead of ModuleDefinition to extract annotations
        // at this point, as the latter might trigger loading of the JAM file
        // in some rare cases.
        java.lang.reflect.Superpackage superPackage = sun.module.JamUtils.getSuperpackage(metadata);

        // Module name
        String moduleName = superPackage.getName();
        if (moduleName.startsWith("java.")) {
            throw new IOException("Non-bootstrap repository could not create " + moduleName + " module definition.");
        }

        // Module version
        java.module.annotation.Version aversion = superPackage.getAnnotation
            (java.module.annotation.Version.class);
        Version moduleVersion = Version.DEFAULT;
        if (aversion != null) {
            try {
                moduleVersion = Version.valueOf(aversion.value());
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        // Platform Binding
        java.module.annotation.PlatformBinding platformBinding = superPackage.getAnnotation
            (java.module.annotation.PlatformBinding.class);
        String platform = null;
        String arch = null;
        if (platformBinding != null) {
            platform = platformBinding.platform();
            arch = platformBinding.arch();
        }

        // JarLibraryPath
        JarLibraryPath jlp = superPackage.getAnnotation(JarLibraryPath.class);
        if (jlp != null) {
            content.setJarLibraryPath(jlp.value());
        }

        NativeLibraryPaths nlps = superPackage.getAnnotation(NativeLibraryPaths.class);
        if (nlps != null) {
            for (NativeLibraryPath nlp : nlps.value()) {
                content.setNativeLibraryPath(nlp.platform(), nlp.arch(), nlp.path());
            }
        }

        // TODO: moduleReleasable might be false in some cases
        return Modules.newJamModuleDefinition(metadata, content, repository, true);
    }

    /**
     * Finds module definitions in a given cache.
     *
     * @param constraint Query to select module definitions
     * @param cache Cache in which to search
     * @return module definitions matching {@code constraint} in {@code cache}
     */
    static List<ModuleDefinition> findModuleDefinitions(
            Query constraint, List<ModuleDefinition> cache) {
        List<ModuleDefinition> rc = new ArrayList<ModuleDefinition>();

        if (constraint == Query.ANY) {
            rc = cache;
        } else {
            for (ModuleDefinition md : cache) {
                if (constraint.match(md)) {
                    rc.add(md);
                }
            }
        }
        return Collections.unmodifiableList(rc);
    }

    /**
     * Indicates whether or not the {@code ModuleDefinition} is appropriate
     * for the given {@code platform} and {@arch}.
     *
     * @return true if the given module definition's platform binding is not
     * given or if it matches the {@code platform} and  {@code platform} given
     * @param platformBinding Platform binding to check
     * @param platform name of current platform
     * @param arch name of current arch
     * @return true if the md's platform & arch match those given
     */
    static boolean bindingMatches(
            PlatformBinding platformBinding, String platform, String arch)  {
        if (platformBinding == null) {
            return true;
        }

        return (platform.equals(platformBinding.platform()) && arch.equals(platformBinding.arch()));
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
    static File[] extractJarsAndLibs(final JarFile jamFile,
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
                    JamUtils.saveStreamAsFile(jamFile.getInputStream(je), f);
                    JarFile jf = new JarFile(f);
                    boolean ok = (jf.getEntry(JamUtils.MODULE_INF_METADATA) == null);
                    JamUtils.close(jf);
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

    /**
     * Returns the directory where module-specific files are kept based on
     * information in a {@code ModuleArchiveInfo}.
     *
     * @param base directory where information for a repository is kept
     * @param mai {@code ModuleArchiveInfo} for a module
     * @return the location under {@code base} where legacy JAR files are kept
     * for a module with the given {@code ModuleArchiveInfo}
     */
    private static File getModuleDir(File base, ModuleArchiveInfo mai) {
        return getModuleDir(
            base,
            mai.getName(), mai.getVersion(),
            mai.getPlatform(), mai.getArchitecture());
    }

    /**
     * Returns the directory where module-specific files are kept based on the
     * information in a {@code ModuleDefinition}.
     *
     * @param base directory where information for a repository is kept
     * @param md {@code ModuleDefinition} for a module
     * @return the location under {@code base} where legacy JAR files are kept
     * for a module with the given {@code ModuleDefinition}
     */
    private static File getModuleDir(File base, ModuleDefinition md) {
        String platform = null;
        String arch = null;
        PlatformBinding pb = md.getAnnotation(PlatformBinding.class);
        if (pb != null) {
            platform = pb.platform();
            arch = pb.arch();
        }
        return getModuleDir(
            base,
            md.getName(), md.getVersion(),
            platform, arch);
    }

    /**
     * Returns the directory where module-specific files are kept.
     *
     * @param base directory where information for a repository is kept
     * @param name module name
     * @param version module version
     * @param platform module platform (may be null)
     * @param arch module arch (may be null if {@code platform} is null)
     * @return the location under {@code base} where repository-specific files
     * are kept for a module with the given {@code name} and {@code version}.
     */
    private static File getModuleDir(
            File base, String name, Version version,
            String platform, String arch) {
        return new File(
            base,
            name + "-" + version
            + ((platform == null) ? "" : "-" + platform + "-" + arch));
    }

    /**
     * @see #getModuleDir(File, ModuleArchiveInfo)
     * @see #getModuleDir(File, ModuleDefinition)
     */
    private static File getModuleDir(
            File base, String name, Version version,
            String platform, String arch, String dest) {
        return new File(
            getModuleDir(base, name, version, platform, arch), dest);
    }
}
