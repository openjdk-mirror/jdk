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

import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * This class represents a module system.
 * <p>
 * A module system within the Java Module System is responsible for the
 * creation, management, and release of
 * {@linkplain Module module instances}. A module system creates
 * module instances from {@linkplain ModuleDefinition module definitions},
 * and a module system must
 * <a href="#Initialization">initialize</a> the module instances it creates.
 *
 * <a name="Initialization"><h3>Module initialization</h3></a>
 *
 * A module system implementation involves performing two tasks on a module
 * instance during module initialization:
 * <a href="#Resolution"><i>resolution</i></a> and
 * <a href="#ShallowValidation"><i>type consistency validation</i></a>.
 * A module instance is <i>fully initialized</i> if the module
 * initialization completes successfully.
 *
 * <a name="Resolution"><h4>Resolution</h4></a>
 * <i>Resolution</i> is the process of resolving the imports of a module instance.
 * During resolution, a module system implementation must select a list of
 * imported module definitions, based on the
 * {@linkplain ModuleDefinition#getImportDependencies() import dependencies}
 * of the module instance, and the module definitions available from the
 * repositories. The selection policy is module system specific. A module
 * system implementation must use the repository which the
 * initializing module belongs to search candidate modules for the imports.
 * <p>
 * Module system implementations must be able to resolve
 * {@linkplain ModuleDependency module dependencies} in a module instance.
 * A module system implementation may be able to resolve other types
 * of import dependency, e.g.
 * {@linkplain PackageDependency package dependency}.
 * A module system implementation must cause the initialization
 * to fail if it can not recognize the type of import dependency in a
 * module instance.
 * <p>
 * Support for cyclic dependencies is module systems specific.
 * Module system implementations may be able to resolve cyclic dependencies
 * between module definitions only in the same repository. A module system
 * implementation may be able to resolve cyclic dependencies between
 * module definitions only from the same module system. A module system
 * implementation may be able to resolve cyclic dependencies
 * between module definitions from different module systems.
 * <p>
 * Module system implementations must support
 * {@linkplain ImportDependency#isOptional() optional} dependencies; a
 * module system implementation must ignore an optional dependency if no
 * module definition in the repository can satisfy the dependency.
 * <p>
 * Module system implementations must support
 * {@linkplain ModuleDependency#isReexported() re-exported} module
 * dependencies. If module <i>M</i> imports module <i>N</i> which
 * imports module <i>O</i> through module dependency and
 * <i>N</i> re-exports <i>O</i>, the module system implementation
 * of <i>M</i> must automatically import <i>O</i> in <i>M</i>.
 *
 * <a name="TypeConsistencyValidation"><h4>Type consistency validation</h4></a>
 * <i>Type consistency validation</i> is the process by which a module system
 * checks the type consistency requirement with a module instane and its
 * imported modules. There are two kinds:
 * <i>shallow validation</i> and <i>deep validation</i>.
 * <p>
 * <i>Shallow validation</i> is the process that enforces the
 * minimal type consistency requirement with a module instance and its
 * imported modules. The minimal type consistency requirement is module
 * system specific. This process examines the member types in module instance
 * <i>M</i> and the exported types from all the module instances imported by
 * <i>M</i> to ensure that <i>M</i> maintains the minimal type consistency
 * requirement as defined by <i>M</i>'s module system.
 * <p>
 * <a name="DeepValidation">
 * <i>Deep validation</i></a> is the process that enforces the maximum type
 * consistency requirement with a module instance and its imported modules.
 * This process examines the member types in the module instance and all the
 * member types from the module instances imported transitively by this
 * module instance. The maximum type consistency requirement is
 * that the sets of the member types in all these module instances must be
 * mutually disjoint. A module instance passing <i>deep validation</i>
 * must always pass <i>shallow validation</i>.
 * <p>
 * <pre>
 * </pre>
 * Module system implementations must support <i>resolution</i> and
 * <i>shallow validation</i>. Module system implementations may support
 * <i>deep validation</i>.
 * <p>
 * Module system implementations must perform <i>resolution</i>, and
 * one of the <i>type consistency validations</i> during
 * module initialization. A module system implementation may perform
 * <i>resolution</i> and <i>type consistency validation</i>
 * in different order or in one or more iterations during module
 * initialization.
 *
 * @see java.module.Module
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystemListener
 * @see java.module.ModuleSystemPermission
 *
 * @since 1.7
 */
@java.util.Service
public abstract class ModuleSystem {

    // Module definitions that have been disabled.
    private final WeakHashMap<ModuleDefinition, Boolean> disabledModuleDefs = new WeakHashMap<ModuleDefinition, Boolean>();


    /**
     * Creates a {@code ModuleSystem} instance.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with
     * {@code ModuleSystemPermission("createModuleSystem")} permission to
     * ensure it's ok to create a module system.
     *
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to create a new
     *         module system instance.
     */
    protected ModuleSystem() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("createModuleSystem"));
        }
    }

    /**
     * Returns a {@code Module} instance for the specified
     * {@code ModuleDefinition} in this {@code ModuleSystem}.
     * Module system implementations must return a {@code Module}
     * instance which is fully initialized.
     * <p>
     * Module system implementations may instantiate, initialize,
     * and return a new {@code Module} instance. Module
     * system implementations must ensure that this new {@code Module}
     * instance is strongly reachable from the module system.
     * Module system implementations must also fire a
     * {@link ModuleSystemEvent.Type#MODULE_INITIALIZED
     * <tt>MODULE_INITIALIZED</tt>} event for this {@code Module}
     * instance.
     * <p>
     * Module system implementations may instantiate a new
     * {@code Module} instance but fail to initialize it.
     * Module system implementations must fire a
     * {@link ModuleSystemEvent.Type#MODULE_INITIALIZATION_EXCEPTION
     * <tt>MODULE_INITIALIZATION_EXCEPTION</tt>} event for this {@code Module}
     * instance.
     * <p>
     * Module system implementations may return an existing
     * {@code Module} instance to maximize sharing if the
     * {@code Module} instance has not been
     * {@linkplain #releaseModule(ModuleDefinition) released}
     * from the module system.
     *
     * @param moduleDef the {@code ModuleDefinition} which designates the
     *        {@code Module} to be returned
     * @return a {@code Module} instance corresponding to
     *         {@code ModuleDefinition}.
     * @throws ModuleInitializationException if the {@code Module} instance
     *         cannot be initialized.
     * @throws IllegalStateException if the specified {@code ModuleDefinition}
     *         has been disabled.
     * @throws IllegalArgumentException if the specified
     *         {@code ModuleDefinition} is associated with another
     *         module system, or with a repository which this
     *         module system does not support.
     */
    public abstract Module getModule(ModuleDefinition moduleDef) throws ModuleInitializationException;

    /**
     * Returns a list of {@code Module} instances for the specified
     * {@code ModuleDefinition}s in this {@code ModuleSystem}.
     * Module system implementations must return a list of
     * {@code Module} instances which are fully initialized.
     * Each {@code Module} instance in the returned list must be
     * a module instance of the corresponding {@code ModuleDefinition} in the
     * specified {@code moduleDefs}.
     * <p>
     * For each {@code ModuleDefinition} in the specified {@code moduleDefs},
     * a module system implementation
     * may instantiate, initialize and return a new {@code Module} instance
     * in the returned list. The module system implementation must ensure that
     * this new {@code Module} instance is strongly reachable from the module
     * system. The module system implementation must also fire a
     * {@link ModuleSystemEvent.Type#MODULE_INITIALIZED
     * <tt>MODULE_INITIALIZED</tt>} event for this {@code Module}
     * instance.
     * <p>
     * For each {@code ModuleDefinition} in the specified {@code moduleDefs},
     * a module system implementation
     * may instantiate a new {@code Module} instance but fail to initialize it.
     * The module system implementation must fire a
     * {@link ModuleSystemEvent.Type#MODULE_INITIALIZATION_EXCEPTION
     * <tt>MODULE_INITIALIZATION_EXCEPTION</tt>} event for this {@code Module}
     * instance.
     * <p>
     * For each {@code ModuleDefinition} in the specified {@code moduleDefs},
     * a module system implementation
     * may return an existing {@code Module} instance in the returned list if
     * the {@code Module} instance has not been
     * {@linkplain #releaseModule(ModuleDefinition) released} from the module system.
     *
     * @param importer the {@code ModuleDefinition} which imports
     * @param moduleDefs the {@code ModuleDefinition} which designates the
     *        list of {@code Module}s to be returned
     * @return a list of {@code Module} instances corresponding to
     *         {@code ModuleDefinition}s.
     * @throws ModuleInitializationException if a {@code Module} instance
     *         cannot be initialized.
     * @throws IllegalStateException if one or more {@code ModuleDefinition}s
     *         in {@code moduleDefs} have been disabled.
     * @throws IllegalArgumentException if one or more
     *         {@code ModuleDefinition}s in {@code moduleDefs} are
     *         associated with another module system, or with a
     *         repository which this module system does not support.
     */
    public abstract List<Module> getModules(ModuleDefinition importer, List<ModuleDefinition> moduleDefs) throws ModuleInitializationException;

    /**
     * Releases all existing {@code Module} instance(s) corresponding to the
     * specified {@code ModuleDefinition} in this {@code ModuleSystem}.
     * <p>
     * A module system implementation may have one or more existing
     * {@code Module} instances corresponding to the specified
     * {@code ModuleDefinition} in the module system.
     * <p>
     * A module system implementation must release an existing {@code Module}
     * instance of the specified {@code ModuleDefinition} if the
     * {@code Module} instance is
     * {@link ModuleDefinition#isModuleReleasable() releasable}.
     * The module system implementation must ensure that a released
     * {@code Module} instance is not strongly reachable from the
     * module system.
     * <p>
     * A module system implementation releasing an existing
     * {@code Module} instance must also release the importing module instances
     * transitively from the importers' module systems.
     * <p>
     * A module system implementation releasing an existing
     * {@code Module} instance must fire a
     * {@link ModuleSystemEvent.Type#MODULE_RELEASED
     * <tt>MODULE_RELEASED</tt>} event for this {@code Module} instance.
     * <p>
     * {@linkplain Module#getClassLoader() Module class loader} of this
     * {@code Module} must continue to make all classes and resources in
     * this {@code Module} visible to its importing module instances
     * after this {@code Module} instance is released.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("releaseModule")} permission to ensure
     * it's ok to release the existing {@code Module} instance of the
     * specified {@code ModuleDefinition} in this {@code ModuleSystem}.
     *
     * @param moduleDef a {@code ModuleDefinition} object.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to release the
     *         {@code Module} instance of the specified
     *         {@code ModuleDefinition}.
     * @throws UnsupportedOperationException if the existing module
     *         instance is not releasable.
     * @throws IllegalArgumentException if the specified
     *         {@code ModuleDefinition} is associated with another
     *         module system, or with a repository which this
     *         module system does not support.
     */
    public abstract void releaseModule(ModuleDefinition moduleDef);

    /**
     * Disables the specified {@code ModuleDefinition} in this
     * {@code ModuleSystem}.
     * <p>
     * A module system implementation must not instantiate any
     * new {@code Module} instance from the {@code ModuleDefinition}
     * after the {@code ModuleDefinition} is disabled.
     * <p>
     * A module system implementation disabling a {@code ModuleDefinition}
     * must fire a
     * {@link ModuleSystemEvent.Type#MODULE_DEFINITION_DISABLED
     * <tt>MODULE_DEFINITION_DISABLED</tt>} event for the
     * {@code ModuleDefinition}.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("disableModuleDefinition")}
     * permission to ensure it's ok to disable the specified
     * {@code ModuleDefinition} in this {@code ModuleSystem}.
     *
     * @param moduleDef the {@code ModuleDefinition} to be disabled.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to disable the
     *         specified {@code ModuleDefinition} in this {@code ModuleSystem}.
     * @throws UnsupportedOperationException if the specified
     *         {@code ModuleDefinition} cannot be disabled.
     * @throws IllegalStateException if the specified {@code ModuleDefinition}
     *         has already been disabled.
     * @throws IllegalArgumentException if the specified
     *         {@code ModuleDefinition} is associated with another
     *         module system, or with a repository which this
     *         module system does not support.
     */
    public void disableModuleDefinition(ModuleDefinition moduleDef) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("disableModuleDefinition"));
        }
        if (moduleDef.getModuleSystem() != this) {
            throw new IllegalArgumentException("Module definition is associated with another module system.");
        }
        synchronized(disabledModuleDefs) {
            if (disabledModuleDefs.containsKey(moduleDef)) {
                throw new IllegalStateException("Module definition has already been disabled.");
            }
            disabledModuleDefs.put(moduleDef, Boolean.TRUE);
        }

        // Send MODULE_DEFINITION_DISABLED event
        ModuleSystemEvent evt = new ModuleSystemEvent(this,
                                    ModuleSystemEvent.Type.MODULE_DEFINITION_DISABLED,
                                    null, moduleDef, null);
        this.processEvent(evt);
    }

    /**
     * Returns true if the specified {@code ModuleDefinition} is disabled in this
     * {@code ModuleSystem}; otherwise, returns false.
     *
     * @param moduleDef the {@code ModuleDefinition}.
     * @return true if the specified {@code ModuleDefinition} is disabled in this
     *         {@code ModuleSystem}; otherwise, returns false.
     * @throws IllegalArgumentException if the specified
     *         {@code ModuleDefinition} is associated with another
     *         module system, or with a repository which this
     *         module system does not support.
     */
    public boolean isModuleDefinitionDisabled(ModuleDefinition moduleDef) {
        if (moduleDef.getModuleSystem() != this) {
            throw new IllegalArgumentException("Module definition is associated with another module system.");
        }
        synchronized(disabledModuleDefs) {
            return disabledModuleDefs.containsKey(moduleDef);
        }
    }

    // Module system listener(s)
    private static ModuleSystemListener moduleSystemListener = null;
    private static Object listenerSyncObject = new Object();

    /**
     * Adds the specified module system listener to receive module system
     * events from the module systems.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("addModuleSystemListener")} permission to
     * ensure it's ok to add a module system listener to the module systems.
     *
     * @param listener the module system listener
     * @throws SecurityException if a security manager exists and its
     *         checkPermission method denies access to add a module system
     *         listener to the module systems.
     * @throws NullPointerException if listener is null.
     */
    public static final void addModuleSystemListener(ModuleSystemListener listener) {
        if (listener == null) {
            throw new NullPointerException("Module system listener must not be null.");
        }

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("addModuleSystemListener"));
        }

        synchronized(listenerSyncObject) {
            moduleSystemListener = EventMulticaster.add(moduleSystemListener, listener);
        }
    }

    /**
     * Removes the specified module system listener so that it no longer
     * receives module system events from the module systems.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a {@code
     * ModuleSystemPermission("removeModuleSystemListener")} permission
     * to ensure it's ok to remove a module system listener from the module
     * systems.
     *
     * @param listener the module system listener
     * @throws SecurityException if a security manager exists and its
     *         checkPermission method denies access to remove a module system
     *         listener from the module systems.
     */
    public static final void removeModuleSystemListener(ModuleSystemListener listener) {
        if (listener == null) {
            throw new NullPointerException("Module system listener must not be null.");
        }

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("removeModuleSystemListener"));
        }

        synchronized(listenerSyncObject) {
            moduleSystemListener = EventMulticaster.remove(moduleSystemListener, listener);
        }
    }

    /**
     * Obtains the default thread factory when ModuleSystem.class is loaded, so
     * the thread factory would be associated with the main thread group for
     * creating threads.
     */
    private static final ThreadFactory threadFactory = Executors.defaultThreadFactory();

    /**
     * ModuleSystemEventHandler that is executed in the event dispatch thread.
     */
    private static class ModuleSystemEventHandler implements Runnable {
        private ModuleSystemListener listener;
        private ModuleSystemEvent event;
        ModuleSystemEventHandler(ModuleSystemListener listener, ModuleSystemEvent event) {
            this.listener = listener;
            this.event = event;
        }
        public void run() {
            if (listener != null) {
                switch (event.getType()) {
                    case MODULE_INITIALIZED:
                    case MODULE_RELEASED:
                    case MODULE_INITIALIZATION_EXCEPTION:
                    case MODULE_DEFINITION_DISABLED:
                        listener.handleEvent(event);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private static ExecutorService executorService = null;

    /**
     * Processes module system event occurring in this {@code ModuleSystem} by
     * dispatching them to any registered {@code ModuleSystemListener} objects
     * asynchronously.
     *
     * @param event the module system event
     */
    protected final void processEvent(ModuleSystemEvent event) {
        ModuleSystemListener listener = moduleSystemListener;

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
        executorService.submit(new ModuleSystemEventHandler(listener, event));
    }
}
