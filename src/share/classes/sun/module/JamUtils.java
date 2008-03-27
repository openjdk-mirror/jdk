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

package sun.module;

import java.lang.reflect.Superpackage;
import java.io.*;
import java.module.Version;
import java.net.*;
import java.security.AccessController;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Unpacker;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipFile;

import sun.security.action.GetPropertyAction;

/**
 * Jam Utilities class.
 *
 * @since 1.7
 */
public final class JamUtils {

    public static final boolean DEBUG;

    static {
        DEBUG = (AccessController.doPrivileged(new GetPropertyAction("java.module.debug")) != null);
    }

    public static final int BUFFER_SIZE = 8192;

    public static final String MODULE_INF = "MODULE-INF";

    public static final String MODULE_METADATA = "MODULE.METADATA";

    public static final String MODULE_INF_METADATA = MODULE_INF + "/" + MODULE_METADATA;

    private JamUtils() {
    }

    public static File getFile(URL url) throws IOException {
        if (!url.getProtocol().equals("file")) {
            throw new IOException("Not a file URL: " + url);
        }

        try {
            return new File(url.toURI());
        } catch (URISyntaxException urse) {
            throw new IOException("URI parsing error: " + url);
        }
    }

    public static final FileFilter DIRECTORY_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };

    public static final FilenameFilter JAM_JAR_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".jam") || name.endsWith(".jar");
        }
    };

    public static final FilenameFilter JAM_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".jar") || name.endsWith(".jam") || name.endsWith(".jam.pack.gz");
        }
    };

    public static final FilenameFilter JAR_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".jar");
        }
    };

    public static final FilenameFilter CLASS_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".class");
        }
    };


    public static File createTempDir() throws IOException {
        File rc = File.createTempFile("jam-" + System.currentTimeMillis(), ".tmp");
        rc.deleteOnExit();
        return rc.getParentFile();
    }

    public static byte[] readFile(File file) throws IOException {
        if (file.isFile() == false) {
            throw new IOException("Not a regular file: " + file);
        }
        long llen = file.length();
        if (llen > 64 * 1024 * 1024) { // 64 MB
            throw new IOException("File too large: " + file);
        }
        InputStream in = new FileInputStream(file);
        int len = (int)llen;
        byte[] data = new byte[len];
        int ofs = 0;
        while (len > 0) {
            int n = in.read(data, ofs, len);
            if (n < 0) {
                break;
            }
            len -= n;
            ofs += n;
        }
        in.close();
        if (len != 0) {
            throw new IOException("Could not read file");
        }
        return data;
    }

    public static byte[] readStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];

        while (true) {
            int n = in.read(buffer, 0, buffer.length);
            if (n < 0) {
                break;
            }
            out.write(buffer, 0, n);
        }

        in.close();
        return out.toByteArray();
    }

    public static void close(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public static void close(ZipFile c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Extracts all files from the {@code sourceJam} into {@code destDir}.
     * Really, it's just "cd $destDir; jar xf $sourceJam".
     *
     * @param sourceJam .jam file to extract.
     * @param destDir directory into which files are extracted.
     * @throws IOException if any problem occurs.
     */
    public static void unjam(File sourceJam, File destDir) throws IOException {
        JarFile jamFile = null;

        try {
            jamFile = new JarFile(sourceJam, false);
            for (Enumeration<JarEntry> e = jamFile.entries(); e.hasMoreElements();) {
                JarEntry je = e.nextElement();

                // Skip directory entries
                if (je.isDirectory())
                    continue;

                // Store entry as file in the destination.
                File outputFile = new File(destDir, je.getName());
                saveStreamAsFile(jamFile.getInputStream(je), outputFile);
            }
        } finally {
            close(jamFile);
        }
    }

    /**
     * Read the content from the input stream and store the content
     * in the output file.  Parent directories for the output file
     * are created.  The stream is closed after the content is stored.
     *
     * @param is Input stream of the content.
     * @param outputFile File where content is stored.
     * @throws IOException if any I/O operation fails.
     */
    public static void saveStreamAsFile(InputStream is, File outputFile) throws IOException {
        // Create parent directories
        outputFile.getParentFile().mkdirs();

        OutputStream os = new FileOutputStream(outputFile);
        byte[] buffer = new byte[BUFFER_SIZE];

        // Read the stream until it is EOF
        while (true) {
            int n = is.read(buffer, 0, buffer.length);
            if (n < 0) {
                break;
            }
            os.write(buffer, 0, n);
        }

        close(is);
        close(os);
    }


    /**
     * Unpacks data from {@code is} that is in .pack.gz form, and writes it
     * into a file {@code jamFile}.
     *
     * @param is input that gets unpacked
     * @param jamFile file into which unpacked data gets written
     * @throws IOException if an error occurs in unpacking or storing
     */
    public static void unpackStreamAsFile(InputStream is, File jamFile) throws IOException {
        GZIPInputStream gis = new GZIPInputStream(is, JamUtils.BUFFER_SIZE);
        JarOutputStream jos = new JarOutputStream(
            new BufferedOutputStream(
                new FileOutputStream(jamFile), JamUtils.BUFFER_SIZE));
        Unpacker unpacker = Pack200.newUnpacker();
        unpacker.unpack(gis, jos);
        jos.close();
        gis.close();
    }

    /**
     * Copies {@code src} to {@code dst}
     *
     * @param src File to be copied
     * @param dst Destination to which {@code src} is copied
     * @throws IOException if an I/O error occurs
     */
    public static void copyFile(File src, File dst) throws IOException {
        FileInputStream fis = new FileInputStream(src);
        saveStreamAsFile(fis, dst);
        close(fis);
    }

    /**
     * Recursively delete the given {@code dir} and all its contents.
     * @return true if all deletions were successful; false if any were
     * unsuccessful (all recursive deletions are attempted regardless of any
     * failures).
     */
    public static boolean recursiveDelete(File dir) throws IOException {
        boolean rc = true;
        if (dir.isDirectory()) {
            // Deletes directory
            File[] contents = dir.listFiles();
            if (contents != null) {
                for (File file : contents) {
                    boolean ok = recursiveDelete(file);
                    if (!ok && DEBUG) println("recursiveDelete could not delete " + dir.getCanonicalPath());
                    rc &= ok;
                }
            }
        }

        if (dir.exists()) {
            // Delete now-empty directory, or whatever else it might be, if it exists.
            boolean ok = dir.delete();
            if (!ok && DEBUG) println("recursiveDelete could not delete " + dir.getCanonicalPath());
            rc &= ok;
        }

        return rc;
    }

    private static void println(String s) { System.out.println(s); }

    private static final class Loader extends ClassLoader {
        Loader() {
            super(null);
        }
        Superpackage doDefineSuperPackage(byte[] data) throws ClassFormatError {
            return defineSuperpackage(null, data, 0, data.length);
        }
    }

    public static Superpackage getSuperpackage(byte[] data) throws ClassFormatError {
        // we use a new ClassLoader for each Superpackage object so that we
        // can support multiple Superpackages of the same name.
        // The final API will work differently.
        Loader loader = new Loader();
        return loader.doDefineSuperPackage(data);
    }

    /**
     * Gets module metadata from a JAM file.
     *
     * @param jamFile JAM file
     * @return a byte[] of data from the MODULE.METADATA file within a JAM file.
     */
    public static byte[] getMetadata(JarFile jamFile) throws IOException {
        JarEntry je = jamFile.getJarEntry("MODULE-INF/MODULE.METADATA");
        if (je == null) {
            throw new IllegalArgumentException(
                jamFile + " does not contain MODULE-INF/MODULE.METADATA");
        }
        InputStream is = jamFile.getInputStream(je);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int count = 0;
        byte[] buf = new byte[JamUtils.BUFFER_SIZE];
        while ((count = is.read(buf, 0, buf.length)) >= 0) {
            baos.write(buf, 0, count);
        }
        JamUtils.close(is);
        return baos.toByteArray();
    }

    /**
     * Returns jam filename with no file extension, based on module name,
     * version, platform and architecture.
     *
     * @param name module name
     * @param version module version
     * @param platform target platform
     * @param arch target architecture
     * @return jam filename with no file extension
     */
    public static String getJamFilenameNoExt(String name, Version version, String platform, String arch)   {
        return name + "-" + version + ((platform == null) ? "" : "-" + platform + "-" + arch);
    }

    /**
     * Returns jam filename based on module name, version, platform and architecture.
     *
     * @param name module name
     * @param version module version
     * @param platform target platform
     * @param arch target architecture
     * @return jam filename
     */
    public static String getJamFilename(String name, Version version, String platform, String arch)  {
        return getJamFilenameNoExt(name, version, platform, arch) + ".jam";
    }
}
