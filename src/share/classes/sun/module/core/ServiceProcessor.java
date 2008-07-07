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

package sun.module.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Service;
import java.util.ServiceProvider;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * An annotation processor for Service and ServiceProvider annotations.  It
 * generates corresponding files under META-INF in the destination directory.
 *
 * @since 1.7
 */
// XXX Increment to RELEASE_7 when that's in place.
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes({"java.util.Service", "java.util.ServiceProvider"})
public class ServiceProcessor extends AbstractProcessor {
    /**
     * Set of classes with the @Service annotation that were discovered
     * during this compilation.
     */
    private final Set<TypeElement> services = new HashSet<TypeElement>();

    /**
     * Mirrors of classes with the @ServiceProvider annotation that were
     * discovered during this compilation.
     */
    private final Set<TypeElement> providers = new HashSet<TypeElement>();

    /** For error reporting. */
    private Messager msg;

    /** Set of types supported. */
    private static Set<String> supportedTypes;

    public ServiceProcessor() {
    }

    @Override
    public void init(ProcessingEnvironment pe) {
        super.init(pe);
        this.msg = pe.getMessager();
    }

    /**
     *  Collect classes that have the Service or ServiceProcessor
     *  annotations.  When processing is over, write out the corresponding
     *  files under the META-INF directory.
     */
    public boolean process(
            Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            if (annotations.isEmpty()) {
                return true;
            }
            for (Element e : roundEnv.getElementsAnnotatedWith(Service.class)) {
                if (e instanceof TypeElement) {
                    services.add((TypeElement) e);
                } else {
                    error("Element " + e + " is not a TypeElement");
                }
            }
            for (Element e : roundEnv.getElementsAnnotatedWith(ServiceProvider.class)) {
                if (e instanceof TypeElement) {
                    providers.add((TypeElement) e);
                } else {
                    error("Element " + e + " is not a TypeElement");
                }
            }
        } else {
            createServiceIndex();
            createServices();
        }
        return true;
    }

    /**
     * Creates or updates the META-INF/service-index file.  Classes with the
     * Service annotation found in this compilation are added to the existing
     * file.
     */
    private void createServiceIndex() {
        if (services.size() == 0) {
            return;
        }

        FileHelper fh = null;
        try {
            fh = new FileHelper("META-INF/service-index", processingEnv);

            // Get existing services, merge in new ones
            Set<String> classnames = fh.getExistingEntries();
            for (TypeElement s : services) {
                classnames.add(s.asType().toString());
            }

            PrintWriter pw = fh.getPrintWriter();
            pw.println("# AUTO GENERATED - SERVICE CLASS");
            for (String name : classnames) {
                pw.println(name);
            }
            pw.close();
            if (pw.checkError()) {
                error(
                    "Unspecified error occured while writing service-index file: "
                    + fh.getFilename());
            }
        } catch (IOException ex) {
            error(
                "Error accessing the service-index file: " + fh.getFilename()
                + ": " + ex.getMessage());
        }
    }

    /**
     * Creates a META-INF/services/x.y.z for each ServiceProvider class found
     * in this compilation.  <em>Note:</em> The relationship between services
     * and service providers is based on subtyping.
     */
    private void createServices() {
        if (providers.size() == 0) {
            return;
        }

        // For each service provider, find the service it implements, and make a
        // map from services to service providers.
        Map<Name, Set<TypeElement>> providedServices =
            new HashMap<Name, Set<TypeElement>>();
        Types types = processingEnv.getTypeUtils();

        for (TypeElement p : providers) {
            List<? extends TypeMirror> supers = p.getInterfaces();
            for (TypeMirror tm : supers) {
                TypeElement clazz = (TypeElement) types.asElement(tm);
                addIfService(clazz, p, providedServices);
            }
            TypeMirror superclass = p.getSuperclass();
            while (superclass.getKind() != TypeKind.NONE) {
                TypeElement clazz = (TypeElement) types.asElement(superclass);
                addIfService(clazz, p, providedServices);
                superclass = clazz.getSuperclass();
            }
        }

        // For each service, create the corresponding META-INF/services file.
        for (Name s : providedServices.keySet()) {
            FileHelper fh = null;
            try {
                fh = new FileHelper("META-INF/services", s.toString(), processingEnv);

                // Get existing service providers, merge in new ones
                Set<String> classnames = fh.getExistingEntries();
                for (TypeElement p : providedServices.get(s)) {
                    classnames.add(p.toString());
                }

                PrintWriter pw = fh.getPrintWriter();
                pw.println("# AUTO GENERATED - SERVICES PROVIDERS");
                for (TypeElement p : providedServices.get(s)) {
                    pw.println(p.toString());
                }
                pw.close();
                if (pw.checkError()) {
                    error(
                        "Unspecified error occurred while writing services file for " + s);
                }
            } catch (IOException ex) {
                error(
                    "Error accessing the services file for " + fh.getFilename()
                    + ": " + ex.getMessage());
            }
        }
    }

    /**
     * If e has a Service annotation, add as one of its providers in the given
     * set.
     * @param e {@code TypeElement} that might be a
     * @param p {@code TypeElement} that is a {@ @ServiceProvider}
     * @param providedServices map from Service names to set of ServiceProviders
     */
    private void addIfService(TypeElement e, TypeElement p,
            Map<Name, Set<TypeElement>> providedServices) {
        if (e.getAnnotation(Service.class) != null) {
            Name serviceName = e.getQualifiedName();
            Set<TypeElement> ps = providedServices.get(serviceName);
            if (ps == null) {
                ps = new HashSet<TypeElement>();
                providedServices.put(serviceName, ps);
            }
            ps.add(p);
        }
    }

    /**
     * Helps in using FileObjects
     * @see #createServiceIndex
     * @see #createServices
     */
    private static class FileHelper {
        private final String prefix;
        private final String suffix;
        private final ProcessingEnvironment pe;

        private String filename;

        FileHelper(String path, ProcessingEnvironment pe) {
            this(path, null, pe);
        }

        FileHelper(String prefix, String suffix, ProcessingEnvironment pe) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.pe = pe;
        }

        String getFilename() {
            return filename;
        }

        /**
         * @return set of entries from the FileObject corresponding to the
         * {@code prefix} and {@suffix} with which this {@code FileHelper} was
         * created.
         */
        Set<String> getExistingEntries() throws IOException {
            Set<String> rc = new HashSet<String>();
            FileObject input = pe.getFiler().getResource(
                StandardLocation.CLASS_OUTPUT,
                "", prefix + (suffix == null ? "" : "/" + suffix));
            filename = input.toUri().getPath();
            File sFile = new File(filename);
            if (sFile.exists()) {
                BufferedReader r = null;
                try {
                    r = new BufferedReader(input.openReader(true));
                    String s;
                    while ((s = r.readLine()) != null) {
                        s = s.trim();
                        if (!s.startsWith("#")) {
                            rc.add(s);
                        }
                    }
                } finally {
                    if (r != null) {
                        r.close();
                    }
                }
            }
            return rc;
        }

        /**
         * @return a {@code PrintWriter} for writing to the FileObject
         * corresponding to the {@code prefix} and {@suffix} with which this
         * {@code FileHelper} was created.
         */
        PrintWriter getPrintWriter() throws IOException {
            new File(filename).getParentFile().mkdirs();
            FileObject output = pe.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT,
                "", prefix + (suffix == null ? "" : "/" + suffix),
                (TypeElement) null);
            return new PrintWriter(output.openWriter());
        }
    }

    void error(String s) {
        msg.printMessage(Diagnostic.Kind.ERROR, "ServiceProcessor: " + s);
    }
}
