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
import java.io.IOException;
import java.module.ModuleContent;
import java.security.CodeSigner;
import java.util.jar.JarFile;

/**
 * Local module definition info.
 *
 * @since 1.7
 */
final class LocalModuleDefInfo extends ModuleDefInfo {

    // Jam file
    private final File jamFile;

    // Code signers of the JAM file
    private final CodeSigner[] codeSigners;

    // Module's content
    private ModuleContent content = null;

    /**
     * Constructs a new local module definition info.
     *
     * @param entryDirectory directory where the object will live.
     * @param metadataBytes byte array that represents the module metadata
     * @param moduleInfo ModuleInfo recified from the module metadata
     * @param jamFile jam file
     * @param codeSigners an array of code signers who signed the JAM file
     */
    LocalModuleDefInfo(File entryDirectory, byte[] metadataBytes,
                       ModuleInfo moduleInfo, File jamFile,
                       CodeSigner[] codeSigners)  {
        super(entryDirectory, metadataBytes, moduleInfo);
        this.jamFile = jamFile;
        this.codeSigners = codeSigners;
    }

     /**
      * Returns the module content that represents the exposed contents in the
      * jam file.
      */
     public ModuleContent getModuleContent() {
        // Creates the module content lazily.
        if (content == null) {
            content = new LocalModuleContent(this);
        }
        return content;
     }

     /**
      * Returns the jam file for this object.
      */
     /* package-private */
     File getJamFile() {
        return jamFile;
     }

     /**
      * Returns the code signers of the JAM file.
      */
    CodeSigner[] getJamCodeSigners() {
        return codeSigners;
     }
}
