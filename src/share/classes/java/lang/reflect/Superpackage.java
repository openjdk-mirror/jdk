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

package java.lang.reflect;

import java.util.*;

import java.lang.annotation.*;

/**
 * Instances of the class {@code Superpackage} represent superpackages
 * in a running Java application.
 *
 * <p> {@code Superpackage} has no public constructor. Instead {@code Superpackage}
 * objects are constructed automatically by the Java Virtual Machine as superpackages
 * are loaded and by calls to the {@code defineSuperpackage} method in the class
 * loader.
 *
 * @see java.lang.Class#getSuperpackage
 * @see java.lang.ClassLoader#findSuperpackage(String)
 * @since  1.7
 * @author Andreas Sterbenz
 */
public final class Superpackage implements AnnotatedElement {

    // The implementation of this class is a temporary approximation
    // designed to function without the javac and JVM support the final
    // implementation will be able to take advantage of.
    // The API is not expected to change, but the final implementation
    // will be very different.

    private final Class<?> clazz;

    private final String name;

    private final Set<String> exported, members;

    private final static String SUFFIX = ".super_package";

    // Called from ClassLoader via privileged reflection
    Superpackage(Class<?> clazz) {
        this.clazz = clazz;

        // Determines module name using @ModuleName annotation if exists;
        // using the class name otherwise.
        //
        // @ModuleName is a workaround for building virtual module
        // definitions in the Java SE platform, and this should be
        // replaced after the actual JSR 294 support arrives.
        sun.module.annotation.ModuleName moduleName = clazz.getAnnotation(sun.module.annotation.ModuleName.class);
        if (moduleName == null) {
            String className = clazz.getName();
            if (className.endsWith(SUFFIX) == false) {
                throw new ClassFormatError("Not a superpackage: " + className);
            }
            name = className.substring(0, className.length() - SUFFIX.length()).replace('$', '.');
            // use LinkedHashSet to preserve order
            Set<String> setExported = new LinkedHashSet<String>();
            Set<String> setMembers = new LinkedHashSet<String>();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                Class type = field.getType();
                String name = field.getName().replace('$', '.');
                if (type == exports.class) {
                    setExported.add(name);
                    setMembers.add(name);
                } else if (type == members.class) {
                    setMembers.add(name);
                }
            }
            exported = Collections.unmodifiableSet(setExported);
            members = Collections.unmodifiableSet(setMembers);
        } else {
            name = moduleName.value();
            exported = Collections.emptySet();
            members = Collections.emptySet();
        }
    }

    private final static String[] S0 = new String[0];

    private final static Superpackage[] SP0 = new Superpackage[0];

    /**
     * Returns the fully qualified name of this superpackage.
     *
     * @return the fully qualified name of this superpackage.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns an array of String objects reflecting the binary names of all
     * classes and interfaces that are a direct member of this superpackage.
     * Types that are a member of a nested superpackages are not returned.
     *
     * <p>The elements in the array returned are
     * not sorted and are not in any particular order.  This method returns an
     * array of length 0 if the superpackage has no direct members that are
     * types.
     *
     * @return an array of the names of all direct member types
     */
    public String[] getMemberTypes() {
        return members.toArray(S0);
    }

    /**
     * Returns an array of Superpackage objects reflecting the
     * superpackages that are a direct member of this superpackage.
     * Superpackages that are a member of these nested superpackages
     * are not returned.
     *
     * <p>The elements in the array returned are
     * not sorted and are not in any particular order.  This method returns an
     * array of length 0 if the superpackage has no members that are
     * superpackages
     *
     * @return an array of the Superpackage objects of all direct member
     *   superpackages
     */
    public Superpackage[] getMemberSuperpackages() {
        return SP0;
    }

    /**
     * Returns an array of String objects reflecting the binary names of all
     * exported classes and interfaces that are a direct member of this superpackage.
     * Types that are a member of a nested superpackages are not returned.
     * The names returned by this method are a subset of the names returned by
     * {@link #getMemberTypes}.
     *
     * <p>The elements in the array returned are
     * not sorted and are not in any particular order.  This method returns an
     * array of length 0 if the superpackage has no direct members that are
     * exported types.
     *
     * @return an array of the names of all direct exported member types
     */
    public String[] getExportedTypes() {
        return exported.toArray(S0);
    }

    /**
     * Returns an array of Superpackage objects reflecting the
     * exported superpackages that are a direct member of this superpackage.
     * Superpackages that are a member of these nested superpackages
     * are not returned.
     * The superpackages returned by this method are a subset of the
     * superpackages returned by
     * {@link #getMemberSuperpackages}.
     *
     * <p>The elements in the array returned are
     * not sorted and are not in any particular order.  This method returns an
     * array of length 0 if the superpackage has no direct members that are
     * exported superpackages.
     *
     * @return an array of the Superpackage objects of all exported member
     *   superpackages
     */
    public Superpackage[] getExportedSuperpackages() {
        return SP0;
    }

    /**
     * Returns the immediately enclosing superpackage of the underlying
     * superpackage.  If the underlying superpackage is a top level
     * superpackage, this method returns <tt>null</tt>.
     *
     * @return the immediately enclosing superpackage of the underlying superpackage
     */
    public Superpackage getEnclosingSuperpackage() {
        return null;
    }

    /**
     * Returns the class loader for the superpackage.  Some implementations may use
     * null to represent the bootstrap class loader. This method will return
     * null in such implementations if this class was loaded by the bootstrap
     * class loader.
     *
     * <p> If a security manager is present, and the caller's class loader is
     * not null and the caller's class loader is not the same as or an ancestor of
     * the class loader for the superpackage whose class loader is requested, then
     * this method calls the security manager's <code>checkPermission</code>
     * method with a <code>RuntimePermission("getClassLoader")</code>
     * permission to ensure it's ok to access the class loader for the superpackage.
     *
     * @return  the class loader that loaded the superpackage
     *          represented by this object.
     * @throws SecurityException
     *    if a security manager exists and its
     *    <code>checkPermission</code> method denies
     *    access to the class loader for the superpackage.
     * @see java.lang.ClassLoader
     * @see SecurityManager#checkPermission
     * @see java.lang.RuntimePermission
     */
    public ClassLoader getClassLoader() {
        return clazz.getClassLoader();
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return clazz.getAnnotation(annotationClass);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return clazz.isAnnotationPresent(annotationClass);
    }


    // inherits doc
    public Annotation[] getAnnotations() {
        return clazz.getAnnotations();
    }

    // inherits doc
    public Annotation[] getDeclaredAnnotations()  {
        return clazz.getDeclaredAnnotations();
    }

    /**
     * Converts the object to a string. The string representation is the
     * string "superpackage"  followed by a space, and then by the
     * fully qualified name of the superpackage in the format returned by
     * <code>getName</code>.
     *
     * @return a string representation of this superpackage object.
     */
    public String toString() {
        return "superpackage " + getName();
    }

    /** temporary for superpackage definition until javac support arrives */
    public static class exports { private exports() {} };
    /** temporary for superpackage definition until javac support arrives */
    public static class members { private members() {} };

}
