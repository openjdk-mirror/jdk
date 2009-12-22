/*
 * Copyright 1996-2006 Sun Microsystems, Inc.  All Rights Reserved.
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

/*
 * Mappings from partial locale names to full locale names
 */
 static char *locale_aliases[] = {
    "ar", "ar_EG",
    "be", "be_BY",
    "bg", "bg_BG",
    "br", "br_FR",
    "ca", "ca_ES",
    "cs", "cs_CZ",
    "cz", "cs_CZ",
    "da", "da_DK",
    "de", "de_DE",
    "el", "el_GR",
    "en", "en_US",
    "eo", "eo",    /* no country for Esperanto */
    "es", "es_ES",
    "et", "et_EE",
    "eu", "eu_ES",
    "fi", "fi_FI",
    "fr", "fr_FR",
    "ga", "ga_IE",
    "gl", "gl_ES",
    "he", "iw_IL",
    "hr", "hr_HR",
    "hu", "hu_HU",
    "id", "in_ID",
    "in", "in_ID",
    "is", "is_IS",
    "it", "it_IT",
    "iw", "iw_IL",
    "ja", "ja_JP",
    "kl", "kl_GL",
    "ko", "ko_KR",
    "lt", "lt_LT",
    "lv", "lv_LV",
    "mk", "mk_MK",
    "nl", "nl_NL",
    "no", "no_NO",
    "pl", "pl_PL",
    "pt", "pt_PT",
    "ro", "ro_RO",
    "ru", "ru_RU",
    "sh", "sh_YU",
    "sk", "sk_SK",
    "sl", "sl_SI",
    "sq", "sq_AL",
    "sr", "sr_YU",
    "su", "fi_FI",
    "sv", "sv_SE",
    "th", "th_TH",
    "tr", "tr_TR",
    "uk", "uk_UA",
    "zh", "zh_CN",
    "big5", "zh_TW.Big5",
    "chinese", "zh_CN",
    "japanese", "ja_JP",
    "tchinese", "zh_TW",
    ""
 };

/*
 * Linux/Solaris language string to ISO639 string mapping table.
 */
static char *language_names[] = {
    "C", "en",
    "POSIX", "en",
    "ar", "ar",
    "be", "be",
    "bg", "bg",
    "br", "br",
    "ca", "ca",
    "cs", "cs",
    "cz", "cs",
    "da", "da",
    "de", "de",
    "el", "el",
    "en", "en",
    "eo", "eo",
    "es", "es",
    "et", "et",
    "eu", "eu",
    "fi", "fi",
    "fo", "fo",
    "fr", "fr",
    "ga", "ga",
    "gl", "gl",
    "hi", "hi",
    "he", "iw",
    "hr", "hr",
    "hu", "hu",
    "id", "in",
    "in", "in",
    "is", "is",
    "it", "it",
    "iw", "iw",
    "ja", "ja",
    "kl", "kl",
    "ko", "ko",
    "lt", "lt",
    "lv", "lv",
    "mk", "mk",
    "nl", "nl",
    "no", "no",
    "nr", "nr",
    "pl", "pl",
    "pt", "pt",
    "ro", "ro",
    "ru", "ru",
    "sh", "sh",
    "sk", "sk",
    "sl", "sl",
    "sq", "sq",
    "sr", "sr",
    "su", "fi",
    "sv", "sv",
    "th", "th",
    "tr", "tr",
    "uk", "uk",
    "zh", "zh",
    "chinese", "zh",
    "japanese", "ja",
    "korean", "ko",
    "",
};

/*
 * Linux/Solaris country string to ISO3166 string mapping table.
 */
static char *country_names[] = {
    "AT", "AT",
    "AU", "AU",
    "AR", "AR",
    "BE", "BE",
    "BR", "BR",
    "BO", "BO",
    "CA", "CA",
    "CH", "CH",
    "CL", "CL",
    "CN", "CN",
    "CO", "CO",
    "CR", "CR",
    "CZ", "CZ",
    "DE", "DE",
    "DK", "DK",
    "DO", "DO",
    "EC", "EC",
    "EE", "EE",
    "ES", "ES",
    "FI", "FI",
    "FO", "FO",
    "FR", "FR",
    "GB", "GB",
    "GR", "GR",
    "GT", "GT",
    "HN", "HN",
    "HR", "HR",
    "HU", "HU",
    "ID", "ID",
    "IE", "IE",
    "IL", "IL",
    "IN", "IN",
    "IS", "IS",
    "IT", "IT",
    "JP", "JP",
    "KR", "KR",
    "LT", "LT",
    "LU", "LU",
    "LV", "LV",
    "MX", "MX",
    "NI", "NI",
    "NL", "NL",
    "NO", "NO",
    "NZ", "NZ",
    "PA", "PA",
    "PE", "PE",
    "PL", "PL",
    "PT", "PT",
    "PY", "PY",
    "RO", "RO",
    "RU", "RU",
    "SE", "SE",
    "SI", "SI",
    "SK", "SK",
    "SV", "SV",
    "TH", "TH",
    "TR", "TR",
    "UA", "UA",
    "UK", "GB",
    "US", "US",
    "UY", "UY",
    "VE", "VE",
    "TW", "TW",
    "YU", "YU",
    "",
};

/*
 * Linux/Solaris variant string to Java variant name mapping table.
 */
static char *variant_names[] = {
    "",
};
