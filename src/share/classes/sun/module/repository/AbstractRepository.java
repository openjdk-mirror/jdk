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

package sun.module.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.module.ModuleArchiveInfo;
import java.module.ModuleDefinition;
import java.module.ModuleFormatException;
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
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sun.module.JamUtils;
import sun.module.repository.cache.Cache;
import sun.module.repository.cache.ModuleDefInfo;

/**
 * A common base class for LocalRepository and URLRepository.
 * <p>
 * @see java.module.ModuleArchiveInfo
 * @see java.module.ModuleDefinition
 * @see java.module.Query
 * @see java.module.Repository
 * @since 1.7
 */
abstract class AbstractRepository extends Repository {

    /**
     * Internal data structures for the repository.
     */
    protected final List<ModuleDefinition> moduleDefs =
                new ArrayList<ModuleDefinition>();
    protected final List<ModuleArchiveInfo> moduleArchiveInfos =
                new ArrayList<ModuleArchiveInfo>();
    protected final Map<String, Map<ModuleArchiveInfo, ModuleDefinition> > contentMapping =
                new HashMap<String, Map<ModuleArchiveInfo, ModuleDefinition> >();

    /** True if this repository has been initialized but not yet shutdown. */
    private boolean active;

    /** True if this repository has been shutdown. */
    private boolean shutdown;

    /** Repository cache. */
    protected Cache repositoryCache = null;

    /** Default configuration */
    protected static final Map<String, String> DEFAULT_CONFIG = Collections.emptyMap();

