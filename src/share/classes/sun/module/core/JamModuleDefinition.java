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

package sun.module.core;

import java.lang.annotation.Annotation;
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
import java.module.ImportDependency;
import java.module.Module;
import java.module.Modules;
import java.module.ModuleContent;
import java.module.ModuleDefinition;
import java.module.ModuleDependency;
import java.module.ModuleSystem;
import java.module.ModuleSystemPermission;
import java.module.PackageDefinition;
import java.module.Repository;
import java.module.Version;
import java.module.VersionConstraint;
import java.module.annotation.Attribute;
import java.module.annotation.Attributes;
import java.module.annotation.ExportResources;
import java.module.annotation.ImportModule;
import java.module.annotation.ImportModules;
import java.module.annotation.MainClass;
import sun.module.JamUtils;

/**
 * This class represents a module definition based on a JAM file's
 * <code>MODULE-INF/MODULE.METADATA</code> file.
 * <p>
 * @see java.module.ModuleDefinition
 * @see java.module.ModuleContent
 *
 * @since 1.7
 */
public final class JamModuleDefinition extends AbstractModuleDefinition {

    private final String name;
    private final Version version;
    private byte[] metadata;
    private final Callable<byte[]> metadataHandle;
    private volatile Set<String> memberClasses;
    private volatile Set<String> exportedClasses;
    private volatile Set<String> exportedResources;
    private volatile boolean memberClassesNotAvailable = false;
    private volatile boolean exportedResourcesNotAvailable = false;
    private volatile Set<PackageDefinition> memberPackageDefs;
    private volatile Set<PackageDefinition> exportedPackageDefs;
    private volatile Map<Class,Annotation> annotations = null;

    public JamModuleDefinition(ModuleSystem moduleSystem,
            String name, Version version, byte[] metadata,
            Callable<byte[]> metadataHandle, ModuleContent content,
            Repository repository, boolean moduleReleasable) {
        super(moduleSystem, name, version, content, repository, moduleReleasable);
        this.name = name;
        this.version = version;
        this.metadata = metadata;
        this.metadataHandle = metadataHandle;
    }

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

    private volatile ModuleInfo moduleInfo;

    private ModuleInfo getModuleInfo() {
        if (moduleInfo == null) {
            synchronized (this) {
                if (moduleInfo == null) {
                    // XXX check name and version against metadata
                    moduleInfo = ModuleInfo.getModuleInfo(getMetadata());
                }
            }
        }
        return moduleInfo;
    }

    @Override
    public String getName() {
        return (name != null) ? name : getModuleInfo().getName();
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
    public Set<String> getMemberClasses() {
        if (memberClassesNotAvailable) {
            throw new UnsupportedOperationException("Module " + getName() +
                "does not support the member class list.");
        }

        if (memberClasses == null) {
            try {
                // Member classes consist of all types in the module
                Set<String> s = new HashSet<String>();

                for (String className : getModuleContent().getEntryNames()) {
                    // Skip classes in META-INF/ or MODULE-INF/
                    if (className.startsWith("META-INF/")
                        || className.startsWith("MODULE-INF/")) {
                        continue;
                    }

                    if (className.endsWith(".class")) {
                        className = className.substring(0, className.length() - 6).replace('/', '.');
                        s.add(className);
                    }
                }

                memberClasses = Collections.unmodifiableSet(s);
            } catch (IOException ioe) {
                memberClassesNotAvailable = true;
                throw new UnsupportedOperationException(ioe);
            }
        }
        return memberClasses;
    }

    @Override
    public Set<String> getExportedClasses() {
        if (exportedClasses == null) {
            // Exported classes consist of all exported types in the module
            Set<String> s =
                new HashSet<String>(Arrays.asList(getModuleInfo().getExportedClasses()));
            exportedClasses = Collections.unmodifiableSet(s);
        }
        return exportedClasses;
    }

    @Override
    public Set<PackageDefinition> getMemberPackageDefinitions() {
        if (memberPackageDefs == null) {
            List<String> memberPackages = Arrays.asList(getModuleInfo().getMemberPackages());

            HashSet<PackageDefinition> packageDefs = new HashSet<PackageDefinition>();
            for (String s : memberPackages) {
                packageDefs.add(new JamPackageDefinition(s, Version.DEFAULT, this));
            }
            memberPackageDefs = Collections.unmodifiableSet(packageDefs);
        }
        return memberPackageDefs;
    }

    @Override
    public Set<PackageDefinition> getExportedPackageDefinitions()  {
        if (exportedPackageDefs == null) {
            List<String> exportedPackages = Arrays.asList(getModuleInfo().getExportedPackages());

            HashSet<PackageDefinition> packageDefs = new HashSet<PackageDefinition>();
            for (String s : exportedPackages) {
                packageDefs.add(new JamPackageDefinition(s, Version.DEFAULT, this));
            }
            exportedPackageDefs = Collections.unmodifiableSet(packageDefs);
        }
        return exportedPackageDefs;
    }

    @Override
    public Set<String> getExportedResources() {
        if (exportedResourcesNotAvailable) {
            throw new UnsupportedOperationException("Module " + getName() +
                "does not support the exported resources list.");
        }

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

                    for (String resource : getModuleContent().getEntryNames()) {
                        if (pattern.matcher(resource).matches())
                            s.add(resource);
                    }
                } catch (PatternSyntaxException pse) {
                    // TODO: use logging
                    System.err.println("Warning: Unrecognized filter in @ExportResources: "
                                        + filters[i] + ", filter is ignored.");
                    pse.printStackTrace();
                } catch (IOException ioe) {
                    exportedResourcesNotAvailable = true;
                    throw new UnsupportedOperationException(ioe);
                }
            }
            exportedResources = Collections.unmodifiableSet(s);
        }
        return exportedResources;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null)  {
            throw new NullPointerException();
        }
        return getModuleInfo().getAnnotation(annotationClass);
    }

    @Override
    public synchronized List<Annotation> getAnnotations() {
        return Collections.unmodifiableList(Arrays.asList(getModuleInfo().getAnnotations()));
    }

}
