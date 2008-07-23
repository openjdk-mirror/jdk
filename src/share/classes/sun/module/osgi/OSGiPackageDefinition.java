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

import java.lang.annotation.Annotation;
import java.module.ModuleDefinition;
import java.module.PackageDefinition;
import java.module.Version;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * XXX: Remove this class when the module system framework
 * provides a concrete implementation for PackageDefinition
 */
public class OSGiPackageDefinition extends PackageDefinition {
    private String packageName;
    private Version version;
    private ModuleDefinition moduleDef;
    private Map<String, String> attributes;

    OSGiPackageDefinition(String packageName, Version version,
                          ModuleDefinition moduleDef, Map<String, String> attributes) {
        this.packageName = packageName;
        this.version = version;
        this.moduleDef = moduleDef;
        this.attributes = attributes;
    }

    @Override
    public String getName() {
        return packageName;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public ModuleDefinition getModuleDefinition() {
        return moduleDef;
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null)
            throw new NullPointerException();
        // No annotation supported
        return null;
    }

    @Override
    public List<Annotation> getAnnotations() {
        // No annotation supported
        return Collections.emptyList();
    }

    @Override
    public Set<String> getAttributeNames() {
        return Collections.unmodifiableSet(attributes.keySet());
    }

    @Override
    public String getAttribute(String name) {
        if (name == null) {
            throw new NullPointerException("Attribute name must not be null.");
        }
        return attributes.get(name);
    }

}
