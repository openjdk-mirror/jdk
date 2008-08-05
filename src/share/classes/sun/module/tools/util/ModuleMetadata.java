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

import com.sun.tools.classfile.Annotation.Annotation_element_value;
import com.sun.tools.classfile.Annotation.Array_element_value;
import com.sun.tools.classfile.Annotation.Class_element_value;
import com.sun.tools.classfile.Annotation.Enum_element_value;
import com.sun.tools.classfile.Annotation.Primitive_element_value;
import com.sun.tools.classfile.Annotation.element_value;
import com.sun.tools.classfile.Annotation;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.RuntimeVisibleAnnotations_attribute;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import sun.module.JamUtils;
import sun.module.MetadataParser;
import sun.module.ModuleParsingException;
import sun.module.tools.Messenger;


/**
 * This clases reads and creates a module metadata object, given the
 * module-info.class, its primary purpose is to parse the annotations
 * that are available in the module-info.class. The rest of the attributes
 * are read by the light weight MetadataParser.
 */
class ModuleMetadata  {
    private ClassFile mdClassFile;
    private static final String ANNOTATION_PREFIX =
            "java.module.annotation.";
    private static final String EXPORT_LEGACY_CLASSES_ANNOTATION =
            ANNOTATION_PREFIX + "ExportLegacyClasses";
    private static final String JAR_LIBRARY_PATH =
            ANNOTATION_PREFIX + "JarLibraryPath";

    HashMap<String, List<String>> moduleInfoMap = null;

    MetadataParser mdp = null;
    Attribute.Factory afactory = new Attribute.Factory();

    /**
     * Construct a module metadata object from an {@link InputStream}.
     * @param is
     * @throws java.io.IOException
     * @throws sun.module.tools.util.ModuleParsingException
     */
    ModuleMetadata(InputStream is) throws IOException, ModuleParsingException {
        byte[] buf = JamUtils.getInputStreamAsBytes(is);
        init(buf);
    }

    /**
     * Construct a module metadata using a byte buffer.
     * @param buf
     * @throws sun.module.tools.util.ModuleParsingException
     */
    ModuleMetadata(byte[] buf) throws ModuleParsingException {
        try {
            init(buf);
        } catch (IOException ioe) {
            throw new ModuleParsingException(ioe);
        }
    }

    /**
     * Construct a module metadata object from a JAR-file, noting that the
     * archive must contain a module metadata file, if a
     * MODULE-INF/MODULE.METADATA exist then will be used in preference to
     * the one found as module-info.class, it is an error if either of them
     * don't exist.
     * @param jarname
     * @throws java.io.IOException
     * @throws sun.module.tools.util.ModuleParsingException
     */
    ModuleMetadata(File jarFile) throws IOException, ModuleParsingException {
        ZipFile zf = null;
        InputStream minfois = null;
        try {
            zf = new ZipFile(jarFile);
            for (ZipEntry ze : Collections.list(zf.entries())) {
                if (JamToolUtils.isModuleInfMetaData(ze.getName())) {
                    byte[] buf = JamUtils.getInputStreamAsBytes(zf.getInputStream(ze));
                    init(buf);
                    return;
                } else if (JamToolUtils.isModuleInfoClassEntry(ze.getName())) {
                    minfois = zf.getInputStream(ze);
                }
            }
            if (minfois == null) {
                throw new IOException(
                        Messenger.formatMsg("error.module.files.notfound",
                        JamUtils.MODULE_INF_METADATA,
                        JamUtils.MODULE_INFO_CLASS)
                );
            }
            init(JamUtils.getInputStreamAsBytes(minfois));
        } finally {
            JamUtils.close(minfois);
            JamUtils.close(zf);
        }
    }

    private void init(byte[] buf) throws ModuleParsingException, IOException {
        mdp = new MetadataParser(buf);
        moduleInfoMap = mdp.getModuleInfoMap();
        ByteArrayInputStream bis = new ByteArrayInputStream(buf);
        afactory.setJSR277(true);
        try {
            mdClassFile = ClassFile.read(bis, afactory);
        } catch (ConstantPoolException cpe) {
            throw new ModuleParsingException(cpe);
        }
        parseAnnotations();
    }

    /**
     * Returns the module's name as coded into the module-info file.
     * @return a {@link String} representing the module name.
     */
    String getModuleName() {
        return mdp.getModuleName().replace('.', '/');
    }

    /**
     * Returns the module's member package attribute List, without the signatures.
     * Example: com.sun.util
     * @return a {@link List} containing the elements
     */
    List<String> getModuleMemberPackageList() {
       return mdp.getModuleMemberPackageList();
    }

