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

package sun.module.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.jar.JarInputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;
import java.util.zip.GZIPOutputStream;
import java.text.MessageFormat;
import sun.module.JamUtils;
import sun.tools.jar.Main;
import sun.tools.jar.CommandLine;

/**
 * Prototype of the Jam packaging tool.
 *
 * So far, the only feature of 'jam' not supported by the 'jar' tool is the
 * copying of the compiled module-info file to MODULE-INF/MODULE.METADATA.
 * This will likely change in the full implementation.
 *
 * This prototype implementation consists of some command line parsing code
 * (copied from jar), plus a bit of code to deal with the module-info file
 * and to call jar. This may have to be refactored.
 *
 * @since 1.7
 */
public final class Jam {

    private final PrintStream out, err;
    private final String program;

    private String moduleName;
    private File moduleInfoDir;
    private String fname, tfname;
    private List<String> jarArgs;

    public Jam(PrintStream out, PrintStream err, String program) {
        this.out = out;
        this.err = err;
        this.program = program;
    }

    /*
     * Starts main program with the specified arguments.
     */
    public synchronized boolean run(String args[]) {

        // initialize instance variables - to avoid side
        // effects if run is executed more than once.
        moduleName = null;
        moduleInfoDir = null;
        fname = null;
        tfname = null;
        jarArgs = null;

        if (!parseArgs(args)) {
            return false;
        }
        // generate module filename if not specified
        File moduleInfoFile = null;
        if (moduleInfoDir == null) {
            moduleInfoFile = new File(moduleName.replace('.', File.separatorChar)
                                  + File.separatorChar + "module_info.class");
        }
        else {
            moduleInfoFile = new File(moduleInfoDir,
                                    moduleName.replace('.', File.separatorChar)
                                    + File.separatorChar + "module_info.class");
        }

        // verify the specified module exists
        if (moduleInfoFile.isFile() == false)  {
            error("Module not found: " + moduleInfoFile);
            return false;
        }
        // copy module-info file to MODULE-INF/MODULE.METADATA
        InputStream fin = null;
        OutputStream fout = null;
        File tmpDir;
        try {
            // create temp directory
            tmpDir = new File(JamUtils.createTempDir(),
                              "jamtmp-" + System.currentTimeMillis());
            tmpDir.mkdirs();
            final File deleteThisDir = tmpDir;
            Runtime.getRuntime().addShutdownHook(
                Executors.defaultThreadFactory().newThread(
                    new Runnable() {
                        public void run() {
                            try {
                                JamUtils.recursiveDelete(deleteThisDir);
                            } catch (IOException ex) {
                                // XXX log this exception
                            }
                        }
                    }));

            // copy module-info file into MODULE-INF/MODULE.METADATA
            fin = new FileInputStream(moduleInfoFile);
            File moduleInfoDir = new File(tmpDir, "MODULE-INF");
            moduleInfoDir.mkdirs();
            moduleInfoDir.deleteOnExit();
            File metadataFile = new File(moduleInfoDir, "MODULE.METADATA");
            fout = new FileOutputStream(metadataFile);
            metadataFile.deleteOnExit();
            byte[] buffer = new byte[2048];
            while (true) {
                int n = fin.read(buffer);
                if (n < 0) {
                    break;
                }
                fout.write(buffer, 0, n);
            }

            // if the output file should be pack200-gzipped, generate the JAR
            // as a temp file.
            if (fname.endsWith(".jam.pack.gz")) {
                File tmpOutputFile = File.createTempFile("output", ".jam", tmpDir);
                tmpOutputFile.deleteOnExit();
                tfname = tmpOutputFile.getCanonicalPath();

                // replaces output file in jar tool's arguments
                jarArgs.remove(1);
                jarArgs.add(1, tfname);
            }
        } catch (IOException e) {
            fatalError(e);
            return false;
        } finally {
            JamUtils.close(fin);
            JamUtils.close(fout);
        }
        // construct final arguments to JAR
        jarArgs.add("-C");
        jarArgs.add(tmpDir.getAbsolutePath());
        jarArgs.add("MODULE-INF");
        String[] argsArray = jarArgs.toArray(new String[0]);

        // call JAR
        Main jar = new Main(out, err, program);
        if (jar.run(argsArray) == false) {
            return false;
        }

        // if output file does not need to be packed, done!
        if (tfname == null)
            return true;

        // otherwise, compress output file with pack200-gzip
        JarInputStream is = null;
        OutputStream os = null;
        try {
            is = new JarInputStream(new BufferedInputStream(
                                        new FileInputStream(tfname)));
            os = new GZIPOutputStream(new FileOutputStream(fname), 8192);
            Packer packer = Pack200.newPacker();
            packer.pack(is, os);
        } catch (IOException e) {
            fatalError(e);
            return false;
        } finally {
            JamUtils.close(is);
            JamUtils.close(os);
        }

        return true;
    }

