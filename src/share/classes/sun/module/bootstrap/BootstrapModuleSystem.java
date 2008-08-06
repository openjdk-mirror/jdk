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

package sun.module.bootstrap;

import java.module.ImportDependency;
import java.module.Module;
import java.module.ModuleDefinition;
import java.module.ModuleDependency;
import java.module.ModuleInitializationException;
import java.module.ModuleSystem;
import java.module.ModuleSystemEvent;
import java.module.ModuleSystemPermission;
import java.module.Repository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the bootstrap module system.
 *
 * @since 1.7
 */
public final class BootstrapModuleSystem extends ModuleSystem {
    private static final BootstrapModuleSystem INSTANCE =
        new BootstrapModuleSystem();

    private Map<ModuleDefinition, VirtualModule> modules = new HashMap<ModuleDefinition, VirtualModule>();

    private BootstrapModuleSystem() {
        // empty
    }

    static BootstrapModuleSystem getInstance() {
        return INSTANCE;
    }

    @Override
    public Module getModule(ModuleDefinition moduleDef) throws ModuleInitializationException {
        if (moduleDef.getModuleSystem() != this) {
            throw new IllegalArgumentException
                ("Module definition is associated with another module system.");
        }
        if (moduleDef.getRepository() != Repository.getBootstrapRepository()) {
            throw new IllegalArgumentException
                ("Module definition is not associated with the bootstrap repository.");
        }

        return getModuleInternal(moduleDef);
    }

    @Override
    public List<Module> getModules(ModuleDefinition importer, List<ModuleDefinition> moduleDefs) throws ModuleInitializationException {
        for (ModuleDefinition moduleDef : moduleDefs) {
            if (moduleDef.getModuleSystem() != this) {
                throw new IllegalArgumentException(
                "Module definition is associated with another module system.");
            }
            if (moduleDef.getRepository() != Repository.getBootstrapRepository()) {
                throw new IllegalArgumentException
                ("Module definition is not associated with the bootstrap repository.");
            }
        }

        List<Module> result = new ArrayList<Module>();
        for (ModuleDefinition moduleDef : moduleDefs) {
            result.add(getModuleInternal(moduleDef));
        }
        return result;
    }

    /**
     * Internal method to obtain a module instance for a given virtual module definition.
     */
    private Module getModuleInternal(ModuleDefinition moduleDef) throws ModuleInitializationException {

        // Checks if the module system has instantiate a module instance for
        // the specified module definition before.
        VirtualModule m = modules.get(moduleDef);
        if (m != null) {
            return m;
        }

        // Constructs a new virtual module instance
        m = new VirtualModule(moduleDef);

        // Put the module instance into the map first, so the same module
        // instance could be found in case this method is called recursively.
        modules.put(moduleDef, m);

        // Based on the import dependency, set up the appropriate imported
        // modules for a virtual module.
        for (ImportDependency dep : moduleDef.getImportDependencies()) {
            ModuleDependency moduleDep = (ModuleDependency) dep;

            // Find the imported module only through the bootstrap repository.
            ModuleDefinition md = Repository.getBootstrapRepository().find(moduleDep.getName(), moduleDep.getVersionConstraint());

            // Instantiate a new module instance of the imported module
            Module importedModule = getModuleInternal(md);

            // Add to the import list of the virtual module.
            m.addImportedModule(importedModule);
        }

        // Send MODULE_INITIALIZED event
        ModuleSystemEvent evt = new ModuleSystemEvent(this,
                                    ModuleSystemEvent.Type.MODULE_INITIALIZED,
                                    m, null, null);
        this.processEvent(evt);

        return m;
    }

    @Override
    public void releaseModule(ModuleDefinition moduleDef) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("releaseModule"));
        }
        if (moduleDef.getModuleSystem() != this) {
            throw new IllegalArgumentException
                ("Module definition is associated with another module system.");
        }
        if (moduleDef.getRepository() != Repository.getBootstrapRepository()) {
            throw new IllegalArgumentException
                ("Module definition is not associated with the bootstrap repository.");
        }
        if (moduleDef.isModuleReleasable() == false) {
            throw new UnsupportedOperationException("Module instance is not releasable.");
        }

        // The MODULE_RELEASED event is never sent because virtual module
        // definition cannot be released.
    }

    @Override
    public void disableModuleDefinition(ModuleDefinition moduleDef) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("disableModuleDefinition"));
        }
        if (moduleDef.getModuleSystem() != this) {
            throw new IllegalArgumentException
                ("Module definition is associated with another module system.");
        }
        if (moduleDef.getRepository() != Repository.getBootstrapRepository()) {
            throw new IllegalArgumentException
                ("Module definition is not associated with the bootstrap repository.");
        } else {
            throw new UnsupportedOperationException
                ("Module definition in the bootstrap repository cannot be disabled.");
        }
    }

    @Override
    public String toString() {
        return "Bootstrap Module System";
    }
}
