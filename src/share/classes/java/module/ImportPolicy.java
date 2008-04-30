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
 * This interface represents the import policy of a module definition. The
 * import policy is used to determine the version constraints that should be
 * used to resolve the import dependencies in a module during initialization.
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
     * Returns a map of import dependencies and the associated version
     * constraints that should used when initializing this module instance.
     * <p>
     * The list of import dependencies that is returned from the
     * {@code getImportDependencies()} method of the
     * {@code ModuleDefinition} object only reflects the import dependencies
     * with the original version constraints that were specified
     * in the module definition at build time. However, it is possible that
     * deployers might have used the system's import override policy to narrow
     * these version constraints at deployment time to control the actual
     * resolution.
     * <p>
     * Some implementations may use the map of import dependencies and
     * overridden version constraints to determine if the version constraint of
     * an import has been overridden. The map is passed in one of the
     * parameters of this method.
     * <p>
     * Some implementations may use the default import policy instance for
     * determining the map of import dependencies and default version
     * constraints for resolving, and it is passed in one of the parameters of
     * this method.
     * <p>
     * All implementations should return a map of import dependencies and
     * version constraints after resolving the imports. If an import cannot
     * be resolved and the import dependency is mandatory (i.e. non-optional),
     * {@code UnsatisfiedDependencyException} must be thrown. If an import
     * cannot be resolved and the import dependency is optional, {@code null}
     * must be used to represent the version constraint of the missing import
     * in the map.
     *
     * @param moduleDef the module definition of this module instance.
     * @param constraints an unmodifiable map of import dependencies and
     *        overridden version constraints.
     * @param defaultImportPolicy the default import policy for this module
     *        instance.
     * @throws UnsatisfiedDependencyException if an import dependency cannot
     *         be satisfied.
     * @throws ModuleInitializationException if there is other error.
     * @return a map of import dependencies and overridden version constraints
     *         for preparing this module instance in the resolving process.
     */
    public Map<ImportDependency, VersionConstraint> getImports(ModuleDefinition moduleDef,
        Map<ImportDependency,VersionConstraint> constraints, ImportPolicy defaultImportPolicy)
        throws ModuleInitializationException;
}
