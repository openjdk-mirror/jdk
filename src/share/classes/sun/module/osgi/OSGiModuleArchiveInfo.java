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

package sun.module.osgi;

import java.module.ModuleArchiveInfo;
import java.module.Repository;
import java.module.Version;

/**
 * This class represents the information of an installed module archive
 * in a repository for the OSGi module system.
 *
 * @see java.module.Repository
 * @see java.module.Version
 *
 * @since 1.7
 */
final class OSGiModuleArchiveInfo implements ModuleArchiveInfo {
    private Repository repository;

    // These fields are a reflection of the information in the module archive.
    private String name;
    private Version version;
    private String fileName;
    private long lastModified;

    /**
     * Constructs a new {@code OSGiModuleArchiveInfo} instance.
     * <p>
     *
     * @param repository the repository
     * @param name the name of the module definition in the module archive.
     * @param version the version of the module definition in the module archive.
     * @param fileName the filename of the module archive.
     * @param lastModified the last modified time of the module archive.
     * @throws NullPointerException if repository is null, name is null,
     *         version is null, or the last modified time is less than 0.
     */
    public OSGiModuleArchiveInfo(Repository repository, String name,
                             Version version, String fileName, long lastModified) {
        if (repository == null) {
            throw new IllegalArgumentException("repository must not be null.");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null.");
        }
        if (version == null) {
            throw new IllegalArgumentException(
                "version must not be null.");
        }
        if (lastModified <0) {
            throw new IllegalArgumentException(
                "lastModified must be greater than or equal to 0.");
        }

        this.repository = repository;
        this.name = name;
        this.version = version;
        this.fileName = fileName;
        this.lastModified = lastModified;
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public String getPlatform() {
        return null;
    }

    @Override
    public String getArch() {
        return null;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the last modified time of the module archive in the repository. The
     * result is the number of milliseconds since January 1, 1970 GMT.
     *
     * @return the time the module archive was last modified, or 0 if not known.
     */
    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public String toString()    {
        StringBuilder builder = new StringBuilder();

        builder.append("OSGiModuleArchiveInfo[repository=");
        builder.append(repository.getName());
        builder.append(",module=");
        builder.append(name);
        builder.append(" v");
        builder.append(version);
        if (fileName != null) {
            builder.append(",fileName=");
            builder.append(fileName);
        }
        if (lastModified >= 0) {
            builder.append(",lastModified=");
            builder.append(new java.util.Date(lastModified));
        }
        builder.append("]");

        return builder.toString();
    }
}
