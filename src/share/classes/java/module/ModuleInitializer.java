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

package java.module;

/**
 * This interface represents a module initializer of a module instance
 * specifically in the JAM module system.
 * <p>
 * The
 * {@link #initialize(ModuleDefinition) initialize} method is invoked when
 * the module system initializes the module instance; the module system must
 * cause the module initialization to fail if this method
 * throws any exception. The
 * {@link #release() release} method is invoked when the module system
 * releases the module instance; the module system must
 * ignore any exception being thrown in this method.
 * <p>
 * @see java.module.Module
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleInitializationException
 * @see java.module.annotation.ModuleInitializerClass
 *
 * @since 1.7
 */
public interface ModuleInitializer {

    /**
     * This method is invoked when a module instance is being initialized in the
     * module system.
     * <p>
     * The imported module instances of the
     * initializing module instance may not be <i>fully initialized</i> when this
     * method is invoked; the exported classes from the imported
     * module instances of the initializing module instance may not be
     * accessible from this method.
     *
     * @param moduleDef the {@code ModuleDefinition} associated with the
     *                  initializing module instance.
     * @throws ModuleInitializationException if this {@code ModuleInitializer}
     *         fails to initialize.
     */
    public void initialize(ModuleDefinition moduleDef) throws ModuleInitializationException;

    /**
     * This method is invoked when a module instance is being released from the
     * module system. There is no guarantee that this method is ever
     * invoked when the virtual machine exits.
     * <p>
     * The module instance may not be <i>fully initialized</i> when
     * this method is invoked.
     * <p>
     * The imported module instances of the module instance may
     * not be fully initialized when this method is invoked;
     * the exported classes from the imported module instances of
     * the module instance may not be accessible from this method.
     */
    public void release();
}
