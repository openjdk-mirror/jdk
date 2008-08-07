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

package sun.module.bootstrap;

import java.module.ModuleDefinition;
import java.module.annotation.ImportModule;
import java.module.annotation.ImportModules;
import java.module.annotation.ServiceProvider;
import java.module.annotation.ServiceProviders;
import java.module.annotation.Services;
import java.module.annotation.Version;
import java.util.ArrayList;
import java.util.List;
import sun.module.annotation.ModuleName;
import sun.module.annotation.ExportPackages;

/**
 * Definitions of the virtual modules for the Java SE platform.
 *
 * @since 1.7
 */
public final class VirtualModuleDefinitions {

    /**
     * Bootstrap classloader.
     *
     * Inherit implementations of loadClass() and getResource(). With a null
     * parent, they delegate to bootstrap, which is a first order
     * approximation of what we want.
     *
     * XXX This will likely have to be revisited.
     */
    private static final ClassLoader BOOTSTRAP_CLASSLOADER = new ClassLoader(null) {
    };

    private static final String PLATFORM_VERSION = "1.7";

    /**
     * Java SE core platform module, version 1.7
     */
    @Version(PLATFORM_VERSION)
    @ModuleName("java.se.core")
    @Services({
        "java.nio.channels.spi.SelectorProvider",
        "java.nio.charset.spi.CharsetProvider",
        "java.rmi.server.RMIClassLoaderSpi",
        "java.sql.Driver",
        "javax.imageio.spi.ImageReader",
        "javax.imageio.spi.ImageWriter",
        "javax.imageio.spi.ImageTranscoder",
        "javax.imageio.spi.ImageInputStream",
        "javax.imageio.spi.ImageOutputStream",
        "javax.management.remote.JMXConnectorProvider",
        "javax.management.remote.JMXConnectorServerProvider",
        "javax.naming.ldap.StartTlsResponse",
        "javax.print.PrintServiceLookup",
        "javax.print.StreamPrintServiceFactory",
        "com.sun.tools.attach.spi.AttachProvider",
        "com.sun.jdi.connect.Connector",
        "com.sun.jdi.connect.Transport"
    })
    // XXX Note that some providers are platform-specific: For now they are
    // added here but it seems we need platform-specific virtual modules.
    @ServiceProviders({
        @ServiceProvider(service="com.sun.mirror.apt.AnnotationProcessorFactory",
                         providerClass="com.sun.istack.internal.ws.AnnotationProcessorFactoryImpl"),
        // XXX Need to handle platform-specific providers
        //@ServiceProvider(service="com.sun.tools.attach.spi.AttachProvider", // Solaris
        //                 providerClass="sun.tools.attach.SolarisAttachProvider"),
        //@ServiceProvider(service="com.sun.tools.attach.spi.AttachProvider", // Windows
        //                 providerClass="sun.tools.attach.WindowsAttachProvider"),
        //@ServiceProvider(service="com.sun.tools.attach.spi.AttachProvider", // Linux
        //                 providerClass="sun.tools.attach.LinuxAttachProvider"),
        //@ServiceProvider(service="javax.print.PrintServiceLookup",
        //                 providerClass="sun.print.UnixPrintServiceLookup"),
        @ServiceProvider(service="javax.print.StreamPrintServiceFactory",
                         providerClass="sun.print.PSStreamPrinterFactory")
    })
    @ExportPackages({
        "java",
        "javax.accessibility",
        "javax.activation",
        "javax.activity",
        // "javax.annotation" - in common annotations and annotation processing module
        "javax.crypto",
        "javax.imageio",
        "javax.jws",
        // "javax.lang" - in annotation processing module
        "javax.management",
        "javax.naming",
        "javax.net",
        "javax.print",
        "javax.rmi",    // XXX: There is a problem because javax.rmi.CORBA would match.
                        // We'll deal with it when JSR 294 support arrives.
        "javax.rmi.ssl",
        // "javax.rmi.CORBA" in corba module
        // "javax.script" in scripting module
        "javax.security",
        "javax.smartcardio",
        "javax.sound",
        "javax.sql",
        "javax.swing",
        // "javax.tools" in javacompiler module
        "javax.transaction",
        // "javax.xml" in jaxp, jaxb, and soap module
        "sunw",
        "com.sun",
        "org.ietf.jgss",
        "org.jcp.xml.dsig"
    })
    private static final class JAVA_SE_CORE_PLATFORM {
        // empty
    }

