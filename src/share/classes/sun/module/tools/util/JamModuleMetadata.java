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
package sun.module.tools.util;

import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Attributes;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ClassTranslator;
import com.sun.tools.classfile.ClassWriter;
import com.sun.tools.classfile.ConstantPool.CPInfo;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.ModuleExportTable_attribute;
import com.sun.tools.classfile.ModuleMemberTable_attribute;
import com.sun.tools.classfile.Module_attribute;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import sun.module.JamUtils;
import sun.module.MetadataParser;
import sun.module.ModuleParsingException;
import sun.module.tools.Messenger;


/**
 * This class is the main helper class for the jar and jam utility tools,
 * which constructs and prepares a module metdata files, based on the
 * rules by analyzing the classes or Jars fed to it.
 */
public class JamModuleMetadata extends ModuleMetadata {
    private Set<String> moduleExportClassList;
    private Set<String> moduleExportPackageList;
    private Set<String> moduleMemberPackageList;
    private Set<UnexportedClass> moduleUnexportedClassList;
    // the modified module_info
    private ClassFile moduleInfoClassFile;
    private boolean done = false;

   /**
    * Construct an object from a JAR-file, based on {@code analyze}, if true
    * will completely build an unmodifiable object, if it is set to false then,
    * analyzeClass can be called to incrementally to analyze and update the
    * metadata object. This method is called and required for jar update logic.
    *
    * @param jarFile
    * @param analyze
    * @throws java.io.IOException
    * @throws sun.module.ModuleParsingException
    */
    public JamModuleMetadata(File jarFile, boolean analyze)
            throws IOException, ModuleParsingException {
        super(jarFile);
        initSets();
        if (analyze) {
            processJar(jarFile, true);
        }
    }

    public JamModuleMetadata(String jarFileName, boolean analyze)
            throws IOException, ModuleParsingException {
        this(new File(jarFileName), analyze);
    }
    /**
     * Construct an object with a given skeletal module metadata file, this
     * typically created by the JAM tool, as an ease of use feature for simple
     * modules.
     * @param metadata a metadata files
     * @throws sun.module.ModuleParsingException
     */
    public JamModuleMetadata(byte[] metadata) throws ModuleParsingException {
        super(metadata);
        initSets();
    }

    /**
     * Contruct an object with an InputStream of a ModuleMetaData
     * file this is used by the Jar tool to create a JamModuleMetaData file and
     * incrementally update, by calling analyzeClass methods.
     * @param is
     * @throws java.io.IOException
     * @throws sun.module.ModuleParsingException
     */
    public JamModuleMetadata(InputStream is)
            throws IOException, ModuleParsingException {
        super(is);
        initSets();
    }

   /*
    * Construct an unmodifiable object from a JAR-file, it is used internally
    * for analyzing an entire jar, solely for diagnostic purposes.
    * @param jarFileName
    * @param analyze
    * @throws java.io.IOException
    * @throws sun.module.tools.util.ModuleParsingException
    */
    private JamModuleMetadata(File jarFile)
            throws IOException, ModuleParsingException {
        super(jarFile);
        initSets();
        processJar(jarFile, false);
    }

    private void initSets() {
        moduleExportClassList = new LinkedHashSet<String>();
        moduleExportPackageList = new LinkedHashSet<String>();
        moduleMemberPackageList = new LinkedHashSet<String>();
        moduleUnexportedClassList = new LinkedHashSet<UnexportedClass>();
    }
    /**
     * Writes the newly created module metadata file to a desired
     * OutputStream, once this method is called this object can no
     * longer be modified.
     * @param os
     * @throws java.io.IOException
     * @throws sun.module.ModuleParsingException
     */
    public void writeTo(OutputStream os)
            throws IOException, ModuleParsingException {
        if (moduleInfoClassFile == null) {
            finish();
        }
        ClassWriter c = new ClassWriter();
        c.write(moduleInfoClassFile, os);
    }

