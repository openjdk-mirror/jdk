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
 * This interface represents the import override policy which is a mechanism
 * that allows deployers to narrow the version constraints in the import
 * dependencies of a specific module definition to control the resolution.
 * <p>
 * The import override policy is invoked before the default or custom
 * import policy of the module instance is executed during initialization.
 * <p>
 * @see java.module.ImportPolicy
 * @see java.module.ModuleDefinition
 * @see java.module.VersionConstraint
 *
 * @since 1.7
 */
public interface ImportOverridePolicy {

    /**
     * Returns a map of imported module names and overridden version
     * constraints for the module definition. The returned map contains the
     * same set of module names as the given map.
     * <p>
     * For each import dependency, the overridden version constraint must be
     * within the boundary of the original version constraint that was
     * specified in the module definition at build time. Otherwise, module
     * initialization will fail.
     *
     * @param importer the importing module definition.
     * @param constraints an unmodifiable map of imported module names and
     *        overridden version constraints.
     * @return the map of imported module names and overridden version
     *         constraints. It contains the same set of module names as the
     *         given map.
     */
    public abstract Map<String,VersionConstraint> narrow(ModuleDefinition importer, Map<String,VersionConstraint> constraints);
}
