/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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
 * This interface represents an import override policy in the JAM module
 * system. The import override policy controls the
 * <a href="ModuleSystem.html#Resolution">resolution</a> of
 * all initializing module instances in the module system by narrowing
 * the version constraints in the import dependencies of the module
 * instances.
 * <p>
 * The system's import override policy can be
 * obtained by invoking the {@link Modules#getImportOverridePolicy()
 * <tt>getImportOverridePolicy</tt>} method of the {@code Modules} class.
 * Similarly, the system's import override policy can be set
 * by invoking the {@link Modules#setImportOverridePolicy(ImportOverridePolicy)
 * <tt>setImportOverridePolicy</tt>} method of the {@code Modules} class.
 * <p>
 * @see java.module.ImportDependency
 * @see java.module.ModuleDefinition
 * @see java.module.VersionConstraint
 *
 * @since 1.7
 */
public interface ImportOverridePolicy {

    /**
     * Returns a map from {@link ImportDependency} objects to
     * {@link VersionConstraint} objects for an initializing module
     * instance of the specified module definition.
     * <p>
     * The given {@code constraints} is constructed from the initializing
     * module instance's
     * {@linkplain ModuleDefinition#getImportDependencies imports}.
     * Implementations of this method should construct a map containing
     * the elements in the given {@code constraints}, override the
     * appropriate version constraints of the import dependencies
     * in the map, and return the map as the result.
     * <p>
     * The set of import dependencies in the returned map must be equal
     * to the set of imports of the initializing module instance.
     * For each import dependency in the returned map, the corresponding
     * version constraint <i>R</i> may be {@code null} if the import
     * dependency is
     * {@linkplain ImportDependency#isOptional() optional}
     * and should be ignored by the module system during
     * <a href="ModuleSystem.html#Resolution">resolution</a>. Otherwise,
     * <i>R</i> must be within the boundary of the declared version constraint
     * in the initializing module instance's imports.
     *
     * @param moduleDef the module definition.
     * @param constraints an unmodifiable map from {@link ImportDependency}
     *        objects to overridden {@link VersionConstraint} objects.
     * @return a map from {@link ImportDependency} objects to
     *         overridden {@link VersionConstraint} objects.
     */
    public abstract Map<ImportDependency, VersionConstraint> narrow(ModuleDefinition moduleDef,
                Map<ImportDependency, VersionConstraint> constraints);
}
