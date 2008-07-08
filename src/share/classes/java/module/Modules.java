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
import sun.module.core.JamModuleDefinition;
import sun.module.config.DefaultImportOverridePolicy;
import sun.module.config.DefaultVisibilityPolicy;
import sun.module.config.ModuleSystemConfig;
import sun.module.repository.LocalRepository;
import sun.module.repository.URLRepository;

/**
 * This class consists exclusively of static methods that are specifically for
 * the JAM (JAva Module) modules in the JAM module system.
 * <p>
 * <h3> ModuleSystem implementation</h3>
 * The JAM module system provides a concrete {@link ModuleSystem}
 * implementation for JAM modules. Applications can obtain the
 * {@code ModuleSystem} objects by calling the
 * {@link getModuleSystem() <tt>getModuleSystem</tt>} method.
 * <p>
 * <h3> ModuleDefinition implementation</h3>
 * The JAM module system provides a concrete {@link ModuleDefinition}
 * implementation for JAM modules. Applications can obtain the
 * {@code ModuleDefinition} objects by calling one of the
 * {@link #newModuleDefinition(byte[], ModuleContent,Repository,boolean)
 * <tt>newModuleDefinition</tt>} factory methods.
 * <p>
 * <h3> Repository implementations</h3>
 * The JAM module system provides two concrete repository implementations:
 * <i>Local repository</i> and <i>URL repository</i>.
 * <p>
 * <h4>Local Repository</h4>
 * A <i>local repository</i> loads module definitions whose module archives
 * are stored in a directory in the file system. This allows deployers to
 * easily deploy module definitions into the repository by copying, ftp-ing,
 * or dragging-and-dropping the module archives into a directory. This also
 * facilitates sharing between different repositories if they process the
 * module archives from the same directory. Hence, this directory is also
 * called a <i>repository interchange directory</i>.
 * <p>
 * The policy for processing the module archives in the repository
 * interchange directory is as follows:
 * <ul>
 *      <li><p>Files whose names end in {@code .jam} that follow the naming
 *             convention scheme defined in Section 4.1 of the JAM Module
 *             System specification will be processed. Files whose names end
 *             in {@code .jam} but do not follow the naming convention scheme
 *             will be ignored. Files whose names end in
 *             {@code .jam.pack.gz}, {@code .jar}, {@code .zip} or other
 *             filename extensions would also be ignored.</p></li>
 *
 *      <li><p>Files would be considered regardless of whether or not they
 *             are "hidden" in the UNIX sense, i.e., the files are stored
 *             under a directory and the name of the directory begins with
 *             {@code '.'}.</p></li>
 *
 *      <li><p>Subdirectories would not be searched recursively, i.e.,
 *             if the directory is foo, the repository implementation should
 *             only looks for JAM files in {@code foo}, not in {@code foo/bar},
 *             {@code foo/baz}, etc.</p></li>
 *
 *      <li><p>The order in which the JAM files are enumerated is not specified
 *             and may vary from platform to platform and even from invocation
 *             to invocation on the same machine. If there is more than one JAM
 *             file containing the same version of the module definition, the
 *             repository implementation should only load the first enumerated
 *             one, and ignore the others.</p></li>
 * </ul>
 * <p>
 * Instances of this {@code Repository} can be constructed using one of the
 * {@link #newLocalRepository(String, File, Map, Repository)
 * <tt>newLocalRepository</tt>} factory methods, and the factory methods
 * also invokes the {@code Repository}'s
 * {@link Repository#initialize() <tt>initialize</tt>} method automatically.
 * <p>
 * Instance of this {@code Repository} is read-only if its <i>repository
 * interchange directory</i> is read-only during repository
 * initialization. Instance of this {@code Repository} also supports the
 * {@link Repository#install(URI) <tt>install</tt>} and
 * {@link Repository#uninstall(ModuleArchiveInfo) <tt>uninstall</tt>} operations
 * if the {@code Repository} is not read-only.
 * <p>
 * Instance of this {@code Repository} supports reload. When the
 * {@code Repository} instance is reloaded, the set of module archives
 * in the <i>repository interchange directory</i> are checked against
 * the set of module archives that were recognized during repository
 * initialization to determine if the set of module archives and the
 * set of module definitions should be changed. See
 * {@link Repository#reload()} for more details.
 * <p>
 * Below is an example showing how to construct a <i>local repository</i> to
 * search for a specific module definition:
 * <pre>
 *      // Create a local repository instance. The source location is
 *      // interpreted by the repository implementation as a local directory
 *      // where the module definitions are stored. The local repository
 *      // instance is automatically initialized during construction.
 *      File file = new File("/home/wombat/repository");
 *      Repository repo = Modules.newLocalRepository("wombat", file, null);
 *
 *      // Search org.foo.xml version 1.0.0 from the repository instance.
 *      ModuleDefinition moduleDef = repo.find("org.foo.xml", VersionConstraint.valueOf("1.0.0"));</pre>
 *
 * <h4>URL Repository</h4>
 * A <i>URL repository</i> loads module definitions whose module archives
 * are stored in a codebase URL, and it is designed to optimize loading
 * through specific files and directories layout. <i>URL repository</i>
 * is typically used to download module definitions from server, but it
 * could also be used with a file-based URL.
 * <p>
 * Instance of this {@code Repository} can be constructed using one of the
 * {@link #newURLRepository(String, URL, Map, Repository)
 * <tt>newURLRepository</tt>} factory methods, and the factory methods also
 * invokes the newly constructed {@code Repository}'s
 * {@link Repository#initialize() <tt>initialize</tt>} method automatically.
 * <p>
 * Information about the module definitions available from the
 * codebase URL must be published in a <i>repository metadata file</i>
 * (i.e. {@code repository-metadata.xml}. The contents of the file must
 * follow the schema of the URL Repository metadata described in the
 * JAM Module System specification.
 * <p>
 * When this {@code Repository}is initialized, the repository metadata file
 * (i.e. repository-metadata.xml) is downloaded from the codebase URL.
 * <pre>
 *      {codebase}/repository-metadata.xml</pre>
 * In the repository metadata file, each module definition is described
 * with a name, a version, a platform binding, and a path (relative to
 * the codebase URL where the JAM module metadata file, the module
 * archive, and/or the packed module archive are located). If no path
 * and no platform binding is specified, the default path is
 * {@code "{name}/{version}"}. If the path is not specified and the
 * module definition has platform binding, the default path is
 * {@code "{name}/{version}/{platform}-{arch}"}.
 * <p>
 * After the {@code Repository} successfully downloads the repository
 * metadata file, the JAM module metadata file of each module definition
 * (i.e. {@code MODULE.METADATA file}) in the repository is downloaded
 * based on the information in the <i>repository metadata file</i>:
 * <pre>
 *      {codebase}/{path}/MODULE.METADATA</pre>
 * If a module definition is platform-specific, its JAM module metadata file
 * is downloaded if and only if the platform binding described in the
 * <i>repository metadata file</i> matches the platform and the architecture
 * of the system.
 * <p>
 * Module definitions are available for searches using one of the
 * {@link Repository#find(Query) <tt>find</tt>} methods after the
 * {@code Repository} instance is initialized. If a module instance is
 * instantiated from a module definition that has no platform binding, the
 * module archive is downloaded by probing in the following order:
 * <pre>
 *      {codebase}/{path}/{name}-{version}.jam.pack.gz
 *      {codebase}/{path}/{name}-{version}.jam</pre>
 * On the other hand, if a module instance is instantiated from a
 * platform-specific module definition, the module archive is
 * downloaded by probing in the following order:
 * <pre>
 *      {codebase}/{path}/{name}-{version}-{platform}-{arch}.jam.pack.gz
 *      {codebase}/{path}/{name}-{version}-{platform}-{arch}.jam</pre>
 * To ensure the integrity of the separately-hosted JAM module metadata
 * file is in sync with that in the module archive of the same module
 * definition, they are compared bit-wise against each other after the
 * module archive is downloaded when the module instance is
 * instantiated from the module definition. If these JAM module metadata
 * files are not in sync, the instantiation of the module definition
 * will fail.
 * <p>
 * Instance of this {@code Repository} is read-only unless the codebase
 * URL is a file-based URL which represents a writable directory on the
 * file system. Instance of this {@code Repository}
 * supports the {@link Repository#install(java.net.URI) <tt>install</tt>} and
 * {@link Repository#uninstall(ModuleArchiveInfo) <tt>uninstall</tt>} operations
 * when the {@code Repository} is not read-only.
 * <p>
 * Instance of this {@code Repository} supports reload. When the
 * {@code Repository} instance is reloaded, the timestamp of the
 * <i>repository metadata file</i> is checked against the timestamp of
 * the same file that was downloaded during repository initialization.
 * If the timestamps are identical, there is no change in the set of
 * module archives and the set of module definitions in the repository.
 * Otherwise, all existing module archives and module definitions in
 * the repository are removed, the repository metadata file is downloaded,
 * and the module definitions described in the <i>repository metadata file</i>
 * are downloaded and are available for subsequent searches.
 * See {@link Repository#reload()} for more details.
 * <p>
 * Below is an example showing how to construct a <i>URL repository</i> to
 * search for a specific module definition:
 * <pre>
 *      // Create a URL repository instance. The source location is
 *      // interpreted by the repository implementation as the codebase
 *      // where the module definitions are downloaded on demand based on
 *      // requests. The URL repository instance is automatically initialized
 *      // during construction.
 *      //
 *      URL url = new URL("http://x.y.z/");
 *      Repository repo = Modules.newURLRepository("x.y.z", url, null);
 *
 *      // Search org.foo.xml version 1.3.0 from the repository instance.
 *      ModuleDefinition moduleDef = repo.find("org.foo.xml", VersionConstraint.valueOf("1.3.0"));</pre>
 *
 * <h3> Module dependency and package dependency implementations</h3>
 * The JAM module system provides a concrete implementation for
 * {@link ModuleDependency} and another concrete implementation for
 * {@link PackageDependency}. Instances of the {@code ModuleDependency}
 * can be created using the
 * {@link #newModuleDependency(String, VersionConstraint, boolean, boolean, Map)
 * <tt>newModuleDependency</tt>} method, and
 * instances of the {@code PackageDependency} can be created using the
 * {@link #newPackageDependency(String, VersionConstraint, boolean, boolean, Map)
 * <tt>newPackageDependency</tt>} method.
 * <p>
 * <h3> Import override policy</h3>
 * The JAM module system allows deployers to narrow the version constraints
 * in the import dependencies of a specific module definition using
 * {@link ImportOverridePolicy} to control the resolution during module
 * initialization.
 * <p>
 * The system's import override policy can be obtained using the
 * {@link getImportOverridePolicy()
 * <tt>getImportOverridePolicy</tt>} method. The system's import override policy
 * can be changed using the {@link setImportOverridePolicy(ImportOverridePolicy)
 * <tt>setImportOverridePolicy</tt>} method.
 * <p>
 * @see java.module.ImportOverridePolicy
 * @see java.module.Module
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleSystemEvent
 * @see java.module.ModuleSystemPermission
 * @see java.module.Repository
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

    // ModuleSystem implementation for JAM module system.
    private static ModuleSystem defaultImpl = null;

    // Default import override policy
    private static ImportOverridePolicy importOverridePolicy;

    // Default visibility policy
    private static VisibilityPolicy visibilityPolicy;

    // private constructor to prevent instantiation and subclassing
    private Modules() {
        // empty
    }

    /**
     * Returns the JAM module system.
     *
     * @return the JAM module system.
     */
    public static synchronized ModuleSystem getModuleSystem()  {
        if (defaultImpl == null)  {
            defaultImpl = sun.module.core.ModuleSystemImpl.INSTANCE;
        }
        return defaultImpl;
    }

    /**
     * Constructs a new {@code ModuleDependency} instance.
     *
     * @param name the name of the imported module.
     * @param constraint the version constraint.
     * @param reexport true if the imported module is re-exported; otherwise, false.
     * @param optional true if the module dependency is optional; otherwise, false.
     * @param attributes map of attributes in the module dependency; null if no
     *        attributes.
     * @throws NullPointerException if name is null or constraint is null.
     */
    public static ModuleDependency newModuleDependency(String name, VersionConstraint constraint,
                                                       boolean reexport, boolean optional,
                                                       Map<String, String> attributes)  {
        return new sun.module.core.JamModuleDependency(name, constraint, reexport, optional, attributes);
    }

    /**
     * Constructs a new {@code PackageDependency} instance.
     *
     * @param name the name of the package.
     * @param constraint the version constraint.
     * @param reexport true if the imported package is re-exported; otherwise, false.
     * @param optional true if the package dependency is optional; otherwise, false.
     * @param attributes map of attributes in the package depdenency; null if no
     *        attributes.
     * @throws NullPointerException if name is null or constraint is null.
     */
    public static PackageDependency newPackageDependency(String name, VersionConstraint constraint,
                                                         boolean reexport, boolean optional,
                                                         Map<String, String> attributes)  {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * Constructs a new {@code Repository} instance that loads module
     * archives from a directory on the file system, and initializes
     * using information from the given {@code config}.
     * <p>
     * If {@code config} is null, the configuration is ignored.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with
     * {@code ModuleSystemPermission("createRepository")} permission to
     * ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param source the directory on the file system.
     * @param config Map of configuration names to their values
     * @param parent the parent repository for delegation.
     * @return a new {@code Repository} instance.
     * @throws SecurityException if a security manager exists and
     *         its {@code checkPermission} method denies access
     *         to create a new repository instance.
     * @throws NullPointerException if name is null, source is null, or
     *         parent is null.
     * @throws IOException if the repository cannot be constructed and
     *         initialized.
     * @throws IllegalArgumentException if a circularity is detected.
     */
    public static Repository newLocalRepository(String name,
            File source,
            Map<String, String> config,
            Repository parent)
            throws IOException {
        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }
        if (source == null) {
            throw new NullPointerException("source must not be null.");
        }
        if (parent == null) {
            throw new NullPointerException("parent must not be null.");
        }

        return new LocalRepository(name, source.toURI(), config, parent);
    }

    /**
     * Constructs a new {@code Repository} instance that loads module
     * definitions from a directory on the file system, and initializes
     * the repository using information from the given {@code config}.
     * Equivalent to:
     * <pre>
     *      newLocalRepository(name, codebase, config, Repository.getSystemRepository());</pre>
     * If {@code config} is null, the configuration is ignored.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with
     * {@code ModuleSystemPermission("createRepository")} permission to
     * ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param source the directory on the file system.
     * @param config Map of configuration names to their values
     * @return a new {@code Repository} instance.
     * @throws SecurityException if a security manager exists and
     *         its {@code checkPermission} method denies access
     *         to create a new repository instance.
     * @throws NullPointerException if name is null or source is null.
     * @throws IOException if the repository cannot be constructed and
     *         initialized.
     */
    public static Repository newLocalRepository(String name, File source,
            Map<String, String> config)
            throws IOException {
        if (name == null)  {
            throw new NullPointerException("name must not be null.");
        }
        if (source == null)  {
            throw new NullPointerException("source must not be null.");
        }

        return new LocalRepository(name, source.toURI(), config,
                            Repository.getSystemRepository());
    }

    /**
     * Constructs and initializes a new {@code Repository} instance that loads
     * module definitions from a codebase URL.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("createRepository")} permission to ensure
     * it's ok to create a repository.
     *
     * @param name the repository name.
     * @param codebase the source location.
     * @param config Map of configuration names to their values
     * @param parent the parent repository for delegation.
     * @return a new {@code Repository} instance.
     * @throws SecurityException if a security manager exists and
     *         its {@code checkPermission} method denies access
     *         to create a new repository instance.
     * @throws NullPointerException if name is null, codebase is null,
     *         or parent is null.
     * @throws IOException if the repository cannot be constructed and
     *         initialized.
     * @throws IllegalArgumentException if a circularity is detected.
     */
    public static Repository newURLRepository(String name, URL codebase,
                                              Map<String, String> config,
                                              Repository parent)
            throws IOException {
        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }
        if (codebase == null) {
            throw new NullPointerException("source must not be null.");
        }
        if (parent == null) {
            throw new NullPointerException("parent must not be null.");
        }

        try {
            return new URLRepository(name, codebase.toURI(), config, parent);
        } catch (java.net.URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Constructs a new {@code Repository} instance that loads module
     * definitions from a codebase URL, and initializes using information
     * from the given {@code config}. Equivalent to:
     * <pre>
     *      newURLRepository(name, codebase, config, Repository.getSystemRepository());</pre>
     * If a security manager is present, this method calls the
     * security manager's {@code checkPermission} method with
     * a {@code ModuleSystemPermission("createRepository")}
     * permission to ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param codebase the source location.
     * @param config Map of configuration names to their values
     * @return a new repository instance.
     * @throws SecurityException if a security manager exists and
     *         its {@code checkPermission} method denies access
     *         to create a new instance of repository.
     * @throws NullPointerException if name is null or codebase is null.
     * @throws IOException if the repository cannot be constructed and
     *         initialized.
     */
    public static Repository newURLRepository(String name, URL codebase,
            Map<String, String> config)
            throws IOException {
        return newURLRepository(name, codebase, config, Repository.getSystemRepository());
    }

    /**
     * Returns the system's import override policy for module definitions.
     * <p>
     * The default class of the override policy can be changed using the
     * {@code java.module.import.override.policy.classname} system property.
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
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("setImportOverridePolicy")}
     * permission to ensure it's ok to set the system's default import
     * override policy.
     *
     * @param policy the import override policy for module definitions.
     * @throws SecurityException if a security manager exists and its
     *         {@code checkPermission} method denies access to set the
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
     * in the repositories.
     * <p>
     * The default class of the visibility policy can be overridden using the
     * {@code java.module.visibility.policy.classname} system property.
     *
     * @return the system's default visibility policy for module definitions.
     */
    synchronized static VisibilityPolicy getVisibilityPolicy() {

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
     * Constructs a new {@code ModuleDefinition} instance for a JAM module.
     *
     * <p>This method will typically be called by repository implementations
     * and not by applications.
     *
     * @param metadata an array of bytes which is the content of the
     *        module metadata file
     * @param content the {@code ModuleContent} to be used to access the
     *   contents of the module definition
     * @param repository the {@code Repository} in which the module definition
     *        is associated with
     * @param moduleReleasable true if the module instance instantiated from
     *        this {@code ModuleDefinition} is releasable from its module
     *        system
     * @throws ModuleFormatException if the content of module metadata file
     *         is not recognized or is not well formed.
     * @return a new {@code ModuleDefinition}.
     */
    public static ModuleDefinition newModuleDefinition(byte[] metadata,
            ModuleContent content, Repository repository, boolean moduleReleasable)
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
            (getModuleSystem(),
             null, null, metadata, null, content, repository, moduleReleasable);
    }

    /**
     * Constructs a new {@code ModuleDefinition} instance for a JAM module.
     *
     * <p>This method will typically be called by repository implementations
     * and not by applications. It is useful in case the metadata has not
     * yet been retrieved but the module name and version are available.
     *
     * @param name the name of the {@code ModuleDefinition}
     * @param version the version of the {@code ModuleDefinition}
     * @param metadataHandle a Callable from which the contents of the
     *        {@code MODULE-INF/METADATA.MODULE} file can be retrieved
     *        as an array of bytes
     * @param content the {@code ModuleContent} to be used to access the
     *        contents of the module definition
     * @param repository the {@code Repository} in which the module definition
     *        is associated with
     * @param moduleReleasable true if the module instance instantiated from
     *        this {@code ModuleDefinition} is releasable from the module
     *        system
     * @return a new {@code ModuleDefinition}.
     */
    public static ModuleDefinition newModuleDefinition(String name, Version version,
            Callable<byte[]> metadataHandle, ModuleContent content,
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
            (getModuleSystem(), name, version, null,
             metadataHandle, content, repository, moduleReleasable);
    }
}
