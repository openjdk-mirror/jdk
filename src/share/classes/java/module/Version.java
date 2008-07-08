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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a version. The format of the version is defined as
 * follows:
 * <pre>
 *    major[.minor[.micro[.update]]][-qualifier]</pre>
 * where
 * <ul>
 *      <p><li> {@code major}, {@code minor}, {@code micro}, and {@code update}
 *              are non-negative integers, i.e. {@code 0} <= <i>x</i> <=
 *              {@link java.lang.Integer#MAX_VALUE <tt>Integer.MAX_VALUE</tt>}.
 *              </li></p>
 *
 *      <p><li> {@code qualifier} is a string, and it can contain regular
 *              alphanumeric characters, {@code '-'}, and {@code '_'}.</li></p>
 *
 *      <p><li> If {@code minor} is not specified, it is treated as {@code 0}.
 *              </li></p>
 *
 *      <p><li> If {@code micro} is not specified, it is treated as {@code 0}.
 *              </li></p>
 *
 *      <p><li> If {@code update} is not specified, it is treated as {@code 0}.
 *              </li></p>
 * </ul></p>
 *
 * For example,
 * <blockquote><pre>
 *    1
 *    1.7
 *    1.7-b61
 *    1.7.0
 *    1.7.0-b61
 *    1.7.0.0
 *    1.7.0.1
 *    1.7.1.3-b32-beta-1
 *    1.7.1.3-b56_rc
 * </pre></blockquote>
 * The version format is described by the following grammar:
 * <pre>
 *    version := simple-version ('-' qualifier)?
 *    simple-version:= major ('.' minor ('.' micro ('.' update)?)?)?
 *    major := digit+
 *    minor := digit+
 *    micro := digit+
 *    update := digit+
 *    qualifier := (alpha | digit | '-' | '_')+</pre>
 * where {@code alpha} is an alphabetic character, {@code a-z, A-Z}.
 *       {@code digit} is a decimal digit, {@code 0-9}.
 * <p>
 * When two versions are compared, {@code major}, {@code minor},
 * {@code micro}, and {@code update} are compared numerically while
 * {@code qualifier} is compared through string comparison lexicographically.
 * Two versions are equivalent if and only if the major numbers, the minor
 * numbers, the micro numbers, the update numbers, and the qualifiers each
 * are equal respectively. If the major numbers, the minor numbers, the
 * micro numbers, and the update numbers of two versions are equal
 * respectively but one version has a qualifier while the other has
 * none, the latter is considered a higher version. For example,
 * <pre>
 *      1 < 1.2 < 1.3.1 < 2
 *
 *      1.6.0.5 < 1.7.0 < 1.7.0.1 < 1.7.0.2
 *
 *      3.4.5.6 < 3.4.5.7 == 3.4.5.07 < 3.4.6
 *
 *      7.8.9-b23 < 7.8.9 < 7.8.9.1-b10-alpha < 7.8.9.1-b18 < 7.8.9.1
 *
 *      4.3.2 == 4.3.2.0 < 4.3.2.1</pre>
 * <p>
 * Applications can obtain {@code Version} objects by calling one of the
 * {@link #valueOf(String) valueOf} factory methods.
 * <p>
 * Instances of this class are immutable and safe for concurrent use by
 * multiple threads.
 *
 * @see java.module.Query
 * @see java.module.VersionConstraint
 * @see java.io.Serializable
 * @since  1.7
 * @serial include
 */
public final class Version implements Comparable<Version>, java.io.Serializable {

    private static final long serialVersionUID = 3842079537334808954L;

    private static final Pattern versionPattern;
    private static final Pattern qualifierPattern;

    static {
        // This static initializer must execute before DEFAULT is initialized,
        // because the latter will make use of "qualifierPattern" through the
        // constructor.
        //
        String regex = "(\\d)+(\\.(\\d)+(\\.(\\d)+(\\.(\\d)+)?)?)?(-([\\p{Alnum}-_])+)?";
        versionPattern = Pattern.compile(regex);

        regex = "([\\p{Alnum}-_])+";
        qualifierPattern = Pattern.compile(regex);
    }

    /**
     * A {@code Version} object that represents the default version
     * "0.0.0.0-default".
     */
    public static final Version DEFAULT = new Version(new int[4], "default");

    private transient int[] components;
    private transient String qualifier;

    /**
     * Returns a {@code Version} object holding the value of the specified
     * string. The string must be in the version format and must not contain
     * any leading or trailing whitespace.
     *
     * @param version the string to be parsed.
     * @return a {@code Version} parsed from the string.
     * @throws IllegalArgumentException if the string cannot be parsed.
     */
    public static Version valueOf(String version) {
        // we could add a basic caching scheme here to reuse Version objects

        // Check if the version matches the general format
        Matcher matcher = versionPattern.matcher(version);
        if (matcher.matches() == false) {
            throw new IllegalArgumentException
                ("Version format is invalid: " + version);
        }

        // Parse qualifier
        String qualifier = null;
        int qualifierIndex = version.indexOf('-');
        if (qualifierIndex > 0) {
            qualifier = version.substring(qualifierIndex + 1);
            version = version.substring(0, qualifierIndex);
        }

        // Parse major, minor, micro, update versions ...
        StringTokenizer st = new StringTokenizer(version, ".");
        List<Integer> numberList = new ArrayList<Integer>();
        while (st.hasMoreTokens()) {
            numberList.add(convertVersionNumber(st.nextToken()));
        }

        // Copy the version numbers into an array of int
        Integer[] numbers = numberList.toArray(new Integer[0]);
        int[] components = new int[numbers.length > 4 ? numbers.length : 4];
        for (int i=0; i < numbers.length; i++) {
            components[i] = numbers[i];
        }

        return new Version(components, qualifier);
    }

    /**
     * Returns a {@code Version} object holding the specified version number.
     * Equivalent to:
     * <pre>
     *      valueOf(major, minor, micro, 0, null)</pre>
     *
     * @param major the major version number.
     * @param minor the minor version number.
     * @param micro the micro version number.
     * @throws IllegalArgumentException if major, minor, or micro is
     *         negative.
     */
    public static Version valueOf(int major, int minor, int micro) {
        return Version.valueOf(major, minor, micro, 0, null);
    }

    /**
     * Returns a {@code Version} object holding the specified version number.
     * If the version number has no qualifier, {@code qualifier} is
     * {@code null}. Equivalent to:
     * <pre>
     *      valueOf(major, minor, micro, 0, qualifier)</pre>
     *
     * @param major the major version number.
     * @param minor the minor version number.
     * @param micro the micro version number.
     * @param qualifier the qualifier
     * @throws IllegalArgumentException if major or minor or micro is
     *         negative, or qualifier contains illegal character.
     */
    public static Version valueOf(int major, int minor, int micro, String qualifier) {
        return Version.valueOf(major, minor, micro, 0, qualifier);
    }

    /**
     * Returns a {@code Version} object holding the specified version number.
     * Equivalent to:
     * <pre>
     *      valueOf(major, minor, micro, update, null)</pre>
     *
     * @param major the major version number.
     * @param minor the minor version number.
     * @param micro the micro version number.
     * @param update the update version number.
     * @throws IllegalArgumentException if major or minor or micro or update is
     *         negative.
     */
    public static Version valueOf(int major, int minor, int micro, int update) {
        return Version.valueOf(major, minor, micro, update, null);
    }

    /**
     * Returns a {@code Version} object holding the specified version number.
     * If the version number has no qualifier, {@code qualifier} is
     * {@code null}.
     *
     * @param major the major version number.
     * @param minor the minor version number.
     * @param micro the micro version number.
     * @param update the update version number.
     * @param qualifier the qualifier
     * @throws IllegalArgumentException if major, minor, micro, or update is
     *         negative, or if qualifier contains illegal characters.
     */
    public static Version valueOf(int major, int minor, int micro, int update, String qualifier) {
        // we could add a basic caching scheme here to reuse Version objects
        int[] components = new int[4];
        components[0] = major;
        components[1] = minor;
        components[2] = micro;
        components[3] = update;

        return new Version(components, qualifier);
    }

    /**
     * Constructs a new {@code Version} instance.
     *
     * @param components an array of version number
     * @param qualifier the qualifier
     * @throws IllegalArgumentException if any version number is negative, or
     *         if qualifier contains illegal character.
     */
    private Version(int[] components, String qualifier) {
        if (components[0] < 0)
            throw new IllegalArgumentException("Major version number must not be negative: " + components[0]);

        if (components[1] < 0)
            throw new IllegalArgumentException("Minor version number must not be negative: " + components[1]);

        if (components[2] < 0)
            throw new IllegalArgumentException("Micro version number must not be negative: " + components[2]);

        if (components[3] < 0)
            throw new IllegalArgumentException("Update version number must not be negative: " + components[3]);

        if (qualifier != null && qualifierPattern.matcher(qualifier).matches() == false)
            throw new IllegalArgumentException("qualifier must contain only legal character: " + qualifier);

        // this constructor is private, and it claims the ownership of
        // the components that was passed in.
        this.components = components;
        this.qualifier = qualifier;
    }

    /**
     * Returns the major version number.
     *
     * @return the major version number.
     */
    public int getMajorNumber() {
        return components[0];
    }

    /**
     * Returns the minor version number.
     *
     * @return the minor version number.
     */
    public int getMinorNumber() {
        return components[1];
    }

    /**
     * Returns the micro version number.
     *
     * @return the micro version number.
     */
    public int getMicroNumber() {
        return components[2];
    }

    /**
     * Returns the update version number.
     *
     * @return the update version number.
     */
    public int getUpdateNumber() {
        return components[3];
    }

    /**
     * Returns the qualifier. If this {@code Version} has no qualifier, this
     * method returns {@code null}.
     *
     * @return the qualifier.
     */
    public String getQualifier()    {
        return qualifier;
    }

    /**
     * Returns true if the string is a version in valid format.
     *
     * @return true if the string is a version in valid format. Otherwise,
     *         returns false.
     */
    /** package private */
    static boolean isVersion(String source) {
        return versionPattern.matcher(source).matches();
    }

    private static int convertVersionNumber(String versionNumber)   {
        int result = 0;
        try {
            result = Integer.parseInt(versionNumber);
            if (result < 0) {
                  throw new IllegalArgumentException("Version "
                      + "number is invalid: " + versionNumber);
            }
        } catch(NumberFormatException e)    {
            throw new IllegalArgumentException("Version "
                + "number is invalid: " + versionNumber, e);
        }
        return result;
    }

    /**
     * Return a {@code VersionConstraint} object that represents this version.
     *
     * @return a {@code VersionConstraint} object.
     */
    public VersionConstraint toVersionConstraint() {
        VersionConstraintBuilder builder = new VersionConstraintBuilder();
        builder.add(this);
        return builder.toVersionConstraint();
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

        // Restores the object state by constructing a new Version
        // instance and cloning its fields.
        Version version;
        try {
            version = Version.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw new IOException("Serialized format of version is invalid for de-serialization.");
        }
        this.components = Arrays.copyOf(version.components, version.components.length);
        this.qualifier = version.qualifier;
    }

    /**
     * Override serialization.
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        // Performs the default serialization for all non-transient,
        // non-static fields
        oos.defaultWriteObject();

        // Writes the string representation of version
        oos.writeUTF(this.toString());
    }

    /**
     * Compare two {@code Version} objects.
     *
     * @param version the {@code Version} to be compared.
     * @return the value 0 if the this {@code Version} is equal to the
     *         {@code Version} argument; a value less than 0 if this
     *         {@code Version} is less than the {@code Version} argument; and a
     *         value greater than 0 if this {@code Version} is greater than the
     *         {@code Version} argument.
     */
    @Override
    public int compareTo(Version version)   {
        if (version == null) {
            throw new NullPointerException("version must not be null.");
        }

        if (this == version)
            return 0;

        // Resize components from both Version objects to be the
        // same size before comparison
        int[] components1 = this.components;
        int[] components2 = version.components;
        if (components1.length > components2.length) {
            components2 = Arrays.copyOf(components2, components1.length);
        } else if (components1.length < components2.length) {
            components1 = Arrays.copyOf(components1, components2.length);
        }

        // Compare major, minor, micro, update version ...
        for (int i = 0 ; i < components1.length; i++) {
            int result = components1[i] - components2[i];
            if (result != 0)
                return result;
        }

        // Is there a qualifier?
        String qualifier2 = version.getQualifier();
        if (qualifier != null)  {
            if (qualifier2 == null) {
                return -1;
            }
            return qualifier.compareTo(qualifier2);
        }
        return qualifier2 == null ? 0 : 1;
    }

    /**
     * Returns a {@code Version} instance, with the qualifier omitted.
     */
    Version trimQualifier() {
        if (getQualifier() == null)
            return this;
        else
            return Version.valueOf(getMajorNumber(), getMinorNumber(),
                                   getMicroNumber(), getUpdateNumber());
    }

    /**
     * Compare two {@code Version} objects for equality. The result is
     * {@code true} if and only if the argument is not {@code null} and is a
     * {@code Version} object that the major, minor, micro, update, and
     * qualifier are the same as those of this {@code Version}.
     *
     * @param obj the object to compare with.
     * @return whether or not two {@code Version} objects are equal
     */
    @Override
    public boolean equals(Object obj)   {
        if (this == obj)
            return true;

        if (!(obj instanceof Version))
            return false;

        return (compareTo((Version)obj) == 0);
    }

    /**
     * Returns a hash code for this {@code Version}.
     *
     * @return a hash code value for this {@code Version}.
     */
    @Override
    public int hashCode()   {
        int result = 17;
        for (int n : components) {
            result = 37 * result + n;
        }
        result = 37 * result + (qualifier != null ? qualifier.hashCode() : 0);
        return result;
    }

    // Returns the String form of this Version.
    // If shortForm is true, the minor number is omitted if possible;
    // If shortForm is false, the minor number is always displayed.
    private String toString(boolean shortForm) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(components[0]);   // major number

        // Finds the last component that is not zero
        int lastIndex = 0;
        for (int i=components.length-1; i > 0; i--) {
            if (components[i] != 0) {
                lastIndex = i;
                break;
            }
        }

        if (shortForm == false || lastIndex != 0) {
            buffer.append('.');
            buffer.append(components[1]);

            for (int i=2; i <= lastIndex; i++) {
                buffer.append('.');
                buffer.append(components[i]);
            }
        }

        if (qualifier != null)  {
            buffer.append('-');
            buffer.append(qualifier);
        }

        return buffer.toString();
    }

    // used by VersionRange
    String toShortString() {
        return toString(true);
    }

    /**
     * Returns a {@code String} object representing this {@code Version}'s
     * value. The value is converted to the version format and returned as a
     * string.
     *
     * @return a string representation of the value of this {@code Version} in
     *         the version format.
     */
    @Override
    public String toString() {
        return toString(false);
    }
}
