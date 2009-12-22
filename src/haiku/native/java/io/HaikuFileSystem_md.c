/*
 * Copyright 1998-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

#include <sys/types.h>
#include <sys/stat.h>
#include <string.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <time.h>
#include <utime.h>

#include "jni.h"
#include "jni_util.h"
#include "jlong.h"
#include "jvm.h"
#include "io_util.h"
#include "java_io_FileSystem.h"
#include "java_io_HaikuFileSystem.h"


/* -- Field IDs -- */

static struct {
	jfieldID path;
} ids;


JNIEXPORT void JNICALL
Java_java_io_HaikuFileSystem_initIDs(JNIEnv *env, jclass cls)
{
	jclass fileClass = (*env)->FindClass(env, "java/io/File");
	if (!fileClass) return;
	ids.path = (*env)->GetFieldID(env, fileClass,
				  "path", "Ljava/lang/String;");
}

/* -- Path operations -- */

extern int canonicalize(char *path, const char *out, int len);

JNIEXPORT jstring JNICALL
Java_java_io_HaikuFileSystem_canonicalize0(JNIEnv *env, jobject this,
                                          jstring pathname)
{
	jstring rv = NULL;

	WITH_PLATFORM_STRING(env, pathname, path) {
	char canonicalPath[JVM_MAXPATHLEN];
	if (canonicalize(JVM_NativePath((char *)path),
			 canonicalPath, JVM_MAXPATHLEN) < 0)
	{
		JNU_ThrowIOExceptionWithLastError(env, "Bad pathname");
	} else {
		rv = JNU_NewStringPlatform(env, canonicalPath);
	}
	} END_PLATFORM_STRING(env, path);
	return rv;
}


/* -- Attribute accessors -- */


static jboolean
statMode(const char *path, int *mode)
{
	struct stat sb;
	if (stat(path, &sb) == 0) {
		*mode = sb.st_mode;
		return JNI_TRUE;
	}
	return JNI_FALSE;
}


JNIEXPORT jint JNICALL
Java_java_io_HaikuFileSystem_getBooleanAttributes0(JNIEnv *env, jobject this,
                                                  jobject file)
{
    jint rv = 0;

    WITH_FIELD_PLATFORM_STRING(env, file, ids.path, path) {
		struct stat sb;
		if (stat(path, &sb) == 0) {
		    rv = (jint) (java_io_FileSystem_BA_EXISTS
		                 | (S_ISREG(sb.st_mode) ? java_io_FileSystem_BA_REGULAR : 0)
		                 | (S_ISDIR(sb.st_mode) ? java_io_FileSystem_BA_DIRECTORY : 0));
		}
    } END_PLATFORM_STRING(env, path);
    return rv;
}


JNIEXPORT jboolean JNICALL
Java_java_io_HaikuFileSystem_checkAccess(JNIEnv *env, jobject this,
                                        jobject file, jboolean write)
{
	jboolean rv = JNI_FALSE;

	WITH_FIELD_PLATFORM_STRING(env, file, ids.path, path) {
	if (access(path, (write ? W_OK : R_OK)) == 0) {
		rv = JNI_TRUE;
	}
	} END_PLATFORM_STRING(env, path);
	return rv;
}


JNIEXPORT jlong JNICALL
Java_java_io_HaikuFileSystem_getLastModifiedTime(JNIEnv *env, jobject this,
                                                jobject file)
{
	jlong rv = 0;

	WITH_FIELD_PLATFORM_STRING(env, file, ids.path, path) {
		struct stat sb;
		if (stat(path, &sb) == 0) {
			rv = 1000 * (jlong)sb.st_mtime;
		}
	} END_PLATFORM_STRING(env, path);
	return rv;
}


JNIEXPORT jlong JNICALL
Java_java_io_HaikuFileSystem_getLength(JNIEnv *env, jobject this,
                                      jobject file)
{
	jlong rv = 0;

	WITH_FIELD_PLATFORM_STRING(env, file, ids.path, path) {
		struct stat sb;
		if (stat(path, &sb) == 0) {
			rv = sb.st_size;
		}
	} END_PLATFORM_STRING(env, path);
	return rv;
}


/* -- File operations -- */


JNIEXPORT jboolean JNICALL
Java_java_io_HaikuFileSystem_createFileExclusively(JNIEnv *env, jclass cls,
                                                  jstring pathname)
{
	jboolean rv = JNI_FALSE;

	WITH_PLATFORM_STRING(env, pathname, path) {
	int fd;
	if (!strcmp (path, "/")) {
		fd = JVM_EEXIST;	/* The root directory always exists */
	} else {
		fd = JVM_Open(path, JVM_O_RDWR | JVM_O_CREAT | JVM_O_EXCL, 0666);
	}
	if (fd < 0) {
		if (fd != JVM_EEXIST) {
		JNU_ThrowIOExceptionWithLastError(env, path);
		}
	} else {
		JVM_Close(fd);
		rv = JNI_TRUE;
	}
	} END_PLATFORM_STRING(env, path);
	return rv;
}


JNIEXPORT jboolean JNICALL
Java_java_io_HaikuFileSystem_delete0(JNIEnv *env, jobject this,
                                    jobject file)
{
	jboolean rv = JNI_FALSE;

	WITH_FIELD_PLATFORM_STRING(env, file, ids.path, path) {
	if (remove(path) == 0) {
		rv = JNI_TRUE;
	}
	} END_PLATFORM_STRING(env, path);
	return rv;
}


JNIEXPORT jboolean JNICALL
Java_java_io_HaikuFileSystem_deleteOnExit(JNIEnv *env, jobject this,
                                         jobject file)
{
	WITH_FIELD_PLATFORM_STRING(env, file, ids.path, path) {
	deleteOnExit(env, path, remove);
	} END_PLATFORM_STRING(env, path);
	return JNI_TRUE;
}


