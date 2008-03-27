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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import sun.module.config.DefaultImportOverridePolicy;
import sun.module.config.DefaultVisibilityPolicy;
import sun.module.config.ModuleSystemConfig;
import sun.module.repository.LocalRepository;
import sun.module.repository.URLRepository;

/**
 * This class consists exclusively of static methods that are specifically for
 * the Java Module System. It contains methods which constructs module
 * definition that are defined by the system's module system. It also contains
 * methods that constructs the local repository and URL repository. In
 * addition, it contains methods for setting or getting the system's visibility
 * policy and import override policy.
 *
 * @see java.module.ImportOverridePolicy
 * @see java.module.Module
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystemEvent;
 * @see java.module.ModuleSystemPermission
 * @see java.module.Repository
 * @see java.module.VisibilityPolicy
 *
 * @since 1.7
 */
public class Modules {

    static  {
        // Load the import override policy and the visibililty policy as early
        // as possible. This is to avoid potential deadlock when setting up
        // the extension module loader that may cause these polcies to be
        // loaded through the extension classloader during module
        // initialization.
        getImportOverridePolicy();
        getVisibilityPolicy();
    }

    // Default import override policy
    private static ImportOverridePolicy importOverridePolicy;

    // Default visibility policy
    private static VisibilityPolicy visibilityPolicy;

    // private constructor to prevent instantiation and subclassing
    private Modules() {
        // empty
    }

    /**
     * Constructs and initializes a new repository instance that loads
     * module definitions from a directory on the file system.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's <code>checkPermission</code> method with
     * <code>ModuleSystemPermission("createRepository")</code> permission to
     * ensure it's ok to create a repository.
     *
     * @param parent the parent repository for delegation.
     * @param name the repository name.
     * @param source the directory on the file system.
     * @return a new repository instance.
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to create a new instance of repository.
     * @throws NullPointerException if <code>parent</code> is null,
     *         <code>name</code> is null, or <code>source</code> is null.
     * @throws IOException if the repository cannot be constructed and
     *         initialized.
     * @throws IllegalArgumentException if a circularity is detected.
     */
    public static Repository newLocalRepository(Repository parent, String name, File source)
            throws IOException {
        return new LocalRepository(parent, name, source.toURI().toURL());
    }

    /**
     * Constructs and initializes a new repository instance that loads
     * module definitions from a directory on the file system.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's <code>checkPermission</code> method with
     * <code>ModuleSystemPermission("createRepository")</code> permission to
     * ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param source the directory on the file system.
     * @return a new repository instance.
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to create a new instance of repository.
     * @throws NullPointerException if <code>name</code> is null,
     *         or <code>source</code> is null.
     * @throws IOException if the repository cannot be constructed and
     *         initialized.
     */
    public static Repository newLocalRepository(String name, File source)
            throws IOException {
        return new LocalRepository(name, source.toURI().toURL());
    }

    /**
     * Constructs a new repository instance that loads module definitions
     * from a directory on the file system, and initializes using information
     * from the given {@code config}.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's <code>checkPermission</code> method with
     * <code>ModuleSystemPermission("createRepository")</code> permission to
     * ensure it's ok to create a repository.
     *
     * @param parent the parent repository for delegation.
     * @param name the repository name.
     * @param source the directory on the file system.
     * @param config Map of configuration names to their values
     * @return a new repository instance.
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to create a new instance of repository.
     * @throws NullPointerException if <code>parent</code> is null,
     *         <code>name</code> is null, <code>source</code> is null,
     *         or <code>config</code> is null.
     * @throws IOException if the repository cannot be constructed and
     *         initialized.
     * @throws IllegalArgumentException if a circularity is detected.
     */
    public static Repository newLocalRepository(Repository parent, String name,
            File source, Map<String, String> config)
            throws IOException {
        return new LocalRepository(parent, name, source.toURI().toURL(), config);
    }

