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

package sun.module;

import java.io.File;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.lang.reflect.*;

import java.module.*;
import java.module.annotation.MainClass;

import sun.module.*;
import sun.module.repository.RepositoryConfig;
import sun.module.core.ProxyModuleLoader;


/**
 * @since 1.7
 */

/*
 * This class is a singleton helper class for launching the Modules for
 * the java(1) and javaw(1) native launchers. This can be used as a standalone
 * driver for the Module launcher tests by using its own main. Note that several
 * methods are called from java.c using JNI.
 */
public final class ModuleLauncher {

    private static final String MODULE_OPT      = "-module";
    private static final String REPOSITORY_OPT  = "-repository";
    private static final String JAM_OPT         = "-jam";
    private static final String MODULEMAIN_OPT  = "-modulemain";
    private static Repository repository = null;
    private static boolean launcherDebug = false;

    static String moduleName       = null;
    static String repositoryName   = null;
    static String jamFileName      = null;
    static String moduleMain       = null;

    // A singleton
    private ModuleLauncher() {}

    /*
     * This is a JNI entry point to cleanup the
     * repositories we create, see java.c
     */
    private static void cleanUp() throws Exception {
        if (repository != null) {
            try {
                repository.shutdown();
            } catch (Exception e) {
                // Ignore the exception
            }
        }
    }

