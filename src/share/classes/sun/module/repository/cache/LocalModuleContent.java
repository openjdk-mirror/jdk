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
import java.module.ModuleContent;
import java.net.URL;
import java.util.Set;
import java.security.CodeSigner;

/**
 * Local module content.
 *
 * @since 1.7
 */
final class LocalModuleContent extends CacheModuleContent {

    // ModuleDefInfo which this module content belongs
    private final LocalModuleDefInfo mdInfo;

    /**
     * Constructs a new local cache module content.
     */
    LocalModuleContent(LocalModuleDefInfo mdInfo) {
        super(mdInfo.getEntryDirectory(), mdInfo.getMetadataByteBuffer(), mdInfo.getModuleInfo());
        this.mdInfo = mdInfo;
    }

    @Override
    protected File getJamFile() {
        return mdInfo.getJamFile();
    }

    @Override
    protected Set<CodeSigner> getJamCodeSigners() {
        return mdInfo.getJamCodeSigners();
    }

    @Override
    protected boolean shouldCompareMetadataBytes() {
        // Module metadata does not need to be compared because the
        // original bytes were extracted directly from the JAM file.
        return false;
    }

    @Override
    public URL getLocation() {
        return mdInfo.getCodeBase();
    }

    @Override
    public boolean isDownloaded() {
        return true;
    }
}
