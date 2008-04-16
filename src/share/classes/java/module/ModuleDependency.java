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

/**
 * This class represents an import dependency of the module definition.
 * <p>
 * @see java.module.ImportDependency
 * @see java.module.VersionConstraint
 * @see java.io.Serializable
 *
 * @since 1.7
 * @serial include
 */
public class ModuleDependency implements ImportDependency, java.io.Serializable {

    private static final long serialVersionUID = -4888614342905988975L;

    private final String name;
    private final VersionConstraint constraint;
    private final boolean reexport;
    private final boolean optional;

    /**
     * Constructs a {@code ModuleDependency} object.
     * <p>
     * The imported module definition is neither re-exported nor optional.
     *
     * @param name the name of the module definition to import.
     * @param constraint the version constraint on the import dependency.
     * @throws NullPointerException if name is null or constraint is null.
     */
    public ModuleDependency(String name, VersionConstraint constraint) {
        this(name, constraint, false, false);
    }

    /**
     * Constructs a {@code ModuleDependency} object.
     *
     * @param name the name of the module definition to import.
     * @param constraint the version constraint on the import dependency.
     * @param reexport true if the imported module definition is re-exported;
     *                 otherwise, false.
     * @param optional true if the imported module definition is optional;
     *                 otherwise, false.
     * @throws NullPointerException if name is null or constraint is null.
     */
    public ModuleDependency(String name, VersionConstraint constraint, boolean reexport, boolean optional)  {

        if (name == null)
            throw new NullPointerException("name must not be null.");

        if (constraint == null)
            throw new NullPointerException("constraint must not be null.");

        this.name = name;
        this.constraint = constraint;
        this.reexport = reexport;
        this.optional = optional;
    }

    /**
     * Returns the name of the module definition to import.
     *
     * @return the name of the module definition to import.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the version constraint of the module definition to import.
     *
     * @return the version constraint of the module definition to import.
     */
    public VersionConstraint getVersionConstraint() {
        return constraint;
    }

    /**
     * Returns true if the imported module definition is re-exported;
     * otherwise, returns false.
     *
     * @return true if the imported module definition is re-exported;
     *         otherwise, returns false.
     */
    public boolean isReexported() {
        return reexport;
    }

    /**
     * Returns true if the imported module definition is optional; otherwise,
     * returns false.
     *
     * @return true if the imported module definition is optional; otherwise,
     *         returns false.
     */
    @Override
    public boolean isOptional() {
        return optional;
    }

    /**
     * Returns the granularity of the import dependency. The granularity is the
     * string "module".
     *
     * @return the granularity of the import dependency.
     */
    @Override
    public String getGranularity() {
        return "module";
    }

    /**
     * Compare two {@code ModuleDependency} objects for equality. The result is
     * {@code true} if and only if the argument is not {@code null} and is a
     * {@code ModuleDependency} object that imported module name, version
     * constraint, reexport, and optional the same as those of this
     * {@code ModuleDependency}.
     *
     * @param obj the object to compare with.
     * @return whether or not the two objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof ModuleDependency))
            return false;

        ModuleDependency moduleDep = (ModuleDependency) obj;

        return (name.equals(moduleDep.getName())
                && constraint.equals(moduleDep.getVersionConstraint())
                && reexport == moduleDep.isReexported()
                && optional == moduleDep.isOptional());
    }

    /**
     * Returns a hash code for this {@code ModuleDependency}.
     *
     * @return a hash code value for this {@code ModuleDependency}.
     */
    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + name.hashCode();
        result = 37 * result + constraint.hashCode();
        result = 37 * result + (reexport ? 0 : 1);
        result = 37 * result + (optional ? 0 : 1);
        return result;
    }

    /**
     * Returns a {@code String} object representing this
     * {@code ModuleDependency}.
     *
     * @return a string representation of the {@code ModuleDependency} object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("ModuleDependency[module ");
        builder.append(name);
        builder.append(" ");
        builder.append(constraint.toString());
        if (reexport) {
            builder.append(",re-export");
        }
        if (optional) {
            builder.append(",optional");
        }
        builder.append("]");

        return builder.toString();
    }
}
