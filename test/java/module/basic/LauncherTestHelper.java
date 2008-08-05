/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.module.Version;
import sun.module.JamUtils;

public class LauncherTestHelper {
    enum RepoType {
        JAR_REPO, JAM_REPO, PACK_REPO
    }

    private static RepoType repoType;

    private final static String WARNING_HEADER =
            "// This is a machine generated file, do not edit\n";

    // the parent of the repository directories
    final static String REPODIRNAME = "repodir";

    // The children repository directories
    static String rawRepository;
    static String jamRepository;
    static String jarRepository;
    static String urlRepository ;
    static String packRepository;

    final static String RAW_REPO   = "raw-repo";
    final static String JAM_REPO   = "jam-repo";
    final static String JAR_REPO   = "jar-repo";
    final static String URL_REPO   = "url-repo";
    final static String PACK_REPO  = "pack-repo";

    private final static String PGM_TAIL =
            "System.exit(0);\n}\n}\n";

    private LauncherTestHelper() {} // all static

    private static void doPrintStream(File out, StringBuilder in)
            throws FileNotFoundException {
        if (out.exists()) out.delete();
        File parent = out.getParentFile();
        parent.mkdirs();
        PrintStream p = new PrintStream(out);
        p.print(new String(in));
        p.flush();
        p.close();
    }

    /*
     * The raw repository layout is as follows:
     * raw-repo
     *    module1
     *       pkg1
     *          java-files
     *          class-files
     *       pkg2
     *         .....
     *   module2
     *       pkg1
     *   .....
     */

    public static void createRawRepo(File testRepoDir) throws IOException {
        File rawrepoDir = new File(testRepoDir, RAW_REPO);
        rawRepository = rawrepoDir.getAbsolutePath();
        if (!rawrepoDir.mkdirs()) {
            throw new IOException("could not mkdir " + rawRepository);
        }
        createModule(rawrepoDir, "t2", "p2", null, null, "Maint2", true, null);
        createModule(rawrepoDir, "t1", "p1", "t2", "p2", "Maint1", true, "Maint2");
        createModule(rawrepoDir, "t2", "p3", null, null, "Foo", false, null);
        createAltMain(rawrepoDir, "t1", "p1", "Maint1", "AltMaint1");
        Utils.compileFiles(rawrepoDir, "t1", "t2");
    }

    private static void createModule(File baseDir,
                                     String module,
                                     String pkg,
                                     String importModule,
                                     String otherpkg,
                                     String klass,
                                     boolean mainclass,
                                     String otherklass) throws IOException {

        File modDir = new File(baseDir, module);
        // Create the java files
        StringBuilder out = new StringBuilder();
        out = out.append(WARNING_HEADER);
        out = out.append("module " + module + ";\n");
        out = out.append("package " + pkg + ";\n");
        out = out.append("import java.util.*;\n");
        out = out.append("public class " + klass + " {\n");
        out = out.append("  static String[] inargs = {\"one\", \"two\", \"three\", \"four\"};\n");
        out = out.append("    public static void main(String[] args) throws Exception {\n");
        out = out.append("      Class clazz = " + klass + Utils.CLASS_EXT + ";\n");
        out = out.append("      ClassLoader cl = clazz.getClassLoader();\n");
        out = out.append("      while (cl != null) {\n");
        out = out.append("          System.out.println(\" -->\" + cl);\n");
        out = out.append("          cl = cl.getParent();\n");
        out = out.append("      }\n");
        out = out.append("      ClassLoader scl = ClassLoader.getSystemClassLoader();\n");
        out = out.append("      if (scl.getParent() != null){ \n");
        out = out.append("           System.out.println(\"Parent of System ClassLoader must be bootstrap\");\n");
        out = out.append("           System.exit(1);");
        out = out.append("      }\n");

        out = out.append("      System.out.println(\"System ClassLoader: \" + scl);\n");
        out = out.append("      if (!(scl instanceof sun.module.core.ProxyModuleLoader)) {\n");
        out = out.append("          throw new Exception(\"Unexpected system classloader\");\n");
        out = out.append("      }\n");
        out = out.append("      System.out.println(\"running: \" + clazz + \"/\" + clazz.getClassLoader());\n");
        out = out.append("      System.out.println(\"Args: \" + Arrays.toString(args));\n");
        out = out.append("      if (args.length != inargs.length)\n");
        out = out.append("        throw new Exception(\"Error: incorrect number of arguments\");\n");
        out = out.append("      for (int i = 0 ; i < args.length ; i++)\n");
        out = out.append("        if (!inargs[i].equals(args[i]))\n");
        out = out.append("           throw new Exception(\"Error: expected \'\" + inargs[i] + \"\' got\'\" + args[i] + \"\'\");\n");
        if (otherpkg != null) {
            out = out.append(otherpkg + "." + otherklass + ".main(args);");
        }

        out = out.append(PGM_TAIL);
        File pkgDir = new File(modDir, pkg);
        File javaFile = new File(pkgDir, klass + Utils.JAVA_EXT);

        doPrintStream(javaFile, out);
        if (mainclass) {
            // Create the java definition files
            out = new StringBuilder();
            out = out.append(WARNING_HEADER);
            out = out.append("@Version(\"" + Version.valueOf(Utils.JAM_VERSION) + "\")\n");
            out = out.append("@MainClass(\"" + pkg + "." + klass + "\")\n");
            out = out.append("@ImportModules({\n");
            out = out.append("   @ImportModule(name=\"java.se\"),\n");
            out = out.append("   @ImportModule(name=\"java.classpath\")");
            if (importModule != null) {
                out = out.append(",\n   @ImportModule(name=\"" + importModule + "\")");
            }
            out = out.append("\n})\n");
// TODO comment it out for now
//        if (otherpkg == null) {
//            out = out.append("    exports " + pkg + "$" + klass + ";");
//        }
            out = out.append("\nmodule " + module + ";\n");
            File modFile = new File(pkgDir, JamUtils.MODULE_INFO_JAVA);
            doPrintStream(modFile, out);
        }
    }


