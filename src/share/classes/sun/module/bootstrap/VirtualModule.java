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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.module.Module;
import java.module.ModuleDefinition;

/**
 * Virtual module instance in the Java SE platform.
 *
 * @since 1.7
 */
final class VirtualModule extends Module {

    private ModuleDefinition moduleDef;
    private boolean useClassPath;
    private ClassLoader classLoader;
    private List<Module> importedModules;

    VirtualModule(ModuleDefinition moduleDef) {
        super();
        this.moduleDef = moduleDef;
        this.useClassPath = "java.classpath".equals(moduleDef.getName());
        if (this.useClassPath) {
            this.classLoader = new ClasspathClassLoader(this);
        } else {
            this.classLoader = new BootstrapClassLoader(this);
        }
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
     * This method is intended to be called by the bootstrap module system
     * to set up the imported dependency in virtual modules.
     */
    void addImportedModule(Module m) {
        importedModules.add(m);
    }

    @Override
    public List<Module> getImportedModules() {
        return Collections.unmodifiableList(importedModules);
    }

    @Override
    public boolean supportsDeepValidation() {
        // Deep validation is supported in all virtual modules, except
        // the java.classpath module - because no exported and member
        // class list exist for that module.
        return (useClassPath == false);
    }

    @Override
    public void deepValidate() {
        if (supportsDeepValidation() == false) {
            throw new UnsupportedOperationException(moduleDef.getName()
                                + " module cannot be deep validated.");
        }
    }

    /**
     * Wrapper class loader for bootstrap classloader.
     *
     * Inherit implementations of loadClass() and getResource().
     * With a null parent, they delegate to bootstrap, which is a first
     * order approximation of what we want.
     *
     * XXX This will likely have to be revisited.
     */
    private static class BootstrapClassLoader extends ClassLoader {
        private Module module;

        private BootstrapClassLoader(Module module) {
            super(null);
            this.module = module;
        }

        @Override
        public Module getModule() {
            return module;
        }
    };

    /**
     * Wrapper class loader for the system class loader.
     *
     * We want getModule() to return the correct value even if the system
     * class loader was overridden and is not the default implementation.
     */
    private static class ClasspathClassLoader extends ClassLoader {
        private Module module;

        private ClasspathClassLoader(Module module) {
            super(sun.misc.Launcher.getLauncher().getClassLoader());
            this.module = module;
        }

        @Override
        public Module getModule() {
            return module;
        }
    }
}
