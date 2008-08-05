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
package sun.module;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * This class is a simplistic and basic class file reader, its designed
 * specifically for the runtime classloader to read the metadata
 * in the most efficient manner, but primarily to keep the ClassLoader footprint
 * as small as possible.
 *
 * Hence it is crucial to keep this class  as *lean* as possible, meaning with
 * very few absolutely required dependencies. In order to keep the classloading
 * to a minimum, the messages, are not resourced as we do not want to loaded
 * resource classes, this may need to be revisited.
 *
 * At this time it reads a few attributes needed for module
 * loader,  and does not support Annotations as there is  no specific
 * requirement to do so, as the classloader reads annotations reflectively.
 */
public class MetadataParser {

    int minor_version;
    int major_version;
    final HashMap<String, List<String>> moduleInfoMap;
    ConstantPool cpool;
    static final String MODULE_EXPORT_CLASS_LIST =
            "ModuleExportClassList";
    static final String MODULE_EXPORT_PACKAGE_LIST =
            "ModuleExportPackageList";
    static final String MODULE_MEMBER_PACKAGE_LIST =
            "ModuleMemberPackageList";
    private final ByteArrayAccessor mbuf;

    boolean haveModuleExportTable = false;
    boolean haveModuleMemberTable = false;

    private static final int DEFAULT_VALUE = 0;

    // the attributes we care about, the others will be ignored
    private static final int Attribute_SourceFile = 0;
    private static final int Attribute_Module = 1;
    private static final int Attribute_ModuleExportTable = 2;
    private static final int Attribute_ModuleMemberTable = 3;
    private final String[] requiredAttributes = {
        "SourceFile",
        "Module",
        "ModuleExportTable",
        "ModuleMemberTable"
    };

    /**
     * Construct an object given a byte array
     * @param buf - a byte buffer
     * @param off - an offset
     * @param len - max length to read
     * @throws sun.module.ModuleParsingException
     */
    public MetadataParser(byte[] buf, int off, int len)
            throws ModuleParsingException {
        mbuf = new ByteArrayAccessor(buf, off, len);
        moduleInfoMap = parseBytes();
    }

    /**
     * Construct an object given a byte array.
     * @param in
     * @throws sun.module.ModuleParsingException
     */
    public MetadataParser(byte[] in) throws ModuleParsingException {
        mbuf = new ByteArrayAccessor(in);
        moduleInfoMap = parseBytes();
    }

    private HashMap<String, List<String>> parseBytes() throws ModuleParsingException {
        int magic = mbuf.u4();
        if (magic != 0xCAFEBABE) {
            throw new ModuleParsingException("content is not a metadata");
        }
        minor_version = mbuf.u2();
        major_version = mbuf.u2();
        if (major_version < 51) {
            throw new ModuleParsingException("expected version 52+, but found " +
                    Integer.toString(major_version));
        }
        cpool = new ConstantPool();
        int access_flags = mbuf.u2();

        String this_class = cpool.getString(mbuf.u2());
        if (!(this_class + ".class").endsWith(JamUtils.MODULE_INFO_CLASS)) {
            throw new ModuleParsingException("not a valid  " +
                    JamUtils.MODULE_METADATA);
        }

        String super_class = cpool.getString(mbuf.u2());
        int interface_count = mbuf.u2();
        if (interface_count > 0) {
            throw new ModuleParsingException("should not contain any interfaces");
        }
        int fields_count = mbuf.u2();
        if (fields_count > 0) {
            throw new ModuleParsingException("should not contain any fields");
        }

        int methods_count = mbuf.u2();
        if (methods_count > 0) {
            throw new ModuleParsingException("should not contain any methods");
        }
        return new Attributes().getModuleInfoMap();
    }

    /**
     * Returns the module's name as coded into the module-info file.
     * @return a {@link String} representing the module name
     */
    public String getModuleName() {
        return moduleInfoMap.get(requiredAttributes[Attribute_Module]).get(DEFAULT_VALUE);
    }

