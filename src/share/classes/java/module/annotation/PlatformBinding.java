/*
 * Copyright 2006-2008 Sun Microsystems, Inc.  All Rights Reserved.
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
 * Indicates the platform binding of a module definition. This metadata
 * annotation is applied to the development module, i.e. the <I>module</I>
 * construct. For example,
 * <blockquote><pre>
 *    //
 *    // src/win32/x86/com/wombat/xyz/module-info.java
 *    //
 *    &#064;Version("1.0.0")
 *    &#064;PlatformBinding(platform="win32", arch="x86")
 *    module com.wombat.xyz;
 *
 *    //
 *    // src/linux/x86/com/wombat/xyz/module-info.java
 *    //
 *    &#064;Version("1.0.0")
 *    &#064;PlatformBinding(platform="linux", arch="x86")
 *    module com.wombat.xyz;
 *
 *    //
 *    // src/solaris/sparc/com/wombat/xyz/module-info.java
 *    //
 *    &#064;Version("1.0.0")
 *    &#064;PlatformBinding(platform="solaris", arch="sparc")
 *    module com.wombat.xyz;
 * </pre></blockquote>
 * @since 1.7
 */
@Target({ElementType.MODULE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlatformBinding {

    /**
     * The name of the platform. It should be one of the possible values of the
     * system property {@code "os.platform"}.
     */
    String platform();

    /**
     * The name of the architecture. It should be one of the possible values of
     * the system property {@code "os.arch"}.
     */
    String arch();
}
