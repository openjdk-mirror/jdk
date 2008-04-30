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
import sun.module.annotation.*;

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

    // The implementation of this class is a temporary approximation
    // designed to function without the javac and JVM support the final
    // implementation will be able to take advantage of.
    // The API is not expected to change, but the final implementation
    // will be very different.

    private final Set<String> exported, members;
    private final static String SUFFIX = ".module_info";

    /*
     * Private storage for the module name and annotations.
     */
    private final String moduleName;
    private transient Map<Class, Annotation> annotations;
    private transient Map<Class, Annotation> declaredAnnotations;

    /**
     * Construct a <code>ModuleInfo</code> instance based on the information
     * of the module-info class.
     *
     * @param clazz the class the represents module-info.
     * @return a new module info.
     */
    ModuleInfo(Class<?> clazz) {
        this.annotations = new HashMap<Class, Annotation>();
        this.declaredAnnotations = new HashMap<Class, Annotation>();

        // Copies the annotations and declared annotations
        for (Annotation a : clazz.getAnnotations()) {
            annotations.put(a.annotationType(), a);
        }
        for (Annotation a : clazz.getDeclaredAnnotations()) {
            declaredAnnotations.put(a.annotationType(), a);
        }

        // Determines module name using @ModuleName annotation if exists;
        // using the class name otherwise.
        //
        // @ModuleName is a workaround for building virtual module
        // definitions in the Java SE platform, and this should be
        // replaced after the actual JSR 294 support arrives.
        sun.module.annotation.ModuleName moduleNameAnnotation = clazz.getAnnotation(sun.module.annotation.ModuleName.class);
        if (moduleNameAnnotation == null) {
            String className = clazz.getName();
            if (className.endsWith(SUFFIX) == false) {
                throw new ClassFormatError("Not a module: " + className);
            }
            moduleName = className.substring(0, className.length() - SUFFIX.length()).replace('$', '.');

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
                }
            }

            LegacyClasses legacyClassesAnno = clazz.getAnnotation(LegacyClasses.class);
            if (legacyClassesAnno != null) {
                // Adds legacy classes as members
                setMembers.addAll(Arrays.asList(legacyClassesAnno.value()));

                ExportLegacyClasses exportLegacyClassesAnno = clazz.getAnnotation(ExportLegacyClasses.class);
                if (exportLegacyClassesAnno != null) {
                    // Adds legacy classes as exported
                    setExported.addAll(Arrays.asList(legacyClassesAnno.value()));
                }
            }

            exported = Collections.unmodifiableSet(getPackages(setExported));
            members = Collections.unmodifiableSet(getPackages(setMembers));
        } else {
            // Virtual modules in the JDK
            moduleName = moduleNameAnnotation.value();

            // XXX @ExportPackages is a workaround for building virtual modules
            // for the Java SE platform. It should be replaced after the
            // actual JSR 294 support arrives in javac.
            //
            sun.module.annotation.ExportPackages exportPackages =
                    getAnnotation(sun.module.annotation.ExportPackages.class);
            if (exportPackages != null) {
                HashSet<String> setExportedPackages = new LinkedHashSet<String>();
                setExportedPackages.addAll(Arrays.asList(exportPackages.value()));
                exported = Collections.unmodifiableSet(setExportedPackages);
                members = exported;
            } else {
                members = Collections.emptySet();
                exported = Collections.emptySet();
            }
        }
    }

    /**
     * Construct a <code>ModuleInfo</code> instance based on the information
     * of the module-info class loaded from the classloader.
     *
     * @param moduleName the module name
     * @param loader the class loader to find the module-info class
     * @return a new module info.
     */
    ModuleInfo(String moduleName, ClassLoader loader) {
        this(getModuleInfo(moduleName, loader));
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
     * @throws UnsupporterOperationException if the packages cannot be
     *         determined.
     */
    public String[] getMemberPackages() {
        return members.toArray(S0);
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
     * @throws UnsupporterOperationException if the packages cannot be
     *         determined.
     */
    public String[] getExportedPackages() {
        return exported.toArray(S0);
    }

    private static Set<String> getPackages(Collection<String> classes) {
        Set<String> packages = new HashSet<String>();
        for (String clazz : classes ) {
            int k = clazz.lastIndexOf('.');
            if (k == -1) {
                packages.add("<unnamed package>");
            } else {
                String pkg = clazz.substring(0, k);
                packages.add(pkg);
            }
        }
        return packages;
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
     * @throws ClassFormatErrror If the data did not contain a valid module
     */
    public static ModuleInfo getModuleInfo(byte[] data) throws ClassFormatError {
        // we use a new ClassLoader for each ModuleInfo object so that we
        // can support multiple ModuleInfo of the same name.
        // The final API will work differently.
        Loader loader = new Loader();
        return loader.doDefineModuleInfo(data);
    }

    private static Class<?> getModuleInfo(String name, ClassLoader loader) {
        try {
            return Class.forName(name + ".module-info", false, loader);
        } catch (ClassNotFoundException ex) {
            // store a proxy for the module info that has no annotations
            class ModuleInfoProxy {}
            return ModuleInfoProxy.class;
        }
    }

    /**
     * Find the module information by name in the callers {@code ClassLoader}
     * instance. The callers {@code ClassLoader} instance is used to find the
     * module information corresponding to the named module.
     *
     * @param name a module name, for example, {@code java.se.core}.
     * @return the module information of the requested name. It may be null if
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
     * @return a new array of module information known to the callers
     *         {@code ClassLoader} instance.  An zero length array is returned
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

    /** Temporary for module definition until javac support arrives */
    public static class exports { private exports() {} };
}
