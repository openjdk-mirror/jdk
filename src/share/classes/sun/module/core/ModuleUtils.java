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

package sun.module.core;

import java.module.ImportDependency;
import java.module.Module;
import java.module.ModuleDefinition;
import java.module.ModuleDependency;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Module Utilities class.
 *
 * @since 1.7
 */
public final class ModuleUtils {

    /**
     * Expand reexports of a specified module instance.
     *
     * @param m module instance
     * @param modules a list of expanded modules
     */
    public static void expandReexports(Module m, List<Module> modules) {
        expandReexports(m, modules, true);
    }

    /**
     * Expand reexports of a specified module instance.
     *
     * @param m module instance
     * @param modules a list of expanded modules
     * @param includeAll true if the expansion should include all imported modules
     */
    private static void expandReexports(Module m, List<Module> modules, boolean includeAll) {
        for (ImportDependency dep : m.getModuleDefinition().getImportDependencies()) {
            ModuleDependency moduleDep = (ModuleDependency) dep;
            if ((includeAll == false) && (moduleDep.isReexported() == false)) {
                continue;
            }
            // Find the actual module that imported via 'dep'
            // Since it is not possible to import multiple modules of the same
            // name, just comparing names is fine.
            // Note that due to optional imports, we cannot assume
            // that module[i] corresponds to import[i]
            String name = moduleDep.getName();
            for (Module importedModule : m.getImportedModules()) {
                if (!importedModule.getModuleDefinition().getName().equals(name)) {
                    continue;
                }
                expandReexports(importedModule, modules, false);
                // add module unless it is already present
                if (modules.contains(importedModule) == false) {
                    modules.add(importedModule);
                }
                break;
            }
        }
    }

    /**
     * Returns the transitive closure of the importing modules of a specified
     * module. The transitive closure includes the specified module.
     *
     * @param module module instance
     * @return a set of modules in the importing transitive closure
     */
    public static Set<Module> findImportingModulesClosure(Module module) {
        // Determine the transitive closure of the importing modules using
        // BFS (Breadth-First-Search)
        Set<Module> visitedModules = new HashSet<Module>();
        Queue<Module> visitingQueue = new LinkedList<Module>();
        visitingQueue.add(module);

        while (visitingQueue.size() != 0) {
            Module v = visitingQueue.remove();

            if (v instanceof ModuleImpl) {
                // Module instance is from this module system; walk up the
                // importer dependency graph.
                for (Module importingModule : ((ModuleImpl) v).getImportingModules()) {
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

    /**
     * Returns the transitive closure of the imported modules of a specified
     * module. The transitive closure includes the specified module.
     *
     * @param module module instance
     * @return a set of modules in the imported transitive closure
     */
    public static Set<Module> findImportedModulesClosure(Module module) {
        // Determine the transitive closure of the imported modules using
        // BFS (Breadth-First-Search)
        Set<Module> visitedModules = new HashSet<Module>();
        Queue<Module> visitingQueue = new LinkedList<Module>();
        visitingQueue.add(module);

        while (visitingQueue.size() != 0) {
            Module v = visitingQueue.remove();

            if (v instanceof ModuleImpl) {
                // Module instance is from this module system; walk up the
                // import dependency graph.
                for (Module importedModule : ((ModuleImpl) v).getImportedModules()) {
                    // Check if we have visited this module instance before.
                    if (visitedModules.contains(importedModule)) {
                        continue;
                    }
                    visitingQueue.add(importedModule);
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
