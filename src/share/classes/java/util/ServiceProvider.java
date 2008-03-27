/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
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

package java.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The ServiceProvider annotation marks a service provider which implements a
 * service. This metadata annotation is applied to the Java class.
 * <p>
 * A service is a well-known set of interfaces and (usually abstract)
 * classes. A service-provider is a specific implementation of a
 * service. The classes in a provider typically implement the
 * interfaces and subclass the classes defined in the service itself.
 * <p>
 * For example,
 * <blockquote><pre>
 *    &#064;ServiceProvider
 *    public class com.xyz.xmlparser.DocumentBuilderFactoryImpl
 *                                      extends javax.xml.parsers.DocumentBuilderFactory {
 *         ...
 *    }
 * </pre></blockquote>
 *
 * @since 1.7.0
 * @author Stanley M. Ho
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceProvider {
}
