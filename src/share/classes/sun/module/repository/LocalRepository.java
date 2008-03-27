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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.module.ModuleArchiveInfo;
import java.module.ModuleDefinition;
import java.module.ModuleSystem;
import java.module.ModuleSystemPermission;
import java.module.Query;
import java.module.Repository;
import java.module.RepositoryEvent;
import java.module.Version;
import java.module.annotation.PlatformBinding;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sun.module.JamUtils;

/**
 * A repository for module definitions stored on the file system.
 * <p>
 * When the repository is initialized, the source location is interpreted by
 * the <code>LocalRepository</code> instance as a directory where the module
 * definitions are stored in the repository interchange directory.
 *
 * @see java.module.ModuleArchiveInfo
 * @see java.module.Repository
 * @since 1.7
 */
public final class LocalRepository extends Repository {
    /** Prefix of all properties use to configure this repository. */
    private static final String PROPERTY_PREFIX = "sun.module.repository.LocalRepository.";

    /**
     * True iff the sourceDir is writable.  Note that this can change upon
     * {@link #reload()}.
     */
    private boolean readOnly;

    /**
     * Directory in which JAM files are installed, derived from the source
     * location given in constructors.
     */
    private File sourceDir;

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
     * True if all modules must be uninstalled during execution of {@code
     * shutdown()}.
     */
    private boolean uninstallOnShutdown;

    private static final String UNINSTALL_ON_SHUTDOWN_KEY =
        PROPERTY_PREFIX + "uninstallOnShutdown";

    /**
     * Directory containing directories for JAM expansion and creation of any
     * other artifacts for this repository, unless their locations are
     * specified by client via properties.
     */
    private File baseDir;

    /**
     * Directory into which embedded jars and native libraries in a JAM are
     * expanded.  Not created in the filesystem unless actually needed.
     */
    private File expansionDir;

    /**
     * Name of expansion directory based on system property and config in
     * {@link #initialize(Map<String, String>)}.
     */
    private String expansionDirName;

    /**
     * Directory name corresponding to {@code EXPANSION_DIR_KEY} as obtained
     * from System properties.
     */
    private static final String sysPropExpansionDirName;

    /**
     * Property name to configure the directory into which native libraries
     * and embedded JAR files are expanded from a JAM file.
     */
    private static final String EXPANSION_DIR_KEY =
        PROPERTY_PREFIX + "expansionDirectory";

    static {
        sysPropSourceLocMustExist = RepositoryUtils.getProperty(SOURCE_LOC_MUST_EXIST_KEY);
        sysPropExpansionDirName = RepositoryUtils.getProperty(EXPANSION_DIR_KEY);
    }

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

    /** JAM files that have been installed. */
    private final List<File> jams = new ArrayList<File>();

    /** True if this repository has been initialized but not yet shutdown. */
    private boolean active;

    /** True if this repository has been shutdown. */
    private boolean shutdown;

    private static String platform = RepositoryUtils.getPlatform();

    private static String arch = RepositoryUtils.getArch();

    private static final Map<String, String> DEFAULT_CONFIG = Collections.emptyMap();

    /**
     * Creates a new <code>LocalRepository</code> instance.
     * Initializes the repository.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's <code>checkPermission</code> method with a
     * <code>ModuleSystemPermission("createRepository")</code> permission to
     * ensure it's ok to create a repository.
     *
     * @param parent the parent repository for delegation.
     * @param name the repository name.
     * @param source the source location.
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to create a new
     *         instance of repository.
     * @throws java.io.IOException if the repository cannot be initialized.
     */
    public LocalRepository(Repository parent, String name, URL source) throws IOException {
        this(parent, name, source, DEFAULT_CONFIG);
    }