    /**
     * Returns the new created module meta data file as an @see InputStream,
     * once this method is called the object can no longer be modified.
     * @return {@link InputStream}
     * @throws java.io.IOException
     * @throws sun.module.ModuleParsingException
     */
    public InputStream getInputStream()
            throws IOException, ModuleParsingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeTo(baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Analyze a class for module information.
     * @param is
     * @throws java.io.IOException
     * @throws sun.module.ModuleParsingException
     */
    public void analyzeClass(InputStream is)
            throws IOException, ModuleParsingException {
        analyzeClass(is, null);
    }

    private void analyzeClass(InputStream is, UnexportedClass uc)
            throws IOException, ModuleParsingException {
        if (done) {
            throw new ModuleParsingException(Messenger.getMsg(
                    "error.module.parsing.done"));
        }
        try {
            ClassFile cf = ClassFile.read(is, afactory);
            String cls = cf.getName();
            String member = getMemberPackage(cf);
            if (member != null) {
                moduleMemberPackageList.add(member);
            }
            if (canExportClass(cf, uc)) {
                moduleExportClassList.add("L" + cls + ";");
                moduleExportPackageList.add(MetadataParser.getPackageName(
                        cls.replace("/", ".")));
            }
        } catch (ConstantPoolException cpe) {
            throw new ModuleParsingException(cpe.getMessage());
        }
    }

    /**
     * Analyze a class for module information, if the File is JarFile then
     * the contents of the JarFile will be analyzed, each entry in the archive
     * will be treated as if it is any other class.
     * @param in
     * @throws sun.module.ModuleParsingException
     * @throws java.io.IOException
     */
    public void analyzeClass(File in)
            throws ModuleParsingException, IOException {
        if (done) {
            throw new ModuleParsingException(
                    Messenger.getMsg("error.module.parsing.done"));
        }
        if (in.getName().endsWith(".jar")) {
            analyzeJar(in);
        } else if (!JamToolUtils.isModuleInfoClass(in.getName())) {
            FileInputStream is = new FileInputStream(in);
            try {
                analyzeClass(is);
            } finally {
                JamUtils.close(is);
            }
        }
    }

    /**
     * Analyzes the contents of an entire JAR archive, it is intended to be used
     * while analyzing an embedded JAR archive in a JAM, the analysis proceeds
     * only if the JAR exists in specified directory MODULE-INF/lib or if
     * specified by the JarLibraryPath annotation.
     * @param in
     * @throws sun.module.ModuleParsingException
     * @throws java.io.IOException
     */
    public void analyzeJar(File in) throws ModuleParsingException, IOException {
        if (!isEmbeddedJar(in)) {
            return; // check and return without much ado
        }
        ZipFile zf = null;
        try {
            zf = new ZipFile(in);
            for (ZipEntry ze : Collections.list(zf.entries())) {
                if (ze.getName().endsWith(".class")) {
                   analyzeClass(zf.getInputStream(ze));
                } else if (JamToolUtils.isModuleInfoClassEntry(ze.getName()) ||
                    JamToolUtils.isModuleInfMetaData(ze.getName())){
                    throw new ModuleParsingException(
                            Messenger.formatMsg("error.module.files.found",
                            in.getAbsolutePath(),
                            JamUtils.MODULE_INF_METADATA,
                            JamUtils.MODULE_INF_METADATA));
                }
            }
        } finally {
            JamUtils.close(zf);
        }
    }

    private boolean isEmbeddedJar(String in) {
        return isEmbeddedJar(new File(in));
    }

    private boolean isEmbeddedJar(File in) {
        String entryDirname = in.getParent().replace('\\', '/');
        if (entryDirname.endsWith(JamToolUtils.getModuleInfDir() + "lib")) {
            return true;
        } else {
            String s = getJarLibraryPath();
            if (s != null) {
                return entryDirname.endsWith(s);
            }
        }
        return false;
    }

   /*
    * a strict check if a class does belong to a module, and throw
    * an Exception if it does not follow the rules.
    * Assumption:
    *    A legacy class has no module affliation.
    * Logic:
    *    a. a legacy class is false
    *    b. a module class affiliated to this module, true.
    *    c. a module class not affliated to this module, error.
    */
    private boolean isMemberClass(ClassFile cf) throws ModuleParsingException {
        try {
            Module_attribute modAttr =
                    (Module_attribute) cf.getAttribute(Attribute.Module);
            if (modAttr != null) {
                String modmember = modAttr.getModuleName(cf.constant_pool);
                if (getModuleName().equals(modmember)) {
                    return true;
                } else {
                    String msg = Messenger.formatMsg("error.module.wrong.module",
                            getModuleName(), cf.getName(), modmember);
                    throw new ModuleParsingException(msg);
                }
            }
            return false;
        } catch (ConstantPoolException cpe) {
            throw new ModuleParsingException(cpe);
        }
    }

    /*
     * checks to see if the class should be exported, based on
     * on the module attribute and  ExportLegacyClass.
     * The rule:
     *  a> a legacy class (one without any module affliation), can be exported
           only if @ExportLegacyClasses is indicated.
     *  b> a module class marked public with affilation to this module.
     *  Note: in b if the class is affliated to a different module it is
     *     an error.
     */
    private boolean canExportClass(ClassFile cf, UnexportedClass uc) throws
            IOException, ModuleParsingException {
        if (cf.access_flags.is(AccessFlags.ACC_PUBLIC)) {
            boolean member = isMemberClass(cf);
            if (member || isExportLegacyClasses()) {
                return true;
            } else if (uc != null) {
                uc.reason = Messenger.getMsg("info.class.notmodule");
                moduleUnexportedClassList.add(uc);
            }
        } else if (uc != null) {
            uc.reason = Messenger.getMsg("info.class.notpublic");
            moduleUnexportedClassList.add(uc);
        }
        return false;
    }

    private String getMemberPackage(ClassFile cf) throws IOException {
        try {
            String name = cf.getName().replace("/", ".");
            int idx = name.lastIndexOf('.');
            return (idx == -1 ) ? "" :  name.substring(0, idx);
        } catch (ConstantPoolException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    private int[] combineListsAsArray(CpArrayList cpList,
            List<String> oldList, Set<String> newList) {
       LinkedHashSet<Integer> out = new LinkedHashSet<Integer>();
       for (String x : oldList) {
           out.add(cpList.getUTF8Index(x));
       }
       for (String x : newList) {
           out.add(cpList.getUTF8Index(x));
       }
       int[] outArray = new int[out.size()];
       int i = 0;
       for (int x : out) {
           outArray[i] = x;
           i++;
       }
       return outArray;
    }

    /*
     * Here is the logic to add new module tables
     * Step 1. Create a Constant Pool (CP)
     *    a. Create a local ordered constant pool image.
     *    b. Check the old metadata if it has any entries copy them over
     *    c. Add the new entries to that list.
     * Step 2. Create the new real CP from the list
     * Step 3. Create the new arrays/tables with indices to the CP, using
     *    the new CP created in Step 1.
     * Step 4. Create an Attribute List from the old metadata
     * Step 5. Get the Attributes list from the old metadata, repeat for each
     *    attribute.
     *    a. if the old does not have the said attribute add new
     *    b. otherwise swap the old one with the new one.
     * Step 6. convert the list to an array using the CP in 2.
     * Step 7. return the classfile container
     */
    private ClassFile addModuleTables(ClassFile cf)
            throws ModuleParsingException {
        try {
            // Step 1: Create a Constant Pool (CP)
            CpArrayList cpList = new CpArrayList(cf.constant_pool);
            List<String> oldModuleExportClassList = getModuleExportClassList(true);
            List<String> oldModuleExportPackageList = getModuleExportPackageList();
            List<String> oldModuleMemberList = getModuleMemberPackageList();

            if (oldModuleExportClassList.isEmpty()) {
                cpList.add(Attribute.ModuleExportTable);
            }
            cpList.addAll(moduleExportClassList);
            cpList.addAll(moduleExportPackageList);

            if (oldModuleMemberList.isEmpty()) {
                cpList.add(Attribute.ModuleMemberTable);
            }
            cpList.addAll(moduleMemberPackageList);

            // Step 2: Create the new real CP from the list
            ConstantPool ncp = cpList.toConstantPool();

            // Step 3: Create the new arrays/tables
            int[] metClassEntries = combineListsAsArray(cpList,
                    oldModuleExportClassList, moduleExportClassList);
            int[] metPackageEntries = combineListsAsArray(cpList,
                    oldModuleExportPackageList, moduleExportPackageList);

            int[] mmtEntries = combineListsAsArray(cpList,
                    oldModuleMemberList, moduleMemberPackageList);


            // Step 4: Create an Attribute List from the old metadata
            ArrayList<Attribute> attrsList =
                    new ArrayList<Attribute>(Arrays.asList(cf.attributes.attrs));

            // Step 5 for ModuleExportTable
            ModuleExportTable_attribute metAttr =
                    new ModuleExportTable_attribute(ncp,
                    metPackageEntries, metClassEntries);

            int attrindex = getAttributeIndex(cf, Attribute.ModuleExportTable);
            if (attrindex == -1) { // add the new one
                attrsList.add(metAttr);
            } else { // replace the existing one
                attrsList.set(attrindex, metAttr);
            }

            // Step 5 for ModuleMemberTable
            ModuleMemberTable_attribute mmtAttr =
                    new ModuleMemberTable_attribute(ncp, mmtEntries);

            attrindex = getAttributeIndex(cf, Attribute.ModuleMemberTable);
            if (attrindex == -1) { // add the new one
                attrsList.add(mmtAttr);
            } else {  // replace the existing one
                attrsList.set(attrindex, mmtAttr);
            }
            attrsList.trimToSize();

            // Step 6: convert the list to an array using the CP in 2
            Attributes nattrs = new Attributes(ncp,
                    attrsList.toArray(new Attribute[attrsList.size()]));

            // Step 7: return the classfile container
            return new ClassFile(cf.magic, cf.major_version, cf.minor_version,
                    ncp, cf.access_flags, cf.this_class, cf.super_class,
                    cf.interfaces, cf.fields, cf.methods, nattrs);
        } catch (ConstantPoolException ex) {
            throw new ModuleParsingException(ex);
        }
    }

    private int getAttributeIndex(ClassFile cf, String attributeName) {
//        TODO uncomment when 6716452 is integrated, and remove
//        the logic following this.
//        return cf.attributes.getIndex(cf.constant_pool,
//                    attributeName);
//      Remove this
        if (cf.getAttribute(attributeName) == null) {
            return -1;
        }
        for (int i = 0 ; i < cf.attributes.size() ; i++) {
            if (cf.attributes.attrs[i].equals(attributeName)) {
                return i;
            }
        }
        return -1;
    }

    /*
     * Indicates that further anaylsis of class-files are no longer required
     * and it is safe to generate the new module metadata file.
     * @throws ModuleParsingException
     */
     void finish() throws ModuleParsingException  {
        ClassFile mdClassFile = super.getClassFile();
        if (mdClassFile == null) {  // auto generate a new one ??
            // TODO
            throw new UnsupportedOperationException("not yet implemented");
        }
        ClassTranslator ctr = new ClassTranslator();
        ClassFile mutatedClassFile = addModuleTables(mdClassFile);
        HashMap<Object, Object> map = new HashMap<Object, Object>();
        map.put( mdClassFile.constant_pool,mutatedClassFile.constant_pool);
        map.put(mdClassFile.attributes, mutatedClassFile.attributes);
        moduleInfoClassFile = ctr.translate(mdClassFile, map);
        done = true;
    }

    private void processJar(File jarFile, boolean shouldFinish)
            throws ModuleParsingException, IOException {
        ZipFile zf = null;
        ZipInputStream zis = null;
        UnexportedClass uc;
        try {
            zf = new ZipFile(jarFile);
            for (ZipEntry ze : Collections.list(zf.entries())) {
                String entry = ze.getName();
                if ( entry.endsWith(".class") &&
                        !JamToolUtils.isModuleInfoClassEntry(entry)) {
                    uc = new UnexportedClass(jarFile.getName(), entry, null);
                    analyzeClass(zf.getInputStream(ze), uc);
                } else if (entry.endsWith(".jar")) {
                    if (isEmbeddedJar(entry)) {
                        zis  = new ZipInputStream(zf.getInputStream(ze));
                        processJar(zis, entry);
                    } else {
                        Messenger.formatMsg("info.jar.notembedded", entry);
                    }
                }
            }
            // create the new module metadata file.
            if (shouldFinish)
                finish();
        } finally {
            JamUtils.close(zf);
            JamUtils.close(zis);
        }
    }

    private void processJar(ZipInputStream zin, String enclosingJar)
            throws ModuleParsingException, IOException {
        ZipEntry ze = zin.getNextEntry();
        while (ze != null) {
            String entry = ze.getName();
            if (entry.endsWith(".class") &&
                    !JamToolUtils.isModuleInfoClassEntry(entry)) {
                UnexportedClass uc = new UnexportedClass(enclosingJar, entry, null);
                analyzeClass(zin, uc);
                zin.closeEntry();
            }
            ze = zin.getNextEntry();
        }
    }

    /**
     * Returns a String representation of the metadata file.
     * @param jarfile
     * @param verbose
     * @return a string
     * @throws java.io.IOException
     * @throws sun.module.ModuleParsingException
     */
    public static String printDiagnostics(String jarfile, boolean verbose)
            throws IOException, ModuleParsingException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        JamModuleMetadata metadata = new JamModuleMetadata(new File(jarfile));
        pw.println(metadata.toString());
        if (verbose && !metadata.moduleUnexportedClassList.isEmpty()) {
            pw.println(Messenger.getMsg("info.unexported.header"));
            for (UnexportedClass c : metadata.moduleUnexportedClassList) {
                pw.println("  " + c.toString());
            }
        }
        pw.flush();
        sw.flush();
        return sw.toString();
    }

    /*
     * A helper class to manage constant pool entries
     */
    class CpArrayList extends ArrayList<CPInfo> {
        CpArrayList(ConstantPool cp) {
            super();
            // initialization, add the 0th entry manually, as access is a no-no,
            // through the javap library.
            add(0, null);
            // add the rest of the entries
            for (int i = 1; i < cp.size(); i++) {
                try {
                    super.add(cp.get(i));
                } catch (ConstantPool.InvalidIndex ii) {
                    // skip a double-word entry
                    super.add(i, null);
                }
            }
        }

        void add(String utf8) {
            super.add(new ConstantPool.CONSTANT_Utf8_info(utf8));
        }

        void addAll(Collection<String> list) {
            for (String x : list) {
                add(x);
            }
        }

        int getUTF8Index(String str) {
            if (str == null) {
                return -1;
            }
            CPInfo cmp = new ConstantPool.CONSTANT_Utf8_info(str);
            for (int i = 1; i < size(); i++) {
                CPInfo c = get(i);
                if (c != null && c.getTag() == ConstantPool.CONSTANT_Utf8 &&
                        cmp.toString().equals(c.toString())) {
                    return i;
                }
            }
            return -1;
        }

        ConstantPool toConstantPool() {
            return new ConstantPool(super.toArray(new CPInfo[size()]));
        }
    }

    /*
     * A container class to encapsulate the diagnostic information as to why a
     * class could not be exported.
     */
    class UnexportedClass {

        String location;
        String classname;
        String reason;

        UnexportedClass() {
            location = null;
            classname = null;
            reason = null;
        }

        UnexportedClass(String location,  String classname,  String reason) {
            this.location = location;
            this.classname = classname;
            this.reason = reason;
        }

        public String toString() {
            return (classname +
                    "@" + ((location == null) ? "input-file" : location) +
                    ":" + reason);
        }
    }
}
