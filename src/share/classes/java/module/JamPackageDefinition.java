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

package java.module;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
* This class represents the reified package definition in the
* module system.
* <p>
* @see java.lang.ClassLoader
* @see java.module.PackageDefinition
* @see java.module.ModuleDefinition
* @see java.module.Version
*
* @since 1.7
*/
class JamPackageDefinition extends PackageDefinition {

    private String packageName;
    private Version version;
    private ModuleDefinition moduleDef;

    JamPackageDefinition(String packageName, Version version, ModuleDefinition moduleDef) {
        this.packageName = packageName;
        this.version = version;
        this.moduleDef = moduleDef;
    }

    /**
     * Returns the name of the package definition.
     *
     * @return the name of the package definition.
     */
    public String getName() {
        return packageName;
    }

    /**
     * Returns the version of the package definition.
     *
     * @return the {@code Version} object.
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Returns the module definition that is associated with this package
     * definition.
     *
     * @return the {@code ModuleDefinition} object.
     */
    public ModuleDefinition getModuleDefinition() {
        return moduleDef;
    }

    /**
     * Returns this element's annotation for the specified type or
     * the value of the specified attribute as an annotation.
     *
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return this element's annotation for the specified annotation type if
     *     present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     */
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null)
            throw new NullPointerException();
        // XXX: not yet implemented
        return null;
    }

    /**
     * Returns an unmodifiable list of all annotations present on this element.
     * If no annotations are present, an empty list is returned.
     *
     * @return an unmodifiable list of all annotations present on this element
     */
    public List<Annotation> getAnnotations() {
        // XXX: not yet implemented
        return new ArrayList<Annotation>();
    }
}
