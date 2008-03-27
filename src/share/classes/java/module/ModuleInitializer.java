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
 * This interface represents the initializer of a module definition. The
 * initializer is invoked when the module system initializes a module
 * instance of the module definition, and when that module instance is
 * released from the module system.
 * <p>
 * @see java.module.Module
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleInitializationException
 *
 * @since 1.7
 */
public interface ModuleInitializer {

    /**
     * This method is invoked during module instance's initialization. It is
     * invoked after the module instance has been shallow validated
     * successfully, but before the module instance becomes ready.
     * <p>
     * If this method throws any exception during execution, it will cause the
     * module instance's initialization to fail.
     * <p>
     * Note that when this method is invoked, it is possible that some of the
     * imported modules are still in the middle of the initialization, and
     * there are two potential issues:
     * <p>
     * 1. The exported classes from these imported modules might not yet be
     *    accessible from this method.
     * 2. The initializer of these imported modules might not yet been invoked.
     * <p>
     * Implementation of this method should avoid accessing classes from the
     * imported modules, and should make no assumption that the imported
     * modules have been fully intitalized. Otherwise, the result is
     * undeterministic.
     *
     * @param module the module instance that this module initializer belongs.
     * @throws ModuleInitializationException if this module initializer
     *         fails to initialize.
     */
    public void initialize(Module module) throws ModuleInitializationException;

    /**
     * This method is invoked when a module instance is released from the
     * module system in the following situations:
     * <p>
     * 1. The {@code initialize} method has been invoked successfully, but
     *    this module instance still gets into error state because one or
     *    more of its imported modules get into error state, or
     * 2. After the <code>releaseModuleDefinition</code> method of the
     *    {@code ModuleSystem} is invoked.
     * <p>
     * Note that after this method is invoked, the module classloader of this
     * module instance might still be accessible from other modules. If the
     * implementation of this method attempts to reset some states or
     * shutdown some functionalities in this module instance, this could be
     * problematic for the importing modules if they continue to access this
     * module instance after this module instance has been released.
     *
     * @param module the module instance that this module initializer belongs.
     */
    public void release(Module module);
}
