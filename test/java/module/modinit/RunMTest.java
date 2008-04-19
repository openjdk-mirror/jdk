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
 * @compile -XDignore.symbol.file RunMTest.java classp/MainX.java
 * @run main/othervm/timeout=600 RunMTest
 */

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;

import javax.tools.*;

import java.module.*;
import java.module.annotation.MainClass;

public class RunMTest {

    private final static char SEP = File.separatorChar;

    private final static String WARNING_HEADER =
        "// This is a machine generated file, do not edit\n" +
        "// Created by RunMTest v0.5\n";

    private final static String[] SP_HEADER = new String[] {
        "import java.lang.ModuleInfo.*;",
        "import java.module.annotation.*;",
        "",
    };

    private static final Map<String,String> defaultProperties;

    static {
        Properties p = System.getProperties();
        @SuppressWarnings("unchecked")
        Map<String,String> m = (Map<String,String>)(Map)p;
        defaultProperties = new HashMap<String,String>(m);
        defaultProperties.put("header", WARNING_HEADER);
    }

    private final File file;
    private final File outputDirectory;
    private final List<ModuleDescription> modules;
    private final List<TestDescription> tests;

    private RunMTest(File file, String baseDirectory) throws IOException {
        this.file = file;
        String mString = SEP + "mtest" + SEP;
        String cPath = file.getCanonicalPath();
        int k = cPath.lastIndexOf(mString);
        String subdir;
        if (k == -1) {
            subdir = file.getName();
        } else {
            subdir = cPath.substring(k + mString.length());
        }
        System.out.println(">>> Test " + subdir);
        outputDirectory = new File(baseDirectory, subdir);
        if (outputDirectory.exists()) {
            recursiveDelete(outputDirectory);
        }
        modules = new ArrayList<ModuleDescription>();
        tests = new ArrayList<TestDescription>();
        parse();
    }

    private String getName() {
        return file.getName();
    }

    private static String template;

