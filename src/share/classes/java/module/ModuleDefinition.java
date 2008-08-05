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

package java.module;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This class represents a module definition in a module system.
 * A module definition identifies a logical module in a module system
 * within the Java Module System. A module definition implementation is
 * responsible for exposing the information in the metadata of a module
 * and exposing the content of that module; the format of the metadata
 * and the content of a module are specific to the corresponding
 * module system.
 * <p>
 * A module definition must have a {@linkplain #getName() name}
 * and a {@linkplain #getVersion() version}. The Java Module
 * System does not enforce any naming convention on module definitions;
 * each module system may enforce its own naming convention on its module
 * definitions. The version of a module definition must conform to the
 * {@linkplain Version versioning scheme} in the Java Module System. A
 * module definition implementation for a module system must be responsible
 * for translating the versioning information in a module based on this
 * versioning scheme, if the module system uses a different scheme.
 * <p>
 * A module definition may have one or more
 * {@linkplain #getAnnotations() annotations}.
 * A module definition may also have one or more
 * {@linkplain #getAttribute(String) module attributes}. Each module
 * attribute is a name-value pair of case-sensitive strings, and module
 * attributes are generally defined and used by components at a higher
 * layer on top of the Java Module System.
 * <p>
 * A module definition has {@linkplain #getMemberClasses() members} that
 * are classes and resources provided by the module. The classes are from
 * one or more Java packages.
 * <p>
 * A module definition may define which classes and resources are visible
 * externally to other modules in the Java Module System through the export
 * mechanism. The export mechanisms for classes and resources serve
 * different purposes:
 * <ul>
 *      <li><p>The class export defines which classes are visible to other
 *             modules at build-time and at runtime. A compiler may use the
 *             information to enforce that only exported classes in a module
 *             can be compiled against other modules. At runtime, a module
 *             system may leverage the information to search classes more
 *             efficiently across modules.
 *             The class export's information can be obtained using the
 *             {@link #getExportedClasses() getExportedClasses},
 *             {@link #getExportedPackageDefinitions() getExportedPackageDefinitions}, and
 *             {@link #isClassExported(String) isClassExported}
 *             methods.</p></li>
 *
 *      <li><p>The resource export defines which resources are visible to
 *             other modules at runtime. A module system may leverage the
 *             information to search resources more efficiently across
 *             modules.
 *             The resource export's information can be obtained using the
 *             {@link #getExportedResources() getExportedResources} and
 *             {@link #isResourceExported(String) isResourceExported}
 *             methods.</p></li>
 * </ul>
 * <p>
 * A module definition may define its dependencies upon other modules
 * using the {@linkplain #getImportDependencies() import} mechanism.
 * A module definition must express its import dependencies as a list
 * of {@link ImportDependency} instances.
 * <p>
 * A module definition implementation may not support the
 * {@link #getExportedClasses() getExportedClasses},
 * {@link #getMemberClasses() getMemberClasses}, and
 * {@link #getModuleContent() getModuleContent} methods;
 * calling these methods may throw
 * {@code UnsupportedOperationException}.
 *
 * @see java.module.Module
 * @see java.module.ModuleSystem
 * @see java.module.Repository
 * @see java.module.Version
 *
 * @since 1.7
 */
public abstract class ModuleDefinition {

    /**
     * Creates a {@code ModuleDefinition} instance.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with
     * {@code ModuleSystemPermission("createModuleDefinition")} permission to
     * ensure it's ok to create a module definition.
     *
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to create a new
     *         module definition.
     */
    protected ModuleDefinition() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("createModuleDefinition"));
        }
    }

    /**
     * Returns the name of this {@code ModuleDefinition}.
     *
     * @return the name of this {@code ModuleDefinition}.
     */
    public abstract String getName();

    /**
     * Returns the version of this {@code ModuleDefinition}.
     *
     * @return the {@code Version} object.
     */
    public abstract Version getVersion();

    /**
     * Returns an unmodifiable set of the names of the attributes
     * associated with this {@code ModuleDefinition}.
     *
     * @return an unmodifiable set of the names of the attributes.
     */
    public abstract Set<String> getAttributeNames();

    /**
     * Returns the value corresponding to the specified attribute name that is
     * associated with this {@code ModuleDefinition}.
     *
     * @param name the name of the attribute.
     * @return the value of the attribute. Returns {@code null} if the
     *         specified attribute name is not found.
     */
    public abstract String getAttribute(String name);

    /**
     * Returns an unmodifiable list of all import dependencies in this
     * {@code ModuleDefinition}. The order of the import dependencies in
     * the list must follow the declared import order in this
     * {@code ModuleDefinition}.
     * <p>
     * If this {@code ModuleDefinition} has a module dependency with another
     * module, the dependency must be represented by an implementation of
     * {@link ModuleDependency} in the list.
     * <p>
     * If this {@code ModuleDefinition} has an package dependency with another
     * module, the dependency must be represented by an implementation of
     * {@link PackageDependency} in the list.
     * <p>
     * If this {@code ModuleDefinition} has an import dependency with another
     * module that is neither a module dependency nor a package dependency, the
     * dependency must not be represented by an implementation of
     * {@link ModuleDependency} or {@link PackageDependency} in the list.
     *
     * @return an unmodifiable list of all import dependencies.
     */
    public abstract List<ImportDependency> getImportDependencies();

    /**
     * Returns the repository that is associated with this
     * {@code ModuleDefinition}.
     *
     * @return the {@code Repository} object.
     */
    public abstract Repository getRepository();

    /**
     * Returns the module system that is associated with this
     * {@code ModuleDefinition}.
     *
     * @return the {@code ModuleSystem} object.
     */
    public abstract ModuleSystem getModuleSystem();

    /**
     * Returns the module archive information that is associated with this
     * {@code ModuleDefinition}.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("getModuleArchiveInfo")}
     * permission to ensure it's ok to access the module archive
     * info of this {@code ModuleDefinition}.
     *
     * @return the {@code ModuleArchiveInfo} object.
     * @throws SecurityException if a security manager exists and
     *         its {@code checkPermission} method denies access
     *         to the module archive info of this {@code ModuleDefinition}.
     */
    public abstract ModuleArchiveInfo getModuleArchiveInfo();

    /**
     * Returns the name of the main class in this {@code ModuleDefinition}.
     * <p>
     * The main class must be declared {@code public}. It must have a
     * {@code main} method which is declared {@code public}, {@code static},
     * and {@code void}; the {@code main} method must accept a single argument
     * that is an array of strings.
     *
     * @return the name of the main class if it exists; otherwise returns null.
     */
    public abstract String getMainClass();

    /**
     * Returns an unmodifiable set of the names of the member classes in
     * this {@code ModuleDefinition}.
     *
     * @return The unmodifiable set of the names of the member classes.
     * @throws UnsupportedOperationException if the set of member classes
     *         cannot be determined.
     */
    public abstract Set<String> getMemberClasses();

    /**
     * Returns an unmodifiable set of the package definitions that represents
     * the member packages in this {@code ModuleDefinition}.
     *
     * @return The unmodifiable set of the member package definitions.
     */
    public abstract Set<PackageDefinition> getMemberPackageDefinitions();

    /**
     * Returns an unmodifiable set of the names of the classes that are
     * exported by this {@code ModuleDefinition}. This must be a subset of the
     * classes returned by the {@link #getMemberClasses() getMemberClasses}
     * method.
     *
     * @return The unmodifiable set of the names of the exported classes.
     * @throws UnsupportedOperationException if the set of exported classes
     *         cannot be determined.
     */
    public abstract Set<String> getExportedClasses();

    /**
     * Returns an unmodifiable set of the package definitions that represents
     * the exported packages in this {@code ModuleDefinition}. This is a subset
     * of the package definitions returned by
     * {@link #getMemberPackageDefinitions() getMemberPackageDefinitions()}.
     *
     * @return The unmodifiable set of the exported package definitions.
     */
    public abstract Set<PackageDefinition> getExportedPackageDefinitions();

    /**
     * Returns true if the specified class may be exported by this
     * {@code ModuleDefinition}; returns false otherwise.
     * <p>
     * If this method returns {@code true} for a specified class, it implies
     * neither that the class actually exists in the module definition, nor
     * the class will be loaded successfully through the
     * {@link ClassLoader#loadClass(String) loadClass} method of
     * the {@code ClassLoader} object of the module instance at runtime.
     *
     * @param name the name of the class.
     * @return true if the class may be exported; otherwise, returns false.
     */
    public boolean isClassExported(String name) {
        try {
            // XXX: convert class name?
            Set<String> exportedClasses = getExportedClasses();
            return exportedClasses.contains(name);
        } catch (UnsupportedOperationException uoe) {
            return false;
        }
    }

    /**
     * Returns an unmodifiable set of the path of the resources exported by
     * this {@code ModuleDefinition}.
     * <p>
     * Each resource's path uses {@code '/'} as the path separator and with no
     * leading {@code '/'}.
     *
     * @return The unmodifiable set of the path of the exported resources.
     * @throws UnsupportedOperationException if the set of exported resources
     *         cannot be determined.
     */
    public abstract Set<String> getExportedResources();

    /**
     * Returns true if the specified resource may be exported by this
     * {@code ModuleDefinition}; returns false otherwise.
     * <p>
     * The resource's path uses {@code '/'} as the path separator and with no
     * leading {@code '/'}.
     * <p>
     * If this method returns {@code true} for a specified resource, it implies
     * neither that the resource actually exists in the module definition, nor
     * the resource will be loaded successfully through the
     * {@link ClassLoader#getResource(String) getResource},
     * {@link ClassLoader#getResourceAsStream(String) getResourceAsStream},
     * or {@link ClassLoader#getResources(String) getResources}
     * methods of the {@code ClassLoader} object in the module
     * instance at runtime.

     * @param path A {@code '/'} delimited path (e.g. {@code x/y/Z.class})
     * @return true if the resource in the path may be exported.
     */
    public boolean isResourceExported(String path) {
        try {
            Set<String> exportedResources = getExportedResources();
            return exportedResources.contains(path);
        } catch (UnsupportedOperationException uoe) {
            return false;
        }
    }

    /**
     * Returns a {@code Module} instance for the specified
     * {@code ModuleDefinition} from its {@code ModuleSystem}.
     * The {@code Module} instance must be fully initialized
     * and its module classloader must be ready for classloading.
     * Equivalent to:
     * <pre>
     *      getModuleSystem().getModule(this);</pre>
     *
     * @return a {@code Module} instance of this {@code ModuleDefinition}.
     * @throws ModuleInitializationException if the module system fails to initialize
     *         the module instance.
     * @throws IllegalStateException if this {@code ModuleDefinition} has
     *         already been disabled in the module system.
     * @see ModuleSystem#getModule(ModuleDefinition)
     */
    public final Module getModuleInstance() throws ModuleInitializationException {
        return getModuleSystem().getModule(this);
    }

    /**
     * Returns this {@code ModuleDefinition}'s annotation for the specified
     * type or the value of the specified attribute as an annotation.
     *
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return this {@code ModuleDefinition}'s annotation for the specified
     *         annotation type if present, else null
     */
    public abstract <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    /**
     * Returns an unmodifiable list of all annotations present on this
     * {@code ModuleDefinition}. If no annotations are present, an empty list
     * is returned.
     *
     * @return an unmodifiable list of all annotations present on this
     *         {@code ModuleDefinition}
     */
    public abstract List<Annotation> getAnnotations();

    /**
     * Returns true if this {@code ModuleDefinition} is local;
     * otherwise, returns false.
     *
     * @return true if this {@code ModuleDefinition} is local; otherwise,
     *         returns false.
     */
    public boolean isLocal() {
        Boolean local = java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Boolean>() {
                        public Boolean run() {
                            return getModuleContent().isDownloaded();
                        }
                    });

        return local.booleanValue();
    }

    /**
     * Returns true if the {@code ModuleSystem} of this
     * {@code ModuleDefinition} can release any {@code Module} instance
     * instantiated from this {@code ModuleDefinition}; otherwise, returns
     * false.
     *
     * @return true if {@code Module} instances can be released; otherwise,
     *         returns false.
     */
    public abstract boolean isModuleReleasable();

    /**
     * Returns a {@code ModuleContent} which represents the content of this
     * {@code ModuleDefinition}.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("getModuleContent")}
     * permission to ensure it's ok to access the content of this
     * {@code ModuleDefinition}.
     *
     * @return the {@code ModuleContent}.
     * @throws SecurityException if a security manager exists and
     *         its {@code checkPermission} method denies access
     *         to the content of this {@code ModuleDefinition}.
     * @throws UnsupportedOperationException if the {@code ModuleContent}
     *         in this {@code ModuleDefinition} cannot be determined.
     */
    public abstract ModuleContent getModuleContent();

    /**
     * Compares the specified object with this {@code ModuleDefinition} for
     * equality.
     * Returns {@code true} if and only if {@code obj} is the same object as
     * this object.
     *
     * @param obj the object to be compared for equality with this
     *        {@code ModuleDefinition}.
     * @return {@code true} if the specified object is the same object as this
     *         {@code ModuleDefinition}; otherwise, returns false.
     */
    @Override
    public final boolean equals(Object obj) {
        return (this == obj);
    }

    /**
     * Returns a hash code for this {@code ModuleDefinition}.
     *
     * @return a hash code value for this {@code ModuleDefinition}.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns a {@code String} object representing this
     * {@code ModuleDefinition}.
     *
     * @return a string representation of the {@code ModuleDefinition} object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("module definition ");
        builder.append(getName());
        builder.append(" v");
        builder.append(getVersion());
        return builder.toString();
    }
}
