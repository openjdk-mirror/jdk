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
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a version in the module system. The version must
 * be in the following format:<p>
 * <blockquote><pre>
 *    <I>major[.minor[.micro[.update]]][-qualifier]</I>
 * </pre></blockquote>
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
 * <blockquote><pre>
 *    version := simple-version ('-' qualifier)?
 *    simple-version:= major ('.' minor ('.' micro ('.' update)?)?)?
 *    major := digit+
 *    minor := digit+
 *    micro := digit+
 *    update := digit+
 *    qualifier := (alpha | digit | '-' | '_')+
 * </pre></blockquote>
 * where <code>alpha</code> is an alphabetic character, e.g. a-z, A-Z.
 *       <code>digit</code> is a decimal digit, e.g. 0-9.
 *
 * <p>Instances of this class are immutable and safe for concurrent use by
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
    public static final Version DEFAULT = new Version(0, 0, 0, 0, "default");

    private int major;
    private int minor;
    private int micro;
    private int update;
    private String qualifier;

    /**
     * Returns a {@code Version} object holding the value of the specified string.
     * The string must be in the version format and must not
     * contain any leading or trailing whitespace.
     *
     * @param version the string to be parsed.
     * @return a <code>Version</code> parsed from the string.
     * @throws IllegalArgumentException if
     *      the string cannot be parsed.
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

        // Parse major, minor, micro, update versions
        StringTokenizer st = new StringTokenizer(version, ".");
        int major = convertVersionNumber(st.nextToken());
        int minor = 0;
        int micro = 0;
        int update = 0;
        if (st.hasMoreTokens())
            minor = convertVersionNumber(st.nextToken());

        if (st.hasMoreTokens())
            micro = convertVersionNumber(st.nextToken());

        if (st.hasMoreTokens())
            update = convertVersionNumber(st.nextToken());

        return Version.valueOf(major, minor, micro, update, qualifier);
    }

    /**
     * Returns a {@code Version} object holding the specified version number.
     *
     * <p>Equivalent to {@code valueOf(major, minor, micro, 0, null);}.
     *
     * @param major the major version number.
     * @param minor the minor version number.
     * @param micro the micro version number.
     * @throws IllegalArgumentException if major, minor, or micro
     *         is negative.
     */
    public static Version valueOf(int major, int minor, int micro) {
        return Version.valueOf(major, minor, micro, 0, null);
    }

    /**
     * Returns a {@code Version} object holding the specified version number.
     *
     * <p>Equivalent to {@code valueOf(major, minor, micro, 0, qualifier);}.
     *
     * @param major the major version number.
     * @param minor the minor version number.
     * @param micro the micro version number.
     * @param qualifier the qualifier
     * @throws IllegalArgumentException if major or minor or micro
     *         is negative, or qualifier contains illegal character.
     */
    public static Version valueOf(int major, int minor, int micro, String qualifier) {
        return Version.valueOf(major, minor, micro, 0, qualifier);
    }

    /**
     * Returns a {@code Version} object holding the specified version number.
     *
     * <p>Equivalent to {@code valueOf(major, minor, micro, update, null);}.
     *
     * @param major the major version number.
     * @param minor the minor version number.
     * @param micro the micro version number.
     * @param update the update version number.
     * @throws IllegalArgumentException if major or minor or micro
     *         or update is negative.
     */
    public static Version valueOf(int major, int minor, int micro, int update) {
        return Version.valueOf(major, minor, micro, update, null);
    }

    /**
     * Returns a {@code Version} object holding the specified version number.
     *
     * @param major the major version number.
     * @param minor the minor version number.
     * @param micro the micro version number.
     * @param update the update version number.
     * @param qualifier the qualifier
     * @throws IllegalArgumentException if major, minor, micro,
     *         or update is negative, or if qualifier contains illegal characters.
     */
    public static Version valueOf(int major, int minor, int micro, int update, String qualifier) {
        // we could add a basic caching scheme here to reuse Version objects
        return new Version(major, minor, micro, update, qualifier);
    }

    /**
     * Constructs a new <code>Version</code> instance.
     * This constructor is for use by subclasses. Applications should use
     * one of the {@link #valueOf(int,int,int) valueOf()} factory methods to
     * obtain {@code Version} instances.
     *
     * @param major the major version number.
     * @param minor the minor version number.
     * @param micro the micro version number.
     * @param update the update version number.
     * @param qualifier the qualifier
     * @throws IllegalArgumentException if major, minor, micro,
     *         or update are negative, or if qualifier contains illegal character.
     */
    protected Version(int major, int minor, int micro, int update, String qualifier) {
        if (major < 0)
            throw new IllegalArgumentException("Major version number must not be negative: " + major);

        if (minor < 0)
            throw new IllegalArgumentException("Minor version number must not be negative: " + minor);

        if (micro < 0)
            throw new IllegalArgumentException("Micro version number must not be negative: " + micro);

        if (update < 0)
            throw new IllegalArgumentException("Update version number must not be negative: " + update);

        if (qualifier != null && qualifierPattern.matcher(qualifier).matches() == false)
            throw new IllegalArgumentException("qualifier must contain only legal character: " + qualifier);

        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.update = update;
        this.qualifier = qualifier;
    }

    /**
     * Returns the major number in the version.
     *
     * @return the major version.
     */
    public int getMajorNumber() {
        return major;
    }

    /**
     * Returns the minor number in the version.
     *
     * @return the minor version.
     */
    public int getMinorNumber() {
        return minor;
    }

    /**
     * Returns the micro number in the version.
     *
     * @return the micro version.
     */
    public int getMicroNumber() {
        return micro;
    }

    /**
     * Returns the update number in the version.
     *
     * @return the update version.
     */
    public int getUpdateNumber() {
        return update;
    }

    /**
     * Returns the qualifier in the version.
     *
     * @return the qualifier.
     */
    public String getQualifier()    {
        return qualifier;
    }

    /**
     * Returns true if the string is a version in valid format.
     *
     * @return true if the string is a version in valid format.
     *         Otherwise, returns false.
     */
    public static boolean isVersion(String source) {
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
     * Return a <code>VersionConstraint</code> object that represents this version.
     *
     * @return a <code>VersionConstraint</code> object.
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
        this.major = version.major;
        this.minor = version.minor;
        this.micro = version.micro;
        this.update = version.update;
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
     * Compare two <code>Version</code> objects.
     *
     * @param version the <code>Version</code> to be compared.
     * @return the value 0 if the argument <code>Version</code>
     *      is equal to this <code>Version</code>; a value
     *      less than 0 if this <code>Version</code> is less
     *      than the <code>Version</code> argument; and a
     *      value greater than 0 if this <code>Version</code>
     *      is greater than the <code>Version</code> argument.
     */
    // @Override // javac 5.0 bug
    public int compareTo(Version version)   {
        if (this == version)
            return 0;

        // Compare major version
        int result = major - version.getMajorNumber();
        if (result != 0)
            return result;

        // Compare minor version
        result = minor - version.getMinorNumber();
        if (result != 0)
            return result;

        // Compare micro version
        result = micro - version.getMicroNumber();
        if (result != 0)
            return result;

        // Compare update version
        result = update - version.getUpdateNumber();
        if (result != 0)
            return result;

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
     * Returns a <code>Version</code> instance, with the qualifier omitted.
     */
    public Version trimQualifier() {
        if (getQualifier() == null)
            return this;
        else
            return Version.valueOf(getMajorNumber(), getMinorNumber(),
                               getMicroNumber(), getUpdateNumber());
    }

    /**
     * Compare two <code>Version</code> objects for equality.
     * The result is <code>true</code> if and only if the
     * argument is not <code>null</code> and is a
     * <code>Version</code> object that the major, minor,
     * micro, update, and qualifier the same as those of this
     * <code>Version</code>.
     *
     * @param obj the object to compare with.
     * @return whether or not the two objects are equal
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
     * Returns a hash code for this <code>Version</code>.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode()   {
        int result = 17;
        result = 37 * result + major;
        result = 37 * result + minor;
        result = 37 * result + micro;
        result = 37 * result + update;
        result = 37 * result + (qualifier != null ? qualifier.hashCode() : 0);
        return result;
    }

    // Returns the String form of this Version.
    // If shortForm is true, the minor number is omitted if possible;
    // If shortForm is false, the minor number is always displayed.
    private String toString(boolean shortForm) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(major);

        if ((shortForm == false) || (minor != 0) || (update != 0) || (micro != 0)) {
            buffer.append('.');
            buffer.append(minor);

            if (micro != 0 || update != 0) {
                buffer.append('.');
                buffer.append(micro);

                if (update != 0) {
                    buffer.append('.');
                    buffer.append(update);
                }
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
     * Returns a <code>String</code> object representing this
     * <code>Version</code>'s value. The value is converted to the version
     * format and returned as a string.
     *
     * @return a string representation of the value of this object in the
     *         version format.
     */
    @Override
    public String toString() {
        return toString(false);
    }
}
