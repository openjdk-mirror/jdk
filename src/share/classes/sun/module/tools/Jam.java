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
import java.io.OutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;
import java.util.zip.GZIPOutputStream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import sun.module.JamUtils;
import sun.module.tools.util.JamModuleMetadata;
import sun.module.ModuleParsingException;
import sun.module.tools.util.JamToolUtils;
import sun.tools.jar.Main;
import sun.tools.jar.CommandLine;

/**
 * Prototype of the Jam packaging tool.
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
    private String fname, tfname;
    private List<String> jarArgs;
    private boolean printModuleInfo;
    private boolean verbose;

    public Jam(PrintStream out, PrintStream err, String program) {
        this.out = out;
        this.err = err;
        this.program = program;
    }

    /*
     * Starts main program with the specified arguments.
     */
    public synchronized boolean run(String...args) {
        /*
         * initialize instance variables - to avoid side
         * effects if run is executed more than once.
         */
        moduleName = null;
        fname = null;
        tfname = null;
        jarArgs = null;

        if (!parseArgs(args)) {
            return false;
        }
        if (printModuleInfo) {
            try {
                if (new File(fname).exists()) {
                    output(JamModuleMetadata.printDiagnostics(fname, verbose));
                    return true;
                }
                error(Messenger.formatMsg("error.file.notfound", fname));
                return false;
            } catch (IOException ex) {
                fatalError(ex);
            } catch (ModuleParsingException ex) {
                fatalError(ex);
            }
        }
        // construct final arguments to JAR
        String[] argsArray = jarArgs.toArray(new String[jarArgs.size()]);
        JamModuleMetadata metadata = null;
        if (moduleName != null) {
            try {
                metadata = new JamModuleMetadata(createMetadata());
            } catch (IOException ex) {
                fatalError(ex);
            } catch (ModuleParsingException ex) {
                fatalError(ex);
            }
        }
        // call JAR
        Main jar = new Main(out, err, program);
        if (jar.jamRun(metadata, argsArray) == false) {
            return false;
        }

        // if output file does not need to be packed, done!
        if (tfname == null)
            return true;

        // otherwise, compress output file with pack200-gzip
        JarInputStream is = null;
        OutputStream os = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(tfname);
            is = new JarInputStream(new BufferedInputStream(fis));
            os = new GZIPOutputStream(new FileOutputStream(fname), 8192);
            Packer packer = Pack200.newPacker();
            packer.pack(is, os);
        } catch (IOException e) {
            fatalError(e);
            return false;
        } finally {
            JamUtils.close(is);
            JamUtils.close(fis);
            JamUtils.close(os);
            new File(tfname).delete();
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
            fatalError(Messenger.formatMsg("error.cant.open", e.getMessage()));
            return false;
        } catch (IOException e) {
            fatalError(e);
            return false;
        }
        /*
         * Parse the args to make sure that the module is specified and only
         * 'jar' options supported by 'jam' are listed. Also, build the
         * arguments to pass to 'jar' by filtering out the module arguments.
         */
        jarArgs = new ArrayList<String>();
        StringBuilder jarFlags = new StringBuilder();
        int count = 1;
        try {
            String flags = args[0];
            if (flags.startsWith("-")) {
                flags = flags.substring(1);
            }
            for (int i = 0; i < flags.length(); i++) {
                char ch = flags.charAt(i);
                switch (ch) {
                    case 'p':
                        printModuleInfo = true;
                        break;
                    case 'x':
                    case 't':
                    case 'c':
                        break;
                    case 'v':
                        verbose = true;
                        break;
                    case 'f':
                        fname = args[count++];
                        break;
                    case '0':
                        // ignore here, passed to jar
                        break;
                    default:
                        error(Messenger.formatMsg("error.invalid.option",
                                String.valueOf(ch)));
                        usageError();
                        return false;
                }
                if (ch != 'p') {
                    jarFlags.append(ch);
                }
            }

            // the other args needed for jam
            for( ; count < args.length ; count++) {
                String opt = args[count];
                if (opt.startsWith("-N")) {
                   moduleName = checkValue("module", args[++count]);
                   continue;
                }
                jarArgs.add(opt);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            usageError();
            return false;
        }
        if (printModuleInfo == true && fname != null) {
            return true;
        }

        if (moduleName != null) {
            if (fname != null) {
                error(Messenger.formatMsg("error.module.filename.conflict"));
                usageError();
                return false;
            }
            jarFlags = jarFlags.append("f");
            fname = moduleName + ".jam";
        } else if (fname == null) {
            usageError();
            return false;
        }

        // checks file extensions
        if ((fname.endsWith(".jar")
            || fname.endsWith(".jam")
            || fname.endsWith(".jam.pack.gz")) == false) {
            error(Messenger.formatMsg("error.file.suffix"));
            return false;
        }

        jarArgs.add(0,jarFlags.toString());
        if (fname.endsWith(".jam.pack.gz")) {
            File tmpFile = null;
            try {
                File baseDir = new File(fname).getCanonicalFile().getParentFile();
                tmpFile = File.createTempFile(program, ".tmp", baseDir);
                tfname = tmpFile.getCanonicalPath();
            } catch (IOException ex) {
                fatalError(Messenger.formatMsg("error.file.create", tmpFile.getAbsolutePath()));
                return false;
            }
            jarArgs.add(1, tfname);
        } else {
            jarArgs.add(1, fname);
        }
        for (int i = count; i < args.length; i++) {
            jarArgs.add(args[i]);
        }
        return true;
    }

    private String checkValue(String value, String in) {
        if (in.startsWith("-")) {
            error(Messenger.formatMsg("error.invalid.value", value));
            usageError();
        }
        return in;
    }
    /*
     * Print usage message.
     */
    private void usageError() {
        error(program + ":" + Messenger.formatMsg("error.usage",
                JamUtils.MODULE_INF_METADATA, JamUtils.MODULE_INFO_CLASS));
        System.exit(1);
    }

    /*
     * A fatal exception has been caught.  No recovery possible
     */
    private void fatalError(Exception e) {
        error(program + ":" + Messenger.formatMsg("error.fatal.0"));
//        e.printStackTrace(); // for debugging, FIXME
        System.exit(1);
    }

    /*
     * A fatal condition has been detected; message is "s".
     * No recovery possible
     */
    private void fatalError(String msg) {
        error(program + ":" + msg);
        System.exit(1);
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

    /**
     * creates a basic metadata file with the given parameters
     * @param module, a module name
     * @return the metadata as a byte array
     * @throws java.io.IOException
     */
    private byte[] createMetadata() throws IOException {
        File tmpDir = null;
        FileInputStream fis = null;
        PrintStream mstream = null;
        byte[] oarray = null;
        try {
            StringBuilder s = new StringBuilder();
            /*
             * note at least one annotation is required for the generation of a
             * class file. So insert only the absolutely required annotation.
             */
            s = s.append("@ImportModules({\n");
            s = s.append("@ImportModule(name=\"java.se\", version=\"1.7+\")\n");
            s = s.append("})\n");
            s = s.append("module " + moduleName + ";\n");

            tmpDir = JamUtils.createTempDir();
            File javaFile = new File(tmpDir, JamUtils.MODULE_INFO_JAVA);
            mstream = new PrintStream(javaFile);
            mstream.print(s.toString());
            mstream.close();
            String[] args = {"-source", "7", javaFile.getAbsolutePath()};
            /*
             * note we need to use JavaCompiler as this class is in jte jre,
             * do not use com.sun.tools.javac.Main.compile(..) entry poing, then
             * all the  classloading at run time will be a-ok.
             */
            JavaCompiler  compiler = ToolProvider.getSystemJavaCompiler();
            int rc = compiler.run(null, null, null, args);
            if (rc != 0) {
                throw new IOException(Messenger.formatMsg("error.file.create",
                        "metadata file"));
            }

            fis = new FileInputStream(new File(tmpDir, JamUtils.MODULE_INFO_CLASS));
            oarray = JamUtils.getInputStreamAsBytes(fis);
            return  oarray;
        } finally {
            if (mstream != null) {
                mstream.close();
            }
            if (fis != null) {
                fis.close();
            }
            if (tmpDir != null) {
                JamUtils.recursiveDelete(tmpDir);
            }
        }
    }

    public static void main(String[] args) {
        Jam jam = new Jam(System.out, System.err, "jam");
        System.exit(jam.run(args) ? 0 : 1);
    }
}