    /**
     * Constructs a new repository instance that loads module definitions
     * from a directory on the file system, and initializes using information
     * from the given {@code config}.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's <code>checkPermission</code> method with
     * <code>ModuleSystemPermission("createRepository")</code> permission to
     * ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param source the directory on the file system.
     * @param config Map of configuration names to their values
     * @return a new repository instance.
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to create a new instance of repository.
     * @throws NullPointerException if <code>name</code> is null,
     *         <code>source</code> is null, or <code>config</code> is null.
     * @throws IOException if the repository cannot be constructed and
     *         initialized.
     */
    public static Repository newLocalRepository(String name, File source,
            Map<String, String> config)
            throws IOException {
        return new LocalRepository(name, source.toURI().toURL(), config);
    }

    /**
     * Constructs and initializes a new repository instance that loads
     * module definitions from a codebase URL.
     * <p>
     * Information about the module definitions available from the
     * codebase URL must be published in a repository metadata file. The
     * contents of the file must follow the schema of the URL Repository
     * metadata for the Java Module System.
     * <p><i>
     *  {codebase}/repository-metadata.xml
     * <p></i>
     * When the repository is initialized, the repository metadata file
     * (i.e. repository-metadata.xml) would be downloaded from the
     * codebase URL.
     * <p>
     * In the repository metadata file, each module definition is described
     * with a name, a version, a platform binding, and a path (relative to
     * the codebase URL where the module file, the module archive, and/or
     * the packed module archive are located). If no path and no platform
     * binding is specified, the default path is "{name}/{version}". If the
     * path is not specified and the module definition has platform
     * binding, the default path is "{name}/{version}/{platform}-{arch}".
     * <p>
     * After the URL repository instance successfully downloads the
     * repository metadata file, the module file of each module definition
     * (i.e. MODULE.METADATA file) in the repository is downloaded based on
     * the information in the repository metadata file:
     * <p><i>
     *  {codebase}/{path}/MODULE.METADATA
     * <p></i>
     * If a module definition is platform-specific, its module file is
     * downloaded if and only if the platform binding described in the
     * repository metadata file matches the platform and the architecture
     * of the system.
     * <p>
     * Module definitions are available for searches after the URL
     * repository instance is initialized. If a module instance is
     * instantiated from a module definition that has no platform binding,
     * the module archive is downloaded by probing in the following order:
     * <p><i>
     *  {codebase}/{path}/{name}-{version}.jam.pack.gz<p>
     *  {codebase}/{path}/{name}-{version}.jam
     * <p></i>
     * On the other hand, if a module instance is instantiated from a
     * platform-specific module definition, the module archive is
     * downloaded by probing in the following order:
     * <p><i>
     *  {codebase}/{path}/{name}-{version}-{platform}-{arch}.jam.pack.gz<p>
     *  {codebase}/{path}/{name}-{version}-{platform}-{arch}.jam
     * <p></i>
     * To ensure the integrity of the separately-hosted module file is in
     * sync with that in the module archive of the same module definition,
     * they are compared bit-wise against each other after the module
     * archive is downloaded.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's <code>checkPermission</code> method with
     * a <code>ModuleSystemPermission("createRepository")</code>
     * permission to ensure it's ok to create a repository.
     *
     * @param parent the parent repository for delegation.
     * @param name the repository name.
     * @param codebase the source location.
     * @return a new repository instance.
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to create a new instance of repository.
     * @throws NullPointerException if <code>parent</code> is null,
     *         <code>name</code> is null, or <code>codebase</code> is null.
     * @throws IOException if the repository cannot be constructed and
     *         initialized.
     * @throws IllegalArgumentException if a circularity is detected.
     */
    public static Repository newURLRepository(Repository parent, String name, URL codebase)
            throws IOException {
        return new URLRepository(parent, name, codebase);
    }

    /**
     * Constructs and initializes a new repository instance that loads
     * module definitions from a codebase URL.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's <code>checkPermission</code> method with
     * <code>ModuleSystemPermission("createRepository")</code> permission to
     * ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param codebase the source location.
     * @return a new repository instance.
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to create a new
     *         instance of repository.
     * @throws NullPointerException if <code>name</code> is null, or
     *         <code>codebase</code> is null.
     * @throws IOException if the repository cannot be constructed and
     *         initialized.
     */
    public static Repository newURLRepository(String name, URL codebase)
            throws IOException {
        return new URLRepository(name, codebase);
    }

