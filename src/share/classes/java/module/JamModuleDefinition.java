/*
 * Copyright 2006-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package java.module;

import java.lang.annotation.Annotation;
import java.lang.reflect.Superpackage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.module.annotation.ExportLegacyClasses;
import java.module.annotation.ExportResources;
import java.module.annotation.ImportModule;
import java.module.annotation.ImportModules;
import java.module.annotation.MainClass;
import java.module.annotation.ModuleAttribute;
import java.module.annotation.ModuleAttributes;
import java.module.annotation.LegacyClasses;
import sun.module.annotation.ExportPackages;
import sun.module.JamUtils;

/**
 * This class represents a module definition based on a JAM file's
 * <code>MODULE-INF/MODULE.METADATA</code> file.
 * <p>
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleDefinitionContent
 *
 * @since 1.7
 */
final class JamModuleDefinition extends ModuleDefinition {

    private final String name;
    private final Version version;
    private byte[] metadata;
    private final Callable<byte[]> metadataHandle;
    private final ModuleDefinitionContent content;
    private final Repository repository;
    private final boolean moduleReleasable;
    private volatile Set<String> memberClasses;
    private volatile Set<String> exportedClasses;
    private volatile Set<String> exportedResources;
    private Map<Class,Annotation> annotations = null;

    JamModuleDefinition(String name, Version version, byte[] metadata,
            Callable<byte[]> metadataHandle, ModuleDefinitionContent content,
            Repository repository, boolean moduleReleasable) {
        this.name = name;
        this.version = version;
        this.metadata = metadata;
        this.metadataHandle = metadataHandle;
        this.content = content;
        this.repository = repository;
        this.moduleReleasable = moduleReleasable;
    }

    //
    // The metadata/superpackage arrangement below is temporary until the
    // full JSR 294 reflective APIs are in place
    //

    /**
     * Returns the contents of the MODULE-INF/MODULE.METADATA file.
     *
     * @return the contents of the MODULE-INF/MODULE.METADATA file.
     */
    synchronized byte[] getMetadata() {
        if (metadata != null) {
            if (metadata.length == 0) {
                throw new RuntimeException("metadata is not available.");
            }
            return metadata;
        }
        try {
            metadata = metadataHandle.call();
            return metadata;
        } catch (Exception e) {
            // XXX
            metadata = new byte[0];
            throw new RuntimeException(e);
        }
    }

    private volatile Superpackage superpackage;

    private Superpackage getSuperpackage() {
        if (superpackage == null) {
            synchronized (this) {
                if (superpackage == null) {
                    // XXX check name and version against metadata
                    superpackage = JamUtils.getSuperpackage(getMetadata());
                }
            }
        }
        return superpackage;
    }

    @Override
    public String getName() {
        return (name != null) ? name : getSuperpackage().getName();
    }

