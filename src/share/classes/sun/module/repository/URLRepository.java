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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.module.ModuleArchiveInfo;
import java.module.ModuleDefinition;
import java.module.ModuleDefinitionContent;
import java.module.ModuleSystem;
import java.module.ModuleSystemPermission;
import java.module.Modules;
import java.module.Query;
import java.module.Repository;
import java.module.RepositoryEvent;
import java.module.Version;
import java.module.annotation.PlatformBinding;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import sun.module.JamUtils;

/**
 * This class represents a repository that loads module definitions
 * from a codebase URL.
 * <p>
 * Information about the module definitions available from the
 * codebase URL must be published in a repository metadata file. The
 * contents of the file must follow the schema of the URL Repository
 * metadata for the Java Module System.
 * <p><i>
 *      {codebase}/repository-metadata.xml
 * <p></i>
 * When the repository is initialized, the repository metadata file
 * (i.e. repository-metadata.xml) would be downloaded from the
 * codebase URL.
 * <p>
 * In the repository metadata file, each module definition is described
 * with a name, a version, a platform binding, and a path (relative to
 * the codebase URL where the module file, the module archive, and/or
 * the packed module archive are located). If no path and no platform
 * binding is specified, the default path is "{name}/{version}". If the
 * path is not specified and the module definition has platform
 * binding, the default path is "{name}/{version}/{platform}-{arch}".
 * <p>
 * After the URL repository instance successfully downloads the
 * repository metadata file, the module file of each module definition
 * (i.e. MODULE.METADATA file) in the repository is downloaded based on
 * the information in the repository metadata file:
 * <p><i>
 *      {codebase}/{path}/MODULE.METADATA
 * <p></i>
 * If a module definition is platform-specific, its module file is
 * downloaded if and only if the platform binding described in the
 * repository metadata file matches the platform and the architecture
 * of the system.
 * <p>
 * Module definitions are available for searches after the URL
 * repository instance is initialized. If a module instance is
 * instantiated from a module definition that has no platform binding,
 * the module archive is downloaded by probing in the following order:
 * <p><i>
 *      {codebase}/{path}/{name}-{version}.jam.pack.gz<p>
 *      {codebase}/{path}/{name}-{version}.jam
 * <p></i>
 * On the other hand, if a module instance is instantiated from a
 * platform-specific module definition, the module archive is
 * downloaded by probing in the following order:
 * <p><i>
 *      {codebase}/{path}/{name}-{version}-{platform}-{arch}.jam.pack.gz<p>
 *      {codebase}/{path}/{name}-{version}-{platform}-{arch}.jam
 * <p></i>
 * To ensure the integrity of the separately-hosted module file is in
 * sync with that in the module archive of the same module definition,
 * they are compared bit-wise against each other after the module
 * archive is downloaded.
 *
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystemPermission
 * @see java.module.Query
 * @since 1.7
 */
public final class URLRepository extends Repository {
    /** Describes the contents of this repository. */
    private RepositoryContents contents = new RepositoryContents();

    /**
     * A cache of ModuleDefinitions that have been installed and which also
     * match the current platform binding.  Module definitions are added as
     * they are installed and removed as they are uninstalled.  (Contrast with
     * {@code contents}, which describes all installed modules regardless of
     * platform binding.)
     */
    private final List<ModuleDefinition> modDefCache =
        new ArrayList<ModuleDefinition>();

    /** Marks the time this repository was created. */
    private final long timestamp;

    /** Prefix of all properties use to configure this repository. */
    private static final String PROPERTY_PREFIX = "sun.module.repository.URLRepository.";

    /**
     * Directory into which MODULE.METADATA file and JAM files are downloaded
     * and expanded.
     */
    private File downloadDir;

    /**
     * Name of metadata directory based on system property and config in
     * {@link #initialize(Map<String,String>)}.
     */
    private String downloadDirName;

    /**
     * Directory name corresponding to {@code DOWNLOAD_DIR_KEY} as obtained
     * from system properties.
     */
    private static final String sysPropDownloadDirName;

    /**
     * Property name to configure the directory to which JAM files are
     * downloaded.
     */
    private static final String DOWNLOAD_DIR_KEY =
        PROPERTY_PREFIX + "downloadDirectory";

    /**
     * True if the source location must exist during execution of {@code
     * initialize()} and {@code reload()}.
     */
    private boolean sourceLocMustExist;

    /** The value from the sourceLocationMustExist system property. */
    private static String sysPropSourceLocMustExist;

    private static final String SOURCELOC_MUST_EXIST_KEY =
        PROPERTY_PREFIX + "sourceLocationMustExist";