    /**
     * Constructs a new repository instance that loads module definitions
     * from a codebase URL, and initializes using information from the
     * given {@code config}.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's <code>checkPermission</code> method with
     * a <code>ModuleSystemPermission("createRepository")</code>
     * permission to ensure it's ok to create a repository.
     *
     * @param parent the parent repository for delegation.
     * @param name the repository name.
     * @param codebase the source location.
     * @param config Map of configuration names to their values
     * @return a new repository instance.
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to create a new instance of repository.
     * @throws NullPointerException if <code>parent</code> is null,
     *         <code>name</code> is null, <code>codebase</code> is null,
     *         or <code>config</code> is null.
     * @throws IOException if the repository cannot be constructed and
     *         initialized.
     * @throws IllegalArgumentException if a circularity is detected.
     */
    public static Repository newURLRepository(Repository parent, String name,
            URL codebase, Map<String, String> config)
            throws IOException {
        return new URLRepository(parent, name, codebase, config);
    }

    /**
     * Constructs a new repository instance that loads module definitions
     * from a codebase URL, and initializes using information from the
     * given {@code config}.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's <code>checkPermission</code> method with
     * a <code>ModuleSystemPermission("createRepository")</code>
     * permission to ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param codebase the source location.
     * @param config Map of configuration names to their values
     * @return a new repository instance.
     * @throws SecurityException if a security manager exists and
     *         its <tt>checkPermission</tt> method denies access
     *         to create a new instance of repository.
     * @throws NullPointerException if <code>name</code> is null,
     *         <code>codebase</code> is null, or <code>config</code> is null.
     * @throws IOException if the repository cannot be constructed and
     *         initialized.
     */
    public static Repository newURLRepository(String name, URL codebase,
            Map<String, String> config)
            throws IOException {
        return new URLRepository(name, codebase, config);
    }

    /**
     * Returns the system's import override policy for module definitions.
     * <p>
     * The default class of the override policy can be changed using the
     * <code>java.module.import.override.policy.classname</code> system property.
     *
     * @return the system's default import override policy for module definitions.
     */
    public synchronized static ImportOverridePolicy getImportOverridePolicy() {

        if (importOverridePolicy == null)  {
            try {
                String clazzName = java.security.AccessController.doPrivileged(
                    new sun.security.action.GetPropertyAction("java.module.import.override.policy.classname"));

                if (clazzName == null)
                    clazzName = ModuleSystemConfig.getImportOverridePolicyDefaultClassName();

                if (clazzName != null) {
                    // Use system classloader - the custom import override policy must be either
                    // in classpath or bootclasspath.
                    ClassLoader cl = ClassLoader.getSystemClassLoader();
                    Class clazz = null;

                    if (cl != null)
                        clazz = cl.loadClass(clazzName);

                    if (clazz != null)
                        importOverridePolicy = (ImportOverridePolicy) clazz.newInstance();
                }
            }
            catch(ClassNotFoundException cnfe) {
                // TODO: log?
            }
            catch(ClassCastException cce) {
                // TODO: log?
            }
            catch(IllegalAccessException iae) {
                // TODO: log?
            }
            catch(InstantiationException ie) {
                // TODO: log?
            }
            finally {
                if (importOverridePolicy == null)
                    importOverridePolicy = new DefaultImportOverridePolicy();
            }
        }

        return importOverridePolicy;
    }

