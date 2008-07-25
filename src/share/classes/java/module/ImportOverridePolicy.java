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
 * This interface represents an import override policy in the JAM
 * module system. An import override policy allows deployers to
 * control the resolution of a module instance in the JAM module system
 * by narrowing the version constraints in the import dependencies.
 * <p>
 * @see java.module.ImportDependency
 * @see java.module.ModuleDefinition
 * @see java.module.VersionConstraint
 *
 * @since 1.7
 */
public interface ImportOverridePolicy {

    /**
     * Returns a map of import dependencies and overridden version constraints
     * for the specified module definition.
     * <p>
     * The returned map must contain the same set of import dependencies as
     * in the given {@code map}. For each import dependency in the returned
     * map, the overridden version constraint must be within the boundary of
     * the original version constraint of the import dependency
     * that is returned from the specified module definition's
     * {@link ModuleDefinition#getImportDependencies() getImportDependencies}
     * method.
     *
     * @param importer the importing module definition.
     * @param map an unmodifiable map of import dependencies and
     *        overridden version constraints.
     * @return the map of import dependencies and overridden version
     *         constraints.
     */
    public abstract Map<ImportDependency, VersionConstraint> narrow(ModuleDefinition importer,
                Map<ImportDependency, VersionConstraint> map);
}
