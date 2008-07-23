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

import java.module.ModuleSystem;
import java.module.ModuleDefinition;
import java.module.ModuleInitializationException;

import java.module.ImportDependency;
import java.module.ModuleDependency;
import java.module.PackageDependency;
import java.module.Module;
import java.module.ModuleDefinition;
import java.module.ModuleSystem;
import java.module.ModuleSystemPermission;
import java.module.ModuleSystemEvent;
import java.module.Repository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static java.module.ModuleSystemEvent.Type.*;

/**
 * This class is just a wrapper.  The OSGi container resolves the
 * bundles and all the work.
 */
class OSGiModuleSystem extends ModuleSystem {
    private final static OSGiModuleSystem osgiModuleSystem =
        new OSGiModuleSystem();

    private final Map<ModuleDefinition, OSGiModule> modules =
        new HashMap<ModuleDefinition, OSGiModule>();

    // Module definitions that have been disabled.
    private final Set<Long> disabledModuleDefs = new HashSet<Long>();

    private OSGiModuleSystem() {
    }

    static OSGiModuleSystem getModuleSystem() {
        return osgiModuleSystem;
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
        OSGiModule moduleToRelease = modules.get(moduleDef);
        if (moduleToRelease == null) {
            // There is no module instance that is fully initialized, partially
            // initialized, or in error state, which corresponds to the module
            // defintion. Therefore, release module instance is a no-op.
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
        Set<Module> importingModulesClosure =
            findImportingModulesClosure(moduleToRelease);

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

            OSGiModule mi = modules.get(md);

            // Visited modules may have been released previously through releaseModule()
            if (mi == null) {
                continue;
            }

            // Releases cached module instance to avoid potential recursion.
            modules.remove(md);

            // Removes references to all importing modules
            mi.removeImportingModules();

            // TODO: stop the bundle to release module

            // Send MODULE_RELEASED event
            ModuleSystemEvent evt = new ModuleSystemEvent(this, MODULE_RELEASED, moduleToRelease, moduleDef, null);
            processEvent(evt);
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
    public void disableModuleDefinition(ModuleDefinition moduleDef) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("disableModuleDefinition"));
        }
        if (moduleDef.getModuleSystem() != this) {
            throw new UnsupportedOperationException("Cannot disable module definition in a different module system..");
        }

        long id = getOSGiModuleDefinition(moduleDef).getBundle().getBundleId();

        synchronized(disabledModuleDefs) {
            if (disabledModuleDefs.contains(id)) {
                throw new IllegalStateException("Cannot disable module definition which has already been disabled.");
            }
            disabledModuleDefs.add(id);
        }

