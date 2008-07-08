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
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import sun.module.bootstrap.BootstrapRepository;
import sun.module.config.DefaultVisibilityPolicy;
import sun.module.repository.RepositoryConfig;

/**
 * This class represents a repository.  A repository is responsible for
 * storing, discovering, and retrieving {@linkplain ModuleDefinition module
 * definitions}.
 * <p>
 * A {@code Repository} is {@linkplain #isActive active} when it has been
 * {@linkplain #initialize() initialized} but not {@linkplain #shutdown()
 * shutdown}.   When a repository is no longer needed, a program should
 * shutdown the repository to release the resources.
 * <p>
 * The {@code Repository} class uses a delegation model to search for
 * module definitions. Each instance of {@code Repository} has an associated
 * parent repository. When requested to find a module definition, a
 * {@code Reposiory} instance will delegate the search for the
 * module definition to its parent repository before attempting to find
 * the module definition itself. The virtual machine's built-in repository,
 * called the <i>bootstrap repository</i>, does not itself have a parent
 * but may serve as the parent of a {@code Repository} instance.
 * <p>
 * The system's {@linkplain #getVisibilityPolicy visibility policy} controls
 * which module definitions be visible in a repository.
 * <p>
 * Applications implement subclasses of {@code Repository} in order to
 * extend the manner in which the Java virtual machine dynamically
 * finds module definitions. Typically, repository implementors
 * should override the
 * {@link #doInitialize()} and {@link #doShutdown()} methods.
 * <p>
 * There are two default repositories provided by the Java platform: the
 * <i>bootstrap repository</i> and the <i>system repository</i>. The
 * <i>bootstrap repository</i> exposes the standard module definitions
 * for the Java platform and is the only repository that does not have
 * a parent.  All module definitions whose name begins with "java." must
 * be exposed by the <i>bootstrap repository</i>.  The <i>system
 * repository</i> is the default parent for new {@code Repository} instances.
 *
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystemPermission
 * @see java.module.Query
 * @see java.module.RepositoryEvent
 * @see java.module.RepositoryListener
 * @see java.module.VersionConstraint
 * @see java.module.VisibilityPolicy
 *
 * @since 1.7
 */
public abstract class Repository {

    private final Repository parent;
    private final String name;
    private final URI source;

    /**
     * Internal data structures for the repository.
     */
    private volatile List<ModuleDefinition> moduleDefs =
                new ArrayList<ModuleDefinition>();
    private volatile List<ModuleArchiveInfo> moduleArchiveInfos =
                new ArrayList<ModuleArchiveInfo>();

    /** True if this repository has been initialized but not yet shutdown. */
    private boolean active;

