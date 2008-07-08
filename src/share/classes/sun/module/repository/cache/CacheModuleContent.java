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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.module.ModuleContent;
import java.module.annotation.JarLibraryPath;
import java.module.annotation.NativeLibraryPath;
import java.module.annotation.NativeLibraryPaths;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.CodeSigner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import sun.module.JamUtils;
import sun.module.repository.RepositoryUtils;


/**
 * Base class to implement ModuleContent in the repository cache.
 *
 * @since 1.7
 */
abstract class CacheModuleContent implements ModuleContent {

    // Entry directory
    private final File entryDirectory;

    // Metadata bytes
    private final byte[] metadataBytes;

    // ModuleInfo that represents the metadata
    private final ModuleInfo moduleInfo;

    // Classpaths searched for classes & resources.
    private final List<JarFile> classPaths = new ArrayList<JarFile>();

    // Entry names of classes & resources.
    private Set<String> entryNames = null;

    // true if the JAM file's embedded JAR files & native libs have been expanded.
    private boolean initialized = false;

    // Exception thrown during initialization
    private IOException initializationException = null;

    /**
     * Constructs a new cache module content.
     */
    CacheModuleContent(File entryDirectory, byte[] metadataBytes, ModuleInfo moduleInfo) {
        this.entryDirectory = entryDirectory;
        this.metadataBytes = metadataBytes;
        this.moduleInfo = moduleInfo;
    }

    /**
     * Returns the JAM file that this module content represents.
     *
     * @return the JAM file.
     * @throws IOException if an I/O exception has occurred.
     */
    protected abstract File getJamFile() throws IOException;

    /**
     * Returns an unmodifiable set of code signers of the JAM file.
     *
     * @return an unmodifiable set of code signers.
     */
    protected abstract Set<CodeSigner> getJamCodeSigners();

    /**
     * Returns true if the module metadata in the JAM file should be compared
     * with the metadata byte array; return false otherwise.
     */
    protected boolean shouldCompareMetadataBytes() {
        return true;
    }

    /**
     * Initialize the module content if necessary, e.g. extract
     * embedded JAR and native library files if not done already/
     */
    private boolean initializeIfNecessary() {
        if (initialized) {
            return true;
        }
        if (initializationException != null) {
            return false;
        }

        // Determines if jar library path has been overridden by @JarLibraryPath
        String jarLibraryPath = JamUtils.MODULE_INF + "/lib";
        JarLibraryPath jarLibraryPathAnnotation = moduleInfo.getAnnotation(JarLibraryPath.class);
        if (jarLibraryPathAnnotation != null) {
            jarLibraryPath = jarLibraryPathAnnotation.value();
        }

        // Determines if native library path has been overridden by @NativeLibraryPath
        String nativeLibraryPath = JamUtils.MODULE_INF + "/bin/"
                                    + RepositoryUtils.getPlatform() + "/"
                                    + RepositoryUtils.getArch();
        NativeLibraryPaths nativeLibraryPathsAnnotation = moduleInfo.getAnnotation(NativeLibraryPaths.class);
        if (nativeLibraryPathsAnnotation != null) {
            for (NativeLibraryPath nlp : nativeLibraryPathsAnnotation.value()) {
                // Retrieves native library path that matches the current
                // platform and architecture
                if (nlp.platform().equals(RepositoryUtils.getPlatform())
                    && nlp.arch().equals(RepositoryUtils.getArch())) {
                    nativeLibraryPath = nlp.path();
                    break;
                }
            }
        }

        /**
         * The embedded jar libraries and native libraries are expanded in the
         * entry directory as follows:
         *
         * <entry-directory>/lib/<embedded-jar-libraries>
         * <entry-directory>/bin/<native libraries>
         */
        File embeddedJarsDirectory = new File(entryDirectory, "lib");
        File nativeLibsDirectory = new File(entryDirectory, "bin");

        try {
            // Open the JAM file with no verification. This operation
            // may trigger JAM file to be downloaded if not yet happened.
            JarFile jamFile = new JarFile(getJamFile(), false);

            // Add JAM file into the classpath
            classPaths.add(jamFile);

            if (shouldCompareMetadataBytes()) {
                // Extracts MODULE.METADATA from the JAM file
                byte[] moduleMetadataBytes = JamUtils.getMetadataBytes(jamFile);
                compareMetadataBytes(moduleMetadataBytes);
            }

            // Extract embedded JAR files and native libraries into the entry directory.
            File[] jarFiles = RepositoryUtils.extractJarsAndLibs(
                jamFile, jarLibraryPath, embeddedJarsDirectory,
                nativeLibraryPath, nativeLibsDirectory);

            // Add each embedded jars into the classpath
            for (File f : jarFiles) {
                // Embedded JAR files are not verified even if they are signed.
                classPaths.add(new JarFile(f, false));
            }

            initialized = true;
            return true;
        } catch (IOException ex) {
            initializationException = ex;
            // XXX Use logging

            // Close each of the jar file
            for (JarFile jf : classPaths) {
                JamUtils.close(jf);
            }
            classPaths.clear();

            return false;
        }
    }