    /**
     * Set the system's import override policy for module definitions.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's <code>checkPermission</code> method with
     * a <code>ModuleSystemPermission("setImportOverridePolicy")</code>
     * permission to ensure it's ok to set the system's default import
     * override policy..
     *
     * @param policy the import override policy for module definitions.
     * @throws NullPointerException if the policy is null.
     * @throws SecurityException if a security manager exists and its
     *         <tt>checkPermission</tt> method denies access to set the
     *         system's default import override policy.
     */
    public synchronized static void setImportOverridePolicy(ImportOverridePolicy policy) {
        if (policy == null)
            throw new NullPointerException("import override policy must not be null.");

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("setImportOverridePolicy"));
        }
        importOverridePolicy = policy;
    }

    /**
     * Returns the system's default visibility policy for module definitions
     * in the repository of the module system.
     * <p>
     * The default class of the visibility policy can be overridden using the
     * <code>java.module.visibility.policy.classname</code> system property.
     *
     * @return the system's default visibility policy for module definitions.
     */
    public synchronized static VisibilityPolicy getVisibilityPolicy() {

        if (visibilityPolicy == null)  {
            try {
                String clazzName = java.security.AccessController.doPrivileged(
                    new sun.security.action.GetPropertyAction("java.module.visibility.policy.classname"));

                if (clazzName == null)
                    clazzName = ModuleSystemConfig.getVisibilityPolicyDefaultClassName();

                if (clazzName != null) {
                    // Use system classloader - the custom visibility policy must be either
                    // in classpath or bootclasspath.
                    ClassLoader cl = ClassLoader.getSystemClassLoader();
                    Class clazz = null;

                    if (cl != null)
                        clazz = cl.loadClass(clazzName);

                    if (clazz != null)
                        visibilityPolicy = (VisibilityPolicy) clazz.newInstance();
                }
            }
            catch(ClassNotFoundException cnfe) {
                // TODO: log?
            }
            catch(ClassCastException cce) {
                // TODO: log?
            }
            catch(IllegalAccessException iae) {
                // TODO: log?
            }
            catch(InstantiationException ie) {
                // TODO: log?
            }
            finally {
                if (visibilityPolicy == null)
                    visibilityPolicy = new DefaultVisibilityPolicy();
            }
        }

        return visibilityPolicy;
    }

    /**
     * Returns a new ModuleDefinition for modules based on the
     * Java Module System format.
     *
     * <p>This method will typically called by repository implementations
     * and not by applications.
     *
     * @param metadata the contents of the {@code MODULE-INF/METADATA.MODULE}
     *   file
     * @param content the ModuleDefinitionContent to be used to access the
     *   contents of the module archive
     * @param repository the repository in which the module archive is stored
     * @param moduleReleasable the module instance instantiated from this module
     *   definition is releasable from the module system
     *
     * @throws NullPointerException if one of metadata, content, or repository
     *   are null
     * @throws ModuleFormatException if the contents of {@code metadata}
     *   are not well formed.
     *
     * @return a new ModuleDefinition
     */
    public static ModuleDefinition newJamModuleDefinition(byte[] metadata,
            ModuleDefinitionContent content, Repository repository, boolean moduleReleasable)
            throws ModuleFormatException {
        if (metadata == null) {
            throw new NullPointerException("metadata must not be null.");
        }
        if (content == null) {
            throw new NullPointerException("content must not be null.");
        }
        if (repository == null) {
            throw new NullPointerException("repository must not be null.");
        }
        return new JamModuleDefinition
            (null, null, metadata, null, content, repository, moduleReleasable);
    }

    /**
     * Returns a new ModuleDefinition for modules based on the
     * Java Module System format.
     *
     * <p>This method will typically called by repository implementations
     * and not by applications.
     * It is useful in case the metadata has not yet been retrieved but
     * the module name and version are available.
     *
     * @param name the name of the module definition
     * @param version the version of the module definition
     * @param metadataHandle a Callable from which the contents of the
     *   {@code MODULE-INF/METADATA.MODULE} file can be retrieved
     * @param content the ModuleDefinitionContent to be used to access the
     *   contents of the module archive
     * @param repository the repository in which the module archive is stored
     * @param moduleReleasable the module instance instantiated from this module
     *   definition is releasable from the module system
     *
     * @throws NullPointerException if one of
     *   name, version, metadataHandle, content, or repository
     *   are null
     *
     * @return a new ModuleDefinition
     */
    public static ModuleDefinition newJamModuleDefinition(String name, Version version,
            Callable<byte[]> metadataHandle, ModuleDefinitionContent content,
            Repository repository, boolean moduleReleasable) {
        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }
        if (version == null) {
            throw new NullPointerException("version must not be null.");
        }
        if (metadataHandle == null) {
            throw new NullPointerException("metadata handle must not be null.");
        }
        if (content == null) {
            throw new NullPointerException("content must not be null.");
        }
        if (repository == null) {
            throw new NullPointerException("repository must not be null.");
        }
        return new JamModuleDefinition
            (name, version, null, metadataHandle, content, repository, moduleReleasable);
    }
}
