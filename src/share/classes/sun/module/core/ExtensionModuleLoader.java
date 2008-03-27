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

import java.io.*;
import java.module.Module;
import java.module.ModuleDefinition;
import java.module.ModuleDefinitionContent;
import java.module.ModuleInitializationException;
import java.module.Repository;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.*;

/**
 * This class represents the extension module loader that is used by the
 * extension classloader for loading classes and resources from extension
 * modules.
 *
 * Extension modules are stored in the extension repository, i.e.
 * {java.home}/lib/module/ext directory. During VM startup, the installed
 * extension modules are examined. The highest version of each module with
 * the same name would be recognized, and its reexported would be expanded.
 * When classes and resources are requested from this classloader, the
 * classloader of each recognized extension module and its reexported modules
 * would be used.
 *
 * If there is more than one extension module with different names, the order
 * which this classloader recognizes these extension module is
 * undeterministric. The current implementation is based on the order of the
 * module definitions returned from the extension repository, but this may
 * change in the future.
 */
public final class ExtensionModuleLoader extends SecureClassLoader {

    private List<Module> extensionModules = new ArrayList<Module>();

    public ExtensionModuleLoader() {
        // Use the bootstrap class loader as parent.
        super(null);
        initializeExtensionModules();
    }

    /**
     * Initialize the extension modules from the extension repository.
     */
    private void initializeExtensionModules() {

        List<ModuleDefinition> extensionModuleDefs = new ArrayList<ModuleDefinition>();
        final Repository bootstrapRepository = Repository.getBootstrapRepository();
        Repository extensionRepository = Repository.getSystemRepository();

        if (extensionRepository != bootstrapRepository) {

            // Locating the extension repository by looking up the immediate child
            // of the bootstrap repository, starting from the system repository.
            while (extensionRepository.getParent() != bootstrapRepository) {
                extensionRepository = extensionRepository.getParent();
            }

            // Checks if the repository is indeed an extension repository
            if (extensionRepository.getName().equals("extension") == false) {
                // Reset the extension repository to be the bootstrap repository
                extensionRepository = bootstrapRepository;
            } else {
                // Locating the system extension repository by looking up the immediate child
                // of the extension repository, starting from the system repository.
                Repository systemExtensionRepository = Repository.getSystemRepository();

                while (systemExtensionRepository.getParent() != extensionRepository) {
                    systemExtensionRepository = systemExtensionRepository.getParent();
                }

                // Checks if the repository is indeed a system extension repository
                if (systemExtensionRepository.getName().equals("system-extension")) {
                    extensionRepository = systemExtensionRepository;
                }

                // Find the highest version of the module definitions that are
                // from the extension repository.
                Map<String, ModuleDefinition> moduleDefMap = new HashMap<String, ModuleDefinition>();
                for (ModuleDefinition md : extensionRepository.findAll()) {
                    // Skips module definition if it is not from the extension
                    // repository
                    if (md.getRepository() == bootstrapRepository) {
                        continue;
                    }
                    // The module definition is a potential candidate, retain the
                    // highest version of the same module definition if it exists
                    ModuleDefinition md2 = moduleDefMap.get(md.getName());
                    if (md2 == null || (md2.getVersion().compareTo(md.getVersion()) < 0))  {
                        moduleDefMap.put(md.getName(), md);
                    }
                }

                extensionModuleDefs = new ArrayList<ModuleDefinition>(moduleDefMap.values());
            }
        }

        // Find the classpath module instance
        ModuleDefinition classpathModuleDef = extensionRepository.find("java.classpath");
        Module classpathModule;
        try {
            classpathModule = classpathModuleDef.getModuleInstance();
        } catch(ModuleInitializationException e) {
            throw new RuntimeException("Unable to instantiate the \"java.classpath\" module.", e);
        }

        for (ModuleDefinition md : extensionModuleDefs) {
            try {
                // Instantiate the module instance of the extension module definition
                Module m = md.getModuleInstance();

                // Find import dependency transitive closure
                Set<Module> importedModulesClosure = ModuleUtils.findImportedModulesClosure(m);

                // An extension module must NEVER import the classpath module
                // transitively. Otherwise, loading classes through this extension
                // module could trigger classloading through the application
                // classloader which delegates to the extension classloader,
                // and it will result in infinite loop.
                if (importedModulesClosure.contains(classpathModule)) {
                    // XXX use logging
                    System.err.println("Warning: module " + md.getName() + " v" + md.getVersion()
                                       + " in the extension repository should not import the \"java.classpath\" module transitively; the module is ignored.");
                    // skip this module
                    continue;
                }

                // Expand the list of reexported module instances for the specified
                // extension module instance
                List<Module> reexportedModules = new ArrayList<Module>();
                ModuleUtils.expandReexports(m, reexportedModules, true);

                // Add the reexported module instances and the extension
                // module instance.
                //
                // Note that the reexported module instances come first in the
                // list before the extension module instance. This is basically
                // the same order in which we do classloading from the
                // imported modules first before the module itself.
                extensionModules.addAll(reexportedModules);
                extensionModules.add(m);

                // XXX: Might want to revisit extension modules ordering
                //
                // Also note that if there is extension module that imports
                // another module which also exists in different versions in
                // the extension repository, e.g.
                //
                // 1. module A imports and reexports module Bv1
                // 2. module Bv1
                // 3. module Bv2
                //
                // the order of the extension modules (after reexport expansion)
                // for searching classes could be problematic.
                //
                // 1. Bv1, A, Bv2 (i.e. "A, Bv2" before reexport expansion)
                // 2. Bv2, Bv1, A (i.e. "Bv2, A" before reexport expansion)
                //
                // In this case, the version of classes in B returned through
                // the extension classloader would depend on the extension
                // modules iteration order at runtime. Unfortunately, there is
                // no simple answer to this ordering problem that would satisfy
                // everyone.
                //

            } catch (ModuleInitializationException e) {
                // ignore exception
            }
        }
    }

