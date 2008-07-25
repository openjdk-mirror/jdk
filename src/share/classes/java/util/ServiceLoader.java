/*
 * Copyright 2005-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package java.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.module.ImportDependency;
import java.module.Module;
import java.module.ModuleDefinition;
import java.module.ModuleInitializationException;
import java.module.ModuleSystem;
import java.module.Query;
import java.module.Repository;
import java.module.Version;
import java.module.VersionConstraint;
import java.module.annotation.ServiceProvider;
import java.module.annotation.ServiceProviders;
import java.module.annotation.Services;
import java.net.URL;
import sun.module.core.ModuleUtils;
import sun.module.repository.RepositoryConfig;


/**
 * A simple service-provider loading facility.
 *
 * <p> A <i>service</i> is a well-known set of interfaces and (usually
 * abstract) classes.  A <i>service provider</i> is a specific implementation
 * of a service.  The classes in a provider typically implement the interfaces
 * and subclass the classes defined in the service itself.  Service providers
 * can be installed in an implementation of the Java platform in the form of
 * extensions, that is, jar files placed into any of the usual extension
 * directories.  <span style="color: rgb(204, 0, 0);">
 * In addition, service providers can be installed in an
 * implementation of the Java platform in the form of
 * <a href="../../java/module/package-summary.html">JAM modules</a>
 * in the repositories.</span> Providers can also be made available by adding them
 * to the application's class path, or by some other platform-specific means.
 *
 * <p> For the purpose of loading, a service is represented by a single type,
 * that is, a single interface or abstract class.  (A concrete class can be
 * used, but this is not recommended.)  A provider of a given service contains
 * one or more concrete classes that extend this <i>service type</i> with data
 * and code specific to the provider.  The <i>provider class</i> is typically
 * not the entire provider itself but rather a proxy which contains enough
 * information to decide whether the provider is able to satisfy a particular
 * request together with code that can create the actual provider on demand.
 * The details of provider classes tend to be highly service-specific; no
 * single class or interface could possibly unify them, so no such type is
 * defined here.  The only requirement enforced by this facility is that
 * provider classes must have a zero-argument constructor so that they can be
 * instantiated during loading.
 *
 * <span style="color: rgb(204, 0, 0);"><h4>Service providers and JAR files</h4></span>
 *
 * <p><a name="format"> A service provider is identified by placing a
 * <i>provider-configuration file</i> in the resource directory
 * <tt>META-INF/services</tt>.  The file's name is the fully-qualified <a
 * href="../lang/ClassLoader.html#name">binary name</a> of the service's type.
 * The file contains a list of fully-qualified binary names of concrete
 * provider classes, one per line.  Space and tab characters surrounding each
 * name, as well as blank lines, are ignored.  The comment character is
 * <tt>'#'</tt> (<tt>'&#92;u0023'</tt>, <font size="-1">NUMBER SIGN</font>); on
 * each line all characters following the first comment character are ignored.
 * The file must be encoded in UTF-8.
 *
 * <p> If a particular concrete provider class is named in more than one
 * configuration file, or is named in the same configuration file more than
 * once, then the duplicates are ignored.  The configuration file naming a
 * particular provider need not be in the same jar file or other distribution
 * unit as the provider itself.  The provider must be accessible from the same
 * class loader that was initially queried to locate the configuration file;
 * note that this is not necessarily the class loader from which the file was
 * actually loaded.
 *
 * <span style="color: rgb(204, 0, 0);"><h4>Service providers and JAM modules</h4>
 *
 * A <i>service module</i> is a JAM module which contains one or more
 * service's types. A service is identified by placing a
 * {@code ModuleServiceTable} attribute in the metadata of the service module.
 * The attribute contains a list of fully-qualified
 * <a href="../lang/ClassLoader.html#name">binary name</a> of the
 * service's types. If a particular service is named
 * in the attribute more than once, then the duplicates are ignored.
 * <p>
 * A <i>service provider module</i> is a JAM module which contains one or more
 * provider classes for one or more services. A service provider module may
 * have more than one provider classes for the same service. A service
 * provider module may have the same provider class for more than one service.
 * A service provider is identified by placing a
 * {@code ModuleServiceProviderTable} attribute in the metadata
 * of the service provider module. The attribute contains a list of
 * fully-qualified binary name of the service's types and concrete
 * provider classes. If a particular pair of service and concrete provider
 * class is named in the attribute more than once, then the duplicates
 * are ignored.
 * <p>
 * A service module which contains provider classes as the default
 * implementations for its services is also a service provider module.
 * A service provider module must import a service module for the service
 * types either directly or transitively (through module re-exports),
 * except if the service provider module is also a service module.
 * <p>
 * For more information about the {@code ModuleServiceTable}
 * and {@code ModuleServiceProviderTable} attributes, see the
 * {@link java.lang.ModuleInfo ModuleInfo} API.
 * </span>
 *
 * <pre>
 * </pre>
 * <p> Providers are located and instantiated lazily, that is, on demand.  A
 * service loader maintains a cache of the providers that have been loaded so
 * far.  Each invocation of the {@link #iterator iterator} method returns an
 * iterator that first yields all of the elements of the cache, in
 * instantiation order, and then lazily locates and instantiates any remaining
 * providers, adding each one to the cache in turn.  The cache can be cleared
 * via the {@link #reload reload} method.
 *
 * <p> Service loaders always execute in the security context of the caller.
 * Trusted system code should typically invoke the methods in this class, and
 * the methods of the iterators which they return, from within a privileged
 * security context.
 *
 * <p> Instances of this class are not safe for use by multiple concurrent
 * threads.
 *
 * <p> Unless otherwise specified, passing a <tt>null</tt> argument to any
 * method in this class will cause a {@link NullPointerException} to be thrown.
 *
 * <p><span style="font-weight: bold; padding-right: 1em">Example</span>
 * Suppose we have a service type <tt>com.example.CodecSet</tt> which is
 * intended to represent sets of encoder/decoder pairs for some protocol.  In
 * this case it is an abstract class with two abstract methods:
 *
 * <blockquote><pre>
 * public abstract Encoder getEncoder(String encodingName);
 * public abstract Decoder getDecoder(String encodingName);</pre></blockquote>
 *
 * Each method returns an appropriate object or <tt>null</tt> if the provider
 * does not support the given encoding.  Typical providers support more than
 * one encoding.
 *
 * <p> If <tt>com.example.impl.StandardCodecs</tt> is an implementation of the
 * <tt>CodecSet</tt> service then its jar file also contains a file named
 *
 * <blockquote><pre>
 * META-INF/services/com.example.CodecSet</pre></blockquote>
 *
 * <p> This file contains the single line:
 *
 * <blockquote><pre>
 * com.example.impl.StandardCodecs    # Standard codecs</pre></blockquote>
 *
 * <p> The <tt>CodecSet</tt> class creates and saves a single service instance
 * at initialization:
 *
 * <blockquote><pre>
 * private static ServiceLoader&lt;CodecSet&gt; codecSetLoader
 *     = ServiceLoader.load(CodecSet.class);</pre></blockquote>
 *
 * <p> To locate an encoder for a given encoding name it defines a static
 * factory method which iterates through the known and available providers,
 * returning only when it has located a suitable encoder or has run out of
 * providers.
 *
 * <blockquote><pre>
 * public static Encoder getEncoder(String encodingName) {
 *     for (CodecSet cp : codecSetLoader) {
 *         Encoder enc = cp.getEncoder(encodingName);
 *         if (enc != null)
 *             return enc;
 *     }
 *     return null;
 * }</pre></blockquote>
 *
 * <p> A <tt>getDecoder</tt> method is defined similarly.
 *
 *
 * <p><span style="font-weight: bold; padding-right: 1em">Usage Note</span> If
 * the class path of a class loader that is used for provider loading includes
 * remote network URLs then those URLs will be dereferenced in the process of
 * searching for provider-configuration files.
 *
 * <p> This activity is normal, although it may cause puzzling entries to be
 * created in web-server logs.  If a web server is not configured correctly,
 * however, then this activity may cause the provider-loading algorithm to fail
 * spuriously.
 *
 * <p> A web server should return an HTTP 404 (Not Found) response when a
 * requested resource does not exist.  Sometimes, however, web servers are
 * erroneously configured to return an HTTP 200 (OK) response along with a
 * helpful HTML error page in such cases.  This will cause a {@link
 * ServiceConfigurationError} to be thrown when this class attempts to parse
 * the HTML page as a provider-configuration file.  The best solution to this
 * problem is to fix the misconfigured web server to return the correct
 * response code (HTTP 404) along with the HTML error page.
 *
 * @param  <S>
 *         The type of the service to be loaded by this loader
 *
 * @author Mark Reinhold
 * @since 1.6
 */

