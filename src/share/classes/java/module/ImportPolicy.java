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
 * import policy is used to determine the list of imported module definitions
 * in the resolving process to prepare the module instance.
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
     * Returns a list of imported module definitions for preparing this module
     * instance.
     * <p>
     * The list of import module dependencies that is returned from the
     * {@code getImportModuleDependencies()} method of the
     * {@code ModuleDefinition} object only reflects the import module
     * dependencies with the original version constraints that were specified
     * in the module definition at build time. However, it is possible that
     * deployers might have used the system's import override policy to narrow
     * these version constraints at deployment time to control the actual
     * resolution.
     * <p>
     * Some implementations may use the map of imported module names and
     * overridden version constraints to determine if the version constraint of
     * an imported module has been overridden. The map is passed in one of the
     * parameters of this method.
     * <p>
     * Some implementations may use the default import policy instance for
     * determining the list of default imported module definitions for
     * resolving, and it is passed in one of the parameters of this method.
     * <p>
     * All implementations should return a list of imported module
     * definitions after it resolves the imports. The order of the imported
     * module definitions in the list must follow the exact declared order of
     * the corresponding imports. If an import cannot be resolved and the
     * import dependency is mandatory (i.e. non-optional),
     * {@code UnsatisfiedDependencyException} must be thrown. If an import
     * cannot be resolved and the import dependency is optional, {@code null}
     * must be used to represent the missing imported module definition in the
     * list.
     *
     * @param moduleDef the module definition of this module instance.
     * @param constraints an unmodifiable map of imported module names and
     *        overridden version constraints.
     * @param defaultImportPolicy the default import policy for this module
     *        instance.
     * @throws UnsatisfiedDependencyException if an import module dependency
     *         cannot be satisfied.
     * @return a list of imported module definitions for preparing this module
     *         instance in the resolving process.
     */
    public abstract List<ModuleDefinition> getImports(ModuleDefinition moduleDef,
        Map<String,VersionConstraint> constraints, ImportPolicy defaultImportPolicy)
        throws ModuleInitializationException;
}
