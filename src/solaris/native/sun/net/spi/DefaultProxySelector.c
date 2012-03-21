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

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"
#include "sun_net_spi_DefaultProxySelector.h"
#include <stdio.h>
#if defined(__linux__) || defined(_ALLBSD_SOURCE)
#include <string.h>
#else
#include <strings.h>
#endif

#include <gconf/gconf-client.h>

#ifdef USE_SYSTEM_GIO
#include <gio/gio.h>
#else
#include <gio_fp.h>
#endif

#ifndef USE_SYSTEM_GCONF
#include <gconf_fp.h>
#endif

static jclass proxy_class;
static jclass isaddr_class;
static jclass ptype_class;
static jmethodID isaddr_createUnresolvedID;
static jmethodID proxy_ctrID;
static jfieldID pr_no_proxyID;
static jfieldID ptype_httpID;
static jfieldID ptype_socksID;

static int gconf_ver = 0;
static void* gconf_client = NULL;
static jboolean use_gio;

#define CHECK_NULL(X) { if ((X) == NULL) fprintf (stderr,"JNI errror at line %d\n", __LINE__); }

/**
 * The GConf-2 settings used are:
 * - /system/http_proxy/use_http_proxy          boolean
 * - /system/http_proxy/use_authentcation       boolean
 * - /system/http_proxy/use_same_proxy          boolean
 * - /system/http_proxy/host                    string
 * - /system/http_proxy/authentication_user     string
 * - /system/http_proxy/authentication_password string
 * - /system/http_proxy/port                    int
 * - /system/proxy/socks_host                   string
 * - /system/proxy/mode                         string
 * - /system/proxy/ftp_host                     string
 * - /system/proxy/secure_host                  string
 * - /system/proxy/socks_port                   int
 * - /system/proxy/ftp_port                     int
 * - /system/proxy/secure_port                  int
 * - /system/proxy/no_proxy_for                 list
 * - /system/proxy/gopher_host                  string
 * - /system/proxy/gopher_port                  int
*/

/*
 * Class:     sun_net_spi_DefaultProxySelector
 * Method:    init
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_net_spi_DefaultProxySelector_init(JNIEnv *env, jclass clazz) {
  jclass cls = NULL;
  CHECK_NULL(cls = (*env)->FindClass(env,"java/net/Proxy"));
  proxy_class = (*env)->NewGlobalRef(env, cls);
  CHECK_NULL(cls = (*env)->FindClass(env,"java/net/Proxy$Type"));
  ptype_class = (*env)->NewGlobalRef(env, cls);
  CHECK_NULL(cls = (*env)->FindClass(env, "java/net/InetSocketAddress"));
  isaddr_class = (*env)->NewGlobalRef(env, cls);
  proxy_ctrID = (*env)->GetMethodID(env, proxy_class, "<init>", "(Ljava/net/Proxy$Type;Ljava/net/SocketAddress;)V");
  pr_no_proxyID = (*env)->GetStaticFieldID(env, proxy_class, "NO_PROXY", "Ljava/net/Proxy;");
  ptype_httpID = (*env)->GetStaticFieldID(env, ptype_class, "HTTP", "Ljava/net/Proxy$Type;");
  ptype_socksID = (*env)->GetStaticFieldID(env, ptype_class, "SOCKS", "Ljava/net/Proxy$Type;");
  isaddr_createUnresolvedID = (*env)->GetStaticMethodID(env, isaddr_class, "createUnresolved", "(Ljava/lang/String;I)Ljava/net/InetSocketAddress;");

#ifdef USE_SYSTEM_GIO
  use_gio = JNI_TRUE;
  g_type_init ();
#else
  use_gio = gio_init();
#endif

  if (use_gio == JNI_TRUE) {
    return use_gio;
  } else {
#ifdef USE_SYSTEM_GCONF
    gconf_ver = 2;
    return JNI_TRUE;
#else
    return init_gconf (&gconf_ver, &gconf_client);
#endif
  }

}

/*
 * Class:     sun_net_spi_DefaultProxySelector
 * Method:    getSystemProxy
 * Signature: ([Ljava/lang/String;Ljava/lang/String;)Ljava/net/Proxy;
 */
