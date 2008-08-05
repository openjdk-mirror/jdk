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

import java.util.List;
import java.util.Map;

/**
 * This interface represents the import policy of a module instance
 * in the JAM module system. The import policy controls the
 * <a href="ModuleSystem.html#Resolution">resolution</a> of
 * an initializing module instance by narrowing the version constraints
 * in the import dependencies of the module instance.
 * <p>
 * @see java.module.ImportDependency
 * @see java.module.ImportOverridePolicy
 * @see java.module.Module
 * @see java.module.ModuleDefinition
 * @see java.module.UnsatisfiedDependencyException
 * @see java.module.VersionConstraint
 *
 * @since 1.7
 */
public interface ImportPolicy {

    /**
     * Returns a map from {@link ImportDependency} objects to
     * {@link VersionConstraint} objects for a module instance of
     * the specified module definition.
     * <p>
     * The given {@code constraints} consists of the same set
     * of import dependencies as the initializing module instance's
     * {@linkplain ModuleDefinition#getImportDependencies imports}.
     * For each import dependency in the given {@code constraints},
     * the corresponding version constraint <i>V</i> represents
     * the version constraint under consideration by
     * the module system for
     * <a href="ModuleSystem.html#Resolution">resolving</a> the
     * import dependency;
     * <i>V</i> may be {@code null} if the import dependency is
     * {@linkplain ImportDependency#isOptional() optional}
     * and should be ignored by the module system.
     * <p>
     * Implementations of this method should construct a map containing the
     * elements in the given {@code constraints}, override the appropriate
     * version constraints of the import dependencies in the
     * map, and return the map as the result. Implementations of this method
     * may also use the given {@code defaultImportPolicy} object to determine
     * the result of the default resolution in the module system.
     * <p>
     * The set of import dependencies in the returned map must be equal to the
     * set of imports of the initializing module instance.
     * For each import dependency in the returned map, the corresponding
     * version constraint <i>R</i> may be {@code null} if the import
     * dependency is optional and should be ignored by the module system during
     * resolution. Otherwise, <i>R</i> must be within the boundary of the
     * declared version constraint in the initializing module instance's
     * imports.
     *
     * @param moduleDef the module definition.
     * @param constraints an unmodifiable map from {@link ImportDependency}
     *        objects to overridden {@link VersionConstraint} objects.
     * @param defaultImportPolicy the default import policy for this module
     *        instance.
     * @return a map from {@link ImportDependency} objects to
     *         overridden {@link VersionConstraint} objects.
     * @throws UnsatisfiedDependencyException if an import dependency cannot
     *         be satisfied.
     * @throws ModuleInitializationException if there is other error.
     */
    public Map<ImportDependency, VersionConstraint> getImports(ModuleDefinition moduleDef,
        Map<ImportDependency,VersionConstraint> constraints, ImportPolicy defaultImportPolicy)
        throws ModuleInitializationException;
}