public final class ServiceLoader<S>
    implements Iterable<S>
{

    private static final String PREFIX = "META-INF/services/";


    // For locating service modules
    private static final Query serviceQuery =
        Query.annotation(Services.class);

    // For locating service-provider modules
    private static final Query serviceProviderQuery =
        Query.annotation(ServiceProviders.class);

    // The class or interface representing the service being loaded
    private final Class<S> service;

    // The class loader used to locate, load, and instantiate providers.  Used
    // only when no repository is available for loading providers.
    private final ClassLoader loader;

    // Cached providers, in instantiation order
    private final LinkedHashMap<String,S> providers = new LinkedHashMap<String,S>();

    // Module containing the service
    private final Module serviceMod;

    // The repository in which ModuleDefinitions are found.
    private final Repository repository;

    // The current service provider lookup iterator
    private List<ServiceLoaderIterator> lookupIterators;

    // True if iteration is over providers from a Repository and then a ClassLoader
    private final boolean isCompound;


    /**
     * Clear this loader's provider cache so that all providers will be
     * reloaded.
     *
     * <p> After invoking this method, subsequent invocations of the {@link
     * #iterator() iterator} method will lazily look up and instantiate
     * providers from scratch, just as is done by a newly-created loader.
     *
     * <p> This method is intended for use in situations in which new providers
     * can be installed into a running Java virtual machine.
     */
    public void reload() {
        providers.clear();
        lookupIterators = new ArrayList<ServiceLoaderIterator>();
        if (repository == null) {
            lookupIterators.add(new LazyIterator(service, loader));
        } else {
            lookupIterators.add(new ModulesIterator(service, serviceMod));
            if (isCompound) {
                lookupIterators.add(new LazyIterator(service, loader));
            }
        }
    }

    private ServiceLoader(Class<S> svc, ClassLoader cl) {
        service = svc;
        loader = cl;
        isCompound = false;

        // If the both service and loader are from modules, use the loader's
        // repository for provider lookups.
        ClassLoader svcClassLoader = service.getClassLoader();
        if (svcClassLoader != null && svcClassLoader.getModule() != null
            && loader != null && loader.getModule() != null) {
            repository =
                loader.getModule().getModuleDefinition().getRepository();
            serviceMod = svcClassLoader.getModule();
        } else {
            repository = null;
            serviceMod = null;
        }
        reload();
    }

    private ServiceLoader(Class<S> svc, Repository repo) {
        this(svc, repo, null, svc.getClassLoader().getModule(), false);
    }

    private ServiceLoader(Class<S> svc, Repository repo, Module mod) {
        this(svc, repo, null, mod, false);
    }

    private ServiceLoader(Class<S> svc, Repository repo, ClassLoader cl,
                          Module mod) {
        this(svc, repo, cl, mod, true);
    }

    private ServiceLoader(Class<S> svc, Repository repo, ClassLoader cl,
                          Module mod, boolean isCompound) {
        this.service = svc;
        this.repository = repo;
        this.loader = cl;
        this.serviceMod = mod;
        this.isCompound = isCompound;
        reload();
    }

    private static void fail(Class service, String msg, Throwable cause)
        throws ServiceConfigurationError
    {
        throw new ServiceConfigurationError(service.getName() + ": " + msg,
                                            cause);
    }

    private static void fail(Class service, String msg)
        throws ServiceConfigurationError
    {
        throw new ServiceConfigurationError(service.getName() + ": " + msg);
    }

    private static void fail(Class service, URL u, int line, String msg)
        throws ServiceConfigurationError
    {
        fail(service, u + ":" + line + ": " + msg);
    }

    // Parse a single line from the given configuration file, adding the name
    // on the line to the names list.
    //
    private int parseLine(Class service, URL u, BufferedReader r, int lc,
                          List<String> names)
        throws IOException, ServiceConfigurationError
    {
        String ln = r.readLine();
        if (ln == null) {
            return -1;
        }
        int ci = ln.indexOf('#');
        if (ci >= 0) ln = ln.substring(0, ci);
        ln = ln.trim();
        int n = ln.length();
        if (n != 0) {
            if ((ln.indexOf(' ') >= 0) || (ln.indexOf('\t') >= 0))
                fail(service, u, lc, "Illegal configuration-file syntax");
            int cp = ln.codePointAt(0);
            if (!Character.isJavaIdentifierStart(cp))
                fail(service, u, lc, "Illegal provider-class name: " + ln);
            for (int i = Character.charCount(cp); i < n; i += Character.charCount(cp)) {
                cp = ln.codePointAt(i);
                if (!Character.isJavaIdentifierPart(cp) && (cp != '.'))
                    fail(service, u, lc, "Illegal provider-class name: " + ln);
            }
            if (!providers.containsKey(ln) && !names.contains(ln))
                names.add(ln);
        }
        return lc + 1;
    }

    // Parse the content of the given URL as a provider-configuration file.
    //
    // @param  service
    //         The service type for which providers are being sought;
    //         used to construct error detail strings
    //
    // @param  u
    //         The URL naming the configuration file to be parsed
    //
    // @return A (possibly empty) iterator that will yield the provider-class
    //         names in the given configuration file that are not yet members
    //         of the returned set
    //
    // @throws ServiceConfigurationError
    //         If an I/O error occurs while reading from the given URL, or
    //         if a configuration-file format error is detected
    //
    private Iterator<String> parse(Class service, URL u)
        throws ServiceConfigurationError
    {
        InputStream in = null;
        BufferedReader r = null;
        ArrayList<String> names = new ArrayList<String>();
        try {
            in = u.openStream();
            r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            int lc = 1;
            while ((lc = parseLine(service, u, r, lc, names)) >= 0);
        } catch (IOException x) {
            fail(service, "Error reading configuration file", x);
        } finally {
            try {
                if (r != null) r.close();
                if (in != null) in.close();
            } catch (IOException y) {
                fail(service, "Error closing configuration file", y);
            }
        }
        return names.iterator();
    }

    // Base class for iterators which access providers of services.
    //
    private abstract class ServiceLoaderIterator
        implements Iterator<S>
    {
        final Class<S> service;

        ServiceLoaderIterator(Class<S> service) {
            this.service = service;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // Private inner class implementing fully-lazy provider lookup from
    // a ClassLoader.
    //
    private class LazyIterator
        extends ServiceLoaderIterator
    {
        final ClassLoader loader;
        Enumeration<URL> configs = null;
        Iterator<String> pending = null;
        String nextName = null;

        private LazyIterator(Class<S> service, ClassLoader loader) {
            super(service);
            this.loader = loader;
        }

        public boolean hasNext() {
            if (nextName != null) {
                return true;
            }
            if (configs == null) {
                try {
                    String fullName = PREFIX + service.getName();
                    if (loader == null)
                        configs = ClassLoader.getSystemResources(fullName);
                    else
                        configs = loader.getResources(fullName);
                } catch (IOException x) {
                    fail(service, "Error locating configuration files", x);
                }
            }
            while ((pending == null) || !pending.hasNext()) {
                if (!configs.hasMoreElements()) {
                    return false;
                }
                pending = parse(service, configs.nextElement());
            }
            nextName = pending.next();
            return true;
        }

        public S next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String cn = nextName;
            nextName = null;
            try {
                S p = service.cast(Class.forName(cn, true, loader)
                                   .newInstance());
                providers.put(cn, p);
                return p;
            } catch (ClassNotFoundException x) {
                fail(service,
                     "Provider " + cn + " not found");
            } catch (Throwable x) {
                fail(service,
                     "Provider " + cn + " could not be instantiated: " + x,
                     x);
            }
            throw new Error();          // This cannot happen
        }
    }

    // Private inner class implementing provider lookup from Modules
    //
    private class ModulesIterator
        extends ServiceLoaderIterator
    {
        private final Module serviceMod;

        // Potential candidates are still screened for compatibility.
        private Iterator<ModuleProvider> candidates = null;

        // The next ModuleProvider from which a service will be returned.
        private ModuleProvider nextMP = null;

        private ModulesIterator(Class<S> service, Module serviceMod) {
            super(service);
            this.serviceMod = serviceMod;
        }

        // In addition to usual hasNext - like behavior, sets nextMP to the
        // next ModuleProvider from which a service provider is to be returned
        // from next().
        public boolean hasNext() {
            if (candidates == null) {
                candidates = getCandidates().iterator();
            }
            if (nextMP != null) { // Was set in prior call to hasNext
                return true;
            } else {
                while (candidates.hasNext()) {
                    nextMP = candidates.next();
                    if (isCompatible(nextMP)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public S next() {
            S rc = null;

            // nextMP is null if hasNext() has not been called.
            if (nextMP == null) {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
            }

            String cn = nextMP.getName();
            try {
                Object o = null;
                Class<?> nextMPClass = nextMP.getProviderClass();
                o = nextMPClass.newInstance();
                rc = service.cast(o);
                providers.put(cn, rc);
            } catch (Throwable x) {
                fail(service,
                     "Provider " + cn + " could not be instantiated: " + x,
                     x);
            }
            nextMP = null;
            return rc;
        }

        /**
         * Gets the candidates provider modules for a service.  A candidate
         * service provider module might not actually be acceptable; see next().
         * <pre>
         * Let S be the requested service
         * Let SM be the service module to which S belongs
         * Let SMD be the service module definition for SM
         *
         * For each module definition MD in the repository which has ServiceProviders
         *   For each service provider P of MD
         *     If P is a provider for S
         *       Let M be the module instance for MD
         *
         *       If SMD's name matches MD's name
         *         P is a default provider
         *         If SM is the same as M
         *           Add P as a default provider for SM
         *         Continue to the next service provider
         *
         *       If M imports SM (directly or transitively via reexports)
         *         Remove candidates with same name as M's but lower version
         *         Add M as a candidate
         * Sort the list of candidates according to name
         * Add all default providers to the head of the list of candidates
         * Return the list of candidates
         *
         * Mapping between above terms and code below:
         * S:   service
         * SM:  serviceMod
         * SMD: serviceModDef
         * MD:  providerModDef
         * M:   providerMod (see use in ModuleChecker)
         * P:   p (rougly; code based on ServiceProvider annotation)
         * </pre>
         * @return a list of {@code ModuleProvider} instances for the service
         */
        private List<ModuleProvider> getCandidates() {
            List<ModuleProvider> rc = new ArrayList<ModuleProvider>();
            List<ModuleProvider> defaultProviders = new ArrayList<ModuleProvider>();
            if (serviceMod == null) {
                return rc;
            }
            ModuleDefinition serviceModDef = serviceMod.getModuleDefinition();

            // Keeps track of candiates per provider name.
            Map<String, ModuleProvider> candidates = new HashMap<String, ModuleProvider>();

            for (ModuleDefinition providerModDef : repository.find(serviceProviderQuery)) {
                ServiceProviders sp = providerModDef.getAnnotation(ServiceProviders.class);
                ModuleChecker checker = new ModuleChecker(serviceMod, providerModDef);

                for (ServiceProvider p : sp.value()) {
                    if (!p.service().equals(service.getName())) {
                        continue;
                    }

                    // If providerModDef's name matches serviceModDef's name,
                    // providerMod is a default provider.
                    String providerName = providerModDef.getName();
                    if (serviceModDef.getName().equals(providerName)) {
                        // The provider is also a service module
                        if (serviceModDef.getVersion().equals(providerModDef.getVersion())) {
                            // If names and versions are the same, we still
                            // need to check the identity
                            if (checker.checkEquals()) {
                                defaultProviders.add(new ModuleProvider(providerModDef, p.providerClass()));
                            }
                        } else {
                            // If they are not the same versions, then the
                            // provider is not suitable for this version of
                            // the service, as it is a default for another
                            // version.
                        }
                        continue;
                    }

                    // To be a candidate, a provider module must import the service module
                    if (!checker.checkImports()) {
                        continue;
                    }

                    // For provider-modules with a given name, keep only the
                    // highest version.
                    ModuleProvider candidate = candidates.get(providerName);
                    if (candidate == null) {
                        candidates.put(providerName,
                                       new ModuleProvider(
                                           providerModDef, p.providerClass()));
                    } else if (providerModDef.getVersion().compareTo(candidate.getVersion()) > 0) {
                        candidates.put(providerName,
                                       new ModuleProvider(
                                           providerModDef, p.providerClass()));
                    }
                }
            }
            rc.addAll(candidates.values());
            Collections.sort(rc, ModuleProvider.comparator);
            rc.addAll(0, defaultProviders);
            return rc;
        }

        /**
         * A candidate provider is compatible if
         * (a) one of the interfaces it implements is the same as that
         *     of the given service, or
         * (b) one of its superclasses is exactly the same as the
         *     given service.
         * @return true if {@code mp} designates a compatible provider
         */
        private boolean isCompatible(ModuleProvider mp) {
            String cn = mp.getName();
            try {
                Class<?> mpClass = mp.getProviderClass();

                Class<?>[] interfaces = mpClass.getInterfaces();
                if (Arrays.asList(interfaces).contains(service)) {
                    return true;
                }

                Class sup = mpClass.getSuperclass();
                while (sup != null) {
                    if (sup == service) {
                        return true;
                    } else {
                        sup = sup.getSuperclass();
                    }
                }
                return false;

            } catch (ClassNotFoundException x) {
                fail(service,
                     "Provider " + cn + " not found");
            } catch (Throwable x) {
                fail(service,
                     "Provider " + cn + " could not be instantiated: " + x,
                     x);
            }
            return false;
        }
    }

    /**
     * Represents a way to compare two modules, one of which is obtained from
     * its given definition.  If that attempt fails, a subsequent attempt is
     * not repeated.  If it succeeds, it is used in all examinations.
     */
    private static class ModuleChecker {
        private final Module serviceMod;
        private final ModuleDefinition providerModDef;

        private Module providerMod;
        private boolean accessAttempted = false;

        ModuleChecker(Module serviceMod, ModuleDefinition providerModDef) {
            this.serviceMod = serviceMod;
            this.providerModDef = providerModDef;
        }

        /** @return true if serviceMod == the provider's module */
        boolean checkEquals() {
            getProviderModule();
            return providerMod == null ? false : serviceMod == providerMod;
        }

        /**
         * @return true if the provider module imports the service module,
         * directly or via reexports of imported modules (transitively).
         */
        boolean checkImports() {
            getProviderModule();
            boolean rc = false;
            if (providerMod != null) {
                List<Module> reexports = new ArrayList<Module>();
                ModuleUtils.expandReexports(providerMod, reexports, true);
                rc = reexports.contains(serviceMod);
            }
            return rc;
        }

        // Make only one attempt at getting the provider's module
        private void getProviderModule() {
            if (!accessAttempted) {
                try {
                    providerMod = providerModDef.getModuleInstance();
                } catch (ModuleInitializationException ex) {
                    // ignore
                } finally {
                    accessAttempted = true;
                }
            }
        }
    }

    /**
     * Represents a service that can be provided from a module
     */
    private static class ModuleProvider {
        private final ModuleDefinition modDef;
        private final String name;

        // Comparison is based on name only.
        private static final Comparator<ModuleProvider> comparator =
            new Comparator<ModuleProvider>() {
            public int compare(ModuleProvider m1, ModuleProvider m2) {
                return m1.modDef.getName().compareTo(m2.modDef.getName());
            }

            @Override
            public boolean equals(Object o) {
                return this == o;
            }
        };

        ModuleProvider(ModuleDefinition modDef, String name) {
            this.modDef = modDef;
            this.name = name;
        }

        String getName() {
            return name;
        }

        Version getVersion() {
            return modDef.getVersion();
        }

        ModuleDefinition getModuleDefinition() {
            return modDef;
        }

        Class<?> getProviderClass() throws ClassNotFoundException, ModuleInitializationException {
            final Module m = modDef.getModuleInstance();
            ClassLoader cl = m.getClassLoader();
            return Class.forName(name, true, cl);
        }
    }

    /**
     * Lazily loads the available providers of this loader's service.
     *
     * <p> The iterator returned by this method first yields all of the
     * elements of the provider cache, in instantiation order.  It then lazily
     * loads and instantiates any remaining providers, adding each one to the
     * cache in turn.
     *
     * <p> To achieve laziness the actual work of parsing the available
     * provider-configuration files and instantiating providers must be done by
     * the iterator itself.  Its {@link java.util.Iterator#hasNext hasNext} and
     * {@link java.util.Iterator#next next} methods can therefore throw a
     * {@link ServiceConfigurationError} if a provider-configuration file
     * violates the specified format, or if it names a provider class that
     * cannot be found and instantiated, or if the result of instantiating the
     * class is not assignable to the service type, or if any other kind of
     * exception or error is thrown as the next provider is located and
     * instantiated.  To write robust code it is only necessary to catch {@link
     * ServiceConfigurationError} when using a service iterator.
     *
     * <p> If such an error is thrown then subsequent invocations of the
     * iterator will make a best effort to locate and instantiate the next
     * available provider, but in general such recovery cannot be guaranteed.
     *
     * <blockquote style="font-size: smaller; line-height: 1.2"><span
     * style="padding-right: 1em; font-weight: bold">Design Note</span>
     * Throwing an error in these cases may seem extreme.  The rationale for
     * this behavior is that a malformed provider-configuration file, like a
     * malformed class file, indicates a serious problem with the way the Java
     * virtual machine is configured or is being used.  As such it is
     * preferable to throw an error rather than try to recover or, even worse,
     * fail silently.</blockquote>
     *
     * <p> The iterator returned by this method does not support removal.
     * Invoking its {@link java.util.Iterator#remove() remove} method will
     * cause an {@link UnsupportedOperationException} to be thrown.
     *
     * @return  An iterator that lazily loads providers for this loader's
     *          service
     */
    public Iterator<S> iterator() {
        return new Iterator<S>() {

            Iterator<Map.Entry<String,S>> knownProviders
                = providers.entrySet().iterator();

            Iterator<ServiceLoaderIterator> lookups = lookupIterators.iterator();
            ServiceLoaderIterator currentLookup = null;

            Error lastError = null;

            public boolean hasNext() {
                if (knownProviders.hasNext())
                    return true;

                initLookups();
                boolean rc = false;

                if (currentLookup.hasNext()) {
                    rc = true;
                } else {
                    while (lookups.hasNext()) {
                        currentLookup = lookups.next();
                        if (currentLookup.hasNext()) {
                            rc = true;
                            break;
                        }
                    }
                }
                return rc;
            }

            public S next() {
                if (knownProviders.hasNext())
                    return knownProviders.next().getValue();

                initLookups();
                S rc = null;

                if (currentLookup.hasNext()) {
                    try {
                        rc = currentLookup.next();
                    } catch (ServiceConfigurationError e) {
                        // XXX This could be a real error, or it could be that
                        // a virtual module does not export this service.
                        lastError = e;
                    }
                }

                if (rc == null) {
                    while (lookups.hasNext()) {
                        currentLookup = lookups.next();
                        if (currentLookup.hasNext()) {
                            try {
                                rc = currentLookup.next();
                                break;
                            } catch (ServiceConfigurationError e) {
                                // XXX This could be a real error, or it could be that
                                // a virtual module does not export this
                                // service.
                                lastError = e;
                            }
                        }
                    }
                }

                if (rc == null) {
                    if (lastError != null) {
                        throw lastError;
                    } else {
                        throw new NoSuchElementException();
                    }
                }
                return rc;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            private void initLookups() {
                if (currentLookup == null && lookups.hasNext()) {
                    lastError = null;
                    currentLookup = lookups.next();
                }
            }
        };
    }

    /**
     * Tries to find a service module in the bootstrap repository for the
     * given {@code service}.
     *
     * @return a ModuleDefinition for {@code service} if one exists in the
     * bootstrap repository, else null.
     */
    private static <S> Module findBootstrapModule(Class<S> service) {
        Module rc = null;
        String serviceName = service.getName();

        for (ModuleDefinition md : Repository.getBootstrapRepository().find(serviceQuery)) {
            Services ss = md.getAnnotation(Services.class);
            for (String sName : ss.value()) {
                if (serviceName.equals(sName)) {
                    try {
                        rc = md.getModuleInstance();
                        break;
                    } catch (ModuleInitializationException ex) {
                        // ignore
                    }
                }
            }
        }
        return rc;
    }

    /**
     * <span style="color: rgb(204, 0, 0);"><B>[UPDATED]</B></span>
     * Creates a new service loader for the given service type and class
     * loader.
     * <p>
     * <span style="color: rgb(204, 0, 0);">
     * The service loader first uses the class loader to search for providers
     * for the given service type; the service loader then uses the following
     * repository in the Java Module System to search for service provider
     * modules for the given service type if the service type is from a
     * module module:
     * <p>
     * <ul>
     *      <li> {@linkplain java.module.Repository#getApplicationRepository Application repository}
     *           if the class loader or an ancestor of the class loader is the system class loader.</li>
     *
     *      <li> {@linkplain java.module.Repository#getSystemRepository System repository}
     *           if the class loader or an ancestor of the class loader is the extension class loader.</li>
     *
     *      <li> The repository associated with the module which the class loader
     *           belongs if the class loader is a
     *           {@linkplain java.module.Module#getClassLoader module class loader}.</li>
     * </ul>
     * </span>
     *
     * @param  service
     *         The interface or abstract class representing the service
     *
     * @param loader
     *         The class loader used to determine the location from which
     *         provider-configuration files and provider classes are loaded.
     *         If <tt>null</tt> the system class loader (or, failing that, the
     *         bootstrap class loader) is used.
     *
     * @return A new service loader
     */
    public static <S> ServiceLoader<S> load(Class<S> service,
                                            ClassLoader loader)
    {
        ClassLoader cl = service.getClassLoader();
        if (cl == null) {
            Module m = findBootstrapModule(service);
            if (m != null) {
                if (loader == null) {
                    // Use bootstrap repo instead of bootstrap loader
                    return new ServiceLoader<S>(
                        service, Repository.getBootstrapRepository(), m);

                } else if (loader == ClassLoader.getSystemClassLoader()) {
                    // Use application repository and system class loader
                    return new ServiceLoader<S>(
                        service, Repository.getApplicationRepository(), loader, m);

                } else if (loader.getModule() != null) {
                    // Use loader's respository
                    return new ServiceLoader<S>(
                        service, loader.getModule().getModuleDefinition().getRepository(), m);
                }
            }
        }
        // Fall back to pre-modules behavior
        return new ServiceLoader<S>(service, loader);
    }

    /**
     * Creates a new service loader for the given service type, using the
     * current thread's {@linkplain java.lang.Thread#getContextClassLoader
     * context class loader}.
     *
     * <p> An invocation of this convenience method of the form
     *
     * <blockquote><pre>
     * ServiceLoader.load(<i>service</i>)</pre></blockquote>
     *
     * is equivalent to
     *
     * <blockquote><pre>
     * ServiceLoader.load(<i>service</i>,
     *                    Thread.currentThread().getContextClassLoader())</pre></blockquote>
     *
     * @param  service
     *         The interface or abstract class representing the service
     *
     * @return A new service loader
     */
    public static <S> ServiceLoader<S> load(Class<S> service) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return ServiceLoader.load(service, cl);
    }

    /**
     * <span style="color: rgb(204, 0, 0);"><B>[NEW]</B></span>
     * Creates a new service loader for the given service type, using the
     * given {@link java.module.Repository}.
     * <p>
     * This method uses the repository to search for service provider modules
     * for the given service type; the service provider module must
     * import (either directly or transitively through module reexports)
     * the same instance of the service module which the given service
     * type belongs, unless the provider is a default provider
     * in the same service module instance.
     *
     * @param  repository
     *         The repository from which service provider modules will be
     *         obtained
     *
     * @param  service
     *         The interface or abstract class representing the service
     *
     * @return A new service loader
     *
     * @throws IllegalArgumentException
     *         if the {@code service} is not in a service module
     *
     * @since 1.7
     */
    public static <S> ServiceLoader<S> load(Repository repository,
                                            Class<S> service)
    {
        if (repository == null) {
            throw new NullPointerException(
                "repository cannot be null");
        }
        ClassLoader cl = service.getClassLoader();
        if (cl == null) {
            Module m = findBootstrapModule(service);
            if (m != null) {
                return new ServiceLoader<S>(service, repository, m);
            } else {
                throw new IllegalArgumentException(
                    "service is not in a service module");
            }
        } else if (cl.getModule() == null) {
            throw new IllegalArgumentException(
                "service is not in a service module");
        }
        return new ServiceLoader<S>(service, repository);
    }

    /**
     * <span style="color: rgb(204, 0, 0);"><B>[UPDATED]</B></span>
     * Creates a new service loader for the given service type, using the
     * <span style="color: rgb(204, 0, 0);">
     * the extension class loader, and using the
     * {@linkplain java.module.Repository#getSystemRepository system repository}
     * if the service type is from a service module.
     * </span>
     *
     * <p> This convenience method simply locates the extension class loader,
     * call it <tt><i>extClassLoader</i></tt>, and then returns
     *
     * <blockquote><pre>
     * ServiceLoader.load(<i>service</i>, <i>extClassLoader</i>)</pre></blockquote>
     *
     * <p> If the extension class loader cannot be found then the system class
     * loader is used; if there is no system class loader then the bootstrap
     * class loader is used.
     *
     * <p> This method is intended for use when only installed providers are
     * desired.  The resulting service will only find and load providers that
     * have been installed into the current Java virtual machine; providers on
     * the application's class path
     * <span style="color: rgb(204, 0, 0);">
     * and providers in the application repository will be ignored.
     * </span>
     *
     * @param  service
     *         The interface or abstract class representing the service
     *
     * @return A new service loader
     */
    public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
        ClassLoader cl = service.getClassLoader();
        if (cl != null && cl.getModule() != null) {
            return ServiceLoader.load(
                Repository.getSystemRepository(), service);
        } else {
            // If the class is in the bootstrap module, it might represent a
            // service defined in that module.
            Module m = findBootstrapModule(service);
            if (m != null) {
                return new ServiceLoader<S>(service, Repository.getBootstrapRepository(), m);
            } else {
                // Fall back to pre-modules behavior
                cl = ClassLoader.getSystemClassLoader();
                ClassLoader prev = null;
                while (cl != null) {
                    prev = cl;
                    cl = cl.getParent();
                }
                return ServiceLoader.load(service, prev);
            }
        }
    }

    /**
     * Returns a string describing this service.
     *
     * @return  A descriptive string
     */
    public String toString() {
        return "java.util.ServiceLoader[" + service.getName() + "]";
    }
}
