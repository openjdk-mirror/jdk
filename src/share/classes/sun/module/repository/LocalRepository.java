/*
 * Copyright 2006-2008 Sun Microsystems, Inc.  All Rights Reserved.
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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.module.ModuleArchiveInfo;
import java.module.ModuleDefinition;
import java.module.ModuleSystem;
import java.module.Modules;
import java.module.Query;
import java.module.Repository;
import java.module.Version;
import java.nio.ByteBuffer;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sun.module.JamUtils;
import sun.module.repository.cache.Cache;
import sun.module.repository.cache.ModuleDefInfo;

/**
 * This class represents the Local repository in the JAM module system.
 * See the {@link java.module.Modules} class for more details.
 *
 * @see java.module.ModuleArchiveInfo
 * @see java.module.ModuleDefinition
 * @see java.module.Query
 * @see java.module.Repository
 * @since 1.7
 */
public final class LocalRepository extends AbstractRepository {
    /** Prefix of all properties use to configure this repository. */
    private static final String PROPERTY_PREFIX = "sun.module.repository.LocalRepository.";

    /**
     * Directory in which JAM files are installed, derived from the source
     * location given in the constructors.
     */
    private File sourceDirectory;

    /**
     * True if the source location must exist during execution of {@code
     * initialize(Map<String, String>)} and  {@link #reload()}.
     */
    private boolean sourceLocMustExist;

    /** The value from the sourceLocationMustExist system property. */
    private static String sysPropSourceLocMustExist;

    /** Property name for getting sysPropSourceLocMustExist. */
    private static final String SOURCE_LOC_MUST_EXIST_KEY =
        PROPERTY_PREFIX + "sourceLocationMustExist";

    /**
     * True if all modules must be removed from the source location
     * during execution of {@code shutdown()}.
     */
    private boolean uninstallOnShutdown;

    private static final String UNINSTALL_ON_SHUTDOWN_KEY =
        PROPERTY_PREFIX + "uninstallOnShutdown";

    /**
     * Directory containing directories for JAM expansion and creation of any
     * other artifacts for this repository.
     */
    private File cacheDirectory;

    /**
     * Directory name corresponding to {@code CACHE_DIR_KEY} as obtained
     * from System properties.
     */
    private static final String sysPropCacheDirName;

    /**
     * Property name to configure the directory into which native libraries
     * and embedded JAR files are expanded from a JAM file.
     */
    private static final String CACHE_DIR_KEY =
        PROPERTY_PREFIX + "cacheDirectory";

    static {
        sysPropSourceLocMustExist = RepositoryUtils.getProperty(SOURCE_LOC_MUST_EXIST_KEY);
        sysPropCacheDirName = RepositoryUtils.getProperty(CACHE_DIR_KEY);
    }

    /**
     * Creates a new <code>LocalRepository</code> instance, and initializes it
     * using information from the given {@code config}.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's <code>checkPermission</code> method with a
     * <code>ModuleSystemPermission("createRepository")</code> permission to
     * ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param source the source location.
     * @param parent the parent repository for delegation.
     * @param config Map of configuration names to their values
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to create a new
     *         instance of repository.
     * @throws java.io.IOException if the repository cannot be initialized.
     */
    public LocalRepository(String name, URI source,
                           Map<String, String> config,
                           Repository parent) throws IOException {
        super(name, source, (config == null ? DEFAULT_CONFIG : config), parent);

        initialize();
    }

    //
    // Extend AbstractRepository
    //

