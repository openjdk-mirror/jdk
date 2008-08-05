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

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;
import javax.tools.*;
import sun.module.JamUtils;

/**
 * Creates a JAM file based on a set of properties.
 * @compile -XDignore.symbol.file JamBuilder.java
 */
public class JamBuilder {
    static final boolean DEBUG = System.getProperty("module.debug") != null;

    private static void debug(String s) {
        if (DEBUG) System.out.println("### JamBuilder: " + s);
    }

    private static void debug(String s, Object...args) {
        if (DEBUG) {
            System.out.print("### JamBuilder: " + s);
            for (Object x : args) {
                System.out.print(x + " ");
            }
            System.out.println("");
        }
    }

    /* Directory in which this builder is started. */
    private final File destDir;

    /* Values taken from args. */
    private String srcPkgName; // -k srcPkgName
    private String srcName;    // -s srcName
    private String modName;    // -m modName
    private String version;    // -v version
    private String platform;   // -p platform
    private String arch;       // -a arch
    private boolean pack;      // -P

    /* Name of an additional method, if any. */
    private String method = null;

    // Extra annotations provided by caller.
    private List<String> annotations = new ArrayList<String>();

    /* For creating source and derived files. */
    private final File tmpDir;

    /** Deletes tmpDir upon exit; enabled by default. */
    private Thread shutdownThread;

