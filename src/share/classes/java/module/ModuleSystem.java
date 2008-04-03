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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * This class represents the module system which constructs a module
 * instance from a module definition.
 * <p>
 * @see java.module.Module
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystemListener
 * @see java.module.ModuleSystemPermission
 *
 * @since 1.7
 */
@java.util.Service
public abstract class ModuleSystem {

    private static ModuleSystem defaultImpl = null;

    /** Counter for generating Ids for module systems. */
    private static final AtomicLong idCounter = new AtomicLong();

    /** Id for this module system. */
    private final long id = idCounter.incrementAndGet();

    /**
     * Constructor used by subclasses.
     */
    protected ModuleSystem() {
        // empty
    }

    /**
     * Returns a long value that represents the unique identifier assigned to
     * this module system. The identifier is assigned by the JVM and is JVM
     * implementation dependent.
     *
     * @return a long value that represents the unique identifier assigned to
     *         this module system.
     */
    public final long getId() {
        return id;
    }

    /**
     * Returns a {@code Module} instance for the specified {@code ModuleDefinition}
     * in the {@code ModuleSystem}. The module is initialized and ready to use.
     * <p>
     * If there is an existing module instance for the specified
     * {@code ModuleDefinition}, that instance is returned.  Otherwise, a new module
     * instance is instantiated and returned.
     *
     * @param moduleDef a {@code ModuleDefinition}
     * @return a {@code Module} instance of the {@code ModuleDefinition}.
     * @throws ModuleInitializationException if the module instance cannot be initialized.
     * @throws IllegalStateException if the specified module definition
     *         has already been disabled.
     */
    public abstract Module getModule(ModuleDefinition moduleDef) throws ModuleInitializationException;

    /**
     * Releases an existing {@code Module} instance corresponding to the
     * specified {@code ModuleDefinition} in the {@code ModuleSystem}.
     * <p>
     * If there is an existing module instance for the specified module
     * definition, it will never be returned by the {@code ModuleSystem}
     * after this method returns. Further, if that module instance is
     * imported by other module instances, each of these importing module
     * instance will also be released.
     * <p>
     * If there is no module instance corresponding to the module
     * definition, calling this method has no effect.
     * <p>
     * {@code Module} instances corresponding to the {@code ModuleDefinition}
     * with name that begins with "java.", or from the bootstrap repository
     * cannot be released. {@code Module} instances corresponding to the
     * {@code ModuleDefinition} that its <code>isModuleReleasable</code>
     * method returns false also cannot be released.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's checkPermission method with a <code>
     * ModuleSystemPermission("releaseModule")</code> permission to ensure
     * it's ok to release the existing module instance of the specified
     * module definition.
     *
     * @param moduleDef a {@code ModuleDefinition}.
     * @throws SecurityException if a security manager exists and its
     *         checkPermission method denies access to release the
     *         module instance of the specified module definition.
     * @throws UnsupportedOperationException if the existing module
     *         instance cannot be released.
     */
    public abstract void releaseModule(ModuleDefinition moduleDef);

    /**
     * Disables the specified {@code ModuleDefinition} in the
     * {@code ModuleSystem}.
     * <p>
     * The ModuleDefinition is {@link #releaseModule released} and marked to
     * disallow creation of new {@code Module} instances. Subsequent calls
     * to {@link #getModule getModule} with this ModuleDefinition throw an
     * IllegalStateException.
     * <p>
     * {@code ModuleDefinition} with name that begins with "java.",
     * or from the bootstrap repository cannot be disabled.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's checkPermission method with a <code>
     * ModuleSystemPermission("disableModuleDefinition")</code> permission to
     * ensure it's ok to disable the specified module definition in the
     * module system.
     *
     * @param moduleDef a {@code ModuleDefinition}.
     * @throws SecurityException if a security manager exists and its
     *         checkPermission method denies access to disable the
     *         specified module definition in the module system.
     * @throws UnsupportedOperationException if the specified module
     *         definition cannot be disabled.
     * @throws IllegalStateException if the specified module definition
     *         has already been disabled.
     */
    public abstract void disableModuleDefinition(ModuleDefinition moduleDef);

    /**
     * Returns the system's default module system.
     * <p>
     * The default class of the module system can be overridden using the
     * <code>java.module.ModuleSystem</code> system property.
     *
     * @return the system's default module system.
     */
    public static synchronized ModuleSystem getDefault() {
        if (defaultImpl == null) {
            defaultImpl = sun.module.core.ModuleSystemImpl.INSTANCE;
        }
        // TODO: check system property
        return defaultImpl;
    }

    // Module system listener(s)
    private static ModuleSystemListener moduleSystemListener = null;
    private static Object listenerSyncObject = new Object();

    /**
     * Adds the specified module system listener to receive module system
     * events from the module systems.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's checkPermission method with a <code>
     * ModuleSystemPermission("addModuleSystemListener")</code> permission to
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
     * manager's checkPermission method with a <code>
     * ModuleSystemPermission("removeModuleSystemListener")</code> permission
     * to ensure it's ok to remove a module system listener from the module
     * systems.
     *
     * @param listener the module system listener
     * @throws SecurityException if a security manager exists and its
     *         checkPermission method denies access to remove a module system
     *         listener from the module systems.
     * @throws NullPointerException if listener is null.
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
     * Processes module system event occuring in this module system by
     * dispatching them to any registered ModuleSystemListener objects.
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
