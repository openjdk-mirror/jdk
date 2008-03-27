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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import sun.module.bootstrap.BootstrapRepository;
import sun.module.repository.RepositoryConfig;

/**
 * This class represents the repository in the module system.
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

    /** Listens to RepositoryEvents. */
    private RepositoryListener repositoryListener = null;

    /**
     * Obtains the default thread factory when Repository.class is loaded, so
     * the thread factory would be associated with the main thread group for
     * creating threads.
     */
    private static final ThreadFactory threadFactory = Executors.defaultThreadFactory();

    /** Used to run the RepositoryEventHandler. */
    private ExecutorService executorService = null;

    /** Shuts down the repository upon JVM exit; see {@link #shutdownOnExit}. */
    private Thread shutdownThread;

    /** Counter for generating Id for repository instances. */
    private static final AtomicLong idCounter = new AtomicLong();

    /** Id for this repository instance. */
    private final long id = idCounter.incrementAndGet();

    /**
     * Creates a repository instance.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's <code>checkPermission</code> method with
     * <code>ModuleSystemPermission("createRepository")</code> permission to
     * ensure it's ok to create a repository.
     *
     * @param parent the parent repository for delegation.
     * @param name the repository name.
     * @param source the source location.
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to create a new
     *         instance of repository.
     * @throws NullPointerException if <code>parent</code> is null,
     *         <code>name</code> is null, <code>source</code> is null,
     *         or <code>system</code> is null.
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
     * Creates a repository instance.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's <code>checkPermission</code> method with
     * <code>ModuleSystemPermission("createRepository")</code> permission to
     * ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param source the source location.
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to create a new
     *         instance of repository.
     * @throws NullPointerException if <code>name</code> is null,
     *         <code>source</code> is null, or <code>system</code> is null.
     * @throws IllegalArgumentException if a circularity is detected.
     */
    protected Repository(String name, URL source, ModuleSystem system) {
        this(getSystemRepository(), name, source, system);
    }

    /**
     * Returns a long value that represents the unique identifier assigned to
     * this repository instance. The identifier is assigned by the JVM and is
     * JVM implementation dependent.
     *
     * @return a long value that represents the unique identifier assigned to
     *         this repository instance.
     */
    public final long getId() {
        return id;
    }

    /**
     * Returns the name of this repository.
     *
     * @return the name.
     */
    public final String getName()   {
        return name;
    }

    /**
     * Returns the source location of this repository.
     *
     * @return the source location.
     */
    public final URL getSourceLocation()    {
        return source;
    }

    /**
     * Returns the parent repository for delegation. If this
     * is the bootstrap repository, its parent is null.
     *
     * @return the parent <code>Repository.</code>.
     */
    public final Repository getParent() {
        return parent;
    }

    /**
     * Returns this Repository's {@code ModuleSystem}.
     *
     * @return this repository's {@code ModuleSystem}
     */
    public final ModuleSystem getModuleSystem() {
        return system;
    }

    /**
     * Returns the bootstrap repository for delegation. This
     * the repository provided by the Java Runtime.
     */
    public static Repository getBootstrapRepository()   {
        return BootstrapRepository.getInstance();
    }

    /**
     * Returns the system repository for delegation. This is
     * the default delegation parent for new Repository
     * instances.
     */
    public static Repository getSystemRepository()  {
        return RepositoryConfig.getSystemRepository();
    }

    /**
     * Initializes the repository using the default configuration.
     *
     * @throws IOException if an I/O error occurs.
     * @throws IllegalStateException if the repository instance has been
     *         initialized or has been shutdown.
     */
    public abstract void initialize() throws IOException;

    /**
     * Shutdown the repository.
     *
     * If a security manager is present, this method calls the security
     * manager's <code>checkPermission</code> method with a
     * <code>ModuleSystemPermission("shutdownRepository")</code> permission
     * to ensure it's ok to shutdown a repository.
     *
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to shutdown the
     *         repository.
     * @throws IOException if an I/O error occurs.
     * @throws IllegalStateException if the repository instance has not been
     *         initialized or has been shutdown.
     */
    public abstract void shutdown() throws IOException;

    /**
     * Enable or disable that the repository is shutdown when the
     * module system terminates. Shutdown will be attempted only
     * during the normal termination of the virtual machine, as
     * defined by the Java Language Specification. By default,
     * shutdown on exit is disabled.
     *
     * If a security manager is present, this method calls the
     * security manager's <code>checkPermission</code> method with
     * a <code>ModuleSystemPermission("shutdownRepository")</code>
     * permission to ensure it's ok to shutdown a repository.
     *
     * @param value indicating enabling or disabling of shutdown.
     * @throws SecurityException if a security manager exists and
     * its <tt>checkPermission</tt> method denies access
     * to shutdown the repository.
     */
    public synchronized void shutdownOnExit(boolean value) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("shutdownRepository"));
        }

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
     * Returns whether or not the repository instance is active.
     * <p>
     * A repository instance is active if it has been initialized but has not
     * been shutdown.
     *
     * @return true if this repository instance is active.
     */
    public abstract boolean isActive();

    /**
     * Returns whether or not this repository is read-only.
     *
     * @return true if this repository is read-only.
     */
    public abstract boolean isReadOnly();

    /**
     * Returns whether or not this repository supports reloading.
     *
     * @return true if this repository supports reloading.
     * @see #reload()
     */
    public boolean isReloadSupported() {
        return false;
    }

    /**
     * Find a module definition. Equivalent to:
     * <pre>
     *      find(moduleName, VersionConstraint.DEFAULT);
     * </pre>
     *
     * If this repository instance has not been initialized when this
     * method is called, it will be initialized automatically by
     * calling the initialize method with no argument.
     *
     * @param name the module definition's name.
     * @return the module definition or null if not found. If
     *         more than one module definition matches the specified
     *         name, the latest version is returned.
     * @throws IllegalStateException if the repository instance has been
     *         shutdown.
     */
    public final ModuleDefinition find(String name) {
        assertActive();
        return find(name, VersionConstraint.DEFAULT);
    }

    /**
     * Find a module definition.
     *
     * If this repository instance has not been initialized when this
     * method is called, it will be initialized automatically by
     * calling the initialize method with no argument.
     *
     * @param name the module definition's name.
     * @param versionConstraint the version constraint.
     * @return the module definition or null if not found. If
     *         more than one module definition matches the specified
     *         name and version constraint, the latest version is returned.
     * @throws IllegalStateException if the repository instance has been
     *         shutdown.
     */
    public final ModuleDefinition find(String name, VersionConstraint versionConstraint) {
        assertActive();
        List<ModuleDefinition> moduleDefs = find(Query.and(Query.name(name), Query.version(versionConstraint)));

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
     *
     * If this repository instance has not been initialized when this
     * method is called, it will be initialized automatically by
     * calling the initialize method with no argument.
     *
     * @return the result list.
     * @throws IllegalStateException if the repository instance has been
     *         shutdown.
     */
    public final List<ModuleDefinition> findAll() {
        assertActive();
        return find(Query.ANY);
    }

    /**
     * Find all matching module definitions that match the specified
     * constraint.
     *
     * If this repository instance has not been initialized when this
     * method is called, it will be initialized automatically by
     * calling the initialize method with no argument.
     *
     * @param constraint the constraint.
     * @return the result list.
     * @throws IllegalStateException if the repository instance has been
     *         shutdown.
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
     * Find all matching module definitions in the repository. This
     * method should be overridden by repository implementations for
     * finding matching module definitions, and will be invoked by
     * the find method after checking the parent repository for
     * the requested module definitions.
     * <p>
     * If this repository instance has not been initialized when this
     * method is called, it will be initialized automatically by
     * calling the initialize method with no argument.
     *
     * @param constraint the constraint.
     * @return the collection of matching module definitions.
     * @throws IllegalStateException if the repository instance has
     * not been initialized or has been shutdown.
     */
    protected abstract List<ModuleDefinition> findModuleDefinitions(Query constraint);

    /**
     * Returns an unmodifiable list of the installed module archives' information
     * in the repository. The list will contain a snapshot of the installed module
     * archives in the repository at the time of the given invocation of this method.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's <code>checkPermission</code> method with
     * a <code>ModuleSystemPermission("listModuleArchive")
     * </code> permission to ensure it's ok to return the information of the
     * installed module archives in a repository.
     *
     * @return an unmodifiable list of the installed module archives' information.
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to return the
     *         the information of the installed module archives.
     * @throws IllegalStateException if the repository instance has not
     *         been initialized or it has been shutdown.
     */
    public abstract List<ModuleArchiveInfo> list();

    /**
     * Install a module archive with the module definition into the repository.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's <code>checkPermission</code> method with
     * a <code>ModuleSystemPermission("installModuleArchive")
     * </code> permission to ensure it's ok to install a module
     * archive into a repository.
     *
     * @param u the URL to the module archive.
     * @return the <code>ModuleArchiveInfo</code> object that represents
     *         the installed module archive.
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to install a
     *         module archive in the repository.
     * @throws IOException if an error occurs while installing the module archive.
     * @throws ModuleFormatException if the module archive format is not
     *         supported by this implementation.
     * @throws UnsupportedOperationException if the repositoryis read-only.
     * @throws IllegalStateException if a module definition with the same name,
     *         version and platform binding is already installed, or if the
     *         repository instance has not been initialized or it has been
     *         shutdown.
     */
    public abstract ModuleArchiveInfo install(URL u) throws IOException;

    /**
     * Uninstall a module archive from the repository.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's <code>checkPermission</code> method with
     * a <code>ModuleSystemPermission("uninstallModuleArchive")
     * </code> permission to ensure it's ok to uninstall a module
     * archive from a repository.
     *
     * @param m the module archive to be uninstalled.
     * @return true if the module archive is found and uninstalled,
     *         returns false otherwise.
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to uninstall the module archive in the repository.
     * @throws IllegalStateException if the module definition in the specified
     *         specified module archive is in use, or
     *         if the repository instance has not been
     *         initialized or it has been shutdown.
     * @throws UnsupportedOperationException if the repository is read-only.
     * @throws IOException If an error occurs while uninstalling the module archive.
     */
    public abstract boolean uninstall(ModuleArchiveInfo m) throws IOException;

    /**
     * Reload the repository. The behavior of this method depends on
     * the implementation.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's <code>checkPermission</code> method with
     * a <code>ModuleSystemPermission("reloadRepository")</code>
     * permission to ensure it's ok to reload module
     * definitions in a repository.
     *
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to reload module definitions in the repository.
     * @throws UnsupportedOperationException if the repository
     *         does not support reload.
     * @throws IllegalStateException if a module definition is in use
     *         thus cannot be reloaded.
     * @throws IOException If an error occurs while reloading the
     *         module definitions.
     */
    public abstract void reload() throws IOException;

    /**
     * Adds the specified repository listener to receive repository events
     * from this repository.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's checkPermission method with a <code>
     * ModuleSystemPermission("addRepositoryListener")</code> permission to
     * ensure it's ok to add a repository listener to the repository.
     *
     * @param listener the repository listener
     * @throws SecurityException if a security manager exists and its
     *         checkPermission method denies access to add a repository
     *         listener to the repository.
     * @throws NullPointerException if listener is null.
     */
    public final void addRepositoryListener(RepositoryListener listener) {
        if (listener == null) {
            throw new NullPointerException("Repository listener must not be null.");
        }

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("addRepositoryListener"));
        }

        synchronized (this) {
            repositoryListener = EventMulticaster.add(repositoryListener, listener);
        }
    }

    /**
     * Removes the specified repository listener so that it no longer
     * receives repository events from this repository.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's checkPermission method with a <code>
     * ModuleSystemPermission("removeModuleSystemListener")</code> permission to
     * ensure it's ok to remove a repository listener from the repository.
     *
     * @param listener the repository listener
     * @throws SecurityException if a security manager exists and its
     *         checkPermission method denies access to remove a repository
     *         listener from the repository.
     * @throws NullPointerException if listener is null.
     */
    public final void removeRepositoryListener(RepositoryListener listener) {
        if (listener == null) {
            throw new NullPointerException("Repository listener must not be null.");
        }

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("removeRepositoryListener"));
        }

        synchronized (this) {
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
     * Processes repository event occuring in this repository by
     * dispatching them to any registered RepositoryListener objects.
     *
     * @param event the repository event
     */
    protected final void processEvent(RepositoryEvent event) {
        RepositoryListener listener = repositoryListener;

        // Skips event notification if there is no listener.
        if (listener == null)
            return;

        synchronized(this) {
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
     * this object.
     *
     * @param obj the object to be compared for equality with this repository.
     * @return {@code true} if the specified object is equal to this repository
     */
    @Override
    public final boolean equals(Object obj)   {
        return (this == obj);
    }

    /**
     * Returns a hash code for this {@code Repository}.
     *
     * @return a hash code value for this object.
     */
    @Override
    public final int hashCode()   {
        return super.hashCode();
    }

    /**
     * Returns a <code>String</code> object representing this
     * <code>Repository</code>.
     *
     * @return a string representation of the <code>Repository</code> object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Repository[name=");
        builder.append(getName());
        if (getSourceLocation() != null) {
            builder.append(",source=");
            builder.append(getSourceLocation());
        }
        builder.append("]");

        return builder.toString();
    }

    private void assertActive() throws IllegalStateException {
         if (!isActive()) {
            throw new IllegalStateException("Repository is not active.");
        }
    }
}
