/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
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

/*
 * @test
 * @summary Test packaging tools jam and jar
 * @compile -XDignore.symbol.file JamTester.java  ../../basic/Utils.java ../JamBuilder.java
 * @run main JamTester
 */
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.module.Version;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.jar.JarFile;
import sun.module.JamUtils;
import sun.module.MetadataParser;
import sun.module.ModuleParsingException;

public class JamTester {
    /* Mutliple packages in a module are tested with several different
     * variations. The repository layout is as follows:
     * repotop
     *    [ module.jam ] the target jam.
     *    module
     *       MODULE-INF/lib/
     *       platform/lib
     *       pkg1
     *          java-files
     *          class-files
     *       pkg2
     *         .....
     * the embedded jars are staged as follows:
     * repotop
     *    module
     *      ......
     *    embedded
     *      standard   // jarred into MODULE-INF/lib
     *        alpha
     *      nonstandard // jarred into platform/lib
     *        beta
     */

    private static BitSet dipSwitch = new BitSet();
    private static final int NEGATIVE_TEST      = 0;
    private static final int MISSING_MODINF0 = 1;
    private static final int EXPORT_LEGACY = 2;
    private static final int EMBEDDED_NONSTD = 3;
    private static final int WRONG_MODULE = 4;
    private static final int AUTO_CREATE = 5;
    final static String testNames[] = {
        "Negative_Test",
        "Missing_ModInfo",
        "Export_legacy",
        "Embedded_NonStd_Jar",
        "Wrong_Module",
        "Auto_Create_Module"
    };
    static enum ClassType {
        PUBLIC, PROTECTED, MODULE, PRIVATE
    };

    static File createJavaFile(ClassType cType, File baseDir,
        String module, String pkg, String className) throws IOException {

        File modDir = new File(baseDir, module == null ? "." : module);
        File pkgDir = new File(modDir, pkg == null ? "." : pkg);
        File javaFile = new File(pkgDir, className + Utils.JAVA_EXT);
        if (!pkgDir.exists()) {
            Utils.abort(pkgDir.mkdirs(), "cannot mkdir " + pkgDir);
        }
        PrintWriter pw = new PrintWriter(new FileWriter(javaFile));
        String indent = "    ";
        if (module != null) {
            pw.println("module " + module + ";");
        }
        if (pkg != null) {
            pw.println("package " + pkg + ";");
        }
        switch (cType) {
            case PUBLIC:
                pw.print("public ");
                break;
            case PROTECTED:
                pw.print("protected ");
                break;
            case MODULE:
                pw.print("module ");
                break;
            case PRIVATE:
                pw.print("private ");
                break;
            default:
                pw.print("");
                break;
        }
        pw.println("class " + className + "{");
        pw.println(indent + "public static void main(String...args) {");
        pw.println(indent + indent + "for (String x : args){");
        pw.println(indent + indent + indent + "System.out.println(x);");
        pw.println(indent + indent + "}");
        pw.println(indent + "}");
        pw.println(indent + "public void aPublicMethod(){}");
        pw.println(indent + "protected void aProtectedMethod(){}");
        pw.println(indent + "private void aPrivateMethod(){}");
//        pw.println(indent + "module void aModuleMethod()"); TODO
        pw.println("}");
        pw.close();
        return javaFile;
    }

    static File createModuleInfoFile(File baseDir, String module,
            String mainClass, String jarLibraryPath, boolean exportLegacy)
            throws IOException {
        File modDir = new File(baseDir, module);
        File modFile = new File(modDir, JamUtils.MODULE_INFO_JAVA);
        PrintWriter pw = new PrintWriter(new FileWriter(modFile));
        String indent = "    ";
        pw.println("@Version(\"" + Version.DEFAULT + "\")");
        if (mainClass != null) {
            pw.println("@MainClass(\"" + mainClass + "\")");
        }
        if (exportLegacy) {
            pw.println("@ExportLegacyClasses");
        }
        if (jarLibraryPath != null) {
            pw.println("@JarLibraryPath(\"" + jarLibraryPath + "\")");
        }
        pw.println("@ImportModules({");
        pw.println(indent + "@ImportModule(name=\"java.se\", version=\"1.7+\"),");
        pw.println(indent + "@ImportModule(name=\"java.classpath\")");
        pw.println("})");
        pw.println("module " + module + ";");
        pw.close();
        return modFile;
    }
    private static File testDir = null;

    static void init(int... switches) throws IOException {
        Utils.abort(Utils.TESTJAVA != null, "java.home must be set");
        testDir = new File(".", "repotop").getAbsoluteFile();
        Utils.abort(JamUtils.recursiveDelete(testDir), "directory delete problem");
        dipSwitch.clear();
        for (int i : switches) {
            dipSwitch.set(i);
        }
    }

    static String getTestName() {
        StringBuilder sb = new StringBuilder();
        for (int i = dipSwitch.nextSetBit(0); i >= 0; i = dipSwitch.nextSetBit(i + 1)) {
            sb = sb.append(testNames[i] + "+");
        }
        return sb.toString();
    }