JNIEXPORT jobject JNICALL
Java_sun_net_spi_DefaultProxySelector_getSystemProxy(JNIEnv *env,
                                                     jobject this,
                                                     jstring proto,
                                                     jstring host)
{
  char *phost = NULL;
  char *mode = NULL;
  int pport = 0;
  gboolean use_proxy = 0;
  gboolean use_same_proxy = 0;
  const char* urlhost;
  jobject isa = NULL;
  jobject proxy = NULL;
  jobject type_proxy = NULL;
  jobject no_proxy = NULL;
  jboolean isCopy;

  if (use_gio == JNI_TRUE || gconf_ver > 0) {
    jstring jhost;
    const char *cproto;
    char *used_proto;

    cproto = (*env)->GetStringUTFChars(env, proto, &isCopy);

    if (use_gio == JNI_TRUE && cproto != NULL) {
      GSettings *settings, *child_settings;
      gchar **ignored;

      settings = g_settings_new ("org.gnome.system.proxy");

      use_same_proxy = g_settings_get_boolean (settings, "use-same-proxy");
      if (use_same_proxy) {
	used_proto = "http";
      } else {
	used_proto = (char*) cproto;
      }

      mode = g_settings_get_string (settings, "mode");
      use_proxy = (mode != NULL && strcasecmp (mode, "manual") == 0);

      child_settings = g_settings_get_child (settings, used_proto);
      if (use_proxy && strcasecmp (used_proto, "http") == 0) {
	use_proxy = g_settings_get_boolean (child_settings, "enabled");
      }

      if (use_proxy) {
	phost = g_settings_get_string (child_settings, "host");
	pport = g_settings_get_int (child_settings, "port");
      }

      ignored = g_settings_get_strv (settings, "ignore-hosts");
      if (ignored != NULL) {
	char **ptr;
	size_t urlhost_len, s_len;
	
	urlhost = (*env)->GetStringUTFChars(env, host, &isCopy);
	urlhost_len = strlen (urlhost);
	if (urlhost != NULL) {
	  for (ptr = ignored; *ptr != NULL; ++ptr) {
	    s_len = strlen (*ptr);
	    if (s_len <= urlhost_len) {
	      if (strcasecmp (urlhost + (urlhost_len - s_len), *ptr) == 0) {
		use_proxy = 0;
		break;
	      }
	    }
	  }
	  if (isCopy == JNI_TRUE)
	    (*env)->ReleaseStringUTFChars(env, host, urlhost);
	}
	
	g_strfreev (ignored);
	g_object_unref (child_settings);
	g_object_unref (settings);
      }
    } else {
      if (gconf_client == NULL) {
	g_type_init();
	gconf_client = gconf_client_get_default();
      }

      if (gconf_client != NULL) {
	char *noproxyfor;
	char *s;

	if (cproto != NULL) {
	  /**
	   * We will have to check protocol by protocol as they do use different
	   * entries.
	   */
	  
	  use_same_proxy = gconf_client_get_bool (gconf_client, "/system/http_proxy/use_same_proxy", NULL);
	  if (use_same_proxy) {
	    use_proxy = gconf_client_get_bool (gconf_client, "/system/http_proxy/use_http_proxy", NULL);
	    if (use_proxy) {
	      phost = gconf_client_get_string(gconf_client, "/system/http_proxy/host", NULL);
	      pport = gconf_client_get_int(gconf_client, "/system/http_proxy/port", NULL);
	    }
	  }
	  
	  /**
	   * HTTP:
	   * /system/http_proxy/use_http_proxy (boolean)
	   * /system/http_proxy/host (string)
	   * /system/http_proxy/port (integer)
	   */
	  if (strcasecmp(cproto, "http") == 0) {
	    use_proxy = gconf_client_get_bool (gconf_client, "/system/http_proxy/use_http_proxy", NULL);
	    if (use_proxy) {
	      if (!use_same_proxy) {
		phost = gconf_client_get_string(gconf_client, "/system/http_proxy/host", NULL);
		pport = gconf_client_get_int(gconf_client, "/system/http_proxy/port", NULL);
	      }
	    }
	  }
	  
	  /**
	   * HTTPS:
	   * /system/proxy/mode (string) [ "manual" means use proxy settings ]
	   * /system/proxy/secure_host (string)
	   * /system/proxy/secure_port (integer)
	   */
	  if (strcasecmp(cproto, "https") == 0) {
	    mode =  gconf_client_get_string(gconf_client, "/system/proxy/mode", NULL);
	    if (mode != NULL && (strcasecmp(mode,"manual") == 0)) {
	      if (!use_same_proxy) {
		phost = gconf_client_get_string(gconf_client, "/system/proxy/secure_host", NULL);
		pport = gconf_client_get_int(gconf_client, "/system/proxy/secure_port", NULL);
	      }
	      use_proxy = (phost != NULL);
	    }
	  }
	  
	  /**
	   * FTP:
	   * /system/proxy/mode (string) [ "manual" means use proxy settings ]
	   * /system/proxy/ftp_host (string)
	   * /system/proxy/ftp_port (integer)
	   */
	  if (strcasecmp(cproto, "ftp") == 0) {
	    mode =  gconf_client_get_string(gconf_client, "/system/proxy/mode", NULL);
	    if (mode != NULL && (strcasecmp(mode,"manual") == 0)) {
	      if (!use_same_proxy) {
		phost = gconf_client_get_string(gconf_client, "/system/proxy/ftp_host", NULL);
		pport = gconf_client_get_int(gconf_client, "/system/proxy/ftp_port", NULL);
	      }
	      use_proxy = (phost != NULL);
	    }
	  }
	  
	  /**
	   * GOPHER:
	   * /system/proxy/mode (string) [ "manual" means use proxy settings ]
	   * /system/proxy/gopher_host (string)
	   * /system/proxy/gopher_port (integer)
	   */
	  if (strcasecmp(cproto, "gopher") == 0) {
	    mode =  gconf_client_get_string(gconf_client, "/system/proxy/mode", NULL);
	    if (mode != NULL && (strcasecmp(mode,"manual") == 0)) {
	      if (!use_same_proxy) {
		phost = gconf_client_get_string(gconf_client, "/system/proxy/gopher_host", NULL);
		pport = gconf_client_get_int(gconf_client, "/system/proxy/gopher_port", NULL);
	      }
	      use_proxy = (phost != NULL);
	    }
	  }
	  
	  /**
	   * SOCKS:
	   * /system/proxy/mode (string) [ "manual" means use proxy settings ]
	   * /system/proxy/socks_host (string)
	   * /system/proxy/socks_port (integer)
	   */
	  if (strcasecmp(cproto, "socks") == 0) {
	    mode =  gconf_client_get_string(gconf_client, "/system/proxy/mode", NULL);
	    if (mode != NULL && (strcasecmp(mode,"manual") == 0)) {
	      if (!use_same_proxy) {
		phost = gconf_client_get_string(gconf_client, "/system/proxy/socks_host", NULL);
		pport = gconf_client_get_int(gconf_client, "/system/proxy/socks_port", NULL);
	      }
	      use_proxy = (phost != NULL);
	    }
	  }

	}
	noproxyfor = gconf_client_get_string(gconf_client, "/system/proxy/no_proxy_for", NULL);
	
	/**
	 * check for the exclude list (aka "No Proxy For" list).
	 * It's a list of comma separated suffixes (e.g. domain name).
	 */
	if (noproxyfor != NULL) {
	  char *tmpbuf[512];
	  
	  s = strtok_r(noproxyfor, ", ", tmpbuf);
	  urlhost = (*env)->GetStringUTFChars(env, host, &isCopy);
	  if (urlhost != NULL) {
	    while (s != NULL && strlen(s) <= strlen(urlhost)) {
	      if (strcasecmp(urlhost+(strlen(urlhost) - strlen(s)), s) == 0) {
		/**
		 * the URL host name matches with one of the sufixes,
		 * therefore we have to use a direct connection.
		 */
		use_proxy = 0;
		break;
	      }
	      s = strtok_r(NULL, ", ", tmpbuf);
	    }
	    if (isCopy == JNI_TRUE)
	      (*env)->ReleaseStringUTFChars(env, host, urlhost);
	  }
	}
      }
    } 
    if (isCopy == JNI_TRUE)
      (*env)->ReleaseStringUTFChars(env, proto, cproto);
    g_free (mode);
    
    if (use_proxy && (phost != NULL)) {
      CHECK_NULL(type_proxy = (*env)->GetStaticObjectField(env, ptype_class, ptype_httpID));
      jhost = (*env)->NewStringUTF(env, phost);
      isa = (*env)->CallStaticObjectMethod(env, isaddr_class, isaddr_createUnresolvedID, jhost, pport);
      proxy = (*env)->NewObject(env, proxy_class, proxy_ctrID, type_proxy, isa);
      g_free (phost);
      return proxy;
    }
  }
   
 CHECK_NULL(no_proxy = (*env)->GetStaticObjectField(env, proxy_class, pr_no_proxyID));
 return no_proxy;
}
