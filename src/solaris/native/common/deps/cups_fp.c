/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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

#include <jvm_md.h>
#include <dlfcn.h>
#include <jni.h>

#include <cups/cups.h>
#include <cups/ppd.h>

#include <cups_fp.h>

jboolean cups_init()
{
  void *handle = dlopen(VERSIONED_JNI_LIB_NAME("cups", "2"),
                        RTLD_LAZY | RTLD_GLOBAL);

  if (handle == NULL) {
    handle = dlopen(JNI_LIB_NAME("cups"), RTLD_LAZY | RTLD_GLOBAL);
    if (handle == NULL) {
      return JNI_FALSE;
    }
  }

  j2d_cupsServer = (fn_cupsServer)dlsym(handle, "cupsServer");
  if (j2d_cupsServer == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_ippPort = (fn_ippPort)dlsym(handle, "ippPort");
  if (j2d_ippPort == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_httpConnect = (fn_httpConnect)dlsym(handle, "httpConnect");
  if (j2d_httpConnect == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_httpClose = (fn_httpClose)dlsym(handle, "httpClose");
  if (j2d_httpClose == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_cupsGetPPD = (fn_cupsGetPPD)dlsym(handle, "cupsGetPPD");
  if (j2d_cupsGetPPD == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_ppdOpenFile = (fn_ppdOpenFile)dlsym(handle, "ppdOpenFile");
  if (j2d_ppdOpenFile == NULL) {
    dlclose(handle);
    return JNI_FALSE;

  }

  j2d_ppdClose = (fn_ppdClose)dlsym(handle, "ppdClose");
  if (j2d_ppdClose == NULL) {
    dlclose(handle);
    return JNI_FALSE;

  }

  j2d_ppdFindOption = (fn_ppdFindOption)dlsym(handle, "ppdFindOption");
  if (j2d_ppdFindOption == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  j2d_ppdPageSize = (fn_ppdPageSize)dlsym(handle, "ppdPageSize");
  if (j2d_ppdPageSize == NULL) {
    dlclose(handle);
    return JNI_FALSE;
  }

  return JNI_TRUE;
}
