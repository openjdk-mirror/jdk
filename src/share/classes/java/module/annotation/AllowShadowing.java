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
 * Indicates that the shadowing check is disabled on the module instance of
 * the corresponding module definition when shallow validation is performed
 * initialization. This metadata annotation is applied to the development
 * module, i.e. the <I>superpackage</I> construct.
 * <p>
 * For example,
 * <blockquote><pre>
 *    &#064;Version("1.7.0")
 *    superpackage opensource.utils {
 *       // Exports classes
 *       export org.foo.utils.*;
 *
 *       // Membership - all classes in the listed package
 *       member package org.foo.utils;
 *       ....
 *    }
 *
 *    // Uses @AllowShadowing to mark a module to explicitly allow
 *    // shadowing of classes from its imported modules. Otherwise,
 *    // shallow validation would fail.
 *    &#064;Version("1.0")
 *    &#064;AllowShadowing
 *    &#064;ImportModules({
 *       // The imported module is optional.
 *       &#064;ImportModule(name="opensource.utils", version="[1.0, 2.0)", optional=true)
 *    })
 *    superpackage com.wombat.webservice {
 *           // Membership - all classes in the listed package
 *       member package org.foo.utils.;
 *           ....
 *    }
 * </pre></blockquote>
 * <p>
 *
 * @since 1.7
 */
@Target(ElementType.SUPERPACKAGE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowShadowing {
}
