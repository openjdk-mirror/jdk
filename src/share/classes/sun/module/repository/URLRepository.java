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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.module.ModuleArchiveInfo;
import java.module.ModuleDefinition;
import java.module.ModuleDefinitionContent;
import java.module.ModuleFormatException;
import java.module.ModuleSystem;
import java.module.ModuleSystemPermission;
import java.module.Modules;
import java.module.Query;
import java.module.Repository;
import java.module.RepositoryEvent;
import java.module.Version;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import sun.module.JamUtils;
import sun.module.repository.cache.Cache;
import sun.module.repository.cache.ModuleDefInfo;

/**
 * This class represents a repository that loads module definitions from a
 * codebase URL.
 * <p>
 * Information about the module definitions available from the codebase URL
 * must be published in a repository metadata file. The contents of the file
 * must follow the schema of the URL Repository metadata for the Java Module
 * System.
 * <p><i>
 *      {codebase}/repository-metadata.xml
 * <p></i>
 * When the repository is initialized, the repository metadata file (i.e.
 * repository-metadata.xml) would be downloaded from the codebase URL.
 * <p>
 * In the repository metadata file, each module definition is described with
 * a name, a version, a platform binding, and a path (relative to the codebase
 * URL where the module file, the module archive, and/or the packed module
 * archive are located). If no path and no platform binding is specified, the
 * default path is "{name}/{version}". If the path is not specified and the
 * module definition has platform binding, the default path is
 * "{name}/{version}/{platform}-{arch}".
 * <p>
 * After the URL repository instance successfully downloads the repository
 * metadata file, the module metadata file of each module definition
 * (i.e. MODULE.METADATA file) in the repository is downloaded based on the
 * information in the repository metadata file:
 * <p><i>
 *      {codebase}/{path}/MODULE.METADATA
 * <p></i>
 * If a module definition is platform-specific, its module metadata file is
 * downloaded if and only if the platform binding described in the
 * repository metadata file matches the platform and the architecture of the
 * system.
 * <p>
 * Module definitions are available for searches after the URL repository
 * instance is initialized. If a module instance is instantiated from a module
 * definition that has no platform binding, the module archive is downloaded
 * by probing in the following order:
 * <p><i>
 *      {codebase}/{path}/{name}-{version}.jam.pack.gz<p>
 *      {codebase}/{path}/{name}-{version}.jam
 * <p></i>
 * On the other hand, if a module instance is instantiated from a
 * platform-specific module definition, the module archive is downloaded by
 * probing in the following order:
 * <p><i>
 *      {codebase}/{path}/{name}-{version}-{platform}-{arch}.jam.pack.gz<p>
 *      {codebase}/{path}/{name}-{version}-{platform}-{arch}.jam
 * <p></i>
 * To ensure the integrity of the separately-hosted module file is in sync
 * with that in the module archive of the same module definition, they are
 * compared bit-wise against each other after the module archive is downloaded.
 *
 * @see java.module.ModuleArchiveInfo
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystemPermission
 * @see java.module.Query
 * @see java.module.Repository
 * @since 1.7
 */
public final class URLRepository extends AbstractRepository {

    /** Prefix of all properties use to configure this repository. */
    private static final String PROPERTY_PREFIX = "sun.module.repository.URLRepository.";

    /**
     * Directory into which MODULE.METADATA file and JAM files are cached
     * and expanded.
     */
    private File cacheDirectory;

    /**
     * Name of metadata directory based on system property and config in
     * {@link #initialize(Map<String,String>)}.
     */
    private String cacheDirName;

    /**
     * Directory name corresponding to {@code CACHE_DIR_KEY} as obtained
     * from system properties.
     */
    private static final String sysPropCacheDirName;

