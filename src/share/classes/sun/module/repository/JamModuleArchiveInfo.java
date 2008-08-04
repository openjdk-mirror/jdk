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

import java.lang.ModuleInfo;
import java.module.ModuleArchiveInfo;
import java.module.ModuleContent;
import java.module.Repository;
import java.module.Version;
import java.nio.ByteBuffer;

/**
 * This class represents the information of an installed module archive
 * in a repository for the JAM module system.
 *
 * @see java.module.Repository
 * @see java.module.Version
 *
 * @since 1.7
 */
public final class JamModuleArchiveInfo implements ModuleArchiveInfo {
    private final Repository repository;
    private ByteBuffer metadataByteBuffer;
    private ModuleContent moduleContent;
    private final String path;

    // These fields are a reflection of the information in the module archive.
    private final String name;
    private final Version version;
    private final String platform;
    private final String arch;
    private final String fileName;
    private final long lastModified;

    /**
     * Constructs a new {@code JamModuleArchiveInfo} instance.
     * <p>
     * If the module archive is portable, both {@code platform} and
     * {@code arch} must be null.
     *
     * @param repository the repository
     * @param name the name of the module definition in the module archive.
     * @param version the version of the module definition in the module archive.
     * @param platform the platform which the module archive targets.
     * @param arch the architecture which the module archive targets.
     * @param path relative path to the source location (for URLRepository)
     * @throws NullPointerException if repository is null, name is null, or
     *         version is null. It is also thrown if platform is null but
     *         arch is not null, or platform is not null but arch is null.
     */
    public JamModuleArchiveInfo(Repository repository,
                                String name, Version version,
                                String platform, String arch,
                                String path) {
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
        if ((platform == null ^ arch == null)) {
            throw new IllegalArgumentException(
                "platform and arch must be either both provided, or neither provided.");
        }

        this.repository = repository;
        this.metadataByteBuffer = null;
        this.moduleContent = null;
        this.name = name;
        this.version = version;
        this.platform = platform;
        this.arch = arch;
        this.path = path;
        this.fileName = null;
        this.lastModified = 0;
    }

    /**
     * Constructs a new {@code JamModuleArchiveInfo} instance.
     * <p>
     * If the module archive is portable, both {@code platform} and
     * {@code arch} must be null.
     *
     * @param repository the repository
     * @param name the name of the module definition in the module archive.
     * @param version the version of the module definition in the module archive.
     * @param platform the platform which the module archive targets.
     * @param arch the architecture which the module archive targets.
     * @param fileName the filename of the module archive.
     * @param lastModified the last modified time of the module archive.
     * @param metadataByteBuffer the metadata byte buffer
     * @param moduleContent the ModuleContent object
     * @throws NullPointerException if repository is null, name is null,
     *         version is null, last modified time is less than 0,
     *         metadataByteBuffer is null or moduleContent is null. It
     *         is also thrown if platform is null but arch is not null, or
     *         platform is not null but arch is null.
     */
    public JamModuleArchiveInfo(Repository repository, String name,
                             Version version, String platform, String arch,
                             String fileName, long lastModified,
                             ByteBuffer metadataByteBuffer,
                             ModuleContent moduleContent) {
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
        if ((platform == null ^ arch == null)) {
            throw new IllegalArgumentException(
                "platform and arch must be either both provided, or neither provided.");
        }
        if (lastModified < 0) {
            throw new IllegalArgumentException(
                "lastModified must be greater than or equal to 0.");
        }
        if (metadataByteBuffer == null) {
            throw new IllegalArgumentException(
                "metadataByteBuffer must not be null.");
        }
        if (moduleContent == null) {
            throw new IllegalArgumentException(
                "moduleContent must not be null.");
        }

        this.repository = repository;
        this.metadataByteBuffer = metadataByteBuffer;
        this.moduleContent = moduleContent;
        this.name = name;
        this.version = version;
        this.platform = platform;
        this.arch = arch;
        this.fileName = fileName;
        this.lastModified = lastModified;
        this.path = null;
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
        return platform;
    }

    @Override
    public String getArch() {
        return arch;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    public String getPath() {
        return path;
    }

    String getCanonicalizedPath()  {
        return getCanonicalizedPath('/');
    }

    String getCanonicalizedPath(char separatorChar)  {
        if (path != null) {
            return path;
        }

        if (isPortable()) {
            return getName() + separatorChar + getVersion();
        } else {
            return getName() + separatorChar
                   + getVersion() + separatorChar
                   + getPlatform() + "-" + getArch();
        }
    }

    /**
     * Determines if the module archive is portable.
     *
     * @return true if the module archive is portable; otherwise return false.
     */
    boolean isPortable() {
        return (platform == null && arch == null);
    }

    /**
     * Returns the metadata byte buffer.
     *
     * @return the metadata byte buffer, or null if not known.
     */
    ByteBuffer getMetadataByteBuffer() {
        return metadataByteBuffer;
    }

    /**
     * Set the metadata byte buffer.
     *
     * @param bf a byte buffer which contains the metadata
     */
    void setMetadataByteBuffer(ByteBuffer bf) {
        metadataByteBuffer = bf;
    }

    /**
     * Returns the module content.
     *
     * @return the module content, or null if not known.
     */
    ModuleContent getModuleContent() {
        return moduleContent;
    }

    /**
     * Set the module content
     *
     * @param content the module content
     */
    void setModuleContent(ModuleContent content) {
        moduleContent = content;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof JamModuleArchiveInfo)) {
            return false;
        }

        JamModuleArchiveInfo jmai = (JamModuleArchiveInfo) other;
        if (!name.equals(jmai.name) || !version.equals(jmai.version)) {
            return false;
        }

        if (isPortable() && jmai.isPortable()) {
            return true;
        } else if (isPortable() || jmai.isPortable()) {
            return false;
        } else {
            return (platform.equals(jmai.platform)
                    && arch.equals(jmai.arch));
        }
    }

    @Override
    public int hashCode() {
        // Hash code is based on name, version, platform, and arch
        int rc = name.hashCode();
        rc = 31 * rc + version.hashCode();
        rc = 31 * rc + (platform == null ? 0 : platform.hashCode());
        rc = 31 * rc + (arch == null ? 0 : arch.hashCode());
        return rc;
    }

    /**
     * Returns a {@code String} object representing this
     * {@code JamModuleArchiveInfo}.
     *
     * @return a string representation of the {@code JamModuleArchiveInfo} object.
     */
    @Override
    public String toString()    {
        StringBuilder builder = new StringBuilder();

        builder.append("JamModuleArchiveInfo[repository=");
        builder.append(repository.getName());
        builder.append(",module=");
        builder.append(name);
        builder.append(" v");
        builder.append(version);
        if (!isPortable()) {
            builder.append(",platform-arch=");
            builder.append(platform);
            builder.append("-");
            builder.append(arch);
        }
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
