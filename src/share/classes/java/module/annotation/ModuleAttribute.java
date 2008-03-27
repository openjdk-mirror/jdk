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
 * Indicates an attribute of a module definition. This metadata annotation
 * is used as nested annotation inside other enclosing annotations.
 * <p>
 * Module attributes are generally defined by higher layers. To
 * minimize the naming conflicts between module attributes, it is
 * recommended that each module attribute should be defined with a
 * fully qualified name.
 * <p>
 * For examples,
 * <blockquote><pre>
 *    &#064;ModuleAttributes({
 *       &#064;ModuleAttribute(name="org.opensource.license", value="GPL"),
 *       &#064;ModuleAttribute(name="java.magic.number", value="CAFEBABE")
 *    })
 *    superpackage com.wombat.xyz {
 *       ...
 *    }
 * </pre></blockquote>
 *
 * @see java.module.annotation.ModuleAttributes
 * @since 1.7
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleAttribute {

    /**
     * Name of the module attribute.
     */
    String name();

    /**
     * Value of the module attribute.
     */
    String value();
}
