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
 * This class represents an event that occurs in a module
 * system.
 *
 * @see java.module.Module
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystem
 * @see java.module.ModuleSystemListener
 *
 * @since 1.7
 */
public class ModuleSystemEvent {

    /**
     * Types of module system events.
     */
    public enum Type {
        /**
         * A module instance has been initialized successfully in a module system.
         */
        MODULE_INITIALIZED,

        /**
         * A module instance has been released successfully in a module system.
         */
        MODULE_RELEASED,

        /**
         * The initialization of a module instance has failed in a module system.
         */
        MODULE_INITIALIZATION_EXCEPTION,

        /**
         * A module definition has been disabled successfully in a module system.
         */
        MODULE_DEFINITION_DISABLED
    };

    private Type type;
    private ModuleSystem source;
    private Module module;
    private ModuleDefinition moduleDef;
    private ModuleInitializationException exception;

    /**
     * Constructs a {@code ModuleSystemEvent} instance using the
     * specified module system, event type, and module instance.
     *
     * @param source the module system where the event occurs
     * @param type the event type
     * @param module the module instance that the event applies to
     * @param moduleDef the module definition that the event applies to
     * @param exception module initialization exception
     * @throws NullPointerException if source is {@code null} or type is
     *         {@code null}.
     * @throws IllegalArgumentException
     * <ul>
     *      <li><p>if type is {@link Type#MODULE_INITIALIZED
     *             <tt>MODULE_INITIALIZED</tt>} or
     *             {@link Type#MODULE_RELEASED
     *             <tt>MODULE_RELEASED</tt>} but
     *             module is {@code null}, or</p></li>
     *      <li><p>if type is {@link Type#MODULE_DEFINITION_DISABLED
     *             <tt>MODULE_DEFINITION_DISABLED</tt>} but
     *             moduleDef is {@code null}, or</p></li>
     *      <li><p>if type is {@link Type#MODULE_INITIALIZATION_EXCEPTION
     *             <tt>MODULE_INITIALIZATION_EXCEPTION</tt>} but
     *             exception is {@code null}.</p></li>
     * </ul></p>
     */
    public ModuleSystemEvent(ModuleSystem source, Type type, Module module,
                    ModuleDefinition moduleDef, ModuleInitializationException exception) {
        if (source == null)
            throw new NullPointerException("source must not be null");

        if (type == null)
            throw new NullPointerException("type must not be null");

        if ((type.equals(Type.MODULE_INITIALIZED) || type.equals(Type.MODULE_RELEASED))
             && module == null)
            throw new IllegalArgumentException("module must not be null with event type " + type);

        if (type.equals(Type.MODULE_DEFINITION_DISABLED) && moduleDef == null)
            throw new IllegalArgumentException("moduleDef must not be null with event type " + type);

        if (type.equals(Type.MODULE_INITIALIZATION_EXCEPTION) && exception == null)
            throw new NullPointerException("exception must not be null with event type " + type);

        this.type = type;
        this.source = source;
        this.module = module;
        if (module != null)
            this.moduleDef = module.getModuleDefinition();
        else {
            this.moduleDef = moduleDef;
        }
        this.exception = exception;
    }

    /**
     * Returns the event type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the module system where the event occurs.
     */
    public ModuleSystem getSource() {
        return source;
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
     * Returns a {@code String} object representing this
     * {@code ModuleSystemEvent}.
     *
     * @return a string representation of the {@code ModuleSystemEvent} object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("ModuleSystemEvent[type=");
        builder.append(type.toString());
        builder.append(",module-system=");
        builder.append(getSource().toString());
        builder.append(",module=");
        builder.append(moduleDef.getName());
        builder.append(" v");
        builder.append(moduleDef.getVersion());
        builder.append("]");

        return builder.toString();
    }
}
