/*
 * $RCSId: xc/lib/fontconfig/fontconfig/fontconfig.h,v 1.30 2002/09/26 00:17:27 keithp Exp $
 *
 * Copyright © 2001 Keith Packard
 *
 * Permission to use, copy, modify, distribute, and sell this software and its
 * documentation for any purpose is hereby granted without fee, provided that
 * the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of Keith Packard not be used in
 * advertising or publicity pertaining to distribution of the software without
 * specific, written prior permission.  Keith Packard makes no
 * representations about the suitability of this software for any purpose.  It
 * is provided "as is" without express or implied warranty.
 *
 * KEITH PACKARD DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO
 * EVENT SHALL KEITH PACKARD BE LIABLE FOR ANY SPECIAL, INDIRECT OR
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE,
 * DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 */

#ifndef _FONTCONFIG_H_
#define _FONTCONFIG_H_

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdarg.h>

#if defined(__GNUC__) && (__GNUC__ >= 4)
#define FC_ATTRIBUTE_SENTINEL(x) __attribute__((__sentinel__(0)))
#else
#define FC_ATTRIBUTE_SENTINEL(x)
#endif

#ifndef FcPublic
#define FcPublic
#endif

typedef unsigned char   FcChar8;
typedef unsigned short  FcChar16;
typedef unsigned int    FcChar32;
typedef int             FcBool;

/*
 * Current Fontconfig version number.  This same number
 * must appear in the fontconfig configure.in file. Yes,
 * it'a a pain to synchronize version numbers like this.
 */

#define FC_MAJOR        2
#define FC_MINOR        5
#define FC_REVISION     0

#define FC_VERSION      ((FC_MAJOR * 10000) + (FC_MINOR * 100) + (FC_REVISION))

/*
 * Current font cache file format version
 * This is appended to the cache files so that multiple
 * versions of the library will peacefully coexist
 *
 * Change this value whenever the disk format for the cache file
 * changes in any non-compatible way.  Try to avoid such changes as
 * it means multiple copies of the font information.
 */

#define FC_CACHE_VERSION    "2"

#define FcTrue          1
#define FcFalse         0

#define FC_FAMILY           "family"            /* String */
#define FC_STYLE            "style"             /* String */
#define FC_SLANT            "slant"             /* Int */
#define FC_WEIGHT           "weight"            /* Int */
#define FC_SIZE             "size"              /* Double */
#define FC_ASPECT           "aspect"            /* Double */
#define FC_PIXEL_SIZE       "pixelsize"         /* Double */
#define FC_SPACING          "spacing"           /* Int */
#define FC_FOUNDRY          "foundry"           /* String */
#define FC_ANTIALIAS        "antialias"         /* Bool (depends) */
#define FC_HINTING          "hinting"           /* Bool (true) */
#define FC_HINT_STYLE       "hintstyle"         /* Int */
#define FC_VERTICAL_LAYOUT  "verticallayout"    /* Bool (false) */
#define FC_AUTOHINT         "autohint"          /* Bool (false) */
#define FC_GLOBAL_ADVANCE   "globaladvance"     /* Bool (true) */
#define FC_WIDTH            "width"             /* Int */
#define FC_FILE             "file"              /* String */
#define FC_INDEX            "index"             /* Int */
#define FC_FT_FACE          "ftface"            /* FT_Face */
#define FC_RASTERIZER       "rasterizer"        /* String */
#define FC_OUTLINE          "outline"           /* Bool */
#define FC_SCALABLE         "scalable"          /* Bool */
#define FC_SCALE            "scale"             /* double */
#define FC_DPI              "dpi"               /* double */
#define FC_RGBA             "rgba"              /* Int */
#define FC_MINSPACE         "minspace"          /* Bool use minimum line spacing */
#define FC_SOURCE           "source"            /* String (deprecated) */
#define FC_CHARSET          "charset"           /* CharSet */
#define FC_LANG             "lang"              /* String RFC 3066 langs */
#define FC_FONTVERSION      "fontversion"       /* Int from 'head' table */
#define FC_FULLNAME         "fullname"          /* String */
#define FC_FAMILYLANG       "familylang"        /* String RFC 3066 langs */
#define FC_STYLELANG        "stylelang"         /* String RFC 3066 langs */
#define FC_FULLNAMELANG     "fullnamelang"      /* String RFC 3066 langs */
#define FC_CAPABILITY       "capability"        /* String */
#define FC_FONTFORMAT       "fontformat"        /* String */
#define FC_EMBOLDEN         "embolden"          /* Bool - true if emboldening needed*/
#define FC_EMBEDDED_BITMAP  "embeddedbitmap"    /* Bool - true to enable embedded bitmaps */
#define FC_DECORATIVE       "decorative"        /* Bool - true if style is a decorative variant */

#define FC_CACHE_SUFFIX             ".cache-"FC_CACHE_VERSION
#define FC_DIR_CACHE_FILE           "fonts.cache-"FC_CACHE_VERSION
#define FC_USER_CACHE_FILE          ".fonts.cache-"FC_CACHE_VERSION

/* Adjust outline rasterizer */
#define FC_CHAR_WIDTH       "charwidth" /* Int */
#define FC_CHAR_HEIGHT      "charheight"/* Int */
#define FC_MATRIX           "matrix"    /* FcMatrix */