    private static void printTestGoals() {
        System.out.print("Test goals: ");
        System.out.println(getTestName());
    }

    static void createModule(int... switches)
            throws Exception {

        init(switches);

        String moduleName = dipSwitch.isEmpty() ? "test.m1" : "m1";
        String mainClass = "p1.APublic";

        printTestGoals();
        // create two packages belonging to the same module one
        // with public class and the other module private
        createJavaFile(ClassType.PUBLIC, testDir, moduleName, "p1", "APublic");
        createJavaFile(ClassType.MODULE, testDir, moduleName, "p3", "AModule");

        if (dipSwitch.get(WRONG_MODULE)) {
            /*
             * create a module with a different module membership
             * and move the file to the module being created.
             */
            createJavaFile(ClassType.PUBLIC, testDir, "m2", "p2", "AWrongModule");
        }

        File embeddedDir = new File(testDir, "embedded");

        // an embedded jar in the standard location
        createJavaFile(ClassType.PUBLIC, embeddedDir, moduleName, "standard",
                "AModuleEmbeddedStandard");
        // with no module affiliation
        createJavaFile(ClassType.PUBLIC, embeddedDir, null, "alpha",
                "AModuleEmbeddedLegacyStandard");
        // an embedded jar in the non standard location
        createJavaFile(ClassType.PUBLIC, embeddedDir, moduleName, "nonstandard",
                "AModuleEmbeddedNonStandard");
        // with no module affiliation
        createJavaFile(ClassType.PUBLIC, embeddedDir, null, "beta",
                "AModuleEmbeddedLegacyNonStandard");

        if (!dipSwitch.get(MISSING_MODINF0)) {
            createModuleInfoFile(testDir, moduleName, mainClass,
                    dipSwitch.get(EMBEDDED_NONSTD) ? "platform/lib" : null,
                    dipSwitch.get(EXPORT_LEGACY));
        }

        Utils.compileFiles(testDir);

        // jar and copy the embedded jars
        // A. the standard location
        File minfDir = new File(new File(testDir, moduleName), JamUtils.MODULE_INF);
        File stdlibDir = new File(minfDir, "lib");
        stdlibDir.mkdirs();
        File stdJar = new File(stdlibDir, "standard.jar");
        Utils.makeJar("cvf", stdJar.getAbsolutePath(),
                "-C", new File(embeddedDir, moduleName).getAbsolutePath(), "standard",
                "-C", embeddedDir.getAbsolutePath(), "alpha");

        // B. the non standard location
        File nonstdDir = new File(new File(testDir, moduleName), "platform");
        File nonstdlibDir = new File(nonstdDir, "lib");
        nonstdlibDir.mkdirs();
        File nonstdJar = new File(nonstdlibDir, "nonstandard.jar");
        Utils.makeJar("cvf", nonstdJar.getAbsolutePath(),
                "-C", new File(embeddedDir, moduleName).getAbsolutePath(), "nonstandard",
                "-C", embeddedDir.getAbsolutePath(), "beta");

        // Setup the expected strings
        String moduleVersion = Version.DEFAULT.toString();
        String moduleImport = "1.7+, java.classpath, java.se";
        String moduleExportClasses = "p1.APublic, standard.AModuleEmbeddedStandard";
        String moduleMemberPackages = "alpha, p1, p3, standard";
        String moduleExportPackages = "p1, standard";

        if (dipSwitch.get(EMBEDDED_NONSTD)) {
            moduleExportClasses =
                    ("nonstandard.AModuleEmbeddedNonStandard, ").concat(moduleExportClasses);
            moduleMemberPackages = "alpha, beta, nonstandard, p1, p3, standard";
            moduleExportPackages = "nonstandard, ".concat(moduleExportPackages);
        }

        if (dipSwitch.get(EXPORT_LEGACY)) {
            moduleExportClasses =
                    "alpha.AModuleEmbeddedLegacyStandard, ".concat(moduleExportClasses);
            moduleExportPackages = ("alpha, ").concat(moduleExportPackages);
        }

        if (dipSwitch.get(AUTO_CREATE)) {
            mainClass = null;
            moduleVersion = null;
            moduleImport = "1.7+, java.se";
        }

        File jamFile = new File(testDir, moduleName + Utils.JAM_EXT);
        String jamfileName = jamFile.getAbsolutePath();

        // setup the arguments
        ArrayList<String> argsList = new ArrayList<String>();
        if (dipSwitch.get(AUTO_CREATE)) {
            argsList.add("cv");
            argsList.add("-N");
            argsList.add(moduleName);
            argsList.add("-C");
            File modDir = new File(testDir, moduleName);
            argsList.add(modDir.getAbsolutePath());
            argsList.add(".");
            jamFile = new File(moduleName + Utils.JAM_EXT);
            jamfileName = jamFile.getAbsolutePath();
            // delete module-info.* if any
            File toDelete = new File(modDir, JamUtils.MODULE_INFO_CLASS);
            toDelete.delete();
            toDelete = new File(modDir, JamUtils.MODULE_INFO_JAVA);
            toDelete.delete();
        } else {
            argsList.add("cvf");
            argsList.add(jamfileName);
            argsList.add("-C");
            argsList.add(new File(testDir, moduleName).getAbsolutePath());
            argsList.add(".");
        }

        if (dipSwitch.get(WRONG_MODULE)) {
            argsList.add("-C");
            argsList.add(new File(testDir, "m2").getAbsolutePath());
            argsList.add(".");
        }

        boolean rc = false;
        // test jam tool
        jamFile.delete();
        rc = Utils.makeJam(argsList);
        verifyJam(dipSwitch.get(NEGATIVE_TEST), rc, jamfileName,
                moduleName,
                mainClass,
                moduleVersion,
                moduleImport,
                moduleExportClasses,
                moduleMemberPackages,
                moduleExportPackages);
    }

