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
import java.nio.ByteBuffer;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import sun.module.core.JamModuleDefinition;
import sun.module.core.ModuleSystemImpl;
import sun.module.config.DefaultImportOverridePolicy;
import sun.module.config.DefaultVisibilityPolicy;
import sun.module.config.ModuleSystemConfig;
import sun.module.repository.LocalRepository;
import sun.module.repository.URLRepository;

/**
 * This class consists exclusively of static methods that are specifically for
 * the JAM modules in the JAM module system.
 * <p>
 * <h2> JAM Module System</h2>
 *
 * The JAM module system is a concrete module system
 * implementation and is the default module system within the Java Module
 * System. See the JAM Module System Specification for more details.
 *
 * <h3> ModuleSystem</h3>
 * The JAM module system provides a concrete {@link ModuleSystem}
 * implementation for JAM modules. Applications can obtain the
 * {@code ModuleSystem} objects by calling the
 * {@link #getModuleSystem() <tt>getModuleSystem</tt>} method.
 * <p>
 * <h3> ModuleDefinition</h3>
 * The JAM module system provides a concrete {@link ModuleDefinition}
 * implementation for JAM modules. Applications can obtain the
 * {@code ModuleDefinition} objects by calling the
 * {@link #newModuleDefinition(java.nio.ByteBuffer,ModuleContent,Repository,boolean)
 * <tt>newModuleDefinition</tt>} factory methods.
 * <p>
 * <h3> Repository</h3>
 * The JAM module system provides two concrete repository implementations:
 * <i>Local repository</i> and <i>URL repository</i>.  Both repository
 * implementations must support the JAM file format as described in the JAM
 * Module System Specification. Both repository implementations must also
 * validate the integrity of a JAM file when the file is installed.
 * <p>
 * <A NAME="SelectionPolicy"></A><h5>Selection policy for JAM modules</h5>
 *
 * Local repository and URL repository have both platform-neutral and
 * <a href="package-summary.html#PlatformSpecificModules">platform-specific modules</a>
 * installed. Implementations of these repositories must use the following
 * policy to select the set of JAM modules appropriate for the platform and
 * architecture of the system:
 * <ul>
 *      <li><p>JAM module with platform binding which does not match
 *             the platform and architecture of the system is ignored.
 *             </p></li>
 *
 *      <li><p>JAM module with platform binding which matches
 *             the system's platform and architecture is preferred
 *             over the JAM module of the same name and version with no
 *             platform binding.</p></li>
 * </ul>
 *
 * <h4>Local Repository</h4>
 * <i>Local repository</i> supports loading JAM modules from a directory on
 * a file system. Different repository instances in the same or different
 * Java virtual machines can process the
 * JAM modules from the same directory for sharing purposes. Hence, the
 * directory is also called <i>repository interchange directory</i>.
 * <p>
 * A local repository must follow the policy below to
 * process JAM modules in the repository interchange directory, when the
 * {@link Repository#initialize() <tt>initialize</tt>} or
 * {@link Repository#reload() <tt>reload</tt>} method is called:
 * <ul>
 *      <li><p>A local repository must process JAM file which
 *             has {@code .jam} extension and follows the naming convention
 *             defined in Section 3.1 of the JAM Module System Specification.
 *             A local repository must ignore JAM file which
 *             has {@code .jam} extension but does not follow the naming
 *             convention.
 *             A local repository must ignore JAM file which
 *             has {@code .jam.pack.gz}, {@code .jar}, {@code .zip} or other
 *             filename extensions.
 *
 *      <li><p>A local repository must support a
 *             "hidden" (in the UNIX sense) repository interchange directory.
 *             </p></li>
 *
 *      <li><p>A local repository must ignore subdirectories
 *             under the repository interchange directory.
 *             </p></li>
 *
 *      <li><p>Local repository implementations may enumerate the JAM files in
 *             any order.</p></li>
 *
 *      <li><p>Local repository implementations must ignore the duplicates
 *             if there is more than one JAM file containing the same JAM module.
 *             </p></li>
 * </ul>
 * <p>
 * A local repository must follow the
 * <a href="#SelectionPolicy">selection policy</a> to determine a
 * selected set of JAM modules, from the set of JAM modules available
 * in the repository interchange directory. Local repository
 * implementations must only create {@code ModuleDefinition}s for the
 * selected set of JAM modules.
 * <p>
 * Local repository implementations must support the
 * {@link Repository#reload() <tt>reload</tt>} operation. During reload,
 * a local repository must compare the set of JAM
 * modules in the repository interchange directory against
 * the set of JAM modules which was last recognized by the local
 * repository.
 * See
 * {@link Repository#reload()} for more details.
 * <p>
 * Local repository implementations must become
 * {@linkplain Repository#isReadOnly() read-only}
 * if the repository interchange directory is read-only as determined
 * when the repository is initialized and reloaded.
 * Local repository implementations must support the
 * {@link Repository#install(URI) <tt>install</tt>} and
 * {@link Repository#uninstall(ModuleArchiveInfo) <tt>uninstall</tt>} operations.
 * <p>
 * Instances of this {@code Repository} can be constructed using one of the
 * {@link #newLocalRepository(String, File, Map, Repository)
 * <tt>newLocalRepository</tt>} factory methods; the factory methods
 * also invokes the {@code Repository}'s
 * {@link Repository#initialize() <tt>initialize</tt>} method automatically.
 * <p>
 * Below is an example showing how to construct a <i>local repository</i> to
 * search for a specific module definition:
 * <pre>
 *      // Create a local repository instance. The source location is
 *      // interpreted by the repository implementation as a local directory
 *      // where the JAM files are stored. The local repository
 *      // instance is automatically initialized during construction.
 *      File file = new File("/home/wombat/repository");
 *      Repository repo = Modules.newLocalRepository("wombat", file, null);
 *
 *      // Search org.foo.xml version 1.0.0 from the repository instance.
 *      ModuleDefinition moduleDef = repo.find("org.foo.xml", VersionConstraint.valueOf("1.0.0"));</pre>
 *
 * <h4>URL Repository</h4>
 * <i>URL repository</i> supports loading JAM modules
 * from a codebase URL. URL repository is typically used to
 * download JAM modules from server, but it can also be used with a
 * file-based URL.
 * <p>
 * URL repository must have a <i>repository metadata file</i>
 * (i.e. repository-metadata.xml) which describes the JAM modules in
 * the repository. The <i>repository metadata file</i> must exist
 * directly under the codebase URL, and it must conform to the
 * <a href="RepositoryMetadata.xml">repository metadata schema</a>.
 * <pre>
 *      {codebase}/repository-metadata.xml</pre>
 *
 * A JAM module in the URL repository is identified by placing an entry
 * in the <i>repository metadata file</i>. Each entry has a
 * module name, module version, path (relative path to a location
 * where the module is stored under the codebase URL), and platform
 * binding. URL repository implementations must ignore the duplicates
 * if there is more than one entry for the JAM module with the
 * same name, version, and platform binding.
 * For a given JAM module in the URL repository, its module
 * metadata file {@code MODULE.METADATA} must exist under the path, and
 * its module archive (i.e. either {@code .jam} file,
 * {@code .jam.pack.gz} file, or both} must also exist under the path.
 * <h5>Platform-neutral JAM module</h5>
 * <pre>
 *      {codebase}/{path}/MODULE.METADATA
 *      {codebase}/{path}/&lt;module-name&gt;-&lt;module-version&gt;.jam
 *      {codebase}/{path}/&lt;module-name&gt;-&lt;module-version&gt;.jam.pack.gz</pre>
 *
 * <h5>Platform-specific JAM module</h5>
 * <pre>
 *      {codebase}/{path}/MODULE.METADATA
 *      {codebase}/{path}/&lt;module-name&gt;-&lt;module-version&gt;[&lt;platform&gt;-&lt;arch&gt;].jam
 *      {codebase}/{path}/&lt;module-name&gt;-&lt;module-version&gt;[&lt;platform&gt;-&lt;arch&gt;].jam.pack.gz</pre>
 *
 * URL repository implementations must follow the
 * <a href="#SelectionPolicy">selection policy</a> to determine a
 * selected set of JAM modules, from the set of JAM modules available
 * in the repository as described in the repository metadata file.
 * URL repository implementations must only create
 * {@code ModuleDefinition}s for the selected set of JAM modules.
 * <p>
 * For each JAM module in the selected set,
 * <ul>
 *      <li><p>URL repository implementations must either download the
 *             {@code .jam} file or {@code .jam.pack.gz} file for the given
 *             JAM module. URL repository implementations must prefer
 *             the {@code .jam.pack.gz} file over the {@code .jam} file
 *             if the codebase URL is not file-based.</p></li>
 *
 *      <li><p>URL repository implementations must ignore the JAM module
 *             if the module's name, version, and platform binding specified
 *             in the repository metadata file do not match the information
 *             in the module metadata file
 *             (i.e. {@code MODULE.METADATA file}).</p></li>
 *
 *      <li><p>A URL repository implementation must throw exception in
 *             methods of {@link ModuleContent} in the
 *             {@link ModuleDefinition}s the URL repository creates, if
 *             the module metadata file (i.e. {@code MODULE.METADATA
 *             file}) is not bit-wise equal to the module metadata file
 *             inside the {@code .jam} file or {@code .jam.pack.gz} file
 *             which the URL repository has downloaded.
 *             </p></li>
 * </ul>
 * URL repository is {@linkplain Repository#isReadOnly() read-only}
 * if the codebase is a non-file-based URL or it is a file-based URL
 * which represents a read-only directory on the file system.
 * URL repository implementations must support the
 * {@link Repository#install(java.net.URI) <tt>install</tt>} and
 * {@link Repository#uninstall(ModuleArchiveInfo) <tt>uninstall</tt>}
 * operations for JAM files.
 * <p>
 * A URL repository must support the
 * {@linkplain Repository#reload() reload}
 * operation; it must compare the timestamp of the
 * <i>repository metadata file</i> against the timestamp of the same file
 * which was last downloaded. If the timestamps are different, a URL
 * repository must do the following:
 * <ol>
 *      <li><p>Download the repository metadata file and determine
 *             the set of JAM modules based on the selection policy.</p></li>
 *      <li><p>Remove all existing module archives and module definitions
 *             from the repository.</p></li>
 *      <li><p>Add new module archives and module definitions to the
 *             repository.</p></li>
 * </ol>
 * See {@link Repository#reload()} for more details.
 * <p>
 * Instance of this {@code Repository} can be constructed using one of the
 * {@link #newURLRepository(String, URL, Map, Repository)
 * <tt>newURLRepository</tt>} factory methods; the factory methods also
 * invokes the newly constructed {@code Repository}'s
 * {@link Repository#initialize() <tt>initialize</tt>} method automatically.
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
 * <h3> Module dependency and package dependency</h3>
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
 * {@link #getImportOverridePolicy()
 * <tt>getImportOverridePolicy</tt>} method. The system's import override policy
 * can be changed using the {@link #setImportOverridePolicy(ImportOverridePolicy)
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

    private static WeakHashMap<ThreadGroup, ModuleSystem> moduleSystemMap = new WeakHashMap<ThreadGroup, ModuleSystem>();

    // Default import override policy
    private static ImportOverridePolicy importOverridePolicy;

    // Default visibility policy
    private static VisibilityPolicy visibilityPolicy;


    static
    {
        // Construct a JAM module system instance for the main thread group
        // as early as possible.
        getModuleSystem();

        // Load the import override policy and the visibililty policy as early
        // as possible. This is to avoid potential deadlock when setting up
        // the extension module loader that may cause these polcies to be
        // loaded through the extension classloader during module
        // initialization.
        getImportOverridePolicy();
        getVisibilityPolicy();
    }

    // private constructor to prevent instantiation and subclassing
    private Modules() {
        // empty
    }

    /**
     * Returns a JAM module system instance.
     * <p>
     * Each JAM module system instance must create all new threads in a
     * {@link java.lang.ThreadGroup}. If a security manager is present,
     * this method returns a JAM module system instance which uses the
     * {@linkplain java.lang.SecurityManager#getThreadGroup()
     * thread group of the system's security manager}; otherwise,
     * this method returns a JAM module system instance which uses the
     * thread group of the current thread.
     *
     * @return an instance of the JAM module system.
     */
    public static synchronized ModuleSystem getModuleSystem()  {
        SecurityManager sm = System.getSecurityManager();
        final ThreadGroup group = (sm != null) ? sm.getThreadGroup() :
                                  Thread.currentThread().getThreadGroup();

        ModuleSystem ms = moduleSystemMap.get(group);
        if (ms == null) {
            ms = AccessController.doPrivileged(new PrivilegedAction<ModuleSystem>() {
                        public ModuleSystem run() {
                            return new ModuleSystemImpl(group);
                        }
                    });
            moduleSystemMap.put(group, ms);
        }
        return ms;
    }

    /**
     * Returns a new {@code ModuleDependency} instance.
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
     * Returns a new {@code PackageDependency} instance.
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
     * Returns a a new {@code Repository} instance which supports
     * JAM modules in a directory, using the given
     * {@code config}. The newly created {@code Repository}
     * instance is initialized automatically.
     * <p>
     * If {@code config} is null, the configuration is ignored.
     * The configuration is implementation dependent.
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
     * Returns a a new {@code Repository} instance which supports
     * JAM modules in a directory, using
     * the given {@code config}. The newly created {@code Repository}
     * instance is initialized automatically. Equivalent to:
     * <pre>
     *      newLocalRepository(name, codebase, config, Repository.getApplicationRepository());</pre>
     * If {@code config} is null, the configuration is ignored.
     * The configuration is implementation dependent.
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
                            Repository.getApplicationRepository());
    }

    /**
     * Returns a a new {@code Repository} instance which supports
     * JAM modules from a codebase URL, using
     * the given {@code config}. The newly created {@code Repository}
     * instance is initialized automatically.
     * <p>
     * If {@code config} is null, the configuration is ignored.
     * The configuration is implementation dependent.
     * <p>
     * If a security manager is present, this method calls the security
     * manager's {@code checkPermission} method with a
     * {@code ModuleSystemPermission("createRepository")} permission to ensure
     * it's ok to create a repository.
     *
     * @param name the repository name.
     * @param codebase the code base of the repository.
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
     * Returns a a new {@code Repository} instance which supports
     * JAM modules from a codebase URL, using
     * the given {@code config}. The newly created {@code Repository}
     * instance is initialized automatically. Equivalent to:
     * <pre>
     *      newURLRepository(name, codebase, config, Repository.getApplicationRepository());</pre>
     * If {@code config} is null, the configuration is ignored.
     * The configuration is implementation dependent.
     * <p>
     * If a security manager is present, this method calls the
     * security manager's {@code checkPermission} method with
     * a {@code ModuleSystemPermission("createRepository")}
     * permission to ensure it's ok to create a repository.
     *
     * @param name the repository name.
     * @param codebase the code base of the repository.
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
        return newURLRepository(name, codebase, config, Repository.getApplicationRepository());
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
     * Returns a new {@code ModuleDefinition} instance for a JAM module.
     * Equivalent to:
     * <pre>
     *      newModuleDefinition(metadata, content, repository, moduleReleasable, getModuleSystem());</pre>
     *
     * The content of the byte buffer is copied.
     * This method will typically be called by repository implementations
     * and not by applications.
     *
     * @param metadata a byte buffer which contains the content of the
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
    public static ModuleDefinition newModuleDefinition(ByteBuffer metadata,
            ModuleContent content, Repository repository, boolean moduleReleasable)
            throws ModuleFormatException {
        return newModuleDefinition(metadata, content, repository, moduleReleasable, getModuleSystem());
    }

    /**
     * Returns a new {@code ModuleDefinition} instance for a JAM module.
     * <p>
     * The content of the byte buffer is copied.
     * This method will typically be called by repository implementations
     * and not by applications.
     *
     * @param metadata a byte buffer which contains the content of the
     *        module metadata file
     * @param content the {@code ModuleContent} to be used to access the
     *   contents of the module definition
     * @param repository the {@code Repository} in which the module definition
     *        is associated with
     * @param moduleReleasable true if the module instance instantiated from
     *        this {@code ModuleDefinition} is releasable from its module
     *        system
     * @param moduleSystem the {@code ModuleSystem} instance to be associated with
     *        the module definition.
     * @throws ModuleFormatException if the content of module metadata file
     *         is not recognized or is not well formed.
     * @return a new {@code ModuleDefinition}.
     */
    public static ModuleDefinition newModuleDefinition(ByteBuffer metadata,
            ModuleContent content, Repository repository, boolean moduleReleasable,
            ModuleSystem moduleSystem)
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
        if (moduleSystem == null)  {
            throw new NullPointerException("moduleSystem must not be null.");
        }

        byte[] metadataBytes = new byte[metadata.remaining()];
        metadata.get(metadataBytes);  // get bytes out of byte buffer.
        return new JamModuleDefinition(moduleSystem,
                 null, null, metadataBytes, null, content, repository, moduleReleasable);
    }
}
