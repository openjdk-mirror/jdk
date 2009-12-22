/*
 * Copyright 2001-2004 Sun Microsystems, Inc.  All Rights Reserved.
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
 * Haiku Specific Implementation to support the Prefs API
 * Bryan Varner    2.24.03
 */

#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <utime.h>
#include <string.h>

#include "jni.h"
#include "jni_util.h"
#include "io_util.h"

#include <support/TypeConstants.h>
#include <kernel/fs_attr.h>
#include <kernel/fs_info.h>
#include <storage/FindDirectory.h>
#include <malloc.h>

/*
 * Class:     java_util_prefs_HaikuPreferences
 * Method:    getUserPrefsDir0
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_java_util_prefs_HaikuPreferences_getUserPrefsDir0(JNIEnv *env, jclass cls)
{
	status_t ret = B_OK;
	dev_t    bootdev = 0;
	char     path[MAXPATHLEN];
	int32    pathlen = 0;
	jstring  usrDir = NULL;
	
	// Get the dev_t for the boot device.
	bootdev = dev_for_path("/boot");
	if (bootdev < 0) {
		fprintf(stderr, "Failed to find dev_for_path(\"/boot\");\n");
	}
	ret = find_directory(B_USER_SETTINGS_DIRECTORY, bootdev, false, path, pathlen);
	sprintf(path, "%s/%s", path, "java");
	if (ret == B_OK) {
		usrDir = NewStringPlatform(env, path);
	} else {
		fprintf(stderr, "HaikuPreferences: find_directory failed\n");
	}
	return usrDir;
}

/*
 * Class:     java_util_prefs_HaikuPreferences
 * Method:    getSystemPrefsDir0
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_java_util_prefs_HaikuPreferences_getSystemPrefsDir0(JNIEnv *env, jclass cls)
{
	status_t ret = B_OK;
	dev_t    bootdev = 0;
	char     path[MAXPATHLEN];
	int32    pathlen = 0;
	jstring  usrDir = NULL;
	
	// Get the dev_t for the boot device.
	bootdev = dev_for_path("/boot");
	if (bootdev < 0) {
		fprintf(stderr, "Failed to find dev_for_path(\"/boot\");\n");
	}
	ret = find_directory(B_COMMON_SETTINGS_DIRECTORY, bootdev, false, path, pathlen);
	sprintf(path, "%s/%s", path, "java");
	if (ret == B_OK) {
		usrDir = NewStringPlatform(env, path);
	} else {
		fprintf(stderr, "HaikuPreferences: find_directory failed\n");
	}
	return usrDir;
}

/*
 * Class:     java_util_prefs_HaikuPreferences
 * Method:    putSpi0
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_java_util_prefs_HaikuPreferences_putSpi0(JNIEnv *env, jclass cls, jstring fileName,
											 jstring key, jstring value)
{
	int fd;
	char *attribName;
	// For Documentation on WITH_PLATFORM_STRING see io_util.h
	WITH_PLATFORM_STRING(env, fileName, path) {
	WITH_PLATFORM_STRING(env, key, ckey) {
	WITH_PLATFORM_STRING(env, value, cval) {
		fd = open(path, O_WRONLY);
		attribName = (char*)calloc(strlen(ckey) + strlen("JAVA:"), sizeof(char));
		sprintf(attribName, "JAVA:%s", ckey);
		if (fd > 0) {
			fs_write_attr(fd, attribName, B_STRING_TYPE, 0, cval, strlen(cval));
		}
		close(fd);
		free(attribName);
	} END_PLATFORM_STRING(env, cval);
	} END_PLATFORM_STRING(env, ckey);
	} END_PLATFORM_STRING(env, path);
}

/*
 * Class:     java_util_prefs_HaikuPreferences
 * Method:    getSpi0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_java_util_prefs_HaikuPreferences_getSpi0(JNIEnv *env, jclass cls, jstring fileName, jstring key)
{
	jstring ret = NULL;
	attr_info info;
	int fd;
	char *buffer = NULL;
	char *attribName = NULL;
	WITH_PLATFORM_STRING(env, fileName, path) {
	WITH_PLATFORM_STRING(env, key, ckey) {
		fd = open(path, O_RDONLY);
		if (fd > 0) {
			attribName = (char*)calloc(strlen(ckey) + strlen("JAVA:") + 1, sizeof(char));
			sprintf(attribName, "JAVA:%s", ckey);
			
			if (fs_stat_attr(fd, attribName, &info) >= 0) {
				buffer = (char *)calloc(info.size + 1, sizeof(char));
				
				if (fs_read_attr(fd, attribName, info.type, 0, buffer, info.size) >= 0) {
					ret = NewStringPlatform(env, buffer);
				}
				free(buffer);
			}
			free(attribName);
		}
		close(fd);
	} END_PLATFORM_STRING(env, ckey);
	} END_PLATFORM_STRING(env, path);
	
	return ret;
}

/*
 * Class:     java_util_prefs_HaikuPreferences
 * Method:    removeSpi0
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_java_util_prefs_HaikuPreferences_removeSpi0(JNIEnv *env, jclass cls, jstring fileName, jstring key)
{
	int fd;
	
	WITH_PLATFORM_STRING(env, fileName, path) {
	WITH_PLATFORM_STRING(env, key, ckey) {
		fd = open(path, O_WRONLY);
		if (fd > 0) {
			fs_remove_attr(fd, strcat("JAVA:", ckey));
		}
		close(fd);
	} END_PLATFORM_STRING(env, ckey);
	} END_PLATFORM_STRING(env, path);;
}

/*
 * Class:     java_util_prefs_HaikuPreferences
 * Method:    keysSpi0
 * Signature: (Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL
Java_java_util_prefs_HaikuPreferences_keysSpi0(JNIEnv *env, jclass cls, jstring fileName)
{
	DIR *atrbDir;
	dirent_t *ent;
	int fd;
	
	int len, maxlen;
	jobjectArray ret, old;
	ret = NULL;
	
	// Open the attribute directory.
	WITH_PLATFORM_STRING(env, fileName, path) {
		atrbDir = fs_open_attr_dir(path);
		if (atrbDir)
			fd = open(path, O_RDONLY);
		else
			return NULL;
	} END_PLATFORM_STRING(env, path);
	
	// Initial size of return value array.
	len = 0;
	maxlen = 16;
	ret = (*env)->NewObjectArray(env, maxlen, JNU_ClassString(env), NULL);
	if (ret == NULL)
		return NULL;
	
	// Check the file descriptor, iterate all attribute entries.
	if (fd > 0) {
		while (ent = fs_read_attr_dir(atrbDir)) {
			jstring name;
			if (len == maxlen) { // If we're full, copy and recreate the array.
				old = ret;
				ret = (*env)->NewObjectArray(env, maxlen <<= 1,
								JNU_ClassString(env), NULL);
				if (ret == NULL ||
					JNU_CopyObjectArray(env, ret, old, len) < 0)
				{
					close(fd);
					fs_close_attr_dir(atrbDir);
					return NULL;
				}
				(*env)->DeleteLocalRef(env, old);
			}
			
			// Verify this is a setting added by Java
			if (strncmp("JAVA:", ent->d_name, 5) == 0) {
				/*
				 Remove the "JAVA:" before the keyname.
				*/
				// get a pointer to the real part of the key name.
				char* keyNamePtr = ent->d_name + strlen("JAVA:");
				
				char* keyName = (char*)calloc(strlen(ent->d_name) - strlen("JAVA:"), sizeof(char));
				strcpy(keyName, keyNamePtr);
				
				// Create the new jstring for this entry.
				name = JNU_NewStringPlatform(env, keyName);
				free(keyName);
				if (name == NULL) {
					close(fd);
					fs_close_attr_dir(atrbDir);
					return NULL;
				}
				(*env)->SetObjectArrayElement(env, ret, len++, name);
				(*env)->DeleteLocalRef(env, name);
			}
		}
		// Finished, clean up open files
		close(fd);
		fs_close_attr_dir(atrbDir);
		
		
	    /* Copy the final results into an appropriately-sized array */
	    old = ret;
	    ret = (*env)->NewObjectArray(env, len, JNU_ClassString(env), NULL);
	    if (ret == NULL)
	    	return NULL;
	    if (JNU_CopyObjectArray(env, ret, old, len) < 0) 
	    	return NULL;
	}
    return ret;
}