    static {
        sysPropDownloadDirName = RepositoryUtils.getProperty(DOWNLOAD_DIR_KEY);
        sysPropSourceLocMustExist = RepositoryUtils.getProperty(SOURCELOC_MUST_EXIST_KEY);
    }

    /** String form of codebase given in constructor but with a trailing '/'. */
    private final String sourcePath;

    /** True if this repository has been initialized and not yet shutdown;
     * false otherwise. */
    private boolean active;

    /** True if this repository has been shutdown. */
    private boolean shutdown;

    /** True is this repository is read-only. */
    private final boolean readOnly;

    /** True if this repository was created with a file: URL; false
     * otherwise. */
    private final boolean fileBased;

    private static final Map<String, String> DEFAULT_CONFIG = Collections.emptyMap();

    /** The platform on which this URLRepository is running. */
    private static String platform = RepositoryUtils.getPlatform();

    /** The architecture on which this URLRepository is running. */
    // Non-final to assist in testing.
    private static String arch = RepositoryUtils.getArch();;

    /** Name used to identify this class in exceptions. */
    private static final String IDENT = "URLRepository";

    /**
     * Creates a new <code>URLRepository</code> instance.
     * Initializes the repository.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's <code>checkPermission</code> method with
     * a <code>ModuleSystemPermission("createRepository")</code>
     * permission to ensure it's ok to create a repository.
     *
     * @param parent the parent repository for delegation.
     * @param name the repository name.
     * @param codebase the source location.
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to create a new instance of repository.
     * @throws IOException if the repository cannot be initialized.
     */
    public URLRepository(Repository parent, String name, URL codebase)
            throws IOException {
        this(parent, name, codebase, DEFAULT_CONFIG);
    }

    /**
     * Creates a new <code>URLRepository</code> instance using the
     * <code>Repository</code> returned by the method
     * <code>getSystemRepository()</code> as the parent repository.
     * Initializes the repository.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's <code>checkPermission</code> method with
     * a <code>ModuleSystemPermission("createRepository")</code>
     * permission to ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param codebase the source location.
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to create a new instance of repository.
     * @throws IOException if the repository cannot be initialized.
     */
    public URLRepository(String name, URL codebase)
            throws IOException {
        this(getSystemRepository(), name, codebase);
    }

    /**
     * Creates a new <code>URLRepository</code> instance, and initializes it
     * using information from the given {@code config}.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's <code>checkPermission</code> method with
     * a <code>ModuleSystemPermission("createRepository")</code>
     * permission to ensure it's ok to create a repository.
     *
     * @param parent the parent repository for delegation.
     * @param name the repository name.
     * @param codebase the source location.
     * @param config Map of configuration names to their values
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to create a new instance of repository.
     * @throws IOException if the repository cannot be initialized.
     */
    public URLRepository(Repository parent, String name, URL codebase,
            Map<String, String> config) throws IOException {
        super(parent, name, codebase, ModuleSystem.getDefault());

        fileBased = codebase.getProtocol().equals("file");
        readOnly = !fileBased;
        String tmp = codebase.toExternalForm();
        if (tmp.endsWith("/")) {
            sourcePath = tmp;
        } else {
            sourcePath = tmp + "/";
        }
        timestamp = System.currentTimeMillis();
        initialize(config);
    }

    /**
     * Creates a new <code>URLRepository</code> instance using the
     * <code>Repository</code> returned by the method
     * <code>getSystemRepository()</code> as the parent repository, and
     * initializes it using information from the given {@code config}.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's <code>checkPermission</code> method with
     * a <code>ModuleSystemPermission("createRepository")</code>
     * permission to ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param codebase the source location.
     * @param config Map of configuration names to their values
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to create a new instance of repository.
     * @throws IOException if the repository cannot be initialized.
     */
    public URLRepository(String name, URL codebase,
            Map<String, String> config) throws IOException {
        this(getSystemRepository(), name, codebase, config);
    }

    //
    // Extend Repository
    //

    /** Initializes the repository using the default configuration. */
    @Override
    public void initialize() throws IOException {
        initialize(DEFAULT_CONFIG);
    }

