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

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a version range in the module system.
 *
 * @see java.module.Query
 * @see java.module.Version
 * @see java.module.VersionConstraint
 *
 * @since 1.7
 */
class VersionRange implements Comparable<VersionRange>  {

    private static final Pattern generalRangePattern;
    private static final Pattern openRangePattern;
    private static final Pattern familyRangePattern;

    static {
        String regex = "([\\[\\(])((\\d)+(\\.(\\d)+(\\.(\\d)+(\\.(\\d)+)?)?)?)"
            + "\\,[ ]*((\\d)+(\\.(\\d)+(\\.(\\d)+(\\.(\\d)+)?)?)?)([\\)\\]])";
        generalRangePattern = Pattern.compile(regex);

        regex = "(\\d)+(\\.(\\d)+(\\.(\\d)+(\\.(\\d)+)?)?)?\\+";
        openRangePattern = Pattern.compile(regex);

        regex = "(\\d)+\\.((\\d)+\\.((\\d)+\\.)?)?\\*";
        familyRangePattern = Pattern.compile(regex);
    }

    private final Version lowerBound;
    private final boolean lowerBoundInclusive;
    private final Version upperBound;
    private final boolean upperBoundInclusive;

    // Normalized version range in the form of "[floor, ceiling)" to make
    // it simplier for comparison.
    private final Version normalizedLowerBound;
    private final Version normalizedUpperBound;

    /**
     * Constructs a new {@code VersionRange} instance.
     * <p>
     * The qualifiers in the lower bound and the upper bound are ignored.
     * <pre>
     *      [a.b.c.d, p.q.r.s)    ~   a.b.c.d <= x <  p.q.r.s
     *      (a.b.c.d, p.q.r.s]    ~   a.b.c.d <  x <= p.q.r.s
     *      (a.b.c.d, p.q.r.s)    ~   a.b.c.d <  x <  p.q.r.s
     *      [a.b.c.d, p.q.r.s]    ~   a.b.c.d <= x <= p.q.r.s
     * </pre>
     *
     * @param lowerBound the lower bound version in the version range.
     * @param lowerBoundInclusive the inclusiveness of the lower bound version.
     * @param upperBound the upper bound version in the version range.
     * @param upperBoundInclusive the inclusiveness of the upper bound version.
     * @throws NullPointerException if lowerBound is null or upperBound is null.
     * @throws IllegalArgumentException if the lowerBound is greater than the
     *         upperBound.
     */
    private VersionRange(Version lowerBound, boolean lowerBoundInclusive,
            Version upperBound, boolean upperBoundInclusive) {
        if (lowerBound == null) {
            throw new NullPointerException
                    ("Lower bound version must not be null.");
        }

        if (upperBound == null) {
            throw new NullPointerException
                    ("Upper bound version must not be null.");
        }

        // Trims qualifier in the lower bound and upper bound version
        lowerBound = lowerBound.trimQualifier();
        upperBound = upperBound.trimQualifier();

        int comp = lowerBound.compareTo(upperBound);

        // Check lower bound > upper bound
        if (comp > 0)
            throw new IllegalArgumentException("Upper bound "
                + "version must be greater than or equals to lower bound version.");

        // If lower bound == upper bound, the only valid combination is [x, x]
        if (comp == 0 && !(lowerBoundInclusive && upperBoundInclusive)) {
            throw new IllegalArgumentException("If lower bound "
            + "version equals to upper bound version, the only valid version range is [x, x].");
        }

        this.lowerBound = lowerBound;
        this.lowerBoundInclusive = lowerBoundInclusive;
        this.upperBound = upperBound;
        this.upperBoundInclusive = upperBoundInclusive;

        // Normalized version range in the form of "[floor, ceiling)"
        if (lowerBoundInclusive == true) {
            this.normalizedLowerBound = lowerBound;
        }
        else {
            this.normalizedLowerBound = Version.valueOf(lowerBound.getMajorNumber(),
                                                    lowerBound.getMinorNumber(),
                                                    lowerBound.getMicroNumber(),
                                                    lowerBound.getUpdateNumber() + 1);
        }
        if (upperBoundInclusive == false) {
            this.normalizedUpperBound = upperBound;
        }
        else {
            this.normalizedUpperBound = Version.valueOf(upperBound.getMajorNumber(),
                                                    upperBound.getMinorNumber(),
                                                    upperBound.getMicroNumber(),
                                                    upperBound.getUpdateNumber() + 1);
        }
    }

