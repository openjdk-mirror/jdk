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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;


/**
 * This class provides an API for building version constraint.
 *
 * <p>The principal operation on a {@code VersionConstraintBuilder} is the
 * {@code add} method, which is overloaded so as to accept version and
 * version range. Each effectively converts a version or a version range
 * and normalizes it in the version constraint builder.
 * <p>
 * For example, if the version constraint builder has "1.3;[2.0, 3.0)"
 * as version constrains, the adding a new version 2.5 would have no effect,
 * while adding a new version range [3.0, 3.6) would alter the version
 * constraint builder to contain "1.3;[2.0, 3.6)" as the version constraints.
 * <p>
 * <p>Instances of {@code VersionConstraintBuilder} are not safe for
 * use by multiple threads.
 *
 * @see java.module.Version
 * @see java.module.VersionRange
 * @see java.module.VersionConstraint
 * @since 1.7
 */
class VersionConstraintBuilder {

    // Regular constraints with versions and version ranges.
    private final List<Object> constraints = new ArrayList<Object>();

    // Normalized constraints with versions and version ranges. If possible,
    // overlapping versions and version ranges would be combined, and
    // contiguous version ranges would be merged.
    private final List<Object> normalizedConstraints = new ArrayList<Object>();

    /**
     * Package private Constructor
     */
    VersionConstraintBuilder() {
    }

    /**
     * Adds a version to the version constraint builder.
     */
    VersionConstraintBuilder add(Version version) {

        // Add version to the list of regular version constraints.
        constraints.add(version);

        // Update normalized constraints
        for (Object ncs : normalizedConstraints) {
            if (ncs instanceof Version) {
                Version v = (Version) ncs;
                // Checks if the specified version is already covered in the
                // normalized version constraints.
                if (v.equals(version) == true)
                    return this;
            }
            else if (ncs instanceof VersionRange) {
                VersionRange vr = (VersionRange) ncs;
                // Checks if the version range in the normalized version
                // constraints already covers the specified version.
                if (vr.contains(version))
                    return this;
            }
        }

        // The specified version is not yet covered, add to the the normalized
        // version constraints.
        normalizedConstraints.add(version);
        return this;
    }

    /**
     * Adds a version range to the version constraint builder.
     */
    VersionConstraintBuilder add(VersionRange versionRange) {

        // Add version range to the list of regular version constraints.
        constraints.add(versionRange);

        List<Object> constraintsToBeRemoved = new ArrayList<Object>();

        for (Object ncs : normalizedConstraints) {
            if (ncs instanceof Version) {
                Version v = (Version) ncs;
                // Checks if the specified version range already covers
                // a version in the normalized version constraints.
                if (versionRange.contains(v))
                    constraintsToBeRemoved.add(v);
            }
            else if (ncs instanceof VersionRange) {
                VersionRange vr = (VersionRange) ncs;

                // Checks if the specified version range can merge with
                // a version range in the normalized version constraints.
                try {
                    versionRange = VersionRange.merge(vr, versionRange);
                    constraintsToBeRemoved.add(vr);
                }
                catch(IllegalArgumentException ex) {
                    // Version ranges are not contiguous or have no intersection.
                }
            }
        }

        // Remove redundent constraints
        normalizedConstraints.removeAll(constraintsToBeRemoved);

        // Add normalized version range as new constraint
        normalizedConstraints.add(versionRange.normalize());
        return this;
    }

    /**
     * Generates a version constraint from the version constraint builder.
     */
    VersionConstraint toVersionConstraint() {

        // Sort normalized constraints in ascending order.
        //
        Collections.sort(normalizedConstraints, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                if (o1 instanceof Version && o2 instanceof Version) {
                    return ((Version) o1).compareTo((Version) o2);
                }
                else if (o1 instanceof VersionRange && o2 instanceof VersionRange) {
                    return ((VersionRange) o1).compareTo((VersionRange) o2);
                }

                if (o1 instanceof VersionRange && o2 instanceof Version) {
                    Version v1 = ((VersionRange) o1).getNormalizedLowerBound();
                    Version v2 = (Version) o2;
                    return v1.compareTo(v2);
                }
                else {
                    Version v1 = (Version) o1;
                    Version v2 = ((VersionRange) o2).getNormalizedLowerBound();
                    return v1.compareTo(v2);
                }
            }
        });

        return new VersionConstraint(Collections.unmodifiableList(constraints),
        Collections.unmodifiableList(normalizedConstraints));
    }
}
