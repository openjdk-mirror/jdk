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
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Creates a repository based on existence of a suitable location in the
 * user's home directory.
 *
 * @since 1.7
 */
public class UserRepositoryFactory extends RepositoryFactory {

    /**
     * Creates a local repository.
     *
     * @param parent the parent repository for delegation.
     * @param name the repository name.
     * @param sourceLocation the source location.
     * @param config Map of configuration names to their values
     * @return a local repository.
     */
    public Repository create(Repository parent, String name,
            URL sourceLocation, Map<String, String> config) throws IOException {

        if ("file".equals(sourceLocation.getProtocol()) == false) {
            throw new IOException("Source location in repository \'" + name
                                  + "\' must be specified using file: URL");
        }

        File f = new File(sourceLocation.getFile()).getCanonicalFile();
        return Modules.newLocalRepository(parent, name, f, config);
    }
}
