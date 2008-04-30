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
 * Indicates the exported resources in a module definition. This metadata
 * annotation is applied to a Java module.
 * <p>
 * The string that specifies the exported resource may contain wildcard:<p>
 * 1. {@code '?'} matches a single character.<p>
 * 2. {@code '*'} matches zero or more characters.<p>
 * 3. {@code '**'} matches zero or more directories.<p>
 * There is also a shorthand. If the string ends with {@code '/'}, it is
 * treated as if {@code '**'} has been appended.
 * <p>
 * For examples,
 * <blockquote><pre>
 *    //
 *    // com/wombat/xyz/module-info.java
 *    //
 *    &#064;Version("1.0.0")
 *    &#064;ExportResources({
 *       "icons/graphics1.jpg",     // exports individual resource
 *       "icons/graphics2.jpg"
 *    })
 *    module com.wombat.xyz;
 *
 *    //
 *    // org/foo/abc/module-info.java
 *    //
 *    &#064;Version("2.0.0")
 *    &#064;ExportResources({
 *       "icons/*",                 // exports resources using wildcards
 *       "META-INF/**"
 *       "resources/"
 *    })
 *    module org.foo.abc;
 * </pre></blockquote>
 *
 * @since 1.7
 */
@Target({ElementType.MODULE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExportResources {

    /**
     * The exported resources in a module definition. Each exported resource
     * must be a relative path to the root of the module archive, using
     * {@code '/'} as path separator and no leading {@code '/'}.
     */
    String[] value();
}