    /**
     * Check if the specified class is exported by the extension modules.
     *
     * @param name the name of the class.
     * @return true if the class is exported.
     */
    public boolean isClassExported(String name) {
        for (Module m : extensionModules)  {
            if (m.getModuleDefinition().isClassExported(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);

        if (c == null) {
            // Iterate through the list of extension module instances
            // and find the one that exports the requested type.
            for (Module m : extensionModules)  {
                if (m.getModuleDefinition().isClassExported(name)) {
                    return m.getClassLoader().loadClass(name);
                }
            }
        }

        if (c == null) {
            // check myself
            c = findClass(name);
        }

        // we do not delegate to the parent

        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    /**
     * Check if the specified resource is exported by the extension modules.
     *
     * @param path A '/' delimited path (e.g. x/y/Z.class")
     * @return true if the resource in the path is exported.
     */
    public boolean isResourceExported(String name) {
        for (Module m : extensionModules)  {
            if (m.getModuleDefinition().isResourceExported(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public URL getResource(String name) {
        // Iterate through the list of extension module instances
        // and find the one that exports the requested resource.
        for (Module m : extensionModules)  {
            ModuleDefinition md = m.getModuleDefinition();
            if (m.getModuleDefinition().isResourceExported(name)) {
                return m.getClassLoader().getResource(name);
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Vector<URL> v = new Vector<URL>();

        // Iterate through the list of extension module instances
        // and find the one that exports the requested resource.
        for (Module m : extensionModules)  {
            ModuleDefinition md = m.getModuleDefinition();
            if (m.getModuleDefinition().isResourceExported(name)) {
                URL url = m.getClassLoader().getResource(name);
                if (url != null) {
                    v.add(url);
                }
            }
        }
        return v.elements();
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        // Iterate through the list of extension module instances
        // and find the one that exports the requested resource.
        for (Module m : extensionModules)  {
            ModuleDefinition md = m.getModuleDefinition();
            if (m.getModuleDefinition().isResourceExported(name)) {
                return m.getClassLoader().getResourceAsStream(name);
            }
        }
        return null;
    }

    @Override
    public String findLibrary(String name) {
        // Iterate through the list of extension module instances
        // and find the one that has the specified native library.
        for (Module m : extensionModules)  {
            final ModuleDefinition md = m.getModuleDefinition();
            ModuleDefinitionContent content = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<ModuleDefinitionContent>() {
                    public ModuleDefinitionContent run() {
                        return md.getModuleDefinitionContent();
                    }
                });
            File lib = content.getNativeLibrary(name);
            if (lib != null) {
                try {
                    return lib.getCanonicalPath();
                } catch (IOException ex) {
                    // ignore exception
                }
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "ExtensionModuleLoader";
    }
}