    /*
     * There are two ways to get the metadata the jam pvf and jam pf,
     * as well as direct access to the metadata noting that this does
     * not carry the annotations.
     */
    static void verifyJam(boolean negative, boolean rc, String jamfile,
            String module, String mainClass,
            String version, String imports,
            String exportClassList, String memberPackageList,
            String exportPackageTable) throws IOException {
        if (negative) {
            if (rc) {
                System.err.println("negative test should have failed");
                Utils.fail();
            } else {
                Utils.pass();
            }
            return;
        }

        // the expected strings in the output
        ArrayList<String> expectedList = new ArrayList<String>();

        if (mainClass != null) {
            expectedList.add("java.module.annotation.MainClass:[" + mainClass + "]");
        }
        if (version != null) {
            expectedList.add("java.module.annotation.Version:[" + version + "]");
        }
        if (imports != null) {
            expectedList.add("java.module.annotation.ImportModules:[" + imports + "]");
        }
        if (exportClassList != null) {
            expectedList.add("ModuleExportClassList:[" + exportClassList + "]");
        }
        if (memberPackageList != null) {
            expectedList.add("ModuleMemberPackageList:[" + memberPackageList + "]");
        }
        if (exportPackageTable != null) {
            expectedList.add("ModuleExportPackageList:[" + exportPackageTable + "]");
        }
        if (module != null) {
            expectedList.add("Module:[" + module + "]");
        }

        // using jam pvf for extra diagnostics.
        checkOutput("diags", expectedList, getJamOutput("pvf", jamfile));
        JarFile jf = new JarFile(jamfile);
        try {
            // use metadata, first we strip the annnotations off
            ArrayList<String> attributeList = new ArrayList<String>();
            for (String x : expectedList) {
                if (!x.startsWith("java.module.annotation")) {
                    attributeList.add(x);
                }
            }
            byte[] mbuf = JamUtils.getMetadataBytes(jf);
            MetadataParser metadata = new MetadataParser(mbuf);
            String mout = metadata.toString();
            System.out.println("###Output of metadata " + getTestName());
            System.out.println(mout);
            System.out.println("###End");
            checkOutput("metadata", attributeList, mout);
        } catch (ModuleParsingException ex) {
            Utils.unexpected(ex);
        } finally {
            JamUtils.close(jf);
        }
    }

    static String getJamOutput(String... jamCmds) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pw = new PrintStream(baos);
        sun.module.tools.Jam jamTool =
                new sun.module.tools.Jam(pw, System.err, "JamTool");
        jamTool.run(jamCmds);
        String output = baos.toString();
        System.out.println("###Output of " + jamCmds[0] + ":" + getTestName());
        System.out.println(output);
        System.out.println("###End");
        return output;
    }

    static void checkOutput(String label, List<String> expectedList, String cmpOutput) {
        boolean ok = true;
        for (String x : expectedList) {
            if (!cmpOutput.contains(x)) {
                System.err.println(getTestName() + "[jam-" + label + ":error: did not find " + x);
                ok = false;
            }
        }
        if (ok) {
            Utils.pass();
        } else {
            Utils.fail();
        }
    }

    public static void realMain(String... args) throws Exception {
        createModule(NEGATIVE_TEST, MISSING_MODINF0);
        createModule(NEGATIVE_TEST, WRONG_MODULE);
        createModule();
        createModule(EXPORT_LEGACY);
        createModule(EMBEDDED_NONSTD);
        createModule(AUTO_CREATE);
    }

    //--------------------- Infrastructure ---------------------------
    public static void main(String[] args) throws Throwable {
        try {
            realMain(args);
        } catch (Throwable t) {
            Utils.unexpected(t);
        }
        System.out.println("\nPassed = " + Utils.passed + " failed = " + Utils.failed);
        if (Utils.failed > 0) {
            throw new AssertionError("Some tests failed");
        }
    }
}
