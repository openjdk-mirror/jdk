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
 * This class represents the information of an installed module archive
 * in the repository.
 *
 * @see java.module.Repository
 * @see java.module.Version
 *
 * @since 1.7
 */
public class ModuleArchiveInfo {
    private Repository repository;

    // These fields are a reflection of the information in the module archive.
    private String name;
    private Version version;
    private String platform;
    private String arch;
    private String fileName;
    private long lastModified;

    /**
     * Constructs a new <code>ModuleArchiveInfo</code> instance.
     *
     * @param repository the repository
     * @param name the name of the module definition in the module archive.
     * @param version the version of the module definition in the module
     *        archive.
     * @param platform the platform of the module definition in the module
     *        archive.
     * @param arch the architecture of the module definition in the module
     *        archive.
     * @param fileName the filename of the module archive.
     * @param lastModified the last modified time of the module archive.
     * @throws NullPointerException if repository is null, name is null, or
     *         version is null. It is also thrown if platform or archive is
     *         provided, they are not provided together, or if the last
     *         modified time is less than 0.
     */
    public ModuleArchiveInfo(Repository repository, String name, Version version, String platform, String arch, String fileName, long lastModified) {
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
                "platform and architecture must be either both provided, or neither provided.");
        }

        if (lastModified <0) {
            throw new IllegalArgumentException(
                "lastModified must be greater than or equal to 0.");
        }

        this.repository = repository;
        this.name = name;
        this.version = version;
        this.platform = platform;
        this.arch = arch;
        this.fileName = fileName;
        this.lastModified = lastModified;
    }

    /**
     * Returns the repository where the module archive is stored.
     *
     * @return the repository.
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * Returns the name of the module definition in the module archive.
     *
     * @return the name of the module definition.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the version of the module definition in the module archive.
     *
     * @return the version of the module definition.
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Returns the name of the platform of the module definition in the
     * module archive. The value should be one of the possible values
     * of the system property "os.platform".
     *
     * @return the name of the platform. If the module definition has no
     *          platform binding, returns null.
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * Returns the name of the architecture of the module definition in the
     * module archive. The value should be one of the possible values of
     * the system property "os.arch".
     *
     * @return the name of the architecture. If the module definition has no
     *          platform binding, returns null.
     */
    public String getArchitecture() {
        return arch;
    }

    /**
     * Returns the filename of the module archive.
     *
     * @return the filename of the module archive. If the module archive does not
     *          have a filename, return null.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the last modified time of the module archive in the repository. The
     * result is the number of milliseconds since January 1, 1970 GMT.
     *
     * @return the time the module archive was last modified, or 0 if not known.
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Compare two <code>ModuleArchiveInfo</code> objects for
     * equality. The result is <code>true</code> if and only if
     * the argument is not <code>null</code> and is a
     * <code>ModuleArchiveInfo</code> object that is the same
     * instance as this <code>ModuleArchiveInfo</code>.
     *
     * @param other the object to compare with.
     * @return whether or not the two objects are equal.
     */
    @Override
    public boolean equals(Object other) {
        return (this == other);
    }

    @Override
    public int hashCode() {
        int rc = repository.hashCode();
        rc = 31 * rc + name.hashCode();
        rc = 31 * rc + version.hashCode();
        rc = 31 * rc + (platform == null ? 0 : platform.hashCode());
        rc = 31 * rc + (arch == null ? 0 : arch.hashCode());
        return rc;
    }

    /**
     * Returns a <code>String</code> object representing this
     * <code>ModuleArchiveInfo</code>.
     *
     * @return a string representation of the <code>ModuleArchiveInfo</code> object.
     */
    @Override
    public String toString()    {
        StringBuilder builder = new StringBuilder();

        builder.append("ModuleArchiveInfo[repository=");
        builder.append(repository.getName());
        builder.append(",name=");
        builder.append(name);
        builder.append(",version=");
        builder.append(version);
        if (platform != null) {
            builder.append(",platform=");
            builder.append(platform);
        }
        if (arch != null) {
            builder.append(",arch=");
            builder.append(arch);
        }
        if (fileName != null) {
            builder.append(",fileName=");
            builder.append(fileName);
        }
        if (lastModified >= 0) {
            builder.append(",lastModified=");
            builder.append(lastModified);
        }
        builder.append("]");

        return builder.toString();
    }
}
