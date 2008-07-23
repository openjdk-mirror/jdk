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

import java.lang.annotation.Annotation;
import java.module.ImportDependency;
import java.module.ModuleContent;
import java.module.ModuleDefinition;
import java.module.ModuleSystem;
import java.module.ModuleSystemPermission;
import java.module.PackageDefinition;
import java.module.Repository;
import java.module.Version;
import java.util.Collections;
import java.util.Set;
import java.util.List;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.ExportedPackage;

/**
 * A ModuleDefinition for OSGi bundle.
 */
public class OSGiModuleDefinition extends ModuleDefinition {
    private final Repository repository;
    private final Bundle bundle;
    private final String name;
    private final Version version;
    private final BundleManifestMapper mapper;
    private final boolean isReleasable;

    OSGiModuleDefinition(Repository repo, Bundle bundle) {
        this.repository = repo;
        this.bundle = bundle;
        this.mapper = new BundleManifestMapper(this, bundle);
        this.name = mapper.getSymbolicName();
        this.version = mapper.getVersion();
        // XXX: singleton bundle - should it be releasable?
        this.isReleasable = (mapper.isSingleton() == false);
    }

    public Bundle getBundle() {
        return bundle;
    }

    public Set<ExportedPackage> getExportedPackages() {
        return OSGiRuntime.getExportedPackages(bundle);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public Set<String> getAttributeNames() {
        return mapper.getModuleAttributesMap().keySet();
    }

    @Override
    public String getAttribute(String name) {
        return mapper.getModuleAttributesMap().get(name);
    }

    @Override
    public List<ImportDependency> getImportDependencies() {
        return mapper.getImportDependencies();
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public ModuleSystem getModuleSystem() {
        return OSGiModuleSystem.getModuleSystem();
    }

    @Override
    public String getMainClass() {
        return null;
    }

    @Override
    public Set<String> getMemberClasses()  {
        throw new UnsupportedOperationException("OSGiModuleDefinition.getMemberClasses not implemented");
    }

    @Override
    public Set<PackageDefinition> getMemberPackageDefinitions() {
        return mapper.getMemberPackageDefinitions();
    }

    @Override
    public Set<String> getExportedClasses() {
        throw new UnsupportedOperationException("OSGiModuleDefinition.getExportedClasses not implemented");
    }

    @Override
    public Set<PackageDefinition> getExportedPackageDefinitions() {
        return mapper.getExportedPackageDefinitions();
    }

    @Override
    public boolean isClassExported(String name) {
        // TODO: need to deal with include and exclude directive
        String packageName = name.substring(0, name.lastIndexOf("."));
        try {
            Set<PackageDefinition> exports = getExportedPackageDefinitions();
            for (PackageDefinition pkg : exports) {
                if (pkg.getName().equals(packageName)) {
                    // FIXME: assume it's exported for now
                    return true;
                }
            }
            return false;
        } catch (UnsupportedOperationException uoe) {
            return true;
        }
    }

    @Override
    public Set<String> getExportedResources() {
        // TODO: to be implemented
        throw new UnsupportedOperationException("OSGiModuleDefinition.getExportedResources not implemented");
    }

    @Override
    public boolean isResourceExported(String path) {
        // TODO: need to deal with include and exclude directive
        try {
            Set<String> exportedResources = getExportedResources();
            return exportedResources.contains(path);
        } catch (UnsupportedOperationException uoe) {
            return true;
        }
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null)  {
            throw new NullPointerException();
        }
        // No annotation
        return null;
    }

    @Override
    public List<Annotation> getAnnotations() {
        // No annotation
        return Collections.emptyList();
    }

    @Override
    public boolean isModuleReleasable() {
        return isReleasable;
    }

    @Override
    public ModuleContent getModuleContent() {
        throw new UnsupportedOperationException("ModuleContent is not supported for OSGi bundle");
    }
}
