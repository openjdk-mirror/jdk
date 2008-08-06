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

import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.module.ImportDependency;
import java.module.Modules;
import java.module.ModuleContent;
import java.module.ModuleDefinition;
import java.module.ModuleSystem;
import java.module.ModuleSystemPermission;
import java.module.PackageDefinition;
import java.module.Repository;
import java.module.Version;
import java.module.VersionConstraint;
import java.module.annotation.Attribute;
import java.module.annotation.Attributes;
import java.module.annotation.ImportModule;
import java.module.annotation.ImportModules;

/**
 */
public abstract class AbstractModuleDefinition extends ModuleDefinition {

    private final ModuleSystem moduleSystem;
    private final String name;
    private final Version version;
    private final ModuleContent content;
    private final Repository repository;
    private final boolean moduleReleasable;
    private volatile List<ImportDependency> importDependencies = null;
    private volatile Map<String, String> attributesMap = null;

    protected AbstractModuleDefinition(ModuleSystem moduleSystem,
            String name, Version version, ModuleContent content,
            Repository repository, boolean releasable) {
        this.moduleSystem = moduleSystem;
        this.name = name;
        this.version = version;
        this.content = content;
        this.repository = repository;
        this.moduleReleasable = releasable;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public java.module.Version getVersion() {
        return version;
    }

    @Override
    public final List<ImportDependency> getImportDependencies() {
        if (importDependencies == null) {
            List<ImportDependency> dependencies = new ArrayList<ImportDependency>();
            ImportModules importModules = getAnnotation(ImportModules.class);
            if (importModules != null) {
                for (ImportModule importModule : Arrays.asList(importModules.value())) {
                    String name = importModule.name();
                    VersionConstraint constraint = VersionConstraint.valueOf(importModule.version());
                    boolean reexport = importModule.reexport();
                    boolean optional = importModule.optional();
                    Attribute[] attributes = importModule.attributes();
                    Map<String, String> attrs = new HashMap<String, String>();
                    if (attributes != null) {
                        for (Attribute a : attributes) {
                            attrs.put(a.name(), a.value());
                        }
                    }
                    dependencies.add(Modules.newModuleDependency(name, constraint, reexport, optional, attrs));
                }
            }
            importDependencies = Collections.unmodifiableList(dependencies);
        }
        return importDependencies;
    }

    private final synchronized Map<String, String> getAttributesMap() {
        if (attributesMap == null) {
            attributesMap = new HashMap<String, String>();
            Attributes attrs = getAnnotation(Attributes.class);
            if (attrs != null) {
                for (Attribute attr : attrs.value()) {
                    attributesMap.put(attr.name(), attr.value());
                }
            }
        }
        return attributesMap;
    }

    @Override
    public final Set<String> getAttributeNames() {
        return Collections.unmodifiableSet(getAttributesMap().keySet());
    }

    @Override
    public final String getAttribute(String name) {
        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }
        return getAttributesMap().get(name);
    }

    @Override
    public final String getMainClass() {
        java.module.annotation.MainClass mainClass = getAnnotation
            (java.module.annotation.MainClass.class);
        if (mainClass != null) {
            return mainClass.value();
        } else {
            return null;
        }
    }

    @Override
    public boolean isClassExported(String className) {
        try {
            return getExportedClasses().contains(className);
        } catch (UnsupportedOperationException uoe) {
            // returns true if the class is of one of the exported packages.
            Set<PackageDefinition> pkgs = getExportedPackageDefinitions();
            for (PackageDefinition p : pkgs) {
                if (className.startsWith(p.getName() + ".")) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public boolean isResourceExported(String name) {
        try {
            return getExportedResources().contains(name);
        } catch (UnsupportedOperationException uoe) {
            // return true if we don't know so that the caller may try
            // to find the resource
            return true;
        }
    }

    @Override
    public final boolean isModuleReleasable() {
         return moduleReleasable;
    }

    @Override
    public final Repository getRepository() {
        return repository;
    }

    @Override
    public final ModuleSystem getModuleSystem() {
        return moduleSystem;
    }

    @Override
    public ModuleContent getModuleContent() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("accessModuleContent"));
        }
        return content;
    }
}