        // Send MODULE_DEFINITION_DISABLED event
        ModuleSystemEvent evt = new ModuleSystemEvent(this, MODULE_DEFINITION_DISABLED, null, moduleDef, null);
        processEvent(evt);
    }

    @Override
    public Module getModule(ModuleDefinition moduleDef) throws ModuleInitializationException {
        long id = getOSGiModuleDefinition(moduleDef).getBundle().getBundleId();

        // Check if the module definition has been disabled.
        //
        synchronized(disabledModuleDefs) {
            if (disabledModuleDefs.contains(id)) {
                throw new IllegalStateException("Cannot instantiate new module instance from a disabled module definition.");
            }
        }
        return getModuleInternal(moduleDef);
    }

    // @Override
    public List<Module> getModules(ModuleDefinition importer, List<ModuleDefinition> moduleDefs)
            throws ModuleInitializationException {
        for (ModuleDefinition moduleDef : moduleDefs) {
            long id = getOSGiModuleDefinition(moduleDef).getBundle().getBundleId();
            // Check if the module definition has been disabled.
            synchronized(disabledModuleDefs) {
                if (disabledModuleDefs.contains(id)) {
                    throw new IllegalStateException("Cannot instantiate new module instance from a disabled module definition.");
                }
            }
        }

        // XXX: To investigate how this impacts the wiring
        List<Module> result = new ArrayList<Module>();
        for (ModuleDefinition moduleDef : moduleDefs) {
            result.add(getModuleInternal(moduleDef));
        }
        return result;
    }

    private OSGiModule getModuleInternal(ModuleDefinition moduleDef) throws ModuleInitializationException {
        // Checks if the module system has instantiated a module instance for
        // the specified module definition before.
        OSGiModule m = modules.get(moduleDef);
        if (m != null) {
            return m;
        }

        OSGiModuleDefinition osgiModuleDef = getOSGiModuleDefinition(moduleDef);
        Bundle bundle = osgiModuleDef.getBundle();

        if (bundle.getState() == Bundle.UNINSTALLED) {
            throw new ModuleInitializationException("Cannot instantiate a module definition that is uninstalled.");
        }

        try {
            // Before an OSGi bundle could be used, we must start it if it hasn't.
            bundle.start();
        } catch (BundleException bex) {
            throw new ModuleInitializationException("Cannot start OSGi bundle", bex);
        }

        m = new OSGiModule(osgiModuleDef);

        // Put the module instance into the map first, so the same module
        // instance could be found in case this method is called recursively.
        modules.put(moduleDef, m);

        // Based on the import dependency, set up the appropriate imported
        // modules.
        for (ImportDependency imp : moduleDef.getImportDependencies()) {
            Bundle importedBundle = null;
            if (imp instanceof PackageDependency) {
                importedBundle = OSGiRuntime.getExportingBundle(bundle, imp.getName());
            } else if (imp instanceof ModuleDependency) {
                importedBundle = OSGiRuntime.getRequiredBundle(bundle, imp.getName());
            } else {
                throw new ModuleInitializationException("Invalid ImportDependency type: " + imp);
            }

            if (importedBundle == null && imp.isOptional() == false) {
                throw new ModuleInitializationException("Cannot resolve: " + imp);
            }
            // TODO: Assume there is only one OSGi repository
            // and all OSGi bundles are exposed in that repository.
            if (importedBundle != null) {
                OSGiModuleDefinition importedMD =
                    new OSGiModuleDefinition(moduleDef.getRepository(),
                                             importedBundle);

                ModuleSystem importedModuleSystem = importedMD.getModuleSystem();
                if (importedModuleSystem == this) {
                    // Get the raw module instance from the module system.
                    // If it has not been initialized yet, it is automatically
                    // enqueued.
                    Module importedModule = getModuleInternal(importedMD);
                    m.addImportedModule(importedModule);
                } else {
                    // imported modules from the other module systems
                    // TODO: to be implemented
                    throw new UnsupportedOperationException("Not implemented yet");
                }
            }
        }

        // Send MODULE_INITIALIZED event
        ModuleSystemEvent evt = new ModuleSystemEvent(this, MODULE_INITIALIZED, m, moduleDef, null);
        this.processEvent(evt);

        return m;
    }

    private OSGiModuleDefinition getOSGiModuleDefinition(ModuleDefinition moduleDef) {
        if (moduleDef.getModuleSystem() != this) {
            throw new UnsupportedOperationException("ModuleDefinition instantiated from module definitions in a different module system.");
        }
        return OSGiModuleDefinition.class.cast(moduleDef);
    }

    @Override
    public String toString() {
        return "OSGi module system";
    }

   /**
     * Returns the transitive closure of the importing modules of a specified
     * module.
     *
     * @param module module instance
     * @return a set of modules in the importing transitive closure
     */
    private static Set<Module> findImportingModulesClosure(Module module) {
        // Determine the transitive closure of the importing modules using
        // BFS (Breadth-First-Search)
        Set<Module> visitedModules = new HashSet<Module>();
        Queue<Module> visitingQueue = new LinkedList<Module>();
        visitingQueue.add(module);
        while (visitingQueue.size() != 0) {
            Module v = visitingQueue.remove();
            if (v instanceof OSGiModule) {
                // Module instance is from this module system; walk up the
                // importer dependency graph.
                for (Module importingModule : ((OSGiModule) v).getImportingModules()) {
                    // Check if we have visited this module instance before.
                    if (visitedModules.contains(importingModule)) {
                        continue;
                    }
                    visitingQueue.add(importingModule);
                }
            }
            else {
                // Module instance is from other module system; treat it as
                // a leaf node.
            }
            visitedModules.add(v);
        }
        return visitedModules;
    }
}