    /**
     * Creates a new <code>AbstractRepository</code> instance, and initializes it
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
    protected AbstractRepository(Repository parent, String name,
            URL source, Map<String, String> config) throws IOException {
        super(parent, name, source, ModuleSystem.getDefault());
        initialize(config);
    }

    //
    // Template methods to be implemented in the subclass.
    //

    protected abstract void doInitialize(final Map<String, String> config) throws IOException;
    protected abstract ModuleArchiveInfo doInstall(URL url) throws IOException;
    protected abstract boolean doUninstall(ModuleArchiveInfo mai) throws IOException;
    protected abstract void doReload() throws IOException;
    protected abstract void doShutdown() throws IOException;
    @Override
    public abstract boolean isReadOnly();
    @Override
    public abstract boolean supportsReload();
    protected abstract void assertValidDirs() throws IOException;

    //
    // Extend Repository
    //

    @Override
    public final void initialize() throws IOException {
        initialize(DEFAULT_CONFIG);
    }

    /**
     * Initializes the repository instance using the supplied configuration.
     * <p>
     * @param config Map of configuration names to their values
     * @throws IOException if the repository cannot be initialized
     */
    private final synchronized void initialize(final Map<String, String> config)
                                            throws IOException {
        if (active) {
            return;             // initialize only once
        }
        if (shutdown) {
            throw new IllegalStateException("Repository is already shutdown");
        }
        if (config == null) {
            throw new NullPointerException("config must not be null");
        }

        try {
            // Initialize the repository under doPrivileged()
            AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                public Boolean run() throws Exception {
                    doInitialize(config);
                    return Boolean.TRUE;
                }
            });
        } catch (PrivilegedActionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw new IOException("Unexpected exception has occurred", e);
            }
        }

        active = true;

        // Send REPOSITORY_INITIALIZED event
        RepositoryEvent evt = new RepositoryEvent(this,
                                    RepositoryEvent.Type.REPOSITORY_INITIALIZED);
        processEvent(evt);
    }

    @Override
    public final synchronized ModuleArchiveInfo install(final URL url)
                                                    throws IOException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("installModuleArchive"));
        }

        if (url == null)  {
            throw new NullPointerException("url must not be null");
        }

        assertActive();
        assertNotReadOnly();
        assertValidDirs();

        // Checks to see if the file to be installed is one of the file format
        // supported.
        if (!(url.getFile().endsWith(".jam")
              || url.getFile().endsWith(".jar")
              || url.getFile().endsWith(".jam.pack.gz"))) {
            throw new ModuleFormatException(
                "Only file format with .jam, .jar, or .jam.pack.gz extension is supported");
        }

        try {
            // Install the module archive under doPrivileged()
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<ModuleArchiveInfo>() {
                    public ModuleArchiveInfo run() throws Exception {
                        return doInstall(url);
                    }
                });
        } catch (PrivilegedActionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else if (e.getCause() instanceof IllegalStateException) {
                throw (IllegalStateException) e.getCause();
            } else {
                throw new IOException("Unexpected exception has occurred", e);
            }
        }
    }

    @Override
    public final synchronized boolean uninstall(final ModuleArchiveInfo mai)
                                            throws IOException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("uninstallModuleArchive"));
        }

        if (mai == null)  {
            throw new NullPointerException("mai must not be null");
        }

        assertActive();
        assertNotReadOnly();
        assertValidDirs();

        try {
            // Uninstall the module archive under doPrivileged()
            Boolean b = AccessController.doPrivileged(
                new PrivilegedExceptionAction<Boolean>() {
                    public Boolean run() throws Exception {
                        return doUninstall(mai);
                    }
                });
            return b.booleanValue();
        } catch (PrivilegedActionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else if (e.getCause() instanceof IllegalStateException) {
                throw (IllegalStateException) e.getCause();
            } else {
                throw new IOException("Unexpected exception has occurred", e);
            }
        }
    }

    /**
     * Put the module archive into the repository cache, cook it, and update
     * the internal data structure to reflect the change.
     *
     * @param file module archive to be installed.
     * @return module archive information
     * @param IOException if there is an I/O exception occurs.
     */
    protected final ModuleArchiveInfo addModuleArchiveInternal(File file)
                                        throws IOException {
        // Put the jam file into the repository cache and cook it
        ModuleDefInfo mdInfo = repositoryCache.getModuleDefInfo(file);

        // Constructs a module archive info
        ModuleArchiveInfo mai  = new ModuleArchiveInfo(
            this, mdInfo.getName(), mdInfo.getVersion(),
            mdInfo.getPlatform(), mdInfo.getArch(),
            file.getAbsolutePath(), file.lastModified());

        // Checks if a module definition already exists for a given module
        // name and version (e.g. platform neutral vs platform specific module).
        String key = mai.getName() + mai.getVersion();
        Map<ModuleArchiveInfo, ModuleDefinition> value = contentMapping.get(key);
        if (value == null) {
            // No module definition exists, and we should create one if the
            // module archive supports the running platform and architecture.
            if (mdInfo.supportsRunningPlatformArch()) {
                // Constructs a module definition
                ModuleDefinition md = Modules.newJamModuleDefinition(
                                            mdInfo.getMetadataBytes(),
                                            mdInfo.getModuleDefinitionContent(),
                                            this, true);
                // Add the module definition into the internal data structure
                moduleDefs.add(md);
                value = new HashMap<ModuleArchiveInfo, ModuleDefinition>();
                value.put(mai, md);
                contentMapping.put(key, value);
            }
        } else {

            // A module definition already exists for a given name and version
            // (e.g. platform neutral vs platform specific module).

            // XXX: do we replace the existing module definition with a new one
            // if the newly installed module archive provides better
            // platform binding?
            //
            // e.g. a platform neutral module is already installed and in use
            // but now we just install a windows-x86 specific module (assuming
            // we're running on Windows as well), should we swap the module
            // definition of the platform neutral module with that of a newly
            // installed one?
            //
            // For simplicity, the answer is no. Otherwise, the behavior could
            // be very confusing and problematic.
        }

        // Adds the module archive into the internal data structure
        moduleArchiveInfos.add(mai);

        // Send MODULE_INSTALLED event
        RepositoryEvent evt = new RepositoryEvent(this,
                                    RepositoryEvent.Type.MODULE_INSTALLED, mai);
        processEvent(evt);

        return mai;
    }

    /**
     * Remove the module archive and the corresponding module definition
     * from the internal data structures so this repository won't
     * recognize it anymore.
     *
     * @param mai module archive information that represents the module archive
     *        to be removed.
     */
    protected final void removeModuleArchiveInternal(ModuleArchiveInfo mai) {
        String key = mai.getName() + mai.getVersion();
        Map<ModuleArchiveInfo, ModuleDefinition> value = contentMapping.get(key);
        if (value != null) {
            // Check if a module definition has been created for the module
            // archive info
            ModuleDefinition md = value.get(mai);

            if (md == null) {
                // Module definition could be null if a platform neutral or
                // platform-specific module with the same name and version
                // already exists, but it's not the module archive that is
                // being removed. In this case, there is no module definition
                // to be removed from the internal data structure.
            } else {
                // Removes the module archive and module definition mapping
                // from internal data structure.
                moduleDefs.remove(md);
                contentMapping.remove(key);

                try {
                    // Disables the module definition in the module system.
                    getModuleSystem().disableModuleDefinition(md);
                } catch (UnsupportedOperationException uoe) {
                    // no-op
                } catch (IllegalStateException ise) {
                    // no-op
                }
                try {
                    // Releases any module instance corresponding to the module
                    // definition in the module system
                    getModuleSystem().releaseModule(md);
                } catch (UnsupportedOperationException uoe) {
                    // no-op
                }

                // It is certainly possible that the repository may have another
                // module archive for the same module name/version. e.g. a platform
                // specific module is uninstalled but there exists a platform
                // neutral module in the repository. In this case, do we recreate
                // the module definition for the platform neutral module and use
                // it in the repository?
                //
                // For simplicity, the answer is no. If the user uninstalls a
                // platform specific module from the repository, he/she would
                // expect that no module definition for the given module
                // name/version would be returned from the repository if it is
                // searched through find(). If the repository returns a
                // module definition of the platform neutral module instead,
                // the behavior could be very confusing and problematic.
            }
        }

        // Removes the module archive from the internal data structure
        moduleArchiveInfos.remove(mai);

        // Send MODULE_UNINSTALLED event
        RepositoryEvent evt = new RepositoryEvent(this,
                                    RepositoryEvent.Type.MODULE_UNINSTALLED, mai);
        processEvent(evt);
    }

    /**
     * Constructs the module definition from the module archives based on the
     * current platform and architecture, and updates the internal data
     * structure.
     */
    protected final void constructModuleDefinitions(Map<ModuleArchiveInfo,
                                                    ModuleDefInfo> mdInfoMap)
                                                    throws IOException {
        constructModuleDefinitions(mdInfoMap,
                                   RepositoryUtils.getPlatform(),
                                   RepositoryUtils.getArch());
    }

    /**
     * Constructs the module definition from the module archives based on the
     * specified platform and architecture, and updates the internal data
     * structure.
     */
    protected final void constructModuleDefinitions(Map<ModuleArchiveInfo,
                                                    ModuleDefInfo> mdInfoMap,
                                                    String platform, String arch)
                                                    throws IOException {
        //
        // It is certainly possible that the source directory may contain more
        // than one module with the same name and same version in some
        // repository implementations, e.g. in the case of LocalRepository,
        // duplicate module with different JAM filename, or platform neutral
        // module vs platform specific module.
        //
        // The list() method would return all the module archives. However, for
        // the find() method, it is important to filter out the unappropriate
        // module archives when constructing module definitions, so for a given
        // name and version, there is at most one module definition in the
        // repository.
        //
        List<ModuleArchiveInfo> appropriateModuleArchiveInfos =
                    getAppropriateModuleArchiveInfos(moduleArchiveInfos, platform, arch);

        //
        // Iterate the list of appropriate module archive, and creates
        // module definition and updates the internal data struture.
        //
        for (ModuleArchiveInfo mai : appropriateModuleArchiveInfos) {
            String key = mai.getName() + mai.getVersion();

            // Looks up the module definition info related to the preferred module archive.
            ModuleDefInfo mdInfo = mdInfoMap.get(mai);

            // Constructs a module definition from the module archive.
            ModuleDefinition md = Modules.newJamModuleDefinition(
                                    mdInfo.getMetadataBytes(),
                                    mdInfo.getModuleDefinitionContent(),
                                    this, true);

            // Updates the internal data structures so the repository would
            // recognize this module archive info and the module definition.
            HashMap<ModuleArchiveInfo, ModuleDefinition> value =
                    new HashMap<ModuleArchiveInfo, ModuleDefinition>();
            value.put(mai, md);
            contentMapping.put(key, value);
            moduleDefs.add(md);
        }
    }

    /**
     * Reconstructs the module definition from the module archives if necessary
     * based on the current platform and architecture, and updates the internal
     * data structure.
     */
    protected final void reconstructModuleDefinitionsIfNecessary()
                                throws IOException {
        reconstructModuleDefinitionsIfNecessary(RepositoryUtils.getPlatform(),
                                                RepositoryUtils.getArch());
    }

    /**
     * Reconstructs the module definition from the module archives if necessary
     * based on the specified platform and architecture, and updates the internal
     * data structure.
     */
    protected final void reconstructModuleDefinitionsIfNecessary(String platform, String arch)
                                throws IOException {
        List<ModuleArchiveInfo> appropriateModuleArchiveInfos =
                    getAppropriateModuleArchiveInfos(moduleArchiveInfos, platform, arch);

        for (ModuleArchiveInfo mai : appropriateModuleArchiveInfos) {
            String key = mai.getName() + mai.getVersion();
            // If there is no module definition for the appropriate module
            // archive.
            if (contentMapping.get(key) == null) {
                // Put the jam file into the repository cache and cook it
                ModuleDefInfo mdInfo = repositoryCache.getModuleDefInfo(
                                                new File(mai.getFileName()));

                // Constructs a module definition
                ModuleDefinition md = Modules.newJamModuleDefinition(
                                            mdInfo.getMetadataBytes(),
                                            mdInfo.getModuleDefinitionContent(),
                                            this, true);

                // Add the module definition into the internal data structure
                moduleDefs.add(md);
                HashMap<ModuleArchiveInfo, ModuleDefinition> value =
                            new HashMap<ModuleArchiveInfo, ModuleDefinition>();
                value.put(mai, md);
                contentMapping.put(key, value);
            }
        }
    }

    /**
     * Returns a unmodifiable list of module archive info that are appropriate
     * to be used for the specified platform and architecture. If there are
     * duplicate modules (i.e. same name, version, and platform binding), the
     * duplicate is removed in the result.
     *
     * @param moduleArchiveInfos a list of module archive info
     * @param p platform
     * @param a architecture
     */
    private static List<ModuleArchiveInfo> getAppropriateModuleArchiveInfos(
                                             Collection<ModuleArchiveInfo> moduleArchiveInfos,
                                             String p, String a) {
        Map<String, ModuleArchiveInfo> preferredMap = new LinkedHashMap<String, ModuleArchiveInfo>();

        for (ModuleArchiveInfo mai : moduleArchiveInfos) {
            String key = mai.getName() + mai.getVersion();
            if (mai.isPlatformArchNeutral()) {
                // The module archive is platform neutral

                // If no platform specific module exists in the preferred map,
                // use the platform neutral module.
                if (preferredMap.get(key) == null) {
                    preferredMap.put(key, mai);
                }
            } else if (p.equals(mai.getPlatform())
                       && a.equals(mai.getArch()))  {
                // The module archive is platform specific to the specified
                // platform/arch.
                ModuleArchiveInfo mai2 = preferredMap.get(key);

                // If no module archive exists in the prefered map for the given
                // module name/version, use this module archive. Or if module
                // archive exists in the preferred map but it is platform
                // neutral, replace it with this module archive.
                if (mai2 == null || mai2.isPlatformArchNeutral()) {
                    preferredMap.put(key, mai);
                } else {
                    // A module archive already exists in the preferred map but
                    // it is also platform specific to the specified
                    // platform/arch. In this case, this is a duplicate -
                    // do nothing.
                }
            } else {
                // The module archive is platform specified to other platform/
                // arch. No-op
            }
        }

        return Collections.unmodifiableList(
                    new ArrayList<ModuleArchiveInfo>(preferredMap.values()));
    }

    @Override
    public final synchronized List<ModuleDefinition> findModuleDefinitions(Query constraint) {
        assertActive();
        List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();

        if (constraint == Query.ANY) {
            result = moduleDefs;
        } else {
            for (ModuleDefinition md : moduleDefs) {
                if (md != null && constraint.match(md)) {
                    result.add(md);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public final synchronized List<ModuleArchiveInfo> list() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("listModuleArchive"));
        }
        assertActive();
        return Collections.unmodifiableList(moduleArchiveInfos);
    }

    @Override
    public final synchronized void reload() throws IOException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("reloadRepository"));
        }
        assertActive();

        if (!supportsReload()) {
            throw new UnsupportedOperationException("Repository does not support reload");
        }

        try {
            // Reload the module archives under doPrivileged()
            AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                public Boolean run() throws Exception {
                    doReload();
                    return Boolean.TRUE;
                }
            });
        } catch (PrivilegedActionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else if (e.getCause() instanceof IllegalStateException) {
                throw (IllegalStateException) e.getCause();
            } else {
                throw new IOException("Unexpected exception has occurred", e);
            }
        }
    }

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

        try {
            // shutdown repository under doPrivileged()
            AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                public Boolean run() throws Exception {
                    doShutdown();

                    // Iterates through each module definition that the
                    // repository is aware of
                    for (ModuleDefinition md : moduleDefs) {
                        try {
                            // Disables the module definition in the module system.
                            getModuleSystem().disableModuleDefinition(md);
                        } catch (UnsupportedOperationException uoe) {
                            // no-op
                        } catch (IllegalStateException ise) {
                            // no-op
                        }
                        try {
                            // Releases any module instance corresponding to the module definition
                            // in the module system
                            getModuleSystem().releaseModule(md);
                        } catch (UnsupportedOperationException uoe) {
                            // no-op
                        }
                    }

                    return Boolean.TRUE;
                }
            });
        } catch (PrivilegedActionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else if (e.getCause() instanceof IllegalStateException) {
                throw (IllegalStateException) e.getCause();
            } else {
                throw new IOException("Unexpected exception has occurred", e);
            }
        }

        moduleDefs.clear();
        moduleArchiveInfos.clear();
        contentMapping.clear();

        // Shutdown repository cache
        if (repositoryCache != null) {
            repositoryCache.shutdown();
        }

        active = false;
        shutdown = true;

        // Send REPOSITORY_SHUTDOWN event
        RepositoryEvent evt = new RepositoryEvent(this,
                                    RepositoryEvent.Type.REPOSITORY_SHUTDOWN);
        processEvent(evt);
    }

    @Override
    public final boolean isActive() {
        return active;
    }

    private void assertActive() throws IllegalStateException {
         if (!isActive()) {
             throw new IllegalStateException("Repository is not active.");
        }
    }

    private void assertNotActive() throws IllegalStateException {
         if (isActive()) {
             throw new IllegalStateException("Repository is active.");
        }
    }

    private void assertNotReadOnly() throws IllegalStateException {
        if (isReadOnly()) {
            throw new UnsupportedOperationException("Repository is read-only.");
        }
    }
}