    /**
     * Returns the module's exported class attributes as a List with or without
     * class signature (field descriptor).
     * Ex: with signature is L/java/lang/Object;
     * Ex: without signature is java.lang.Object
     * @param asSignature
     * @return a {@link List} containing the elements
     */
    public List<String> getModuleExportClassList(boolean asSignature) {
        ArrayList<String> out = new ArrayList<String>();
        if (moduleInfoMap.get(MODULE_EXPORT_CLASS_LIST) != null) {
            for (String x : moduleInfoMap.get(MODULE_EXPORT_CLASS_LIST)) {
                if (asSignature) {
                    out.add(getStringAsSignature(x));
                } else {
                    out.add(stripClassSignature(x));
                }
            }
        }
        return out;
    }

    /**
     * Returns a list of the module's exported packages in the module metadata.
     * Noting that an anonymous package will be denoted as an empty string ie. "".
     * @return a {@link List} containing the elements
     */
    public List<String> getModuleExportPackageList() {
        ArrayList<String> out = new ArrayList<String>();
        if (moduleInfoMap.get(MODULE_EXPORT_PACKAGE_LIST) != null) {
            for (String x : moduleInfoMap.get(MODULE_EXPORT_PACKAGE_LIST)) {
                out.add(x);
            }
        }
        return out;
    }

    /**
     * returns the module's member package attribute List, without the signatures
     * ex: com.sun.util
     * @return a {@link List} containing the elements
     */
    public List<String> getModuleMemberPackageList() {
        ArrayList<String> out = new ArrayList<String>();
        if (moduleInfoMap.get(MODULE_MEMBER_PACKAGE_LIST) != null) {
            for (String x : moduleInfoMap.get(MODULE_MEMBER_PACKAGE_LIST)) {
                out.add(stripClassSignature(x));
            }
        }
        return out;
    }

    /**
     * a human readable representation of the module metadata.
     * @return a {@link String} representation of this metadata object
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String x : moduleInfoMap.keySet()) {
            if (x == null) {
                continue;
            }
            sb = sb.append(x + ":" + moduleInfoMap.get(x) + "\n");
        }
        return sb.toString();
    }

    public HashMap<String, List<String>> getModuleInfoMap() {
        return moduleInfoMap;
    }

    public static String getPackageName(String in) {
        int idx = in.lastIndexOf('.');
        return (idx >= 0) ? in.substring(0, idx) : "";
    }

    public static String stripClassSignature(String in) {
        return in.startsWith("L") && in.endsWith(";")
                ? in.substring(1, in.length() - 1).replace("/", ".")
                : in.replace("/", ".");
    }

    public static String getStringAsSignature(String s) {
        return (s.startsWith("L") && s.endsWith(";"))
                ? s
                : "L" + s.replace(".", "/") + ";";
    }

    /*
     * A helper class to read the Attributes we need
     */
    private class Attributes {

        HashMap<String, List<String>> map = new HashMap<String, List<String>>();

