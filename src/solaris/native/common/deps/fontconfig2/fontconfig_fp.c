/*
 * Copyright (c) 1998, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <fontconfig_fp.h>

#include <stdlib.h>
#include <string.h>

void* dlOpenFontConfig() {

    char *homeEnv;
    static char *homeEnvStr = "HOME="; /* must be static */
    void* libfontconfig = NULL;
#ifdef __solaris__
#define SYSINFOBUFSZ 8
    char sysinfobuf[SYSINFOBUFSZ];
#endif

    /* Private workaround to not use fontconfig library.
     * May be useful during testing/debugging
     */
    char *useFC = getenv("USE_J2D_FONTCONFIG");
    if (useFC != NULL && !strcmp(useFC, "no")) {
        return NULL;
    }

#ifdef __solaris__
    /* fontconfig is likely not properly configured on S8/S9 - skip it,
     * although allow user to override this behaviour with an env. variable
     * ie if USE_J2D_FONTCONFIG=yes then we skip this test.
     * NB "4" is the length of a string which matches our patterns.
     */
    if (useFC == NULL || strcmp(useFC, "yes")) {
        if (sysinfo(SI_RELEASE, sysinfobuf, SYSINFOBUFSZ) == 4) {
            if ((!strcmp(sysinfobuf, "5.8") || !strcmp(sysinfobuf, "5.9"))) {
                return NULL;
            }
        }
    }
#endif
#if defined(AIX)
    /* On AIX, fontconfig is not a standard package supported by IBM.
     * insted it has to be installed from the "AIX Toolbox for Linux Applications" 
     * site http://www-03.ibm.com/systems/power/software/aix/linux/toolbox/alpha.html
     * and will be installed under /opt/freeware/lib/libfontconfig.a.
     * Notice that the archive contains the real 32- and 64-bit shared libraries.
     * We first try to load 'libfontconfig.so' from the default library path in the 
     * case the user has installed a private version of the library and if that 
     * doesn't succeed, we try the version from /opt/freeware/lib/libfontconfig.a
     */
    libfontconfig = dlopen("libfontconfig.so", RTLD_LOCAL|RTLD_LAZY);
    if (libfontconfig == NULL) {
        libfontconfig = dlopen("/opt/freeware/lib/libfontconfig.a(libfontconfig.so.1)", RTLD_MEMBER|RTLD_LOCAL|RTLD_LAZY);
        if (libfontconfig == NULL) {
            return NULL;
        }
    }
#else
    /* 64 bit sparc should pick up the right version from the lib path.
     * New features may be added to libfontconfig, this is expected to
     * be compatible with old features, but we may need to start
     * distinguishing the library version, to know whether to expect
     * certain symbols - and functionality - to be available.
     * Also add explicit search for .so.1 in case .so symlink doesn't exist.
     */
    libfontconfig = dlopen(FONTCONFIG_DLL_VERSIONED, RTLD_LOCAL|RTLD_LAZY);
    if (libfontconfig == NULL) {
        libfontconfig = dlopen(FONTCONFIG_DLL, RTLD_LOCAL|RTLD_LAZY);
        if (libfontconfig == NULL) {
            return NULL;
        }
    }
#endif

    /* Version 1.0 of libfontconfig crashes if HOME isn't defined in
     * the environment. This should generally never happen, but we can't
     * control it, and can't control the version of fontconfig, so iff
     * its not defined we set it to an empty value which is sufficient
     * to prevent a crash. I considered unsetting it before exit, but
     * it doesn't appear to work on Solaris, so I will leave it set.
     */
    homeEnv = getenv("HOME");
    if (homeEnv == NULL) {
        putenv(homeEnvStr);
    }

    if (libfontconfig == NULL) {
        return NULL;
    }

    PatternBuild     =
        (FcPatternBuildFuncType)dlsym(libfontconfig, "FcPatternBuild");
    ObjectSetBuild   =
        (FcObjectSetFuncType)dlsym(libfontconfig, "FcObjectSetBuild");
    FontList         =
        (FcFontListFuncType)dlsym(libfontconfig, "FcFontList");
    PatternGetString =
        (FcPatternGetStringFuncType)dlsym(libfontconfig, "FcPatternGetString");
    StrDirname       =
        (FcStrDirnameFuncType)dlsym(libfontconfig, "FcStrDirname");
    PatternDestroy   =
        (FcPatternDestroyFuncType)dlsym(libfontconfig, "FcPatternDestroy");
    FontSetDestroy   =
        (FcFontSetDestroyFuncType)dlsym(libfontconfig, "FcFontSetDestroy");