JNIEXPORT jobjectArray JNICALL
Java_java_io_HaikuFileSystem_listRoots0(JNIEnv *env, jclass ignored)
{
	// Our return value array
	jobjectArray rv, old;
	
	int len;
	int maxlen;
	len = 0;
	//maxlen = 8;
	maxlen = 1;
	
	// Create the array
	rv = (*env)->NewObjectArray(env, maxlen, JNU_ClassString(env), NULL);
	if (rv == NULL)
		return NULL;
	
	// Read Trackers setting.
	
	// If Disks
		// Show all the volumes
	// else If Desktop && ! Disks
		// Read /boot/home/Desktop
		// Add all _volumes_ from /
	// Else
	{
		jstring name;
		name = JNU_NewStringPlatform(env, "/boot/");
		if (name == NULL)
			return NULL;
		(*env)->SetObjectArrayElement(env, rv, len++, name);
		(*env)->DeleteLocalRef(env, name);
	}
	
	/* Copy the final results into an appropriately-sized array */
    old = rv;
    rv = (*env)->NewObjectArray(env, len, JNU_ClassString(env), NULL);
    if (rv == NULL)
    	return NULL;
    if (JNU_CopyObjectArray(env, rv, old, len) < 0)
    	return NULL;
	
    return rv;
}

JNIEXPORT jobjectArray JNICALL
Java_java_io_HaikuFileSystem_list(JNIEnv *env, jobject this,
                                 jobject file)
{
	DIR *dir = NULL;
	struct dirent *ptr;
	int len, maxlen;
	jobjectArray rv, old;

	WITH_FIELD_PLATFORM_STRING(env, file, ids.path, path) {
	dir = opendir(path);
	} END_PLATFORM_STRING(env, path);
	if (dir == NULL) return NULL;

	/* Allocate an initial String array */
	len = 0;
	maxlen = 16;
	rv = (*env)->NewObjectArray(env, maxlen, JNU_ClassString(env), NULL);
	if (rv == NULL) goto error;

	/* Scan the directory */
	while ((ptr = readdir(dir)) != NULL) {
	jstring name;
  	if (!strcmp(ptr->d_name, ".") || !strcmp(ptr->d_name, ".."))
		continue;
	if (len == maxlen) {
		old = rv;
		rv = (*env)->NewObjectArray(env, maxlen <<= 1,
					JNU_ClassString(env), NULL);
		if (rv == NULL) goto error;
		if (JNU_CopyObjectArray(env, rv, old, len) < 0) goto error;
		(*env)->DeleteLocalRef(env, old);
	}
	name = JNU_NewStringPlatform(env, ptr->d_name);
	if (name == NULL) goto error;
	(*env)->SetObjectArrayElement(env, rv, len++, name);
	(*env)->DeleteLocalRef(env, name);
	}
	closedir(dir);

	/* Copy the final results into an appropriately-sized array */
    old = rv;
    rv = (*env)->NewObjectArray(env, len, JNU_ClassString(env), NULL);
    if (rv == NULL) {
        return NULL;
    }
    if (JNU_CopyObjectArray(env, rv, old, len) < 0) {
        return NULL;
    }
    return rv;

 error:
	closedir(dir);
	return NULL;
}


JNIEXPORT jboolean JNICALL
Java_java_io_HaikuFileSystem_createDirectory(JNIEnv *env, jobject this,
                                            jobject file)
{
	jboolean rv = JNI_FALSE;

	WITH_FIELD_PLATFORM_STRING(env, file, ids.path, path) {
	if (mkdir(path, 0777) == 0) {
		rv = JNI_TRUE;
	}
	} END_PLATFORM_STRING(env, path);
	return rv;
}


JNIEXPORT jboolean JNICALL
Java_java_io_HaikuFileSystem_rename0(JNIEnv *env, jobject this,
                                    jobject from, jobject to)
{
	jboolean rv = JNI_FALSE;

	WITH_FIELD_PLATFORM_STRING(env, from, ids.path, fromPath) {
	WITH_FIELD_PLATFORM_STRING(env, to, ids.path, toPath) {
		if (rename(fromPath, toPath) == 0) {
			rv = JNI_TRUE;
		}
	} END_PLATFORM_STRING(env, toPath);
	} END_PLATFORM_STRING(env, fromPath);
	return rv;
}

JNIEXPORT jboolean JNICALL  
Java_java_io_HaikuFileSystem_setLastModifiedTime(JNIEnv *env, jobject this,
                                                jobject file, jlong time)
{
	jboolean rv = JNI_FALSE;

	WITH_FIELD_PLATFORM_STRING(env, file, ids.path, path) {
	struct utimbuf tb;
	struct stat sb;
	// Get the last access time...
	if (stat(path, &sb) == 0) {
		tb.actime = sb.st_atime;
	}

	/* Change last-modified time */
	tb.modtime = time;

	if (utime(path, &tb) >= 0)
		rv = JNI_TRUE;

	error: ;
	} END_PLATFORM_STRING(env, path);

	return rv;
}


JNIEXPORT jboolean JNICALL  
Java_java_io_HaikuFileSystem_setReadOnly(JNIEnv *env, jobject this,
                                        jobject file)
{
	jboolean rv = JNI_FALSE;

	WITH_FIELD_PLATFORM_STRING(env, file, ids.path, path) {
	int mode;
	if (statMode(path, &mode)) {
		if (chmod(path, mode & ~(S_IWUSR | S_IWGRP | S_IWOTH)) >= 0) {
			rv = JNI_TRUE;
		}
	}
	} END_PLATFORM_STRING(env, path);
	return rv;
}
