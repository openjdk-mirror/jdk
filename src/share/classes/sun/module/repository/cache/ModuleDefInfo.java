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
import java.module.Version;
import java.module.annotation.PlatformBinding;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import sun.module.JamUtils;
import sun.module.repository.RepositoryUtils;

/**
 * Module definition info.
 */
 public abstract class ModuleDefInfo {

    // Directory where this object lives.
    private final File entryDirectory;

    // Byte array that represents the module metadata
    private final byte[] metadataBytes;

    // ModuleInfo rectified from the module metadata
    private final ModuleInfo moduleInfo;

    // Module name
    private String moduleName;

    // Module version
    private Version moduleVersion = Version.DEFAULT;

    // Module's platform
    private String platform = null;

    // Module's architecture
    private String arch = null;

    // Module's content
    private ModuleContent content = null;

    /**
     * Constructs a new module definition info.
     *
     * @param entryDirectory directory where the object will live.
     * @param metadataBytes byte array that represents the module metadata
     * @param moduleInfo ModuleInfo recified from the module metadata
     */
     ModuleDefInfo(File entryDirectory, byte[] metadataBytes, ModuleInfo moduleInfo) {
        this.entryDirectory = entryDirectory;
        this.metadataBytes = metadataBytes;
        this.moduleInfo = moduleInfo;

        // Module name
        this.moduleName = moduleInfo.getName();

        // Module version
        java.module.annotation.Version versionAnnotation =
                moduleInfo.getAnnotation(java.module.annotation.Version.class);
        if (versionAnnotation != null)  {
            try {
                moduleVersion = Version.valueOf(versionAnnotation.value());
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        // Platform Binding
        PlatformBinding platformBinding =
                moduleInfo.getAnnotation(PlatformBinding.class);
        if (platformBinding != null) {
            platform = platformBinding.platform();
            arch = platformBinding.arch();
        }
    }

    /**
     * Returns the byte array of the module metadata.
     */
    public byte[] getMetadataBytes() {
        // XXX make a copy
        return metadataBytes;
    }

    /**
     * Returns the module name.
     */
    public String getName() {
        return moduleName;
    }

    /**
     * Returns the module version.
     */
    public Version getVersion()  {
        return moduleVersion;
    }

    /**
     * Returns the platform which the module is binded to.
     */
     public String getPlatform() {
        return platform;
     }

     /**
      * Returns the architecture which the module is binded to.
      */
     public String getArch()  {
         return arch;
     }

     /**
      * Determines if the module definition is platform and architecture neutral.
      *
      * @return true if the module archive is platform and architecture neutral;
      *         return false otherwise.
      */
     public boolean isPlatformArchNeutral()  {
         return (getPlatform() == null && getArch() == null);
     }

     /**
      * Returns true if the module definition supports the running platform and
      * architecture.
      */
     public boolean supportsRunningPlatformArch()  {
         if (isPlatformArchNeutral()) {
            return true;
         }

         return (RepositoryUtils.getPlatform().equals(getPlatform())
                && RepositoryUtils.getArch().equals(getArch()));
     }

     /**
      * Returns the module content that represents the exposed contents in the
      * jam file.
      */
     public abstract ModuleContent getModuleContent();

     /**
      * Returns the entry directory for this object.
      */
     /* package-private */
     File getEntryDirectory()  {
        return entryDirectory;
     }

     /**
      * Returns the ModuleInfo that represents the module in this object.
      */
     /* package-private */
     ModuleInfo getModuleInfo()  {
        return moduleInfo;
     }
 }
