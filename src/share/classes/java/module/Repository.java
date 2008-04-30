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

package java.module;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import sun.module.bootstrap.BootstrapRepository;
import sun.module.repository.RepositoryConfig;

/**
 * This class represents a repository. A repository is a mechanism for storing,
 * discovering, and retrieving module definitions that can be used by a module
 * system.
 *
 * <p> Unless otherwise specified, passing a <tt>null</tt> argument to any
 * method in this class will cause a {@link NullPointerException} to be thrown.
 * <p>
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystemPermission
 * @see java.module.Query
 * @see java.module.RepositoryEvent
 * @see java.module.RepositoryListener
 * @see java.module.VersionConstraint
 *
 * @since 1.7
 */
public abstract class Repository {

    private final Repository parent;
    private final String name;
    private final URL source;

    private final ModuleSystem system;

    /** Listeners of RepositoryEvents. */
    private static RepositoryListener repositoryListener = null;
    private static Object listenerSyncObject = new Object();

    /**
     * Obtains the default thread factory when Repository.class is loaded, so
     * the thread factory would be associated with the main thread group for
     * creating threads.
     */
    private static final ThreadFactory threadFactory = Executors.defaultThreadFactory();

    /** Used to run the RepositoryEventHandler. */
    private static ExecutorService executorService = null;

    /** Shuts down the repository upon JVM exit; see {@link #shutdownOnExit}. */
    private Thread shutdownThread;