    /**
     * Compare the module metadata bytes to see if they are identical.
     *
     * @throws IOException if the content of the given module metadata
     * does not match what the module metadata we obtained earlier.
     */
    private void compareMetadataBytes(byte[] moduleMetadataBytes) throws IOException {

    System.out.println(" " + metadataBytes.length);
    System.out.println(" " + moduleMetadataBytes.length);

        if ((metadataBytes.length != moduleMetadataBytes.length)
            || Arrays.equals(metadataBytes, moduleMetadataBytes) == false) {
            throw new IOException("Mismatch between MODULE.METADATA file and the one in the JAM file");
        }
    }

    @Override
    public final File getNativeLibrary(String libraryName) throws IOException {
        if (initializeIfNecessary() == false) {
            throw initializationException;
        }
        File nativeLibrary = new File(new File(entryDirectory, "bin"), libraryName);
        if (nativeLibrary.exists() && nativeLibrary.isFile()) {
            return nativeLibrary;
        } else {
            return null;
        }
    }

    @Override
    public final boolean hasEntry(String name) throws IOException {
        return getEntryNames().contains(name);
    }

    @Override
    public final Set<String> getEntryNames() throws IOException {
        if (initializeIfNecessary() == false) {
            throw initializationException;
        }
        if (entryNames == null) {
            Set<String> names = new HashSet<String>();
            boolean enclosingJam = true;
            for (JarFile jf : classPaths) {
                if (enclosingJam) {
                    // If it is the jam file, adds all entries
                    for (JarEntry je : Collections.list(jf.entries())) {
                        if (je.isDirectory())  {
                            continue;
                        }
                        names.add(je.getName());
                    }
                    enclosingJam = false;
                } else {
                    // If it is an embedded jar file, adds all entries except
                    // the ones under META-INF/ and MODULE-INF
                    for (JarEntry je : Collections.list(jf.entries())) {
                        if (je.isDirectory())  {
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
    public final ReadableByteChannel getEntryAsChannel(String name) throws IOException {
        if (initializeIfNecessary() == false)  {
            throw initializationException;
        }
        for (JarFile jf : classPaths)   {
            JarEntry je = jf.getJarEntry(name);
            if (je != null) {
                return Channels.newChannel(jf.getInputStream(je));
            }
        }
        return null;
    }

    @Override
    public ByteBuffer getEntryAsByteBuffer(String name) throws IOException {
        ReadableByteChannel src = getEntryAsChannel(name);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel dest = Channels.newChannel(baos);

        ByteBuffer buffer = ByteBuffer.allocate(8192);

        // Read the stream until it is EOF
        while (src.read(buffer) != -1) {
             // prepare the buffer to be drained
             buffer.flip();

             // make sure the buffer was fully drained.
             while (buffer.hasRemaining()) {
                dest.write (buffer);
             }

             // make the buffer empty, ready for filling
             buffer.clear();
        }

        // Close the channels
        src.close();
        dest.close();

        // Convert to byte array
        return ByteBuffer.wrap(baos.toByteArray()).asReadOnlyBuffer();
    }

    @Override
    public final Set<CodeSigner> getCodeSigners() throws IOException {
        if (initializeIfNecessary() == false)  {
            throw initializationException;
        }

        return getJamCodeSigners();
    }

    @Override
    public abstract boolean isDownloaded();
}
