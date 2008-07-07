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

package sun.module.core;

import java.io.*;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.*;

/**
 * This class acts a ModuleLoader proxy, ie. it breaks the catch-22
 * of setting the system class loader, and loading the classes
 * for the module classes.
 * This class does the following:
 * a. setup the parent as the BootClassLoader namely null.
 * b. this class acts as a proxy for the ModuleLoader, initially
 *    it delegates the class loading to the parent BootStrap loader,
 *    once the ModuleLoader is initialized this class will begin
 *    delegation to the primordial module class loader.
 */

public class ProxyModuleLoader extends SecureClassLoader {
    private static volatile ModuleLoader primaryModuleLoader = null;

    final ClassLoader parent;

    public ProxyModuleLoader(ClassLoader parent) {
        // Disregard the parent assigned by the classloader,
        // we want to be adopted by the bootstrap class loader,
        super(null);
        this.parent = null;
    }

    public static void setPrimaryModuleLoader(ClassLoader m) {
        if (primaryModuleLoader == null) {
            // a good thing, will force a ClassCastException
            primaryModuleLoader = (ModuleLoader) m;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ProxyModuleClassLoader[");
        builder.append(primaryModuleLoader);
        builder.append("]");
        return builder.toString();
    }


    @Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException
    {
        return (primaryModuleLoader == null)
            ? super.loadClass(name, resolve)
            : primaryModuleLoader.loadClass(name, resolve);
    }

    @Override
    public URL getResource(String name) {
        return (primaryModuleLoader == null)
            ? super.getResource(name)
            : primaryModuleLoader.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return (primaryModuleLoader == null)
            ? super.getResources(name)
            : primaryModuleLoader.getResources(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return (primaryModuleLoader == null)
            ? super.getResourceAsStream(name)
            : primaryModuleLoader.getResourceAsStream(name);
    }

    @Override
    public java.module.Module getModule() {
        if (primaryModuleLoader == null) {
            return null;
        } else {
            return primaryModuleLoader.getModule();
        }
    }
}
