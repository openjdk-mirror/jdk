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

package sun.module.repository;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.module.Modules;
import java.module.ModuleSystemPermission;
import java.module.Repository;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import sun.module.JamUtils;
import sun.module.config.ModuleSystemConfig;
import sun.security.util.PropertyExpander;

/**
 * Establishes the configuration of a set of repositories in a running JVM.  A
 * configuration specifies a list of repositories, one of which is a child of
 * the bootstrap repository and the others are successive children.
 * <p>
 * Repositories can be configured automatically via configuration files, or by
 * setting a system property to the name of a configuration file.
 * <p>
 * The initial call to {@code getSystemRepository} causes repositories to be
 * configured via a configuration file, with the system repository set to the
 * configured repository that is most distant (in the parent-child distance of
 * repository instances).
 * @since 1.7
 */
public final class RepositoryConfig
{

    static {
        // Setup "repository.system.home" system property if it doesn't exist
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                if (System.getProperty("repository.system.home") == null) {
                    String platform = System.getProperty("os.platform");
                    String repositorySystemHome = null;
                    if (platform.equalsIgnoreCase("windows")) {
                        repositorySystemHome = "C:\\Windows\\Sun\\Java";
                    } else if (platform.equalsIgnoreCase("solaris")) {
                        repositorySystemHome = "/usr/java/packages";
                    } else if (platform.equalsIgnoreCase("linux")) {
                        repositorySystemHome = "/usr/jdk/packages";
                    }
                    if (repositorySystemHome != null) {
                        System.setProperty("repository.system.home", repositorySystemHome);
                    }
                }
                return null;
            }
        });
    }

    /** Application's system repository. */
    private static Repository systemRepository = Repository.getBootstrapRepository();

    /**
     * True if setSystemRepository ever completes normally: this allows for
     * it being changed at most once from its default.
     */
    private static boolean systemRepositoryWasSet = false;

    /** True once configRepositories has completed. */
    private static boolean configDone = false;

    /** Last repository in the configuration. */
    private static Repository lastRepository;

    /** Attribute which designates the parent of a repository. */
    private static final String parentAttr = "parent";

    /**Attribute which designates the source location of a repository. */
    private static final String sourceAttr = "source";

    /**
     * Optional attribute which designates the class name of a repository.
     * Note: only one of "classname" or "factoryname" can be used for
     * configuring a particular repository.
     */
    private static final String classAttr = "classname";

    /**
     * Optional attribute which designates the class name of a factory that
     * can create a repository. Note: only one of "classname" or "factoryname"
     * can be used for configuring a particular repository.
     */
    private static final String factoryAttr = "factoryname";

    /**
     * Value of the parentAttr which specifies the repository that is to be
     * created as the imediate child of the bootstrap repository.
     */
    private static final String bootstrapValue = "bootstrap";

    /** Cache of RepositoryFactory instances. */
    private static final Map<String, RepositoryFactory> factories =
        new HashMap<String, RepositoryFactory>();

    /**
     * Sets the system repository.
     * @param r {@code Repository} that will be the system repository
     * SecurityException if a security manager exists and its
     * <tt>checkPermission</tt> method denies access to shutdown the
     * repository.
     * @throws IllegalArgumentException if the system repository has already
     * been set via this method.
     * @throws SecurityException if a security manager exists and its
     * {@code checkPermission} method denies access to set the system
     * repository.
     */
    public static void setSystemRepository(Repository r) throws IllegalArgumentException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("setSystemRepository"));
        }
        if (systemRepositoryWasSet) {
            throw new IllegalArgumentException("System repository is already set.");
        } else {
            systemRepository = r;
            systemRepositoryWasSet = true;
        }
    }

    /**
     * Returns the current system repository, or if one has not been set (for
     * example, by the ModuleLauncher), creates it first.
     * @return the system repository
     */
    public static synchronized Repository getSystemRepository() {
        if (!configDone) {
            systemRepository = configRepositories();
        }
        return systemRepository;
    }

    /**
     * Returns the repository specified by the repository configuration which
     * has the largest number of repositories between itself and the bootstrap
     * repository.
     * @return the repository configured to be farthest from the bootstrap
     * repository.
     */
    public static synchronized Repository getLastRepository() {
        getSystemRepository(); // Force initialization
        return lastRepository;
    }

    /**
     * Creates repositories as described in the repository.properties file.
     * The format of  the property file contents is described in {@link
     * #configRepositories(Properties configProps)}.
     * @return The repository in the configuration that is furthest in the
     * list from the bootstrap repository
     * @throws IllegalStateException if configuration has already been done
     * @throws RuntimeException if repositories cannot be configured
     */
    private static Repository configRepositories() throws RuntimeException {
        if (configDone) {
            throw new IllegalStateException("Repositories have already been configured.");
        }

        String location = ModuleSystemConfig.getRepositoryPropertiesFileName();
        Properties rp = new Properties();
        try {
            File f = new File(PropertyExpander.expand(location));
            if (f.exists() && f.canRead()) {
                BufferedInputStream is = null;
                try {
                    is = new BufferedInputStream(new FileInputStream(f));
                    rp.load(is);
                } finally {
                    JamUtils.close(is);
                }
            } else {
                throw new RuntimeException(
                    "Cannot load repository properties from file " + f.getAbsolutePath());
            }
        } catch (PropertyExpander.ExpandException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return configRepositories(rp);
    }

    /**
     * Creates repositories as described in the given properties.  Each
     * property has the form:
     * <pre>
     * repositoryName . attribute = value
     * </pre>
     * For each named repository, there can be several attribute/value pairs.
     * One and only one named repository must have the attribute
     * "parent" with the value "bootstrap": this indicates which repository
     * is to be created as the immediate child of the bootstrap repository.
     * Each other named repository must have a parent, and these must describe
     * a list of repositories.
     * <p>
     * Each repository must provide a attribute "source" which
     * indicates the source location of the repository.
     * <p>
     * Each repository can optionally provide a "classname" attribute, with a
     * corresponding value that indicates the name of the Class of the
     * repository, or a "factoryname"  attribute, which indicates the name of
     * a Class that can create a Repository.
     * <p>
     * If classname is given, then an instance of that named class will be
     * created. The repository class must have a constructor which has a
     * signature like this:
     * <pre>
     * FooRepository(Repository parent, String name, URL source, Map&lt;String, String&gt; config) throws IOException;
     * </pre>
     * <p>
     * If factoryname is given, an instance of that will be created (but only
     * once per JVM) and its {@code create} method used to create a repository
     * instance.  See {@link RepositoryFactory}.
     * <p>
     * If neither classname nor factoryname is given, an appropriate
     * repository will be created based on the source.  If source
     * is a file-based URL or a filename, a LocalRepository will be created.
     * If it is a non-file-based URL, a URLRepository will be created.
     * <p>
     * Other attributes are allowed, and are particular to the class of
     * repository that is created.  They are passed to the repository's
     * constructor in it's {@code Map<String, String> config} parameter.
     * @param configProps Properties that configure a list of repositories
     * @return The repository in the configuration that is furthest in the
     * list from the bootstrap repository.
     * @throws IllegalStateException if repositories have already been
     * configured.
     */
    private static Repository configRepositories(Properties configProps) {
        if (configDone) {
            throw new IllegalStateException("Repositories have already been configured.");
        }

        if (!configProps.isEmpty()) {
            LinkedHashMap<String, Map<String, String>> orderedConfig =
                getConfigFromProps(configProps);
            lastRepository = createRepositories(orderedConfig);
            configDone = true;
        }
        return lastRepository;
    }

    /**
     * Examine the given properties to create a Map from repository name to
     * (repository property, repository value).  The returned Map is a {@code
     * LinkedHashMap} ordered such that the bootstrap repository is first.
     */
    private static LinkedHashMap<String, Map<String, String>> getConfigFromProps(
            Properties configProps) {
        Map<String, Map<String, String>> config
            = new HashMap<String, Map<String, String>>();
        boolean foundBootstrap = false;

        for (String name : configProps.stringPropertyNames()) {
            int dotPos = name.indexOf('.');
            String repoName = name.substring(0, dotPos);
            String repoKey = name.substring(dotPos +1);
            String repoValue = configProps.getProperty(name);
            try {
                repoValue = PropertyExpander.expand(repoValue);
            } catch (PropertyExpander.ExpandException ex) {
                throw new IllegalArgumentException(
                    "Invalid property value in repository configuration: '"
                    + repoValue + "'");
            }
            Map<String, String> repoConfig = config.get(repoName);
            if (repoConfig == null) {
                repoConfig = new HashMap<String, String>();
                config.put(repoName, repoConfig);
            }
            repoConfig.put(repoKey, repoValue);
            if (repoKey.equals(parentAttr) && repoValue.equals(bootstrapValue)) {
                foundBootstrap = true;
            }
        }

        if (!foundBootstrap) {
            throw new IllegalArgumentException(
                "RepositoryConfig: No repository specified as child of bootstrap repository");
        }

        /*
         * Sort config into orderedConfig, with bootstrap repository first,
         * then its child, then its grandchild, and so on.
         */
        LinkedHashMap<String, Map<String, String>> orderedConfig =
            new LinkedHashMap<String, Map<String, String>>();
        int size = config.size();
        String parentName = bootstrapValue;
        while (orderedConfig.size() < size) {
            boolean repoFound = false;
            for (String repoName : config.keySet()) {
                Map<String, String> repoConfig = config.get(repoName);
                String repoParent = repoConfig.get(parentAttr);

                if (repoParent == null) {
                    throw new RuntimeException(
                        "Invalid repository configuration: no parent specified by repository '"
                        + repoName + "'");
                }

                if (!repoParent.equals(bootstrapValue) && config.get(repoParent) == null) {
                    throw new RuntimeException(
                        "Invalid repository configuration: missing parent specified for repository '"
                        + repoName + "'");
                }

                if (parentName.equals(repoParent)) {
                    orderedConfig.put(repoName, repoConfig);
                    parentName = repoName;
                    repoFound = true;
                    break;
                }
            }
            if (!repoFound) {
                throw new RuntimeException(
                    "Invalid repository configuration: no child repository found for parent repository '"
                    + parentName + "'");
            }
        }
        return orderedConfig;
    }

    /**
     * RepositoryCreator provides for creating instances of Repository
     * subclasses.  Each of its subclasses provides a different way of
     * specifying how to create a Repository.
     */
    private abstract static class RepositoryCreator {
        protected abstract Repository create(Repository parent, String repoName,
                                   String sourceName, Map<String, String> config)
                throws IOException, InstantiationException, NoSuchMethodException,
                ClassNotFoundException, IllegalAccessException,
                InvocationTargetException, URISyntaxException;
    }

    /**
     * Creates a Local or URL repository, depending on whether the
     * source is a file or other URL .
     */
    private static class DefaultCreator extends RepositoryCreator {
        static final RepositoryCreator instance = new DefaultCreator();
        private DefaultCreator() { }

        protected Repository create(Repository parent, String repoName,
                          String sourceName, Map<String, String> config)
        throws IOException, InstantiationException, NoSuchMethodException,
                ClassNotFoundException, IllegalAccessException,
                InvocationTargetException {
            Repository rc = null;
            try {
                URL u = new URL(sourceName);
                if ("file".equals(u.getProtocol())) {
                    File f = new File(u.getFile()).getCanonicalFile();
                    rc = Modules.newLocalRepository(repoName, f, config, parent);
                } else {
                    rc = Modules.newURLRepository(repoName, u, config, parent);
                }
            } catch (MalformedURLException ex) {
                File f = new File(sourceName).getCanonicalFile();
                rc = Modules.newLocalRepository(repoName, f, config, parent);
            }
            return rc;
        }
    }

    /**
     * Creates a repository given the name of its class.
     */
    private static class ClassBasedCreator extends RepositoryCreator {
        static final ClassBasedCreator instance = new ClassBasedCreator();
        private ClassBasedCreator() { }

        /** Name of Repository subclass to create. */
        private String className;

        void setClassName(String name) {
            className = name;
        }

        protected Repository create(Repository parent, String repoName,
                          String sourceName, Map<String, String> config)
        throws IOException, InstantiationException, NoSuchMethodException,
                ClassNotFoundException, IllegalAccessException,
                InvocationTargetException, URISyntaxException {
            URI u = sourceName == null ? null : new URI(sourceName);
            Class<?> clazz = Class.forName(className);
            Constructor ctor = clazz.getDeclaredConstructor(
                String.class, URI.class, Map.class,
                Repository.class);
            return (Repository) ctor.newInstance(
                repoName, u, config, parent);
        }
    }

    /**
     * Creates a repository given the name of a factory that can create it.
     */
    private static class FactoryBasedCreator extends RepositoryCreator {
        static final FactoryBasedCreator instance = new FactoryBasedCreator();
        private FactoryBasedCreator() { }

        /** Name of RepositoryFactory class to create Repository subclass. */
        private String factoryName;

        void setFactoryName(String name) {
            factoryName = name;
        }

        protected synchronized Repository create(Repository parent, String repoName,
                          String sourceName, Map<String, String> config)
        throws IOException, InstantiationException, NoSuchMethodException,
                ClassNotFoundException, IllegalAccessException,
                InvocationTargetException, URISyntaxException {
            URL u = sourceName == null ? null : new URL(sourceName);
            RepositoryFactory rf  = factories.get(factoryName);
            if (rf == null) {
                Class<?> clazz = Class.forName(factoryName);
                Constructor ctor = clazz.getDeclaredConstructor();
                rf = (RepositoryFactory) ctor.newInstance();
                factories.put(factoryName, rf);
            }
            return rf.create(parent, repoName, u, config);
        }
    }

    /**
     * Creates repositories based on the given {@code LinkedHashMap}, whose
     * keys are presumed to be ordered such that the first entry describes the
     * repository to create as a child of the bootstrap repository, the next
     * to create as a child of that, and so on.
     * @return The repository in the configuration that is furthest in the
     * list from the bootstrap repository.
     */
    private synchronized static Repository createRepositories(
            LinkedHashMap<String, Map<String, String>> orderedConfig) {

        Repository parentRepo = Repository.getBootstrapRepository();
        for (String repoName : orderedConfig.keySet()) {
            Map<String, String> repoConfig = orderedConfig.get(repoName);

            String sourceName = repoConfig.get(sourceAttr);

            sourceName = sourceName.replace('\\', '/');

            Repository repo = null;

            String clazzName = repoConfig.get(classAttr);
            String factoryName = repoConfig.get(factoryAttr);
            if (clazzName != null && factoryName != null) {
                throw new IllegalArgumentException(
                    "Cannot specify both classname and factoryname for '" + repoName);
            }

            RepositoryCreator creator = DefaultCreator.instance;
            if (clazzName != null || factoryName != null) {
                if (clazzName != null) {
                    ClassBasedCreator.instance.setClassName(clazzName);
                    creator = ClassBasedCreator.instance;
                } else {
                    FactoryBasedCreator.instance.setFactoryName(factoryName);
                    creator = FactoryBasedCreator.instance;
                }
            }

            try {
                repo = creator.create(parentRepo, repoName, sourceName, repoConfig);
            } catch (Error ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw ex;
            } catch (InvocationTargetException ex) {
                String msg = ex.getMessage();
                Throwable t = ex.getCause();
                if (t != null) {
                    String s = t.getMessage();
                    if (s != null) {
                        msg += "\nCause: " + t.getMessage();
                    }
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    t.printStackTrace(pw);
                    pw.close();
                    msg += "\n" + sw.toString();
                }
                throw new IllegalArgumentException(
                    "Cannot create repository named '" + repoName + "' at location '" + sourceName
                    + "': " + msg);
            } catch (Exception ex) {
                throw new IllegalArgumentException(
                    "Cannot create repository named '" + repoName + "' at location '" + sourceName
                    + "': " + ex);
            }

            /*
             * If a repository was created, make it the parent for the next
             * one created (if any).  If a repository was *not* created, then
             * don't change the current parent.
             */
            if (repo != null) {
                parentRepo = repo;
            }
        }
        return parentRepo;
    }
}
