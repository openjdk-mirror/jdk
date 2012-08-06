/*
 * Copyright (c) 2004, 2010, Oracle and/or its affiliates. All rights reserved.
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

#include <gconf_fp.h>
#include <dlfcn.h>
#include "jvm_md.h"

fp_client_get_default_func* my_get_default_func = NULL;
fp_client_get_string_func* my_get_string_func = NULL;
fp_client_get_int_func* my_get_int_func = NULL;
fp_client_get_bool_func* my_get_bool_func = NULL;
fp_conf_init_func* my_gconf_init_func = NULL;
type_init_func type_init = NULL;
free_func gfree = NULL;

jboolean init_gconf(int* gconf_ver, void** gconf_client)
{
  /**
   * Let's try to load le GConf-2 library
   */
  if (dlopen(JNI_LIB_NAME("gconf-2"), RTLD_GLOBAL | RTLD_LAZY) != NULL ||
      dlopen(VERSIONED_JNI_LIB_NAME("gconf-2", "4"),
             RTLD_GLOBAL | RTLD_LAZY) != NULL) {
    *gconf_ver = 2;
  }
  if (*gconf_ver > 0) {
    /*
     * Now let's get pointer to the functions we need.
     */
    type_init = (type_init_func) dlsym(RTLD_DEFAULT, "g_type_init");
    gfree = (free_func) dlsym(RTLD_DEFAULT, "g_free");
    my_get_default_func = (fp_client_get_default_func*) dlsym(RTLD_DEFAULT, "gconf_client_get_default");
    if (type_init != NULL && gfree != NULL && my_get_default_func != NULL) {
      /**
       * Try to connect to GConf.
       */
      (*type_init)();
      (*gconf_client) = (*my_get_default_func)();
      if ((*gconf_client) != NULL) {
        my_get_string_func = (fp_client_get_string_func*) dlsym(RTLD_DEFAULT, "gconf_client_get_string");
        my_get_int_func = (fp_client_get_int_func*) dlsym(RTLD_DEFAULT, "gconf_client_get_int");
        my_get_bool_func = (fp_client_get_bool_func*) dlsym(RTLD_DEFAULT, "gconf_client_get_bool");
        if (my_get_int_func != NULL && my_get_string_func != NULL &&
            my_get_bool_func != NULL) {
          /**
           * We did get all we need. Let's enable the System Proxy Settings.
           */
          return JNI_TRUE;
        }
      }
    }
  }
  return JNI_FALSE;
}
