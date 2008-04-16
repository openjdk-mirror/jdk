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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.module.ModuleContent;
import java.net.URL;
import java.security.CodeSigner;
import java.util.jar.JarFile;
import sun.module.JamUtils;
import sun.module.repository.RepositoryUtils;

/**
 * URL module content.
 *
 * @since 1.7
 */
final class URLModuleContent extends CacheModuleContent {

    // URLModuleDefInfo which this module content belongs
    private final URLModuleDefInfo mdInfo;

    // Jam file
    private File jamFile;

    // Code signers of the JAM file
    private CodeSigner[] codeSigners;

    // Flag to indicate if the module archive has been downloaded.
    private boolean downloaded = false;

    /**
     * Constructs a new URL module content.
     */
    URLModuleContent(URLModuleDefInfo mdInfo) {
        super(mdInfo.getEntryDirectory(), mdInfo.getMetadataBytes(), mdInfo.getModuleInfo());
        this.mdInfo = mdInfo;
    }

    @Override
    protected File getJamFile() throws IOException {
        if (jamFile != null) {
            return jamFile;
        }

        String jamName = mdInfo.getName() + "-" + mdInfo.getVersion();
        if (mdInfo.getPlatform() != null && mdInfo.getArch() != null) {
            jamName += "-" + mdInfo.getPlatform() + "-" + mdInfo.getArch();
        }

        jamFile = null;
        InputStream is = null;

        try {
            String[] ext = { ".jam.pack.gz", ".jam" };
            URL jamURL = null;
            File file = null;

            if (mdInfo.getCodeBase().getProtocol().equals("file")) {
                // file based
                File sourceFile = null;
                for (String s : ext) {
                    jamURL = new URL(mdInfo.getCodeBase() + mdInfo.getPath() + "/" + jamName + s);
                    try {
                        sourceFile = JamUtils.getFile(jamURL);
                        if (sourceFile != null && sourceFile.exists() && sourceFile.isFile()) {
                            break;
                        }
                    } catch (IOException ex) {
                        continue;
                    }
                }

                if (sourceFile == null) {
                    throw new FileNotFoundException("File not found: " + jamURL);
                }

                if (jamURL.getFile().endsWith(".jam")
                    && RepositoryUtils.shouldShadowCopyFiles() == false) {
                    // This is an optimization. Use the JAM file directory
                    // from the source location.
                    file = sourceFile;
                } else {
                    is = new FileInputStream(sourceFile);
                }
            } else {
                // URL based
                IOException ioe = null;
                for (int i = 0; i < ext.length; i++) {
                    jamURL = new URL(mdInfo.getCodeBase() + mdInfo.getPath() + "/" + jamName + ext[i]);
                    try {
                        is = jamURL.openStream();
                        break;
                    } catch (IOException ex) {
                        // ignore the exception for now
                        ioe = ex;
                        continue;
                    }
                }

                // If no JAM file is downloaded, rethrows last IOException
                if (is == null && ioe != null) {
                    throw ioe;
                }
            }

            if (file == null) {
                //
                // The jam file is downloaded in the cache entry as follows:
                //
                // <entry-directory>/<jam-file>
                //
                file = new File(mdInfo.getEntryDirectory(), jamName + ".jam");
                file.getParentFile().mkdirs();
                file.deleteOnExit();

                if (jamURL.getFile().endsWith(".jam.pack.gz")) {
                    JamUtils.unpackStreamAsFile(is, file);
                } else {
                    JamUtils.saveStreamAsFile(is, file);
                }
            }

            JarFile f = null;
            try {
                // Open JAM file, and it should be verified if it is signed.
                f = new JarFile(file, true);

                // Checks if the jam file is consistently signed by the same
                // signers.
                codeSigners = JamUtils.getCodeSigners(f);
            } finally {
                // Close the JAM file so the file is no longer opened.
                JamUtils.close(f);
            }

            // finally assign the file if everything succeeds
            jamFile = file;

            downloaded = true;

            return jamFile;
        } finally {
            JamUtils.close(is);
        }
    }

    @Override
    protected CodeSigner[] getJamCodeSigners() {
        return codeSigners;
    }

    @Override
    public boolean isDownloaded() {
        return downloaded;
    }
}
