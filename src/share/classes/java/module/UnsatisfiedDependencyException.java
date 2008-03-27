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

/**
 * Thrown to indicate that there is an unsatisifed import dependency in a
 * module during resolution.
 *
 * @see java.module.ImportDependency
 * @see java.module.ModuleDefinition
 *
 * @since 1.7
 */
public class UnsatisfiedDependencyException extends ModuleInitializationException {

    static final long serialVersionUID = -3333250440766142016L;

    private final transient ModuleDefinition moduleDef;
    private final transient ImportDependency importDep;
    private final transient VersionConstraint versionConstraint;

    /**
     * Constructs a <code>UnsatisfiedDependencyException</code> with the
     * detail message, the specified module definition, the import
     * dependency, and the override version constraint.
     *
     * @param s the detail message.
     * @param moduleDef the module definition.
     * @param importDep the unsatisifed import dependency.
     * @param versionConstraint the override version constraint.
     */
    public UnsatisfiedDependencyException(String s, ModuleDefinition moduleDef,
            ImportDependency importDep, VersionConstraint versionConstraint) {
        super(s);
        this.moduleDef = moduleDef;
        this.importDep = importDep;
        this.versionConstraint = versionConstraint;
    }

    /**
     * Constructs a <code>UnsatisfiedDependencyException</code> with the
     * detail message, the cause, the specified module definition, and the
     * import dependency, and the override version constraint.
     *
     * @param s the detail message.
     * @param cause the cause.
     * @param moduleDef the module definition.
     * @param importDep the unsatisifed import dependency.
     * @param versionConstraint the override version constraint.
     */
    public UnsatisfiedDependencyException(String s, Throwable cause,
            ModuleDefinition moduleDef, ImportDependency importDep,
            VersionConstraint versionConstraint) {
        super(s, cause);
        this.moduleDef = moduleDef;
        this.importDep = importDep;
        this.versionConstraint = versionConstraint;
    }

    /**
     * Returns the module definition that has the unsatisfied dependency.
     */
    public ModuleDefinition getModuleDefinition() {
        return moduleDef;
    }

    /**
     * Returns the import dependency of the module definition that is unsatisfied.
     */
    public ImportDependency getImportDependency() {
        return importDep;
    }

    /**
     * Returns the override version constraint.
     */
    public VersionConstraint getOverrideVersionConstraint() {
        return versionConstraint;
    }
}
