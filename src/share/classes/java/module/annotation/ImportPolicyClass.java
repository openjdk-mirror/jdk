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
 * Indicates the class of the import policy of a module definition. Th class
 * must extend the <code>ImportPolicy</code> class; it must also be declared
 * as public, and have a public default constructor. This metadata annotation
 * is applied to the development module, i.e. the <I>superpackage</I> construct.
 * <p>
 * For example,
 * <blockquote><pre>
 *    &#064;ImportPolicyClass("com.wombat.xyz.ImportPolicyImpl")
 *    superpackage com.wombat.xyz {
 *       ...
 *    }
 * </pre></blockquote>
 *
 * @see java.module.ImportPolicy
 * @since 1.7
 */
@Target({ElementType.SUPERPACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ImportPolicyClass {

    /**
     * Import policy class.
     */
    String value();
}
