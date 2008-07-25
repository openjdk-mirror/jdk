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
 * Indicates a
 * <a href="../../../java/util/ServiceLoader.html"><i>service provider</i></a>
 * which is defined in a module definition. The module definition must export the
 * <i>service provider</i>'s types, and must import a module definition
 * which defines the <i>service</i> implemented by the <i>service provider</i>.
 * This metadata annotation is used as nested annotation inside other
 * enclosing annotations. For example,
 * <blockquote><pre>
 *    //
 *    // com/xyz/xmlparser/module-info.java
 *    //
 *    &#064;Version("1.0.0")
 *    &#064;ServiceProviders({
 *       &#064;ServiceProvider(service="javax.xml.parsers.DocumentBuilderFactory",
 *                         providerClass="com.xyz.xmlparser.DocumentBuilderFactoryImpl"),
 *       &#064;ServiceProvider(service="javax.xml.parsers.SAXParserFactory",
 *                         providerClass="com.xyz.xmlparser.SAXParserFactoryImpl")
 *    })
 *    &#064;ImportModules({
 *       &#064;ImportModule(name="javax.xml.parsers",   // service module
 *                      version="[1.0, 2.0)")
 *    })
 *    module com.xyz.xmlparser;
 * </pre></blockquote>
 * @see java.module.annotation.ServiceProviders
 * @see java.module.annotation.Services
 * @see java.util.ServiceLoader
 * @since 1.7
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceProvider {

    /**
     * The name of the <i>service</i> implemented by the
     * <i>service provider</i>.
     */
    String service();

    /**
     * The name of the <i>service provider</i> class.
     */
    String providerClass();
}