    private static void createTestJarFile() {
        PrintStream p = null;
        File clsFile = null;
        File srcFile = new File("HelloWorld.java");

        if (srcFile.exists()) {
            Utils.abort(srcFile.delete(), "Could not delete" + srcFile);
        }

        try {
            StringBuilder out = new StringBuilder(WARNING_HEADER);
            out = out.append("import java.util.logging.Level;\n");
            out = out.append("import java.util.logging.Logger;\n");
            out = out.append("public class HelloWorld {\n");
            out = out.append("public static void main(String[] args) {\n");
            out = out.append("Logger.getLogger(\"global\").log(Level.INFO,\"A Test\");\n");
            out = out.append("for (String x : args) {\n");
            out = out.append("Logger.getLogger(\"global\").log(Level.INFO, null, \"arg:\" + x);\n");
            out = out.append("System.exit(0);\n");
            out = out.append("}\n");
            out = out.append("}\n");
            out = out.append("}\n");
            p = new PrintStream(srcFile);
            p.print(new String(out));
            p.flush();
            p.close();
            Utils.compileFile(srcFile.getName());
            String fName = Utils.baseName(srcFile);
            clsFile = new File(fName + Utils.CLASS_EXT);
            sun.tools.jar.Main jartool = new sun.tools.jar.Main(
                System.out, System.err, "JarCreator");

            String[] args = new String[] {
                "cfe",
                new File(jarRepository, fName + Utils.JAR_EXT).getAbsolutePath(),
                fName,
                clsFile.getName(),
            };
            Utils.abort(jartool.run(args), "JarCreator Failed") ;
        } catch (Exception ex) {
            Utils.unexpected(ex);
        } finally {
            JamUtils.close(p);
            srcFile.delete();
            clsFile.delete();
        }
    }

    private static void createAltMain(File baseDir,
                                      String module,
                                      String pkg,
                                      String klass,
                                      String altklass) throws IOException {
        StringBuilder out = new StringBuilder();
        out = out.append(WARNING_HEADER);
        out = out.append("module " + module + ";\n");
        out = out.append("package " + pkg + ";\n");
        out = out.append("import java.util.*;\n");
        out = out.append("import java.lang.reflect.*;\n");
        out = out.append("public class " + altklass + " {\n");
        out = out.append("public static void main(String[] args) throws Exception {\n");
        out = out.append("    Class<?> clazz = " + altklass + Utils.CLASS_EXT + ";\n");
        out = out.append("    System.out.println(\"running: \" + clazz + \"/\" +"
                         + " clazz.getClassLoader());\n");
        out = out.append("    ClassLoader scl = ClassLoader.getSystemClassLoader();\n");
        out = out.append("    clazz = scl.loadClass(\"HelloWorld\");\n");
        out = out.append("    Method main = clazz.getMethod(\"main\",String[].class);\n");
        out = out.append("    main.invoke(null, (Object)args);\n");
        out = out.append("    System.exit(1);\n");  // The other main should exit with 0
        out = out.append("}\n");
        out = out.append("}\n");
        File modDir = new File(baseDir, module);
        File pkgDir = new File(modDir, pkg);
        File javaFile = new File(pkgDir, altklass + Utils.JAVA_EXT);
        doPrintStream(javaFile, out);
    }

