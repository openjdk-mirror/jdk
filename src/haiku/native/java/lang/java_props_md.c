/*
 * Copyright 1998-2006 Sun Microsystems, Inc.  All Rights Reserved.
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

#include "FindDirectory.h"
#include "java_props.h"
#include "locale_str.h"

#include <sys/param.h>		// For MAXPATHLEN
#include <sys/utsname.h>	// For os_name and os_version
#include <pwd.h>
#include <time.h>
#include <errno.h>
#include <locale.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

#ifdef HAVE_ALTZONE
#else
static long altzone = 0;
#endif

/* Take an array of string pairs (map of key->value) and a string (key).
 * Examine each pair in the map to see if the first string (key) matches the
 * string.  If so, store the second string of the pair (value) in the value and
 * return 1.  Otherwise do nothing and return 0.  The end of the map is
 * indicated by an empty string at the start of a pair (key of "").
 */
static int
mapLookup(char* map[], const char* key, char** value) {
    int i;
    for (i = 0; strcmp(map[i], ""); i += 2){
        if (!strcmp(key, map[i])){
            *value = map[i + 1];
            return 1;
        }
    }
    return 0;
}

/* This function set up enviroment variable using envstring.
 * The format of envstring is "name=value".
 * If the name has already existed, it will append value to the name.
 */
static void
setPathEnvironment(char *envstring)
{
    char name[20], *value, *current;
    
    value = strchr(envstring, '='); /* locate name and value seprator */
    
    if (! value)
	return; /* not a valid environment setting */
    
    /* copy first part as environment name */
    strncpy(name, envstring, value - envstring);
    name[value-envstring] = '\0';
    
    value++; /* set value point to value of the envstring */
    
    current = getenv(name);
    if (current) {
	if (! strstr(current, value)) {
	    /* value is not found in current environment, append it */
	    char *temp = malloc(strlen(envstring) + strlen(current) + 2);
        strcpy(temp, name);
        strcat(temp, "=");
        strcat(temp, current);
        strcat(temp, ":");
        strcat(temp, value);
        putenv(temp);
	}
	/* else the value has already been set, do nothing */
    }
    else {
	/* environment variable is not found */
	putenv(envstring);
    }
}

void setupLocale (java_props_t * props) {	
	char * lc;
	char temp[64];
	char *language = NULL, *country = NULL,
		*variant = NULL, *encoding = NULL;
	char *std_language = NULL, *std_country = NULL,
		*std_variant = NULL, *std_encoding = NULL;
	char *p, encoding_variant[64];
	int i, found;

	lc = setlocale(LC_CTYPE, "");
    if (lc == NULL || !strcmp(lc, "C") || !strcmp(lc, "POSIX")) {
		lc = "en_US.utf8";
	}

	/*
	* locale string format is
	* <language name>_<country name>.<encoding name>@<variant name>
	* <country name>, <encoding name>, and <variant name> are optional.
	*/
	
#ifndef __linux__
        /*
         * Workaround for Solaris bug 4201684: Xlib doesn't like @euro
         * locales. Since we don't depend on the libc @euro behavior,
         * we just remove the qualifier.
         * On Linux, the bug doesn't occur; on the other hand, @euro
         * is needed there because it's a shortcut that also determines
         * the encoding - without it, we wouldn't get ISO-8859-15.
         * Therefore, this code section is Solaris-specific.
         */
	lc = strdup(lc);	/* keep a copy, setlocale trashes original. */
	strcpy(temp, lc);
	p = strstr(temp, "@euro");
	if (p != NULL) 
	*p = '\0';
	setlocale(LC_ALL, temp);
#endif
	
	strcpy(temp, lc);
	
	/* Parse the language, country, encoding, and variant from the
	* locale.  Any of the elements may be missing, but they must occur
	* in the order language_country.encoding@variant, and must be
	* preceded by their delimiter (except for language).
	*
	* If the locale name (without .encoding@variant, if any) matches
	* any of the names in the locale_aliases list, map it to the
	* corresponding full locale name.  Most of the entries in the
	* locale_aliases list are locales that include a language name but
	* no country name, and this facility is used to map each language
	* to a default country if that's possible.  It's also used to map
	* the Solaris locale aliases to their proper Java locale IDs.
	*/
	if ((p = strchr(temp, '.')) != NULL) {
		strcpy(encoding_variant, p); /* Copy the leading '.' */
		*p = '\0';
	} else if ((p = strchr(temp, '@')) != NULL) {
		strcpy(encoding_variant, p); /* Copy the leading '@' */
		*p = '\0';
	} else {
		*encoding_variant = '\0';
	}
	
	if (mapLookup(locale_aliases, temp, &p)) {
		strcpy(temp, p);
	}
	
	language = temp;
	if ((country = strchr(temp, '_')) != NULL) {
		*country++ = '\0';
	}
	
	p = encoding_variant;
	if ((encoding = strchr(p, '.')) != NULL) {
		p[encoding++ - p] = '\0';
		p = encoding;
	}
	if ((variant = strchr(p, '@')) != NULL) {
		p[variant++ - p] = '\0';
	}
	
	/* Normalize the language name */
	std_language = "en";
	if (language != NULL) {
		mapLookup(language_names, language, &std_language);
	}
	props->language = std_language;
	
	/* Normalize the country name */
	if (country != NULL) {
		std_country = country;
		mapLookup(country_names, country, &std_country);
		props->country = strdup(std_country);
	}
	
	/* Normalize the variant name.  Note that we only use
	* variants listed in the mapping array; others are ignored. */
	if (variant != NULL) {
		mapLookup(variant_names, variant, &std_variant);
		props->variant = std_variant;
	}
	
	/* return same result nl_langinfo would return for en_UK,
	* in order to use optimizations. */
	std_encoding = (*p != '\0') ? p : "UTF-8";
	
	props->encoding = strdup(std_encoding);
}

