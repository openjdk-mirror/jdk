/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package sun.module.osgi;

import java.module.Module;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;
import java.net.URL;
import java.security.SecureClassLoader;
import org.osgi.framework.Bundle;


/**
 * Class loader implementation for the module system.
 */
final class BundleClassLoader extends SecureClassLoader {

    private final OSGiModule module;
    private final OSGiModuleDefinition moduleDef;
    private final Bundle bundle;

    BundleClassLoader(OSGiModule osgiModule, OSGiModuleDefinition moduleDef) {
        super(null);
        this.module = osgiModule;
        this.moduleDef = moduleDef;
        this.bundle = moduleDef.getBundle();
    }

    @Override
    public Module getModule() {
        return module;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException  {
        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);

        if (c == null) {
            // check myself
            c = bundle.loadClass(name);
        }

        // we do not delegate to the parent

        if (resolve) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        InputStream is = null;
        try {
            URL url = getResource(name);
            if (url != null) {
                is = url.openStream();
            }
        } catch (IOException e) {
        }
        return is;
    }

    @Override
    public URL getResource(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        return findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (name == null) {
            throw new NullPointerException();
        }
        Vector<URL> v = new Vector<URL>();
        for (URL url : Collections.list(findResources(name))) {
            if (url != null) {
                v.add(url);
            }
        }
        return v.elements();
    }

    @Override
    protected URL findResource(String name) {
        return bundle.getResource(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Enumeration<URL> findResources(String name) throws IOException {
        return bundle.getResources(name);
    }

    @Override
    protected String findLibrary(String libname) {
        if (libname == null) {
            throw new NullPointerException();
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("BundleLoaderClass[bundle=");
        builder.append(moduleDef.getName());
        builder.append(", v");
        builder.append(moduleDef.getVersion());
        builder.append("]");

        return builder.toString();
    }
}
