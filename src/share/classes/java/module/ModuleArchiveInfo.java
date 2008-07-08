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
 * This interface represents the information of an installed module archive
 * in a repository.
 *
 * @see java.module.Repository
 * @see java.module.Version
 *
 * @since 1.7
 */
public interface ModuleArchiveInfo {

    /**
     * Returns the repository where the module archive is stored.
     *
     * @return the repository.
     */
    public Repository getRepository();

    /**
     * Returns the name of the module definition in the module archive.
     *
     * @return the name of the module definition in the module archive.
     */
    public String getName();

    /**
     * Returns the version of the module definition in the module archive.
     *
     * @return the version of the module definition in the module archive.
     */
    public Version getVersion();

    /**
     * Returns the name of the platform which the module archive targets.
     * The value should be one of the possible values
     * of the system property {@code "os.platform"}.
     *
     * @return the name of the platform. If the module archive has no
     *          platform binding, returns null.
     */
    public String getPlatform();

    /**
     * Returns the name of the architecture of the module archive targets.
     * The value should be one of the possible values of the system property {@code "os.arch"}.
     *
     * @return the name of the architecture. If the module archive has no
     *          platform binding, returns null.
     */
    public String getArch();

    /**
     * Returns the filename of the module archive.
     *
     * @return the filename of the module archive. If the module archive does not
     *          have a filename, return null.
     */
    public String getFileName();

    /**
     * Returns the last modified time of the module archive in the repository. The
     * result is the number of milliseconds since January 1, 1970 GMT.
     *
     * @return the time the module archive was last modified, or 0 if not known.
     */
    public long getLastModified();
}
