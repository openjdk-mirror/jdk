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

import java.io.File;
import java.io.IOException;
import java.module.ModuleArchiveInfo;
import java.module.ModuleDefinition;
import java.module.ModuleFormatException;
import java.module.ModuleSystemPermission;
import java.module.Repository;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;

/**
 * A repository for OSGi bundles.
 *
 * This repository will auto-install all bundles in the given source location
 * when initialized.
 */
public class OSGiRepository extends Repository {
    private final Map<String, String> config;
    private Map<String, Map<ModuleArchiveInfo, OSGiModuleDefinition>> contentMapping =
        new HashMap<String, Map<ModuleArchiveInfo, OSGiModuleDefinition> >();

    private URI source;

    /**
     * Returns the OSGi repository.
     *
     * FIXME: initialize() is not called in the constructor to workaround
     * the bootstrapping issue which will be resolved later.
     */
    public OSGiRepository(String name, URI source, Repository parent,
                          Map<String, String> config) throws IOException {
        super(name, parent);
        this.source = source;
        this.config = config;
    }

    /**
     * Returns the source location of this {@code Repository}.
     *
     * @return the source location.
     */
    public final URI getSourceLocation()    {
        return source;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean supportsReload() {
        return true;
    }

    private static final String CONTAINER = "container";

    @Override
    protected List<ModuleArchiveInfo> doInitialize() throws IOException {
        // Start the OSGi runtime and initialize the map for installed bundles
        // and ModuleDefinitions
        OSGiRuntime.start(this.getSourceLocation(),
                          config.get(CONTAINER));
        Set<Bundle> bundles = OSGiRuntime.getInstalledBundles();
        List<ModuleArchiveInfo> moduleArchiveInfos = new ArrayList<ModuleArchiveInfo>();
        for (Bundle bundle : bundles) {
            moduleArchiveInfos.add(newModuleArchiveInfo(bundle));
            // add the OSGiModuleDefinition to the internal data structure
            addModuleDefinition(new OSGiModuleDefinition(this, bundle));
        }
        return moduleArchiveInfos;
    }

    private ModuleArchiveInfo newModuleArchiveInfo(Bundle bundle) {
        return new OSGiModuleArchiveInfo(this,
                                     bundle.getSymbolicName(),
                                     BundleManifestMapper.getVersion(bundle),
                                     bundle.getLocation(), /* XXX: is it a valid filename? */
                                     bundle.getLastModified());
    }

    @Override
    public final synchronized ModuleArchiveInfo install(final URI uri)
            throws IOException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("installModuleArchive"));
        }

        if (uri == null) {
            throw new NullPointerException("uri must not be null");
        }

        assertActive();
        assertNotReadOnly();

        // Checks to see if the file to be installed is one of the file format
        // supported.
        if (!uri.toURL().getFile().endsWith(".jar")) {
            throw new ModuleFormatException(
                    "Only file format with .jar extension is supported");
        }

        try {
            // Install the module archive under doPrivileged()
            return AccessController.doPrivileged(
                    new PrivilegedExceptionAction<ModuleArchiveInfo>() {

                        public ModuleArchiveInfo run() throws Exception {
                            return installBundle(uri);
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

    /**
     * Put the module archive into the repository cache, cook it, and update
     * the internal data structure to reflect the change.
     *
     * @param uri bundle to be installed.
     * @return module archive information
     * @param IOException if there is an I/O exception occurs.
     */
    private final ModuleArchiveInfo installBundle(URI uri)
                                        throws IOException {
        // Installs a bundle and constructs a module archive info
        Bundle bundle = OSGiRuntime.installBundle(uri);
        ModuleArchiveInfo mai = newModuleArchiveInfo(bundle);

        // Checks if a module definition already exists for a given module
        // name and version (e.g. platform neutral vs platform specific module).
        String key = mai.getName() + mai.getVersion();
        Map<ModuleArchiveInfo, OSGiModuleDefinition> value = contentMapping.get(key);
        if (value == null) {
            // No module definition exists, and we should create one if the
            // module archive supports the running platform and architecture.
            // XXX: need to check for platform and architecture
            value = new HashMap<ModuleArchiveInfo, OSGiModuleDefinition>();
            if (true) {
                // Constructs a module definition
                OSGiModuleDefinition md = new OSGiModuleDefinition(this, bundle);
                addModuleDefinition(md);
                value.put(mai, md);
            }
            contentMapping.put(key, value);
        } else {

            // A module definition already exists for a given name and version
            // (e.g. portable module vs platform specific module).

            // XXX: do we replace the existing module definition with a new one
            // if the newly installed module archive provides better
            // platform binding?
            //
            // e.g. a portable module is already installed and in use
            // but now we just install a windows-x86 specific module (assuming
            // we're running on Windows as well), should we swap the module
            // definition of the portable module with that of a newly
            // installed one?
            //
            // For simplicity, the answer is no. Otherwise, the behavior could
            // be very confusing and problematic.
        }
        addModuleArchiveInfo(mai);
        return mai;
    }

    @Override
    public final synchronized boolean uninstall(final ModuleArchiveInfo mai)
            throws IOException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("uninstallModuleArchive"));
        }

        if (mai == null) {
            throw new NullPointerException("mai must not be null");
        }

        assertActive();
        assertNotReadOnly();

        try {
            // Uninstall the module archive under doPrivileged()
            Boolean b = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Boolean>() {

                        public Boolean run() throws Exception {
                            return uninstallBundle(mai);
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

    private boolean uninstallBundle(ModuleArchiveInfo mai) throws IOException {
        // Checks if the module archive still exists.
        String key = mai.getName() + mai.getVersion();
        Map<ModuleArchiveInfo, OSGiModuleDefinition> value = contentMapping.get(key);
        if (value == null) {
            return false;
        }

        // TODO: assume it's a local jar file

        // Remove the module archive if it is the same one that was installed,
        // as determined by timestamp.  Don't remove if the timestamp is
        // different, as that could mean a file has been copied over the
        // installed file.
        File f = new File(mai.getFileName());

        // XXX: Bundle's last modification date is zero
        // Comment out the last modification date check for now.
        /*
        if (f.lastModified() != mai.getLastModified()) {
            throw new IOException(
                "Could not delete module archive " + mai.getFileName() +
                " because the modification date " + f.lastModified() +
                " was different than expected: " + mai.getLastModified());
        }
        */
        if (f.isFile() && !f.delete()) {
            throw new IOException(
                "Could not delete module archive: " + mai.getFileName());
        }

        // Check if a module definition has been created for the module
        // archive info
        OSGiModuleDefinition md = value.get(mai);
        if (md == null) {
            // Module definition could be null if a portable or
            // platform-specific module with the same name and version
            // already exists, but it's not the module archive that is
            // being removed. In this case, there is no module definition
            // to be removed from the internal data structure.
        } else {
            OSGiRuntime.uninstallBundle(md.getBundle());

            // Removes the module archive and module definition mapping
            // from internal data structure.
            removeModuleDefinition(md);
            contentMapping.remove(key);

            try {
                // Disables the module definition in the module system.
                md.getModuleSystem().disableModuleDefinition(md);
            } catch (UnsupportedOperationException uoe) {
                // no-op
            } catch (IllegalStateException ise) {
                // no-op
            }
            try {
                // Releases any module instance corresponding to the module
                // definition in the module system
                md.getModuleSystem().releaseModule(md);
            } catch (UnsupportedOperationException uoe) {
                // no-op
            }

            // It is certainly possible that the repository may have another
            // module archive for the same module name/version. e.g. a platform
            // specific module is uninstalled but there exists a platform
            // neutral module in the repository. In this case, do we recreate
            // the module definition for the portable module and use
            // it in the repository?
            //
            // For simplicity, the answer is no. If the user uninstalls a
            // platform specific module from the repository, he/she would
            // expect that no module definition for the given module
            // name/version would be returned from the repository if it is
            // searched through find(). If the repository returns a
            // module definition of the portable module instead,
            // the behavior could be very confusing and problematic.
        }

        // Removes the module archive from the internal data structure
        return removeModuleArchiveInfo(mai);
    }

    @Override
    public final synchronized List<ModuleArchiveInfo> list() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("listModuleArchive"));
        }
        assertActive();
        return getModuleArchiveInfos();
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

    private void doReload() throws IOException {
        // Build a list of modules to uninstall, and of modules currently
        // installed that won't be uninstalled by this reload.
        List<ModuleArchiveInfo> uninstallCandidates = new ArrayList<ModuleArchiveInfo>();
        Set<File> existingBundles = new HashSet<File>();
        for (ModuleArchiveInfo mai : list()) {
            File f = new File(mai.getFileName());
            long modTime = mai.getLastModified();
            // Uninstall if source file is missing, or if it has been updated on disk.
            if (!f.isFile() || (modTime != 0 && f.lastModified() != modTime)) {
                uninstallCandidates.add(mai);
            } else {
                existingBundles.add(f);
            }
        }

        // Remove modules from the internal data structures for which there
        // is no corresponding JAM file in the source directory.
        for (ModuleArchiveInfo mai : uninstallCandidates) {
            // Removes the module archive and the corresponding module
            // definition from the internal data structure so this
            // repository won't recognize it anymore.
            uninstallBundle(mai);
        }

        File bundleLocation = new File(getSourceLocation().toURL().getFile());
        for (File file : bundleLocation.listFiles()) {
            if (!existingBundles.contains(file)) {
                if (file.getName().endsWith(".jar")) {
                    installBundle(file.toURI());
                }
            }
        }
    }

    protected void doShutdown() throws IOException {
        // XXX: To be implemented
    }

    private void assertActive() throws IllegalStateException {
        if (!isActive()) {
            throw new IllegalStateException("OSGi repository is not active.");
        }
    }

    private void assertNotActive() throws IllegalStateException {
        if (isActive()) {
            throw new IllegalStateException("OSGi repository is active.");
        }
    }

    private void assertNotReadOnly() throws IllegalStateException {
        if (isReadOnly()) {
            throw new UnsupportedOperationException("OSGi repository is read-only.");
        }
    }
}