    /**
     * CORBA API, version 3.0
     */
    @Version("3.0")
    @ModuleName("corba")
    @ExportPackages({
        "javax.rmi.CORBA",
        "org.omg.CORBA",
        "org.omg.CORBA.DynAnyPackage",
        "org.omg.CORBA.ORBPackage",
        "org.omg.CORBA.portable",
        "org.omg.CORBA.TypeCodePackage",
        "org.omg.CORBA_2_3",
        "org.omg.CORBA_2_3.portable",
        "org.omg.CosNaming",
        "org.omg.CosNaming.NamingContextExtPackage",
        "org.omg.CosNaming.NamingContextPackage",
        "org.omg.Dynamic",
        "org.omg.DynamicAny",
        "org.omg.DynamicAny.DynAnyFactoryPackage",
        "org.omg.DynamicAny.DynAnyPackage",
        "org.omg.IOP",
        "org.omg.IOP.CodecFactoryPackage",
        "org.omg.IOP.CodecPackage",
        "org.omg.Messaging",
        "org.omg.PortableInterceptor",
        "org.omg.PortableInterceptor.ORBInitInfoPackage",
        "org.omg.PortableServer",
        "org.omg.PortableServer.CurrentPackage",
        "org.omg.PortableServer.POAManagerPackage",
        "org.omg.PortableServer.POAPackage",
        "org.omg.PortableServer.portable",
        "org.omg.PortableServer.ServantLocatorPackage",
        "org.omg.SendingContext",
        "org.omg.stub.java.rmi"
    })
    @ImportModules({
        @ImportModule(name="java.se.core")
    })
    private static final class CORBA {
        // empty
    }

    /**
     * Java API for XML Processing (JAXP), version 1.4
     */
    @Version("1.4")
    @ModuleName("javax.xml")
    @ExportPackages({
        "javax.xml",
        "javax.xml.datatype",
        "javax.xml.namespace",
        "javax.xml.parsers",
        "javax.xml.stream",
        "javax.xml.stream.events",
        "javax.xml.stream.util",
        "javax.xml.transform",
        "javax.xml.transform.dom",
        "javax.xml.transform.sax",
        "javax.xml.transform.stax",
        "javax.xml.transform.stream",
        "javax.xml.validation",
        "javax.xml.xpath",
        "org.w3c.dom",
        "org.w3c.dom.bootstrap",
        "org.w3c.dom.events",
        "org.w3c.dom.ls",
        "org.w3c.dom.ranges",
        "org.w3c.dom.traversal",
        "org.xml.sax",
        "org.xml.sax.ext",
        "org.xml.sax.helpers"
    })
    @ImportModules({
        @ImportModule(name="java.se.core")
    })
    private static final class JAXP {
        // empty
    }

    /**
     * Java Architecture for XML Binding (JAXB), version 2.0
     */
    @Version("2.0")
    @ModuleName("javax.xml.bind")
    @ExportPackages({
        "javax.xml.bind",
        "javax.xml.bind.annotation",
        "javax.xml.bind.annotation.adapters",
        "javax.xml.bind.attachment",
        "javax.xml.bind.helpers",
        "javax.xml.bind.util"
    })
    @ImportModules({
        @ImportModule(name="java.se.core")
    })
    private static final class JAXB {
        // empty
    }

    /**
     * Java API for XML-Based Web Services (JAX-WS), version 2.0
     */
    @Version("2.0")
    @ModuleName("javax.xml.ws")
    @ExportPackages({
        "javax.xml.ws",
        "javax.xml.ws.handler",
        "javax.xml.ws.handler.soap",
        "javax.xml.ws.http",
        "javax.xml.ws.soap",
        "javax.xml.ws.spi"
    })
    @ImportModules({
        @ImportModule(name="java.se.core")
    })
    private static final class JAX_WS {
        // empty
    }

    /**
     * SOAP with Attachments API for Java (SAAJ), version 1.3
     */
    @Version("1.3")
    @ModuleName("javax.xml.soap")
    @ExportPackages({
        "javax.xml.soap"
    })
    @ImportModules({
        @ImportModule(name="java.se.core")
    })
    private static final class SAAJ {
        // empty
    }