    /**
     * If this URLRepository uses a file: - based source:
     * <ul>
     * <li>
     * Copy the JAM file designated by the URL into this repository.
     * </li>
     * <li>
     * Create a MODULE.METADATA file for the JAM to the right directory in
     * this repository.
     * </li>
     * <li>
     * Update the repository's repository-metadata.xml file with information
     * about the newly-installed module.
     *</li>
     * </ul>
     */
    @Override
    public synchronized ModuleArchiveInfo install(URL u) throws IOException {
        assertActive();
        assertNotReadOnly();

        if (!u.getProtocol().equals("file")) {
            throw new IllegalArgumentException(
                msg("Only installation via file: protocol is supported"));
        }

        String srcJamFileName = u.getFile();
        if (!new File(srcJamFileName).exists()) {
            throw new IllegalArgumentException(
                msg("Cannot install non-existent file: " + srcJamFileName));
        }
        if (!(srcJamFileName.endsWith(".jam")
              || srcJamFileName.endsWith(".jar")
              || srcJamFileName.endsWith(".jam.pack.gz"))) {
            throw new IllegalArgumentException(
                msg("Only installation of .jam, .jar, and .jam.pack.gz files are supported; cannot install "
                    + srcJamFileName));
        }

        File srcJamFile = null;
        boolean packedJam = false;
        if (srcJamFileName.endsWith(".jam.pack.gz")) {
            packedJam = true;
            srcJamFile = File.createTempFile("jamgz-" + System.currentTimeMillis(), ".jar");
            JamUtils.unpackStreamAsFile(u.openStream(), srcJamFile);
            // Tag it for deletion, in case an exception is thrown between
            // here and when it is deleted below.
            srcJamFile.deleteOnExit();
        } else {
            srcJamFile = new File(srcJamFileName);
        }

        JarFile mdJamFile = new JarFile(srcJamFile);

        // Extracts module metadata from JAM file
        byte[] metadata = JamUtils.getMetadata(mdJamFile);
        mdJamFile.close();

        // Constructs a superpackage from module metadata, for extracting the
        // stock annotations.
        java.lang.reflect.Superpackage superPackage = sun.module.JamUtils.getSuperpackage(metadata);

        // Module name
        String moduleName = superPackage.getName();

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
        String modulePlatform = null;
        String moduleArch = null;
        String moduleBinding = null;
        if (platformBinding != null) {
            modulePlatform = platformBinding.platform();
            moduleArch = platformBinding.arch();
            moduleBinding = modulePlatform + "-" + moduleArch;
        }

        if (((modulePlatform == null) ^ (moduleArch == null))) {
            throw new IOException(
                msg(
                    "Module " + moduleName
                    + " v" + moduleVersion
                    + " has mismatched platform and architecture:"
                    + " platform=" + platform + ", arch=" + arch));
        }

        // Refuse to install if comparable module is already installed.
        for (ModuleArchiveInfo mai : contents.getModuleArchiveInfos()) {
            if (moduleName.equals(mai.getName())
                && moduleVersion.equals(mai.getVersion())) {

                // Platform binding matches only if both are null or both have
                // same platform and arch.  The test for platform XOR arch was
                // done above so don't repeat that here, and never create a
                // ModuleArchiveInfo where there's a mismatch.
                if ((modulePlatform == null && mai.getPlatform() == null)
                    || (modulePlatform.equals(mai.getPlatform())
                     && moduleArch.equals(mai.getArchitecture()))) {
                    throw new IllegalStateException(
                        msg(
                            "Module " + moduleName
                            + "  v" + moduleVersion
                            + " already exists in the repository at "
                            + getSourceLocation()));
                }
            }
        }

        initializeCache();

        /*
         * Installing the module requires these steps:
         * (1) Creating MODULE.METADATA
         * (2) Copying the JAM file
         * (3) Creating and caching the ModuleDefinition
         * (4) Updating repository-metadata.xml
         * (5) updating the contents field of this repository instance
         *
         * If any step fails, undo any of the other steps already done.
         */

        // (1) Create the new MODULE.METADATA file.
        //
        File sourceDir = new File(getSourceLocation().getFile());

        // The module destination directory in the URLRepository should be
        // <source location>/<module-name>/<module-version>
        //    or
        // <source location>/<module-name>/<module-version>/<platform>-<arch>
        File moduleDestDir = new File(sourceDir,
                                      getFilePath(moduleName, moduleVersion, modulePlatform, moduleArch));
        moduleDestDir.mkdirs();

        // Copy MODULE.METADATA file
        File destMDFile = new File(moduleDestDir, JamUtils.MODULE_METADATA);
        BufferedOutputStream bos =
            new BufferedOutputStream(new FileOutputStream(destMDFile));
        bos.write(metadata, 0, metadata.length);
        bos.flush();
        bos.close();

        // Copy JAM file
        String destJamName = JamUtils.getJamFilename(moduleName, moduleVersion, modulePlatform, moduleArch);
        File destJamFile = new File(moduleDestDir, destJamName);

        ModuleArchiveInfo mai = null;
        try {
            // (2) Copy the JAM file
            JamUtils.copyFile(srcJamFile, destJamFile);
            if (packedJam) {
                srcJamFile.delete();
            }

            // (3) Create and cache the ModuleDefinition
            mai = new ModuleArchiveInfo(
                this, moduleName, moduleVersion,
                modulePlatform, moduleArch,
                destJamFile.getCanonicalPath(), destJamFile.lastModified());

            ModuleDefinition installedMD = null;
            if (RepositoryUtils.bindingMatches(platformBinding, platform, arch)) {
                installedMD = RepositoryUtils.createURLModuleDefinition(
                    this,
                    downloadDir,
                    new ModuleInfo(
                        moduleName,
                        moduleVersion,
                        modulePlatform,
                        moduleArch,
                        null),
                    destMDFile);
                modDefCache.add(installedMD);
            }

            // (4) Create an updated repository-metadata.xml.tmp file
            writeRepositoryMetadata(mai, true);

            // (5) Update contents field
            contents.put(mai, installedMD);

        } catch (IOException ex) {
            destMDFile.delete();
            destJamFile.delete();
            throw ex;
        }

        // Send MODULE_INSTALLED event
        RepositoryEvent evt = new RepositoryEvent(this, RepositoryEvent.Type.MODULE_INSTALLED, mai);
        processEvent(evt);

        return mai;
    }

