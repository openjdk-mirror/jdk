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

import java.io.File;
import java.io.IOException;
import java.module.Modules;
import java.module.Repository;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a Global repository.
 * @since 1.7
 */
public class GlobalRepositoryFactory extends RepositoryFactory {
    /** The currently executing platform. */
    private static final String platform = RepositoryUtils.getPlatform();

    private static final Map<String, String> locations = new HashMap<String, String>();

    static {
        locations.put("linux", "/usr/jdk/packages/lib/module");
        locations.put("solaris", "/usr/java/packages/lib/module");
        locations.put("windows", "%SystemRoot%\\Sun\\Java\\lib\\module");
    }

    /**
     * Creates a LocalRepository with the source location appropriate for the
     * current platform.
     *
     * @param parent the parent repository for delegation
     * @param name the repository name
     * @param sourceLocation the source location <em>Note:</em> Ignored by
     * this implementation; instead uses a platform-specific location.
     * @param config Map of configuration names to their values
     * @return a LocalRepository for the platform-specific location
     */
    public Repository create(Repository parent, String name,
            URL sourceLocation, Map<String, String> config) throws IOException {
        String sourceName = config.get(platform);
        if (sourceName == null) {
            sourceName = locations.get(platform);
        }
        return Modules.newLocalRepository(
            name, new File(sourceName), config, parent);
    }
}
