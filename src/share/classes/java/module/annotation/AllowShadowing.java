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
 * Indicates that class shadowing is allowed in the module instances of
 * a module definition. Shallow validation in the JAM module
 * sytem must permit the member classes in the module instance to be
 * shadowed by the exported classes of its imported modules, if class
 * shadowing is explicitly allowed in the module instance. This
 * metadata annotation is applied to a Java module. For example,
 * <blockquote><pre>
 *    //
 *    // com/wombat/webservice/module-info.java
 *    //
 *    &#064;Version("1.0.0")
 *    &#064;AllowShadowing
 *    &#064;ImportModules({
 *       // The imported module has exported classes that can shadow
 *       // classes in the module, so shadowing check needs to be disabled.
 *       &#064;ImportModule(name="opensource.utils", version="[1.0, 2.0)", optional=true)
 *    })
 *    module com.wombat.webservice;
 * </pre></blockquote>
 * <p>
 * @since 1.7
 */
@Target({ElementType.MODULE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowShadowing {
}
