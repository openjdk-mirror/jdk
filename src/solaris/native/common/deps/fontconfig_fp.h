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

#ifndef __FONTCONFIG_FP_H__
#define __FONTCONFIG_FP_H__

#include <dlfcn.h>
#ifndef __linux__ /* i.e. is solaris */
#include <link.h>
#endif

#include <fontconfig/fontconfig.h>

void* dlOpenFontConfig();
void dlCloseFontConfig(void *libfontconfig);

typedef void* (FcFiniFuncType)();
typedef FcConfig* (*FcInitLoadConfigFuncType)();
typedef FcPattern* (*FcPatternBuildFuncType)(FcPattern *orig, ...);
typedef FcObjectSet* (*FcObjectSetFuncType)(const char *first, ...);
typedef FcFontSet* (*FcFontListFuncType)(FcConfig *config,
                                         FcPattern *p,
                                         FcObjectSet *os);
typedef FcResult (*FcPatternGetBoolFuncType)(const FcPattern *p,
                                               const char *object,
                                               int n,
                                               FcBool *b);
typedef FcResult (*FcPatternGetIntegerFuncType)(const FcPattern *p,
                                                const char *object,
                                                int n,
                                                int *i);
typedef FcResult (*FcPatternGetStringFuncType)(const FcPattern *p,
                                               const char *object,
                                               int n,
                                               FcChar8 ** s);
typedef FcChar8* (*FcStrDirnameFuncType)(const FcChar8 *file);
typedef void (*FcPatternDestroyFuncType)(FcPattern *p);
typedef void (*FcFontSetDestroyFuncType)(FcFontSet *s);
typedef FcPattern* (*FcNameParseFuncType)(const FcChar8 *name);
typedef FcBool (*FcPatternAddStringFuncType)(FcPattern *p,
                                             const char *object,
                                             const FcChar8 *s);
typedef void (*FcDefaultSubstituteFuncType)(FcPattern *p);
typedef FcBool (*FcConfigSubstituteFuncType)(FcConfig *config,
                                             FcPattern *p,
                                             FcMatchKind kind);
typedef FcPattern* (*FcFontMatchFuncType)(FcConfig *config,
                                          FcPattern *p,
                                          FcResult *result);
typedef FcFontSet* (*FcFontSetCreateFuncType)();
typedef FcBool (*FcFontSetAddFuncType)(FcFontSet *s, FcPattern *font);

typedef FcResult (*FcPatternGetCharSetFuncType)(FcPattern *p,
                                                const char *object,
                                                int n,
                                                FcCharSet **c);
typedef FcFontSet* (*FcFontSortFuncType)(FcConfig *config,
                                         FcPattern *p,
                                         FcBool trim,
                                         FcCharSet **csp,
                                         FcResult *result);
typedef FcCharSet* (*FcCharSetUnionFuncType)(const FcCharSet *a,
                                             const FcCharSet *b);
typedef FcChar32 (*FcCharSetSubtractCountFuncType)(const FcCharSet *a,
                                                   const FcCharSet *b);

typedef int (*FcGetVersionFuncType)();

typedef FcStrList* (*FcConfigGetCacheDirsFuncType)(FcConfig *config);
typedef FcChar8* (*FcStrListNextFuncType)(FcStrList *list);
typedef FcChar8* (*FcStrListDoneFuncType)(FcStrList *list);

FcInitLoadConfigFuncType InitLoadConfig;
FcPatternBuildFuncType PatternBuild;
FcObjectSetFuncType ObjectSetBuild;
FcFontListFuncType FontList;
FcPatternGetStringFuncType PatternGetString;
FcStrDirnameFuncType StrDirname;
FcPatternDestroyFuncType PatternDestroy;
FcFontSetDestroyFuncType FontSetDestroy;
FcNameParseFuncType NameParse;
FcPatternAddStringFuncType PatternAddString;
FcConfigSubstituteFuncType ConfigSubstitute;
FcDefaultSubstituteFuncType  DefaultSubstitute;
FcFontMatchFuncType FontMatch;
FcPatternGetBoolFuncType PatternGetBool;
FcPatternGetIntegerFuncType PatternGetInteger;
FcGetVersionFuncType GetVersion;
FcPatternGetCharSetFuncType PatternGetCharSet;
FcFontSortFuncType FontSort;
FcFontSetDestroyFuncType FontSetDestroy;
FcCharSetUnionFuncType CharSetUnion;
FcCharSetSubtractCountFuncType CharSetSubtractCount;
FcConfigGetCacheDirsFuncType ConfigGetCacheDirs;
FcStrListNextFuncType StrListNext;
FcStrListDoneFuncType StrListDone;
#if 0
FcFiniFuncType Fini;
#endif

#define FcPatternBuild (*PatternBuild)
#define FcObjectSetBuild (*ObjectSetBuild)
#define FcFontList (*FontList)
#define FcPatternGetString (*PatternGetString)
#define FcStrDirname (*StrDirname)
#define FcFontSetDestroy (*FontSetDestroy)
#define FcPatternDestroy (*PatternDestroy)
#define FcNameParse (*NameParse)
#define FcPatternAddString (*PatternAddString)
#define FcConfigSubstitute (*ConfigSubstitute)
#define FcDefaultSubstitute (*DefaultSubstitute)
#define FcFontMatch (*FontMatch)
#define FcPatternGetBool (*PatternGetBool)
#define FcPatternGetInteger (*PatternGetInteger)
#define FcGetVersion (*GetVersion)
#define FcStrListNext (*StrListNext)
#define FcStrListDone (*StrListDone)
#define FcConfigGetCacheDirs (*ConfigGetCacheDirs)
#define FcFontSort (*FontSort)
#define FcPatternGetCharSet (*PatternGetCharSet)
#define FcCharSetSubtractCount (*CharSetSubtractCount)
#define FcCharSetUnion (*CharSetUnion)

#endif