    /**
     * Initializes the repository instance using the supplied configuration.
     * This repository can be configured with these keys:
     * <p>
     * <ul>
     * <li>{@code sun.module.repository.LocalRepository.sourceLocationMustExist}:
     * If true, then {@code initialize()} (and {@link #reload()} will throw
     * {@code IOException} if the source location specified during
     * construction does not exist.  If false, it will complete normally, but
     * the repository's {@code list()} and {@code find()} operations will
     * return empty lists.  If  the source location is created by some other
     * means and {@link #reload()} is invoked on the repository, modules at the
     * location will then be available via {@code list()} and {@code find()}.
     * </li>
     * <li>{@code sun.module.repository.LocalRepository.expansionDir}:
     * Specifies the directory into which JAM files are to be expanded.
     * </li>
     * </ul>
     * <em>Note:</em> Values are read from System properties only once, when
     * this class is loaded.
     * @param config Map of configuration names to their values
     * @throws IOException if the repository cannot be initialized
     */
    @Override
    protected List<ModuleArchiveInfo> doInitialize2() throws IOException {
        if (config == null) {
            throw new NullPointerException("config must not be null");
        }
        if ("true".equalsIgnoreCase(sysPropSourceLocMustExist)) {
            sourceLocMustExist = true;
        } else {
            sourceLocMustExist = "true".equalsIgnoreCase(config.get(SOURCE_LOC_MUST_EXIST_KEY));
        }

        uninstallOnShutdown = "true".equalsIgnoreCase(config.get(UNINSTALL_ON_SHUTDOWN_KEY));

        String cacheDirName = sysPropCacheDirName != null
            ? sysPropCacheDirName :      config.get(CACHE_DIR_KEY);
        if (cacheDirName != null) {
            cacheDirectory = new File(cacheDirName);
        }

        sourceDirectory = JamUtils.getFile(getSourceLocation().toURL());
        if (sourceDirectory.isDirectory() == false) {
            if (sourceLocMustExist) {
                missingDir("source", sourceDirectory);
            }
        }

        // Constructs a repository cache instance
        if (cacheDirectory != null) {
            repositoryCache = Cache.newInstance(cacheDirectory);
        } else {
            repositoryCache = Cache.newInstance();
        }

        assertValidDirs();

        List<ModuleArchiveInfo> result = new ArrayList<ModuleArchiveInfo>();

        File[] jamFiles = sourceDirectory.listFiles(JamUtils.JAM_JAR_FILTER);
        if (jamFiles == null) {
            return result;
        }

        // Iterates the JAM files in the source directory, and cook them
        // one-by-one.
        for (File file : jamFiles) {
            try {
                // Put the jam file into the repository cache and cook it
                ModuleDefInfo mdInfo = repositoryCache.getModuleDefInfo(getSourceLocation().toURL(), file);

                // Constructs a module archive info
                JamModuleArchiveInfo mai  = new JamModuleArchiveInfo(
                    this, mdInfo.getName(), mdInfo.getVersion(),
                    mdInfo.getPlatform(), mdInfo.getArch(),
                    file.getAbsolutePath(), file.lastModified(),
                    mdInfo.getMetadataByteBuffer(),
                    mdInfo.getModuleContent());

                result.add(mai);
            } catch (Exception ex) {
                // XXX log warning but otherwise ignore
                System.err.println("Failed to load module from " + file + ": " + ex);
            }
        }

        // The source directory may contain more than one module with the
        // same name and same version, e.g. duplicate module with different
        // JAM filename, or portable module vs platform specific module.

        // Constructs the module definitions from the module archives
        // based on the current platform and architecture.
        addModuleDefinitions(constructModuleDefinitions(result));

        return result;
    }

