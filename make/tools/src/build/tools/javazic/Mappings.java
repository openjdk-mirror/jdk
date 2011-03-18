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

import  java.util.ArrayList;
import  java.util.HashMap;
import  java.util.HashSet;
import  java.util.Iterator;
import  java.util.LinkedList;
import  java.util.List;
import  java.util.Map;
import  java.util.Set;
import  java.util.TreeMap;
import  java.util.TreeSet;

/**
 * <code>Mappings</code> generates two Maps and a List which are used by
 * javazic BackEnd.
 *
 * @since 1.4
 */
class Mappings {
    // All aliases specified by Link statements. It's alias name to
    // real name mappings.
    private Map aliases;

    private List rawOffsetsIndex;

    private List rawOffsetsIndexTable;

    // Zone names to be excluded from rawOffset table. Those have GMT
    // offsets to change some future time.
    private List excludeList;

    /**
     * Constructor creates some necessary instances.
     */
    Mappings() {
        aliases = new TreeMap();
        rawOffsetsIndex = new LinkedList();
        rawOffsetsIndexTable = new LinkedList();
    }

    /**
     * Generates aliases and rawOffsets tables.
     * @param zi a Zoneinfo containing Zones
     */
    void add(Zoneinfo zi) {
        Map zones = zi.getZones();

        for (String zoneName : zones.keySet()) {
            Zone zone = (Zone)zones.get(zoneName);
            String zonename = (String)zone.getName();
            int rawOffset = zone.get(zone.size()-1).getGmtOffset();

            // If the GMT offset of this Zone will change in some
            // future time, this Zone is added to the exclude list.
            boolean isExcluded = false;
            if (zone.size() > 1) {
                ZoneRec zrec = zone.get(zone.size()-2);
                if ((zrec.getGmtOffset() != rawOffset)
                    && (zrec.getUntilTime(0) > Time.getCurrentTime())) {
                    if (excludeList == null) {
                        excludeList = new ArrayList();
                    }
                    excludeList.add(zone.getName());
                    isExcluded = true;
                }
            }

            if (!rawOffsetsIndex.contains(new Integer(rawOffset))) {
                // Find the index to insert this raw offset zones
                int n = rawOffsetsIndex.size();
                int i;
                for (i = 0; i < n; i++) {
                    if (((Integer)rawOffsetsIndex.get(i)).intValue() > rawOffset) {
                        break;
                    }
                }
                rawOffsetsIndex.add(i, new Integer(rawOffset));

                Set perRawOffset = new TreeSet();
                if (!isExcluded) {
                    perRawOffset.add(zonename);
                }
                rawOffsetsIndexTable.add(i, perRawOffset);
            } else if (!isExcluded) {
                int i = rawOffsetsIndex.indexOf(new Integer(rawOffset));
                Set perRawOffset = (Set)rawOffsetsIndexTable.get(i);
                perRawOffset.add(zonename);
            }
        }

        Map a = zi.getAliases();
        // If there are time zone names which refer to any of the
        // excluded zones, add those names to the excluded list.
        if (excludeList != null) {
            for (String zoneName : a.keySet()) {
                String realname = (String)a.get(zoneName);
                if (excludeList.contains(realname)) {
                    excludeList.add(zoneName);
                }
            }
        }
        aliases.putAll(a);
    }

    /**
     * Adds valid aliases to one of per-RawOffset table and removes
     * invalid aliases from aliases List. Aliases referring to
     * excluded zones are not added to a per-RawOffset table.
     */
    void resolve() {
        int index = rawOffsetsIndexTable.size();
        List toBeRemoved = new ArrayList();
        for (String key : aliases.keySet()) {
            boolean validname = false;
            for (int j = 0; j < index; j++) {
                Set perRO = (Set)rawOffsetsIndexTable.get(j);
                boolean isExcluded = (excludeList == null) ?
                                        false : excludeList.contains(key);

                if ((perRO.contains(aliases.get(key)) || isExcluded)
                    && Zone.isTargetZone(key)) {
                    validname = true;
                    if (!isExcluded) {
                        perRO.add(key);
                        Main.info("Alias <"+key+"> added to the list.");
                    }
                    break;
                }
            }

            if (!validname) {
                Main.info("Alias <"+key+"> removed from the list.");
                toBeRemoved.add(key);
            }
        }

        // Remove zones, if any, from the list.
        for (String key : toBeRemoved) {
            aliases.remove(key);
        }
    }

    Map getAliases() {
        return(aliases);
    }

    List getRawOffsetsIndex() {
        return(rawOffsetsIndex);
    }

    List getRawOffsetsIndexTable() {
        return(rawOffsetsIndexTable);
    }

    List getExcludeList() {
        return excludeList;
    }
}