    /**
     * Creates a new <code>LocalRepository</code> instance using the
     * <code>Repository</code> returned by the method
     * <code>getSystemRepository()</code> as the parent repository.
     * Initializes the repository.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's <code>checkPermission</code> method with a
     * <code>ModuleSystemPermission("createRepository")</code> permission to
     * ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param source the source location.
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to create a new
     *         instance of repository.
     * @throws java.io.IOException if the repository cannot be initialized.
     */
    public LocalRepository(String name, URL source) throws IOException {
        this(getSystemRepository(), name, source);
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
     * @param parent the parent repository for delegation.
     * @param name the repository name.
     * @param source the source location.
     * @param config Map of configuration names to their values
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to create a new
     *         instance of repository.
     * @throws java.io.IOException if the repository cannot be initialized.
     */
    public LocalRepository(Repository parent, String name,
            URL source, Map<String, String> config) throws IOException {
        super(parent, name, source, ModuleSystem.getDefault());
        timestamp = System.currentTimeMillis();
        initialize(config);
    }

    /**
     * Creates a new <code>LocalRepository</code> instance using the
     * <code>Repository</code> returned by the method
     * <code>getSystemRepository()</code> as the parent repository, and
     * initializes it using information from the given {@code config}.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's <code>checkPermission</code> method with a
     * <code>ModuleSystemPermission("createRepository")</code> permission to
     * ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param source the source location.
     * @param config Map of configuration names to their values
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to create a new
     *         instance of repository.
     * @throws java.io.IOException if the repository cannot be initialized.
     */
    public LocalRepository(String name, URL source, Map<String, String> config) throws IOException {
        this(getSystemRepository(), name, source, config);
    }

    //
    // Extend Repository
    //

    @Override
    public void initialize() throws IOException {
        initialize(DEFAULT_CONFIG);
    }

    /**
     * @see Repository#install(URL u)
     * @return a {code ModuleArchiveInfo} corresponding to the newly-installed
     * module, or null if the module was already installed.
     * @throws IOException if given URL names a file that does not exist.
     *         directory.
     */
    @Override
    public synchronized ModuleArchiveInfo install(URL u) throws IOException {
        assertActive();
        assertNotReadOnly();
        assertValidDirs();

        File f = JamUtils.getFile(u);
        File dest = new File(sourceDir + File.separator + f.getName());
        if (dest.exists()) {
            throw new IllegalStateException(
                msg("Cannot overwrite " + f.getName()
                    + " in repository's source directory"));
        }
        JamUtils.saveStreamAsFile(new FileInputStream(f), dest);

        ModuleArchiveInfo rc = installInternal(dest);
        return rc;
    }

    @Override
    public synchronized boolean uninstall(ModuleArchiveInfo mai) throws IOException {
        assertActive();
        assertNotReadOnly();
        assertValidDirs();

        if (!contents.contains(mai)) {
            return false;
        }

        // Remove any legacy JAR files for this module
        File legacyJarDir = RepositoryUtils.getLegacyJarDir(expansionDir, mai.getName(), mai.getVersion(), mai.getPlatform(), mai.getArchitecture());
        if (legacyJarDir.isDirectory()) {
            // XXX Remove the Windows-specificity once disableModuleDefinition is implemented
            if (!platform.startsWith("windows")) {
                if (!JamUtils.recursiveDelete(legacyJarDir)) {
                    throw new IOException(
                        msg("Could not delete expansion directory " + legacyJarDir));
                }
            }
        } else if (legacyJarDir.exists()) {
            throw new IllegalStateException(
                msg("File " + legacyJarDir.getCanonicalPath()
                    + " is expected to be a directory but is not"));
        }

        ModuleDefinition md = contents.get(mai);
        // XXX Uncomment below disableModuleDefinition is implemented
        //if (md != null) {
        //    getModuleSystem().disableModuleDefinition(md);
        //}

        // Remove the module file if it is the same one that was installed,
        // as determined by timestamp.  Don't remove if the timestamp is
        // different, as that could represent file copied over the installed
        // file.
        // XXX Remove the Windows-specificity once disableModuleDefinition is implemented
        if (!platform.startsWith("windows")) {
            File f = new File(mai.getFileName());
            if (f.lastModified() == mai.getLastModified() && f.isFile() && !f.delete()) {
                throw new IOException(
                    msg("Could not delete module for " + mai.getName()
                        + "from the repository"));
            }
        }

        // Remove from cache & contents
        modDefCache.remove(md);
        boolean rc = contents.remove(mai);

        if (rc) {
            // Send MODULE_UNINSTALLED event
            RepositoryEvent evt = new RepositoryEvent(this, RepositoryEvent.Type.MODULE_UNINSTALLED, mai);
            processEvent(evt);
        }
        return rc;
    }

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
     * Compares the cache with the contents of the source directory.  Modules
     * that are in both places are not affected.  Modules in the cache but not
     * source directory are uninstalled.  Modules in the source directory but
     * not the cache are installed.
     */
    @Override
    public synchronized void reload() throws IOException {
        assertActive();
        readOnly = (sourceDir.canWrite() == false);
        initializeCache();

        // Build a list of modules to uninstall, and of modules currently
        // installed that won't be uninstalled by this reload.
        List<ModuleArchiveInfo> uninstallCandidates = new ArrayList<ModuleArchiveInfo>();
        Set<File> currentModules = new HashSet<File>();
        for (ModuleArchiveInfo mai : contents.getModuleArchiveInfos()) {
            File f = new File(mai.getFileName());
            long modTime = mai.getLastModified();
            // Uninstall if source file is missing, or if it has been updated on disk.
            if (!f.isFile() || (modTime != 00 && f.lastModified() != modTime)) {
                uninstallCandidates.add(mai);
            } else {
                currentModules.add(f);
            }
        }

        // Uninstall modules for which there is no corresponding file in the
        // source directory
        for (ModuleArchiveInfo mai : uninstallCandidates) {
            uninstall(mai);
        }

        // Install modules that have a JAM in the source directory, but are
        // not in the cache.
        for (File f : sourceDir.listFiles(JamUtils.JAM_FILTER)) {
            if (!currentModules.contains(f)) {
                installInternal(f);
            }
        }
    }

    /**
     * Uninstalls all modules.  Removes repository-specific directories.
     *
     * @throws java.io.IOException if there's an error removing directories.
     */
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

        if (uninstallOnShutdown) {
            // Only remove what was installed
            for (ModuleArchiveInfo mai : contents.getModuleArchiveInfos()) {
                File f = new File(mai.getFileName());
                if (f.isFile()) {
                    f.delete();
                }
            }
        }

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
     * @return true if this repository is read-only; false otherwise
     */
    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * @return true
     */
    @Override
    public boolean isReloadSupported() {
        return true;
    }

    //
    // Implementation specific to LocalRepository
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
    private synchronized void initialize(Map<String, String> config) throws IOException {
        if (active) {
            return;             // initialize only once
        }
        if (shutdown) {
            throw new IllegalStateException(msg("Repository is shut down."));
        }
        if (config == null) {
            throw new NullPointerException(msg("Parameter 'config' cannot be null."));
        }

        if ("true".equalsIgnoreCase(sysPropSourceLocMustExist)) {
            sourceLocMustExist = true;
        } else {
            sourceLocMustExist = "true".equalsIgnoreCase(config.get(SOURCE_LOC_MUST_EXIST_KEY));
        }

        uninstallOnShutdown = "true".equalsIgnoreCase(config.get(UNINSTALL_ON_SHUTDOWN_KEY));

        expansionDirName = sysPropExpansionDirName != null
            ? sysPropExpansionDirName : config.get(EXPANSION_DIR_KEY);

        sourceDir = JamUtils.getFile(getSourceLocation());
        readOnly = (sourceDir.canWrite() == false);
        if (sourceDir.isDirectory()) {
            initializeCache();
            installJams();
        } else {
            if (sourceLocMustExist) {
                missingDir("source", sourceDir);
            }
        }

        active = true;

        // Send REPOSITORY_INITIALIZED event
        RepositoryEvent evt = new RepositoryEvent(this, RepositoryEvent.Type.REPOSITORY_INITIALIZED);
        processEvent(evt);
    }

    /**
     * Installs given JAM file into the repository.
     */
    private ModuleArchiveInfo installInternal(File file) throws IOException {
        if (file.isFile() == false) {
            throw new IOException(msg("File does not exist: " + file));
        }

        ModuleDefinition md = RepositoryUtils.createLocalModuleDefinition(
            this, file, expansionDir);

        ModuleArchiveInfo mai  = new ModuleArchiveInfo(
            this, md.getName(), md.getVersion(),
            platform, arch,
            file.getAbsolutePath(), file.lastModified());

        // Determine if the module definition matches the current platform platform
        PlatformBinding platformBinding = md.getAnnotation(PlatformBinding.class);

        if (RepositoryUtils.bindingMatches(platformBinding, platform, arch)) {
            modDefCache.add(md);
            contents.put(mai, md);
        } else {
            contents.put(mai, null);
        }

        if (mai != null) {
            // Send MODULE_INSTALLED event
            RepositoryEvent evt = new RepositoryEvent(this, RepositoryEvent.Type.MODULE_INSTALLED, mai);
            processEvent(evt);
        }

        return mai;
    }

    /**
     * Ensure that the required directories exist.
     */
    private void initializeCache() throws IOException {
        if (expansionDirName != null) {
            expansionDir = new File(expansionDirName,
                                    getName() + "-" + timestamp + "-expand");
        } else {
            if (baseDir == null) {
                baseDir = new File(JamUtils.createTempDir(), "LocalRepository");
                baseDir = new File(baseDir, getName() + "-" + timestamp);
            }
            expansionDir = new File(baseDir, "expand");
        }
        assertValidDirs();
    }

    /**
     * Removes the contents of the repository's metadata and download
     * directories.  Clears repository contents and list of loaded JAMs.
     */
    private void removeCache() throws IOException {
        if (baseDir != null) {
            JamUtils.recursiveDelete(baseDir);
        }

        if (expansionDir != null) {
            JamUtils.recursiveDelete(expansionDir);
        }

        baseDir = null;
        expansionDir = null;
        contents.clear();
        jams.clear();
    }

    /**
     * Install JAM files in the source dir that aren't already installed.
     */
    private void installJams() {
        File[] jamFiles = sourceDir.listFiles(JamUtils.JAM_JAR_FILTER);
        for (File jf : jamFiles) {
            if (!jams.contains(jf)) {
                // no need to retry if it does not work the first time
                jams.add(jf);
                try {
                    // XXX Log this action
                    installInternal(jf);
                } catch (Exception ex) {
                    // XXX log warning but otherwise ignore
                }
            }
        }
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
        if (sourceLocMustExist) {
            if (sourceDir == null || !sourceDir.isDirectory()) {
                missingDir("source", sourceDir);
            }
        }
        if (expansionDir != null
            && (expansionDir.exists() && !expansionDir.isDirectory())) {
            throw new IOException(
                msg("Specified expansion directory "
                    + expansionDir + " is not a directory"));
        }
    }

    private void missingDir(String type, File dir) throws IOException {
        throw new IOException(
            msg("Specified " + type + " directory "
                + dir + " does not exist or is not a directory"));
    }

    private String msg(String s) {
        return "LocalRepository: " + getName() + " at "
            + getSourceLocation().toExternalForm() + ": " + s;
    }
}