    public static void main(String[] args) {
        try {
            JamBuilder builder = new JamBuilder(
                    args,
                new File(System.getProperty("user.dir")));
            builder.createJam();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Create a JAM file based on the given args. */
    public static File createJam(
            String srcPkgName,
            String srcName,
            String modName,
            String version,
            String platform,
            String arch,
            boolean pack,
            File destDir) throws Exception {
        JamBuilder jb = new JamBuilder(
                srcPkgName,
                srcName,
                modName,
                version,
                platform,
                arch,
                pack,
                destDir);
        return jb.createJam();
    }

    public JamBuilder(String[] args, File destDir) throws Exception {
        this(destDir);

        parseArgs(args);

        createJam();
    }

    public JamBuilder(
            String srcPkgName,
            String srcName,
            String modName,
            String version,
            String platform,
            String arch,
            boolean pack,
            File destDir) throws Exception {
        this(destDir);

        this.srcPkgName = srcPkgName;
        this.srcName = srcName;
        this.modName = modName;
        this.version = version == null ? " 0.0" : version;
        this.platform = platform;
        this.arch = arch;
        this.pack = pack;
    }

    private JamBuilder(File destDir) throws IOException {
        if (!destDir.isDirectory()) {
            throw new IOException(destDir.getCanonicalPath()
                + " is not a directory or does not exist");
        }
        this.destDir = destDir;

        tmpDir = new File(
            System.getProperty("test.scratch", "."),
            "JamBuilder").getCanonicalFile();
        JamUtils.recursiveDelete(tmpDir);
        tmpDir.mkdirs();

        shutdownOnExit(true);
    }

    /** @return number of args found.
     * @throws IllegalArgumentException if an invalid flag is given.
     */
    private int parseArgs(String[] args) {
        int rc = 0;
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (s.length() == 2 && s.charAt(0) == '-') {
                switch (s.charAt(1)) {
                case 'k':
                    srcPkgName = args[++i];
                    rc += 2;
                    break;
                case 's':
                    srcName = args[++i];
                    rc += 2;
                    break;
                case 'm':
                    modName = args[++i];
                    rc += 2;
                    break;
                case 'v':
                    version = args[++i];
                    rc += 2;
                    break;
                case 'p':
                    platform = args[++i];
                    rc += 2;
                    break;
                case 'a':
                    arch = args[++i];
                    rc += 2;
                    break;
                case 'P':
                    pack = true;
                    rc++;
                    break;
                default:
                    throw new IllegalArgumentException("unrecognized flag: " + args[i]);
                }
            }
        }
        return rc;
    }

    public void setSrcPkgName(String srcPkgName) {
        this.srcPkgName = srcPkgName;
    }

    public void setSrcName(String srcName) {
        this.srcName = srcName;
    }

    public void setModName(String modName) {
        this.modName = modName;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public void setPack(boolean pack) {
        this.pack = pack;
    }

    public void setMethod(String name) {
        method = name;
    }

    public void addAnnotation(String a) {
        annotations.add(a);
    }

    public void clearAnnotations() {
        annotations.clear();
    }

    public File createJam() throws Exception {
        PrintWriter pw;

        // Create source files with the module-info.java and main
        File srcDir = new File(tmpDir, srcPkgName);
        JamUtils.recursiveDelete(srcDir);
        srcDir.mkdirs();
        File modFile = new File(srcDir, JamUtils.MODULE_INFO_JAVA);
        File srcFile = new File(srcDir, srcName + ".java");

        createSrcFiles(modFile, srcFile);

        // Compile the source files
        compileFile(modFile);
        compileFile(srcFile);

        File contentDir = setupContent(modFile, srcFile);

        // Create the JAM
        File jamFile = createJamFile(contentDir);
        if (DEBUG) debug("createJam jamFile: " + jamFile.getCanonicalPath());


        // Pack it if necessary
        if (pack) {
            File tmp = packJam(jamFile);
            jamFile.delete();
            jamFile = tmp;
        }

        // Cleanup
        JamUtils.recursiveDelete(tmpDir);
        return jamFile;
    }

    public void shutdownOnExit(boolean value) {
        // Create/destroy shutdownThread based on given value.
        if (value) {
            if (shutdownThread == null) {
                shutdownThread = Executors.defaultThreadFactory().newThread(
                    new Runnable() {
                        public void run() {
                            try {
                                JamUtils.recursiveDelete(tmpDir);
                            } catch (IOException ex) {
                                if (DEBUG) debug("Can't delete " + tmpDir);
                            }
                        }
                    });
                Runtime.getRuntime().addShutdownHook(shutdownThread);
            }
        } else {
            if (shutdownThread != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownThread);
                shutdownThread = null;
            }
        }
    }

    /**
     * Takes a .jam file and creates the corresponding .jam.pack.gz.
     * @param jamName basename of the JAM file
     * @param jamFile the .jam file for which the .jam.pack.gz gets created
     */
    private File packJam(File jamFile) throws IOException {
        File jamDir = jamFile.getParentFile();
        Packer packer = Pack200.newPacker();
        File gzFile = new File(jamDir, jamFile.getName() + ".pack.gz");
        OutputStream gzos = new BufferedOutputStream(
            new GZIPOutputStream(
                new FileOutputStream(gzFile), JamUtils.BUFFER_SIZE));
        packer.pack(
                new JarFile(jamFile),
            gzos);
        gzos.close();
        return gzFile;
    }

    private File createJamFile(File contentDir) throws IOException {
        String binding =
            platform == null ? "" : "-" + platform + "-" + arch;
        File rc = new File(destDir,
                           modName + "-" + version + binding + ".jam");
        String cmd = (DEBUG) ? "cvf" : "cf";
        String[] args = new String[] {
            cmd,
            rc.getCanonicalPath(),
            "-C",
            contentDir.getCanonicalPath(),
            srcPkgName
        };
        sun.module.tools.Jam jamtool =
            new sun.module.tools.Jam(System.out, System.err, "JamBuilder");
        debug("jam args: ", (java.lang.Object[])args);
        boolean status = jamtool.run(args);
        if (DEBUG) {
            debug("+module listing: " + rc.getAbsolutePath());
            jamtool.run("pf", rc.getAbsolutePath());
        }
        return (status) ? rc : null;
    }

    /* Create a module's directory structure like this:
     * <dir-path>
     *    urlrepotest
     *        Sample.class
     *        module-info.class
     * <path>/<name>-<version>[-<platform>-<arch>].jam
     * where dir-path depends on version, platform, and name: If path
     * is given, then dir-path is path.  If platform is given, then it
     * is name/version/platform.  Otherwise it is name/platform.
     *
     */
    private File setupContent(File modFile, File srcFile) throws IOException {
        File contentDir = new File(tmpDir, "JamBuilderContent");

        debug("content dir.mkdirs returns " + contentDir.mkdirs());
        File pkgDir = new File(contentDir, srcPkgName);
        pkgDir.mkdirs();
        JamUtils.copyFile(new File(srcFile.getParent(), srcName + ".class"),
                 new File(pkgDir, srcName + ".class"));
        JamUtils.copyFile(new File(modFile.getParent(), JamUtils.MODULE_INFO_CLASS),
                 new File(pkgDir, JamUtils.MODULE_INFO_CLASS));


        return contentDir;
    }

    private void createSrcFiles(File modFile, File srcFile) throws Exception {
        PrintWriter pw;

        pw = new PrintWriter(new FileWriter(modFile));
        pw.printf("@Version(\"%s\")\n", version);
        pw.printf("@MainClass(\"%s.%s\")\n", srcPkgName, srcName);
        if (platform != null && arch != null) {
            pw.printf("@PlatformBinding(platform=\"%s\", arch=\"%s\")\n", platform, arch);
        }
        for (String a : annotations) {
            pw.printf(a);
        }
        pw.printf("@ImportModules({\n");
        pw.printf("\t@ImportModule(name=\"java.se\")\n");
        pw.printf("})\n");
        pw.printf("module " + modName + ";");
        pw.close();
        if (pw.checkError()) {
            throw new Exception("Failed to write module");
        }

        pw = new PrintWriter(new FileWriter(srcFile));
        pw.printf("module " + modName + ";");
        pw.printf("package %s;\n\n", srcPkgName);
        pw.printf("import java.util.*;\n\n");
        pw.printf("public class %s {\n", srcName);
        pw.printf("    public static void main(String[] args) {\n");
        pw.printf("        System.out.println(\"%s.%s: Args:\" + Arrays.toString(args));\n", srcPkgName, srcName);
        pw.printf("    }\n");

        if (method != null) {
            pw.printf("    public static String " + method + "() {\n");
            pw.printf("        return \"" + method + "\";\n");
            pw.printf("    }\n");
        }
        pw.printf("}\n");
        pw.close();
        if (pw.checkError()) {
            throw new Exception("Failed to write super package");
        }
    }

    public static void compileFile(File src) throws Exception {
        compileFile(src, null);
    }

    public static void compileFile(File src, File dest) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String cmd = "-source 7 -implicit:none";
        if (DEBUG) {
            cmd += " -verbose";
        }
        if (dest != null) {
            cmd += " -d " + dest.getCanonicalPath();
        }
        cmd += " " + src.getCanonicalPath();
        if (DEBUG) {
              debug("javac args: " + cmd);
        }
        int rc = compiler.run(null, null, null, cmd.split(" "));
        if (rc != 0) {
            throw new Exception("Failed to compile " + src);
        }
    }
}
