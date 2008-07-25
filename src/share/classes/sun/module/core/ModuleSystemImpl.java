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

package sun.module.core;

import java.util.*;
import java.util.concurrent.*;
import java.module.*;

/**
 * Module system implementation.
 *
 * @since  1.7
 */
public final class ModuleSystemImpl extends ModuleSystem {

    final static boolean DEBUG = sun.module.JamUtils.DEBUG;

    // Singleton instance
    public static final ModuleSystem INSTANCE = new ModuleSystemImpl();

    // Map containing all Modules that have been created
    // Includes modules that are fully initialized, partially initialized,
    // and those in error state
    private final Map<ModuleDefinition,ModuleImpl> modules;

    // New modules that we need to start initializing
    // Separate queue from initializingModules because may be updated by a
    // different thread while we are iterating the initializingModules queue.
    // Also, we need a blocking queue so that we sleep when there is nothing to do
    private final BlockingQueue<ModuleImpl> newModules;

    private final Thread initializerThread;

    private final Initializer initializer;

    // Modules currently in the process of being initialized
    private final Deque<ModuleImpl> initializingModules;

    // Modules that just moved into error state
    private final List<ModuleImpl> newErrorModules;

    private ModuleSystemImpl() {
        modules = new IdentityHashMap<ModuleDefinition,ModuleImpl>();
        newModules = new LinkedBlockingQueue<ModuleImpl>();
            initializingModules = new LinkedBlockingDeque<ModuleImpl>();
                newErrorModules = new ArrayList<ModuleImpl>();
                    // XXX doPrivileged()
                    // XXX ThreadGroup?
                    initializer = new Initializer();
        initializerThread = new Thread(null, initializer, "Module Initialization Thread");
        initializerThread.setDaemon(true);
        initializerThread.start();
    }

