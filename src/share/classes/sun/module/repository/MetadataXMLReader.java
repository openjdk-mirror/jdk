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

import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.module.Repository;
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
 * {@code JamModuleArchiveInfo} instances.
 * @since 1.7
 */
public class MetadataXMLReader extends DefaultHandler implements ErrorHandler {

    private Repository repository;

    /** JamModuleArchiveInfo corresponding to data read from repository-metadata.xml. */
    private final Set<JamModuleArchiveInfo> result = new HashSet<JamModuleArchiveInfo>();

    /** For handling errors. */
    private Locator locator;

    /** URL connection to the repository metadata file. */
    private final URLConnection conn;

    /** Builds up information during parsing. */
    private String moduleName;
    private String moduleVersion;
    private String platform;
    private String arch;
    private String path;

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
     * Returns information from given URL as a set of
     * JamModuleArchiveInfo instances.  Validates the data aginst the
     * schema in {@code java/module/repository-metadata-schema.xml}.
     *
     * @param conn {@code URLConnection} to read the repository metadata file.
     * @return a {@code Set<JamModuleArchiveInfo>}, with one
     *         {@code JamModuleArchiveInfo} for each
     *         <tt>module</tt> entry in the repository metadata file.
     * @throws NullPointerException if any argument is null.
     * @throws IllegalArgumentException if the repository metadata file has
     *         duplicate entries (note that the <tt>path</tt> element is not
     *         considered when checking for duplicates, though other elements
     *         are).
     */
    public static Set<JamModuleArchiveInfo> read(Repository repository, URLConnection conn) throws SAXException, IOException {
        InputStream schemaStream = null;
        InputStream repoStream = null;

        // Save context class loader
        Thread t = Thread.currentThread();
        ClassLoader cl = t.getContextClassLoader();

        try {
            // Set context class loader to a bootstrap class loader wrapper.
            t.setContextClassLoader(new ClassLoader(null) {});

            // SchemaFactory will use the context class loader to search for
            // providers. However, this has caused undesirable effect in
            // bootstrapping. More specifically, the system repository,
            // extension class loader, extension module loader may still being
            // setup when this method is invoked, and accessing the context
            // class loader (usually it is the system class loader) would
            // not be appropriate. To workaround the issue, force the
            // SchemaFactory to look for providers in the bootstrap class loader
            // by setting the context class loader to a bootstrap class loader
            // wrapper.
            //
            // Note: setting the context class loader to null can't workaround
            // the issue because SchemaFactory uses the system class
            // loader if context class loader is null.
            SchemaFactory f = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

            // Read the soruce into a byte array so it does not need to be downloaded
            // more than once.
            repoStream = conn.getInputStream();
            byte[] byteBuffer = JamUtils.getInputStreamAsBytes(repoStream);

            // Validate schema
            schemaStream = new BufferedInputStream(
                    ClassLoader.getSystemResourceAsStream(
                        "java/module/repository-metadata-schema.xml"));

            StreamSource ss = new StreamSource(schemaStream);
            Schema s = f.newSchema(ss);
            Validator v = s.newValidator();
            InputStream is = new ByteArrayInputStream(byteBuffer);
            v.validate(new StreamSource(is));

            XMLReader xmlReader = XMLReaderFactory.createXMLReader();

            MetadataXMLReader moduleTypeReader =
                new MetadataXMLReader(repository, conn);
            xmlReader.setErrorHandler(moduleTypeReader);
            xmlReader.setContentHandler(moduleTypeReader);
            is = new ByteArrayInputStream(byteBuffer);
            xmlReader.parse(new InputSource(is));;

            return moduleTypeReader.result;
        } finally {
            // Restore context class loader
            t.setContextClassLoader(cl);

            JamUtils.close(repoStream);
            JamUtils.close(schemaStream);
        }
    }

    private MetadataXMLReader(Repository repository, URLConnection conn) {
        this.repository = repository;
        this.conn = conn;
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
            moduleName = null;
            moduleVersion = null;
            platform = null;
            arch = null;
            path = null;
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
            Version version = Version.valueOf(moduleVersion);

            if (path != null) {
                if (path.startsWith("/") || path.endsWith("/")) {
                    throw new IllegalArgumentException(
                        "The path must not have leading or trailing \"'\".");
                }
            }

            JamModuleArchiveInfo jmai = new JamModuleArchiveInfo(repository,
                                                moduleName, version,
                                                platform, arch, path);
            if (result.contains(jmai)) {
                throw new IllegalArgumentException(
                    "The repository metadata file has duplicate module entry.");
            } else {
                result.add(jmai);
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
                    moduleName = tmp;
                    break;
                case VERSION:
                    moduleVersion = tmp;
                    break;
                case PLATFORM:
                    platform = tmp;
                    break;
                case ARCH:
                    arch = tmp;
                    break;
                case PATH:
                    path = tmp;
                    break;
                case NONE:
                    break;
            }
        }
    }
}
