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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class represents a version constraint in the module system. A version
 * constraint is either a version, a version range, or combination of both.
 * <p>
 * For example,
 * <blockquote><pre>
 *    <i>Version:</i>
 *    1
 *    1.7
 *    1.7-b61
 *    1.7.0
 *    1.7.0-b61
 *    1.7.0.0
 *    1.7.0.1
 *    1.7.1.3-b32-beta-1
 *    1.7.1.3-b56_rc
 *
 *    <i>General version range:</i>
 *    [1.2.3.4, 5.6.7.8)        ~ 1.2.3.4 <= x < 5.6.7.8
 *    (1.2.3.4, 5.6.7.8]        ~ 1.2.3.4 < x <= 5.6.7.8
 *    (1.2.3.4, 5.6.7.8)        ~ 1.2.3.4 < x < 5.6.7.8
 *    [1.2.3.4, 5.6.7.8]        ~ 1.2.3.4 <= x <= 5.6.7.8
 *
 *    <i>Open version range:</i>
 *    1+                        ~ [1, infinity)                 ~ 1 <= x < infinity
 *    1.2+                      ~ [1.2, infinity)               ~ 1.2 <= x < infinity
 *    1.2.3+                    ~ [1.2.3, infinity)             ~ 1.2.3 <= x < infinity
 *    1.2.3.4+                  ~ [1.2.3.4, infinity)           ~ 1.2.3.4 <= x < infinity
 *
 *    <i>Family version range:</i>
 *    1.*                       ~ [1, 2)                        ~ 1 <= x < 2
 *    1.2.*                     ~ [1.2, 1.3)                    ~ 1.2 <= x < 1.3
 *    1.2.3.*                   ~ [1.2.3, 1.2.4)                ~ 1.2.3 <= x < 1.2.4
 *
 *    <i>Union of ranges:</i>
 *    [1.2.3.4, 2.0);2.*;3+     ~ 1.2.3.4 <= x < infinity
 *    1.*;[2.0, 2.7.3)          ~ 1.0.0 <= x < 2.7.3
 * </pre></blockquote>
 * The version constraint format is described by the following grammar:
 * <blockquote><pre>
 *   version-constraint := simple-version-constraint (';' simple-version-constraint)*
 *   simple-version-constraint := version | version-range
 *
 *   version-range := simple-version-range (';' simple-version-range)*
 *   simple-version-range := general-version-range | open-version-range | family-version-range
 *   general-version-range := ('[' | '(') simple-version ',' simple-version (')' | ']')
 *   open-version-range := simple-version '+'
 *   family-version-range := major '.' (minor '.' (micro '.')?)? '*'
 *
 *   version := simple-version ('-' qualifier)?
 *   simple-version:= major ('.' minor ('.' micro ('.' update)?)?)?
 *   major := digit+
 *   minor := digit+
 *   micro := digit+
 *   update := digit+
 *   qualifier := (alpha | digit | '-' | '_')+
 * </pre></blockquote>
 * where {@code alpha} is an alphabetic character, {@code a-z, A-Z}.
 *       {@code digit} is a decimal digit, {@code 0-9}.
 *
 * <p>Applications can obtain {@code VersionConstraint} objects by calling the
 * {@link #valueOf(String) valueOf()} factory method.
 *
 * <p>Instances of this class are immutable and safe for concurrent use by
 * multiple threads.
 *
 * @see java.module.Version
 * @see java.io.Serializable
 * @since  1.7
 * @serial include
 */
public final class VersionConstraint implements java.io.Serializable {

    private static final long serialVersionUID = -1248781624630974037L;

    /**
     * A {@code VersionConstraint} object that represents the default version
     * constraint that is "0.0.0.0+".
     */
    public static final VersionConstraint DEFAULT = VersionConstraint.valueOf("0.0.0.0+");

    // A list of versions and version ranges that are part of the version
    // constraint. Each versions and version ranges have neither been
    // normalized nor combined.
    private transient List<Object> constraints;

    // A list of versions and version ranges that are part of the normalized
    // version constraint. These versions and version ranges have been
    // normalized and combined if possible.
    private transient List<Object> normalizedConstraints;

    /**
     * Package private constructor
     */
    VersionConstraint(List<Object> constraints, List<Object> normalizedConstraints) {
        this.constraints = constraints;
        this.normalizedConstraints = normalizedConstraints;
    }


    /**
     * Returns true if the specified {@code Version} is contained within any of
     * the ranges known to this {@code VersionConstraint}.
     *
     * @param version the {@code Version} object.
     * @return true if the specified version is contained within any of ranges
     *         known to this version constraint. Otherwise, returns false.
     */
    public boolean contains(Version version) {
        for (Object cs : normalizedConstraints) {
            if (cs instanceof Version) {
                Version v = (Version) cs;
                if (v.equals(version))
                    return true;
            }
            else if (cs instanceof VersionRange) {
                VersionRange vr = (VersionRange) cs;
                if (vr.contains(version))
                    return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the specified {@code VersionRange} is contained within
     * any of the ranges known to this {@code VersionConstraint}.
     *
     * @param versionRange the {@code VersionRange} object.
     * @return true if the specified version range is contained within any of
     *         ranges known to this version constraint. Otherwise, returns
     *         false.
     */
    private boolean contains(VersionRange versionRange) {
        for (Object cs : normalizedConstraints) {
            if (cs instanceof VersionRange) {
                VersionRange vr = (VersionRange) cs;
                if (vr.contains(versionRange))
                    return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the specified {@code VersionConstraint} is contained
     * within any of the ranges known to this {@code VersionConstraint}.
     *
     * @param versionConstraint the {@code VersionConstraint} object.
     * @return true if the specified version constraint is contained within
     *         any of ranges known to this version constraint. Otherwise,
     *         returns false.
     */
    public boolean contains(VersionConstraint versionConstraint)  {

        for (Object cs : versionConstraint.normalizedConstraints) {
            if (cs instanceof Version) {
                Version v = (Version) cs;
                if (this.contains(v) == false)
                    return false;
            }
            else if (cs instanceof VersionRange) {
                VersionRange vr = (VersionRange) cs;
                if (this.contains(vr) == false)
                    return false;
            }
        }

        return true;
    }

    /**
     * Determines if the given {@code VersionConstraint} and
     * {@code VersionRange} intersect, and if so adds that intersection to
     * {@code VersionConstraintBuilder}.
     *
     * @param versionConstraint the {@code VersionConstraint} object.
     * @param versionRange the {@code VersionRange} object.
     * @return the true if the specified version range intersects with this
     *         version constraint. Otherwise, returns false.
     */
    private static boolean intersection(VersionConstraint versionConstraint,
                                        VersionRange versionRange,
                                        VersionConstraintBuilder builder) {

        boolean intersect = false;

        for (Object cs : versionConstraint.normalizedConstraints) {
            if (cs instanceof Version) {
                Version v = (Version) cs;
                if (versionRange.contains(v)) {
                    intersect = true;
                    builder.add(v);
                }
            }
            if (cs instanceof VersionRange) {
                VersionRange vr = (VersionRange) cs;
                VersionRange ivr = VersionRange.intersection(vr, versionRange);
                if (ivr != null) {
                    intersect = true;
                    builder.add(ivr);
                }
            }
        }

        return intersect;
    }

    /**
     * Returns a {@code VersionConstraint} that represents the intersection
     * between the specified {@code VersionConstraint} and this
     * {@code VersionConstraint}.
     *
     * @param versionConstraint the {@code VersionConstraint} object.
     * @return the version constraint of intersection if the specified version
     *         constraint and this version constraint intersect. Otherwise,
     *         returns null.
     */
    public VersionConstraint intersection(VersionConstraint versionConstraint)  {

        // Checks for equality
        if (this.equals(versionConstraint))
            return this;

        VersionConstraintBuilder builder = new VersionConstraintBuilder();
        boolean intersect = false;

        for (Object cs : versionConstraint.normalizedConstraints) {
            if (cs instanceof Version) {
                Version v = (Version) cs;
                if (this.contains(v)) {
                    intersect = true;
                    builder.add(v);
                }
            }
            else if (cs instanceof VersionRange) {
                VersionRange vr = (VersionRange) cs;
                if (intersection(this, vr, builder)) {
                    intersect = true;
                }
            }
        }

        if (intersect)
            return builder.toVersionConstraint();
        else
            return null;
    }

    /**
     * Returns a {@code VersionConstraint} object holding the value of the
     * specified string. The string must be in the version constraint format
     * and must not contain any leading or trailing whitespace.
     *
     * @param versionConstraint the string to be parsed.
     * @return A {@code VersionConstraint} parsed from the string.
     * @throws IllegalArgumentException if the string cannot be parsed.
     */
    public static VersionConstraint valueOf(String versionConstraint) {
        // we could add a basic caching scheme here to reuse VersionConstraint objects

        StringTokenizer tokenizer = new StringTokenizer(versionConstraint, ";");
        VersionConstraintBuilder builder = new VersionConstraintBuilder();

        if (tokenizer.hasMoreTokens() == false) {
            throw new IllegalArgumentException("Version constraint format is invalid: "
                    + versionConstraint);
        }

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            if (Version.isVersion(token)) {
                builder.add(Version.valueOf(token));
            } else if (VersionRange.isVersionRange(token)) {
                builder.add(VersionRange.valueOf(token));
            } else {
                throw new IllegalArgumentException("Version constraint format is invalid: "
                        + versionConstraint);
            }
        }

        return builder.toVersionConstraint();
    }

    /**
     * Returns a normalized version constraint.
     */
    VersionConstraint normalize() {
        if (constraints == normalizedConstraints)
            return this;
        else
            return new VersionConstraint(normalizedConstraints, normalizedConstraints);
    }

    /**
     * Override de-serialization.
     */
    private void readObject(ObjectInputStream ois)
                    throws ClassNotFoundException, IOException {
        // Always perform the default de-serialization first
        ois.defaultReadObject();

        // Reads the string representation of version constraint
        String s = ois.readUTF();

        // Restores the object state by constructing a new VersionConstraint
        // instance and cloning its fields.
        VersionConstraint versionConstraint;
        try {
            versionConstraint = VersionConstraint.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new IOException("Serialized format of version constraint is invalid for de-serialization.");
        }
        this.constraints = new ArrayList<Object>(versionConstraint.constraints);
        this.normalizedConstraints = new ArrayList<Object>(versionConstraint.normalizedConstraints);
    }

    /**
     * Override serialization.
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        // Performs the default serialization for all non-transient,
        // non-static fields
        oos.defaultWriteObject();

        // Writes the string representation of version constraint
        oos.writeUTF(this.toString());
    }

    /**
     * Compare two {@code VersionConstraint} objects for equality. The result
     * is {@code true} if and only if the argument is not {@code null} and is
     * a {@code VersionConstraint} object that it has the same normalized
     * versions and version ranges as those of this {@code VersionConstraint}.
     *
     * @param obj the object to compare with.
     * @return whether or not the two objects are equal
     */
    @Override
    public boolean equals(Object obj)   {
        if (this == obj)
            return true;

        if (!(obj instanceof VersionConstraint))
            return false;

        VersionConstraint cs = (VersionConstraint) obj;

        // If both version constraints can satisfy each other, then
        // they must be equal.
        return (this.contains(cs) && cs.contains(this));
    }

    /**
     * Returns a hash code for this {@code VersionConstraint}.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode()   {
        int result = 17;
        result = 37 * result + normalizedConstraints.hashCode();
        return result;
    }

    /**
     * Returns a {@code String} object representing this
     * {@code VersionConstraint}'s value. The value is converted to the version
     * constraint format and returned as a string.
     *
     * @return a string representation of the value of this object in the
     *         version constraint format.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Iterator iter = constraints.iterator();

        while (iter.hasNext()) {
            Object cs = iter.next();
            builder.append(cs.toString());
            if (iter.hasNext())
                builder.append(';');
        }

        return builder.toString();
    }
}
