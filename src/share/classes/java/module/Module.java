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

import java.util.List;

/**
 * This class represents a module instance in a module system.
 * A module instance has its own copies of the classes defined by its
 * {@linkplain ModuleDefinition module definition}.
 * A newly created module instance is <i>uninitialized</i>.
 * A <i>fully initialized</i> module instance is one which
 * has completed the <a href="ModuleSystem.html#Initialization">
 * initialization</a> successfully.
 *
 * @see java.lang.ClassLoader
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystemPermission
 * @since 1.7
 */
public abstract class Module {

    /**
     * Creates a new {@code Module} instance.
     */
    protected Module()  {
            // empty
    }

    /**
     * Returns the {@code ModuleDefinition} of this {@code Module}.
     *
     * @return the {@code ModuleDefinition} object.
     */
    public abstract ModuleDefinition getModuleDefinition();

    /**
     * Returns the class loader associated with this {@code Module}. This
     * class loader is also called the <i>module class loader</i>.
     * <p>
     * Module class loader implementations must be able to load all
     * classes and resources in this {@code Module} if this {@code Module}
     * has been <i>fully initialized</i>. The module class loader
     * implementation must return this {@code Module} in its
     * {@link ClassLoader#getModule() getModule}
     * method if this {@code Module} is fully initialized.
     * <p>
     * Module class loader implementations must support parallel
     * classloading as defined in Java SE 7 to enable searches that cross
     * class loader boundaries from multiple threads without deadlocks.
     * <p>
     * Module class loader implementations may search classes and resources
     * from the imported module instances by delegating to their module
     * class loaders, based on how the imports were
     * <a href="ModuleSystem.html#Resolution">resolved</a> during
     * module initialization. The delegation model and the search model for
     * classes and resources are module system specific.
     * <p>
     * Module class loader implementations must continue to make all
     * classes and resources in this {@code Module} visible to its
     * importing module instances after this {@code Module} instance is
     * {@linkplain ModuleSystem#releaseModule released} from its module
     * system.
     * <p>
     * If a security manager is present, and the caller's class loader is not
     * {@code null} and the caller's class loader is not the same as or an
     * ancestor of the class loader for the module instance whose class loader
     * is requested, then this method calls the security manager's
     * {@code checkPermission} method with a
     * {@code RuntimePermission("getClassLoader")} permission to ensure it's
     * ok to access the class loader for this {@code Module}.
     *
     * @return the {@code ClassLoader} object for this {@code Module}.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to the class loader
     *         for this {@code Module}.
     * @throws IllegalStateException if the class loader has not yet been
     *         created during module initialization.
     */
    public abstract ClassLoader getClassLoader();

    /**
     * Returns an unmodifiable list of imported module instances.
     *
     * @return an unmodifiable list of imported module instances.
     * @throws IllegalStateException if the list of imported
     *         module instances has not yet been created during module
     *         initialization.
     */
    public abstract List<Module> getImportedModules();

    /**
     * Returns true if this {@code Module} supports
     * <a href="ModuleSystem.html#DeepValidation">deep validation</a>;
     * otherwise, returns false.
     * <p>
     * This {@code Module} may support deep validation if its
     * {@linkplain ModuleDefinition#getMemberClasses() member classes}
     * and all the member classes from the {@code Module}s
     * imported transitively by this {@code Module} are known.
     *
     * @return true if deep validation is supported; otherwise, returns false.
     * @see #deepValidate()
     */
    public abstract boolean supportsDeepValidation();

    /**
     * Performs <a href="ModuleSystem.html#DeepValidation">deep validation</a> on this
     * {@code Module}.
     *
     * @throws UnsupportedOperationException if this {@code Module} does not
     *         support deep validation.
     * @throws ModuleInitializationException if deep validation fails.
     * @see #supportsDeepValidation()
     */
    public abstract void deepValidate() throws ModuleInitializationException;

    /**
     * Compares the specified object with this {@code Module} for equality.
     * Returns {@code true} if and only if {@code obj} is the same object as
     * this {@code Module}.
     *
     * @param obj the object to be compared for equality with this
     *        {@code Module}.
     * @return {@code true} if the specified object is equal to this
     *         {@code Module}.
     */
    @Override
    public final boolean equals(Object obj)   {
        return (this == obj);
    }

    /**
     * Returns a hash code for this {@code Module}.
     *
     * @return a hash code value for this {@code Module}.
     */
    @Override
    public final int hashCode()   {
        return super.hashCode();
    }

    /**
     * Returns a {@code String} object representing this {@code Module}.
     *
     * @return a string representation of the {@code Module} object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("module instance ");
        builder.append(getModuleDefinition().getName());
        builder.append(" v");
        builder.append(getModuleDefinition().getVersion());
        builder.append(" (");
        builder.append(getModuleDefinition().getRepository().getName());
        builder.append(" repository)");

        return builder.toString();
    }
}
