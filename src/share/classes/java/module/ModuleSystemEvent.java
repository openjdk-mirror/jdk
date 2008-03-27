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

import java.util.EventObject;

/**
 * This class represents a module system event that occurs in the module
 * system.
 *
 * @see java.module.Module
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystem
 * @see java.module.ModuleSystemListener
 *
 * @since 1.7
 */
public class ModuleSystemEvent extends EventObject {

    private static final long serialVersionUID = -2113392048839692418L;

    /**
     * Types of module system events.
     */
    public enum Type {
        /**
         * A module instance has been initialized successfully.
         */
        MODULE_INITIALIZED,

        /**
         * A module instance has been released successfully.
         */
        MODULE_RELEASED,

        /**
         * The initialization of a module instance has failed.
         */
        MODULE_INITIALIZATION_EXCEPTION
    };

    private Type type;
    private transient Module module;
    private transient ModuleDefinition moduleDef;
    private transient ModuleInitializationException exception;

    /**
     * Constructs a ModuleSystemEvent object with the specified module system,
     * event type, and module instance.
     *
     * @param source the module system where the event occurs
     * @param type the event type
     * @param module the module instance that the event applies to
     * @throws NullPointerException if source is null, type is null, or
     *         module is null.
     */
    public ModuleSystemEvent(ModuleSystem source, Type type, Module module) {
        super(source);

        if (source == null)
            throw new NullPointerException("source must not be null.");

        if (type == null)
            throw new NullPointerException("type must not be null.");

        if (module == null)
            throw new NullPointerException("module must not be null.");

        this.type = type;
        this.module = module;
        this.moduleDef = module.getModuleDefinition();
        this.exception = null;
    }

    /**
     * Constructs a ModuleSystemEvent object with the specified module system,
     * module definition, and module initialization exception.
     *
     * @param source the module system where the event occurs
     * @param moduleDef the module definition that the event applies to
     * @param exception module initialization exception
     * @throws NullPointerException if source is null, moduleDef is null, or
     *         exception is null.
     */
    public ModuleSystemEvent(ModuleSystem source, ModuleDefinition moduleDef, ModuleInitializationException exception) {
        super(source);

        if (source == null)
            throw new NullPointerException("source must not be null.");

        if (moduleDef == null)
            throw new NullPointerException("moduleDef must not be null.");

        if (exception == null)
            throw new NullPointerException("exception must not be null.");

        this.type = ModuleSystemEvent.Type.MODULE_INITIALIZATION_EXCEPTION;
        this.module = null;
        this.moduleDef = moduleDef;
        this.exception = exception;
    }

    /**
     * Returns the event type.
     */
    public Type getType() {
        return type;
    }


    /**
     * Returns the module.
     */
    public Module getModule() {
        return module;
    }

    /**
     * Returns the module definition.
     */
    public ModuleDefinition getModuleDefinition() {
        return moduleDef;
    }

    /**
     * Returns the module initialization exception.
     */
    public ModuleInitializationException getException() {
        return exception;
    }

    /**
     * Returns a <code>String</code> object representing this
     * <code>ModuleSystemEvent</code>.
     *
     * @return a string representation of the <code>ModuleSystemEvent</code> object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("ModuleSystemEvent[type=");
        builder.append(type.toString());

        ModuleDefinition md = moduleDef;
        if (type.equals(ModuleSystemEvent.Type.MODULE_INITIALIZATION_EXCEPTION) == false) {
            // MODULE_INITIALIZED or MODULE_RELEASED
            md = module.getModuleDefinition();
        }

        builder.append(",module-name=");
        builder.append(md.getName());
        builder.append(",module-version=");
        builder.append(md.getVersion());
        builder.append("]");

        return builder.toString();
    }
}