    private static void createJarRepo(File testRepoDir) throws IOException {
        File jarrepoDir = new File(testRepoDir, JAR_REPO);
        if (!jarrepoDir.mkdirs()) {
              throw new IOException("could not mkdir " + jarRepository);
        }
        jarRepository = jarrepoDir.getAbsolutePath();
        createRepo0(repoType.JAR_REPO);
        createTestJarFile();
    }

    private static void createJamRepo(File testRepoDir) throws IOException {
        File jamrepoDir = new File(testRepoDir, JAM_REPO);
        if (!jamrepoDir.mkdirs()) {
             throw new IOException("could not mkdir " + jamRepository);
        }
        jamRepository = jamrepoDir.getAbsolutePath();
        createRepo0(repoType.JAM_REPO);
    }

    private static void createPackRepo(File testRepoDir) throws IOException {
        File packrepoDir = new File(testRepoDir, PACK_REPO);
        if (!packrepoDir.mkdirs()) {
            throw new IOException("could not mkdir " + packRepository);
        }
        packRepository = packrepoDir.getAbsolutePath();
        createRepo0(repoType.PACK_REPO);
    }

    private static void createRepo0(RepoType type) throws IOException {
        for (File src: Utils.getDirs(rawRepository)) {
            switch(type) {
            case JAR_REPO:
            {
                File dst = new File(jarRepository, src.getName() + Utils.JAR_EXT);
                Utils.makeJar(src, dst, "t2.Maint2");
                break;
            }
            case JAM_REPO:
            {
                File dst = new File(jamRepository, src.getName() + Utils.JAM_EXT);
                Utils.makeJam(src, dst);
                break;
            }
            case PACK_REPO:
            {
                File dst = new File(packRepository,
                        src.getName() + Utils.JAM_EXT + Utils.PACK_EXT);
                Utils.makeJam(src, dst);
                break;
            }
            default:
                Utils.abort(false, "Should not reach here");
            }
        }
    }

    /*
     * The repository layout is as follows:
     *    repository-dir
     *      repository-metadata.xml
     *      module1
     *          version (1.0, 2.0, etc)
     *              MODULE-METADATA
     *              module1-$version.jam
     *      module2
     *          ....
     *      module3
     *          ....
     */

    private static void createURLRepo(File testRepoDir) throws IOException {
        File urlrepoDir = new File(testRepoDir, URL_REPO);
        urlRepository = urlrepoDir.getAbsolutePath();
        if (!urlrepoDir.mkdirs()) {
            throw new IOException("could not mkdir " + urlRepository);
        }

        PrintStream repoxml = new PrintStream(new File(urlrepoDir, Utils.REPO_XML));

        repoxml.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        repoxml.println("<modules>");

        File[] jamfiles = new File(jamRepository).listFiles(Utils.JAM_FILTER);
        for (File src: jamfiles) {
            repoxml.println("\t<module>");
            repoxml.println("\t\t<name>" + Utils.baseName(src) + "</name>");
            repoxml.println("\t\t<version>" + Version.valueOf(Utils.JAM_VERSION) + "</version>");
            repoxml.println("\t</module>");

            // Copy the jamFile to url repo with a version #
            File d1 = new File(urlRepository, Utils.baseName(src));
            File d2 = new File(d1, Version.valueOf(Utils.JAM_VERSION).toString());

            File dst = new File(d2, Utils.baseName(src) +
                    Utils.JAM_SEP + Version.valueOf(Utils.JAM_VERSION) + Utils.JAM_EXT );
            Utils.copyFile(src, dst);

            File mdDst =new File(d2, JamUtils.MODULE_METADATA);
            Utils.extractFileTo(dst, JamUtils.MODULE_INF_METADATA, mdDst);
        }
        repoxml.println("</modules>");
        repoxml.flush();
        repoxml.close();
    }

    static void createRepo() throws IOException {
        Utils.abort(Utils.TESTJAVA != null, "createRepo: java.home must be set");
        File testRepoDir = new File(".", REPODIRNAME).getAbsoluteFile();
        Utils.recursiveDelete(testRepoDir);

        // create all our repos
        createRawRepo(testRepoDir);
        createJarRepo(testRepoDir);
        createJamRepo(testRepoDir);
        createPackRepo(testRepoDir);
        createURLRepo(testRepoDir);
        System.out.println("Repository created: " + testRepoDir);
    }

}