    /** True if this repository has been shutdown. */
    private boolean shutdown;

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
     * @param name the repository name.
     * @param source the source location.
     * @param parent the parent repository for delegation.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to create a new
     *         repository instance.
     * @throws IllegalArgumentException if a circularity among the same
     *         repsoitory is detected.
     */
    protected Repository(String name, URI source, Repository parent) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("createRepository"));
        }
        if (source == null && !(this instanceof BootstrapRepository)) {
            throw new NullPointerException("source must not be null.");
        }
        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }
        if (parent == null && !(this instanceof BootstrapRepository)) {
            throw new NullPointerException("parent must not be null.");
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
    }

    /**
     * Creates a {@code Repository} instance with the
     * {@linkplain #getSystemRepository system repository} as the
     * parent for delegation.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with
     * {@code ModuleSystemPermission("createRepository")} permission to ensure
     * it's ok to create a repository.
     *
     * @param name the repository name.
     * @param source the source location.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to create a new
     *         instance of repository.
     */
    protected Repository(String name, URI source) {
        this(name, source, getSystemRepository());
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
    public final URI getSourceLocation()    {
        return source;
    }

    /**
     * Returns the parent repository for delegation. This method will return
     * {@code null} if this repository is the bootstrap repository.
     *
     * @return the parent {@code Repository}.
     */
    public final Repository getParent() {
        return parent;
    }

    /**
     * Returns the bootstrap repository.
     * <p>
     * The bootstrap repository is an implementation-dependent instance of
     * this class , and it is provided by the Java Runtime.
     *
     * @return the bootstrap repository.
     */
    public static Repository getBootstrapRepository()   {
        return BootstrapRepository.getInstance();
    }

    /**
     * Returns the system repository for delegation. This is the default
     * delegation parent for new {@code Repository} instances, and is
     * typically the repository used to start the application.
     * <p>
     * The system repository is an implementation-dependent instance of
     * this class, and it is provided by the Java Runtime.
     *
     * @return the system repository.
     */
    public static Repository getSystemRepository()  {
        return RepositoryConfig.getSystemRepository();
    }

    /**
     * Returns the system's default visibility policy for module definitions
     * in the repositories.
     * <p>
     * The default class of the visibility policy can be overridden using the
     * {@code java.module.visibility.policy.classname} system property.
     *
     * @return the system's default visibility policy for module definitions.
     */
    public static VisibilityPolicy getVisibilityPolicy() {
        return Modules.getVisibilityPolicy();
    }

    /**
     * Initializes this {@code Repository}. This method should be overridden
     * by repository implementations as follows:
     * <p><ol>
     *   <li><p> If this {@code Repository} is {@linkplain #isActive() active},
     *           return.</p></li>
     *
     *   <li><p> If this {@code Repository} has been
     *           {@linkplain #shutdown() shutdown},
     *           throws an {@code IllegalStateException}.</p></li>
     *
     *   <li><p> Perform the actual initialization and this is repository
     *           implementation specific. Fire a single
     *           {@link RepositoryEvent.Type#MODULE_DEFINITION_ADDED
     *           <tt>MODULE_DEFINITION_ADDED</tt>} event if there is one or
     *           more module definitions constructed during initialization.
     *           </p></li>
     *
     *   <li><p> Fire a {@link RepositoryEvent.Type#REPOSITORY_INITIALIZED
     *           <tt>REPOSITORY_INITIALIZED</tt>} event.</p></li>
     * </ol>
     *
     * The default implementation of this method initializes the repository as
     * follows:
     * <p><ol>
     *   <li><p> If this {@code Repository} is {@linkplain #isActive() active},
     *           return.</p></li>
     *
     *   <li><p> If this {@code Repository} has been
     *           {@linkplain #shutdown() shutdown},
     *           throws an {@code IllegalStateException}.</p></li>
     *
     *   <li><p> Invoke the {@link #doInitialize()} method.</p></li>
     *
     *   <li><p> Fire a {@link RepositoryEvent.Type#REPOSITORY_INITIALIZED
     *           <tt>REPOSITORY_INITIALIZED</tt>} event.</p></li>
     * </ol>
     *
     * <p> Subclasses of <tt>Repository</tt> are encouraged to override {@link
     * #doInitialize()} method, rather than this method.</p>
     *
     * @throws IllegalStateException if this {@code Repository} has been
     *         shutdown.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized void initialize() throws IOException {
        if (active) {
            return;             // initialize only once
        }
        if (shutdown) {
            throw new IllegalStateException("Repository is already shutdown");
        }

        moduleArchiveInfos = Collections.unmodifiableList(doInitialize());

        active = true;

        // Send REPOSITORY_INITIALIZED event
        RepositoryEvent evt2 = new RepositoryEvent(this,
                                    RepositoryEvent.Type.REPOSITORY_INITIALIZED, null, null);
        processEvent(evt2);
    }


    /**
     * Initializes this {@code Repository}. This method should be overridden
     * by repository implementations to initialize the repository by load
     * module archives and construct module definitions, and will be invoked
     * by the {@link #initialize()} method.
     * <p>
     * If the module archives are loaded successfully by this method,
     * the implementation should return the information of the module archives
     * as a list of {@code ModuleArchiveInfo}s. The implementation should NOT
     * invoke the {@link #addModuleArchiveInfo(ModuleArchiveInfo)} method with
     * the information of the module archives.
     * <p>
     * If new module definition(s) are constructed successfully by this method,
     * the implementation should add the module definitions to this
     * {@code Repository} by invoking the
     * {@link #addModuleDefinitions(Set)} method or the
     * {@link #addModuleDefinition(ModuleDefinition)} method.
     * <p>
     * The default implementation of this method throws an {@code IOException}.
     *
     * @return a list of {@code ModuleArchiveInfo}s that represents the module
     *         archives loaded during initialization.
     * @throws IOException if an I/O error occurs.
     * @see #initialize()
     */
    protected List<ModuleArchiveInfo> doInitialize() throws IOException {
        throw new IOException("Repository's initialization is not yet implemented.");
    }


    /**
     * Shutdown this {@code Repository}. This method should be overridden by
     * repository implementations as follows:
     * <p><ol>
     *   <li><p> If a security manager is present, calls the security
     *           manager's {@code checkPermission} method with a
     *           {@code ModuleSystemPermission("shutdownRepository")}
     *           permission to ensure it's ok to shutdown this
     *           {@code Repository}.</p></li>
     *
     *   <li><p> If this {@code Repository} is not
     *           {@linkplain #isActive() active},
     *           throws an {@code IllegalStateException}.</p></li>
     *
     *   <li><p> Perform the actual shutdown and this is repository
     *           implementation specific.</p></li>
     *
     *   <li><p> For each module definition in this {@code Repository}, invoke
     *           its module system's
     *           {@link ModuleSystem#disableModuleDefinition(ModuleDefinition)
     *           <tt>disableModuleDefinition(ModuleDefinition)</tt>}
     *           method and {@link ModuleSystem#releaseModule(ModuleDefinition)
     *           <tt>releaseModule(ModuleDefinition)</tt>} method.</p></li>
     *
     *   <li><p> Fire a single {@link RepositoryEvent.Type#MODULE_DEFINITION_REMOVED
     *           <tt>MODULE_DEFINITION_REMOVED</tt>} event if there is one or
     *           more module definitions in this {@code Repository}.</p></li>
     *
     *   <li><p> Fire a {@link RepositoryEvent.Type#REPOSITORY_SHUTDOWN
     *           <tt>REPOSITORY_SHUTDOWN</tt>} event.</p></li>
     * </ol>
     *
     * The default implementation of this method shutdown the repository as follows:
     * <p><ol>
     *   <li><p> If a security manager is present, calls the security
     *           manager's {@code checkPermission} method with a
     *           {@code ModuleSystemPermission("shutdownRepository")}
     *           permission to ensure it's ok to shutdown this
     *           {@code Repository}.</p></li>
     *
     *   <li><p> If this {@code Repository} is not
     *           {@linkplain #isActive() active},
     *           throws an {@code IllegalStateException}.</p></li>
     *
     *   <li><p> Invoke the {@link #doShutdown()} method.</p></li>
     *
     *   <li><p> Invoke the {@link #removeModuleDefinitions(Set)}
     *           method with the set of module definitions in this
     *           {@code Repository}.</p></li>
     *
     *   <li><p> Fire a {@link RepositoryEvent.Type#REPOSITORY_SHUTDOWN
     *           <tt>REPOSITORY_SHUTDOWN</tt>} event.</p></li>
     * </ol>
     *
     * <p> Subclasses of <tt>Repository</tt> are encouraged to override {@link
     * #doShutdown()}, rather than this method.</p>
     *
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to shutdown this
     *         {@code Repository}.
     * @throws IllegalStateException if this {@code Repository} is not active.
     * @throws IOException if an I/O error occurs.
     */
    public synchronized void shutdown() throws IOException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("shutdownRepository"));
        }
        if (shutdown) {
            return;             // shutdown only once
        }
        assertActive();

        doShutdown();

        removeModuleDefinitions(new HashSet<ModuleDefinition>(moduleDefs));

        moduleDefs = null;
        moduleArchiveInfos = null;

        active = false;
        shutdown = true;

        // Send REPOSITORY_SHUTDOWN event
        RepositoryEvent evt2 = new RepositoryEvent(this,
                                    RepositoryEvent.Type.REPOSITORY_SHUTDOWN, null, null);
        processEvent(evt2);
    }

    /**
     * Shutdown this {@code Repository}. This method should be overridden
     * by repository implementations, and will be invoked by the
     * {@link #shutdown()} method. The default implementation is a no-op.
     *
     * @throws IOException if an I/O error occurs.
     * @see #shutdown()
     */
    protected void doShutdown() throws IOException {
        // no-op
    }

    /**
     * Enable or disable that this {@code Repository} is shutdown when the
     * Java Module System terminates. Shutdown will be attempted only during
     * the normal termination of the virtual machine, as defined by the Java
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
     * @see #shutdown()
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
     * Returns whether or not this {@code Repository} is active. A
     * {@code Repository} is active when it has been
     * {@linkplain #initialize() initialized} but not
     * {@linkplain #shutdown() shutdown}. This method should be overridden
     * by subclasses of {@code Repository} if the {@link initialize()} and
     * {@link shutdown()} methods are also overridden.
     *
     * @return true if this {@code Repository} is active; otherwise, returns
     *         false.
     * @see #initialize()
     * @see #shutdown()
     */
    public synchronized boolean isActive() {
        return active;
    }

    /**
     * Returns whether or not this {@code Repository} is read-only. This method
     * should be overridden by repository implementations. The default
     * implementation returns {@code true}.
     *
     * @return true if this {@code Repository} is read-only; otherwise, returns
     *         false.
     */
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Returns whether or not this {@code Repository} supports reload of
     * module archives. This method should be overridden by repository
     * implementations. The default implementation returns {@code false}.
     *
     * @return true if this {@code Repository} supports reload; otherwise,
     *         returns false.
     * @see #reload()
     */
    public boolean supportsReload() {
        return false;
    }

    /**
     * Find a module definition across multiple repositories through
     * the delegation model. Equivalent to:
     * <pre>
     *      find(name, VersionConstraint.DEFAULT);</pre>
     *
     * @param name the module definition's name.
     * @return the module definition that matches the specified name and
     *         version constraint, or null if there is no match.
     * @throws IllegalStateException if this {@code Repository} is not active.
     * @see #find(String, VersionConstraint)
     */
    public final ModuleDefinition find(String name) {
        return find(name, VersionConstraint.DEFAULT);
    }

    /**
     * Find a module definition across multiple repositories through the
     * delegation model. Equivalent to:
     * <pre>
     *      find(Query.module(name, versionConstraint))</pre>
     * and returns only the highest version if more than one module definition
     * matches the specified name and version constraint.
     *
     * @param name the module definition's name.
     * @param versionConstraint the version constraint.
     * @return the module definition that matches the specified name and
     *         version constraint, or null if there is no match.
     * @throws IllegalStateException if this {@code Repository} is not active.
     * @see #find(Query)
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
     * Find all module definitions across multiple repositories through the
     * delegation model. Equivalent to:
     * <pre>
     *      find(Query.ANY);</pre>
     *
     * @return a list of module definitions.
     * @throws IllegalStateException if this {@code Repository} is not active.
     * @see #find(Query)
     */
    public final List<ModuleDefinition> findAll() {
        assertActive();
        return find(Query.ANY);
    }

    /**
     * Find all matching module definitions across multiple repositories
     * through the delegation model. This method performs the following:
     * <p><ol>
     *      <li><p> Invoke the {@link #getParent()} method to determine the
     *              parent repository.</p></li>
     *
     *      <li><p> If the parent repository is {@code null}, invoke
     *              the {@link #findModuleDefinitions(Query)} method to obtain
     *              the set of module definitions and return.</p></li>
     *
     *      <li><p> Invoke the {@link #find(Query)} method of the parent
     *              repository to obtain the set of module definitions
     *              <i>P</i>.</p></li>
     *
     *      <li><p> Invoke the {@link #findModuleDefinitions(Query)} method
     *              to obtain the set of module definitions <i>C</i>.</p></li>
     *
     *      <li><p> If the name of any module definition in <i>C</i> begins with
     *             "java.", throw a {@code SecurityException}.</p></li>
     *
     *      <li><p> For each module definition in <i>C</i>, invoke the
     *              {@link VisibilityPolicy#isVisible(ModuleDefinition)
     *              <tt>isVisible()</tt>} method of the {@link VisibilityPolicy}
     *              object returned from {@link #getVisibilityPolicy()} to
     *              determine if the module definition is visible. If not,
     *              remove the module definition from <i>C</i>.</p></li>
     *
     *      <li><p> Determine the set of module definitions <i>R</i> as
     *              follows:</p></li>
     *          <ul>
     *          <li><p> All module definitions in <i>P</i> are in <i>R</i>.</p></li>
     *          <li><p> For each module definition in <i>C</i>, it is in
     *                  <i>R</i> if and only if the module definition with the
     *                  same name and version is not present in <i>P</i>.</p></li>
     *          </ul>
     *
     *      <li><p> Return <i>R</i> as the result.</p></li>
     * </ol></p>
     *
     * @param constraint the constraint.
     * @return the list of matching module definitions.
     * @throws IllegalStateException if this {@code Repository} is not active.
     * @see #findModuleDefinitions(Query)
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
        VisibilityPolicy vp = getVisibilityPolicy();

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
     * method should be overridden by repository implementations only for
     * optimization, and will be invoked by the {@link #find(Query)} method.
     * The default implementation determines the result by matching each
     * module definition in this {@code Repository} with the specified
     * constraint.
     *
     * @param constraint the constraint.
     * @return the list of matching module definitions.
     * @throws IllegalStateException if this {@code Repository} is not active.
     * @see find(Query)
     */
    protected synchronized List<ModuleDefinition> findModuleDefinitions(Query constraint) {
        assertActive();

        if (constraint == Query.ANY) {
            return moduleDefs;
        } else {
            List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
            for (ModuleDefinition md : moduleDefs) {
                if (md != null && constraint.match(md)) {
                    result.add(md);
                }
            }
            return result;
        }
    }

    /**
     * Returns an unmodifiable list of the installed module archives'
     * information in this {@code Repository}. This method should be overridden
     * and implemented by repository implementations as follows:
     * <p><ul>
     *   <li><p> If a security manager is present, calls the security
     *           manager's {@code checkPermission} method with a
     *           {@code ModuleSystemPermission("listModuleArchive")} permission
     *           to ensure it's ok to return the information of the installed
     *           module archives in this {@code Repository}.</p></li>
     *
     *   <li><p> If this {@code Repository} is not
     *           {@linkplain #isActive() active},
     *           throws an {@code IllegalStateException}.</p></li>
     *
     *   <li><p> Invoke the {@link #getModuleArchiveInfos()} method to
     *           determine the list of {@code ModuleArchiveInfo}s, and return
     *           the result.</p></li>
     * </ul>
     * The default implementation of this method throws an
     * {@code UnsupportedOperationException}.
     *
     * @return an unmodifiable list of {@code ModuleArchiveInfo}.
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to return the
     *         information of the installed module archives.
     * @throws UnsupportedOperationException if {@code Repository} does not
     *         support this operation.
     * @throws IllegalStateException if this {@code Repository} is not active.
     */
    public List<ModuleArchiveInfo> list() {
        throw new UnsupportedOperationException("Repository does not support the list operation.");
    }

    /**
     * Install a module archive into this {@code Repository}. This method
     * should be overridden by repository implementations as follows:
     * <p><ol>
     *   <li><p> If a security manager is present, calls the security
     *           manager's {@code checkPermission} method with a
     *           {@code ModuleSystemPermission("installModuleArchive")} permission
     *           to ensure it's ok to return the information of the installed
     *           module archives in this {@code Repository}.</p></li>
     *
     *   <li><p> If {@code uri} is {@code null}, throws a
     *           {@code NullPointerException}.</p></li>
     *
     *   <li><p> If this {@code Repository} is not
     *           {@linkplain #isActive() active},
     *           throws an {@code IllegalStateException}.</p></li>
     *
     *   <li><p> Invoke the {@link #isReadOnly()} method to check if this
     *           {@code Repository} is read-only. If it returns {@code false},
     *           throws an {@code UnsupportedOperationException}.</p></li>
     *
     *   <li><p> Perform the actual installation of the module archive and this
     *           is repository implementation specific.</p></li>
     *
     *   <li><p> If the module archive is installed successfully, invoke the
     *           {@link #addModuleArchiveInfo(ModuleArchiveInfo)} method.
     *           </p></li>
     * </ol>
     *
     * <p> If the repository implementations change the set of module
     * definitions after the installation of the module archive, they should
     * perform the following:
     * <p><ul>
     *   <li><p> If a new module definition is added, invoke the
     *           {@link #addModuleDefinition(ModuleDefinition)} method with
     *           that module definition.
     *           </p></li>
     *
     *   <li><p> If an existing module definition is replaced, invoke the
     *           {@link #removeModuleDefinition(ModuleDefinition)}
     *           method with the old module definition, and invoke the
     *           {@link #addModuleDefinition(ModuleDefinition)}
     *           method with the new module definition.</p></li>
     * </ul>
     * The default implementation of this method throws an
     * {@code UnsupportedOperationException}.
     *
     * @param uri the URI to the module archive.
     * @return the {@code ModuleArchiveInfo} object that represents the
     *         installed module archive.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to install a
     *         module archive in this {@code Repository}.
     * @throws UnsupportedOperationException if {@code Repository} does
     *         not support this operation.
     * @throws ModuleFormatException if the module archive format is not
     *         supported by this implementation.
     * @throws IllegalStateException if the same module archive is
     *         already installed, or if this
     *         {@code Repository} is not active.
     * @throws IOException if an I/O error occurs while installing the module
     *         archive.
     */
    public ModuleArchiveInfo install(URI uri) throws IOException {
        throw new UnsupportedOperationException("Repository does not support the install operation.");
    }

    /**
     * Uninstall a module archive from this {@code Repository}. This method
     * should be overridden by repository implementations as follows:
     * <p><ol>
     *   <li><p> If a security manager is present, calls the security
     *           manager's {@code checkPermission} method with a
     *           {@code ModuleSystemPermission("uninstallModuleArchive")}
     *           permission to ensure it's ok to return the information of the
     *           installed module archives in this {@code Repository}.</p></li>
     *
     *   <li><p> If {@code mai} is {@code null}, throws a
     *           {@code NullPointerException}.</p></li>
     *
     *   <li><p> If this {@code Repository} is not
     *           {@linkplain #isActive active},
     *           throws an {@code IllegalStateException}.</p></li>
     *
     *   <li><p> Invoke the {@link #isReadOnly()} method to check if this
     *           {@code Repository} is read-only. If it returns {@code false},
     *           throws an {@code UnsupportedOperationException}.</p></li>
     *
     *   <li><p> Perform the actual uninstallation of the module archive and
     *           this is repository implementation specific.</p></li>
     *
     *   <li><p> If the module archive is uninstalled successfully, invoke the
     *           {@link #removeModuleArchiveInfo(ModuleArchiveInfo)} method.
     *           </p></li>
     * </ol>
     *
     * <p> If the repository implementations change the set of module
     * definitions after the uninstallation of the module archive, they
     * should perform the following:
     * <p><ul>
     *   <li><p> If an existing module definition is removed, invoke the
     *           {@link #removeModuleDefinition(ModuleDefinition)} method
     *           with that module definition.
     *           </p></li>
     *
     *   <li><p> If an existing module definition is replaced, invoke the
     *           {@link #removeModuleDefinition(ModuleDefinition)}
     *           method with the old module definition, and invoke the
     *           {@link #addModuleDefinition(ModuleDefinition)}
     *           method with the new module definition.</p></li>
     * </ul>
     * The default implementation of this method throws an
     * {@code UnsupportedOperationException}.
     *
     * @param mai the module archive to be uninstalled.
     * @return true if the module archive is found and uninstalled, returns
     *         false otherwise.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to uninstall the
     *         module archive in this {@code Repository}.
     * @throws UnsupportedOperationException if this {@code Repository} does
     *         not support this operation.
     * @throws IllegalStateException if this {@code Repository} is not active.
     * @throws IOException If an I/O error occurs while uninstalling the module
     *         archive.
     */
    public boolean uninstall(ModuleArchiveInfo mai) throws IOException {
        throw new UnsupportedOperationException("Repository does not support the uninstall operation.");
    }

    /**
     * Reload the module archives in this {@code Repository}. This method
     * should be overridden and implemented by repository implementations
     * as follows:
     * <p><ol>
     *   <li><p> If a security manager is present, calls the security
     *           manager's {@code checkPermission} method with a
     *           {@code ModuleSystemPermission("reloadRepository")}
     *           permission to ensure it's ok to reload the module
     *           definitions in this {@code Repository}.</p></li>
     *
     *   <li><p> If this {@code Repository} is not
     *           {@linkplain #isActive active},
     *           throws an {@code IllegalStateException}.</p></li>
     *
     *   <li><p> Invoke the {@link #supportsReload()} method to check if this
     *           {@code Repository} supports reload. If it returns {@code false},
     *           throws an {@code UnsupportedOperationException}.</p></li>
     *
     *   <li><p> Perform the actual reload of module definitions and this is
     *           repository implementation specific.</p></li>
     * </ol>
     * <p> If the repository implementations change the set of module archives
     * after reload, they should perform the following:
     * <p><ul>
     *   <li><p> If a new module archive is added, invoke the
     *           {@link #addModuleArchiveInfo(ModuleArchiveInfo)}
     *           method with that module archive's information.
     *           </p></li>
     *
     *   <li><p> If an existing module archive is removed, invoke the
     *           {@link #removeModuleArchiveInfo(ModuleArchiveInfo)} method
     *           with that module archive's information.
     *           </p></li>
     *
     *   <li><p> If an existing module archive is replaced, invoke the
     *           {@link #removeModuleArchiveInfo(ModuleArchiveInfo)}
     *           method with the old module archive's information, and invoke the
     *           {@link #addModuleArchiveInfo(ModuleArchiveInfo)}
     *           method with the new module archive's information.</p></li>
     * </ul>
     * <p> If the repository implementations change the set of module
     * definitions after reload, they should perform the following:
     * <p><ul>
     *   <li><p> If a new module definition is added, invoke the
     *           {@link #addModuleDefinition(ModuleDefinition)}
     *           method with that module definition.
     *           </p></li>
     *
     *   <li><p> If an existing module definition is removed, invoke the
     *           {@link #removeModuleDefinition(ModuleDefinition)} method
     *           with that module definition.
     *           </p></li>
     *
     *   <li><p> If an existing module definition is replaced, invoke the
     *           {@link #removeModuleDefinition(ModuleDefinition)}
     *           method with the old module definition, and invoke the
     *           {@link #addModuleDefinition(ModuleDefinition)}
     *           method with the new module definition.</p></li>
     * </ul>
     * The default implementation of this method throws an
     * {@code UnsupportedOperationException}.
     *
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to reload module
     *         definitions in this {@code Repository}.
     * @throws UnsupportedOperationException if this {@code Repository}
     *         does not support this operation.
     * @throws IllegalStateException if a module archive is in use thus
     *         cannot be reloaded, or if this {@code Repository} is not
     *         active.
     * @throws IOException If an I/O error occurs while reloading the module
     *         archives.
     */
    public void reload() throws IOException {
        throw new UnsupportedOperationException("Repository does not support the reload operation.");
    }

    /**
     * Adds a module definition into this {@code Repository}. Returns
     * {@code true} if the module definitions in this
     * {@code Repository} changed as a result of the call, and fires
     * a {@link RepositoryEvent.Type#MODULE_DEFINITION_ADDED
     * <tt>MODULE_DEFINITION_ADDED</tt>} event. Returns {@code false}
     * if this {@code Repository} already contains
     * the specified module definition.
     *
     * @param moduleDef module definition to be added
     * @return true if the module definitions in this {@code Repository}
     *         changed as a result of the call.
     * @throws IllegalStateException if this {@code Repository} has been
     *         shutdown.
     */
    protected final boolean addModuleDefinition(ModuleDefinition moduleDef) {
        Set<ModuleDefinition> mds = new HashSet<ModuleDefinition>();
        mds.add(moduleDef);
        return addModuleDefinitions(mds);
    }

    /**
     * Adds a set of module definitions into this {@code Repository}.
     * Returns {@code true} if the module definitions in this
     * {@code Repository} changed as a result of the call, and fires
     * a single {@link RepositoryEvent.Type#MODULE_DEFINITION_ADDED
     * <tt>MODULE_DEFINITION_ADDED</tt>} event. Returns {@code false}
     * if this {@code Repository} already contains
     * one of more of the specified module definitions.
     *
     * @param moduleDef a set of module definitions to be added
     * @return true if the module definitions in this {@code Repository}
     *         changed as a result of the call.
     * @throws IllegalStateException if this {@code Repository} has been
     *         shutdown.
     */
    protected final boolean addModuleDefinitions(Set<ModuleDefinition> mds) {
        assertNotShutdown();

        if (mds.size() == 0) {
            return false;
        }

        for (ModuleDefinition moduleDef : mds) {
            if (moduleDefs.contains(moduleDef)) {
                return false;
            }
        }

        // Add module definition into internal data structure
        List<ModuleDefinition> newModuleDefs = new ArrayList<ModuleDefinition>(moduleDefs);
        newModuleDefs.addAll(mds);
        moduleDefs = Collections.unmodifiableList(newModuleDefs);

        // Send a MODULE_DEFINITION_ADDED event
        RepositoryEvent evt = new RepositoryEvent(this,
                                    RepositoryEvent.Type.MODULE_DEFINITION_ADDED,
                                    null, mds);
        processEvent(evt);

        return true;
    }

    /**
     * Removes a module definition from this {@code Repository}. Returns
     * {@code true} if the module definitions in this
     * {@code Repository} changed as a result of the call, and fires a
     * {@link RepositoryEvent.Type#MODULE_DEFINITION_REMOVED
     * <tt>MODULE_DEFINITION_REMOVED</tt>} event. Returns {@code false}
     * if the specified module definition is not present in this
     * {@code Repository}.
     * <p>
     * If the module definition is removed successfully, this method invokes
     * its module system's
     * {@link ModuleSystem#disableModuleDefinition(ModuleDefinition)
     * <tt>disableModuleDefinition(ModuleDefinition)</tt>} method and the
     * {@link ModuleSystem#releaseModule(ModuleDefinition)
     * <tt>releaseModule(ModuleDefinition)</tt>} method.</p></li>
     *
     * @param moduleDef module definition to be removed
     * @return true if the module definitions in this {@code Repository}
     *         changed as a result of the call.
     * @throws IllegalStateException if this {@code Repository} has been
     *         shutdown.
     */
    protected final boolean removeModuleDefinition(final ModuleDefinition moduleDef) {
        Set<ModuleDefinition> mds = new HashSet<ModuleDefinition>();
        mds.add(moduleDef);
        return removeModuleDefinitions(mds);
    }

    /**
     * Removes a set of module definitions from this {@code Repository}.
     * Returns {@code true} if the module definitions in this
     * {@code Repository} changed as a result of the call, and fires a single
     * {@link RepositoryEvent.Type#MODULE_DEFINITION_REMOVED
     * <tt>MODULE_DEFINITION_REMOVED</tt>} event. Returns {@code false} if
     * any of the specified module definitions is not present in this
     * {@code Repository}.
     * <p>
     * For each module definition that is removed successfully, this method
     * invokes its module system's
     * {@link ModuleSystem#disableModuleDefinition(ModuleDefinition)
     * <tt>disableModuleDefinition(ModuleDefinition)</tt>}
     * method and
     * {@link ModuleSystem#releaseModule(ModuleDefinition)
     * <tt>releaseModule(MOduleDefinition)</tt>} method.
     *
     * @param mds the set of module definitions to be removed
     * @return true if the module definitions in this {@code Repository}
     *         changed as a result of the call.
     * @throws IllegalStateException if this {@code Repository} has been
     *         shutdown.
     */
    protected final boolean removeModuleDefinitions(final Set<ModuleDefinition> mds) {
        assertNotShutdown();

        if (mds.size() == 0 || moduleDefs.containsAll(mds) == false) {
            return false;
        }

        // Remove module definition from internal data structure
        List<ModuleDefinition> newModuleDefs = new ArrayList<ModuleDefinition>(moduleDefs);
        newModuleDefs.removeAll(mds);
        moduleDefs = Collections.unmodifiableList(newModuleDefs);

        // Send MODULE_DEFINITION_REMOVED event
        RepositoryEvent evt = new RepositoryEvent(this,
                                    RepositoryEvent.Type.MODULE_DEFINITION_REMOVED,
                                    null, mds);
        processEvent(evt);

        // disable and release module definitions under doPrivileged()
        AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                for (ModuleDefinition moduleDef : mds) {
                    try {
                        // Disables the module definition in the module system.
                        moduleDef.getModuleSystem().disableModuleDefinition(moduleDef);
                    } catch (UnsupportedOperationException uoe) {
                        // no-op
                    } catch (IllegalStateException ise) {
                        // no-op
                    }
                    try {
                        // Releases any module instance corresponding to the module definition
                        // in the module system
                        moduleDef.getModuleSystem().releaseModule(moduleDef);
                    } catch (UnsupportedOperationException uoe) {
                        // no-op
                    }
                }

                return Boolean.TRUE;
            }
        });

        return true;
    }

    /**
     * Adds a {@code ModuleArchiveInfo} into this {@code Repository}. Returns
     * {@code true} if the {@code ModuleArchiveInfo}s in this
     * {@code Repository} changed as a result of the call, and fires
     * a {@link RepositoryEvent.Type#MODULE_ARCHIVE_INSTALLED
     * <tt>MODULE_ARCHIVE_INSTALLED</tt>} event. Returns {@code false}
     * if this {@code Repository} already contains the specified
     * {@code ModuleArchiveInfo}.
     *
     * @param mai {@code ModuleArchiveInfo} to be added
     * @return true if the {@code ModuleArchiveInfo}s in this
     *         {@code Repository} changed as a result of the call.
     * @throws IllegalStateException if this {@code Repository} has been
     *         shutdown.
     */
    protected final boolean addModuleArchiveInfo(ModuleArchiveInfo mai) {
        assertNotShutdown();

        if (moduleArchiveInfos.contains(mai)) {
            return false;
        }

        // Add module archive info into internal data structure
        List<ModuleArchiveInfo> newModuleArchiveInfos = new ArrayList<ModuleArchiveInfo>(moduleArchiveInfos);
        newModuleArchiveInfos.add(mai);
        moduleArchiveInfos = Collections.unmodifiableList(newModuleArchiveInfos);

        // Send MODULE_ARCHIVE_INSTALLED event
        RepositoryEvent evt = new RepositoryEvent(this,
                                    RepositoryEvent.Type.MODULE_ARCHIVE_INSTALLED,
                                    mai, null);
        processEvent(evt);

        return true;
    }

    /**
     * Removes a {@code ModuleArchiveInfo} from this {@code Repository}.
     * Returns {@code true} if the {@code ModuleArchiveInfo}s in this
     * {@code Repository} changed as a result of the call, and fires
     * a {@link RepositoryEvent.Type#MODULE_ARCHIVE_UNINSTALLED
     * <tt>MODULE_ARCHIVE_UNINSTALLED</tt>} event. Returns {@code false}
     * if the specified {@code ModuleArchiveInfo} is not present in this
     * {@code Repository}.
     *
     * @param mai {@code ModuleArchiveInfo} to be removed
     * @return true if the {@code ModuleArchiveInfo}s in this {@code Repository}
     *         changed as a result of the call.
     * @throws IllegalStateException if this {@code Repository} has been
     *         shutdown.
     */
    protected final boolean removeModuleArchiveInfo(ModuleArchiveInfo mai) {
        assertNotShutdown();

        if (moduleArchiveInfos.contains(mai) == false) {
            return false;
        }

        // Remove module archive info from internal data structure
        List<ModuleArchiveInfo> newModuleArchiveInfos = new ArrayList<ModuleArchiveInfo>(moduleArchiveInfos);
        newModuleArchiveInfos.remove(mai);
        moduleArchiveInfos = Collections.unmodifiableList(newModuleArchiveInfos);

        // Send MODULE_ARCHIVE_UNINSTALLED event
        RepositoryEvent evt = new RepositoryEvent(this,
                                    RepositoryEvent.Type.MODULE_ARCHIVE_UNINSTALLED,
                                    mai, null);
        processEvent(evt);

        return true;
    }

    /**
     * Returns an unmodifiable list of {@code ModuleArchiveInfo}s in this
     * {@code Repository}.
     *
     * @return an unmodifiable list of {@code ModuleArchiveInfo}s.
     * @throws IllegalStateException if this {@code Repository} has been
     *         shutdown.
     */
    protected final List<ModuleArchiveInfo> getModuleArchiveInfos() {
        assertNotShutdown();
        return moduleArchiveInfos;
    }

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
                    case MODULE_DEFINITION_ADDED:
                    case MODULE_DEFINITION_REMOVED:
                    case MODULE_ARCHIVE_INSTALLED:
                    case MODULE_ARCHIVE_UNINSTALLED:
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

    private void assertNotActive() throws IllegalStateException {
         if (isActive()) {
             throw new IllegalStateException("Repository is active.");
        }
    }

    private void assertNotShutdown() throws IllegalStateException {
         if (shutdown) {
             throw new IllegalStateException("Repository is already shutdown.");
        }
    }

    private void assertNotReadOnly() throws IllegalStateException {
        if (isReadOnly()) {
            throw new UnsupportedOperationException("Repository is read-only.");
        }
    }
}
