/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * @test
 * @compile -XDignore.symbol.file BasicLauncherTests.java Utils.java HttpRepositoryTest.java ../repository/URLRepositoryServer.java
 * @run main/othervm/timeout=300  BasicLauncherTests
 */


/*
 * This is the main driver for the launcher tests, to ensure that
 * the launcher shall:
 *      a) process the appropriate arguments
 *      b) pass the application arguments correctly to the application
 *      c) upon an error prints appropriate diagnostics
 *      d) returns an appropriate exit value.
 *
 * There are 3 types of files we create:
 *      a> A  main class, with a dependency on another module,
 *         which also compares the arguments passed, and returns
 *         a non-zero exit value if it does not get all the expected arguments.
 *      b> an alternate main class
 * These tests are used by all java launcher argument combinations.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.module.Version;
import java.net.URL;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

enum RepoType {
    JAR_REPO, JAM_REPO, PACK_REPO
}

public class BasicLauncherTests {

    private static RepoType repoType;

    final static String SEP         = File.separator;
    final static String MINF        = "MODULE-INF";
    final static String MD          = "MODULE.METADATA";
    final static String JAVA_EXT    = ".java";
    final static String CLASS_EXT   = ".class";
    final static String JAM_EXT     = ".jam";
    final static String JAR_EXT     = ".jar";
    final static String PACK_EXT    = ".pack.gz";
    final static String JAM_VERSION = "1.0.0.0";
    final static String JAM_SEP     = "-";
    final static String REPO_XML    = "repository-metadata.xml";
    final static String SP_NAME     = "super_package";
    final static String REPODIRNAME = "repodir";
    final static String T1_JAM      = "t1" + JAM_EXT;
    final static String T1_PACK     = "t1" + JAM_EXT + PACK_EXT;

    static final String TESTJAVA = System.getProperty("java.home");

    private final static String WARNING_HEADER =
            "// This is a machine generated file, do not edit\n";

    private final static String[] JAVAC_OPTS = new String[] {
        "-source",
        "6",
        "-target",
        "6",
        "-implicit:none",
        "-Xlint:all",
        "-XDignore.symbol.file"
    };


    // The children repository directories
    static String expRepository;
    static String jamRepository;
    static String jarRepository;
    static String urlRepository ;
    static String packRepository;

    private final static String EXPANDED_REPO   = "raw-repo";
    private final static String JAMBASED_REPO   = "jam-repo";
    private final static String JARBASED_REPO   = "jar-repo";
    private final static String URLBASED_REPO   = "url-repo";
    private final static String PACKBASED_REPO  = "pack-repo";

    private final static String PGM_TAIL =
            "System.exit(0);\n}\n}\n";


    // A singleton
    private BasicLauncherTests() {}

    private static JavaCompiler compiler;

    static {
        compiler = ToolProvider.getSystemJavaCompiler();
    }

    static ArrayList<File> javaFiles = new ArrayList();
    static ArrayList<File> modFiles = new ArrayList();

    private static void doPrintStream(String filename, StringBuilder in)
            throws FileNotFoundException {
        File f = new File(filename);
        if (f.exists()) f.delete();
        File parent = f.getParentFile();
        parent.mkdirs();
        PrintStream p = new PrintStream(f);
        p.print(new String(in));
        p.flush();
        p.close();
        if (filename.contains(SP_NAME)) {
            modFiles.add(f);
        }
        javaFiles.add(f);
    }

    private static void createModule(String basepath,
                                     String pkg,
                                     String otherpkg,
                                     String klass,
                                     String otherklass) throws IOException {

        String outpath = basepath + SEP + pkg + SEP + pkg + SEP;
        // Create the java files
        StringBuilder out = new StringBuilder();
        out = out.append(WARNING_HEADER);
        out = out.append("package " + pkg + ";\n");
        out = out.append("import java.util.*;\n");
        out = out.append("public class " + klass + " {\n");
        out = out.append("  static String[] inargs = {\"one\", \"two\", \"three\", \"four\"};\n");
        out = out.append("    public static void main(String[] args) throws Exception {\n");
        out = out.append("      Class clazz = " + klass + CLASS_EXT + ";\n");
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

        String java_file = outpath + klass + JAVA_EXT;

        doPrintStream(java_file, out);

        // Create the java definition files
        out = new StringBuilder();
        out = out.append(WARNING_HEADER);
        out = out.append("package " + pkg + ";\n");
        out = out.append("import java.lang.reflect.Superpackage.*;\n");
        out = out.append("import java.module.annotation.*;\n");
        out = out.append("@MainClass(\"" + pkg + "." + klass + "\")\n");
        out = out.append("@ImportModules({\n");
        out = out.append("   @ImportModule(name=\"java.se\"),\n");
        out = out.append("   @ImportModule(name=\"java.classpath\")");
        if (otherpkg != null) {
            out = out.append(",\n   @ImportModule(name=\"" + otherpkg + "\")");
        }
        out = out.append("})\n");
        out = out.append("class " + SP_NAME + "{\n");
        if (otherpkg == null) {
            out = out.append("    exports " + pkg + "$" + klass + ";");
        }
        out = out.append("\n}\n");
        String modfile  = outpath + SP_NAME + JAVA_EXT;
        doPrintStream(modfile, out);
    }

    // Simple single file compiler
    private static void compileFile(String srcPath) throws IOException {
        String[] args = Arrays.copyOf(JAVAC_OPTS, JAVAC_OPTS.length + 1);
        args[JAVAC_OPTS.length] = srcPath;
        abort(compiler.run(null, null, null, args)==0,
              "Single-file compilation failed");
    }

    private static void compileFiles() throws IOException {
        @SuppressWarnings("unchecked")
        List<String> args = new ArrayList<String>(Arrays.asList(JAVAC_OPTS));
        for (File srcFile: javaFiles) {
            args.add(srcFile.getAbsolutePath());
        }
        String[] a = args.toArray(new String[0]);
        abort(compiler.run(null, null, null, a)==0,
              "Multi-file compilation failed");
    }

    private static void createTestJarFile() {
        PrintStream p = null;
        File clsFile = null;
        File srcFile = new File("HelloWorld.java");

        if (srcFile.exists()) {
            abort(srcFile.delete(), "Could not delete" + srcFile);
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
            compileFile(srcFile.getName());
            String fName = Utils.baseName(srcFile);
            clsFile = new File(fName + CLASS_EXT);
            sun.tools.jar.Main jartool = new sun.tools.jar.Main(
                System.out, System.err, "JarCreator");

            String[] args = new String[] {
                "cfe",
                jarRepository + SEP + fName + JAR_EXT,
                fName,
                clsFile.getName(),
            };
            abort(jartool.run(args), "JarCreator Failed") ;
        } catch (Exception ex) {
            unexpected(ex);
        } finally {
            if (p != null) {
                p.close();
            }
            srcFile.delete();
            clsFile.delete();
        }
    }

    private static void createAltMain(String basepath,
                                      String pkg,
                                      String klass,
                                      String altklass) throws IOException {
        StringBuilder out = new StringBuilder();
        out = out.append(WARNING_HEADER);
        out = out.append("package " + pkg + ";\n");
        out = out.append("import java.util.*;\n");
        out = out.append("import java.lang.reflect.*;\n");
        out = out.append("public class " + altklass + " {\n");
        out = out.append("public static void main(String[] args) throws Exception {\n");
        out = out.append("    Class<?> clazz = " + altklass + CLASS_EXT + ";\n");
        out = out.append("    System.out.println(\"running: \" + clazz + \"/\" +"
                         + " clazz.getClassLoader());\n");
        out = out.append("    ClassLoader scl = ClassLoader.getSystemClassLoader();\n");
        out = out.append("    clazz = scl.loadClass(\"HelloWorld\");\n");
        out = out.append("    Method main = clazz.getMethod(\"main\",String[].class);\n");
        out = out.append("    main.invoke(null, (Object)args);\n");
        out = out.append("    System.exit(1);\n");  // The other main should exit with 0
        out = out.append("}\n");
        out = out.append("}\n");
        doPrintStream(basepath + SEP + pkg + SEP + pkg + SEP + altklass + JAVA_EXT, out);
    }

    private static void createJarRepo() throws IOException {
        createRepo0(repoType.JAR_REPO);
        createTestJarFile();
    }

    private static void createJamRepo() throws IOException {
        createRepo0(repoType.JAM_REPO);
    }

    private static void createPackRepo() throws IOException {
        createRepo0(repoType.PACK_REPO);
    }

    private static void createRepo0(RepoType type) throws IOException {
        for (File src: Utils.getDirs(expRepository)) {
            String dst = null;
            switch(type) {
            case JAR_REPO:
                new File(jarRepository).mkdirs();
                dst = jarRepository + SEP + src.getName() + JAR_EXT;
                Utils.makeJar(src, new File(jarRepository + SEP
                        + src.getName() + JAR_EXT), "t2.Maint2");
                break;
            case JAM_REPO:
                new File(jamRepository).mkdirs();
                dst = jamRepository + SEP + src.getName() + JAM_EXT;
                Utils.makeJam(src, new File(dst));
                break;
            case PACK_REPO:
                new File(packRepository).mkdirs();
                dst = packRepository + SEP + src.getName() + JAM_EXT + PACK_EXT;
                Utils.makeJam(src, new File(dst));
                break;
            default:
                abort(false, "Should not reach here");
            }
        }
    }

    private static void createURLRepo() throws IOException {
        if (!new File(urlRepository).mkdirs()) {
            throw new IOException("could not mkdir " + urlRepository);
        }

        String httpMetaXML = urlRepository + SEP + REPO_XML;
        PrintStream repoxml = new PrintStream(httpMetaXML);

        repoxml.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        repoxml.println("<modules>");

        File[] jamfiles = new File(jamRepository).listFiles(Utils.JAM_FILTER);
        for (File src: jamfiles) {
            repoxml.println("\t<module>");
            repoxml.println("\t\t<name>" + Utils.baseName(src) + "</name>");
            repoxml.println("\t\t<version>" + Version.valueOf(JAM_VERSION) + "</version>");
            repoxml.println("\t</module>");
            // Copy the jamFile to url repo with a version #
            File dst = new File(urlRepository + SEP + Utils.baseName(src) + SEP
                        + Version.valueOf(JAM_VERSION) + SEP + Utils.baseName(src)
                        + JAM_SEP + Version.valueOf(JAM_VERSION) + JAM_EXT);
            Utils.copyFile(src, dst);
            File f = new File(urlRepository + SEP + Utils.baseName(src) + SEP
                        + Version.valueOf(JAM_VERSION));
            File md_src = new File(expRepository + SEP + Utils.baseName(src)
                        + SEP + Utils.baseName(src) + SEP + SP_NAME + CLASS_EXT);
            File md_dst = new File(f.getCanonicalPath() + SEP + MD);
            Utils.copyFile(md_src, md_dst);
        }
        repoxml.println("</modules>");
        repoxml.flush();
        repoxml.close();
    }

    public static void createExpandedRepo() throws IOException {
        createModule(expRepository, "t2", null, "Maint2", null);
        createModule(expRepository, "t1", "t2", "Maint1", "Maint2");
        createAltMain(expRepository, "t1", "Maint1", "AltMaint1");
        compileFiles();
    }

    public static void realMain(String[] args) throws Exception {
        abort(TESTJAVA != null, "java.home must be set");
        File testRepoDir = new File("." + SEP + REPODIRNAME).getCanonicalFile();
        Utils.recursiveDelete(testRepoDir);

        expRepository = testRepoDir + SEP + EXPANDED_REPO;
        createExpandedRepo();

        jarRepository = testRepoDir + SEP + JARBASED_REPO;
        jamRepository = testRepoDir + SEP + JAMBASED_REPO;
        packRepository = testRepoDir + SEP + PACKBASED_REPO;
        createJarRepo();
        createJamRepo();
        createPackRepo();

        urlRepository = testRepoDir + SEP + URLBASED_REPO;
        createURLRepo();

        ArrayList<TestExec> tests = new ArrayList();
        addNegativeTests(tests);
        // XXX Uncomment when ModuleLauncher provides an ExpandedRepository
        // addModuleTests(tests);
        addJamTests(tests);
        // XXX Uncomment when ModuleLauncher supports .jam.pack.gz
        // addPackTests(tests);
        for (TestExec t: tests) {
            t.run();
        }
        //Start the HttpRepositoryTests
        HttpRepositoryTest.realMain(null);
    }

    // httpTests called from HttpRepositoryTest
    static void runHttpTests(URL u) throws IOException {
        TestExec t = new TestExec("HttpURLRepository tests");
        // run module test using file url
        t = new TestExec("modules: Http protocol");
        t.repository = u.toString();
        t.run();
    }

    // negative tests.
    static void addNegativeTests(List<TestExec> params) throws IOException {
        TestExec t;

        // run module test at an absolute directory with wrong args
        t = new TestExec("negative: wrong arguments", false);
        t.repository = urlRepository;
        String[] args = {"one"};
        t.cmdArgs = args;
        params.add(t);

        t = new TestExec("negative: no module", false);
        t.module = "";
        params.add(t);

        t = new TestExec("negative: module and jam", false);
        t.jam    = jamRepository + SEP + t.module + JAM_EXT;
        params.add(t);

        String jamfile = jamRepository + SEP + t.module + JAM_EXT;

        t = new TestExec("negative: repository and jam", false);
        t.repository =  urlRepository;
        t.jam = jamfile;
        t.module = null;
        params.add(t);

        t = new TestExec("negative: module, repository and jam", false);
        t.jam    = jamfile;
        t.repository = urlRepository;
        params.add(t);

        String jarfile = jamRepository + SEP + "t2" + JAR_EXT;

        t = new TestExec("negative: module and jar", false);
        t.jar = jarfile;
        params.add(t);

        t = new TestExec("negative: repository and jar", false);
        t.module = null;
        t.repository = urlRepository;
        t.jar = jarfile;
        params.add(t);

        t = new TestExec("negative: jam and jar", false);
        t.module = null;
        t.jam = jamfile;
        t.jar = jarfile;
        params.add(t);

        t = new TestExec("negative: module, repository, jam, jar", false);
        t.jam    = jamfile;
        t.repository = urlRepository;
        t.jar = jarfile;
        params.add(t);
    }

    static void addModuleTests(List<TestExec> params) throws IOException {
        TestExec t;
        t = new TestExec("modules: current directory");
        t.workingdir = expRepository;
        params.add(t);

        t = new TestExec("modules: relative directory");
        t.workingdir = ".";
        t.repository = "." + SEP + REPODIRNAME + SEP + EXPANDED_REPO;
        params.add(t);

        t = new TestExec("modules: absolute directory");
        t.repository = expRepository;
        params.add(t);

        t = new TestExec("modules: file protocol");
        t.repository =new File(urlRepository).toURI().toURL().toString();
        params.add(t);
    }

    static void addJamTests(List<TestExec>params) throws IOException {
        String jamfile = jamRepository + SEP + T1_JAM;
        TestExec t;

        t = new TestExec("jam: current directory");
        t.workingdir = jamRepository;
        t.jam = T1_JAM;
        t.module = null;
        params.add(t);

        t = new TestExec("jam: relative directory");
        t.workingdir = ".";
        t.jam = "." + SEP +  REPODIRNAME + SEP + JAMBASED_REPO + SEP + T1_JAM;
        t.module = null;
        params.add(t);

        t = new TestExec("jam: absolute directory");
        t.jam = jamfile;
        t.module = null;
        params.add(t);

        t = new TestExec("jam: absolute directory with an alternate main");
        t.classpath = jarRepository + SEP + "HelloWorld" + JAR_EXT;
        t.jam = jamfile;
        t.module = null;
        t.modulemain = "t1.AltMaint1";
        params.add(t);
    }

   static void addPackTests(List<TestExec>params) throws IOException {
        String jamfile = packRepository + SEP + T1_PACK;
        TestExec t;

        t = new TestExec("jam-packed: absolute directory");
        t.jam = jamfile;
        t.module = null;
        params.add(t);

        t = new TestExec("jam-packed: absolute directory with an alternate main");
        t.jam = jamfile;
        t.module = null;
        t.modulemain = "t1.AltMaint1";
        params.add(t);
    }


    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void abort(boolean cond, String msg) {
        if (!cond) {
            System.out.println(msg);
            ++failed;
            Thread.dumpStack();
            System.exit(1);
        }
    }
    static boolean pass() {passed++; return true;}
    static boolean fail() {failed++; Thread.dumpStack(); return false;}
    static boolean fail(String msg) {System.out.println(msg); return fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static boolean check(boolean cond) {if (cond) pass(); else fail(); return cond;}
    static boolean check(boolean cond, String msg) { boolean ret = check(cond); if (!ret) System.out.println(msg); return ret;}
    static boolean equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) return pass();
        else return fail(x + " not equal to " + y);}

    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");
    }
}

class TestExec {
    // Set by the ctor to indicate test type.
    boolean positive;
    String testname;

    String workingdir   = ".";
    String repository   = null;
    String jam          = null;
    String jar          = null;
    String module       = "t1";
    String modulemain   = null;
    String classpath    = null;

    final File downloadDir;
    final String downloadDirName;

    String[] cmdArgs    = { "one", "two", "three", "four" };
    static BasicLauncherTests blt;

    private ArrayList<String> javaCmdArgs = new ArrayList();
    private ArrayList<String> errlog = new ArrayList();

    public TestExec(String tname) {
        testname = tname;
        positive = true;
        File baseDir = new File(System.getProperty("test.scratch", "."));
        downloadDir = new File(baseDir, "download" + System.currentTimeMillis());
        try {
            Utils.recursiveDelete(downloadDir);
            downloadDir.mkdirs();
            downloadDirName = downloadDir.getCanonicalPath();
        } catch (IOException ex) {
            throw new IllegalArgumentException(
                "Can't create test " + tname + ": " + ex);
        }
    }

    public TestExec(String tname, boolean v) {
        this(tname);
        positive = v;
    }

    void run() {
        String javaCmd = blt.TESTJAVA + File.separator + "bin" + File.separator + "java";
        if (!new File(javaCmd).exists()) {
            javaCmd = javaCmd + ".exe";
        }

        javaCmdArgs.add(javaCmd);

        javaCmdArgs.add("-Dsun.module.repository.URLRepository.downloadDirectory="
                        + downloadDirName);

        if (classpath != null) {
            javaCmdArgs.add("-classpath");
            javaCmdArgs.add(classpath);
        }

        if (jam != null) {
            javaCmdArgs.add("-jam");
            javaCmdArgs.add(jam);
        }

        if (jar != null) {
            javaCmdArgs.add("-jar");
            javaCmdArgs.add(jar);
        }

        if (repository != null) {
            javaCmdArgs.add("-repository");
            javaCmdArgs.add(repository);
        }

        if (module != null) {
            javaCmdArgs.add("-module");
            javaCmdArgs.add(module);
        }

        if (modulemain != null) {
            javaCmdArgs.add("-modulemain");
            javaCmdArgs.add(modulemain);
        }

        for (String arg : cmdArgs) {
            javaCmdArgs.add(arg);
        }

        ProcessBuilder pb = new ProcessBuilder(javaCmdArgs);
        Map<String, String> env = pb.environment();

        try {
            File thisDir = new File(workingdir).getCanonicalFile();
            pb.directory(thisDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader rd = new BufferedReader(
                new InputStreamReader(p.getInputStream()),
                8192);
            String in = rd.readLine();
            while (in != null) {
                errlog.add(in);
                in = rd.readLine();
            }
            int retval = p.waitFor();
            System.out.print(testname);
            if (positive && retval != 0) {
                blt.fail(this.toString());
                System.out.println(":FAIL");
            } else  if (!positive && retval == 0) {
                blt.fail(this.toString());
                System.out.println(":FAIL");
            } else {
                blt.pass();
                System.out.println(":PASS");
            }
            p.destroy();

        } catch (Exception ex) {
            blt.unexpected(ex);
        }

        try {
            Utils.recursiveDelete(downloadDir);
        } catch (IOException ex) {
            // ignore: it will get cleaned up by normal test mechanism
        }
    }

    public String toString() {
        StringBuilder out = new StringBuilder();
        out = out.append("Test: " + testname + "\n");
        out = out.append("  type:\t " + (positive ? "positive" : "negative") + "\n");
        out = out.append("  workdir:\t" + workingdir + "\n");
        out = out.append("  classpath:\t" + classpath + "\n");
        out = out.append("  repository:\t" + repository + "\n");
        out = out.append("  jam:\t" + jam + "\n");
        out = out.append("  module:\t" + module + "\n");
        out = out.append("  modulemain:\t" + modulemain + "\n");
        out = out.append("The full command line:\n");
        for (String arg: javaCmdArgs) {
            out = out.append(arg + " ");
        }
        out = out.append("\nThe cmd output:\n");
        for (String a : errlog) {
            out.append(a + "\n");
        }
        return new String(out.append("\n"));
    }
}
