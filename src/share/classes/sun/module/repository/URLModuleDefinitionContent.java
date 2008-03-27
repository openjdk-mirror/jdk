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

import java.io.*;
import java.module.*;
import java.module.annotation.JarLibraryPath;
import java.net.URL;
import java.security.CodeSigner;
import java.util.*;
import java.util.jar.*;
import sun.module.*;

/**
 * This {@code ModuleDefinitionContent} downloads the JAM for a
 * {@code ModuleDefinition} when that actual content is required.
 *
 * @since 1.7
 */
public class URLModuleDefinitionContent extends ModuleDefinitionContent {
    /**
     * Directory into which this content's JAM is downloaded.  Its name
     * is taken from the module's name and version (and platform and arch,
     * if the JAM is platform/arch - specific).
     */
    private final File baseDir;

    /** URLRepository containing the corresponding JAM. */
    private final URLRepository repository;

    /** Data describing the module (obtained from repository-metadata.xml). */
    private final ModuleInfo moduleInfo;

    /** The MODULE.METADATA corresponding to this content. */
    private final File metadataFile;

    /**
     * Default path in JAM file where legacy JAR files reside.  Value is
     * changed if module uses @JarLibraryPath (see {@link #setJarLibraryPath}).
     */
    private String jarLibraryPath = RepositoryUtils.getJarLibraryPathDefault();

    /**
     * Default path in JAM file where native library files reside.  Value is
     * changed module uses @NativeLibraryPath (see {@link
     * #setNativeLibraryPath}).
     */
    private static String nativeLibraryPath = RepositoryUtils.getNativeLibraryPathDefault();

    /**
     * List of the JAM corresponding to this content followed by any
     * embedded JARs contained in the JAM (which get extracted during download().
     */
    private List<JarFile> classPaths;

    /** Entry names of classes & resources. */
    private Set<String> entryNames = null;

    /** True if the JAM has been downloaded. */
    private boolean downloaded = false;

    /** True if downloading the JAM failed. */
    private boolean downloadFailed = false;

    /** Name used to identify this class in exceptions. */
    private static final String IDENT = "URLModuleDefinitionContent";

    URLModuleDefinitionContent(File baseDir, URLRepository repository,
            ModuleInfo moduleInfo, File metadataFile) throws IOException {
        this.baseDir = baseDir;
        this.repository = repository;
        this.moduleInfo = moduleInfo;
        this.metadataFile = metadataFile;
    }

    /**
     * Sets the path at which embedded JAR files are found in this content's
     * corresponding JAM file.
     */
    void setJarLibraryPath(String path) {
        jarLibraryPath = path;
    }

    /**
     * Sets the path at which native library files are found in this content's
     * corresponding JAM file if the given {@code platform} and {@code arch}
     * match the current platform and arch.
     */
    void setNativeLibraryPath(String platform, String arch, String path) {
        if (RepositoryUtils.getPlatform().equals(platform)
                && RepositoryUtils.getArch().equals(arch)) {
            nativeLibraryPath = path;
        }
    }

    @Override
    public File getNativeLibrary(String libraryName) {
        if (!downloadJamContent()) {
            return null;
        }
        File nativeLibrary = new File(baseDir, "bin" + File.separator + libraryName);
        if (nativeLibrary.exists() && nativeLibrary.isFile()) {
            return nativeLibrary;
        } else {
            return null;
        }
    }

    @Override
    public boolean hasEntry(String name) {
        return getEntryNames().contains(name);
    }

    @Override
    public Set<String> getEntryNames() {
        if (!downloadJamContent()) {
            return Collections.unmodifiableSet(new HashSet<String>());
        }
        if (entryNames == null) {
            Set<String> names = new HashSet<String>();
            boolean enclosingJam = true;

            for (JarFile jf : classPaths) {
                if (enclosingJam) {
                    // If it is the jam file, adds all entries
                    for (JarEntry je : Collections.list(jf.entries())) {
                        if (je.isDirectory()) {
                            continue;
                        }
                        names.add(je.getName());
                    }
                    enclosingJam = false;
                } else {
                    // If it is the embedded jar files, adds all entries except
                    // the ones under META-INF/ and MODULE-INF/
                    for (JarEntry je : Collections.list(jf.entries())) {
                        if (je.isDirectory()) {
                            continue;
                        }
                        String s = je.getName();
                        if (s.startsWith("META-INF/") == false
                            && s.startsWith("MODULE-INF/") == false) {
                            names.add(s);
                        }
                    }
                }
            }
            entryNames = Collections.unmodifiableSet(names);
        }
        return entryNames;
    }

