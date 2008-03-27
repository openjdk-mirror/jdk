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

package java.module.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates the path relative to the root of the module archive for searching
 * native libraries for a particular platform and architecture. This metadata
 * annotation is used as nested annotation inside other enclosing annotations.
 * <p>
 * For example,
 * <blockquote><pre>
 *    &#064;NativeLibraryPaths({
 *       &#064;NativeLibraryPath(platform="windows", arch="x86", path="native/windows-x86"),
 *       &#064;NativeLibraryPath(platform="linux", arch="x86", path="native/linux-x86"),
 *       &#064;NativeLibraryPath(platform="solaris", arch="sparc", path="native/solaris-sparc"),
 *    })
 *    superpackage com.sun.java3d {
 *       ...
 *    }
 * </pre></blockquote>
 *
 * @see java.module.annotation.NativeLibraryPaths
 * @since 1.7
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface NativeLibraryPath {

    /**
    * Name of the platform. It should be one of the possible values
     * of the system property "os.platform".
     */
    String platform();

    /**
     * Name of the architecture. It should be one of the possible
     * values of the system property "os.arch".
     */
    String arch();

    /**
     * The path relative to the root of the module archive for searching native
     * library.
     */
    String path();
}