    /**
     * Returns the module's exported class attributes as a List with or
     * without class signature (field descriptor).
     * Example: with signature is L/java/lang/Object;
     * Example: without signature is java.lang.Object
     * @param asSignature
     * @return a {@link List} containing the elements
     */
    List<String> getModuleExportClassList(boolean asSignature) {
       return mdp.getModuleExportClassList(asSignature);
    }

    /**
     * Returns a list of the module's exported packages in the module metadata.
     * Noting that an anonymous package will be denoted as a \"\".
     * @return a {@link List} containing the elements
     */
    List<String> getModuleExportPackageList() {
       return mdp.getModuleExportPackageList();
    }



    /**
     * A human readable representation of the module metadata.
     * @return a {@link String} representation of this metadata object
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String x : moduleInfoMap.keySet()) {
            if (x == null) continue;
            sb = sb.append(x + ":" + moduleInfoMap.get(x) + "\n");
        }

        return sb.toString();
    }

    ClassFile getClassFile() {
        return this.mdClassFile;
    }

    String getJarLibraryPath() {
        List<String> list = moduleInfoMap.get(JAR_LIBRARY_PATH);
        return (list != null && list.size() > 0) ? list.get(0) : null;
    }

    boolean isExportLegacyClasses() {
        return moduleInfoMap.containsKey(EXPORT_LEGACY_CLASSES_ANNOTATION);
    }

    private List<String> parseAnnotation(Annotation a) {
        List<String> aList = null;
        if (a.num_element_value_pairs > 0) {
            ElementVisitor elementVisitor = new ElementVisitor();
            aList = new ArrayList<String>();
            for (Annotation.element_value_pair ev : a.element_value_pairs) {
                aList = elementVisitor.visit(ev.value);
            }
            Collections.sort(aList);
        }
        return aList;
    }

    private void parseAnnotations() throws ModuleParsingException {
        RuntimeVisibleAnnotations_attribute rva =
                (RuntimeVisibleAnnotations_attribute)
                mdClassFile.getAttribute(Attribute.RuntimeVisibleAnnotations);
        for (Annotation a : rva.annotations) {
            try {
                String s = mdClassFile.constant_pool.getUTF8Value(a.type_index);
                moduleInfoMap.put(MetadataParser.stripClassSignature(s), parseAnnotation(a));
            } catch (ConstantPool.InvalidIndex ex) {
                throw new ModuleParsingException(ex);
            } catch (ConstantPool.UnexpectedEntry ex) {
                throw new ModuleParsingException(ex);
            }
        }
    }

    /* TODO: fix review comments:
     * make the visitor stateless, by changing the supertype of the
     * visitor from Visitor<Void,Void> to Visitor<ArrayList<String,ArrayList<String>>
     * therefore line 254, 265, 293: redundant.
     */
    class ElementVisitor implements Annotation.element_value.Visitor<Void, Void> {
        ConstantPool cp = mdClassFile.constant_pool;
        List<String> visitList = new ArrayList<String>();
        public Void  visitPrimitive(Primitive_element_value ev, Void p) {
            try {
                visitList.add(cp.getUTF8Value(ev.const_value_index));
                return null;
            } catch (Exception ex) {
                // TODO: provide way for extra debugging
            }
            return null;
        }

        public Void visitEnum(Enum_element_value ev, Void p) {
            try {
                visitList.add(cp.getUTF8Value(ev.const_name_index));
                visitList.add(cp.getUTF8Value(ev.type_name_index));
                return null;
            } catch (Exception ex) {
                // TODO: provide way for extra debugging
            }
            return null;
        }

        public Void visitClass(Class_element_value ev, Void p) {
           try {
                visitList.add(cp.getUTF8Value(ev.class_info_index));
                return null;
            } catch (Exception ex) {
                // TODO: provide way for extra debugging
            }
            return null;
        }

        public Void visitAnnotation(Annotation_element_value ev, Void p) {
            visitList.addAll(parseAnnotation(ev.annotation_value));
            return null;
        }

        public Void visitArray(Array_element_value ev, Void p) {
            try {
                ElementVisitor aev = new ElementVisitor();
                for (element_value e : ev.values) {
                    this.visitList = aev.visit(e);
                }
                return null;
            } catch (Exception ex) {
                System.err.println(ex.getCause());
            }
            return null;
        }

        public List<String> visit(element_value ev) {
            ev.accept(this, null);
            Collections.sort(this.visitList);
            return this.visitList;
        }
    }
}
