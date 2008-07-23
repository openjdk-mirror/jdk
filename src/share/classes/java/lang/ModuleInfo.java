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

package java.lang;

import java.util.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.module.annotation.*;
import sun.module.MetadataParser;
import sun.module.ModuleParsingException;

/**
 * A {@code ModuleInfo} object contain information about a Java module. The
 * module information is retrieved and made available by the
 * {@link ClassLoader} instance that loaded the class(es).
 * <p>
 * {@code ModuleInfo} has no public constructor. Instead {@code ModuleInfo}
 * objects are constructed automatically by the Java Virtual Machine as modules
 * are loaded and by calls to the {@code defineModuleInfo} method in the class
 * loader.
 * <p>
 * Within each {@code ClassLoader} instance all classes from the same
 * java module have the same {@code ModuleInfo} object.  The static methods
 * allow a module to be found by name, by module data, or the set of all
 * modules known to the current class loader to be found.
 *
 * @see java.lang.Class#getModuleInfo
 * @see java.lang.ClassLoader#defineModuleInfo
 * @see java.lang.ClassLoader#findModuleInfo
 * @since  1.7
 */
public final class ModuleInfo implements AnnotatedElement {

    private final String moduleName;
    private final Set<String> exportedClasses;
    private final Set<String> exportedPackages;
    private final Set<String> memberPackages;
    private final Map<Class, Annotation> annotations;
    private final Map<Class, Annotation> declaredAnnotations;

    /**
     * Construct a <code>ModuleInfo</code> instance based on the information
     * of the module-info class.
     *
     * @param clazz the class the represents module-info.
     * @param b
     *        The bytes that make up the module data.  The bytes in positions
     *        <tt>off</tt> through <tt>off+len-1</tt> should have the format
     *        of a valid module file as defined by the <a
     *        href="http://java.sun.com/docs/books/vmspec/">Java Virtual
     *        Machine Specification</a>.
     * @param off
     *        The start offset in <tt>b</tt> of the module data
     * @param len
     * @throws ClassFormatError if the data doesn't contain a valid module info.
     */
    ModuleInfo(Class<?> clazz, byte[] b, int off, int len) throws ClassFormatError {
        this.annotations = new HashMap<Class, Annotation>();
        this.declaredAnnotations = new HashMap<Class, Annotation>();

        // Copies the annotations and declared annotations
        for (Annotation a : clazz.getAnnotations()) {
            annotations.put(a.annotationType(), a);
        }
        for (Annotation a : clazz.getDeclaredAnnotations()) {
            declaredAnnotations.put(a.annotationType(), a);
        }

        try {
            MetadataParser metadata = new MetadataParser(b, off, len);
            moduleName = metadata.getModuleName();
            exportedClasses = new HashSet<String>(metadata.getModuleExportClassList(false));
            memberPackages = new HashSet<String>(metadata.getModuleMemberPackageList());
            exportedPackages = new HashSet<String>(metadata.getModuleExportPackageList());
        } catch (ModuleParsingException mpe) {
            throw new ClassFormatError("Invalid module data: " + mpe.getMessage());
        }
    }

    private final static String[] S0 = new String[0];

    /**
     * Returns the fully qualified name of this module.
     *
     * @return the fully qualified name of this module.
     */
    public String getName() {
        return moduleName;
    }

    /**
     * Returns an array of String objects reflecting the binary names of all
     * packages that are a member of this module.
     *
     * <p>The elements in the array returned are not sorted and are not in
     * any particular order.  This method returns an array of length 0 if
     * the module has no members.
     *
     * @return an array of the names of all member packages
     * @throws UnsupportedOperationException if the packages cannot be
     *         determined.
     */
    public String[] getMemberPackages() {
        return memberPackages.toArray(S0);
    }

    /**
     * Returns an array of String objects reflecting the binary names of all
     * packages that have exported classes and interfaces that are a member
     * of this module. The names returned by this method are a subset of the
     * names returned by {@link #getMemberPackages}.
     *
     * <p>The elements in the array returned are not sorted and are not in
     * any particular order.  This method returns an array of length 0 if
     * the module has no members that are exported types.
     *
     * @return an array of the names of all packages that have exported types.
     * @throws UnsupportedOperationException if the packages cannot be
     *         determined.
     */
    public String[] getExportedPackages() {
        return exportedPackages.toArray(S0);
    }

    /**
     * Returns an array of String objects reflecting the binary names of all
     * exported classes and interfaces that are a member of this module.
     *
     * <p>The elements in the array returned are not sorted and are not in
     * any particular order.  This method returns an array of length 0 if
     * the module has no exported types.
     *
     * @return an array of the names of all exported types.
     * @throws UnsupportedOperationException if the exported types cannot be
     *         determined.
     */
    public String[] getExportedClasses() {
        return exportedClasses.toArray(S0);
    }

    private static final class Loader extends ClassLoader {
        Loader() {
            super(null);
        }
        ModuleInfo doDefineModuleInfo(byte[] data) throws ClassFormatError {
            return defineModuleInfo(null, data, 0, data.length);
        }
    }

    /**
     * Constructs a <code>ModuleInfo</code> instance that represents the module
     * information in the module data.
     *
     * @param data the module data
     * @return a new module info that represents the module data.
     * @throws ClassFormatError If the data did not contain a valid module
     */
    public static ModuleInfo getModuleInfo(byte[] data) throws ClassFormatError {
        // we use a new ClassLoader for each ModuleInfo object so that we
        // can support multiple ModuleInfo of the same name.
        // The final API will work differently.
        Loader loader = new Loader();
        return loader.doDefineModuleInfo(data);
    }

    /**
     * Find the module information by name in the caller's {@code ClassLoader}
     * instance. The caller's {@code ClassLoader} instance is used to find the
     * module information corresponding to the named module.
     *
     * @param name a module name, for example, {@code java.se.core}.
     * @return the module information of the requested name. It returns null if
     *          no module information is available.
     */
    public static ModuleInfo getModuleInfo(String name) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Get all the module information currently known for the caller's
     * {@code ClassLoader} instance.  Those modules correspond to classes loaded
     * via or accessible by name to that {@code ClassLoader} instance.
     *
     * @return a new array of module information known to the caller's
     *         {@code ClassLoader} instance.  A zero length array is returned
     *         if none are known.
     */
    public static ModuleInfo[] getModuleInfos() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        if (annotationClass == null)
            throw new NullPointerException();

        return (A) annotations.get(annotationClass);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        if (annotationClass == null)
            throw new NullPointerException();

        return getAnnotation(annotationClass) != null;
    }

    private static Annotation[] EMPTY_ANNOTATIONS_ARRAY = new Annotation[0];

    // inherits doc
    public Annotation[] getAnnotations() {
        return annotations.values().toArray(EMPTY_ANNOTATIONS_ARRAY);
    }

    // inherits doc
    public Annotation[] getDeclaredAnnotations()  {
        return declaredAnnotations.values().toArray(EMPTY_ANNOTATIONS_ARRAY);
    }

    /**
     * Return the hash code computed from the module name.
     * @return the hash code computed from the module name.
     */
    public int hashCode(){
        return moduleName.hashCode();
    }

    /**
     * Returns a {@code String} object representing this {@code ModuleInfo}.
     * The string representation is the string "module"  followed by a space,
     * and then by the fully qualified name of the module in the format
     * returned by {@link #getName() getName()}.
     *
     * @return a string representation of the {@code ModuleInfo} object.
     */
    public String toString() {
        return "module " + moduleName;
    }
}
