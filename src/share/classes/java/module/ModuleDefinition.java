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
 * This class represents a reified module definition in a module system.
 * <p>
 * A {@code ModuleDefinition} identifies a logical module in a module system.
 * It specifies which classes and resources are provided by the module, and
 * what the module imports and exports. The classes in the
 * {@code ModuleDefinition} could be from one or more Java packages.
 * {@code ModuleDefinition}s from one or more module systems can coexist in
 * the same Java virtual machine (JVM) instance. A {@code ModuleDefinition}
 * is inherently stateless, and its identity is represented by its
 * {@link #getName() <tt>name</tt>} and
 * {@link #getVersion() <tt>version</tt>}.
 * <p>
 * A module definition can be instantiated by its module system creating a
 * {@code Module} instance at runtime, using the
 * {@link #getModuleInstance() <tt>getModuleInstance</tt>} method or the
 * module system's
 * {@link ModuleSystem#getModule(ModuleDefinition)
 * <tt>getModule(ModuleDefinition)</tt>} method.
 * <p>
 * The name of a {@code ModuleDefinition} is a case-sensitive string.
 * The Java Module System does not enforce any naming convention on module
 * definitions; each module system can have its own naming convention for its
 * module definitions.
 * <p>
 * Each {@code ModuleDefinition} contains zero or more module
 * attributes. Each module attribute is a name-value pair of case-sensitive
 * strings. Module attributes are generally defined and used by components
 * at a higher layer on top of the Java Module System.
 * <p>
 * Each {@code ModuleDefinition} defines which classes that are part of
 * the module, and the information can be obtained using the
 * {@link getMemberClasses()} method.
 * <p>
 * Each {@code ModuleDefinition} defines which classes and resources are visible
 * externally to other modules though the export mechanism. The export
 * mechanisms for classes and resources serve different purposes:
 * <ul>
 *      <li><p>The class export defines which classes are visible to other
 *             modules at build-time and after the module is interconnected
 *             at runtime. When Java code is compiled against other imported
 *             modules at build-time, the compiler would leverage the class
 *             export of the imported modules to ensure only exported classes
 *             can be compiled against. At runtime, a module system could also
 *             leverage the class export to determine how to search classes
 *             efficiently across imported modules in a module.
 *             The class export information can be obtained using the
 *             {@link #getExportedClasses()} method and the
 *             {@link #isClassExported(String)} method.</p></li>
 *
 *      <li><p>The resource export defines which resources are visible to
 *             other modules at runtime after the module is interconnected.
 *             A module system could leverage the resource export to search
 *             resources efficiently across imported modules; however, at
 *             build-time, the compiler compiles classes but not resources,
 *             thus the resource export is not used at all.
 *             Similarly, the resource export information can be obtained
 *             using the {@link #getExportedResources()} method and the
 *             {@link #isResourceExported(String)} method.</p></li>
 * </ul>
 * <p>
 * Each {@code ModuleDefinition} also defines its dependencies upon other
 * module definitions using the import mechanism. These imports are expressed
 * as a list of {@link ImportDependency} instances which are returned by the
 * {@link #getImportDependencies()} method. At runtime, the module system of
 * the {@code ModuleDefinition} is the one responsible to recognize and
 * resolve these imports accordingly when it creates a module instance from
 * the module definition.
 * <p>
 * Some implememtations of {@code ModuleDefinition} may not support the
 * {@link #getExportedClasses()}, {@link #getMemberClasses()}, and
 * {@link #getModuleContent()} methods; invoking these methods may throw
 * {@code UnsupportedOperationException}.
 *
 * <p>
 * @see java.module.Module
 * @see java.module.ModuleSystem
 * @see java.module.Repository
 * @see java.module.Version
 *
 * @since 1.7
 */
public abstract class ModuleDefinition {

    /**
     * Constructor used by subclasses.
     */
    protected ModuleDefinition() {
        // empty
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
     * associated with this {@code ModuleDefinition}. If this
     * {@code ModuleDefinition} has attributes with duplicate names, the value
     * of the attribute in the last occurrence is returned.
     *
     * @param name the name of the attribute.
     * @return the value of the attribute. Returns null if the specified
     *         attribute name is not found.
     */
    public abstract String getAttribute(String name);

    /**
     * Returns an unmodifiable list of all import dependencies. The order of
     * the import dependencies in the list follows the declared import order in
     * the {@code ModuleDefinition}.
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
     * Returns the name of the main class in this {@code ModuleDefinition}.
     *
     * @return the name of the main class if it exists; otherwise returns null.
     */
    public abstract String getMainClass();

    /**
     * Returns an unmodifiable set of the names of the classes that are members
     * of this {@code ModuleDefinition}.
     *
     * @return The unmodifiable set of the names of the member classes.
     * @throws UnsupportedOperationException if the set of member classes
     *         in this {@code ModuleDefinition} cannot be determined.
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
     * exported by this {@code ModuleDefinition}. This is a subset of the
     * classes returned by {@link #getMemberClasses() getMemberClasses()}.
     *
     * @return The unmodifiable set of the names of the exported classes.
     * @throws UnsupportedOperationException if the set of exported classes
     *         in this {@code ModuleDefinition} cannot be determined.
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
     * Returns true if the specified class is exported by this
     * {@code ModuleDefinition}; returns false otherwise.
     * <p>
     * If this method returns {@code true} for a specified class, it implies
     * neither that the class is actually found in the module definition, nor
     * the class is guaranteed to be loaded successfully through the
     * {@link ClassLoader#loadClass(String) loadClass} methods of
     * the {@code ClassLoader} object in the module instance at runtime.
     *
     * @param name the name of the class.
     * @return true if the class is exported; otherwise, returns false.
     */
    public boolean isClassExported(String name) {
        try {
            Set<String> exportedClasses = getExportedClasses();
            return exportedClasses.contains(name);
        } catch (UnsupportedOperationException uoe) {
            return true;
        }
    }

    /**
     * Returns an unmodifiable set of the path of the resources exported by
     * this {@code ModuleDefinition}.
     * <p>
     * Each resource's path is specified using {@code '/'} as path
     * separator, with no leading {@code '/'}.
     *
     * @return The unmodifiable set of the path of the exported resources.
     * @throws UnsupportedOperationException if the set of exported resources
     *         in this {@code ModuleDefinition} cannot be determined.
     */
    public abstract Set<String> getExportedResources();

    /**
     * Returns true if the specified resource is exported by this
     * {@code ModuleDefinition}; returns false otherwise.
     * <p>
     * If this method returns {@code true} for a specified resource, it implies
     * neither that the resource is actually found in the module definition, nor
     * the resource is guaranteed to be loaded successfully through the
     * {@link ClassLoader#getResource(String) getResource},
     * {@link ClassLoader#getResourceAsStream(String) getResourceAsStream},
     * or {@link ClassLoader#getResources(String) getResources}
     * methods of the {@code ClassLoader} object in the module
     * instance at runtime.

     * @param path A {@code '/'} delimited path (e.g. {@code x/y/Z.class})
     * @return true if the resource in the path is exported.
     */
    public boolean isResourceExported(String path) {
        try {
            Set<String> exportedResources = getExportedResources();
            return exportedResources.contains(path);
        } catch (UnsupportedOperationException uoe) {
            return true;
        }
    }

    /**
     * Returns a {@code Module} instance for the specified
     * {@code ModuleDefinition} in the {@code ModuleSystem}. The {@code Module}
     * is initialized and ready to use. Equivalent to:
     * <pre>
     *      getModuleSystem().getModule(this);</pre>
     *
     * @return a {@code Module} instance of the {@code ModuleDefinition}.
     * @throws ModuleInitializationException if the module instance cannot be initialized.
     * @throws IllegalStateException if this {@code ModuleDefinition} has
     *         already been disabled.
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
     * Checks if the entire content of this {@code ModuleDefinition} is stored
     * locally.
     *
     * @return true if the entire content of this {@code ModuleDefinition} is
     *         stored locally; otherwise, returns false.
     */
    public boolean isDownloaded() {
        Boolean local = java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Boolean>() {
                        public Boolean run() {
                            return getModuleContent().isDownloaded();
                        }
                    });

        return local.booleanValue();
    }

    /**
     * Check if the {@code Module} instances instantiated from this
     * {@code ModuleDefinition} can be released from its {@code ModuleSystem}.
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
     * {@code ModuleSystemPermission("accessModuleContent")}
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
        builder.append(" (");
        builder.append(getRepository().getName());
        builder.append(" repository)");
        return builder.toString();
    }
}
