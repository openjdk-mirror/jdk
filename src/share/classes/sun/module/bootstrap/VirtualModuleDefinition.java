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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.module.ImportDependency;
import java.module.Modules;
import java.module.ModuleContent;
import java.module.ModuleDefinition;
import java.module.ModuleSystem;
import java.module.PackageDefinition;
import java.module.Version;
import java.module.VersionConstraint;
import java.module.Repository;
import java.module.annotation.Attribute;
import java.module.annotation.Attributes;
import java.module.annotation.ImportModule;
import java.module.annotation.ImportModules;
import sun.module.core.AbstractModuleDefinition;
import sun.module.core.JamPackageDefinition;

/**
 * A ModuleDefinition for the virtual modules.
 *
 * XXX: Need to revisit the implementation for
 *    getExportedPackageDefinition() and getMemberPackageDefinitions()
 *
 * One approach we can consider is to write a tool to
 * generate the list of exported packages and member packages
 * at JDK build time instead of hardcoding in the list in
 * VirtualModuleDefinitions.java
 */
class VirtualModuleDefinition extends AbstractModuleDefinition {

    private static final ModuleSystem moduleSystem = BootstrapModuleSystem.getInstance();
    private static final Repository repository = BootstrapRepository.getInstance();

    private final Class metadataClass;
    private volatile Set<PackageDefinition> memberPackageDefs;
    private volatile Set<PackageDefinition> exportedPackageDefs;

    @SuppressWarnings({"unchecked"})
    VirtualModuleDefinition(String name, Version version, Class<?> metadataClass) {
        super(moduleSystem,
              name,
              version,
              null,
              repository,
              false);
        this.metadataClass = metadataClass;
    }

    @Override
    public Set<String> getMemberClasses() {
        throw new UnsupportedOperationException("Information about member classes is " +
            "not available for \"" + getName() + "\" module");
    }

    @Override
    public Set<String> getExportedClasses() {
        throw new UnsupportedOperationException("Information about exported classes is " +
            " not available for \"" + getName() + "\" module");
    }

    @Override
    public Set<PackageDefinition> getMemberPackageDefinitions() {
        if (memberPackageDefs == null) {
            // XXX: return the exported package definitions for now
            memberPackageDefs = getExportedPackageDefinitions();
        }
        return memberPackageDefs;
    }

    @Override
    public Set<PackageDefinition> getExportedPackageDefinitions()  {
        if (exportedPackageDefs == null) {
            sun.module.annotation.ExportPackages exportPackages =
                getAnnotation(sun.module.annotation.ExportPackages.class);
            List<String> exportedPackages = Arrays.asList(exportPackages.value());

            HashSet<PackageDefinition> packageDefs = new HashSet<PackageDefinition>();
            for (String s : exportedPackages) {
                packageDefs.add(new JamPackageDefinition(s, Version.DEFAULT, this));
            }
            exportedPackageDefs = Collections.unmodifiableSet(packageDefs);
        }
        return exportedPackageDefs;
    }

    @Override
    public String getMainClass() {
        return null;
    }

    @Override
    public boolean isClassExported(String className) {
        for (PackageDefinition packageDef : getExportedPackageDefinitions()) {
            String packageName = packageDef.getName();
            if (packageName.equals("*")) {
                // "*" is exported by the "java.classpath" module.
                return true;
            }

            // Checks if the specified class is exported from this module.
            if (className.startsWith(packageName + ".")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getExportedResources() {
        throw new UnsupportedOperationException("Information about exported resources is " +
            "not available for \"" + getName() + "\" module");
    }

    @Override
    public boolean isResourceExported(String name) {
        // XXX special hack for now
        // Module definitions from the bootstrap repository are expected
        // to export all resources.
        return true;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null)  {
            throw new NullPointerException();
        }
        return (T) metadataClass.getAnnotation(annotationClass);
    }

    @Override
    public synchronized List<Annotation> getAnnotations() {
        return Collections.unmodifiableList(Arrays.asList(metadataClass.getAnnotations()));
    }

    @Override
    public boolean isDownloaded() {
         return true;
    }

    @Override
    public ModuleContent getModuleContent() {
        throw new UnsupportedOperationException("No module content supported for \"" +
            getName() + "\" module");
    }
}
