/*
 * Copyright 2004-2006 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.util.List;
import java.util.ArrayList;

/**
 * Day of week enum.
 *
 * @since 1.6
 */

public class DayOfWeek {
    public static final DayOfWeek SUNDAY = new DayOfWeek("Sun", 0);
    public static final DayOfWeek MONDAY = new DayOfWeek("Mon", 1);
    public static final DayOfWeek TUESDAY = new DayOfWeek("Tue", 2);
    public static final DayOfWeek WEDNESDAY = new DayOfWeek("Wed", 3);
    public static final DayOfWeek THURSDAY = new DayOfWeek("Thu", 4);
    public static final DayOfWeek FRIDAY = new DayOfWeek("Fri", 5);
    public static final DayOfWeek SATURDAY = new DayOfWeek("Sat", 6);

    public static List values() {
    	List list = new ArrayList();
    	list.add(SUNDAY);
    	list.add(MONDAY);
    	list.add(TUESDAY);
    	list.add(WEDNESDAY);
    	list.add(THURSDAY);
    	list.add(FRIDAY);
    	list.add(SATURDAY);
    	return list;
    }

    private final String abbr;
    private final int ordinal;

    private DayOfWeek(String abbr, int ordinal) {
        this.abbr = abbr;
        this.ordinal = ordinal;
    }

    String getAbbr() {
        return abbr;
    }

    int value() {
        return ordinal + 1;
    }
}