    /**
     * Constructs a new {@code VersionRange} instance.
     * <p>
     * The lower bound is assumed to be inclusive, and the upper bound is
     * assumed to be infinity and exclusive. The qualifiers in the lower
     * bound and the upper bound are ignored.
     * <pre>
     *      a.b.c.d+    ~   [a.b.c.d, infinity)   ~   a.b.c.d <= x < infinity
     * </pre>
     *
     * @param lowerBound the lower bound version in the version range.
     */
    private VersionRange(Version lowerBound) {

        if (lowerBound == null)
            throw new NullPointerException("Lower bound version must not be null.");

        // Trims qualifier in the lower bound version
        lowerBound = lowerBound.trimQualifier();

        this.lowerBound = lowerBound;
        this.lowerBoundInclusive = true;
        this.upperBound = null;
        this.upperBoundInclusive = false;

        // Normalized version range in the form of "[floor, ceiling)"
        this.normalizedLowerBound = lowerBound;
        this.normalizedUpperBound = null;
    }

    /**
     * Constructs a new {@code VersionRange} instance.
     * <p>
     * The lower bound is assumed to be inclusive, and the upper bound is
     * assumed to be exclusive. The qualifiers in the lower bound and the
     * upper bound are ignored.
     * <pre>
     *      [a.b.c.d, p.q.r.s)    ~   a.b.c.d <= x < p.q.r.s
     * </pre>
     *
     * @param lowerBound the lower bound version in the version range.
     * @param upperBound the upper bound version in the version range.
     * @throws NullPointerException if lowerBound is null or upperBound is
     *         null.
     * @throws IllegalArgumentException if the lowerBound is greater than
     *         the upperBound.
     */
    private VersionRange(Version lowerBound, Version upperBound) {
        this(lowerBound, true, upperBound, false);
    }

    /**
     * Returns the lower bound version in the version range.
     *
     * @return the lower bound version.
     */
    Version getLowerBound() {
        return lowerBound;
    }

    /**
     * Returns true if the lower bound version in the version range is
     * inclusive.
     *
     * @return true if the lower bound version is inclusive. Otherwise,
     *         returns false.
     */
    boolean isLowerBoundInclusive() {
        return lowerBoundInclusive;
    }

    /**
     * Returns the upper bound version in the version range. If upper bound
     * version is {@code null}, the upper bound is infinity.
     *
     * @return the upper bound version.
     */
    Version getUpperBound() {
        return upperBound;
    }

    /**
     * Returns true if the upper bound version in the version range is
     * inclusive.
     *
     * @return true if the upper bound version is inclusive. Otherwise, returns
     *         false.
     */
    boolean isUpperBoundInclusive() {
        return upperBoundInclusive;
    }

    /**
     * Returns the normalized lower bound version in the version range.
     *
     * @return the normalized lower bound version.
     */
    Version getNormalizedLowerBound() {
        return normalizedLowerBound;
    }

    /**
     * Returns the normalized upper bound version in the version range.
     *
     * @return the normalized upper bound version.
     */
    Version getNormalizedUpperBound() {
        return normalizedUpperBound;
    }

    /**
     * Returns true if the string is a version range in valid format.
     *
     * @return true if the string is a version range in valid format.
     *         Otherwise, returns false.
     */
    static boolean isVersionRange(String source) {
        Matcher matcher = generalRangePattern.matcher(source);
        if (matcher.matches() == true)
            return true;

        matcher = openRangePattern.matcher(source);
        if (matcher.matches() == true)
            return true;

        matcher = familyRangePattern.matcher(source);
        return matcher.matches();
    }

