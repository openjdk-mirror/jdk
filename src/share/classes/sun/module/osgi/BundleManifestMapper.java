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

package sun.module.osgi;

import java.module.Modules;
import java.module.ModuleDefinition;
import java.module.ImportDependency;
import java.module.PackageDefinition;
import java.module.Version;
import java.module.VersionConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import static org.osgi.framework.Constants.*;

/**
 * This class maps from the bundle manifest to the ModuleDefinition.
 */
class BundleManifestMapper {

    private final ModuleDefinition moduleDef;
    private final Bundle bundle;
    private final Map<String, String> headers;
    private final String symbolicName;
    private static final String DEFAULT_OSGI_VERSION =
            org.osgi.framework.Version.emptyVersion.toString();

    /**
     * TODO:
     * <ol>
     * <li>getHeader(Locale)</li>
     * </ol>
     */
    BundleManifestMapper(ModuleDefinition modDef, Bundle bundle) {
        this.moduleDef = modDef;
        this.bundle = bundle;
        this.headers = new HashMap<String, String>();
        Dictionary dict = bundle.getHeaders();
        for (Enumeration keys = dict.keys(); keys.hasMoreElements();) {
            Object k = keys.nextElement();
            headers.put((String) k, (String) dict.get(k));
        }

        String header = headers.get(BUNDLE_SYMBOLICNAME);
        // XXX: for now - NullPointerException if header == null
        // should check the bundle version
        String[] entries = header.split(";");
        this.symbolicName = entries[0];
    }

    /**
     * Returns the Version object of the bundle.
     */
    static Version getVersion(Bundle bundle) {
        String osgiVersion = (String) bundle.getHeaders().get(BUNDLE_VERSION);
        return convertOSGiVersion(osgiVersion);
    }

    private static Version convertOSGiVersion(String osgiVersion) {
        // Comparing two versions with and without the qualifier
        //    OSGi version 1.2.3 > 1.2.3.qualifier
        //    Java module version 1.2.3 < 1.2.3-qualifier
        String version = osgiVersion.trim();
        org.osgi.framework.Version v =
                org.osgi.framework.Version.parseVersion(version);
        if (v.getQualifier().isEmpty()) {
            return Version.valueOf(version);
        } else {
            return Version.valueOf(v.getMajor(), v.getMinor(), v.getMicro(), 1, v.getQualifier());
        }
    }

    private static VersionConstraint convertOSGiVersionRange(String osgiVersionRange) {
        if (osgiVersionRange.startsWith("[") || osgiVersionRange.startsWith("(")) {
            // parse the version range
            int len = osgiVersionRange.length();
            String v[] = osgiVersionRange.substring(1, len-1).split(",");

            Version v0 = convertOSGiVersion(v[0]);
            Version v1 = convertOSGiVersion(v[1]);
            String versionRange = osgiVersionRange.substring(0,1) +
                v0 + "," + v1 + osgiVersionRange.substring(len-1, len);

            return VersionConstraint.valueOf(versionRange);
        } else {
            // single version - append "+" before conversion
            Version v = convertOSGiVersion(osgiVersionRange);
            return VersionConstraint.valueOf(osgiVersionRange.trim() + "+");
        }
    }

    String getSymbolicName() {
        // XXX: Felix implementation of bundle.getSymbolicName()
        // returns the entire Bundle-SymbolicName manifest header
        return symbolicName;
    }

    Version getVersion() {
        return convertOSGiVersion(headers.get(BUNDLE_VERSION));
    }

