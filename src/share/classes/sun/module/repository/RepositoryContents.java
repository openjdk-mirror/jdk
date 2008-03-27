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

package sun.module.repository;

import java.module.ModuleArchiveInfo;
import java.module.ModuleDefinition;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set of mappings from ModuleArchiveInfo to ModuleDefinition
 * instances.
 *
 * @since 1.7
 */
class RepositoryContents {
    private final List<ModuleArchiveInfo> maiList = new ArrayList<ModuleArchiveInfo>();
    private final List<ModuleDefinition> mdList = new ArrayList<ModuleDefinition>();

    RepositoryContents() { }

    /**
     * Adds the given ModuleDefinition and ModuleArchiveInfo to this contents.
     * The ModuleArchiveInfo must not be null; the ModuleDefinition can be null.
     */
    ModuleDefinition put(ModuleArchiveInfo mai, ModuleDefinition md) {
        ModuleDefinition rc = null;
        if (mai != null) {
            int index = maiList.indexOf(mai);
            if (index > -1) {
                maiList.remove(index);
                rc = mdList.remove(index);
            }
            maiList.add(mai);
            mdList.add(md);
        }
        return rc;
    }

    /**
     * Returns {@code the ModuleDefinition} corresponding to the given {@code
     * ModuleArchiveInfo}, or null if not found.
     */
    ModuleDefinition get(ModuleArchiveInfo mai) {
        ModuleDefinition rc = null;
        int index = maiList.indexOf(mai);
        if (index > -1) {
            rc = mdList.get(index);
        }
        return rc;
    }

    /**
     * Returns true iff this {@code RepositoryContents} contains the given
     * {@code ModuleArchiveInfo}.
     */
    boolean contains(ModuleArchiveInfo mai) {
        return maiList.indexOf(mai) > -1;
    }

    List<ModuleDefinition> getModuleDefinitions() { return mdList; }

    List<ModuleArchiveInfo> getModuleArchiveInfos() { return maiList; }

    /**
     * Removes the given {@code ModuleArchiveInfo} and corresponding
     * {@code ModuleDefinition}.
     * @return true if they were removed, false if they weren't present.
     */
    boolean remove(ModuleArchiveInfo mai) {
        boolean rc = false;
        int index = maiList.indexOf(mai);
        if (index > -1) {
            maiList.remove(index);
            mdList.remove(index);
            rc = true;
        }
        return rc;
    }

    /**
     * Removes all {@code ModuleArchiveInfo} and {@code ModuleDefinition}
     * elements.
     */
    void clear() {
        maiList.clear();
        mdList.clear();
    }
}
