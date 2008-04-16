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
 * Indicates the version of a module definition. The version format must follow the
 * one described in the {@link java.module.Version} class. This metadata annotation
 * is applied to the development module, i.e. the <I>module</I> construct.
 * For examples,
 * <blockquote><pre>
 *    //
 *    // com/wombat/xyz/module-info.java
 *    //
 *    &#064;Version("1.0.0")
 *    module com.wombat.xyz;
 *
 *    //
 *    // org/foo/bar/module-info.java
 *    //
 *    &#064;Version("2.3.4-b17-beta")
 *    module org.foo.bar;
 *
 *    //
 *    // p/q/r/module-info.java
 *    //
 *    &#064;Version("3.4.5.6-b44-rc")
 *    module p.q.r;
 * </pre></blockquote>
 *
 * @see java.module.Version
 * @since 1.7
 */
@Target({ElementType.MODULE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Version {

    /**
     * The version of the module definition.
     */
    String value();
}