    /**
     * Creates a {@code Repository} instance.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with
     * {@code ModuleSystemPermission("createRepository")} permission to ensure
     * it's ok to create a repository.
     *
     * @param parent the parent repository for delegation.
     * @param name the repository name.
     * @param source the source location.
     * @param system the module system with which the module definitions in the
     *        repository are associated.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to create a new
     *         repository instance.
     * @throws IllegalArgumentException if a circularity is detected.
     */
    protected Repository(Repository parent, String name, URL source, ModuleSystem system) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("createRepository"));
        }

        if (parent == null && !(this instanceof BootstrapRepository)) {
            throw new NullPointerException("parent must not be null.");
        }

        if (source == null && !(this instanceof BootstrapRepository)) {
            throw new NullPointerException("source must not be null.");
        }

        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }

        if (system == null) {
            throw new NullPointerException("system must not be null.");
        }

        Repository theParent = parent;
        while (theParent != null) {
            if (theParent == this) {
                throw new IllegalArgumentException("Repositories in circular repository relationship.");
            }
            theParent = theParent.parent;
        }

        this.parent = parent;
        this.name = name;
        this.source = source;
        this.system = system;
    }

    /**
     * Creates a {@code Repository} instance.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with
     * {@code ModuleSystemPermission("createRepository")} permission to ensure
     * it's ok to create a repository.
     *
     * @param name the repository name.
     * @param source the source location.
     * @param system the module system with which the module definitions in the
     *        repository are associated.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to create a new
     *         instance of repository.
     * @throws IllegalArgumentException if a circularity is detected.
     */
    protected Repository(String name, URL source, ModuleSystem system) {
        this(getSystemRepository(), name, source, system);
    }

    /**
     * Returns the name of this {@code Repository}.
     *
     * @return the name.
     */
    public final String getName()   {
        return name;
    }

    /**
     * Returns the source location of this {@code Repository}.
     *
     * @return the source location.
     */
    public final URL getSourceLocation()    {
        return source;
    }

    /**
     * Returns the parent repository for delegation. If this is the bootstrap
     * repository, its parent is null.
     *
     * @return the parent {@code Repository}.
     */
    public final Repository getParent() {
        return parent;
    }

    /**
     * Returns the {@code ModuleSystem} associated with the module definitions
     * in this {@code Repository}.
     *
     * @return the {@code ModuleSystem} associated with the module definitions
     *         in this {@code Repository}.
     */
    public final ModuleSystem getModuleSystem() {
        return system;
    }

    /**
     * Returns the bootstrap repository. This is the repository provided by
     * the Java Runtime.
     *
     * @return the bootstrap repository.
     */
    public static Repository getBootstrapRepository()   {
        return BootstrapRepository.getInstance();
    }

    /**
     * Returns the system repository. This is the default delegation parent
     * for new {@code Repository} instances.
     *
     * @return the system repository.
     */
    public static Repository getSystemRepository()  {
        return RepositoryConfig.getSystemRepository();
    }

    /**
     * Initializes this {@code Repository}.
     *
     * @throws IOException if an I/O error occurs.
     * @throws IllegalStateException if this {@code Repository} has been
     *         initialized or has been shutdown.
     */
    public abstract void initialize() throws IOException;

    /**
     * Shutdown this {@code Repository}.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("shutdownRepository")} permission to
     * ensure it's ok to shutdown this {@code Repository}.
     *
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to shutdown this
     *         {@code Repository}.
     * @throws IOException if an I/O error occurs.
     * @throws IllegalStateException if this {@code Repository} has not been
     *         initialized or has been shutdown.
     */
    public abstract void shutdown() throws IOException;

    /**
     * Enable or disable that this {@code Repository} is shutdown when the
     * module system terminates. Shutdown will be attempted only during the
     * normal termination of the virtual machine, as defined by the Java
     * Language Specification. By default, shutdown on exit is disabled.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("shutdownRepository")} permission
     * to ensure it's ok to shutdown this {@code Repository}.
     *
     * @param value indicating enabling or disabling of shutdown.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to shutdown this
     *         {@code Repository}.
     */
    public final synchronized void shutdownOnExit(final boolean value) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("shutdownRepository"));
        }
        // shutdownOnExit under doPrivileged()
        AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                doShutdownOnExit(value);
                return Boolean.TRUE;
            }
        });
    }

    private void doShutdownOnExit(boolean value) {
        // Create/destroy shutdownThread based on given value.
        if (value) {
            if (shutdownThread == null) {
                shutdownThread = threadFactory.newThread(
                    new Runnable() {
                        public void run() {
                            try {
                                shutdown();
                            } catch (IOException ex) {
                                // XXX log this exception
                            }
                        }
                    });
                Runtime.getRuntime().addShutdownHook(shutdownThread);
            }
        } else {
            if (shutdownThread != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownThread);
                shutdownThread = null;
            }
        }
    }

    /**
     * Returns whether or not this {@code Repository} is active. A repository
     * instance is active if it has been initialized but has not been shutdown.
     *
     * @return true if this repository instance is active; otherwise, returns
     *         false.
     */
    public abstract boolean isActive();

    /**
     * Returns whether or not this {@code Repository} is read-only.
     *
     * @return true if this {@code Repository} is read-only; otherwise, returns
     *         false.
     */
    public abstract boolean isReadOnly();

    /**
     * Returns whether or not this {@code Repository} supports reload
     * of module definitions.
     *
     * @return true if this {@code Repository} supports reload; otherwise,
     *         returns false.
     * @see #reload()
     */
    public abstract boolean supportsReload();

    /**
     * Find a module definition. Equivalent to:
     * <pre>
     *      find(moduleName, VersionConstraint.DEFAULT);
     * </pre>
     * @param name the module definition's name.
     * @return the module definition or null if not found. If more than one
     *         module definition matches the specified name, the highest
     *         version is returned.
     * @throws IllegalStateException if this {@code Repository} has not been
     *         initialized or if it has been shutdown.
     */
    public final ModuleDefinition find(String name) {
        assertActive();
        return find(name, VersionConstraint.DEFAULT);
    }

    /**
     * Find a module definition.
     *
     * @param name the module definition's name.
     * @param versionConstraint the version constraint.
     * @return the module definition or null if not found. If more than one
     *         module definition matches the specified name and version
     *         constraint, the highest version is returned.
     * @throws IllegalStateException if this {@code Repository} has not been
     *         initialized or if it has been shutdown.
     */
    public final ModuleDefinition find(String name, VersionConstraint versionConstraint) {
        assertActive();
        List<ModuleDefinition> moduleDefs = find(Query.module(name, versionConstraint));

        if (moduleDefs.isEmpty()) {
            return null;
        }

        ModuleDefinition result = moduleDefs.get(0);
        Version recentVersion = result.getVersion();

        for (ModuleDefinition moduleDef : moduleDefs) {
            Version version = moduleDef.getVersion();
            if (recentVersion.compareTo(version) < 0) {
                result = moduleDef;
                recentVersion = version;
            }
        }

        return result;
    }

    /**
     * Find all module definitions. Equivalent to:
     * <pre>
     *      find(Query.ANY);
     * </pre>
     * @return the list of matching module definitions.
     * @throws IllegalStateException if this {@code Repository} has not been
     *         initialized or if it has been shutdown.
     */
    public final List<ModuleDefinition> findAll() {
        assertActive();
        return find(Query.ANY);
    }

    /**
     * Find all matching module definitions that match the specified constraint.
     *
     * @param constraint the constraint.
     * @return the list of matching module definitions.
     * @throws IllegalStateException if this {@code Repository} has not been
     *         initialized or if it has been shutdown.
     */
    public final List<ModuleDefinition> find(Query constraint) {
        assertActive();
        if (parent == null) {
            return BootstrapRepository.getInstance().findModuleDefinitions(constraint);
        }

        // First, find module definitions from the parent repository ...
        List<ModuleDefinition> parentModuleDefs = parent.find(constraint);

        // Find module definitions from this repository
        List<ModuleDefinition> thisModuleDefs = findModuleDefinitions(constraint);

        // Adds module definitions from parent repository to the result
        List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
        result.addAll(parentModuleDefs);

        // Obtains system's visibility policy
        VisibilityPolicy vp = Modules.getVisibilityPolicy();

        // Iterates each module definition in this repository
        for (ModuleDefinition moduleDef : thisModuleDefs)  {

            // Checks if this repository return any SE platform modules
            if (moduleDef.getName().startsWith("java."))
                throw new SecurityException("Non-bootstrap repository must not return " + moduleDef.getName() + " module definition.");

            boolean inParent = false;

            // Checks if a module definition in this repository is available
            // from the parent repository.
            for (ModuleDefinition parentModuleDef : parentModuleDefs) {
                if (moduleDef.getName().equals(parentModuleDef.getName())
                    && moduleDef.getVersion().equals(parentModuleDef.getVersion())) {
                    inParent = true;
                    break;
                }
            }

            // Adds the module definition into the result only if the parent
            // repository doesn't have the module definition and that module
            // definition is visible accordingly to the visibility policy.
            if (!inParent && vp.isVisible(moduleDef)) {
                result.add(moduleDef);
            }
        }

        return result;
    }

    /**
     * Find all matching module definitions in this {@code Repository}. This
     * method must be implemented by repository implementations for finding
     * matching module definitions, and will be invoked by the
     * {@link #find} method after checking the parent repository for the
     * requested module definitions.
     *
     * @param constraint the constraint.
     * @return the list of matching module definitions.
     * @throws IllegalStateException if this {@code Repository} has not been
     *         initialized or has been shutdown.
     */
    protected abstract List<ModuleDefinition> findModuleDefinitions(Query constraint);

    /**
     * Returns an unmodifiable list of the installed module archives'
     * information in this {@code Repository}. The list will contain a snapshot
     * of the installed module archives in this {@code Repository} at the time
     * of the given invocation of this method.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("listModuleArchive")} permission to
     * ensure it's ok to return the information of the installed module
     * archives in this {@code Repository}.
     *
     * @return an unmodifiable list of the installed module archives'
     *         information.
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to return the
     *         information of the installed module archives.
     * @throws IllegalStateException if this {@code Repository} has not been
     *         initialized or it has been shutdown.
     */
    public abstract List<ModuleArchiveInfo> list();

    /**
     * Install a module archive with the module definition into this
     * {@code Repository}.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("installModuleArchive")} permission
     * to ensure it's ok to install a module archive into this
     * {@code Repository}.
     *
     * @param url the URL to the module archive.
     * @return the {@code ModuleArchiveInfo} object that represents the
     *         installed module archive.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to install a
     *         module archive in this {@code Repository}.
     * @throws UnsupportedOperationException if {@code Repository} is
     *         read-only.
     * @throws IOException if an error occurs while installing the module
     *         archive.
     * @throws ModuleFormatException if the module archive format is not
     *         supported by this implementation.
     * @throws IllegalStateException if a module definition with the same name,
     *         version and platform binding is already installed, or if this
     *         {@code Repository} has not been initialized or it has been
     *         shutdown.
     */
    public abstract ModuleArchiveInfo install(URL url) throws IOException;

    /**
     * Uninstall a module archive from this {@code Repository}.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("uninstallModuleArchive")}
     * permission to ensure it's ok to uninstall a module archive from this
     * {@code Repository}.
     *
     * @param m the module archive to be uninstalled.
     * @return true if the module archive is found and uninstalled, returns
     *         false otherwise.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to uninstall the
     *         module archive in this {@code Repository}.
     * @throws UnsupportedOperationException if this {@code Repository} is
     *         read-only.
     * @throws IllegalStateException if the module definition in the specified
     *         specified module archive is in use, or if this
     *         {@code Repository} has not been initialized or it has been
     *         shutdown.
     * @throws IOException If an error occurs while uninstalling the module
     *         archive.
     */
    public abstract boolean uninstall(ModuleArchiveInfo m) throws IOException;

    /**
     * Reload this {@code Repository}. The behavior of this method depends on
     * the implementation.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("reloadRepository")} permission
     * to ensure it's ok to reload module definitions in this
     * {@code Repository}.
     *
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to reload module
     *         definitions in this {@code Repository}.
     * @throws UnsupportedOperationException if this {@code Repository}
     *         does not support reload.
     * @throws IllegalStateException if a module definition is in use thus
     *         cannot be reloaded.
     * @throws IOException If an error occurs while reloading the module
     *         definitions.
     */
    public abstract void reload() throws IOException;

    /**
     * Adds the specified repository listener to receive repository events from
     * the repositories.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("addRepositoryListener")} permission
     * to ensure it's ok to add a repository listener to the repositories.
     *
     * @param listener the repository listener
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to add a
     *         repository listener to the repositories.
     */
    public static final void addRepositoryListener(RepositoryListener listener) {
        if (listener == null) {
            throw new NullPointerException("Repository listener must not be null.");
        }

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("addRepositoryListener"));
        }

        synchronized(listenerSyncObject) {
            repositoryListener = EventMulticaster.add(repositoryListener, listener);
        }
    }

    /**
     * Removes the specified repository listener so that it no longer receives
     * repository events from the repositories.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("removeModuleSystemListener")}
     * permission to ensure it's ok to remove a repository listener from the
     * repositories.
     *
     * @param listener the repository listener
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to remove a
     *         repository listener from the repositories.
     */
    public static final void removeRepositoryListener(RepositoryListener listener) {
        if (listener == null) {
            throw new NullPointerException("Repository listener must not be null.");
        }

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("removeRepositoryListener"));
        }

        synchronized(listenerSyncObject) {
            repositoryListener = EventMulticaster.remove(repositoryListener, listener);
        }
    }

    /**
     * RepositoryEventHandler that is executed in the event dispatch thread.
     */
    private static class RepositoryEventHandler implements Runnable {
        private RepositoryListener listener;
        private RepositoryEvent event;
        RepositoryEventHandler(RepositoryListener listener, RepositoryEvent event) {
            this.listener = listener;
            this.event = event;
        }
        public void run() {
            if (listener != null) {
                switch (event.getType()) {
                    case REPOSITORY_INITIALIZED:
                    case REPOSITORY_SHUTDOWN:
                    case MODULE_INSTALLED:
                    case MODULE_UNINSTALLED:
                        listener.handleEvent(event);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Processes repository event occuring in this {@code Repository} by
     * dispatching them to any registered {@code RepositoryListener} objects.
     *
     * @param event the repository event
     */
    protected final void processEvent(RepositoryEvent event) {
        RepositoryListener listener = repositoryListener;

        // Skips event notification if there is no listener.
        if (listener == null)
            return;

        synchronized(listenerSyncObject) {
            if (executorService == null) {
                // Creates a single thread executor that creates thread in the main
                // thread group.
                executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
                                        public Thread newThread(Runnable r) {
                                            Thread t = threadFactory.newThread(r);
                                            // Changes thread to daemon so it will allow application
                                            // to exit normally.
                                            t.setDaemon(true);
                                            return t;
                                        }
                                  });

            }
        }

        // Dispatch the event in the event dispatch thread
        executorService.submit(new RepositoryEventHandler(listener, event));
    }

    /**
     * Compares the specified object with this {@code Repository} for equality.
     * Returns {@code true} if and only if {@code obj} is the same object as
     * this {@code Repository}.
     *
     * @param obj the object to be compared for equality with this
     *        {@code Repository}.
     * @return {@code true} if the specified object is equal to this
     *         {@code Repository}
     */
    @Override
    public final boolean equals(Object obj)   {
        return (this == obj);
    }

    /**
     * Returns a hash code for this {@code Repository}.
     *
     * @return a hash code value for this {@code Repository}.
     */
    @Override
    public final int hashCode()   {
        return super.hashCode();
    }

    /**
     * Returns a {@code String} object representing this {@code Repository}.
     *
     * @return a string representation of the {@code Repository} object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("repository ");
        builder.append(getName());
        if (getSourceLocation() != null) {
            builder.append(" (");
            builder.append(getSourceLocation());
            builder.append(")");
        }
        return builder.toString();
    }

    private void assertActive() throws IllegalStateException {
         if (!isActive()) {
            throw new IllegalStateException("Repository is not active.");
        }
    }
}