        Attributes() throws ModuleParsingException {
            int attribute_count = mbuf.u2();
            for (int i = 0; i < attribute_count; i++) {
                String attributeName = cpool.getString(mbuf.u2());
                int attribute_length = mbuf.u4();
                if (attributeName.equals(
                        requiredAttributes[Attribute_SourceFile])) {
                    addToMap(attributeName, cpool.getString(mbuf.u2()));
                } else if (attributeName.equals(
                        requiredAttributes[Attribute_Module])) {
                    addToMap(attributeName,
                            stripClassSignature(cpool.getString(mbuf.u2())));
                } else if (attributeName.equals(
                        requiredAttributes[Attribute_ModuleMemberTable])) {
                    haveModuleMemberTable = true;
                    int table_length = mbuf.u2();
                    ArrayList<String> values = new ArrayList<String>();
                    for (int k = 0; k < table_length; k++) {
                        values.add(cpool.getString(mbuf.u2()));
                    }
                    addToMap(MODULE_MEMBER_PACKAGE_LIST, values);
                } else if (attributeName.equals(
                        requiredAttributes[Attribute_ModuleExportTable])) {
                    haveModuleExportTable = true;

                    // ModuleExportPackageList
                    ArrayList<String> values = new ArrayList<String>();
                    int export_package_length = mbuf.u2();

                    for (int k = 0; k < export_package_length; k++) {
                        values.add(cpool.getString(mbuf.u2()));
                    }
                    addToMap(MODULE_EXPORT_PACKAGE_LIST, values);

                    // ModuleExportClassList
                    values = new ArrayList<String>();
                    int export_type_length = mbuf.u2();
                    for (int k = 0; k < export_type_length; k++) {
                        values.add(stripClassSignature(cpool.getString(mbuf.u2())));
                    }
                    addToMap(MODULE_EXPORT_CLASS_LIST, values);
                } else {
                    long len = mbuf.skip(attribute_length);
                    if (len != attribute_length) {
                        throw new ModuleParsingException("metadata is corrupted");
                    }
                }
            }
        }

        private void addToMap(String name, ArrayList<String> out) {
            out.trimToSize();
            Collections.sort(out);
            map.put(name, out);
        }

        private void addToMap(String name, String attrValue) {
            ArrayList<String> tmp = new ArrayList<String>();
            tmp.add(attrValue);
            addToMap(name, tmp);
        }

        private HashMap<String, List<String>> getModuleInfoMap() {
            return map;
        }
    }

    /*
     * A helper class to read the ConstantPool
     */
    private class ConstantPool {

        public static final String UTF8_ENCODING = "UTF8";
        // The constant pool constants
        public static final int CONSTANT_Utf8 = 1;
        public static final int CONSTANT_Integer = 3;
        public static final int CONSTANT_Float = 4;
        public static final int CONSTANT_Long = 5;
        public static final int CONSTANT_Double = 6;
        public static final int CONSTANT_Class = 7;
        public static final int CONSTANT_String = 8;
        public static final int CONSTANT_Fieldref = 9;
        public static final int CONSTANT_Methodref = 10;
        public static final int CONSTANT_InterfaceMethodref = 11;
        public static final int CONSTANT_NameAndType = 12;
        ArrayList<CPInfo> cpoolList = new ArrayList<CPInfo>();
        String[] cpName;
        byte[] cpTag;
        int cpLen;
        ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);
        private final String[] cpTagName = {
            /* 0:  */null,
            /* 1:  */ "Utf8",
            /* 2:  */ null,
            /* 3:  */ "Integer",
            /* 4:  */ "Float",
            /* 5:  */ "Long",
            /* 6:  */ "Double",
            /* 7:  */ "Class",
            /* 8:  */ "String",
            /* 9:  */ "Fieldref",
            /* 10: */ "Methodref",
            /* 11: */ "InterfaceMethodref",
            /* 12: */ "NameAndType",
            null
        };

        private ConstantPool() throws ModuleParsingException {
            cpoolList.add(null);
            cpLen = mbuf.u2();
            cpName = new String[cpLen];
            cpTag = new byte[cpLen];
            readCP();
        }