    @Override
    public synchronized boolean uninstall(ModuleArchiveInfo mai) throws IOException {
        assertActive();
        assertNotReadOnly();
        assertValidDirs();
        return uninstall0(mai);
    }

    /**
     * Uninstalls the specified module.  In a http-based repository, this is
     * limited to removing downloaded temporary structures and in-memory cache
     * structures.  In a file-based repository, this also removes the metadata
     * and associated JAM file.
     * @param mai Specifies module to uninstall
     * @throws IOException if an error occurs during uninstallation
     */
    private boolean uninstall0(ModuleArchiveInfo mai) throws IOException  {
        if (!contents.contains(mai)) {
            return false;
        }

        File sourceDir = new File(getSourceLocation().getFile());

        // Delete file/filesystem resources related to md, and then
        // remove it from contents.

        // XXX ModuleArchiveInfo doesn't contain path information.  Get from
        // XXX ModuleInfo?  Defer until install() handles it?

        String moduleName = mai.getName();
        Version moduleVersion = mai.getVersion();
        String modulePlatform = mai.getPlatform();
        String moduleArch = mai.getArchitecture();

        // This is the directory which contains MODULE.METADATA and the JAM file
        File moduleDir = new File(sourceDir, getFilePath(moduleName, moduleVersion, modulePlatform, moduleArch));
        verifyExistence(moduleDir);

        String jamName = JamUtils.getJamFilenameNoExt(moduleName, moduleVersion, modulePlatform, moduleArch);

        // This is where any downloads for this module are
        File moduleDownloadDir = new File(downloadDir, jamName);

        /*
         * Presume that ability to rename implies ability to remove:
         * rename the files to be deleted; if that succeeds then
         * delete them.  If any rename or deletion fails, undo
         * whatever can be undone.
         */
        File moduleDirToRemove = null;
        File moduleDownloadDirToRemove = null;

        try {
            moduleDirToRemove = rename(moduleDir);
            if (moduleDownloadDir.isDirectory()) {
                // XXX Remove the Windows-specificity once disableModuleDefinition is implemented
                if (!platform.startsWith("windows")) {
                    moduleDownloadDirToRemove = rename(moduleDownloadDir);
                }
            }

            // Updated the repository metadata without the specified
            // module definition.
            writeRepositoryMetadata(mai, false);

            if (!JamUtils.recursiveDelete(moduleDirToRemove)) {
                throw new IOException(
                    msg("Could not remove directory: " + moduleDirToRemove.getCanonicalPath()));
            }
            // XXX: should we really throw exception if we can't clean up the
            // internal cache only but everything else works?
            if (moduleDownloadDirToRemove != null
                && !JamUtils.recursiveDelete(moduleDownloadDirToRemove)) {
                throw new IOException(
                    msg("Could not remove directory: " + moduleDownloadDirToRemove.getCanonicalPath()));
            }
        } catch (IOException ex) {
            // restore previus state as much as possible
            if (moduleDownloadDirToRemove != null) {
                rename(moduleDownloadDirToRemove, moduleDownloadDir);
            }
            if (moduleDirToRemove != null) {
                rename(moduleDirToRemove, moduleDir);
            }
            writeRepositoryMetadata(mai, true);
            throw ex;
        }

        return uninstall1(mai);
    }