    /**
     * Parses a string according to the version range format. The string must
     * not contain any leading or trailing whitespace.
     *
     * @param versionRange the string to be parsed.
     * @return A {@code Version} parsed from the string.
     * @throws IllegalArgumentException if the string cannot be parsed.
     */
    static VersionRange valueOf(String versionRange) {

        // Check if the version matches the general version range format
        if (generalRangePattern.matcher(versionRange).matches())  {

            // Parse out lower bound and upper bound versions
            StringTokenizer tokenizer = new StringTokenizer(versionRange, " ,[]()");

            boolean lowerBoundInclusive = versionRange.startsWith("[");
            Version lowerBound = Version.valueOf(tokenizer.nextToken());
            Version upperBound = Version.valueOf(tokenizer.nextToken());
            boolean upperBoundInclusive = versionRange.endsWith("]");

            return new VersionRange
                (lowerBound, lowerBoundInclusive, upperBound, upperBoundInclusive);
        }

        // Checks if the version matches the open version range format
        if (openRangePattern.matcher(versionRange).matches())  {

            // Truncate the last '+' character
            String version = versionRange.substring(0, versionRange.length() - 1);

            return new VersionRange(Version.valueOf(version));
        }

        // Checks if the version matches the family version range format
        if (familyRangePattern.matcher(versionRange).matches())  {

            // Parse out the version numbers
            StringTokenizer tokenizer = new StringTokenizer(versionRange, ".*");

            try {
                // Check if the pattern is "x.*"
                int major = convertVersionNumber(tokenizer.nextToken());
                if (tokenizer.hasMoreTokens() == false) {
                    return new VersionRange(Version.valueOf(major, 0, 0),
                        Version.valueOf(major + 1, 0, 0));
                }
                // Check if the pattern is "x.y.*"
                int minor = convertVersionNumber(tokenizer.nextToken());
                if (tokenizer.hasMoreTokens() == false) {
                    return new VersionRange(Version.valueOf(major, minor, 0),
                        Version.valueOf(major, minor + 1, 0));
                }

                // Check if the pattern is "x.y.z.*"
                int micro = convertVersionNumber(tokenizer.nextToken());
                if (tokenizer.hasMoreTokens() == false) {
                    return new VersionRange(Version.valueOf(major, minor, micro),
                        Version.valueOf(major, minor, micro + 1));
                }
            }
            catch(Exception e) {
                throw new IllegalArgumentException("Version range format is invalid: "
                        + versionRange, e);
            }
        }

        throw new IllegalArgumentException("Version range format is invalid: "
                + versionRange);
    }

    private static int convertVersionNumber(String versionNumber) {
        int result = 0;
        try {
            result = Integer.parseInt(versionNumber);
            if (result < 0)
                throw new IllegalArgumentException("Version number is invalid: "
                        + versionNumber);
        }
        catch(NumberFormatException e)    {
            throw new IllegalArgumentException("Version number is invalid: "
                    + versionNumber, e);
        }
        return result;
    }

    /**
     * Returns true if the specified version range is within this version range.
     *
     * @param versionRange the {@code VersionRange} object.
     * @return true if the specified version range is within this version
     *         range. Otherwise, returns false.
     */
    boolean contains(VersionRange versionRange) {

        // Compare version ranges using normalized lower bound and upper bound.
        //

        // Compare lower bound
        if (versionRange.normalizedLowerBound.compareTo(this.normalizedLowerBound) < 0)
            return false;

        // Check for infinity upper bound
        if (this.normalizedUpperBound == null)
            return true;

        // Check for infinity upper bound
        if (versionRange.normalizedUpperBound == null)
            return false;

        // Compare upper bound
        return (versionRange.normalizedUpperBound.compareTo(this.normalizedUpperBound) <= 0);
    }

    /**
     * Returns true if the specified version is within this version range.
     *
     * @param version the {@code Version} object.
     * @return true if the specified version is within this version range.
     *         Otherwise, returns false.
     */
    boolean contains(Version version) {
        version = version.trimQualifier();

        // Checks if version is less than normalized lower bound
        if (version.compareTo(this.normalizedLowerBound) < 0)
            return false;

        // Checks for infinity upper bound
        if (this.normalizedUpperBound == null)
            return true;

        // Checks if version greater than or equals to normalized upper bound
        if (version.compareTo(this.normalizedUpperBound) >= 0)
            return false;

        return true;
    }

    /**
     * Returns true if the specified version range intersects with this version
     * range.
     *
     * @param versionRange the {@code VersionRange} object.
     * @return true if the specified version range intersects with this version
     *         range. Otherwise, returns false.
     */
    boolean intersects(VersionRange versionRange) {

        // Compare version ranges using normalized lower bound and upper bound.
        //

        // If two version ranges intersect, one of the lower bounds must be
        // contained within the version range of the other.
        //
        return (this.contains(versionRange.normalizedLowerBound)
                || versionRange.contains(this.normalizedLowerBound));
    }

