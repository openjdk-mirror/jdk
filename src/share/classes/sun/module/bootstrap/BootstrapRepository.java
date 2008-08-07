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

package sun.module.bootstrap;

import java.net.URI;
import java.net.URL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.module.ModuleArchiveInfo;
import java.module.ModuleDefinition;
import java.module.ModuleSystemPermission;
import java.module.Query;
import java.module.Repository;
import sun.module.repository.JamModuleArchiveInfo;

/**
 * Implementation of the bootstrap repository. It contains the standard module
 * definitions in the Java SE platform.
 *
 * @since 1.7
 */
public final class BootstrapRepository extends Repository {

    private static final BootstrapRepository INSTANCE = new BootstrapRepository();

    public static Repository getInstance() {
        return INSTANCE;
    }

    private BootstrapRepository() {
        super("bootstrap", null);
    }

    @Override
    protected List<ModuleArchiveInfo> doInitialize() {
        // Obtains the set of standard module definitions.
        List<ModuleDefinition> moduleDefs = VirtualModuleDefinitions.getModuleDefinitions();

        List<ModuleArchiveInfo> moduleArchiveInfos = new ArrayList<ModuleArchiveInfo>();
        for (ModuleDefinition md : moduleDefs) {
            moduleArchiveInfos.add(md.getModuleArchiveInfo());
        }

        addModuleDefinitions(new HashSet<ModuleDefinition>(moduleDefs));

        return moduleArchiveInfos;
    }

    @Override
    protected void doShutdown() throws IOException {
        throw new IOException("Bootstrap repository cannot be shutdown.");
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean supportsReload() {
        return false;
    }

    @Override
    public List<ModuleArchiveInfo> list() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("listModuleArchive"));
        }

        // lazy initialize
        try {
            initialize();
        } catch(IOException e) {
            throw new AssertionError("bootstrap repository internal error: cannot initialize.");
        }

        return getModuleArchiveInfos();
    }

    @Override
    public List<ModuleDefinition> findModuleDefinitions(Query q) {
        // lazy initialize
        try {
            initialize();
        } catch(IOException e) {
            throw new AssertionError("bootstrap repository internal error: cannot initialize.");
        }

        return super.findModuleDefinitions(q);
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