    @Override
    public InputStream getEntryAsStream(String name) throws IOException {
        if (!downloadJamContent()) {
            return null;
        }
        for (JarFile jf : classPaths)   {
            JarEntry je = jf.getJarEntry(name);
            if (je != null) {
                return jf.getInputStream(je);
            }
        }
        return null;
    }

    @Override
    public CodeSigner[] getCodeSigners() {
        // TODO Replace w/ correct implementation.
        return null;
    }

    @Override
    public boolean isDownloaded() {
        return downloaded;
    }

    /**
     * Downloads the JAM that is associated with the ModuleDefinition.
     * @return true if JAM content was downloaded without errors
     * @throws RuntimeException if an IOException occurs
     */
    private synchronized boolean downloadJamContent() {
        if (downloaded) {
            return true;
        }
        if (downloadFailed) {
            return false;
        }

        final String[] ext = {
            ".jam.pack.gz",
            ".jam"
        };

        String jamName = JamUtils.getJamFilenameNoExt(moduleInfo.getName(), moduleInfo.getVersion(),
                                                      moduleInfo.getPlatform(), moduleInfo.getArch());
        String path = moduleInfo.getCanonicalizedPath();
        JarFile jamFile = null;
        try {
            URL jamURL = null;
            InputStream is = null;
            for (int i = 0; i < ext.length; i++) {
                jamURL = new URL(repository.getSourcePath() + path + "/" + jamName + ext[i]);
                try {
                    is = jamURL.openStream();
                    break;
                } catch (IOException ex) {
                    continue;
                }
            }
            if (is == null) {
                throw new IOException(IDENT + ": Cannot access JAM file for "
                    + jamName + " at " + repository.getSourcePath() + path);
            } else {
                File jamDir = new File(baseDir, "jam");
                if (!jamDir.isDirectory() && !jamDir.mkdirs()) {
                    throw new IOException(IDENT + ": Cannot create directory for downloading module "
                        + jamName + " at "
                        + jamDir.getAbsolutePath());
                }

                File tmpFile = new File(jamDir, jamName + ".jam");
                if (jamURL.getFile().endsWith(".jam.pack.gz")) {
                    JamUtils.unpackStreamAsFile(is, tmpFile);
                } else {
                    JamUtils.saveStreamAsFile(is, tmpFile);
                }
                JamUtils.close(is);
                jamFile = new JarFile(tmpFile);

                byte[] moduleMetadata = JamUtils.getMetadata(
                    jamFile);

                compareMetadata(moduleMetadata);

                classPaths = new ArrayList<JarFile>();
                classPaths.add(jamFile);

                // Extract legacy/embedded JAR files and native libraries
                File[] jarFiles = RepositoryUtils.extractJarsAndLibs(
                    jamFile, jarLibraryPath, new File(baseDir, "lib"),
                    nativeLibraryPath, new File(baseDir, "bin"));
                for (File f : jarFiles) {
                    classPaths.add(new JarFile(f, false));
                }
            }
           downloaded = true;
        } catch (IOException ex) {
            JamUtils.close(jamFile);
            downloadFailed = true;

            // XXX Use logging
            System.err.println(IDENT + " for repository at " + repository.getSourceLocation()
                               +": Failed to download " + path + jamName + ": " + ex.getMessage());
        }
        return downloaded;
    }

    /**
     * @throws RuntimeException if the content of the given {@code moduleMetadata}
     * does not match that which was downloaded earlier and is in File {@code
     * metadataFile}.
     */
    private void compareMetadata(byte[] moduleMetadata) {
        boolean match = false;
        if (metadataFile.length() == moduleMetadata.length) {
            try {
                byte[] repoData = JamUtils.readFile(metadataFile);
                match = Arrays.equals(repoData, moduleMetadata);
            } catch (IOException ex) {
                throw new RuntimeException(
                    "IOException while comparing " + JamUtils.MODULE_METADATA + " from repository "
                    + repository.getName() + " at " + repository.getSourceLocation()
                    + " with that of module: " + ex);
            }
        }
        if (!match) {
            throw new RuntimeException(
                "Module definition for module " + moduleInfo.getName()
                + " that was downloaded during initialization of repository "
                + repository.getName() + " at " + repository.getSourceLocation()
                + " does not match the module definition in the corresponding JAM file.");
        }
    }
}
