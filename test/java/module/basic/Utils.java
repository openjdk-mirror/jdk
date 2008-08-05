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

/*
 * Utils.java
 */
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class Utils {

    final static String SEP         = File.separator;
    final static String JAVA_EXT    = ".java";
    final static String CLASS_EXT   = ".class";
    final static String JAM_EXT     = ".jam";
    final static String JAR_EXT     = ".jar";
    final static String PACK_EXT    = ".pack.gz";
    final static String JAM_VERSION = "1.0.0.0";
    final static String JAM_SEP     = "-";
    final static String REPO_XML    = "repository-metadata.xml";

    final static String REPODIRNAME = "repodir";

    final static String T1_JAM      = "t1" + JAM_EXT;
    final static String T1_PACK     = "t1" + JAM_EXT + PACK_EXT;

    static final String TESTJAVA = System.getProperty("java.home");

    final static String[] JAVAC_OPTS = new String[]{
        "-source",
        "7",
        "-implicit:none",
        "-Xlint:all",
        "-XDignore.symbol.file"
    };

    private static JavaCompiler compiler;

    static {
        compiler = ToolProvider.getSystemJavaCompiler();
    }
    // All static
    private Utils() {}

    // Simple single file compiler
    static void compileFile(String srcPath) throws IOException {
        String[] args = Arrays.copyOf(JAVAC_OPTS, JAVAC_OPTS.length + 1);
        args[JAVAC_OPTS.length] = srcPath;
        Utils.abort(compiler.run(null, null, null, args) == 0,
              "Single-file compilation failed");
    }

    // Compiles the modules hierachy
    static void compileFiles(File dir, String... mods) throws IOException {
        List<String> args = new ArrayList<String>(Arrays.asList(JAVAC_OPTS));
        for (String mod : mods) {
            File[] pkgDirs = new File(dir, mod).listFiles(Utils.DIRECTORY_FILTER);
            for (File pkgDir : pkgDirs) {
                for (File srcFile : pkgDir.listFiles(Utils.JAVA_FILTER)) {
                    args.add(srcFile.getAbsolutePath());
                }
            }
        }
        String[] a = args.toArray(new String[args.size()]);
        Utils.abort(compiler.run(null, null, null, a) == 0,
              "Multi-file compilation failed");
    }

    /*
     * finds files in the start directory using the the filter, appends
     * the files to the dirList.
     */
    static void findFiles(File startDir, List<String> dirList, FileFilter filter)
            throws IOException {
        File[] foundFiles = startDir.listFiles(filter);
        for (File f : foundFiles) {
            dirList.add(f.toString());
        }
        File[] dirs = startDir.listFiles(DIRECTORY_FILTER);
        for (File dir : dirs) {
            findFiles(dir, dirList, filter);
        }
    }

    // Compiles all the java files in a directory.
    static void compileFiles(File dir) throws IOException {
        List<String> args = new ArrayList<String>(Arrays.asList(JAVAC_OPTS));
        findFiles(dir, args, Utils.JAVA_FILTER);
        Utils.abort(compiler.run(null, null, null,
                args.toArray(new String[args.size()])) == 0,
                "Recursive compilation failed");
    }

    static String baseName(File f) {
        return baseName(f.getName());
    }

    static String baseName(String filename) {
        return filename.substring(0,filename.lastIndexOf("."));
    }

    static final FileFilter CLASS_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(CLASS_EXT);
        }
    };

    static final FileFilter JAVA_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(JAVA_EXT);
        }
    };

    static final FileFilter JAM_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(JAM_EXT);
        }
    };

    static final FileFilter DIRECTORY_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };

    static void makeJar(File srcDir, File dst) {
        makeJar(srcDir, dst, null);
    }

    // Makes a jar file in dst with contents that are in srcDir.
    static void makeJar(File srcDir, File dst, String entry) {
        sun.tools.jar.Main jartool =
                new sun.tools.jar.Main(System.out, System.err, "JarCreator");
        String[] args;
        if (entry != null) {
            args = new String[] {
                "cfe",
                dst.getAbsolutePath(),
                entry,
                "-C",
                srcDir.getAbsolutePath(),
                "."
            };
        } else {
            args = new String[]{
                "cf",
                dst.getAbsolutePath(),
                "-C",
                srcDir.getAbsolutePath(),
                "."
            };
        }
        abort(jartool.run(args), "JarCreator Failed") ;
    }

    static boolean makeJar(String...args) {
        sun.tools.jar.Main jartool =
                new sun.tools.jar.Main(System.out, System.err, "JarCreator");
        return jartool.run(args);
    }

    static boolean makeJar(List<String> args) {
        return makeJar(args.toArray(new String[args.size()]));
    }

    static void makeJam(File srcDir, File dst) {
        sun.module.tools.Jam jamtool =
                new sun.module.tools.Jam(System.out, System.err, "JamCreator");
        String[] args = new String[]{
            "cf",
            dst.getAbsolutePath(),
            "-C",
            srcDir.getAbsolutePath(),
            "."
        };
        abort(jamtool.run(args), "JamCreator");
    }

    static boolean makeJam(String...args) {
        sun.module.tools.Jam jamtool =
                new sun.module.tools.Jam(System.out, System.err, "JamCreator");
        return jamtool.run(args);
    }

    static boolean makeJam(List<String> args) {
        return makeJam(args.toArray(new String[args.size()]));
    }

    /*
     * given a File (ZipFile) in,  extracts name to the output file
     */
    static void extractFileTo(File in, String name, File outFile) throws IOException {
        ZipFile z = new ZipFile(in);
        for (ZipEntry ze : Collections.list(z.entries())) {
            if (ze.getName().equals(name)) {
                byte buf[] = new byte[8192];
                InputStream is = z.getInputStream(ze);
                int n = is.read(buf);
                FileOutputStream fos = new FileOutputStream(outFile);
                while (n > 0) {
                    fos.write(buf, 0, n);
                    n = is.read(buf);
                }
                is.close();
                fos.close();
                z.close();
                return;
            }
        }
        z.close();
    }

    // A very fast copying method for files, takes advantage of the OS.
    static void copyFile(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            dst.mkdirs();
            return;
        } else {
            File baseDirFile = dst.getParentFile();
            if (!baseDirFile.exists()) baseDirFile.mkdirs();
        }

        FileChannel srcChannel = (new FileInputStream(src)).getChannel();
        FileChannel dstChannel = (new FileOutputStream(dst)).getChannel();

        long retval = srcChannel.transferTo(0, src.length(), dstChannel);
        if (src.length() != dst.length()) {
            throw new IOException("file copy failed for " + src);
        }
    }

    static void recursiveDelete(File dir) throws IOException {
        if (dir.isFile()) {
            dir.delete();
        } else if (dir.isDirectory()) {
            File[] entries = dir.listFiles();
            for (File entry : entries) {
                if (entry.isDirectory()) {
                    recursiveDelete(entry);
                }
                entry.delete();
            }
            dir.delete();
        }
    }

    static File[] getDirs(String dir) {
        return getDirs(new File(dir));
    }

    static File[] getDirs(File dirFile) {
        return dirFile.listFiles(DIRECTORY_FILTER);
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
}