    @Override
    public java.module.Version getVersion() {
        if (version != null) {
            return version;
        }
        java.module.annotation.Version aversion = getAnnotation
            (java.module.annotation.Version.class);
        Version v = null;
        if (aversion != null) {
            try {
                v = Version.valueOf(aversion.value());
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
        return (v != null) ? v : Version.DEFAULT;
    }

    @Override
    public List<ImportDependency> getImportDependencies() {
        List<ImportDependency> dependencies = new ArrayList<ImportDependency>();
        Superpackage sp = getSuperpackage();
        ImportModules importModules = sp.getAnnotation(ImportModules.class);
        if (importModules != null) {
            for (ImportModule importModule : Arrays.asList(importModules.value())) {
                String name = importModule.name();
                VersionConstraint constraint = VersionConstraint.valueOf(importModule.version());
                boolean reexport = importModule.reexport();
                boolean optional = importModule.optional();
                dependencies.add(new ImportDependency(name, constraint, reexport, optional));
            }
        }
        return Collections.unmodifiableList(dependencies);
    }

    @Override
    public Set<String> getAttributeNames() {
        HashSet<String> names = new HashSet<String>();
        ModuleAttributes attrs = getAnnotation(ModuleAttributes.class);
        if (attrs != null) {
            for (ModuleAttribute attr : attrs.value()) {
                names.add(attr.name());
            }
        }
        return Collections.unmodifiableSet(names);
    }

    @Override
    public String getAttribute(String name) {
        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }
        ModuleAttributes attrs = getAnnotation(ModuleAttributes.class);
        if (attrs != null) {
            for (ModuleAttribute attr : attrs.value()) {
                if (name.equals(attr.name())) {
                    return attr.value();
                }
            }
        }
        return null;
    }

    @Override
    public Set<String> getMemberClasses() {
        if (memberClasses == null) {
            // Member classes consist of classes in the superpackage
            Set<String> s = new HashSet<String>
                            (Arrays.asList(getSuperpackage().getMemberTypes()));
            // As well as classes in the embedded legacy jars
            LegacyClasses legacyClassesAnnotation = getAnnotation(LegacyClasses.class);
            if (legacyClassesAnnotation != null) {
                s.addAll(Arrays.asList(legacyClassesAnnotation.value()));
            }
            memberClasses = Collections.unmodifiableSet(s);
        }
        return memberClasses;
    }

    @Override
    public Set<String> getExportedClasses() {
        if (exportedClasses == null) {
            // Exported classes consist of exported classes in the superpackage
            Set<String> s = new HashSet<String>
                            (Arrays.asList(getSuperpackage().getExportedTypes()));
            // As well as exported legacy classes in the embedded jars
            ExportLegacyClasses exportLegacyClassesAnnotation = getAnnotation(ExportLegacyClasses.class);
            if (exportLegacyClassesAnnotation != null) {
                LegacyClasses legacyClassesAnnotation = getAnnotation(LegacyClasses.class);
                if (legacyClassesAnnotation != null) {
                    s.addAll(Arrays.asList(legacyClassesAnnotation.value()));
                }
            }
            exportedClasses = Collections.unmodifiableSet(s);
        }
        return exportedClasses;
    }

    @Override
    public boolean isClassExported(String className) {
        // XXX convert class name?

        // XXX @ExportPackages is a workaround for building virtual modules
        // for the Java SE platform. It should be replaced after the
        // actual JSR 294 support arrives in javac.
        //
        ExportPackages exportPackages = getAnnotation(ExportPackages.class);
        if (exportPackages != null) {
            String[] p = exportPackages.value();
            if (p.length == 1 && p[0].equals("*")) {
                // "*" is exported by the "java.classpath" module.
                return true;
            }

            for (String s : p) {
                // Checks if the specified class is exported from this module.
                if (className.startsWith(s + ".")) {
                    return true;
                }
            }
            return false;
        } else {
            return getExportedClasses().contains(className);
        }
    }

    @Override
    public Set<String> getExportedResources() {
        if (exportedResources == null) {
            Set<String> s = new HashSet<String>();

            ExportResources exportResourcesAnnotation = getAnnotation(ExportResources.class);
            String[] filters = new String[0];
            if (exportResourcesAnnotation != null) {
                filters = exportResourcesAnnotation.value();
            }

            // Use regular expression to check if the filter pattern is acceptable.
            // Valid characters include alphanumerics, #$%&+-./?@_~, excluding "'()*,:;<=>[\]^`{|}
            //
            final String regExp = "\\p{Alnum}\\#\\$\\%\\&\\+\\-\\.\\?\\@\\_\\~";
            final String charRegExp = "[" + regExp + "]";
            final String directoryRegExp = "[" + regExp + "\\/]*";

            Pattern filterPattern = Pattern.compile("((" + charRegExp + "+\\/)|(\\*\\*\\/))*"
                                     + "(((" + charRegExp + ")*(\\*)?(" + charRegExp + ")*)|\\*\\*)");

            for (int i=0; i < filters.length; i++) {
                String filter = filters[i];

                // Check if the export resources filter matches the pattern
                if (filterPattern.matcher(filter).matches() == false)  {
                    // TODO: use logging
                    System.err.println("Warning: Unrecognized filter in @ExportResources: "
                                        + filters[i] + ", filter is ignored.");
                    continue;
                }

                /**
                 * Replaces the wildcard in the filter with regular expression.
                 * The wildcards are:
                 *
                 *    '?' matches a single character
                 *    '*' matches zero or more characters
                 *    '**' matches zero or more directories
                 *
                 * Valid characters include alphanumerics, #$%&+-./?@_~
                 * excluding "'()*,:;<=>[\]^`{|}
                 *
                 * Examples:
                 *
                 * "*" - all files under the root directory
                 * "**" - all files under the root directory and sub-directories
                 * "abc/*" - all files under the abc directory
                 * "abc/**" - all files under the abc directory and sub-directories
                 */

                if (filter.equals("*")) {
                    filter = charRegExp + "+";
                } else if (filter.equals("**")) {
                    filter = directoryRegExp;
                } else {
                    // Handles shorthand.
                    if (filter.endsWith("/")) {
                        filter = filter + "**";
                    }

                    // Replaces '**', '*', and '?' into other illegal characters first.
                    // This makes it easier to handle string subsitution.
                    filter = filter.replace("**", ">");
                    filter = filter.replace("*", "|");
                    filter = filter.replace("?", ":");

                    // Replaces '**/' with regular expression to matches zero or more
                    // directories. e.g.
                    //
                    // 1. '**/xyz.txt'
                    // 2. '**/**/xyz.txt'
                    // 3. 'abc/**/xyz.txt'
                    //
                    filter = filter.replace(">/", directoryRegExp);

                    // Handles specical case that the filter has '/**' at the end.
                    // Replaces it with regular expression to matches zero or
                    // more files and directories. e.g.
                    //
                    // 1. 'abc/**'
                    // 2. 'abc/def/**'
                    //
                    if (filter.endsWith("/>")) {
                        filter = filter.replace("/>", "\\/" + directoryRegExp);
                    }

                    // Replaces "*" with regular expression to match zero or more files.
                    filter = filter.replace("|",  charRegExp + "*");

                    // Replaces "?" with regular expression to match a valid character,
                    // excluding '/'
                    filter = filter.replace(":",  charRegExp);
                }

                try {
                    Pattern pattern = Pattern.compile(filter);

                    for (String resource : content.getEntryNames()) {
                        if (pattern.matcher(resource).matches())
                            s.add(resource);
                    }
                } catch (PatternSyntaxException pse) {
                    // TODO: use logging
                    System.err.println("Warning: Unrecognized filter in @ExportResources: "
                                        + filters[i] + ", filter is ignored.");
                    pse.printStackTrace();
                }
            }
            exportedResources = Collections.unmodifiableSet(s);
        }
        return exportedResources;
    }

    @Override
    public boolean isResourceExported(String name) {
        // XXX special hack for now
        if (getRepository() == Repository.getBootstrapRepository()) {
            // Module definitions from the bootstrap repository are expected
            // to export all resources.
            return true;
        } else {
            return getExportedResources().contains(name);
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null)  {
            throw new NullPointerException();
        }
        return getSuperpackage().getAnnotation(annotationClass);

    /*
        // XXX: Loading side files to synthetize annotaions would have
        // the side effect of downloading the JAM file eagerly in some
        // cases. This is undesirable, and thus the code is commented out
        // for now.

        // Determines if requested annotation is a stock annotation.
        if (annotationClass.equals(LegacyClasses.class) == false)  {
            return getSuperpackage().getAnnotation(annotationClass);
        }

        // Determines if requested annotation is a synthetic annotation.
        initAnnotationsIfNecessary();
        for (Class c : annotations.keySet()) {
            if (annotationClass.isAssignableFrom(c))
                return (T) annotations.get(c);
        }
        return null;
    */
    }


    @Override
    public synchronized List<Annotation> getAnnotations() {
        return Collections.unmodifiableList(Arrays.asList(getSuperpackage().getAnnotations()));
        /*
        // XXX: See comments in getAnnotation(Class<T>)

        initAnnotationsIfNecessary();
        return Collections.unmodifiableList(new ArrayList<Annotation>(annotations.values()));
        */
    }

    private void initAnnotationsIfNecessary() {
        if (annotations == null) {
            Map<Class, Annotation> annotationMap = new HashMap<Class, Annotation>();

            for (Annotation a : Arrays.asList(getSuperpackage().getAnnotations()))  {
                // Skips over LegacyClasses annotation if it exists in the superpackage
                if (a instanceof LegacyClasses)  {
                    continue;
                }
                else {
                    annotationMap.put(a.getClass(), a);
                }
            }

            // Constructs a legacy classes annotation on the fly based on the
            // content of MODULE-INF/legacy-classes.list
            final String legacyClassesEntry = "MODULE-INF/legacy-classes.list";
            try {
                if (content.hasEntry(legacyClassesEntry)) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(content.getEntryAsStream(legacyClassesEntry)));
                    Set<String> rc = new HashSet<String>();
                    String s;
                    while ((s = r.readLine()) != null) {
                        s = s.trim();
                        if (!s.equals("") && !s.startsWith("#")) {
                            rc.add(s);
                        }
                    }
                    r.close();

                    final String[] legacyClasses = new String[rc.size()];
                    int i = 0;
                    for (String lc : rc) {
                        legacyClasses[i++] = lc;
                    }

                    LegacyClasses legacyClassesAnnotation = new LegacyClasses() {
                        public String[] value() {
                            return legacyClasses;
                        }
                        public Class<? extends Annotation> annotationType() {
                            return LegacyClasses.class;
                        }
                    };
                    annotationMap.put(LegacyClasses.class, legacyClassesAnnotation);
                }
            } catch (IOException ioe) {
                // TODO: use logging
                System.err.println("Warning: Unrecognized file format in MODULE-INF/legacy-classes.list, "
                                    + "legacy classes list is ignored.");
                ioe.printStackTrace();
            }
            annotations = annotationMap;
        }
    }

    @Override
    public boolean isModuleReleasable() {
         return moduleReleasable;
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public ModuleDefinitionContent getModuleDefinitionContent() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new ModuleSystemPermission("accessModuleDefinitionContent"));
        }
        return content;
    }
}
