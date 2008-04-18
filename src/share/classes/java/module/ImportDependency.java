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
 * This class represents an import dependency in a module definition.
 * <p>
 * @see java.module.VersionConstraint
 * @see java.io.Serializable
 *
 * @since 1.7
 * @serial include
 */
public class ImportDependency implements java.io.Serializable {

    private static final long serialVersionUID = -4888614342905988975L;

    private final String type;
    private final String name;
    private final VersionConstraint constraint;
    private final boolean reexport;
    private final boolean optional;
    private final Map<String, String> attributes;

    /**
     * Constructs a {@code ImportDependency} object.
     *
     * @param type the type of the import.
     * @param name the name of the import.
     * @param constraint the version constraint of the import.
     * @param reexport true if the import is re-exported; otherwise, false.
     * @param optional true if the import is optional; otherwise, false.
     * @param attributes map of attributes of the import; null if there is no
     *        attributes.
     * @throws NullPointerException if type is null, name is null or
     *         constraint is null.
     */
    public ImportDependency(String type, String name,
                            VersionConstraint constraint,
                            boolean reexport, boolean optional,
                            Map<String, String> attributes)  {

        if (type == null)
            throw new NullPointerException("type must not be null.");

        if (name == null)
            throw new NullPointerException("name must not be null.");

        if (constraint == null)
            throw new NullPointerException("constraint must not be null.");

        this.type = type;
        this.name = name;
        this.constraint = constraint;
        this.reexport = reexport;
        this.optional = optional;
        if (attributes == null) {
            this.attributes = Collections.emptyMap();;
        } else {
            this.attributes = new HashMap<String, String>(attributes);
        }
    }

    /**
     * Returns the name of the import.
     *
     * @return the name of the import.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the version constraint of the import.
     *
     * @return the version constraint of the import.
     */
    public VersionConstraint getVersionConstraint() {
        return constraint;
    }

    /**
     * Returns true if the import is re-exported; otherwise, returns false.
     *
     * @return true if the import is re-exported; otherwise, returns false.
     */
    public boolean isReexported() {
        return reexport;
    }

    /**
     * Returns true if the import is optional; otherwise, returns false.
     *
     * @return true if the import is optional; otherwise, returns false.
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Returns the type of the import dependency.
     *
     * @return the type of the import dependency.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns an unmodifiable set of the names of the attributes associated
     * with this {@code ImportDependency}.
     *
     * @return an unmodifiable set of the names of the attributes.
     */
    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }

    /**
     * Returns the value corresponding to the specified attribute name that is
     * associated with this {@code ImportDependency}.
     *
     * @param name the name of the attribute.
     * @return the value of the attribute. Returns null if the specified
     *         attribute name is not found.
     */
    public String getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Compare two {@code ImportDependency} objects for equality. The result is
     * {@code true} if and only if the argument is not {@code null} and is a
     * {@code ImportDependency} object that imported type, name, version
     * constraint, reexport, optional, and attributes are the same as those of
     * this {@code ImportDependency}.
     *
     * @param obj the object to compare with.
     * @return whether or not the two objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof ImportDependency))
            return false;

        ImportDependency dep = (ImportDependency) obj;

        return (type.equals(dep.getType())
                && name.equals(dep.getName())
                && constraint.equals(dep.getVersionConstraint())
                && reexport == dep.isReexported()
                && optional == dep.isOptional()
                && attributes.equals(dep.attributes));
    }

    /**
     * Returns a hash code for this {@code ImportDependency}.
     *
     * @return a hash code value for this {@code ImportDependency}.
     */
    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + type.hashCode();
        result = 37 * result + name.hashCode();
        result = 37 * result + constraint.hashCode();
        result = 37 * result + (reexport ? 0 : 1);
        result = 37 * result + (optional ? 0 : 1);
        result = 37 * result + attributes.hashCode();
        return result;
    }

    /**
     * Returns a {@code String} object representing this
     * {@code ImportDependency}.
     *
     * @return a string representation of the {@code ImportDependency} object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("ImportDependency[type=");
        builder.append(type);
        builder.append(",name=");
        builder.append(name);
        builder.append(",version=");
        builder.append(constraint.toString());
        if (reexport) {
            builder.append(",re-export");
        }
        if (optional) {
            builder.append(",optional");
        }
        if (attributes != null) {
            builder.append(",attributes=");
            builder.append(attributes);
        }
        builder.append("]");

        return builder.toString();
    }
}
