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
import java.util.List;
import java.util.Set;

/**
* This class represents a reified package definition in a module system.
* <p>
* @see java.lang.ClassLoader
* @see java.module.ModuleDefinition
* @see java.module.Version
*
* @since 1.7
*/
public abstract class PackageDefinition {

    /**
     * Constructor used by subclasses.
     */
    protected PackageDefinition() {
        // empty
    }

    /**
     * Returns the name of this {@code PackageDefinition}.
     *
     * @return the name of this {@code PackageDefinition}.
     */
    public abstract String getName();

    /**
     * Returns the version of this {@code PackageDefinition}.
     *
     * @return the {@code Version} object.
     */
    public abstract Version getVersion();

    /**
     * Returns the {@code ModuleDefinition} that is associated with this
     * {@code PackageDefinition}.
     *
     * @return the {@code ModuleDefinition} object.
     */
    public abstract ModuleDefinition getModuleDefinition();

    /**
     * Returns this {@code PackageDefinition}'s annotation for the specified
     * type or the value of the specified attribute as an annotation.
     *
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return this {@code PackageDefinition}'s annotation for the specified
     *         annotation type if present, else null
     */
    public abstract <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    /**
     * Returns an unmodifiable list of all annotations present in this
     * {@code PackageDefinition}. If no annotations are present, an empty
     * list is returned.
     *
     * @return an unmodifiable list of all annotations present in this
     *         {@code PackageDefinition}.
     */
    public abstract List<Annotation> getAnnotations();

    /**
     * Returns an unmodifiable set of the names of the attributes associated
     * with this {@code PackageDefinition}.
     *
     * @return an unmodifiable set of the names of the attributes.
     */
    public abstract Set<String> getAttributeNames();

    /**
     * Returns the value corresponding to the specified attribute name that is
     * associated with this {@code PackageDefinition}.
     *
     * @param name the name of the attribute.
     * @return the value of the attribute. Returns null if the specified
     *         attribute name is not found.
     */
    public abstract String getAttribute(String name);

    /**
     * Compares the specified object with this {@code PackageDefinition} for
     * equality.
     * Returns {@code true} if and only if {@code obj} is the same object as
     * this object.
     *
     * @param obj the object to be compared for equality with this package definition.
     * @return {@code true} if the specified object is equal to this package definition
     */
    @Override
    public final boolean equals(Object obj)   {
        return (this == obj);
    }

    /**
     * Returns a hash code for this {@code PackageDefinition}.
     *
     * @return a hash code value for this object.
     */
    @Override
    public final int hashCode()   {
        return super.hashCode();
    }

    /**
     * Returns a {@code String} object representing this
     * {@code PackageDefinition}.
     *
     * @return a string representation of the
     *          {@code PackageDefinition} object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("package definition ");
        builder.append(getName());
        builder.append(" v");
        builder.append(getVersion());
        builder.append(" (");
        builder.append(getModuleDefinition().toString());
        builder.append(")");
        return builder.toString();
    }
}