    @Override
    public synchronized void releaseModule(ModuleDefinition moduleDef) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("releaseModule"));
        }
        if (moduleDef.getModuleSystem() != this) {
            throw new IllegalArgumentException("Module definition is associated with another module system.");
        }
        if (moduleDef.isModuleReleasable() == false) {
            throw new UnsupportedOperationException("Module instance is not releasable.");
        }
        Module moduleToRelease = modules.get(moduleDef);
        if (moduleToRelease == null) {
            // There is no module instance that is fully initialized, partially
            // initialized, or in error state, which corresponds to the module
            // definition. Therefore, release module instance is a no-op.
            return;
        }

        // Otherwise, module instance is either fully initialized, partially
        // initialized, or in error state. Call getModule() to either a
        // fully initialized module instance, or obtain an initialization
        // exception.
        try {
            getModuleInternal(moduleDef);
        } catch(ModuleInitializationException mie) {
            // No module instance is instantiated successfully, thus nothing
            // to release.
            return;
        }

        // Determine the transitive closure of the importing modules using
        // BFS (Breadth-First-Search)
        Set<Module> importingModulesClosure = ModuleUtils.findImportingModulesClosure(moduleToRelease);

        // First, releases all importing module instances that are from
        // this module system.
        for (Module m : importingModulesClosure) {
            ModuleDefinition md = m.getModuleDefinition();
            if (md.getModuleSystem() != this) {
                continue;
            }
            if (md.isModuleReleasable() == false) {
                continue;
            }

            ModuleImpl mi = modules.get(md);

            // Visited modules may have been released previously through releaseModule()
            if (mi == null) {
                continue;
            }

            // Releases cached module instance to avoid potential recursion.
            modules.remove(md);

            // Removes references to all importing modules
            mi.removeImportingModules();

            // Invokes module initializer's release method
            mi.callReleaseOnModuleInitializer();

            // Send MODULE_RELEASED event
            ModuleSystemEvent evt = new ModuleSystemEvent(this,
                                        ModuleSystemEvent.Type.MODULE_RELEASED,
                                        mi, null, null);
            this.sendEvent(evt);
        }

        // Then, releases all all importing module instances that are from
        // other module systems.
        for (Module m : importingModulesClosure) {
            ModuleDefinition md = m.getModuleDefinition();
            ModuleSystem ms = md.getModuleSystem();
            if (ms == this) {
                    continue;
            }
            if (md.isModuleReleasable()) {
                    ms.releaseModule(md);
            }
        }
    }

    @Override
    public Module getModule(ModuleDefinition moduleDef) throws ModuleInitializationException {
        if (moduleDef.getModuleSystem() != this) {
            throw new IllegalArgumentException("Module definition is associated with another module system.");
        }
        // Check if the module definition has been disabled.
        //
        if (isModuleDefinitionDisabled(moduleDef)) {
            throw new IllegalStateException("Module definition has been disabled.");
        }
        return getModuleInternal(moduleDef);
    }

    @Override
    public List<Module> getModules(ModuleDefinition importer, List<ModuleDefinition> moduleDefs) throws ModuleInitializationException {
        for (ModuleDefinition moduleDef : moduleDefs) {
            if (moduleDef.getModuleSystem() != this) {
                throw new IllegalArgumentException("Module definition is associated with another module system.");
            }
            // Check if the module definition has been disabled.
            if (isModuleDefinitionDisabled(moduleDef)) {
                throw new IllegalStateException("Module definition has been disabled.");
            }
        }

        List<Module> result = new ArrayList<Module>();
        for (ModuleDefinition moduleDef : moduleDefs) {
            result.add(getModuleInternal(moduleDef));
        }
        return result;
    }

    /**
     * Internal method to get a module instance from a module definition.
     */
    private Module getModuleInternal(ModuleDefinition moduleDef) throws ModuleInitializationException {
        try {
            ModuleImpl m = getModuleInstance(moduleDef);
            if (m.initializationComplete() || (Thread.currentThread() != initializerThread)) {
                // Wait for the module initializer thread to ready the module
                // or if initialization is already complete, check if there
                // was an exception.
                m.awaitInitialization();
            } else {
                // We are being called from the module initializer thread.
                // This can happen if the execution of an ImportOverridePolicy
                // or an ImportPolicy causes other modules to be initialized.
                // In that case, drive the initialization directly.
                // Also, in order to catch recursive dependencies on the module
                // that caused this loop, temporarily remove it from the queue.
                ModuleImpl current = initializer.removeInitializingModule();
                try {
                    while (m.initializationComplete() == false) {
                        // We should never block. If we do, something is wrong.
                        initializer.serviceQueues(false);
                    }
                } finally {
                    initializer.restoreInitializingModule(current);
                }
                // call just to throw the exception if initialization failed
                m.awaitInitialization();
            }
            return m;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized ModuleImpl getModuleInstance(ModuleDefinition moduleDef) {
        ModuleImpl module = modules.get(moduleDef);
        if (module == null) {
            module = new ModuleImpl(this, moduleDef);
            modules.put(moduleDef, module);
            newModules.add(module);
        }
        return module;
    }

    // Remove modules in error state from the cache.
    // That ensures that if e.g. the missing dependency of the module is
    // installed in the repository, future attempts to obtain the module
    // will succeed.
    private synchronized void removeErrorModules() {
        for (ModuleImpl m : newErrorModules) {
             modules.remove(m.getModuleDefinition());
        }
        newErrorModules.clear();
    }

    void sendEvent(ModuleSystemEvent evt) {
        super.processEvent(evt);
    }

    // Initialization driver code.
    // Runs in a separate thread.
    private class Initializer implements Runnable {

        Initializer() {
            // empty
        }

        public void run() {
            while (true) {
                serviceQueues(true);
            }
        }

        /**
         * Drive the module initialization.
         *
         * The normal process is
         * (1) perform a blocking wait until newModules is non-empty
         * (2) transfer modules from newModules to initializingModules
         * (3) initialize all modules in initializingModules
         * (4) initializingModules is now empty, go to step 1
         *
         * However, code in an ImportOverridePolicy or ImportPolicy can cause
         * module initialization. If that happens, it is possible for
         * newModules to be empty and initializingModules to be non-empty.
         * The logic here needs to deal with that.
         */
        void serviceQueues(boolean mayBlock) {
            // Move modules from the newModules queue to the
            // initializingModules queue
            ModuleImpl m = newModules.poll();
            if ((m != null) || initializingModules.isEmpty()) {
                try {
                    if (m == null) {
                        removeErrorModules();
                        // Queue is empty, need to block waiting for next module.
                        if (mayBlock == false) {
                            throw new AssertionError
                                ("Internal module error: would block");
                        }
                        m = newModules.take();
                    }
                    do {
                        initializingModules.addFirst(m);
                        m = newModules.poll();
                    } while (m != null);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            // Do the initialization
            initializeModules();
        }

        private ModuleImpl initializingModule;

        ModuleImpl removeInitializingModule() {
            ModuleImpl m = initializingModule;
            if (m == null) {
                throw new AssertionError("Internal module error: no recursion?");
            }
            initializingModules.remove(m);
            initializingModule = null;
            return m;
        }

        void restoreInitializingModule(ModuleImpl m) {
            initializingModules.add(m);
        }

        private void initializeModules() {
            // For each module, try to continue initialization.
            // Repeat as long as we make progress with at least one of the modules.
            // When we return, either all modules are initialized or
            // initialization has caused modules to be enqueued in newModules.
            boolean updated;
            do {
                updated = false;
                for (Iterator<ModuleImpl> t = initializingModules.iterator(); t.hasNext();) {
                    ModuleImpl m = t.next();
                    initializingModule = m;
                    // try to make progress with the initialization
                    if (m.initStep()) {
                        updated = true;
                        if (m.initializationComplete()) {
                            // if initialization is complete, remove from queue
                            t.remove();
                            if (m.inError()) {
                                newErrorModules.add(m);
                            }
                        }
                    }
                }
            } while (updated);
            // If the newModules queue is empty and there are initializingModules
            // remaining that we could not be completely initialize,
            // this can only be the result of a recursion caused by an
            // ImportPolicy/ImportOverridePolicy that has a circular dependency
            // on the module that caused the recursion, which was removed from
            // this list via removeInitializingModule().
            // This means that this module and all modules that depend on it
            // (the modules in the initializingModules) are invalid and we need
            // to put them into error state.
            if ((initializingModules.isEmpty() == false) && (newModules.peek() == null)) {
                for (ModuleImpl m : initializingModules) {
                    try {
                        // mark module as invalid
                        m.fail(null, "Invalid recursive dependency from import policy or import override policy in module "
                               + m.getModuleDefinition().getName() + " v" + m.getModuleDefinition().getVersion());
                    } catch (ModuleInitializationException e) {
                        // ignore
                    }
                }
                initializingModules.clear();
            }
        }
    }

    @Override
        public String toString() {
            return "Java Module System";
        }
}
