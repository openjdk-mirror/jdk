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

import java.module.ModuleArchiveInfo;
import java.module.Version;

/**
 * Represents the information about a single module as described by the schema
 * <tt>java.module.RepositoryMetada.xml</tt>.
 * @since 1.7
 */
public class ModuleInfo {
    // These fields are a reflection of the information in a
    // repository-metadata.xml file.  They're not private so they can
    // be accessed by subclasses e.g. {@code ModuleInfoXMLReader.MutableModuleInfo}.
    String name;
    Version version;
    String platform;
    String arch;
    String path;

    /**
     * Having this constructor restricts its use to subclasses in this package,
     * specifically to {@code MetadataXMLReader.MutableModuleInfo}.
     **/
    ModuleInfo() { }

    ModuleInfo(ModuleInfo other) {
        this.name = other.name;
        this.version = other.version;
        this.platform = other.platform;
        this.arch = other.arch;
        this.path = other.path;
    }

    ModuleInfo(String name, Version version, String platform, String arch, String path) {
        if ((platform == null ^ arch == null)) {
            throw new IllegalArgumentException(
                "module platform and name must be either both provided, or neither provided");
        }

        if (name == null) {
            throw new IllegalArgumentException(
                "name must not be null");
        }

        if (version == null) {
            throw new IllegalArgumentException(
                "version must not be null");
        }

        this.name = name;
        this.version = version;
        this.platform = platform;
        this.arch = arch;
        this.path = path;
    }

    ModuleInfo(ModuleArchiveInfo mai) {
        name = mai.getName();
        version = mai.getVersion();
        platform = mai.getPlatform();
        arch = mai.getArchitecture();
    }

    public String getName() {
        return name;
    }

    public Version getVersion() {
        return version;
    }

    public String getPlatform() {
        return platform;
    }

    public String getArch() {
        return arch;
    }

    public String getPath() {
        return path;
    }

    public String getCanonicalizedPath()  {
        if (path != null) {
            return path;
        } else {
            return getName() + "/" + getVersion() + "/"
               + (getPlatform() == null ? "" : getPlatform() + "-" + getArch());
        }
    }

    /** Two ModuleInfo's are equal iff all fields are equal. */
    public boolean equals(Object other) {
        if (other == null || !(other instanceof ModuleInfo)) {
            return false;
        }

        ModuleInfo mi = (ModuleInfo) other;
        if (!name.equals(mi.name) || !version.equals(mi.version)) {
            return false;
        }

        if (platform == null) {
            if (mi.platform != null) {
                return false;
            }
        } else if (!platform.equals(mi.platform)) {
            return false;
        }

        if (arch == null) {
            if (mi.arch != null) {
                return false;
            }
        } else if (!arch.equals(mi.arch)) {
            return false;
        }

        // Note that path is not compared on purpose, because it is
        // a property of where the module lives, not a property of
        // the module itself.

        return true;
    }

    /** A ModuleInfo's hash code is based on all fields except {@code path}. */
    public int hashCode() {
        int rc = name.hashCode();
        rc = 31 * rc + version.hashCode();
        rc = 31 * rc + (platform == null ? 0 : platform.hashCode());
        rc = 31 * rc + (arch == null ? 0 : arch.hashCode());

        // Note that path is not used on purpose, because it is
        // a property of where the module lives, not a property of
        // the module itself.

        return rc;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("ModuleInfo[name=");
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
        if (path != null) {
            builder.append(",path=");
            builder.append(path);
        }
        builder.append("]");
        return builder.toString();
    }
}
