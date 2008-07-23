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

import java.module.Module;
import java.module.ModuleDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Module for an OSGi bundle.
 */
class OSGiModule extends Module {

    private OSGiModuleDefinition moduleDef;
    private BundleClassLoader classLoader;

    // List of imported module providers that are added
    // during the resolving process.
    private List<Module> importedModules;

    // Set of modules that import the current module.
    private Set<Module> importingModules;

    OSGiModule(OSGiModuleDefinition osgiModuleDef) {
        super();
        this.moduleDef = osgiModuleDef;
        this.classLoader = new BundleClassLoader(this, osgiModuleDef);
        this.importedModules = new ArrayList<Module>();
    }

    @Override
    public ModuleDefinition getModuleDefinition() {
        return moduleDef;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * This method is intended to be called by the OSGi module system
     * to set up the imported dependency in OSGi modules.
     */
    void addImportedModule(Module m) {
        importedModules.add(m);
    }

    @Override
    public List<Module> getImportedModules() {
        return Collections.unmodifiableList(importedModules);
    }

    /**
     * Returns an unmodifiable list of module instances that imports this module
     * instance.
     */
    Set<Module> getImportingModules() {
        if (importingModules == null) {
            throw new NullPointerException("Importing modules list has not been created yet");
        }
        return Collections.unmodifiableSet(importingModules);
    }

    /**
     * Adds a module instance that imports this module instance.
     */
    void addImportingModule(Module module) {
        if (importingModules == null) {
            throw new NullPointerException("Importing modules list has not been created yet");
        }
        importingModules.add(module);
    }

    /**
     * Removes a module instance that imports this module instance.
     */
    void removeImportingModule(Module module) {
        // This may be called when a module instance gets into ERROR state,
        // before the importing module list is created.
        if (importingModules != null) {
            importingModules.remove(module);
        }
    }

    /**
     * Removes all module instances that import this module instance.
     */
    void removeImportingModules() {
        if (importingModules != null) {
            importingModules.clear();
        }
    }

    @Override
    public boolean supportsDeepValidation() {
        return false;
    }

    @Override
    public void deepValidate() {
        throw new UnsupportedOperationException(
            "OSGi module cannot be deep validated.");
    }
}