    /**
     * Property name to configure the directory to which JAM files are
     * cached.
     */
    private static final String CACHE_DIR_KEY =
        PROPERTY_PREFIX + "cacheDirectory";

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
        sysPropCacheDirName = RepositoryUtils.getProperty(CACHE_DIR_KEY);
        sysPropSourceLocMustExist = RepositoryUtils.getProperty(SOURCELOC_MUST_EXIST_KEY);
    }

    /** String form of codebase given in constructor but with a trailing '/'. */
    private URL canonicalizedCodebase;

    /** The platform on which this URLRepository is running. */
    private static String platform = RepositoryUtils.getPlatform();

    /** The architecture on which this URLRepository is running. */
    // Non-final to assist in testing.
    private static String arch = RepositoryUtils.getArch();;

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
        super(parent, name, codebase, config);
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
    // Extend AbstractRepository
    //

    /**
     * Initializes the repository instance using the supplied configuration.
     * Loads configuration information from the given {@code Map}.  Only
     * examines entries that start with
     * {@code sun.module.repository.URLRepository.}.
     *
     * These keys are checked for in {@code config} and in System properties;
     * entries in System properties override entries in {@code config}:
     * <ul>
     * <li>{@code sun.module.repository.URLRepository.cacheDirectory}:
     * Specifies the directory into which the contents of JAM files
     * are cached.  Will be created if it does not already exist.
     * </li>
     * </ul>
     * <em>Note:</em> Values are read from System properties only once, when
     * this class is loaded.
     * <p>
     * @param config Map of configuration names to their values
     * @throws IOException if the repository cannot be initialized.
     */
    protected final void doInitialize(Map<String, String> config) throws IOException {
        String tmp = getSourceLocation().toExternalForm();
        if (tmp.endsWith("/")) {
            canonicalizedCodebase = new URL(tmp);
        } else {
            canonicalizedCodebase = new URL(tmp + "/");
        }

        if ("true".equalsIgnoreCase(sysPropSourceLocMustExist)) {
            sourceLocMustExist = true;
        } else {
            sourceLocMustExist = "true".equalsIgnoreCase(config.get(SOURCELOC_MUST_EXIST_KEY));
        }

        cacheDirName = sysPropCacheDirName != null
            ? sysPropCacheDirName : config.get(CACHE_DIR_KEY);

        // Constructs a repository cache instance
        if (cacheDirName == null) {
            repositoryCache = Cache.newInstance();
        } else {
            repositoryCache = Cache.newInstance(new File(cacheDirName));
        }

        assertValidDirs();

        // The ability to configure platform and arch are for testing only!
        String v;
        if ((v = config.get(PROPERTY_PREFIX + "test.platform")) != null) {
            platform = v;
        }
        if ((v = config.get(PROPERTY_PREFIX + "test.arch")) != null) {
            arch = v;
        }

        Set<ModuleInfo> moduleInfoSet = null;
        try {
            URL repoMD = new URL(canonicalizedCodebase + "repository-metadata.xml");
            moduleInfoSet = MetadataXMLReader.read(repoMD);

            // Initializes the internal data structures based on the module
            // info set.
            doInitialize(moduleInfoSet);
        } catch (IOException ex) {
            // no-op
        } catch (Exception ex) {
            throw new IOException(
                "Error processing repository-metadata.xml: " + ex.getMessage(), ex);
        }
    }

    /**
     * Initialize the internal data structures of the repository based on the
     * modules information in the module info set. Creates the appropriate
     * module definitions if necessary.
     */
    private void doInitialize(Set<ModuleInfo> moduleInfoSet) throws IOException {
        if (moduleInfoSet != null) {
            Map<ModuleArchiveInfo, ModuleDefInfo> mdInfoMap =
                                        new HashMap<ModuleArchiveInfo, ModuleDefInfo>();
            for (ModuleInfo mi : moduleInfoSet) {
                try {
                    // Retrieves the module metadata
                    ModuleDefInfo mdInfo = repositoryCache.getModuleDefInfo(
                                            canonicalizedCodebase,
                                            mi.getName(), mi.getVersion(),
                                            mi.getPlatform(), mi.getArch(),
                                            mi.getCanonicalizedPath());

                    // Constructs a module archive info
                    ModuleArchiveInfo mai = new ModuleArchiveInfo(
                        this, mdInfo.getName(), mdInfo.getVersion(),
                        mdInfo.getPlatform(), mdInfo.getArch(), null, 0);

                    mdInfoMap.put(mai, mdInfo);

                    // Adds the module archive info into the internal data structure.
                    moduleArchiveInfos.add(mai);
                } catch (Exception ex) {
                    // XXX log warning but otherwise ignore
                    if (mi.getPlatform() == null && mi.getArch() == null) {
                        System.err.println("Failed to load module " + mi.getName()
                                           + " v" + mi.getVersion() + " from "
                                           + getSourceLocation() + ": " + ex);
                    } else {
                        System.err.println("Failed to load module " + mi.getName()
                                           + " v" + mi.getVersion() + " "
                                           + mi.getPlatform() + "-" + mi.getArch()
                                           + " from " + getSourceLocation() + ": " + ex);
                    }
                }
            }

            // Constructs the module definitions from the module archives
            // based on the specified platform and architecture.
            constructModuleDefinitions(mdInfoMap, platform, arch);
        }
    }

    /**
     * Install a module archive from a URL.
     */
    protected ModuleArchiveInfo doInstall(URL url) throws IOException {
        InputStream is = null;
        File sourceFile = null;
        File tmpFile = null;
        File destMDFile = null;
        File destJamFile = null;

        try {
            URLConnection uc = url.openConnection();
            is = uc.getInputStream();

            // Store the file in a temp directory first. Unpack the file if
            // necessary.
            sourceFile = File.createTempFile("url-repository-install-", "tmp");
            sourceFile.deleteOnExit();
            JamUtils.saveStreamAsFile(is, sourceFile);

            if (url.getFile().endsWith(".jam.pack.gz")) {
                tmpFile = File.createTempFile("url-repository-install-", "tmp");
                tmpFile.deleteOnExit();
                JamUtils.unpackJamPackGz(sourceFile, tmpFile);
            } else {
                tmpFile = sourceFile;
            }

            // Retrieve the module metadata from the JAM file to find out
            // the module information, e.g. name, version, etc.
            //
            // No need to shadow copy (if set) because this is a temp file.
            ModuleDefInfo mdInfo = repositoryCache.getModuleDefInfo(tmpFile, false);

            // Check to see if there exists a module archive that has
            // the same name, version, and platform binding.
            for (ModuleArchiveInfo mai : moduleArchiveInfos) {
                if (mai.getName().equals(mdInfo.getName())
                    && mai.getVersion().equals(mdInfo.getVersion()))  {
                    if (mai.isPlatformArchNeutral()) {
                        if (mdInfo.isPlatformArchNeutral()) {
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

            /*
             * Installing the module requires these steps:
             * (1) Creating MODULE.METADATA
             * (2) Copying the JAM file
             * (3) Updating repository-metadata.xml
             * (4) updating the internal data structures of this repository instance
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
            File moduleDestDir= new File(sourceDir, getFilePath(mdInfo.getName(), mdInfo.getVersion(),
                                         mdInfo.getPlatform(), mdInfo.getArch()));
            moduleDestDir.mkdirs();

            // Copy MODULE.METADATA file
            destMDFile = new File(moduleDestDir, JamUtils.MODULE_METADATA);
            BufferedOutputStream bos =
                new BufferedOutputStream(new FileOutputStream(destMDFile));
            byte[] metadataBytes = mdInfo.getMetadataBytes();
            bos.write(metadataBytes, 0, metadataBytes.length);
            bos.flush();
            bos.close();

            // Copy JAM file
            String destFileName = JamUtils.getJamFilename(mdInfo.getName(),
                                            mdInfo.getVersion(),
                                            mdInfo.getPlatform(),
                                            mdInfo.getArch());
            if (sourceFile == tmpFile) {
                // no unpack
                destFileName +=".jam";
            } else {
                destFileName +=".jam.pack.gz";
            }

            destJamFile = new File(moduleDestDir, destFileName);

            // (2) Copy the JAM file
            JamUtils.copyFile(sourceFile, destJamFile);

            // (3) Create an updated repository-metadata.xml file
            //
            // Note that we create a temp ModuleArchiveInfo so we
            // could update the repository metadata before we
            // have a real ModuleArchiveInfo in step 4.
            writeRepositoryMetadata(new ModuleArchiveInfo(this, mdInfo.getName(),
                                        mdInfo.getVersion(), mdInfo.getPlatform(),
                                        mdInfo.getArch(), null, 0), true);

            // (4) Update internal data structures.
            return addModuleArchiveInternal(destJamFile);
        } catch (IOException ex) {
            if (destMDFile != null) {
                destMDFile.delete();
            }
            if (destJamFile != null) {
                destJamFile.delete();
            }
            throw ex;
        } finally {
            if (sourceFile != null) {
                sourceFile.delete();
            }
            if (tmpFile != null) {
                tmpFile.delete();
            }
            JamUtils.close(is);
        }
    }

    /**
     * Uninstall a module archive.
     */
    protected boolean doUninstall(ModuleArchiveInfo mai) throws IOException {
        // Checks if the module archive still exists.
        if (!moduleArchiveInfos.contains(mai)) {
            return false;
        }

        // Source location
        File sourceDir = new File(getSourceLocation().getFile());

        // Delete file/filesystem resources related to md, and then
        // remove it from contents.
        String moduleName = mai.getName();
        Version moduleVersion = mai.getVersion();
        String modulePlatform = mai.getPlatform();
        String moduleArch = mai.getArch();

        // This is the directory which contains MODULE.METADATA and JAM file.
        //
        // XXX moduleDir may not be right if the deployers have configured
        // a custom path in the repository-metadata.xml before. Will fix.
        File moduleDir = new File(sourceDir, getFilePath(moduleName,
                                            moduleVersion, modulePlatform,
                                            moduleArch));
        verifyExistence(moduleDir);


        //
        // A module archive could be platform neutral or platform specific,
        // and it resides under the same top directory for a given module
        // name and version:
        //
        // <source location>/<module-name>/<module-version>/
        // <source location>/<module-name>/<module-version>/<platform>-<arch>/
        //
        // Thus, we cannot just blow away the directory because there
        // could be a platform specific subdirectory for other platform
        // specific module archives.

        // jam name has no file extension
        String jamName = JamUtils.getJamFilename(moduleName, moduleVersion,
                                                 modulePlatform, moduleArch);

        File packGzJamFile = new File(moduleDir, jamName + ".jam.pack.gz");
        File jamFile = new File(moduleDir, jamName + ".jam");
        File metadataFile = new File(moduleDir, JamUtils.MODULE_METADATA);
        File packGzJamFileToRemove = null;
        File jamFileToRemove = null;
        File metadataFileToRemove = null;

        /*
         * Presume that ability to rename implies ability to remove:
         * rename the files to be deleted; if that succeeds then
         * delete them.  If any rename or deletion fails, undo
         * whatever can be undone.
         */
        try {
            if (metadataFile.exists()) {
                metadataFileToRemove = rename(metadataFile);
            }
            if (jamFile.exists()) {
                jamFileToRemove = rename(jamFile);
            }
            if (packGzJamFile.exists()) {
                packGzJamFileToRemove = rename(packGzJamFile);
            }

            // Updated the repository metadata without the specified
            // module archive.
            writeRepositoryMetadata(mai, false);

            if (metadataFileToRemove != null) {
                metadataFileToRemove.delete();
                metadataFileToRemove = null;
            }
            if (jamFileToRemove != null) {
                jamFileToRemove.delete();
                jamFileToRemove = null;
            }
            if (packGzJamFileToRemove != null) {
                packGzJamFileToRemove.delete();
                packGzJamFileToRemove = null;
            }

            // Only when there is no other file in the directory, blow the directory away
            File[] files = moduleDir.listFiles();
            if (files == null || files.length == 0)  {
                JamUtils.recursiveDelete(moduleDir);
            }
        } catch (IOException ex) {
            if (metadataFileToRemove != null) {
                rename(metadataFileToRemove, metadataFile);
            }
            if (jamFileToRemove != null) {
                rename(jamFileToRemove, jamFile);
            }
            if (packGzJamFileToRemove != null) {
                rename(packGzJamFileToRemove, packGzJamFile);
            }

            writeRepositoryMetadata(mai, true);
            throw ex;
        }

        // Remove the module archive and its corresponding module definition
        // from the internal data structures
        removeModuleArchiveInternal(mai);

        return true;
    }

    /**
     * Reload all module archives.
     */
    protected void doReload() throws IOException {
        try {
            /**
             * Since the repository-metadata.xml does not contain any
             * modification date information about the module metadata
             * and the .jam/.jam.pack.gz file, it is rather complicated
             * to detect actual change in individual module. For
             * simplicity, this implementation would simply drop all the
             * existing modules, and reload all module archives from the
             * codebase again.
             *
             * XXX: We should check the timestamp of the repository
             * metadata and only reload if it has been modified. Also, for
             * each MODULE.METADATA which is downloaded via a
             * URLConnection, one can getLastModified(), and we could use
             * the information to determine if a module has been updated.
             */
            URL repoMD = new URL(canonicalizedCodebase + "repository-metadata.xml");
            Set<ModuleInfo> moduleInfoSet = MetadataXMLReader.read(repoMD);

            // Uninstall all existing module archives and module definitions
            for (ModuleArchiveInfo mai : new ArrayList<ModuleArchiveInfo>(moduleArchiveInfos)) {
                removeModuleArchiveInternal(mai);
            }

            moduleDefs.clear();
            moduleArchiveInfos.clear();
            contentMapping.clear();

            // Initializes the internal data structures based on the module
            // info set again.
            doInitialize(moduleInfoSet);

            for (ModuleArchiveInfo mai : moduleArchiveInfos) {
                RepositoryEvent evt = new RepositoryEvent(
                    this, RepositoryEvent.Type.MODULE_INSTALLED, mai);
                processEvent(evt);
            }

            // Reconstructs module definitions from the module archives if
            // necessary.
            reconstructModuleDefinitionsIfNecessary(platform, arch);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(
                "Unable to reload the repository", ex);
        }
    }

    @Override
    protected void doShutdown() throws IOException {
        // Nothing specific to do during shutdown. No-op.
    }

    /**
     * @return true if this repository is read-only, which is the case if it
     * was created with any other than a file: URL.
     */
    @Override
    public boolean isReadOnly() {
        return (getSourceLocation().getProtocol().equals("file") == false);
    }

    /**
     * @return true if this repository supports reload.
     */
    @Override
    public boolean supportsReload() {
        return true;
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
                    "Cannot update repository metadata file for "
                        + mai.getName()
                        + ": cannot create temporary repository metadata file "
                        + tmpRepoMDFile);
            }
        }
        RepoMDWriter writer = new RepoMDWriter(tmpRepoMDFile);
        writer.begin();
        for (ModuleArchiveInfo m : moduleArchiveInfos) {
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
                "Cannot update repository metadata file for "
                    + mai.getName()
                    + ": failure while writing temporary repository metadata file "
                    + tmpRepoMDFile);
        }

        // Rename the repository-metadata.xml file to repository-metadata.xml.prev
        // repository-metadata.xml may not exist if the URLRepository is new and
        // no repository-metadata.xml has been created yet.
        if (repoMDFile.exists()) {
            File prev = new File(repoMDFile.getCanonicalPath() + ".prev");
            prev.delete();
            if (!repoMDFile.renameTo(prev)) {
                throw new IOException(
                    "Cannot update repository metadata file for "
                        + mai.getName()
                        + ": cannot rename " + repoMDFile + " to " + prev);
            }
            prev.deleteOnExit();
        }


        if (!tmpRepoMDFile.renameTo(repoMDFile)) {
            throw new IOException(
                "Cannot update repository metadata file for "
                    + mai.getName()
                    + ": cannot create updated repository-metadata.xml file"
                    + " by renaming " + tmpRepoMDFile + " to " + repoMDFile);
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
            if (!mai.isPlatformArchNeutral()) {
                output("<platform-binding>");
                indent++;
                output("<platform>" + mai.getPlatform() + "</platform>");
                output("<arch>" + mai.getArch() + "</arch>");
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
     * @throws IOException if file {@code f} doesn't exist
     */
    private void verifyExistence(File f) throws IOException {
        if (!f.exists()) {
            throw new FileNotFoundException(
                "File not found: " + f.getAbsolutePath());
        }
    }

    /**
     * @return file {@code prev} renamed by adding a time-based extension
     */
    private File rename(File prev) throws IOException {
        File next = new File(prev.getCanonicalPath() + "."
                                + System.currentTimeMillis());
        if (!prev.renameTo(next)) {
            throw new IOException(
                "Could not rename file for deletion: "
                    + prev.getCanonicalPath());
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
     * Returns file path for a module under codebase, based on module name,
     * version, platform and architecture.
     *
     * @param name module name
     * @param version module version
     * @param platform target platform
     * @param arch target architecture
     * @return file path
     */
    private static String getFilePath(String name, Version version,
                                      String platform, String arch) {
        return name + File.separator + version
               + ((platform == null) ? "" :
                    File.separator + platform + "-" + arch);
    }

    protected void assertValidDirs() throws IOException {
        if (getSourceLocation().getProtocol().equals("file")) {
            File sourceDirectory = JamUtils.getFile(getSourceLocation());
            if (sourceDirectory.exists() == false
                || sourceDirectory.isDirectory() == false) {
                if (sourceLocMustExist) {
                    missingDir("source", sourceDirectory);
                }
            }
        }
    }

    private void missingDir(String type, File dir) throws IOException {
        throw new FileNotFoundException(
            dir + " does not exist or is not a directory");
    }
}