    static String getTemplate() throws IOException {
        if (template != null) {
            return template;
        }
        File f = new File(System.getProperty("test.src", "."), "cl-template.java");
        InputStream in = new FileInputStream(f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        while (true) {
            int n = in.read(data);
            if (n < 0) {
                break;
            }
            out.write(data, 0, n);
        }
        in.close();
        byte[] b = out.toByteArray();
        template = new String(b, "UTF8");
        return template;
    }

    private void parse() throws IOException {
        InputStream in = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        while (true) {
            String line = getLine(reader);
            if (line == null) {
                break;
            }
            if (line.trim().length() == 0) {
                continue;
            }
            if (line.startsWith(">>> begin module ")) {
                ModuleDescription m = parseModule(line, reader);
                modules.add(m);
            } else if (line.startsWith(">>> begin test ")) {
                TestDescription t = parseTest(line, reader);
                tests.add(t);
            } else {
                throw new IOException("Invalid declaration: " + line);
            }
        }
        in.close();
    }

    private String getLine(BufferedReader reader) throws IOException {
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }
            if (line.startsWith("#")) {
                continue;
            }
            return line;
        }
    }

    private static void recursiveDelete(File dir) throws IOException {
        if (dir.isFile()) {
            dir.delete();
        } else if (dir.isDirectory()) {
            File[] entries = dir.listFiles();
            for (int i = 0; i < entries.length; i++) {
                if (entries[i].isDirectory()) {
                    recursiveDelete(entries[i]);
                }
                entries[i].delete();
            }
            dir.delete();
        }
    }

    private static class ModuleDescription {

        private final String name;
        private final RunMTest mTest;
        private List<String> annotations;
        private List<String> imports;
        private List<String> exports;
        private List<ClassDescription> classes;
        private List<FileDescription> otherFiles;

        private List<File> sourceFiles;

        private File moduleDir;

        private ModuleDescription(RunMTest mTest, String name) {
            this.mTest = mTest;
            this.name = name;
            annotations = new ArrayList<String>();
            imports = new ArrayList<String>();
            exports = new ArrayList<String>();
            classes = new ArrayList<ClassDescription>();
            otherFiles = new ArrayList<FileDescription>();
            sourceFiles = new ArrayList<File>();
        }

        private String getMangledName() {
            return name.replace('.', '$');
        }

        private File getModuleDir() {
            if (moduleDir == null) {
                String suffix = "";
                for (String s : annotations) {
                    if (s.startsWith("@Version(\"")) {
                        suffix = "-" + s.substring(10, s.length() - 2);
                    }
                }
                moduleDir = new File(mTest.outputDirectory, getMangledName() + suffix);
            }
            return moduleDir;
        }

        private File write() throws IOException {
            File moduledir = getModuleDir();
            File dir = new File(moduleDir, name.replace('.', SEP));
            dir.mkdirs();
            String mangledName = getMangledName();
            File spfile = new File(dir, "module_info.java");
            PrintWriter writer = new PrintWriter(spfile);
            writer.println(WARNING_HEADER);
            writer.println("package " + mangledName + ";");
            writer.println();
            for (String s : SP_HEADER) {
                writer.println(s);
            }
            for (String s : annotations) {
                writer.println(s);
            }
            writer.println("class module_info {");
            writer.println();
            for (String s : exports) {
                writer.println("    exports " + s.replace(".", "$") + ";");
            }
            writer.println();
            writer.println("}");
            writer.close();
            return spfile;
        }

    }

    private static String substituteProperties(String template, Map<String,String> props) {
        while (true) {
            int k = template.indexOf("${");
            if (k == -1) {
                break;
            }
            int e = template.indexOf("}", k);
            String name = template.substring(k + 2, e);
            String prop = props.get(name);
            if (prop == null) {
                prop = defaultProperties.get(name);
            }
            if (prop == null) {
                throw new IllegalArgumentException("Property not defined: " + name);
            }
            template = template.substring(0, k) + prop + template.substring(e + 1);
        }
        return template;
    }

    private static class ClassDescription {

        private final String name;
        private final String pkg;

        private final Map<String,String> properties;

        private ClassDescription(String line) {
            String[] ss = line.split(" ");
            String s = ss[ss.length - 1];
            int k = s.lastIndexOf('.');
            pkg = s.substring(0, k);
            name = s.substring(k + 1);
            properties = new HashMap<String,String>();
            properties.put("name", name);
            properties.put("pkg", pkg);
            properties.put("import", "");
            properties.put("super", "");
            properties.put("info", "");
            properties.put("run", "");
            properties.put("body", "");
        }

        private void updateProperty(String propertyName, String line) {
            String s = properties.get(propertyName);
            if (s == null) {
                properties.put(propertyName, line);
            } else {
                properties.put(propertyName, s + "\n" + line);
            }
        }

        private File write(ModuleDescription md) throws IOException {
            File moduledir = md.getModuleDir();
            File pkgdir = new File(moduledir, pkg.replace('.', SEP));
            pkgdir.mkdirs();
            File srcfile = new File(pkgdir, name + ".java");
            String template = getTemplate();
            String srcstring = substituteProperties(template, properties);
            OutputStream out = new FileOutputStream(srcfile);
            out.write(srcstring.getBytes("UTF8"));
            out.close();
            return srcfile;
        }

    }

    private void runTests() throws Exception {
        for (TestDescription t : tests) {
            t.runTest(this);
        }
    }

    private static class TestDescription {

        private final String name;
        private String result;

        private TestDescription(String name) {
            this.name = name;
        }

        private void runTest(RunMTest mTest) throws Exception {
            System.out.println("> Running test " + name + "...");
            Repository parent = sun.module.repository.RepositoryConfig.getSystemRepository();
            Repository repository = Modules.newLocalRepository(parent, mTest.getName(), mTest.outputDirectory);
            ModuleDefinition md = repository.find(name);
            try {
                Module m = md.getModuleInstance();
                String mainClassName = md.getAnnotation(MainClass.class).value();
                ClassLoader cl = m.getClassLoader();
                Class<?> clazz = cl.loadClass(mainClassName);
                Method method = clazz.getMethod("main", String[].class);
                method.invoke(null, (Object)new String[0]);
            } catch (Exception e) {
                if ((e instanceof InvocationTargetException) && (e.getCause() instanceof Exception)) {
                    e = (Exception)e.getCause();
                }
                if (result.equals("return")) {
                    throw new Exception("test failed", e);
                }
                if (result.startsWith("exception ")) {
                    String excname = result.substring("exception ".length());
                    String[] fqe = e.getClass().getName().split("\\.");
                    if (fqe[fqe.length - 1].equals(excname)) {
                        System.out.println("> Test completed with expected exception: " + e);
                        return;
                    }
                    throw new Exception("Expected exception " + excname, e);
                }
                throw new Exception("Unknown test result: " + result);
            }
            if (!result.equals("return")) {
                throw new Exception("Test unexpectedly returned normally");
            }
            System.out.println("> Test completed.");
        }

    }

    private TestDescription parseTest(String header, BufferedReader reader) throws IOException {
        String[] s = header.split(" ");
        String name = s[s.length - 1];
        TestDescription test = new TestDescription(name);
        while (true) {
            String line = getLine(reader);
            if (line == null) {
                throw new EOFException();
            }
            if (line.equals(">>> end test")) {
                break;
            }
            test.result = line;
        }
        return test;
    }

    private ClassDescription parseClass(String header, BufferedReader reader) throws IOException {
        ClassDescription cd = new ClassDescription(header);
        String prop = "run";
        while (true) {
            String line = getLine(reader);
            if (line == null) {
                throw new EOFException();
            }
            if (line.equals(">> end class")) {
                break;
            }
            if (line.startsWith("> ")) {
                prop = line.substring(2).trim();
                continue;
            }
            cd.updateProperty(prop, line);
        }
        return cd;
    }

    private static class FileDescription {

        final String name;
        String sourceName;

        private FileDescription(String line) {
            String[] ss = line.split(" ");
            name = ss[ss.length - 1];
        }

        private File write(ModuleDescription md) throws IOException {
            File moduledir = md.getModuleDir();
            File destFile = new File(moduledir, name.replace('/', SEP));
            destFile.getParentFile().mkdirs();
            File srcFile = new File(md.mTest.file.getParentFile(), sourceName.replace('/', SEP));
            InputStream in = new FileInputStream(srcFile);
            OutputStream out = new FileOutputStream(destFile);
            byte[] buffer = new byte[2048];
            while (true) {
                int n = in.read(buffer);
                if (n < 0) {
                    break;
                }
                out.write(buffer, 0, n);
            }
            in.close();
            out.close();
            return destFile;
        }

    }

    private FileDescription parseFile(String header, BufferedReader reader) throws IOException {
        FileDescription fd = new FileDescription(header);
        while (true) {
            String line = getLine(reader);
            if (line == null) {
                throw new EOFException();
            }
            if (line.equals(">> end file")) {
                break;
            }
            if (line.startsWith("> copy ")) {
                String name = line.substring(7).trim();
                fd.sourceName = name;
                continue;
            }
            throw new IOException("Invalid file declaration: " + line);
        }
        return fd;

    }

    private ModuleDescription parseModule(String header, BufferedReader reader) throws IOException {
        String[] s = header.split(" ");
        String name = s[s.length - 1];
        ModuleDescription md = new ModuleDescription(this, name);
        String section = "";
        while (true) {
            String line = getLine(reader);
            if (line == null) {
                throw new EOFException();
            }
            if (line.startsWith(">> begin class ")) {
                md.classes.add(parseClass(line, reader));
                continue;
            }
            if (line.startsWith(">> begin file ")) {
                md.otherFiles.add(parseFile(line, reader));
                continue;
            }
            if (line.startsWith("> ")) {
                section = line;
                continue;
            }
            if (line.equals(">>> end module")) {
                break;
            }
            if (section.equals("> annotations")) {
                md.annotations.add(line);
            } else if (section.equals("> import")) {
                md.imports.add(line);
            } else if (section.equals("> export")) {
                md.exports.add(line);
            } else {
                throw new IOException("Invalid module declaration: " + line);
            }
        }
        return md;
    }

    private void createSources() throws IOException {
        File f;
        for (ModuleDescription md : modules) {
            md.sourceFiles.clear();
            f = md.write();
            md.sourceFiles.add(f);
            for (ClassDescription c : md.classes) {
                f = c.write(md);
                md.sourceFiles.add(f);
            }
            for (FileDescription fd : md.otherFiles) {
                fd.write(md);
            }
        }
    }

    private void makeJAM() throws IOException {
        // since we may have multiple versions of the same module,
        // with types of the same name, which javac does not like, compile
        // each module separately
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StringBuilder srcpath = new StringBuilder();
        boolean first = true;
        for (ModuleDescription module : modules) {
            File moduleDir = module.getModuleDir();
            if (first) {
                first = false;
            } else {
                srcpath.append(File.pathSeparatorChar);
            }
            srcpath.append(moduleDir);
        }
        for (ModuleDescription module : modules) {
            StringBuilder srclist = new StringBuilder();
            for (File f : module.sourceFiles) {
                srclist.append(f.getPath());
                srclist.append(" ");
            }
            String cmd = "-source 6 -target 6 -XDignore.symbol.file -implicit:none -Xlint:all -sourcepath "
                + srcpath.toString() + " " + srclist;
            int r = compiler.run(null, null, null, cmd.split(" "));
            if (r != 0) {
                throw new RuntimeException("Compilation failed: " + r);
            }

            File moduleDir = module.getModuleDir();
            File moduleJam = new File(outputDirectory, moduleDir.getName() + ".jam");

            ArrayList<String> args = new ArrayList<String>();
            args.add("cfsS");
            args.add(moduleJam.getCanonicalPath());
            args.add(module.getMangledName());
            args.add(moduleDir.getCanonicalPath() + File.separator);

            // Presume all other entries are directories containing classes.
            for (File f : moduleDir.listFiles()) {
                if (!f.getName().equals("MODULE-INF")) {
                    args.add("-C");
                    args.add(moduleDir.getCanonicalPath());
                    args.add(f.getName());
                }
                else {
                    File tf = new File(moduleDir.getCanonicalPath(), "MODULE-INF" + File.separator + "legacy-classes.list");
                    if (tf.exists() && !tf.isDirectory()) {
                        args.add("-C");
                        args.add(moduleDir.getCanonicalPath());
                        args.add("MODULE-INF" + File.separator + "legacy-classes.list");
                    }
                }
            }

            sun.module.tools.Jam jam = new sun.module.tools.Jam(System.out, System.err, "RunMTest");
            jam.run(args.toArray(new String[0]));
        }
    }

    private static void findFiles(File directory, Collection<File> results) throws IOException {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                findFiles(file, results);
            }
            if (file.isFile() && file.getPath().endsWith(".mtest")) {
                results.add(file);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        if (args.length == 0) {
            args = new String[] { "." };
        }
        Set<File> testFiles = new TreeSet<File>();
        File base = new File(System.getProperty("test.src", ".") + SEP + "mtest");
        for (String arg : args) {
            File file;
            if (!arg.equals(".")) {
                file = new File(arg);
                if (file.isFile()) {
                    testFiles.add(file);
                }
                if (file.isDirectory()) {
                    findFiles(file, testFiles);
                }
            }
            file = new File(base, arg);
            if (file.isFile()) {
                testFiles.add(file);
            }
            if (file.isDirectory()) {
                findFiles(file, testFiles);
            }
        }
        for (Iterator<File> t = testFiles.iterator(); t.hasNext(); ) {
            File f = t.next();
            String fname = f.getName();
            if (fname.startsWith("s.") || fname.startsWith("p.") || fname.startsWith(",")) {
                t.remove();
            }
        }
        if (testFiles.isEmpty()) {
            throw new RuntimeException("No tests found");
        }
        String outputDir = System.getProperty("test.classes", ".") + SEP + "tmp_mtest";
        int modules = 0;
        int tests = 0;
        for (File file : testFiles) {
            RunMTest test = new RunMTest(file, outputDir);
            test.createSources();
            test.makeJAM();
            test.runTests();
            modules += test.modules.size();
            tests += test.tests.size();
        }
        long stop = System.currentTimeMillis();
        System.out.println("All tests completed ("
            + testFiles.size() + " mtest files, "
            + modules + " modules, "
            + tests + " tests, time "
            + (stop - start) + " ms)");
    }

}
