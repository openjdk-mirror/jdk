/*
 * Copyright 1999-2004 Sun Microsystems, Inc.  All Rights Reserved.
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

#include <stdio.h>
#include <string.h>
#include "TimeZone_md.h"
#include "time.h"

/*
#define SKIP_SPACE(p)	while (*p == ' ' || *p == '\t') p++;
*/

/*
 * findJavaTZ_md() maps platform time zone ID to Java time zone ID
 * using <java_home>/lib/tzmappings. If the TZ value is not found, it
 * trys some libc implementation dependent mappings. If it still
 * can't map to a Java time zone ID, it falls back to the GMT+/-hh:mm
 * form. `country', which can be null, is not used for UNIX platforms.
 */
char *
findJavaTZ_md(const char *java_home_dir, const char *country)
{
	return getGMTOffsetID();
}

/**
 * Returns a GMT-offset-based time zone ID. (e.g., "GMT-08:00")
 */
char *
getGMTOffsetID()
{
    time_t offset;
    char sign, buf[16];

	tzset();
	
    if (timezone == 0) {
		return strdup("GMT");
    }

    /* Note that the time offset direction is opposite. */
    if (timezone > 0) {
		offset = timezone;
		sign = '-';
    } else {
		offset = -timezone;
		sign = '+';
    }
    sprintf(buf, (const char *)"GMT%c%02d:%02d",
	    sign, (int)(offset/3600), (int)((offset%3600)/60));
    return strdup(buf);
}