    /**
     * Updates data structures for an uninstall, and sends MODULE_UNINSTALLED
     * event.
     */
    private boolean uninstall1(ModuleArchiveInfo mai) {
        ModuleDefinition md = contents.get(mai);
        // XXX Uncomment below disableModuleDefinition is implemented
        //if (md != null) {
        //    getModuleSystem().disableModuleDefinition(md);
        //}
        modDefCache.remove(md);
        boolean rc = contents.remove(mai);

        if (rc) {
            // Send MODULE_UNINSTALLED event
            RepositoryEvent evt = new RepositoryEvent(this, RepositoryEvent.Type.MODULE_UNINSTALLED, mai);
            processEvent(evt);
        }
        return rc;
    }

    /**
     * Finds all matching module definitions in this repository.
     *
     * @param constraint the constraint.
     * @return the collection of matching module definitions.
     */
    @Override
    public synchronized List<ModuleDefinition> findModuleDefinitions(Query constraint) {
        assertActive();
        return RepositoryUtils.findModuleDefinitions(constraint, modDefCache);
    }

    @Override
    public List<ModuleArchiveInfo> list() {
        return Collections.unmodifiableList(contents.getModuleArchiveInfos());
    }

    /**
     * Compares the cache with a freshly-downloaded repository-metadata.xml.
     * Modules that are in both places are not affected.  Modules in the cache
     * but not metadata are uninstalled.  Modules in the metadata but not the
     * cache, for which a MODULE.METADATA is newer than that already
     * installed, have their metadata loaded.
     */
    @Override
    public synchronized void reload() throws IOException {
        assertActive();

        initializeCache();

        URL repoMD = new URL(sourcePath + "repository-metadata.xml");
        try {
            // Build a list of modules to uninstall, and of modules currently
            // installed that won't be uninstalled by this reload.
            Set<ModuleInfo> moduleInfoSet = MetadataXMLReader.read(repoMD);
            Set<ModuleArchiveInfo> uninstallCandidates = new HashSet<ModuleArchiveInfo>();
            Map<ModuleInfo, ModuleArchiveInfo> replaceCandidates =
                new HashMap<ModuleInfo, ModuleArchiveInfo>();

            for (ModuleArchiveInfo mai : contents.getModuleArchiveInfos()) {
                ModuleInfo mi = new ModuleInfo(mai);
                if (!moduleInfoSet.contains(mi)) {
                    uninstallCandidates.add(mai);
                } else {
                    replaceCandidates.put(mi, mai);
                }

                // Remove a ModuleInfo from this set if it has a corresponding
                // entry in contents; that is, if it is already installed in
                // this repository.  After this loop, moduleInfoSet will list
                // entries newly-added to repository-metadata.xml
                moduleInfoSet.remove(mi);
            }

            // Uninstall modules for which there is no corresponding file in the
            // source directory
            for (ModuleArchiveInfo mai : uninstallCandidates) {
                uninstall1(mai);
            }

            // Reload already-installed modules.
            for (Map.Entry<ModuleInfo, ModuleArchiveInfo> entry : replaceCandidates.entrySet()) {
                ModuleArchiveInfo mai =
                    loadMetadata(entry.getKey(), entry.getValue());
                RepositoryEvent evt = new RepositoryEvent(
                    this, RepositoryEvent.Type.MODULE_INSTALLED, mai);
                processEvent(evt);
            }

            // Load new modules
            for (ModuleInfo mi : moduleInfoSet) {
                ModuleArchiveInfo mai = loadMetadata(mi, null);
                RepositoryEvent evt = new RepositoryEvent(
                    this, RepositoryEvent.Type.MODULE_INSTALLED, mai);
                processEvent(evt);

            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(
                msg("Error reloading repository at " + sourcePath
                    + ": " + ex.getMessage()), ex);
        }
    }

    /**
     * Uninstalls all modules.  Removes repository-specific directories.
     *
     * @throws java.io.IOException if there's an error removing directories.
     * */
    @Override
    public synchronized void shutdown() throws IOException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("shutdownRepository"));
        }
        if (shutdown) {
            return;             // shutdown only once
        }
        assertActive();

        removeCache();

        active = false;
        shutdown = true;

        // Send REPOSITORY_SHUTDOWN event
        RepositoryEvent evt = new RepositoryEvent(this, RepositoryEvent.Type.REPOSITORY_SHUTDOWN);
        processEvent(evt);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * @return true if this repository is read-only, which is the case if it
     * was created with any other than a file: URL.
     */
    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * @return true if this repository's source location is a file-based URL.
     */
    @Override
    public boolean isReloadSupported() {
        return getSourceLocation().getProtocol().equals("file");
    }

    //
    // Implementation specific to URLRepository
    //

    /**
     * @return the path of the url given in the constructor but always with a
     * trailing '/'.
     */
    String getSourcePath() {
        return sourcePath;
    }

    /**
     * Initializes the repository instance using the supplied configuration.
     * Loads configuration information from the given {@code Map}.  Only
     * examines entries that start with
     * {@code sun.module.repository.URLRepository.}.
     *
     * These keys are checked for in {@code config} and in System properties;
     * entries in System properties override entries in {@code config}:
     * <ul>
     * <li>{@code sun.module.repository.URLRepository.downloadDirectory}:
     * Specifies the directory into which the contents of
     * JAM files are downloaded.  Will be created if it
     * does not already exist.
     * </li>
     * </ul>
     * <em>Note:</em> Values are read from System properties only once, when
     * this class is loaded.
     * <p>
     * @param config Map of configuration names to their values
     * @throws IOException if the repository cannot be initialized.
     */
    private synchronized void initialize(Map<String, String> config) throws IOException {
        if (active) {
            return;             // initialize only once
        }
        if (shutdown) {
            throw new IllegalStateException(msg("Repository has been shut down."));
        }
        if (config == null) {
            throw new NullPointerException(msg("Parameter 'config' cannot be null."));
        }

        if ("true".equalsIgnoreCase(sysPropSourceLocMustExist)) {
            sourceLocMustExist = true;
        } else {
            sourceLocMustExist = "true".equalsIgnoreCase(config.get(SOURCELOC_MUST_EXIST_KEY));
        }
        downloadDirName = sysPropDownloadDirName != null
            ? sysPropDownloadDirName : config.get(DOWNLOAD_DIR_KEY);

        // The ability to configure platform and arch are for testing only!
        String value;
        if ((value = config.get(PROPERTY_PREFIX + "test.platform")) != null) {
            platform = value;
        }
        if ((value = config.get(PROPERTY_PREFIX + "test.arch")) != null) {
            arch = value;
        }
        Set<ModuleInfo> moduleInfoSet = null;
        try {
            URL repoMD = new URL(sourcePath + "repository-metadata.xml");
            moduleInfoSet = MetadataXMLReader.read(repoMD);
        } catch (IOException ex) {
            if (sourceLocMustExist) {
                throw ex;
            }
        } catch (Exception ex) {
            throw new IOException(
                msg("Error processing repository-metadata.xml: " + ex.getMessage()), ex);
        }

        if (moduleInfoSet != null) {
            initializeCache();
            for (ModuleInfo mi : moduleInfoSet) {
                loadMetadata(mi, null);
            }
        }

        active = true;

        // Send REPOSITORY_INITIALIZED event
        RepositoryEvent evt = new RepositoryEvent(this, RepositoryEvent.Type.REPOSITORY_INITIALIZED);
        processEvent(evt);
    }

    /**
     * Loads MODULE.METADATA from {@code sourcePath} for the given
     * {code ModuleInfo}.  After this, the module definition is available via
     * the find and list methods.
     * @param moduleInfo Describes the module whose metadata is to be loaded
     * @param mai Denotes an already-installed module.  If not null, then the
     * module described by {@code moduleInfo} is loaded only if it is newer
     * than the module denoted by {@code mai}.
     * @return ModuleArchiveInfo for the newly-loaded module or for the
     * already-installed module as appropriate.
     */
    private ModuleArchiveInfo loadMetadata(
            ModuleInfo moduleInfo, ModuleArchiveInfo mai) throws IOException {

        URL moduleMD = new URL(sourcePath + moduleInfo.getCanonicalizedPath() + "/" + JamUtils.MODULE_METADATA);
        URLConnection uc = null;
        try {
            uc = moduleMD.openConnection();
        } catch (IOException ex) {
            throw new IOException(msg("Cannot open connection to " + moduleMD), ex);
        }

        File moduleDir = new File(
            downloadDir + File.separator + moduleInfo.getName()
            + (moduleInfo.getVersion() == null ? "" : "-" + moduleInfo.getVersion()));
        File mdFile = new File(moduleDir, JamUtils.MODULE_METADATA);

        // During reload, only download metadata if it is for a new or
        // replaced JAM.
        if (mai != null) {
            if (mdFile.exists()) {
                long fileMod = mdFile.lastModified();
                long urlMod = uc.getLastModified();
                if (fileMod != 0 && fileMod == urlMod) {
                    return mai;
                } else {
                    modDefCache.remove(contents.get(mai));
                    contents.remove(mai);
                }
            }
        }

        InputStream is = null;
        OutputStream os = null;
        try {
            is = new BufferedInputStream(uc.getInputStream(),
                                         JamUtils.BUFFER_SIZE);
        } catch (IOException ex) {
            throw new IOException(msg("Cannot load " + JamUtils.MODULE_METADATA
                                      + " for " + moduleMD), ex);
        }

        try {
            // The repository-metadata.xml file might specify a path, but
            // ignore that when storing the metadata locally.
            if (!moduleDir.isDirectory() && !moduleDir.mkdirs()) {
                throw new IOException(msg("Cannot create directory for metadata: " + moduleDir));
            }
            os = new BufferedOutputStream(
                    new FileOutputStream(mdFile));

            byte[] buf = new byte[JamUtils.BUFFER_SIZE];
            int len = 0;
            while ((len = is.read(buf, 0, buf.length)) > 0) {
                os.write(buf, 0, len);
            }
            os.close();
            ModuleDefinition md = RepositoryUtils.createURLModuleDefinition(
                this, downloadDir, moduleInfo, mdFile);
            mai = new ModuleArchiveInfo(
                this, md.getName(), md.getVersion(),
                moduleInfo.getPlatform(), moduleInfo.getArch(),
                null, 0);

            PlatformBinding platformBinding = md.getAnnotation(PlatformBinding.class);
            if (RepositoryUtils.bindingMatches(platformBinding, platform, arch)) {
                modDefCache.add(md);
                contents.put(mai, md);
            } else {
                contents.put(mai, null);
            }
            return mai;
        } finally {
            JamUtils.close(is);
            JamUtils.close(os);
        }
    }

    /**
     * Writes the repository's repository-metadata.xml file based on {@code
     * contents}.
     * @param mai {@code ModuleArchiveInfo} on behalf of which the metadata is
     * written.
     * @param writeMAI if true, then write the given {@code mai} in addition
     * to the repository's other contents.
     */
    private void writeRepositoryMetadata(
            ModuleArchiveInfo mai,
            boolean writeMAI) throws IOException {
        URL repoMD = new URL(
            getSourceLocation().toExternalForm()
            + "/repository-metadata.xml");
        File repoMDFile = new File(repoMD.getFile());
        File repoMDDir = repoMDFile.getParentFile();
        File tmpRepoMDFile = new File(repoMDDir, "repository-metadata.xml.tmp");
        if (tmpRepoMDFile.exists()) {
            if (!tmpRepoMDFile.delete()) {
                throw new IOException(
                    msg("Cannot update repository metadata file for "
                        + mai.getName()
                        + ": cannot create temporary repository metadata file "
                        + tmpRepoMDFile));
            }
        }
        RepoMDWriter writer = new RepoMDWriter(tmpRepoMDFile);
        writer.begin();
        for (ModuleArchiveInfo m : contents.getModuleArchiveInfos()) {
            if (!writeMAI && m.equals(mai)) {
                // Don't write this ModuleArchiveInfo if it matches that given
            } else {
                writer.writeModule(m);
            }
        }
        if (writeMAI) {
            writer.writeModule(mai);
        }

        if (!writer.end()) {
            throw new IOException(
                msg("Cannot update repository metadata file for "
                    + mai.getName()
                    + ": failure while writing temporary repository metadata file "
                    + tmpRepoMDFile));
        }

        File prev = new File(repoMDFile.getCanonicalPath() + ".prev");
        prev.delete();
        if (!repoMDFile.renameTo(prev)) {
            throw new IOException(
                msg("Cannot update repository metadata file for "
                    + mai.getName()
                    + ": cannot rename " + repoMDFile + " to " + prev));
        }
        prev.deleteOnExit();

        if (!tmpRepoMDFile.renameTo(repoMDFile)) {
            throw new IOException(
                msg("Cannot update repository metadata file for "
                    + mai.getName()
                    + ": cannot create updated repository-metadata.xml file"
                    + " by renaming " + tmpRepoMDFile + " to " + repoMDFile));
        }
    }

    /** Writes XML for a repository-metadata.xml file. */
    static class RepoMDWriter {
        private final PrintWriter pw;
        private int indent = 0;
        private static final int WIDTH = 4;
        private static final String spaces = "                                ";

        RepoMDWriter(File out) throws IOException {
            pw = new PrintWriter(
                new BufferedOutputStream(
                    new FileOutputStream(out)), false);
        }

        void begin() {
            pw.println("<modules>");
            indent++;
        }

        boolean end() {
            boolean rc = false;
            indent--;
            pw.println("</modules>");
            rc = pw.checkError();
            pw.close();
            return !rc; // Return false if error
        }

        void writeModule(ModuleArchiveInfo mai) {
            output("<module>");
            indent++;
            output("<name>" + mai.getName() + "</name>");
            output("<version>" + mai.getVersion().toString() + "</version>");
            String platform = mai.getPlatform();
            if (platform != null) {
                output("<platform-binding>");
                indent++;
                output("<platform>" + platform + "</platform>");
                output("<arch>" + mai.getArchitecture() + "</arch>");
                indent--;
                output("</platform-binding>");
            }

            // Note that we don't support <path>, since there's no way to
            // specify that via Repository.install().

            indent--;
            output("</module>");
        }

        void output(String s) {
            pw.print(spaces.substring(0, indent * WIDTH));
            pw.println(s);
        }
    }

    /**
     * Create a directory.
     * @param parentDirName Client-provided full pathname to a directory.  May
     * be null.
     * @param dirName Name of directory to be created in {@code
     * parentDirName}.  The actual directory created is suffixed with the
     * repository's {@code timestamp}.  If the {@code parentDirName} is null,
     * then {@code dirName} is created in a repository-specific directory
     * under the system temporary directory.
     * @return a {@code File} representing the directory
     * @throws IOException if the directory cannot be created
     */
    private File createDir(String parentDirName, String dirName) throws IOException {
        File rc = new File(parentDirName,
                           getName() + "-" + timestamp + "-" + dirName);

        if (!rc.isDirectory() && !rc.mkdirs()) {
            try {
                // Remove any intermediate directories created
                JamUtils.recursiveDelete(rc);
            } catch (IOException ex) {
                // Ignore if some cannot be removed
            }
            throw new IOException(msg("Cannot create directory at "
                                      + rc.getAbsolutePath()));
        }
        return rc;
    }

    /**
     * Ensure that the required directories exist.
     */
    private void initializeCache() throws IOException {
        if (fileBased) {
            File sourceDir = new File(getSourceLocation().getFile());
            File repoMDFile = new File(sourceDir, "repository-metadata.xml");
            if (!repoMDFile.exists()) {
                BufferedOutputStream bos =
                    new BufferedOutputStream(new FileOutputStream(repoMDFile));
                bos.write("<modules></modules>".getBytes("ASCII"));
                bos.close();
            }
        }
        if (downloadDir == null) {
            downloadDir = createDir(downloadDirName, "download");
        }
    }

    /**
     * Removes the contents of the repository's metadata and download
     * directories.
     */
    private void removeCache() throws IOException {
        if (downloadDir != null) {
            JamUtils.recursiveDelete(downloadDir);
        }

        downloadDir = null;
        contents.clear();
    }

    /**
     * @throws IOException if file {@code f} doesn't exist
     */
    private void verifyExistence(File f) throws IOException {
        if (!f.exists()) {
            throw new IOException(
                msg("Expected file is missing: " + f.getAbsolutePath()));
        }
    }

    /**
     * @return file {@code prev} renamed by adding a time-based extension
     */
    private File rename(File prev) throws IOException {
        File next = new File(prev.getCanonicalPath() + "." + System.currentTimeMillis());
        if (!prev.renameTo(next)) {
            throw new IOException(
                msg("Could not rename file for deletion: "
                    + prev.getCanonicalPath()));
        }
        return next;
    }

    /**
     * Renames {@code prev} to {@code next} if prev is not null
     */
    private void rename(File prev, File next) {
        if (prev != null) {
            prev.renameTo(next);
        }
    }

    /**
     * Returns file path based on module name, version, platform and architecture.
     *
     * @param name module name
     * @param version module version
     * @param platform target platform
     * @param arch target architecture
     * @return file path
     */
    private static String getFilePath(String name, Version version, String platform, String arch) {
        return name + File.separator + version + ((platform == null) ? "" : File.separator + platform + "-" + arch);
    }

    private void assertActive() throws IllegalStateException {
         if (!isActive()) {
             throw new IllegalStateException(msg("Repository is not active."));
        }
    }

    private void assertNotActive() throws IllegalStateException {
         if (isActive()) {
             throw new IllegalStateException(msg("Repository is active."));
        }
    }

    private void assertNotReadOnly() throws IllegalStateException {
        if (isReadOnly()) {
            throw new UnsupportedOperationException(msg("Repository is read-only."));
        }
    }

    private void assertValidDirs() throws IOException {
        if (fileBased) {
            File sourceDir = new File(getSourceLocation().getFile());
            if (sourceDir == null || !sourceDir.isDirectory()) {
                missingDir("source", sourceDir);
            }
        }
        if (downloadDir == null || !downloadDir.isDirectory()) {
            missingDir("download", downloadDir);
        }
    }

    private void missingDir(String type, File dir) throws IOException {
        throw new IOException(
            msg("specified " + type + " directory "
                + dir + " does not exist or is not a directory"));
    }

    String msg(String s) {
        return IDENT + " " + getName() + " at "
            + getSourceLocation().toExternalForm() + ": " + s;
    }

}
