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

import java.lang.reflect.Superpackage;
import java.net.URL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.module.ModuleDefinition;
import java.module.ModuleArchiveInfo;
import java.module.Query;
import java.module.Repository;

/**
 * Implementation of the bootstrap repository. It contains the standard module
 * definitions in the Java SE platform.
 *
 * @since 1.7
 */
public final class BootstrapRepository extends Repository {

    private static final BootstrapRepository INSTANCE = new BootstrapRepository();

    private List<ModuleDefinition> moduleDefs;
    private List<ModuleArchiveInfo> moduleArchiveInfos;
    private boolean initialized = false;

    public static Repository getInstance() {
        return INSTANCE;
    }

    private BootstrapRepository() {
        super(null, "bootstrap", null, new BootstrapModuleSystem());
    }

    /**
     * Initialize the bootstrap repostory lazily.
     */
    private void lazyInitialize() {
        if (initialized) {
            return;
        }

        initialized = true;

        // Obtains the set of standard module definitions.
        moduleDefs = VirtualModuleDefinitions.getModuleDefinitions();

        // Setup ModuleArchiveInfo for the standard module definition.
        moduleArchiveInfos = new ArrayList<ModuleArchiveInfo>();
        for (ModuleDefinition md : moduleDefs) {
            moduleArchiveInfos.add(new ModuleArchiveInfo(this,
                                        md.getName(), md.getVersion(),
                                        null, null, null, 0));
        }
    }

    @Override
    public void initialize() {
        if (initialized)
            throw new IllegalStateException("Bootstrap repository has already been initialized.");

        lazyInitialize();
    }

    @Override
    public List<ModuleDefinition> findModuleDefinitions(Query constraint) {
        lazyInitialize();

        if (constraint == Query.ANY) {
            return Collections.unmodifiableList(moduleDefs);
        } else {
            List<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
            for (ModuleDefinition md : moduleDefs) {
                if (constraint.match(md)) {
                    result.add(md);
                }
            }
            return result;
        }
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
    public void reload() throws IOException {
        // empty
    }

    @Override
    public ModuleArchiveInfo install(URL u) throws IOException {
        throw new IOException("Bootstrap repository is read-only.");
    }

    @Override
    public boolean uninstall(ModuleArchiveInfo m) throws IOException {
        throw new IOException("Bootstrap repository is read-only.");
    }

    @Override
    public void shutdown() throws IOException {
        throw new IOException("Bootstrap repository cannot be shutdown.");
    }

    @Override
    public List<ModuleArchiveInfo> list() {
        lazyInitialize();
        return Collections.unmodifiableList(moduleArchiveInfos);
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
