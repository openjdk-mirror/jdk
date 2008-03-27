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
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.Set;

/**
* This class represents the reified module definition in the
* module system.
* <p>
* @see java.lang.ClassLoader
* @see java.module.Repository
* @see java.module.Version
*
* @since 1.7
*/
public abstract class ModuleDefinition {

    /** Counter for generating Id for module definitions. */
    private static final AtomicLong idCounter = new AtomicLong();

    /** Id for this module definition. */
    private final long id = idCounter.incrementAndGet();

    /**
     * Constructor used by subclasses.
     */
    protected ModuleDefinition() {
        // empty
    }

    /**
     * Returns a long value that represents the unique identifier assigned to
     * this module definition. The identifier is assigned by the JVM and is JVM
     * implementation dependent.
     *
     * @return a long value that represents the unique identifier assigned to
     *         this module definition.
     */
    public final long getId() {
        return id;
    }

    /**
     * Returns the name of the module definition.
     *
     * @return the name of the module definition.
     */
    public abstract String getName();

    /**
     * Returns the version of the module definition.
     *
     * @return the <code>Version</code> object.
     */
    public abstract Version getVersion();

    /**
     * Returns an unmodifiable set of the names of the attributes
     * associated with the module definition.
     *
     * @return an unmodifiable set of the names of the attributes.
     */
    public abstract Set<String> getAttributeNames();

    /**
     * Returns the value corresponding to the specified attribute name that is
     * associated with the module definition. If the module definition has
     * attributes with duplicate names, the value of the attribute in the last
     * occurrence is returned.
     *
     * @param name the name of the attribute.
     * @return the value of the attribute. Returns null if the specified
     *         attribute name is not found.
     */
    public abstract String getAttribute(String name);

    /**
     * Returns an unmodifiable list of import dependency. The order
     * of the import dependency in the list follows the declared
     * import order in the module definition.
     *
     * @return an unmodifiable list of import dependency.
     */
    public abstract List<ImportDependency> getImportDependencies();

    /**
     * Returns repository that is associated with the module
     * definition.
     *
     * @return the <code>Repository</code> object.
     */
    public abstract Repository getRepository();

    /**
     * Returns an unmodifiable set of the names of the classes
     * that a member of this module definition.
     *
     * @return The unmodifiable set of the names of the member classes.
     */
    public abstract Set<String> getMemberClasses();

    /**
     * Returns an unmodifiable set of the names of the classes
     * that are exported by this module definition.
     * This is a subset of the classes returned by
     * {@link #getMemberClasses}.
     *
     * @return The unmodifiable set of the names of the exported classes.
     */
    public abstract Set<String> getExportedClasses();

    /**
     * Check if the specified class is exported by this module definition.
     *
     * @param name the name of the class.
     * @return true if the class is exported.
     */
    public boolean isClassExported(String name) {
        // TODO: convert class name?
        Set<String> exportedClasses = getExportedClasses();
        return exportedClasses.contains(name);
    }

    /**
     * Returns an unmodifiable set of the path of the resources exported by
     * this module definition.
     * <p>
     * Resources are specified as '/' separated paths, with no leading '/'.
     *
     * @return The unmodifiable set of the path of the exported resources.
     */
    public abstract Set<String> getExportedResources();

    /**
     * Check if the specified resource is exported by this module definition.
     *
     * @param path A '/' delimited path (e.g. x/y/Z.class")
     * @return true if the resource in the path is exported.
     */
    public boolean isResourceExported(String path) {
        Set<String> exportedResources = getExportedResources();
        return exportedResources.contains(path);
    }

    /**
     * Returns a {@code Module} instance for the specified {@code ModuleDefinition}
     * in the {@code ModuleSystem}. The module is initialized and ready to use.
     * Equivalent to:
     * <pre>
     *      getRepository().getModuleSystem().getModule(this);
     * </pre>
     *
     * @return a {@code Module} instance of the {@code ModuleDefinition}.
     * @throws ModuleInitializationException if the module instance cannot be initialized.
     * @throws IllegalStateException if the specified module definition
     *         has already been disabled.
     */
    public final Module getModuleInstance() throws ModuleInitializationException {
        return getRepository().getModuleSystem().getModule(this);
    }

    /**
     * Returns this element's annotation for the specified type or
     * the value of the specified attribute as an annotation.
     *
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return this element's annotation for the specified annotation type if
     *     present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     */
    public abstract <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    /**
     * Returns an unmodifiable list of all annotations present on this element.
     * If no annotations are present, an empty list is returned.
     *
     * @return an unmodifiable list of all annotations present on this element
     */
    public abstract List<Annotation> getAnnotations();

    /**
     * Checks if the entire content of this module definition is stored locally.
     *
     * @return true if the entire content of this module definition is stored
     *         locally. Otherwise, returns false.
     */
    public boolean isDownloaded() {
        Boolean local = java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Boolean>() {
                        public Boolean run() {
                            return getModuleDefinitionContent().isDownloaded();
                        }
                    });

        return local.booleanValue();
    }

    /**
     * Check if the {@code Module} instance for this module definition can be
     * released from the {@code ModuleSystem}.
     *
     * @return true if {@code Module} instance of this {@code ModuleDefinition}
     *         can be released. Otherwise, returns false.
     */
    public abstract boolean isModuleReleasable();

    /**
     * Returns a <code>ModuleDefinitionContent</code> instance which
     * represents the content of this module definition.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's <code>checkPermission</code> method with a
     * <code>ModuleSystemPermission("accessModuleDefinitionContent")</code>
     * permission to ensure it's ok to access the content of this module
     * definition.
     *
     * @return the <code>ModuleDefinitionContent</code> instance.
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to the content of this module definition.
     */
    public abstract ModuleDefinitionContent getModuleDefinitionContent();

    /**
     * Compares the specified object with this {@code ModuleDefinition} for
     * equality.
     * Returns {@code true} if and only if {@code obj} is the same object as
     * this object.
     *
     * @param obj the object to be compared for equality with this module definition.
     * @return {@code true} if the specified object is equal to this module definition
     */
    @Override
    public final boolean equals(Object obj)   {
        return (this == obj);
    }

    /**
     * Returns a hash code for this {@code ModuleDefinition}.
     *
     * @return a hash code value for this object.
     */
    @Override
    public final int hashCode()   {
        return super.hashCode();
    }

    /**
     * Returns a <code>String</code> object representing this
     * <code>ModuleDefinition</code>.
     *
     * @return a string representation of the
     *          <code>ModuleDefinition</code> object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
/*
        builder.append("ModuleDefinition[name=");
        builder.append(getName());
        builder.append(",version=");
        builder.append(getVersion());
        builder.append(",repository=");
        builder.append(getRepository().getName());
        builder.append("]");
*/
        builder.append("ModuleDefinition ");
        builder.append(getName());
        builder.append(" v");
        builder.append(getVersion());
        builder.append(" (");
        builder.append(getRepository().getName());
        builder.append(" repository)");

        return builder.toString();
    }
}