    /**
     * Intersects two version ranges and returns the version range of
     * intersection.
     *
     * @param versionRange1 the {@code VersionRange} object.
     * @param versionRange2 another {@code VersionRange} object.
     * @return the version range of intersection between two version ranges.
     *         Otherwise, returns null.
     */
    static VersionRange intersection(VersionRange versionRange1, VersionRange versionRange2) {

        // Checks for equality
        if (versionRange1.equals(versionRange2))
            return versionRange1;

        // Checks if two version ranges have intersection.
        if (versionRange1.intersects(versionRange2) == false)
            return null;

        // Intersect version ranges using normalized lower bound and upper bound.
        //
        Version lowerBound = null;
        Version upperBound = null;

        // The version ranges overlap, so the largest of the two lower bounds
        // of the version ranges is the lower bound of the intersecting version
        // range.
        //
        if (versionRange1.getNormalizedLowerBound().compareTo
            (versionRange2.getNormalizedLowerBound()) <= 0) {
            lowerBound = versionRange2.getNormalizedLowerBound();
        } else {
            lowerBound = versionRange1.getNormalizedLowerBound();
        }

        // The version ranges overlap, so the smallest of the two upper bounds
        // of the version ranges is the upper bound of the intersecting version
        // range.
        //
        if (versionRange1.getNormalizedUpperBound() != null
            && versionRange2.getNormalizedUpperBound() != null) {
            if (versionRange1.getNormalizedUpperBound().compareTo
                (versionRange2.getNormalizedUpperBound()) <= 0) {
                upperBound = versionRange1.getNormalizedUpperBound();
            } else {
                upperBound = versionRange2.getNormalizedUpperBound();
            }
        }
        else if (versionRange1.getNormalizedUpperBound() != null) {
            upperBound = versionRange1.getNormalizedUpperBound();
        }
        else {
            upperBound = versionRange2.getNormalizedUpperBound();
        }

        // Checks infinity upper bound
        if (upperBound == null)
            return new VersionRange(lowerBound);
        else
            return new VersionRange(lowerBound, upperBound);
    }

    /**
     * Merges two version ranges together and returns the combined version
     * range.
     *
     * @param versionRange1 the {@code VersionRange} object.
     * @param versionRange2 another {@code VersionRange} object.
     * @return the merged version range.
     * @throws IllegalArgumentException if two version ranges have no
     *   intersection or are not contiguous.
     */
    static VersionRange merge(VersionRange versionRange1, VersionRange versionRange2) {

        // Checks for equality
        if (versionRange1 == versionRange2)
            return versionRange1;
        if (versionRange1.equals(versionRange2))
            return versionRange1;

        // Checks if one version range contains another version range completely.
        if (versionRange1.contains(versionRange2))
            return versionRange1;

        if (versionRange2.contains(versionRange1))
            return versionRange2;

        // Checks if two version ranges have intersection.
        if (versionRange1.intersects(versionRange2) == false) {

            // Two version ranges have no intersection, but they may be contiguous.
            //

            // Swap version range so versionRange1 < versionRange2
            if (versionRange1.getNormalizedLowerBound().compareTo
                    (versionRange2.getNormalizedLowerBound()) > 0) {
                VersionRange temp = versionRange1;
                versionRange1 = versionRange2;
                versionRange2 = temp;
            }

            // if the version ranges are [x, y) and [y, z), then merges them into [x, z)
            if (versionRange1.getNormalizedUpperBound().equals
                    (versionRange2.getNormalizedLowerBound())) {

                // Merges contiguous range
                if (versionRange2.getNormalizedUpperBound() == null) {
                    return new VersionRange(versionRange1.getNormalizedLowerBound());
                } else {
                    return new VersionRange(versionRange1.getNormalizedLowerBound(),
                        versionRange2.getNormalizedUpperBound());
                }
            }
            else {
                throw new IllegalArgumentException("Version ranges that have no intersection "
                        + "or are not contiguous cannot be merged.");
            }
        }
        else {

            // Combine version ranges together using normalized lower bound and upper bound.
            //
            Version lowerBound = null;
            Version upperBound = null;

            // Two version ranges are overlapped, so the smallest of the two lower bounds
            // of the version ranges would be the lower bound of the merged version range.
            //
            if (versionRange1.getNormalizedLowerBound().compareTo
                    (versionRange2.getNormalizedLowerBound()) <= 0) {
                lowerBound = versionRange1.getNormalizedLowerBound();
            } else {
                lowerBound = versionRange2.getNormalizedLowerBound();
            }
            // Two version ranges are overlapped, so the largest of the two upper bounds
            // of the version ranges would be the upper bound of the merged version range.
            //
            if (versionRange1.getNormalizedUpperBound() != null
                    && versionRange2.getNormalizedUpperBound() != null) {
                if (versionRange1.getNormalizedUpperBound().compareTo
                        (versionRange2.getNormalizedUpperBound()) <= 0) {
                    upperBound = versionRange2.getNormalizedUpperBound();
                } else {
                    upperBound = versionRange1.getNormalizedUpperBound();
                }
            }

            // Checks infinity upper bound
            if (upperBound == null)
                return new VersionRange(lowerBound);
            else
                return new VersionRange(lowerBound, upperBound);
        }
    }