#define FC_WEIGHT_THIN              0
#define FC_WEIGHT_EXTRALIGHT        40
#define FC_WEIGHT_ULTRALIGHT        FC_WEIGHT_EXTRALIGHT
#define FC_WEIGHT_LIGHT             50
#define FC_WEIGHT_BOOK              75
#define FC_WEIGHT_REGULAR           80
#define FC_WEIGHT_NORMAL            FC_WEIGHT_REGULAR
#define FC_WEIGHT_MEDIUM            100
#define FC_WEIGHT_DEMIBOLD          180
#define FC_WEIGHT_SEMIBOLD          FC_WEIGHT_DEMIBOLD
#define FC_WEIGHT_BOLD              200
#define FC_WEIGHT_EXTRABOLD         205
#define FC_WEIGHT_ULTRABOLD         FC_WEIGHT_EXTRABOLD
#define FC_WEIGHT_BLACK             210
#define FC_WEIGHT_HEAVY             FC_WEIGHT_BLACK
#define FC_WEIGHT_EXTRABLACK        215
#define FC_WEIGHT_ULTRABLACK        FC_WEIGHT_EXTRABLACK

#define FC_SLANT_ROMAN              0
#define FC_SLANT_ITALIC             100
#define FC_SLANT_OBLIQUE            110

#define FC_WIDTH_ULTRACONDENSED     50
#define FC_WIDTH_EXTRACONDENSED     63
#define FC_WIDTH_CONDENSED          75
#define FC_WIDTH_SEMICONDENSED      87
#define FC_WIDTH_NORMAL             100
#define FC_WIDTH_SEMIEXPANDED       113
#define FC_WIDTH_EXPANDED           125
#define FC_WIDTH_EXTRAEXPANDED      150
#define FC_WIDTH_ULTRAEXPANDED      200

#define FC_PROPORTIONAL             0
#define FC_DUAL                     90
#define FC_MONO                     100
#define FC_CHARCELL                 110

/* sub-pixel order */
#define FC_RGBA_UNKNOWN     0
#define FC_RGBA_RGB         1
#define FC_RGBA_BGR         2
#define FC_RGBA_VRGB        3
#define FC_RGBA_VBGR        4
#define FC_RGBA_NONE        5

/* hinting style */
#define FC_HINT_NONE        0
#define FC_HINT_SLIGHT      1
#define FC_HINT_MEDIUM      2
#define FC_HINT_FULL        3

typedef enum _FcType {
    FcTypeVoid,
    FcTypeInteger,
    FcTypeDouble,
    FcTypeString,
    FcTypeBool,
    FcTypeMatrix,
    FcTypeCharSet,
    FcTypeFTFace,
    FcTypeLangSet
} FcType;

typedef struct _FcMatrix {
    double xx, xy, yx, yy;
} FcMatrix;

#define FcMatrixInit(m) ((m)->xx = (m)->yy = 1, \
                         (m)->xy = (m)->yx = 0)

/*
 * A data structure to represent the available glyphs in a font.
 * This is represented as a sparse boolean btree.
 */

typedef struct _FcCharSet FcCharSet;

typedef struct _FcObjectType {
    const char  *object;
    FcType      type;
} FcObjectType;

typedef struct _FcConstant {
    const FcChar8  *name;
    const char  *object;
    int         value;
} FcConstant;

typedef enum _FcResult {
    FcResultMatch, FcResultNoMatch, FcResultTypeMismatch, FcResultNoId,
    FcResultOutOfMemory
} FcResult;

typedef struct _FcPattern   FcPattern;

typedef struct _FcLangSet   FcLangSet;

typedef struct _FcValue {
    FcType      type;
    union {
        const FcChar8   *s;
        int             i;
        FcBool          b;
        double          d;
        const FcMatrix  *m;
        const FcCharSet *c;
        void            *f;
        const FcLangSet *l;
    } u;
} FcValue;

typedef struct _FcFontSet {
    int         nfont;
    int         sfont;
    FcPattern   **fonts;
} FcFontSet;

typedef struct _FcObjectSet {
    int         nobject;
    int         sobject;
    const char  **objects;
} FcObjectSet;

typedef enum _FcMatchKind {
    FcMatchPattern, FcMatchFont, FcMatchScan
} FcMatchKind;

typedef enum _FcLangResult {
    FcLangEqual = 0,
    FcLangDifferentCountry = 1,
    FcLangDifferentTerritory = 1,
    FcLangDifferentLang = 2
} FcLangResult;

typedef enum _FcSetName {
    FcSetSystem = 0,
    FcSetApplication = 1
} FcSetName;

typedef struct _FcAtomic FcAtomic;

#if defined(__cplusplus) || defined(c_plusplus) /* for C++ V2.0 */
#define _FCFUNCPROTOBEGIN extern "C" {  /* do not leave open across includes */
#define _FCFUNCPROTOEND }
#else
#define _FCFUNCPROTOBEGIN
#define _FCFUNCPROTOEND
#endif

typedef enum { FcEndianBig, FcEndianLittle } FcEndian;

typedef struct _FcConfig    FcConfig;

typedef struct _FcGlobalCache   FcFileCache;

typedef struct _FcBlanks    FcBlanks;

typedef struct _FcStrList   FcStrList;

typedef struct _FcStrSet    FcStrSet;

typedef struct _FcCache     FcCache;

#undef FC_ATTRIBUTE_SENTINEL


#ifndef _FCINT_H_

/*
 * Deprecated functions are placed here to help users fix their code without
 * digging through documentation
 */

#define FcConfigGetRescanInverval   FcConfigGetRescanInverval_REPLACE_BY_FcConfigGetRescanInterval
#define FcConfigSetRescanInverval   FcConfigSetRescanInverval_REPLACE_BY_FcConfigSetRescanInterval

#endif

#endif /* _FONTCONFIG_H_ */
