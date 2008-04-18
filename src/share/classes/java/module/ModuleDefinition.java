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
* @see java.lang.ClassLoader
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
     * Returns an unmodifiable list of all kinds of import dependencies. The
     * order of the import dependency in the list follows the declared import
     * order in the {@code ModuleDefinition}.
     *
     * @return an unmodifiable list of all kinds of import dependencies.
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
     * {@code ModuleDefinition}. Equivalent to:
     * <pre>
     *      getRepository().getModuleSystem();
     * </pre>
     * @return the {@code ModuleSystem} object.
     */
    public abstract ModuleSystem getModuleSystem();


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
     * Check if the specified class is exported by this
     * {@code ModuleDefinition}.
     *
     * @param name the name of the class.
     * @return true if the class is exported; otherwise, returns false.
     */
    public boolean isClassExported(String name) {
        try {
            // TODO: convert class name?
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
     * Resources are specified as '/' separated paths, with no leading '/'.
     *
     * @return The unmodifiable set of the path of the exported resources.
     * @throws UnsupportedOperationException if the set of exported resources
     *         in this {@code ModuleDefinition} cannot be determined.
     */
    public abstract Set<String> getExportedResources();

    /**
     * Check if the specified resource is exported by this
     * {@code ModuleDefinition}.
     *
     * @param path A '/' delimited path (e.g. x/y/Z.class")
     * @return true if the resource in the path is exported.
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
     * {@code ModuleDefinition} in the {@code ModuleSystem}. The {@code Module}
     * is initialized and ready to use. Equivalent to:
     * <pre>
     *      getModuleSystem().getModule(this);
     * </pre>
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
     * @throws NullPointerException if the given annotation class is null
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
     * Returns a {@code ModuleContent} instance which represents the content
     * of this {@code ModuleDefinition}.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("accessModuleContent")}
     * permission to ensure it's ok to access the content of this
     * {@code ModuleDefinition}.
     *
     * @return the {@code ModuleContent} instance.
     * @throws SecurityException if a security manager exists and
     *         its {@code checkPermission} method denies access
     *         to the content of this {@code ModuleDefinition}.
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
     * @return {@code true} if the specified object is equal to this
     *         {@code ModuleDefinition}; otherwise, returns false.
     */
    @Override
    public final boolean equals(Object obj)   {
        return (this == obj);
    }

    /**
     * Returns a hash code for this {@code ModuleDefinition}.
     *
     * @return a hash code value for this {@code ModuleDefinition}.
     */
    @Override
    public final int hashCode()   {
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