    /**
     * Java Compiler API, version 1.0
     */
    @Version("1.0")
    @ModuleName("javax.tools")
    @ExportPackages({
        "javax.tools"
    })
    @ImportModules({
        @ImportModule(name="java.se.core")
    })
    private static final class JAVA_COMPILER {
        // empty
    }

    /**
     * Pluggable Annotation Processing API, version 1.0
     */
    @Version("1.0")
    @ModuleName("javax.annotation.processing")
    @ExportPackages({
        "javax.annotation.processing",
        "javax.lang.model",
        "javax.lang.model.element",
        "javax.lang.model.type",
        "javax.lang.model.util"
    })
    @ImportModules({
        @ImportModule(name="java.se.core")
    })
    private static final class ANNOTATION_PROCESSING {
        // empty
    }

    /**
     * Common Annotations for the Java Platform, version 1.0
     */
    @Version("1.0")
    @ModuleName("javax.annotation")
    @ExportPackages({
        "javax.annotation"
    })
    @ImportModules({
        @ImportModule(name="java.se.core")
    })
    private static final class COMMON_ANNOTATIONS {
        // empty
    }

    /**
     * Scripting for the Java Platform, version 1.0
     */
    @Version("1.0")
    @ModuleName("javax.script")
    @ExportPackages({
        "javax.script"
    })
    @ImportModules({
        @ImportModule(name="java.se.core")
    })
    private static final class SCRIPTING {
        // empty
    }

    /**
     * Java SE full platform module, version 1.7
     */
    @Version(PLATFORM_VERSION)
    @ModuleName("java.se")
    @ExportPackages({
        // export nothing
    })
    @ImportModules({
        @ImportModule(name="java.se.core", version=PLATFORM_VERSION, reexport=true),
        @ImportModule(name="corba", version="3.0", reexport=true),
        @ImportModule(name="javax.xml", version="1.4", reexport=true),
        @ImportModule(name="javax.xml.bind", version="2.0", reexport=true),
        @ImportModule(name="javax.xml.ws", version="2.0", reexport=true),
        @ImportModule(name="javax.xml.soap", version="1.3", reexport=true),
        @ImportModule(name="javax.annotation", version="1.0", reexport=true),
        @ImportModule(name="javax.annotation.processing", version="1.0", reexport=true),
        @ImportModule(name="javax.script", version="1.0", reexport=true),
        @ImportModule(name="javax.tools", version="1.0", reexport=true)
    })
    private static final class JAVA_SE_FULL_PLATFORM {
        // empty
    }

    /**
     * Java classpath module, version 1.7
     */
    @Version(PLATFORM_VERSION)
    @ModuleName("java.classpath")
    @ExportPackages({
        "*"     // export everything
    })
    @ImportModules({
        @ImportModule(name="java.se")
    })
    private static final class JAVA_CLASSPATH {
        // empty
    }

    /**
     * Returns a list of virtual module definitions for the Java SE platform.
     */
    @SuppressWarnings("unchecked")
    static List<ModuleDefinition> getModuleDefinitions() {
        Class[] metadataClasses = { JAVA_SE_CORE_PLATFORM.class,
                                    CORBA.class,
                                    JAXP.class,
                                    JAXB.class,
                                    JAX_WS.class,
                                    SAAJ.class,
                                    JAVA_COMPILER.class,
                                    ANNOTATION_PROCESSING.class,
                                    COMMON_ANNOTATIONS.class,
                                    SCRIPTING.class,
                                    JAVA_SE_FULL_PLATFORM.class,
                                    JAVA_CLASSPATH.class };

        List<ModuleDefinition> moduleDefs = new ArrayList<ModuleDefinition>();

        for (Class clazz : metadataClasses) {
            ModuleName moduleNameAnnotation = (ModuleName) clazz.getAnnotation(ModuleName.class);
            Version versionAnnotation = (Version) clazz.getAnnotation(Version.class);
            String name = moduleNameAnnotation.value();
            String version = versionAnnotation.value();
            moduleDefs.add(new VirtualModuleDefinition(name, java.module.Version.valueOf(version), clazz));
        }
        return moduleDefs;
    }
}
