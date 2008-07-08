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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * This interface represents a package-level import dependency in a module
 * definition.
 * <p>
 * @see java.module.ImportDependency
 * @see java.module.VersionConstraint
 * @see java.io.Serializable
 *
 * @since 1.7
 * @serial include
 */
public interface PackageDependency extends ImportDependency {

    /**
     * Returns the name of the package to import.
     *
     * @return the name of the package to import.
     */
    public String getName();

    /**
     * Returns the version constraint.
     *
     * @return the version constraint.
     */
    public VersionConstraint getVersionConstraint();

    /**
     * Returns true if the imported package is re-exported; otherwise, returns false.
     *
     * @return true if the imported package is re-exported; otherwise, returns false.
     */
    public boolean isReexported();

    /**
     * Returns true if this {@code PackageDependency} is optional;
     * otherwise, returns false.
     *
     * @return true if this {@code PackageDependency} is optional; otherwise, returns false.
     */
    public boolean isOptional();

    /**
     * Returns an unmodifiable set of the names of the attributes associated
     * with this {@code PackageDependency}.
     *
     * @return an unmodifiable set of the names of the attributes.
     */
    public Set<String> getAttributeNames();

    /**
     * Returns the value corresponding to the specified attribute name that is
     * associated with this {@code PackageDependency}.
     *
     * @param name the name of the attribute.
     * @return the value of the attribute. Returns null if the specified
     *         attribute name is not found.
     */
    public String getAttribute(String name);
}
