/*
 * Copyright 2000-2004 Sun Microsystems, Inc.  All Rights Reserved.
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

package build.tools.javazic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Month enum handles month related manipulation.
 *
 * @since 1.4
 */
public class Month {
    public static final Month JANUARY = new Month("Jan", 0);
    public static final Month FEBRUARY = new Month("Feb", 1);
    public static final Month MARCH = new Month("Mar", 2);
    public static final Month APRIL = new Month("Apr", 3);
    public static final Month MAY = new Month("May", 4);
    public static final Month JUNE = new Month("Jun", 5);
    public static final Month JULY = new Month("Jul", 6);
    public static final Month AUGUST = new Month("Aug", 7);
    public static final Month SEPTEMBER = new Month("Sep", 8);
    public static final Month OCTOBER = new Month("Oct", 9);
    public static final Month NOVEMBER = new Month("Nov", 10);
    public static final Month DECEMBER = new Month("Dec", 11);

    public static final List values() {
    	List result = new ArrayList();
    	result.add(JANUARY);
    	result.add(FEBRUARY);
    	result.add(MARCH);
    	result.add(APRIL);
    	result.add(MAY);
    	result.add(JUNE);
    	result.add(JULY);
    	result.add(AUGUST);
    	result.add(SEPTEMBER);
    	result.add(OCTOBER);
    	result.add(NOVEMBER);
    	result.add(DECEMBER);
    	return result;    	
    }

    private final String abbr;
    private final int ordinal;

    private static final Map abbreviations
                                = new HashMap(12);

    static {
        for (Month m : Month.values()) {
            abbreviations.put(m.abbr, m);
        }
    }

    private Month(String abbr, int ordinal) {
        this.abbr = abbr;
        this.ordinal = ordinal;
    }

    int value() {
        return ordinal + 1;
    }

    /**
     * Parses the specified string as a month abbreviation.
     * @param name the month abbreviation
     * @return the Month value
     */
    static Month parse(String name) {
        Month m = (Month)abbreviations.get(name);
        if (m != null) {
            return m;
        }
        return null;
    }

    /**
     * @param month the nunmth number (1-based)
     * @return the month name in uppercase of the specified month
     */
    static String toString(int month) {
        if (month >= JANUARY.value() && month <= DECEMBER.value()) {
            return "Calendar." + Month.values().get(month - 1);
        }
        throw new IllegalArgumentException("wrong month number: " + month);
    }
}
