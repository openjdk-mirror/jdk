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
 * The permission which the {@code SecurityManager} will check when code that
 * is running with a {@code SecurityManager} calls methods defined in the
 * Java Module System.
 * <P>
 * The following table provides a summary description of what the permission
 * allows, and discusses the risks of granting code the permission.
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
 *   <td>createModuleSystem</td>
 *   <td>Creation of a module system.</td>
 *   <td>This is an extremely dangerous permission to grant. Malicious
 *       applications that can instantiate their own module systems could then
 *       load their rogue modules and classes into the system.</td>
 * </tr>
 * <tr>
 *   <td>createModuleDefinition</td>
 *   <td>Creation of a module definition.</td>
 *   <td>This is an extremely dangerous permission to grant. Malicious
 *       applications that can instantiate their own module definitions could
 *       then load their rogue modules and classes into the system through an
 *       existing module system.</td>
 * </tr>
 * <tr>
 *   <td>createRepository</td>
 *   <td>Creation of a repository.</td>
 *   <td>This is an extremely dangerous permission to grant. Malicious
 *       applications that can instantiate their own repositories could then
 *       load their rogue modules and classes into the system.</td>
 * </tr>
 * <tr>
 *   <td>disableModuleDefinition</td>
 *   <td>Disables a module definition in a module system via calls to
 *       the {@code ModuleSystem}'s
 *       {@link ModuleSystem#disableModuleDefinition(ModuleDefinition)
 *       <tt>disableModuleDefinition</tt>} method.</td>
 *   <td>This is an extremely dangerous permission to grant. Malicious
 *       applications could allow an attacker to perform denial-of-service
 *       attacks by disabling a module definition, and the module system will
 *       not create any new module instances from the module definition.</td>
 * </tr>
 * <tr>
 *   <td>releaseModule</td>
 *   <td>Releases an existing module instance from a module system via calls to
 *       the {@code ModuleSystem}'s
 *       {@link ModuleSystem#releaseModule(ModuleDefinition)
 *       <tt>releaseModule</tt>} method.</td>
 *   <td>This is a dangerous permission to grant. Malicious applications could
 *       allow an attacker to release an existing module instance, so the
 *       runtime characteristics of the Java virtual machine are changed
 *       unexpectedly and could cause the system to misbehave.</td>
 * </tr>
 * <tr>
 *   <td>installModuleArchive</td>
 *   <td>Installs a module archive in a repository via calls to the
 *       {@code Repository}'s {@link Repository#install(URI)
 *       <tt>install</tt>} method.</td>
 *   <td>This allows an attacker to install malicious code into a repository.</td>
 * </tr>
 * <tr>
 *   <td>uninstallModuleArchive</td>
 *   <td>Uninstalls a module archive in a repository via calls to the
 *       {@code Repository}'s {@link Repository#uninstall(ModuleArchiveInfo)
 *       <tt>uninstall</tt>} method.</td>
 *   <td>This allows an attacker to remove critical module definitions from a
 *       repository.</td>
 * </tr>
 * <tr>
 *   <td>listModuleArchive</td>
 *   <td>Discovers the installed module archives in a repository via calls
 *       to the {@code Repository}'s {@link Repository#list()
 *       <tt>list</tt>} method.</td>
 *   <td>This allows an attacker to discover the installed module archives in a
 *       repository.</td>
 * </tr>
 * <tr>
 *   <td>shutdownRepository</td>
 *   <td>Shutdown a repository via calls
 *       to the {@code Repository}'s {@link Repository#shutdown()
 *       <tt>shutdown</tt>} method.</td>
 *   <td>This allows an attacker to shutdown a repository so the repository
 *       can no longer serve any module definition.</td>
 * </tr>
 * <tr>
 *   <td>reloadRepository</td>
 *   <td>Reloads module definitions in a repository via calls
 *       to the {@code Repository}'s {@link Repository#reload()
 *       <tt>reload</tt>} method.</td>
 *   <td>This allows an attacker to reload the module definitions
 *       in the repository, so the runtime characteristics of the
 *       Java virtual machine are changed unexpectedly and could
 *       cause the system to misbehave.</td>
 * </tr>
 * <tr>
 *   <td>getModuleArchiveInfo</td>
 *   <td>Gets the module archive info of the module definition via calls
 *       to the {@code ModuleDefinition}'s
 *       {@link ModuleDefinition#getModuleArchiveInfo()
 *       <tt>getModuleArchiveInfo</tt>} method.</td>
 *   <td>This allows an attacker to access the module archive info of
 *       the module definition, which may has sensitive information.</td>
 * </tr>
 * <tr>
 *   <td>getModuleContent</td>
 *   <td>Gets the content of the module definition via calls
 *       to the {@code ModuleDefinition}'s
 *       {@link ModuleDefinition#getModuleContent()
 *       <tt>getModuleContent</tt>} method.</td>
 *   <td>This allows an attacker to access the content of the
 *       module definition, which may contain sensitive information.</td>
 * </tr>
 * <tr>
 *   <td>getVisibilityPolicy</td>
 *   <td>Gets the system's visibility policy
 *       via calls to the {@code Modules}'s
 *       {@link Modules#getVisibilityPolicy()
 *       <tt>getVisibilityPolicy</tt>} method.</td>
 * .</td>
 *   <td>This allows an attacker to determine which module definitions
 *       are visible.</td>
 * </tr>
 * <tr>
 *   <td>setImportOverridePolicy</td>
 *   <td>Changes the system's import override policy in the JAM module system
 *       via calls to the {@code Modules}'s
 *       {@link Modules#setImportOverridePolicy(ImportOverridePolicy)
 *       <tt>setImportOverridePolicy</tt>} method.</td>
 * .</td>
 *   <td>This allows an attacker to install a malicious import override
 *       policy to control the resolution of module instances in the JAM
 *       module system.</td>
 * </tr>
 * <tr>
 *   <td>getImportOverridePolicy</td>
 *   <td>Gets the system's import override policy in the JAM module system
 *       via calls to the {@code Modules}'s
 *       {@link Modules#getImportOverridePolicy()
 *       <tt>getImportOverridePolicy</tt>} method.</td>
 * .</td>
 *   <td>This allows an attacker to determine the resolution result of module
 *       instances in the JAM module system.</td>
 * </tr>
 * <tr>
 *   <td>addModuleSystemListener</td>
 *   <td>Adds a module system listener that listens to all module systems via
 *       calls to the {@code ModuleSystem}'s
 *       {@link ModuleSystem#addModuleSystemListener(ModuleSystemListener)
 *       <tt>addModuleSystemListener</tt>} method.</td>
 *   <td>This allows an attacker to monitor the events in the module systems.</td>
 * </tr>
 * <tr>
 *   <td>removeModuleSystemListener</td>
 *   <td>Removes a module system listener from listening to any module system
 *       via calls to the {@code ModuleSystem}'s
 *       {@link ModuleSystem#removeModuleSystemListener(ModuleSystemListener)
 *       <tt>removeModuleSystemListener</tt>} method.</td>
 *   <td>This allows an attacker to remove a system-provided event listener from
 *       the module systems.</td>
 * </tr>
 * <tr>
 *   <td>addRepositoryListener</td>
 *   <td>Adds a repository listener that listens to all repositories
 *       via calls to the {@code Repository}'s
 *       {@link Repository#addRepositoryListener(RepositoryListener)
 *       <tt>addRepositoryListener</tt>} method.</td>
 *   <td>This allows an attacker to monitor the events in the repositories.</td>
 * </tr>
 * <tr>
 *   <td>removeRepositoryListener</td>
 *   <td>Removes a repository listener from listening to any repository
 *       via calls to the {@code Repository}'s
 *       {@link Repository#removeRepositoryListener(RepositoryListener)
 *       <tt>removeRepositoryListener</tt>} method.</td>
 *   <td>This allows an attacker to remove a system-provided event
 *       listener from the repositories.</td>
 * </tr>
 * </table>
 * <p>
 * Programmers do not normally create {@code ModuleSystemPermission} objects
 * directly. Instead they are created by the security policy code based on
 * reading the security policy file.
 *
 * @see java.security.BasicPermission
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 * @see java.lang.SecurityManager
 * @see java.module.Modules
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystem
 * @see java.module.Repository
 *
 * @since 1.7
 */

public final class ModuleSystemPermission extends BasicPermission {

    private static final long serialVersionUID = 1228383723285909334L;

    /**
     * Constructs a new {@code ModuleSystemPermission} instance with the
     * specified name.
     *
     * @param name Permission name.
     * @throws IllegalArgumentException if the name argument is invalid.
     */
    public ModuleSystemPermission(String name) {
        super(name);
    }

    /**
     * Constructs a new {@code ModuleSystemPermission} instance.
     *
     * @param name Permission name.
     * @param actions Must be either null or the empty string.
     * @throws IllegalArgumentException if arguments are invalid.
     */
    public ModuleSystemPermission(String name, String actions) {
        super(name);
    }
}