    /**
     * Returns a normalized version range.
     *
     * This method is used by {@code java.module.VersionConstraint}.
     */
    VersionRange normalize() {

        // Checks if this version range is already normalized.
        if (lowerBound == normalizedLowerBound && lowerBoundInclusive == true
            && upperBound == normalizedUpperBound && upperBoundInclusive == false) {
            return this;
        }
        else {
            if (normalizedUpperBound == null)
                return new VersionRange(normalizedLowerBound);
            else
                return new VersionRange(normalizedLowerBound, normalizedUpperBound);
        }
    }

    /**
     * Return a {@code VersionConstraint} object that represents this version
     * range.
     *
     * @return a {@code VersionConstraint} object.
     */
    VersionConstraint toVersionConstraint() {
        VersionConstraintBuilder builder = new VersionConstraintBuilder();
        builder.add(this);
        return builder.toVersionConstraint();
    }

    /**
     * Compare two {@code VersionRange} objects.
     *
     * @param versionRange the {@code VersionRange} to be compared.
     * @return the value 0 if the argument {@code Version} is equal to this
     *         {@code VersionRange}; a value less than 0 if this
     *         {@code VersionRange} is less than the {@code VersionRange}
     *         argument; and a value greater than 0 if this
     *         {@code VersionRange} is greater than the
     *         {@code VersionRange} argument.
     */
    @Override
    public int compareTo(VersionRange versionRange)   {
        if (this == versionRange)
            return 0;

        // Compare lower bound
        int result = normalizedLowerBound.compareTo(versionRange.normalizedLowerBound);
        if (result != 0)
            return result;

        // Compare upper bound

        // Checks for infinity
        if (normalizedUpperBound != null && versionRange.normalizedUpperBound != null)
            return normalizedUpperBound.compareTo(versionRange.normalizedUpperBound);
        else if (normalizedUpperBound == null)
            return 1;
        else if (versionRange.normalizedUpperBound == null)
            return -1;
        else
            return 0;
    }

    /**
     * Compare two {@code VersionRange} objects for equality. The result is
     * {@code true} if and only if the argument is not {@code null} and is a
     * {@code VersionRange} object that lowerBound, lowerBoundInclusive,
     * upperBound, and upperBoundInclusive the same as those of this
     * {@code VersionRange}.
     *
     * @param obj the object to compare with.
     * @return whether or not the two objects are equal
     */
    @Override
    public boolean equals(Object obj)   {
        if (this == obj)
            return true;

        if (!(obj instanceof VersionRange))
            return false;

        VersionRange vr = (VersionRange) obj;

        // Checks for infinity upper bound
        if (this.upperBound != null && vr.getUpperBound() != null) {
            return (normalizedLowerBound.equals(vr.getNormalizedLowerBound())
                && normalizedUpperBound.equals(vr.getNormalizedUpperBound()));
        }
        else if (this.upperBound == null && vr.getUpperBound() == null) {
            return (normalizedLowerBound.equals(vr.getNormalizedLowerBound()));
        }
        else {
            return false;
        }
    }

    /**
     * Returns a hash code for this {@code VersionRange}.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode()   {
        int result = 17;
        result = 37 * result + normalizedLowerBound.hashCode();
        result = 37 * result + (upperBound == null ? 0 : normalizedUpperBound.hashCode());
        return result;
    }

    /**
     * Returns a {@code String} object representing this {@code VersionRange}'s
     * value. The value is converted to the version range format and returned
     * as a string.
     *
     * @return a string representation of the value of this object in the
     *         version range format.
     */
    @Override
    public String toString() {

        // Checks for infinity upper bound
        if (upperBound == null) {
            return lowerBound.toShortString() + "+";
        }

        StringBuilder buffer = new StringBuilder();

        if (lowerBoundInclusive)
            buffer.append('[');
        else
            buffer.append('(');

        buffer.append(lowerBound.toShortString());
        buffer.append(", ");
        buffer.append(upperBound.toShortString());

        if (upperBoundInclusive)
            buffer.append(']');
        else
            buffer.append(')');

        return buffer.toString();
    }
}