    /*
     * Parse command line arguments.
     */
    private boolean parseArgs(String args[]) {
        /* Preprocess and expand @file arguments */
        try {
            args = CommandLine.parse(args);
        } catch (FileNotFoundException e) {
            fatalError(formatMsg("error.cant.open", e.getMessage()));
            return false;
        } catch (IOException e) {
            fatalError(e);
            return false;
        }
        // Parse the args to make sure that the module is specified and only
        // 'jar' options supported by 'jam' are listed. Also, build the
        // arguments to pass to 'jar' by filtering out the module arguments.
        jarArgs = new ArrayList<String>();
        StringBuilder jarFlags = new StringBuilder();
        int count = 1;
        try {
            String flags = args[0];
            if (flags.startsWith("-")) {
                flags = flags.substring(1);
            }
            boolean cflag = false;
            for (int i = 0; i < flags.length(); i++) {
                char ch = flags.charAt(i);
                switch (ch) {
                case 'c':
                    break;
                case 'v':
                    // ignore here, passed to jar
                    break;
                case 'f':
                    fname = args[count++];
                    break;
                case 's':
                    moduleName = args[count++];
                    ch = '\0';
                    break;
                case 'S':
                    moduleInfoDir = new File(args[count++]);
                    ch = '\0';
                    break;
                case '0':
                    // ignore here, passed to jar
                    break;
                default:
                    error(formatMsg("error.illegal.option",
                                String.valueOf(ch)));
                    usageError();
                    return false;
                }
                if (ch != '\0') {
                    jarFlags.append(ch);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            usageError();
            return false;
        }
        if (fname == null || moduleName == null) {
            usageError();
            return false;
        }
        // module name must not contain file separator
        if (moduleName.indexOf('/') != -1 || moduleName.indexOf('\\') != -1) {
            error(program + ": module name must not contain \\ or / character.");
            return false;
        }
        // checks file extensions
        if ((fname.endsWith(".jar")
            || fname.endsWith(".jam")
            || fname.endsWith(".jam.pack.gz")) == false) {
            error(program + ": jam filename must end with .jar, .jam, or .jam.pack.gz extension.");
            return false;
        }

        jarArgs.add(jarFlags.toString());
        jarArgs.add(fname);
        for (int i = count; i < args.length; i++) {
            jarArgs.add(args[i]);
        }
        return true;
    }

    private String getMsg(String key) {
        return key;
//      try {
//          return (rsrc.getString(key));
//      } catch (MissingResourceException e) {
//          throw new Error("Error in message file");
//      }
    }

    private String formatMsg(String key, String arg) {
        String msg = getMsg(key);
        String[] args = new String[1];
        args[0] = arg;
        return MessageFormat.format(msg, (Object[]) args);
    }

    /*
     * Print usage message.
     */
    private void usageError() {
        error("Usage: jam cfs[v0S] jam-file module-name [module-info-dir] [-C dir] files ...");
        error("Options:");

        error("    -c  create new module archive");
        error("    -v  generate verbose output on standard output");
        error("    -0  store only; use no ZIP compression");
        error("    -f  specify module archive file name");
        error("    -s  specify module name");
        error("    -S  specify where to find module-info file");
        error("    -C  change to the specified directory and include the following file");
        error("If any file is a directory then it is processed recursively.");
        error("");
        error("Example 1: to archive the hello module and its class files into a module");
        error("           archive called 'hello.jam':");
        error("       jam cvfs hello.jam hello hello/*.class");
        error("");
        error("Example 2: to archive the hello module, its class files, and all files in");
        error("           the foo/ directory into a module archive called 'hello.jam':");
        error("       jam cvfs hello.jam hello hello/*.class -C foo/ .");
        error("");
        error("Example 3: to find the module-info file from the foo/ directory and to");
        error("           archive the hello module and its class files into a module archive");
        error("           called 'hello.jam':");
        error("       jam cvfsS hello.jam hello foo/ hello/*.class");
        error("");
        error("Example 4: to archive the hello module and its class files into a module");
        error("           archive compressed with pack200-gzip called 'hello.jam.pack.gz':");
        error("       jam cvfs hello.jam.pack.gz hello hello/*.class");
    }

    /*
     * A fatal exception has been caught.  No recovery possible
     */
    private void fatalError(Exception e) {
        e.printStackTrace();
    }

    /*
     * A fatal condition has been detected; message is "s".
     * No recovery possible
     */
    private void fatalError(String s) {
        error(program + ": " + s);
    }

    /**
     * Print an output message; like verbose output and the like
     */
    private void output(String s) {
        out.println(s);
    }

    /**
     * Print an error mesage; like something is broken
     */
    private void error(String s) {
        err.println(s);
    }

    public static void main(String[] args) {
        Jam jam = new Jam(System.out, System.err, "jam");
        System.exit(jam.run(args) ? 0 : 1);
    }

}