    /*
     * Load and get the main class, this is the main JNI entry point
     * any signature changes to this method will need a corresponding
     * adjustment to  java.c.
     */
    private static Class<?> loadModuleClass() {
        try {
            ModuleDefinition moddef = getModuleDefinition();
            String mainClassName;
            if (moduleMain != null) {
                mainClassName = moduleMain;
            } else {
                MainClass mainClass = moddef.getAnnotation(MainClass.class);
                mainClassName = (mainClass != null) ? mainClass.value() : null;
                if (mainClassName == null) {
                    throw new Exception("No Main-Class attribute in the module definition");
                }
            }
            Module m = moddef.getModuleInstance();
            ClassLoader loader = m.getClassLoader();
            ProxyModuleLoader.setPrimaryModuleLoader(loader);
            Class<?> clazz = loader.loadClass(mainClassName);
            return clazz;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (launcherDebug) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static void launchModule(String[] args) throws Exception {
        Class<?> clazz = loadModuleClass();
        if (clazz == null) {
            System.err.println("Main class not found");
            System.exit(1);
        }
        Method method = clazz.getMethod("main", String[].class);
        try {
            method.invoke(null, (Object)args);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanUp();
        }
    }

    /*
     * Will try to create a URL even if it is a local file,
     * if at all possible, and returns a null if it cannot.
     */
    private static URL getURL(String spec) {
        URL u = null;
        try {
            u = new URL(spec);
            return u;
        } catch (MalformedURLException mue) {
            // Try to construct a file URL.
            File f = new File(spec);
            if (f.exists()) {
                try {
                    return f.toURI().toURL();
                } catch (MalformedURLException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private static boolean isURL(String spec) {
        URL u = null;
        try {
            u = new URL(spec);
            return true;
        } catch (MalformedURLException mue) {
            //Ignore exception
        }
        return false;
    }

    /*
     * For debugging, dump out repositories
     */
    private static void dumpModules() {
        List<ModuleDefinition> mlist = repository.findAll();
        System.out.println("---repository @" + repository + "----");
        for (ModuleDefinition md : mlist) {
            System.out.println(md.getName());
            Set<String> attrs = md.getAttributeNames();
            for (String s: attrs) {
                System.out.println("\tAttribute:"+s);
            }
        }
    }

    /*
     * Method gets the ModuleDefinition from a JAM file, creating temporary
     * directories as needed, it also assumes that the other JAM files in the
     * basedirectory of the JAM are to be added to the temporary repository
     * created for the primary JAM.
     */
    private static ModuleDefinition getModuleDefinitionFromJam() throws Exception {
        URL u = getURL(jamFileName);
        if (u == null) {
            throw new IllegalArgumentException("file "
                + jamFileName + " not found");
        }
        if (!(new File(u.getPath())).exists()) {
            throw new IllegalArgumentException("file "
                + jamFileName + " not found");
        }

        // The directory which houses the jam becomes the system repository.
        File baseDir = new File(jamFileName).getAbsoluteFile().getParentFile();
        repository = Modules.newLocalRepository(
                RepositoryConfig.getSystemRepository(),
                "application",
                baseDir.getAbsoluteFile()
                );
        ModuleDefinition definition = null;
        // Get the basename of the jam file.
        String fname = new File(jamFileName).getName();
        List<ModuleArchiveInfo> ilist =  repository.list();
        for (ModuleArchiveInfo i : ilist) {
            File maiFile = new File(i.getFileName());
            if (maiFile.getName().equals(fname)) {
                definition = getModuleDefinition(repository, i.getName());
                break;
            }
        }
        if (definition == null) {
            throw new Exception("could not find module for " + jamFileName);
        }
        RepositoryConfig.setSystemRepository(repository);
        return definition;
    }

    private static ModuleDefinition getModuleDefinition() throws Exception {
        // Jam file processing
        if (jamFileName != null) {
            if (moduleName != null || repositoryName != null) {
                throw new IllegalArgumentException(MODULE_OPT +
                    " or " + REPOSITORY_OPT +
                    " cannot be used with " + JAM_OPT);
            }
            if (!jamFileName.endsWith(".jam")
                    && !jamFileName.endsWith(".jar")
                    && !jamFileName.endsWith(".jam.pack.gz")) {
                throw new IllegalArgumentException("jam filename must have a .jam, .jam.pack.gz or .jar extension");
            }
            return getModuleDefinitionFromJam();
        }

        // Repository processing
        if (repositoryName == null) {
            // Assume the current directory as the system repository.
            repository = Modules.newLocalRepository(
                    RepositoryConfig.getSystemRepository(),
                    "application",
                    new File(".").getAbsoluteFile());
        } else {
            if (moduleName == null) {
                throw new IllegalArgumentException(MODULE_OPT +
                    " : a module name is expected");
            }
            URL u = getURL(repositoryName);
            if (u == null) {
                throw new IllegalArgumentException("could not find repository "
                    + repositoryName);
            }
            if (isURL(repositoryName)) {
                if (u.getProtocol().equals("file")) {
                    File repFile = JamUtils.getFile(u);
                    if (!repFile.exists()) {
                        throw new IllegalArgumentException("Repository not found at "
                           + repFile.toString());
                    }
                }
                repository = Modules.newURLRepository(
                        RepositoryConfig.getSystemRepository(),
                        "application", u);
            } else {
                repository = Modules.newLocalRepository(
                        RepositoryConfig.getSystemRepository(),
                        "application", new File(u.toURI()));
            }
        }
        RepositoryConfig.setSystemRepository(repository);
        ModuleDefinition definition = getModuleDefinition(repository, moduleName);
        if (definition == null) {
            throw new IllegalArgumentException("Module not found: " + moduleName);
        }
        return definition;
    }

    private static ModuleDefinition getModuleDefinition(Repository repository,
                                                        String moduleName) {
        VersionConstraint cs = VersionConstraint.DEFAULT;
        String mname = moduleName;
        int cidx = moduleName.indexOf(':');
        if (cidx > 0) {
            mname = moduleName.substring(0,cidx);
            String version = moduleName.substring(cidx + 1, moduleName.length());
            // TODO:may need to present VersionException in a friendly form ?
            cs = VersionConstraint.valueOf(version);
        }
        return repository.find(mname, cs);
    }


    /*
     * Convenience methods to aid JNI calling into the launcher
     * see java.c.
     */
    public static void setRepository(String s) { repositoryName = s; }
    public static void setModule(String s) { moduleName = s; }
    public static void setModuleMain(String s) { moduleMain = s; }
    public static void setJamFile(String s) { jamFileName = s; }
    public static void setDebug(boolean v) { launcherDebug = v; }

    /*
     * A simple test driver.
     */
    public static void main(String[] args) throws Exception {
        launcherDebug = true; // print all the exceptions etc.
        if (args.length < 1) doUsage();
        ArrayList<String> argList = new ArrayList<String>();
        for (int i = 0 ; i < args.length; i++) {
            if ( args[i].charAt(0) == '-') {
                if (args[i].equals(MODULE_OPT)) {
                    setModule(args[++i]);
                    continue;
                }
                if (args[i].equals(REPOSITORY_OPT)) {
                    setRepository(args[++i]);
                    continue;
                }
                if (args[i].equals(JAM_OPT)) {
                    setJamFile(args[++i]);
                    continue;
                }
                if (args[i].equals(MODULEMAIN_OPT)) {
                    setModuleMain(args[++i]);
                    continue;
                }
                doUsage("option " + args[i] + "  not recognised");
            }
            argList.add(args[i]);
        }
        // Create application arguments list.
        String[] appArgs = new String[argList.size()];
        argList.toArray(appArgs);
        launchModule(appArgs);
    }

    /*
     * Used by the test driver
     */
    private static void doUsage() {
        doUsage(null);
    }

     /*
      * Used by the test driver
      */
    private static void doUsage(String message) {
        if (message != null) System.out.println("Error: " + message);
        System.out.println("Usage: java -jam /baz/bar/foo.jam");
        System.out.println("Usage: java -module org.wombat.foo");
        System.out.println("Usage: java -repository /baz/bar -module org.wombat.foo:1.0+");
        System.out.println("Usage: java -repository /baz/bar -module org.wombat.foo -modulemain FooBar");
        System.exit(1);
    }

}