/* This function gets called very early, before VM_CALLS are setup.
 * Do not use any of the VM_CALLS entries!!!
 */
java_props_t *
GetJavaProperties(JNIEnv *env)
{
    static java_props_t props
      = { 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0,
          0, 0, 0, 0, 0,
          0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, }; // twenty-five fields in java_props_t
    static bool ready = false;

    if (ready) {
        return &props;
    }
    
    /* os name, release, architecture */
    {
	    struct utsname name;
	    uname(&name);
	    props.os_name = strdup(name.sysname);
	    props.os_version = strdup(name.release);
#ifndef ARCH
		if (strcmp(name.machine,"BePC") == 0) {
	        /* use x86 to match the value received on win32 */
		    props.os_arch = "x86";
		} else {
			props.os_arch = "ppc";
		}
#else
	    if (strcmp(ARCH,"i586") == 0) {
	        /* use x86 to match the value received on win32 */
		    props.os_arch = "x86";
		} else {
			props.os_arch = ARCH;
		}
#endif
	}

	{
		char directory[MAXPATHLEN];
		
		/* tmp dir */
	    find_directory(B_COMMON_TEMP_DIRECTORY,0,true,directory,sizeof(directory));
		props.tmp_dir = strdup(directory);
		
		/* font dir */
		props.font_dir = getenv("JAVA_FONTS");
    	if (props.font_dir == 0) {
			char system_fonts[MAXPATHLEN];
			char common_fonts[MAXPATHLEN];
			char user_fonts[MAXPATHLEN];
			char font_path[MAXPATHLEN*3+2];
		    find_directory(B_SYSTEM_FONTS_DIRECTORY,0,false,system_fonts,sizeof(system_fonts));
		    find_directory(B_COMMON_FONTS_DIRECTORY,0,false,common_fonts,sizeof(common_fonts));
		    find_directory(B_USER_FONTS_DIRECTORY,0,false,user_fonts,sizeof(user_fonts));
			font_path[0] = '\0';
			strcat(font_path, system_fonts);
			strcat(font_path, "/ttfonts");
			if (strcmp(common_fonts, system_fonts) != 0) {
				strcat(font_path, ":");
				strcat(font_path, common_fonts);
				strcat(font_path, "/ttfonts");
			}
			if ((strcmp(user_fonts, system_fonts) != 0) &&
			    (strcmp(user_fonts, common_fonts) != 0)) {
				strcat(font_path, ":");
				strcat(font_path, user_fonts);
				strcat(font_path, "/ttfonts");
		    }
			props.font_dir = strdup(font_path);
		}
		
		/* user dir = current directory */
		getcwd(directory,sizeof(directory));
		props.user_dir = strdup(directory);
	}
	
	/* separators */
    props.file_separator = "/";
    props.path_separator = ":";
    props.line_separator = "\n";

	/* user info */
	{
		struct passwd * pwent = getpwuid(getuid());
		props.user_name = pwent ? strdup(pwent->pw_name) : "?";
		props.user_home = pwent ? strdup(pwent->pw_dir) : "?";
	}
	
	/* localization information */
	setupLocale(&props);
	
	/* User TIMEZONE */
	/*
	 * We defer setting up timezone until it's actually necessary.
	 * Refer to TimeZone.getDefault(). However, the system
	 * property is necessary to be able to be set by the command
	 * line interface -D. Here temporarily set a null string to
	 * timezone.
	 */
	tzset();	/* for compatibility */
	props.timezone = "";
	
    /* Printing properties */
    props.printerJob = "sun.awt.haiku.BPrinterJob";
	
	/* Graphics Environment */
	props.graphics_env = "sun.awt.HaikuGraphicsEnvironment";

	/* AWT Toolkit */
	props.awt_toolkit = "sun.awt.haiku.BToolkit";

	/* The default endianness of unicode */
#if __BYTE_ORDER == __LITTLE_ENDIAN
    props.unicode_encoding = "UnicodeLittle";
#else
    props.unicode_encoding = "UnicodeBig";
#endif

	/* list of supported instruction sets */
	if (strcmp(props.os_arch,"x86") == 0) {
		props.cpu_isalist = "pentium";
	} else {
		props.cpu_isalist = NULL;
	}
	
	/* endianness of platform */
	{
		unsigned int endianTest = 0xff000000;
		if (((char*)(&endianTest))[0] != 0)
		    props.cpu_endian = "big";
		else
		    props.cpu_endian = "little";
	}

    /* Preferences properties */
    props.util_prefs_PreferencesFactory = "java.util.prefs.HaikuPreferencesFactory";

	/* 32 or 64 bit data model */
	props.data_model = "32";

    /* patches/service packs installed */
    props.patch_level = "unknown";
    
    ready = true;
    return &props;
}
