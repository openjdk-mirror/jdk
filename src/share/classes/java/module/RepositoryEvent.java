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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents an event that occurs in a repository.
 *
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleArchiveInfo
 * @see java.module.Repository
 * @see java.module.RepositoryListener
 *
 * @since 1.7
 */
public class RepositoryEvent {

    /**
     * Types of repository events.
     */
    public enum Type {

        /**
         * A repository has been initialized successfully.
         */
        REPOSITORY_INITIALIZED,

        /**
         * A repository has been shutdown successfully.
         */
        REPOSITORY_SHUTDOWN,

        /**
         * One or more module definitions have been added in a repository successfully.
         */
        MODULE_DEFINITION_ADDED,

        /**
         * One or more module definitions have been removed in a repository successfully.
         */
        MODULE_DEFINITION_REMOVED,

        /**
         * A module archive has been installed successfully in a repository.
         */
        MODULE_ARCHIVE_INSTALLED,

        /**
         * A module archive has been uninstalled successfully in a repository.
         */
        MODULE_ARCHIVE_UNINSTALLED
    };

    private Type type;
    private Repository source;
    private ModuleArchiveInfo info;
    private Set<ModuleDefinition> moduleDefs;

    /**
     * Constructs a {@code RepositoryEvent} instance using the specified
     * repository, event type, module archive information, and a set
     * of module definitions
     *
     * @param source the repository where the event occurs
     * @param type the event type
     * @param info the module archive information
     * @param moduleDefs a set of module definitions
     * @throws NullPointerException if source is {@code null} or type is
     *         {@code null}.
     * @throws IllegalArgumentException
     * <ul>
     *      <li><p>if type is {@link Type#MODULE_ARCHIVE_INSTALLED
     *             <tt>MODULE_ARCHIVE_INSTALLED</tt>} or
     *             {@link Type#MODULE_ARCHIVE_UNINSTALLED
     *             <tt>MODULE_ARCHIVE_UNINSTALLED</tt>}, and
     *             info is {@code null}, or</p></li>
     *      <li><p>if type is {@link Type#MODULE_DEFINITION_ADDED
     *             <tt>MODULE_DEFINITION_ADDED</tt>} or
     *             {@link Type#MODULE_DEFINITION_REMOVED
     *             <tt>MODULE_DEFINITION_REMOVED</tt>}, and
     *             moduleDefs is {@code null} or is an empty set.</p></li>
     * </ul>
     */
    public RepositoryEvent(Repository source, Type type, ModuleArchiveInfo info,
                    Set<ModuleDefinition> moduleDefs) {
        if (source == null)
            throw new NullPointerException("source must not be null.");

        if (type == null)
            throw new NullPointerException("type must not be null.");

        if ((type.equals(Type.MODULE_ARCHIVE_INSTALLED) || type.equals(Type.MODULE_ARCHIVE_UNINSTALLED))
             && info == null)
            throw new IllegalArgumentException("info must not be null with event type " + type);

        if ((type.equals(Type.MODULE_DEFINITION_ADDED) || type.equals(Type.MODULE_DEFINITION_REMOVED))
             && (moduleDefs == null || moduleDefs.size() == 0))
            throw new IllegalArgumentException("moduleDefs must not be null or empty set with event type " + type);

        this.type = type;
        this.source = source;
        this.info = info;
        if (moduleDefs != null) {
            this.moduleDefs = Collections.unmodifiableSet(moduleDefs);
        } else {
            this.moduleDefs = null;
        }
    }


    /**
     * Returns the event type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the repository where the event occurs.
     */
    public Repository getSource() {
        return source;
    }

    /**
     * Returns an unmodifiable set of module definitions.
     */
    public Set<ModuleDefinition> getModuleDefinitions() {
        return moduleDefs;
    }

    /**
     * Returns the module archive information.
     */
    public ModuleArchiveInfo getModuleArchiveInfo() {
        return info;
    }

    /**
     * Returns a {@code String} object representing this {@code RepositoryEvent}.
     *
     * @return a string representation of the {@code RepositoryEvent} object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("RepositoryEvent[type=");
        builder.append(type.toString());
        builder.append(",repository=");
        builder.append(getSource().getName());
        if (moduleDefs != null) {
            builder.append(",module-definitions=");
            builder.append(moduleDefs.toString());
        }
        if (info != null) {
            builder.append(",module-archive-info=");
            builder.append(info.toString());
        }
        builder.append("]");

        return builder.toString();
    }
}