        private void readCP() throws ModuleParsingException {
            int cpTem[][] = new int[cpLen][];
            for (int i = 1; i < cpLen; i++) {
                try {
                    cpTag[i] = (byte) mbuf.u1();
                    switch (cpTag[i]) {
                        case CONSTANT_Utf8:
                            buf.reset();
                            for (int len = mbuf.u2(),  j = 0; j < len; j++) {
                                buf.write(mbuf.u1());
                            }
                            cpName[i] = buf.toString(UTF8_ENCODING);
                            break;
                        case CONSTANT_Integer:
                            cpName[i] = String.valueOf(mbuf.u4());
                            break;
                        case CONSTANT_Float:
                            cpName[i] =
                                    String.valueOf(Float.intBitsToFloat(mbuf.u4()));
                            break;
                        case CONSTANT_Long:
                            cpName[i] = String.valueOf(mbuf.u8());
                            i += 1;
                            break;
                        case CONSTANT_Double:
                            cpName[i] =
                                    String.valueOf(Double.longBitsToDouble(mbuf.u8()));
                            i += 1;
                            break;
                        case CONSTANT_Class:
                        case CONSTANT_String:
                            cpTem[i] = new int[]{mbuf.u2()};
                            break;
                        case CONSTANT_Fieldref:
                        case CONSTANT_Methodref:
                        case CONSTANT_InterfaceMethodref:
                        case CONSTANT_NameAndType:
                            cpTem[i] = new int[]{mbuf.u2(), mbuf.u2()};
                            break;
                    }
                } catch (UnsupportedEncodingException ex) {
                    throw new ModuleParsingException(ex);
                }
            }
            for (int i = 1; i < cpLen; i++) {
                switch (cpTag[i]) {
                    case CONSTANT_Class:
                    case CONSTANT_String:
                        cpName[i] = cpName[cpTem[i][0]];
                        break;
                    case CONSTANT_NameAndType:
                        cpName[i] = cpName[cpTem[i][0]] + " " +
                                cpName[cpTem[i][1]];
                        break;
                }
            }
            // do fieldref et al after nameandtype are all resolved
            for (int i = 1; i < cpLen; i++) {
                switch (cpTag[i]) {
                    case CONSTANT_Fieldref:
                    case CONSTANT_Methodref:
                    case CONSTANT_InterfaceMethodref:
                        cpName[i] = cpName[cpTem[i][0]] + " " +
                                cpName[cpTem[i][1]];
                        break;
                }
            }
            for (int i = 0; i < cpName.length; i++) {
                if (cpName[i] == null) {
                    continue;
                }
                CPInfo cpinfo = new CPInfo(getCpTagName(cpTag[i]), cpName[i]);
                cpoolList.add(i, cpinfo);
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (CPInfo x : cpoolList) {
                sb = sb.append(x);
            }
            return sb.toString();
        }

        public String getString(int index) {
            return cpoolList.get(index).cpname;
        }

        private String getCpName(int id) {
            if (id >= 0 && id < cpName.length) {
                return cpName[id];
            } else {
                return "[CP#" + Integer.toHexString(id) + "]";
            }
        }

        private String getCpTagName(int t) {
            t &= 0xFF;
            String ts = null;
            if (t < cpTagName.length) {
                ts = cpTagName[t];
            }
            if (ts != null) {
                return ts;
            }
            return ("UnknownTag" + t).intern();
        }

        private class CPInfo {
            String cptag;
            String cpname;

            CPInfo(String cptag, String cpname) {
                this.cptag = cptag;
                this.cpname = cpname;
            }

            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb = sb.append(cptag);
                sb = sb.append(":");
                sb = sb.append(cpname);
                return sb.toString();
            }
        }
    }

    /*
     * a simple byte area reader, it is designed to read directly from
     * a byte array without the need for the ByteArrayInputStream.
     */
    private class ByteArrayAccessor {
        private final byte[] in;
        private int pos;
        private int len;

        ByteArrayAccessor(byte[] buffer) {
            this.in = buffer;
            this.pos = 0;
            this.len = buffer.length;
        }

        ByteArrayAccessor(byte[] buffer, int off, int len) {
            this.in = buffer;
            this.pos = off;
            this.len = len;
        }

        private long u8() {
            return ((long) u4() << 32) + (((long) u4() << 32) >>> 32);
        }

        private int u4() {
            return (u2() << 16) + u2();
        }

        private int u2() {
            return (u1() << 8) + u1();
        }

        private int u1() {
            int x = this.in[pos];
            pos++;
            return x & 0xFF;
        }

        private int skip(int len) {
            if ((pos + len) > this.len || len < 0) {
                return -1;
            }
            pos += len;
            return len;
        }
    }
}