#if 0
    Fini             =
        (FcFiniFuncType)dlsym(libfontconfig, "FcFini");
#endif
    NameParse = (FcNameParseFuncType)dlsym(libfontconfig, "FcNameParse");
    PatternAddString =
        (FcPatternAddStringFuncType)dlsym(libfontconfig, "FcPatternAddString");
    ConfigSubstitute =
        (FcConfigSubstituteFuncType)dlsym(libfontconfig, "FcConfigSubstitute");
    DefaultSubstitute = (FcDefaultSubstituteFuncType)
        dlsym(libfontconfig, "FcDefaultSubstitute");
    FontMatch = (FcFontMatchFuncType)dlsym(libfontconfig, "FcFontMatch");
    PatternGetBool = (FcPatternGetBoolFuncType)
        dlsym(libfontconfig, "FcPatternGetBool");
    PatternGetInteger = (FcPatternGetIntegerFuncType)
        dlsym(libfontconfig, "FcPatternGetInteger");
    PatternDestroy =
        (FcPatternDestroyFuncType)dlsym(libfontconfig, "FcPatternDestroy");
    GetVersion = (FcGetVersionFuncType)dlsym(libfontconfig, "FcGetVersion");
    PatternGetCharSet =
        (FcPatternGetCharSetFuncType)dlsym(libfontconfig,
                                           "FcPatternGetCharSet");
    FontSort =
        (FcFontSortFuncType)dlsym(libfontconfig, "FcFontSort");
    FontSetDestroy =
        (FcFontSetDestroyFuncType)dlsym(libfontconfig, "FcFontSetDestroy");
    CharSetUnion =
        (FcCharSetUnionFuncType)dlsym(libfontconfig, "FcCharSetUnion");
    CharSetSubtractCount =
        (FcCharSetSubtractCountFuncType)dlsym(libfontconfig,
                                              "FcCharSetSubtractCount");

    if (PatternBuild     == NULL ||
        ObjectSetBuild   == NULL ||
        PatternGetString == NULL ||
        FontList         == NULL ||
        StrDirname       == NULL ||
        PatternDestroy   == NULL ||
        FontSetDestroy   == NULL ||
        NameParse          == NULL ||
        PatternAddString   == NULL ||
        ConfigSubstitute   == NULL ||
        DefaultSubstitute  == NULL ||
        FontMatch          == NULL ||
        PatternGetBool     == NULL ||
        PatternGetInteger  == NULL ||
        PatternDestroy     == NULL ||
	GetVersion         == NULL ||
        PatternGetCharSet  == NULL ||
        CharSetUnion       == NULL ||
        GetVersion         == NULL ||
        CharSetSubtractCount == NULL) {/* problem with the library: return.*/
        dlCloseFontConfig (libfontconfig);
        return NULL;
    }

    /* Optionally get the cache dir locations. This isn't
     * available until v 2.4.x, but this is OK since on those later versions
     * we can check the time stamps on the cache dirs to see if we
     * are out of date. There are a couple of assumptions here. First
     * that the time stamp on the directory changes when the contents are
     * updated. Secondly that the locations don't change. The latter is
     * most likely if a new version of fontconfig is installed, but we also
     * invalidate the cache if we detect that. Arguably even that is "rare",
     * and most likely is tied to an OS upgrade which gets a new file anyway.
     */
    ConfigGetCacheDirs =
        (FcConfigGetCacheDirsFuncType)dlsym(libfontconfig,
                                            "FcConfigGetCacheDirs");
    StrListNext =
        (FcStrListNextFuncType)dlsym(libfontconfig, "FcStrListNext");
    StrListDone =
        (FcStrListDoneFuncType)dlsym(libfontconfig, "FcStrListDone");

    return libfontconfig;
}

void dlCloseFontConfig(void* libfontconfig)
{
    dlclose(libfontconfig);
}
