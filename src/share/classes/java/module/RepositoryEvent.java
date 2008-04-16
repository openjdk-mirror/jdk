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

/**
 * This class represents a repository event that occurs in a repository.
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
         * A module definition has been installed successfully in a repository.
         */
        MODULE_INSTALLED,

        /**
         * A module definition has been uninstalled successfully in a repository.
         */
        MODULE_UNINSTALLED
    };

    private Type type;
    private Repository source;
    private ModuleArchiveInfo info;

    /**
     * Constructs a {@code RepositoryEvent} object with the specified repository,
     * and event type.
     *
     * @param source the repository where the event occurs
     * @param type the event type
     * @throws NullPointerException if source is null or type is null.
     */
    public RepositoryEvent(Repository source, Type type) {
        if (source == null)
            throw new NullPointerException("source must not be null.");

        if (type == null)
            throw new NullPointerException("type must not be null.");

        this.type = type;
        this.source = source;
        this.info = null;
    }

    /**
     * Constructs a {@code RepositoryEvent} object with the specified repository,
     * event type, and module archive information.
     *
     * @param source the repository where the event occurs
     * @param type the event type
     * @param info the module archive information
     * @throws NullPointerException if source is null, type is null, or
     *         info is null.
     */
    public RepositoryEvent(Repository source, Type type, ModuleArchiveInfo info) {
        if (source == null)
            throw new NullPointerException("source must not be null.");

        if (type == null)
            throw new NullPointerException("type must not be null.");

        if (info == null)
            throw new NullPointerException("info must not be null.");

        this.type = type;
        this.source = source;
        this.info = info;
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
     * Returns the module archive information associated with the event.
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
        if (info != null) {
            builder.append(",module-archive-info=");
            builder.append(info.toString());
        }
        builder.append("]");

        return builder.toString();
    }
}
