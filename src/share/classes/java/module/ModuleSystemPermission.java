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

import java.security.BasicPermission;

/**
 * The permission which the SecurityManager will check when code that
 * is running with a SecurityManager calls methods defined in the
 * module system for the Java platform.
 * <P>
 * The following table provides a summary description of what the
 * permission allows, and discusses the risks of granting code the
 * permission.
 * <P>
 *
 * <table border=1 cellpadding=5 summary="Table shows permission target name, wh
at the permission allows, and associated risks">
 * <tr>
 * <th>Permission Target Name</th>
 * <th>What the Permission Allows</th>
 * <th>Risks of Allowing this Permission</th>
 * </tr>
 *
 * <tr>
 *   <td>releaseModule</td>
 *   <td>Releases an existing module instance from the module system via calls to
 *       the ModuleSystem <code>releaseModule</code> method.</td>
 *   <td>This is a dangerous permission to grant.
 *       Malicious applications that allows an attacker to
 *       release an existing module instance, so the
 *       runtime characteristics of the Java virtual
 *       machine is changed and it could cause the system to
 *       misbehave.</td>
 * </tr>
 * <tr>
 *   <td>disableModuleDefinition</td>
 *   <td>Disables a module definition in the module system via calls to
 *       the ModuleSystem <code>disableModuleDefinition</code> method.</td>
 *   <td>This is an extremely dangerous permission to grant.
 *       Malicious applications that allows an attacker to perform
 *       denial-of-service attack by disabling a module definition,
 *       so the existing module instance is released, and also disallows
 *       the module system from creating a new module instance from
 *       that disabled module definition.</td>
 * </tr>
 * <tr>
 *   <td>installModuleArchive</td>
 *   <td>Installs a module archive in a repository via calls
 *       to the Repository <code>install</code> method.</td>
 *   <td>This allows an attacker to install malicious code
 *       into the repository of the module system.</td>
 * </tr>
 * <tr>
 *   <td>uninstallModuleArchive</td>
 *   <td>Uninstalls a module archive in a repository via calls
 *       to the Repository <code>uninstall</code> method.</td>
 *   <td>This allows an attacker to remove critical module
 *       definitions from the repository of the module system.</td>
 * </tr>
 * <tr>
 *   <td>listModuleArchive</td>
 *   <td>Discovers the installed module archives in a repository via calls
 *       to the Repository <code>list</code> method.</td>
 *   <td>This allows an attacker to discover the installed module archives
 *       in the repository of the module system.</td>
 * </tr>
 * <tr>
 *   <td>createRepository</td>
 *   <td>Creation of a repository.</td>
 *   <td>This is an extremely dangerous permission to grant.
 *       Malicious applications that can instantiate their
 *       own repositories could then load their rogue
 *       modules and classes into the module system.</td>
 * </tr>
 * <tr>
 *   <td>shutdownRepository</td>
 *   <td>Shutdown a repository.</td>
 *   <td>This allows an attacker to shutdown a repository
 *       so the repository can no longer serve any module
 *       definition.</td>
 * </tr>
 * <tr>
 *   <td>reloadRepository</td>
 *   <td>Reloads module definitions in a repository.</td>
 *   <td>This allows an attacker to invalidate the lifetime of
 *       the outstanding module instances instantiated from
 *       the module definitions in the repository.</td>
 * </tr>
 * <tr>
 *   <td>accessModuleDefinitionContent</td>
 *   <td>Accesses the actual content of the module definition.</td>
 *   <td>This allows an attacker to have access to the actual content
 *       of the module definition in the repository.</td>
 * </tr>
 * <tr>
 *   <td>setImportOverridePolicy</td>
 *   <td>Changes the default import override policy in the module system.</td>
 *   <td>This allows an attacker to choose specific versions of imported
 *       modules when a module instance is initialized.</td>
 * </tr>
 * <tr>
 *   <td>addModuleSystemListener</td>
 *   <td>Adds a module system listener to the module system.</td>
 *   <td>This allows an attacker to monitor the module system events in the
 *       module system.</td>
 * </tr>
 * <tr>
 *   <td>removeModuleSystemListener</td>
 *   <td>Removes a module system listener to the module system.</td>
 *   <td>This allows an attacker to remove a system-provided module system
 *       listener from the module system.</td>
 * </tr>
 * <tr>
 *   <td>addRepositoryListener</td>
 *   <td>Adds a repository listener to the repository.</td>
 *   <td>This allows an attacker to monitor the repository events in the
 *       repository.</td>
 * </tr>
 * <tr>
 *   <td>removeRepositoryListener</td>
 *   <td>Removes a repository listener to the repository.</td>
 *   <td>This allows an attacker to remove a system-provided repository
 *       listener from the repository.</td>
 * </tr>
 * </table>
 * <p>
 * Programmers do not normally create ModuleSystemPermission
 * objects directly. Instead they are created by the security
 * policy code based on reading the security policy file.
 *
 * @see java.security.BasicPermission
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 * @see java.lang.SecurityManager
 *
 * @since 1.7
 */

public final class ModuleSystemPermission extends BasicPermission {

    private static final long serialVersionUID = 1228383723285909334L;

    /**
     * Constructs a new <code>ModuleSystemPermission</code> instance with the
     * specified name.
     *
     * @param name Permission name.
     * @throws IllegalArgumentException if the name argument is invalid.
     */
    public ModuleSystemPermission(String name) {
        super(name);
    }

    /**
     * Constructs a new <code>ModuleSystemPermission</code> instance.
     *
     * @param name Permission name.
     * @param actions Must be either null or the empty string.
     * @throws IllegalArgumentException if arguments are invalid.
     */
    public ModuleSystemPermission(String name, String actions) {
        super(name);
    }
}