    List<ImportDependency> getImportDependencies() {
        List<ImportDependency> dependencies = new ArrayList<ImportDependency>();

        // parse Require-Bundle header
        String header = headers.get(REQUIRE_BUNDLE);
        if (header != null) {
            String[] requireBundles = header.split(",");
            for (String reqBundle : requireBundles) {
                String[] entries = reqBundle.split(";");
                String importName = entries[0].trim();
                String versionRange =
                        getAttribute(entries, BUNDLE_VERSION_ATTRIBUTE, DEFAULT_OSGI_VERSION);
                VersionConstraint versionConstraint = convertOSGiVersionRange(versionRange);
                String visibility =
                        getDirective(entries, VISIBILITY_DIRECTIVE, VISIBILITY_PRIVATE);
                String resolution =
                        getDirective(entries, RESOLUTION_DIRECTIVE, RESOLUTION_MANDATORY);

                dependencies.add(Modules.newModuleDependency(
                                     importName,
                                     versionConstraint,
                                     visibility.equals(VISIBILITY_REEXPORT),
                                     resolution.equals(RESOLUTION_OPTIONAL),
                                     buildAttributeMap(entries)));

            }
        }

        // parse Import-Package header

        header = headers.get(IMPORT_PACKAGE);
        if (header != null) {
            String[] importPkgs = header.split(",");

            for (String importPackage : importPkgs) {
                String[] entries = importPackage.split(";");
                String importName = entries[0].trim();
                String versionRange =
                        getAttribute(entries, VERSION_ATTRIBUTE, DEFAULT_OSGI_VERSION);
                VersionConstraint versionConstraint = convertOSGiVersionRange(versionRange);
                String resolution =
                        getDirective(entries, RESOLUTION_DIRECTIVE, RESOLUTION_MANDATORY);

                dependencies.add(Modules.newPackageDependency(
                                    importName,
                                    versionConstraint,
                                    false,
                                    resolution.equals(RESOLUTION_OPTIONAL),
                                    buildAttributeMap(entries)));

            }
        }
        return dependencies;
    }

    Set<PackageDefinition> getExportedPackageDefinitions() {
        Set<PackageDefinition> exports = new HashSet<PackageDefinition>();

        // parse Export-Package header
        String header = headers.get(EXPORT_PACKAGE);
        if (header != null) {
            String[] exportedPkgs = header.split(",");
            for (String exportPackage : exportedPkgs) {
                String[] entries = exportPackage.split(";");
                String exportName = entries[0].trim();
                String version =
                        getAttribute(entries, VERSION_ATTRIBUTE, DEFAULT_OSGI_VERSION);
                PackageDefinition pkgDef =
                    new OSGiPackageDefinition(exportName,
                        convertOSGiVersion(version),
                        moduleDef,
                        buildAttributeMap(entries));
                exports.add(pkgDef);
            }
        }
        return exports;
    }

    Set<PackageDefinition> getMemberPackageDefinitions() {
        Set<PackageDefinition> members = new HashSet<PackageDefinition>(getExportedPackageDefinitions());
        // TODO: add other member packages
        return members;
    }

    boolean isSingleton() {
        String header = headers.get(BUNDLE_SYMBOLICNAME);
        String value = "false"; // default
        if (header != null) {
            String[] entries = header.split(";");
            value = getDirective(entries, SINGLETON_DIRECTIVE, "false");
        }
        return Boolean.valueOf(value);
    }

    private String getDirective(String[] entries, String directive, String defaultValue) {
        for (String s : entries) {
            if (s.contains(":=")) {
                String[] ss = strtok(s, ":=");
                if (ss[0].equals(directive)) {
                    return ss[1];
                }
            }
        }
        return defaultValue;
    }

    private String getAttribute(String[] entries, String attribute, String defaultValue) {
        for (String s : entries) {
            if (s.contains("=")) {
                String[] ss = strtok(s, "=");
                if (ss[0].equals(attribute)) {
                    return ss[1];
                }
            }
        }
        return defaultValue;
    }

    /**
     * Returns the key and value in an array of String separated by
     * the given delimiter.
     *
     */
    private String[] strtok(String str, String delimiter) {
        String[] result = new String[2];

        String[] ss = str.trim().split(delimiter);
        assert ss.length == 2;
        result[0] = ss[0].trim();
        String value = ss[1].trim();
        if (value.startsWith("\"")) {
            result[1] = value.substring(1, value.length() - 1);
        } else {
            result[1] = value;
        }
        return result;
    }

    private Map<String, String> buildAttributeMap(String[] entries) {
        Map<String, String> attributes = new HashMap<String, String>();
        for (String s : entries) {
            String[] ss;
            if (s.contains(":=")) {
                ss = strtok(s, ":=");
            } else if (s.contains("=")) {
                ss = strtok(s, "=");
            } else {
                // Not a directive nor an attribute
                continue;
            }
            attributes.put(ss[0], ss[1]);
        }
        return attributes;
    }

    Map<String, String> getModuleAttributesMap() {
        // TODO: should we return all manifest headers?
        return Collections.unmodifiableMap(headers);
    }
}
