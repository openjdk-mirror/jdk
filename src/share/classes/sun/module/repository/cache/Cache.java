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

package sun.module.repository.cache;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.module.Version;
import sun.module.JamUtils;
import sun.module.repository.RepositoryUtils;

/**
 * Repository cache.
 */
 public final class Cache {

    // Root directory of the cache.
    private File rootDirectory;

    /**
     * Constructs a new repository cache instance.
     *
     * @param rootDirectory root directory where the cache will live.
     */
     private Cache(File rootDirectory)  {
        this.rootDirectory = rootDirectory;
    }

    /**
     * Returns a new repository cache instance. The cache will live in a temporary
     * location.
     */
    public static Cache newInstance() throws IOException  {
        // Creates the cache in the default temp directory.
        return newInstance(JamUtils.getTempDir());
    }

    /**
     * Returns a new repository cache instance that lives in the specified cache directory.
     *
     * @param cacheDirectory cache directory where the cache will live.
     */
    public static Cache newInstance(File cacheDirectory) throws IOException  {
        File tmpDir = new File(cacheDirectory, "repository-cache-" + System.currentTimeMillis());
        tmpDir.deleteOnExit();
        return new Cache(tmpDir);
    }

    /**
     * Returns a module definition info that is created based on the specified jam file.
     *
     * @param source source location
     * @param file jam file.
     * @throws IOException if there is an I/O error occurred.
     */
    public ModuleDefInfo getModuleDefInfo(URL source, File file) throws IOException {
        return getModuleDefInfo(source, file, RepositoryUtils.shouldShadowCopyFiles());
     }

     /**
      * Returns a module definition info that is created based on the specified jam file.
      *
      * @param source source location
      * @param file jam file.
      * @param shadowCopyFiles true if the specified jam file should be shadow copied.
      * @throws IOException if there is an I/O error occurred.
      */
     public ModuleDefInfo getModuleDefInfo(URL source, File file, boolean shadowCopyFiles) throws IOException  {
        //
        // Creates a module-specific directory under the root directory.
        //
        // Note that the module-specific directory name is random; the
        // the location is stored in cache entry that will be used for
        // subsequence lookup, and there is no need to have fancy directory
        // convention. Randomness also helps when sometimes we may have
        // more than one cache entry for the same module (e.g. a module
        // is replaced during repository's reload()).
        //
        File entryDirectory = new File(rootDirectory, file.getName() + "-" + System.currentTimeMillis());
        entryDirectory.deleteOnExit();

        File jamFile = null;
        String filename = file.getName();

        if (filename.endsWith(".jam.pack.gz")) {
            entryDirectory.mkdirs();

            // Unpack file into <entry-directory>
            File tmpFile = new File(entryDirectory, filename.substring(0, filename.length() - 8));
            JamUtils.unpackJamPackGz(file, tmpFile);
            tmpFile.deleteOnExit();

            jamFile = tmpFile;
        } else {
            // Shadow copy the original file into the cache first. This is to
            // avoid file locking on some operating systems, etc.
            if (shadowCopyFiles)  {
                entryDirectory.mkdirs();

                // Copy file into <entry-directory>
                File tmpFile = new File(entryDirectory, file.getName());
                JamUtils.copyFile(file, tmpFile);
                tmpFile.deleteOnExit();

                jamFile = tmpFile;
            } else {
                jamFile = file;
            }
        }


        JarFile f = null;
        try {
            // Open JAM file, and it should be verified if it is signed.
            f = new JarFile(jamFile, true);

            // Checks if the jam file is consistently signed by the same
            // signers.
            Set<CodeSigner> codeSigners = JamUtils.getCodeSigners(f);

            // Extracts MODULE.METADATA from the jam file.
            byte[] metadataBytes = JamUtils.getMetadataBytes(f);

            // Determines if the MODULE.METADATA file is well-formed
            ModuleInfo moduleInfo = ModuleInfo.getModuleInfo(metadataBytes);

            return new LocalModuleDefInfo(entryDirectory, metadataBytes, moduleInfo, jamFile, source, codeSigners);
        } catch (ClassFormatError cfe) {
            throw new IOException("MODULE.METADATA is malformed in " + file.getName(), cfe);
        } finally {
            // Close the JAM file so the file is no longer opened.
            JamUtils.close(f);
        }
    }

    /**
     * Returns a module definition info that is created based on the
     * specified jam file from a URL repository.
     *
     * @param codebase source location of the URL repository
     * @param name module's name
     * @param version module's version
     * @param platform module's platform
     * @param arch module's arch
     * @throws IOException if there is an I/O error occurred.
     */
     public ModuleDefInfo getModuleDefInfo(URL codebase, String name, Version version,
                                        String platform, String arch) throws IOException  {
        String path;
        if (platform == null || arch == null) {
            // portable module
            path = name + "/" + version;
        } else {
            // platform specific module
            path = name + "/" + version + "/" + platform + "-" + arch;
        }

        return getModuleDefInfo(codebase, name, version, platform, arch, path);
    }

    /**
     * Returns a module definition info that is created based on the specified
     * jam file from a URL repository.
     *
     * @param codebase source location of the URL repository
     * @param name module's name
     * @param version module's version
     * @param platform module's platform
     * @param arch module's arch
     * @param path path of the module metadata and module archive under the source
     *        location
     * @throws IOException if there is an I/O error occurred.
     */
     public ModuleDefInfo getModuleDefInfo(URL codebase, String name, Version version,
                                        String platform, String arch, String path) throws IOException   {

        URL metadataUrl = new URL(codebase + path + "/" + JamUtils.MODULE_METADATA);
        byte[] metadataBytes;

        // Loads and read the MODULE.METADATA from the source location
        URLConnection uc = metadataUrl.openConnection();
        metadataBytes = JamUtils.getInputStreamAsBytes(uc.getInputStream());

        // Determines if the MODULE.METADATA file is well-formed
        ModuleInfo moduleInfo = null;
        try  {
            moduleInfo = ModuleInfo.getModuleInfo(metadataBytes);
        } catch (ClassFormatError cfe)  {
            throw new IOException("MODULE.METADATA is malformed: " + metadataUrl, cfe);
        }

        // Checks if module's name and version in MODULE.METADATA are the ones
        // expected.
        if (moduleInfo.getName().equals(name) == false) {
            throw new IOException("Unexpected module's name in MODULE.METADATA: " +
                        moduleInfo.getName() + " != " + name + ", " + metadataUrl);
        }
        java.module.annotation.Version aversion = moduleInfo.getAnnotation
            (java.module.annotation.Version.class);
        Version v = Version.DEFAULT;
        if (aversion != null) {
            try {
                v = Version.valueOf(aversion.value());
            } catch (IllegalArgumentException e)  {
                // ignore
            }
        }
        if (v.equals(version) == false) {
            throw new IOException("Unexpected module's version in MODULE.METADATA: " +
                        v + " != " + version + ", " + metadataUrl);
        }

        // Checks if module's platform and arch in MODULE.METADATA are the ones
        // expected.
        java.module.annotation.PlatformBinding platformBinding = moduleInfo.getAnnotation
            (java.module.annotation.PlatformBinding.class);
        if (platformBinding == null) {
            if (platform != null || arch != null) {
                throw new IOException("Expected module's platform binding was missing in MODULE.METADATA: "
                    + metadataUrl);
            }
        } else {
            if (!platformBinding.platform().equals(platform)
                    || !platformBinding.arch().equals(arch)) {
                throw new IOException("Unexpected module's platform binding was found in MODULE.METADATA: "
                    + metadataUrl);
            }
        }

        File entryDirectory = new File(rootDirectory, "url" + "-" + System.currentTimeMillis());
        return new URLModuleDefInfo(entryDirectory, metadataBytes, moduleInfo,
                                    codebase, path);
    }

    /**
     * Shutdown the repository cache.
     */
    public void shutdown() {
        try {
            if (rootDirectory != null) {
                JamUtils.recursiveDelete(rootDirectory);
            }
        } catch (IOException ioe) {
            // XXX ignore the exception for now
        }
        rootDirectory = null;
    }
 }
