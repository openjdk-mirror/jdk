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

package sun.module.repository;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.module.Version;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import javax.xml.validation.Validator;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import sun.module.JamUtils;

/**
 * Reads from data from a {@code URL} which conforms to the
 * <tt>RepositoryMetadata.xml</tt> schema, providing a set of
 * {@code ModuleInfo} instances.
 * @since 1.7
 */
public class MetadataXMLReader extends DefaultHandler implements ErrorHandler {
    /** ModuleInfo corresponding to data read from repository-metadata.xml. */
    private final Set<ModuleInfo> moduleInfos = new HashSet<ModuleInfo>();

    /** For handling errors. */
    private Locator locator;

    /** Repository's source location. */
    private final String sourceLocation;

    /** Builds up information used to create elements of {@codemoduleInfos}. */
    private MutableModuleInfo moduleInfo;

    /**
     * The setting of @{code expect} determines how the next invocation of
     * {@code characters} is to use those characters.
     */
    private enum Kind { NAME, VERSION, PLATFORM, ARCH, PATH, NONE }

    /**
     * Initial {@code Kind} indicates that the reader expects to begin
     * creating a {@code ModuleInfo}.
     */
    private Kind expect = Kind.NONE;

    /**
     * Returns information from given URL as a set
     * of ModuleInfo instances.  Validates the data aginst the
     * schema in {@code java/module/RepositoryMetadata.xml}.
     *
     * @param source {@code URL} to a file which conforms to
     * the <tt>RepositoryMetadata.xml</tt> schema.
     * @return a {@code Set<ModuleInfo>}, with one {@code ModuleInfo} for each
     * <tt>module</tt> entry in the <tt>repository-metadata.xml</tt>.
     * @throws NullPointerException if any argument is null.
     * @throws IllegalArgumentException if the {@code source} has duplicate entries
     * (note that the <tt>path</tt> element is not considered when checking for
     * duplicates, though other elements are).
     */
    public static Set<ModuleInfo> read(URL source) throws SAXException, IOException {
        SchemaFactory f = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        InputStream schemaStream = null;
        InputStream repoStream = null;
        try {
            schemaStream = new BufferedInputStream(
                    ClassLoader.getSystemResourceAsStream(
                        "java/module/RepositoryMetadata.xml"));

            StreamSource ss = new StreamSource(schemaStream);
            Schema s = f.newSchema(ss);
            Validator v = s.newValidator();
            repoStream = new BufferedInputStream(source.openStream());
            v.validate(new StreamSource(repoStream));
        } finally {
            JamUtils.close(schemaStream);
            JamUtils.close(repoStream);
        }

        XMLReader xmlReader = XMLReaderFactory.createXMLReader();

        MetadataXMLReader moduleTypeReader =
            new MetadataXMLReader(source.toString());
        xmlReader.setErrorHandler(moduleTypeReader);
        xmlReader.setContentHandler(moduleTypeReader);
        repoStream = new BufferedInputStream(source.openStream());
        try {
            xmlReader.parse(new InputSource(repoStream));;
        } finally {
            JamUtils.close(repoStream);
        }
        return moduleTypeReader.moduleInfos;
    }

    private MetadataXMLReader(String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    //
    // Implement ErrorHandler
    // XXX Needs improvement.
    //

    public void error(SAXParseException ex) throws SAXException {
        if (locator != null) msg(locator);
        throw ex;
    }
    public void fatalError(SAXParseException ex) throws SAXException {
        if (locator != null) msg(locator);
        throw ex;
    }
    public void warning(SAXParseException ex) throws SAXException {
        if (locator != null) msg(locator);
    }

    void msg(Locator l) {
        System.err.printf("SAXParseException at line %d, column %d%n",
                l.getLineNumber(), l.getColumnNumber());
    }

    //
    // Extend DefaultHandler
    //

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void startElement(String ns, String name, String qname, Attributes attrs)
            throws SAXException {
        if ("module".equals(name)) {
            moduleInfo = new MutableModuleInfo();
        } else if ("name".equals(name)) {
            expect = Kind.NAME;
        } else if ("version".equals(name)) {
            expect = Kind.VERSION;
        } else if ("platform".equals(name)) {
            expect = Kind.PLATFORM;
        } else if ("arch".equals(name)) {
            expect = Kind.ARCH;
        } else if ("path".equals(name)) {
            expect = Kind.PATH;
        }
    }

    @Override
    public void endElement(String ns, String name, String qname)
            throws SAXException, IllegalArgumentException {
        if ("module".equals(name)) {
            ModuleInfo mi = new ModuleInfo(moduleInfo);
            if (moduleInfos.contains(mi)) {
                throw new IllegalArgumentException(
                    "duplicate ModuleInfo is not allowed in " + sourceLocation);
            } else {
                moduleInfos.add(mi);
            }
            expect = Kind.NONE;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        String tmp = new String(ch, start, length);
        if (!tmp.trim().equals("")) {
            switch (expect) {
                case NAME:
                    moduleInfo.setName(tmp);
                    break;
                case VERSION:
                    moduleInfo.setVersion(Version.valueOf(tmp));
                    break;
                case PLATFORM:
                    moduleInfo.setPlatform(tmp);
                    break;
                case ARCH:
                    moduleInfo.setArch(tmp);
                    break;
                case PATH:
                    moduleInfo.setPath(tmp);
                    break;
                case NONE:
                    break;
            }
        }
    }

    //
    // Local implementation support
    //

    private static class MutableModuleInfo extends ModuleInfo {
        void setName(String name)         { this.name = name; }
        void setVersion(Version version)   { this.version = version; }
        void setPlatform(String platform) { this.platform = platform; }
        void setArch(String arch)         { this.arch = arch; }
        void setPath(String path)         { this.path = path; }
    }
}