    @Override
    protected ModuleArchiveInfo doInstall(URL url) throws IOException {
        InputStream is = null;
        File tmpFile = null;
        File jamFile = null;

        try {
            URLConnection uc = url.openConnection();
            is = uc.getInputStream();

            // Store the file in a temp directory first. Unpack the file if
            // necessary.
            tmpFile = File.createTempFile("local-repository-install-", "tmp");
            tmpFile.deleteOnExit();

            if (url.getFile().endsWith(".jam.pack.gz")) {
                JamUtils.unpackStreamAsFile(is, tmpFile);
            } else {
                JamUtils.saveStreamAsFile(is, tmpFile);
            }

            // Retrieve the module metadata from the JAM file to find out
            // the module information, e.g. name, version, etc.
            //
            // No need to shadow copy (if set) because this is a temp file.
            ModuleDefInfo mdInfo = repositoryCache.getModuleDefInfo(getSourceLocation().toURL(), tmpFile, false);

            // Check to see if there exists a module archive that has
            // the same name, version, and platform binding.
            for (ModuleArchiveInfo a : getModuleArchiveInfos()) {
                JamModuleArchiveInfo mai = (JamModuleArchiveInfo) a;

                if (mai.getName().equals(mdInfo.getName())
                    && mai.getVersion().equals(mdInfo.getVersion()))  {
                    if (mai.isPortable()) {
                        if (mdInfo.isPortable()) {
                            throw new IllegalStateException("A module definition with the same name,"
                                + " version, and platform binding is already installed");
                        }
                    } else if (mai.getPlatform().equals(mdInfo.getPlatform())
                               && mai.getArch().equals(mdInfo.getArch())) {
                        throw new IllegalStateException("A module definition with the same name,"
                            + " version, and platform binding is already installed");
                    }
                }
            }

            // Generate the destination jam file
            String jamName = JamUtils.getJamFilename(mdInfo.getName(),
                                mdInfo.getVersion(), mdInfo.getPlatform(),
                                mdInfo.getArch());
            jamFile = new File(sourceDirectory + File.separator + jamName + ".jam");

            // Checks to see if the jam file already exists in the source directory.
            //
            // Typically, this check is a bit redundent because we have already
            // checked above that there is no existing module archive that has
            // the same name, version, and platform binding as the
            // about-to-be-installed module archive. If such file exists, it could
            // happen only if that file was installed into source location somehow
            // but the local repository in this JVM session was not aware of it.
            if (jamFile.exists() && !jamFile.canWrite()) {
                throw new IOException(
                    "Cannot overwrite " + jamFile.getName()
                        + " in the source directory: " + sourceDirectory);
            }

            // Copy the jam file into the source directory.
            //
            // Note that we always copy the unpacked JAM file into the
            // source directory. This is done to optimize the startup
            // performance of the local repository in subsequence launch.
            JamUtils.copyFile(tmpFile, jamFile);

            // Put the JAM file into the repository cache, cook it, and
            // reflect the change.
            return addModuleArchiveInternal(jamFile);
        } catch(IOException ioe) {
            // Something went wrong, remove the jam file in the source
            // directory.
            if (jamFile != null) {
                jamFile.delete();
            }
            throw ioe;
        } finally {
            JamUtils.close(is);
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    @Override
    protected boolean doUninstall(ModuleArchiveInfo mai) throws IOException {
        // Remove the module archive if it is the same one that was installed,
        // as determined by timestamp.  Don't remove if the timestamp is
        // different, as that could mean a file has been copied over the
        // installed file.
        File f = new File(mai.getFileName());
        if (f.lastModified() != mai.getLastModified()) {
            throw new IOException(
                "Could not delete module archive because the modification "
                + "date was different than expected: " + mai.getFileName());
        }
        if (f.isFile() && !f.delete()) {
            throw new IOException(
                "Could not delete module archive: " + mai.getFileName());
        }

        // Remove the module archive and the corresponding module definition
        // (if it has been created) so this repository wont'recognize them
        // anymore.
        removeModuleArchiveInternal(mai);

        return true;
    }

    @Override
    protected void doReload() throws IOException {
        Set<ModuleArchiveInfo> uninstalledJams = new HashSet<ModuleArchiveInfo>();
        Set<File> existingJams = new HashSet<File>();

        for (ModuleArchiveInfo mai : getModuleArchiveInfos()) {
            File f = new File(mai.getFileName());
            long modTime = mai.getLastModified();

            // A module archive is considered "uninstalled" if source file is
            // missing, or if it has been updated on disk.
            if (!f.isFile() || (modTime != 0 && f.lastModified() != modTime)) {
                uninstalledJams.add(mai);
            } else {
                existingJams.add(f);
            }
        }

        // Removes the module archive and the corresponding module
        // definition from the repository.
        for (ModuleArchiveInfo mai : uninstalledJams) {
            removeModuleArchiveInternal(mai);
        }

        // Adds the new module archive from the source directory, but are
        // not in the cache.
        for (File file : sourceDirectory.listFiles(JamUtils.JAM_FILTER)) {
            if (!existingJams.contains(file))  {
                try {
                    // Put the JAM file into the repository cache, cook it,
                    // and reflect the change.
                    addModuleArchiveInternal(file);
                } catch(IOException ioe) {
                    // XXX: if reload() throws exception, there is no gurantee
                    // that the set of module archives and module definitions
                    // in the repository remains the same as before reload()
                    // is called.
                    throw ioe;
                }
            }
        }

        // A repository may have both portable module and platform specific
        // modules with the same name and version installed, and it has
        // constructed a module definition from one of the platform specific
        // module. If this platform specific module is uninstalled, the
        // repository will need to construct a new module definition from
        // the portable module.
        reconstructModuleDefinitionsIfNecessary();
    }

    @Override
    protected void doShutdown2() throws IOException {
        // XXX This is a hook for testing shutdownOnExit.
        if (uninstallOnShutdown) {
            // Only remove what was installed
            for (ModuleArchiveInfo mai : getModuleArchiveInfos()) {
                try {
                    uninstall(mai);
                } catch(Exception e) {
                    // ignore exception
                }
            }
        }
    }

    @Override
    public boolean isReadOnly() {
        return (sourceDirectory.canWrite() == false);
    }

    @Override
    public boolean supportsReload() {
        return true;
    }

    protected void assertValidDirs() throws IOException {
        if (sourceLocMustExist) {
            if (sourceDirectory == null || !sourceDirectory.isDirectory()) {
                missingDir("source", sourceDirectory);
            }
        }
        if (cacheDirectory != null
            && (cacheDirectory.exists() && !cacheDirectory.isDirectory())) {
            throw new IOException(
                cacheDirectory + " is not a directory");
        }
    }

    private void missingDir(String type, File dir) throws IOException {
        throw new FileNotFoundException(
            dir + " does not exist or is not a directory");
    }
}
