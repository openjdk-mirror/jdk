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
 * Indicates an array of services that a module definition defines.
 * A service is a well-known set of interfaces and (usually abstract) classes.
 * A service-provider is a specific implementation of a service. The classes
 * in a service-provider typically implement the interfaces and subclass the
 * classes defined in the service itself. If a module definition defines a
 * service, it should contain and export the set of interfaces and classes
 * that are part of the service. This metadata annotation is applied to a
 * Java module. For example,
 * <blockquote><pre>
 *    //
 *    // javax/xml/parsers/module-info.java
 *    //
 *    &#064;Version("1.0.0")
 *    &#064;Services({
 *       "javax.xml.parsers.DocumentBuilderFactory",
 *       "javax.xml.parsers.SAXParserFactory"
 *    })
 *    module javax.xml.parsers;
 * </pre></blockquote>
 * @see java.module.annotation.ServiceProvider
 * @see java.module.annotation.ServiceProviders
 * @since 1.7
 */
@Target({ElementType.MODULE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Services {

    /**
    * An array of the names of the services.
    */
    String[] value();
}
