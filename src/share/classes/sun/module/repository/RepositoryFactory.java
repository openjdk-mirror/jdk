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

import java.io.IOException;
import java.module.Repository;
import java.net.URL;
import java.util.Map;

/**
 * Provides a means for creating a repository.
 * @since 1.7
 */
public abstract class RepositoryFactory {
    /**
     * Creates a repository.
     *
     * @param parent the parent repository for delegation
     * @param name the repository name
     * @param sourceLocation the source location
     * @param config Map of configuration names to their values
     * @return a Repository
     * @throws IOException if the repository cannot be initialized.
     */
    public abstract Repository create(Repository parent, String name,
            URL sourceLocation, Map<String, String> config) throws IOException;
}
