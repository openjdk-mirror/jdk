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

package java.module;

import java.util.concurrent.atomic.AtomicLong;
import java.util.List;

/**
 * This class represents the reified module instance in the module system.
 * <p>
 * @see java.lang.ClassLoader
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystemPermission
 * @since 1.7
 */
public abstract class Module
{
    private static ModuleSystem defaultImpl = null;

    /** Counter for generating Id for module instances. */
    private static final AtomicLong idCounter = new AtomicLong();

    /** Id for this module instance. */
    private final long id = idCounter.incrementAndGet();

    /**
     * Creates a new <code>Module</code> instance.
     */
    protected Module()  {
            // empty
    }

    /**
     * Returns a long value that represents the unique identifier assigned to
     * this module instance. The identifier is assigned by the JVM and is JVM
     * implementation dependent.
     *
     * @return a long value that represents the unique identifier assigned to
     *         this module instance.
     */
    public final long getId()  {
            return id;
    }

    /**
        * Returns the <code>ModuleDefinition</code> of the module instance.
        *
        * @return the <code>ModuleDefinition</code> object.
        */
    public abstract ModuleDefinition getModuleDefinition();

    /**
     * Returns the classloader associated with the module instance.
     * <p>
     * If a security manager is present, and the caller's
     * class loader is not null and the caller's class loader
     * is not the same as or an ancestor of the class loader
     * for the module instance whose class loader is
     * requested, then this method calls the security
     * manager's <code>checkPermission</code> method with a
     * <code>RuntimePermission("getClassLoader")</code>
     * permission to ensure it's ok to access the class
     * loader for the module instance.
     *
     * @return the classloader object of the module.
     *  @throws SecurityException if a security manager
     *         exists and its <code>checkPermission</code>
     *         method denies access to the class loader for
     *         the module instance.
     */
    public abstract ClassLoader getClassLoader();

    /**
     * Returns an unmodifiable list of imported module instances.
     *
     * @return an unmodifiable list of imported module instances.
     */
    public abstract List<Module> getImportedModules();

    /**
     * Check if deep validation is supported.
     *
     * @return true if deep validation is supported.
     */
    public abstract boolean supportsDeepValidation();

    /**
     * Perform deep validation on the module instance.
     *
     * @throws UnsupportedOperationException if deep validation is not supported.
     * @throws ModuleInitializationException if deep validation fails.
     */
    public abstract void deepValidate() throws ModuleInitializationException;

    /**
     * Compares the specified object with this {@code Module} for equality.
     * Returns {@code true} if and only if {@code obj} is the same object as
     * this object.
     *
     * @param obj the object to be compared for equality with this module.
     * @return {@code true} if the specified object is equal to this module
     */
    @Override
    public final boolean equals(Object obj)   {
        return (this == obj);
    }

    /**
     * Returns a hash code for this {@code Module}.
     *
     * @return a hash code value for this object.
     */
    @Override
    public final int hashCode()   {
        return super.hashCode();
    }

    /**
     * Returns a <code>String</code> object representing this
     * <code>Module</code>.
     *
     * @return a string representation of the <code>Module</code> object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("module ");
        builder.append(getModuleDefinition().getName());
        builder.append(" v");
        builder.append(getModuleDefinition().getVersion());
        builder.append(" (");
        builder.append(getModuleDefinition().getRepository().getName());
        builder.append(" repository)");

        return builder.toString();
    }
}
