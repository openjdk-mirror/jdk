/*
 * Copyright 1995-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

#include "jni.h"
#include "jvm.h"
#include "jvm_md.h"
#include "jni_util.h"

/*
 * Platform-specific support for java.lang.HaikuProcess
 */

#include <stdlib.h>
#include <sys/wait.h>
#include <signal.h>
#include <string.h>
#include <errno.h>
#include <image.h>
#include <debugger.h>

/* path in the environment */
static char **PATH = 0;
/* effective uid */
static uid_t uid;
/* effective group id */
static gid_t gid;

static void
parsePath () {
    char *path, *c, *len;
    int count = 0;
    int i;

    /* get uid, gid */
    uid = geteuid();
    gid = getegid();
    if ((path = getenv("PATH")) == 0) {
	return;
    }
    path = strdup(path);
    len = path + strlen(path);
    /* count path elements */
    for (c = path; c < len; c++) {
	if (*c == ':')
	    count++;
    }
    PATH = (char **)malloc((++count+1) * sizeof(char*));

    /* fill it in */
    PATH[0] = path;
    PATH[count] = 0;

    for (i = 1; i < count; i++) {
	c = strchr(path, ':');
	if (c == 0) { /* shouldn't happen */
	    /*	    jio_fprintf(stderr, "processmd.c: wrong count parsing path: i=%d, count=%d\n", i, count); */
	    break;
	}
	*c++ = 0;
	PATH[i] = path = c;
    }
}

/* return 0 if it is executable && readable by this process
 * -1 if no such file, -2 if it cannot be executed.
 */
static int
statExecutable(char *exe, struct stat *b) {
    if (stat(exe, b)) { /* doesn't exist */
	return -1;
    }
    if (S_ISDIR(b->st_mode)) {
	/* cannot execute */
	return -2;
    }
    /* check for user permissions */
    if (b->st_uid == uid) {
	return (b->st_mode & S_IXUSR) ? 0 : -2;
    }
    /* check for group permissions */
    if (b->st_gid == gid) {
	return (b->st_mode & S_IXGRP) ? 0 : -2;
    }

    /* check for world permissions */
    return b->st_mode & S_IXOTH ? 0 : -2;
}

/* Find the command like a shell would.
 * signal an error for things not executable || not readable || not found.
 */

static char *fullPath(JNIEnv *env, char *part, char *full) {
    char **tmp;
    struct stat b;
    int ret;
    /*
     * If the filename we want to exec has any slashes in it then
     * we shouldn't do a path search, as in /foo ./foo or foo/bar.
     */
    if ((strchr(part, '/') == NULL) && PATH) {
	for (tmp = PATH; *tmp; tmp++) {
	    strcpy(full, *tmp);
	    /*
	     * empty path elements are like '.' so we don't want to append
	     * a slash to them.  Otherwise foo becomes /foo.
	     */
	    if (full[0] != '\0') {
		strcat(full, "/");
	    }
	    strcat(full, part);
	    ret = statExecutable(full, &b);
	    if (ret == -1) { /* doesn't exist */
		continue;
	    } else if (ret == -2) { /* can't execute */
		continue;    /* bug 4199993 - got to keep searching. */
	    } else {
		return full;
	    }
	}
    } else if (!(ret = statExecutable(part, &b))) {
	/* always copy value to be returned so `part' may always be freed
	 * after call (if needed) */
	strcpy(full, part);
	return full;
    } else if (ret == -2) { /* cannot execute */
	jio_snprintf(full, MAXPATHLEN, "%s: cannot execute", part);
	JNU_ThrowIOException(env, full);
	return 0;
    }

    /* not found if we got here */
    jio_snprintf(full, MAXPATHLEN, "%s: not found", part);
    JNU_ThrowIOException(env, full);
    return 0;
}

static jfieldID field_fd = 0;
static jfieldID field_exitcode = 0;

static int
initFieldIDs(JNIEnv *env, jobject process, jobject fd) {
    jclass tmpC;

    if (field_exitcode != 0) return 0;

    tmpC = (*env)->GetObjectClass(env, process);;
    field_exitcode = (*env)->GetFieldID(env, tmpC, "exitcode", "I");
    if (field_exitcode == 0) {
	JNU_ThrowInternalError(env, "Can't find field HaikuProcess.exitcode");
	return -1;
    }
    tmpC = (*env)->GetObjectClass(env, fd);
    field_fd = (*env)->GetFieldID(env, tmpC, "fd", "I");
    if (field_fd == 0) {
	JNU_ThrowInternalError(env, "Can't find field FileDescriptor.fd");
	field_exitcode = 0;
	return -1;
    }
    return 0;
}


/* Block until a child process exits and return its exit code.
   Note, can only be called once for any given pid. */
JNIEXPORT jint JNICALL
Java_java_lang_HaikuProcess_waitForThreadDeath(JNIEnv* env,
					      jobject junk,
					      jint pid)
{   int status;
    int info;
    /* Wait for the child process to exit.  This returns immediately if
       the child has already exited. */
     if (waitpid(pid, &info, 0) < 0)
        if (errno == ECHILD) {
            return 0;
        } else {
            return -1;
        }

    if (WIFEXITED(info)) {
        /*
         * The child exited normally, get its exit code
         */
        status = (signed char)WEXITSTATUS(info);
    } else if (WIFSIGNALED(info)) {
        /*
         * The child exited because of a signal,
         * compute its exit code based on the signal number.
         */
         status = 0x80 + WTERMSIG(status);
    } else {
        /*
         * Unknown exit code, pass it through.
         */
        status = info;
    }
    return status;
}

int closeDescriptors()
{
    DIR *dp;
    struct dirent *dirp;
    char procdir[20];
    long desc;
    pid_t pid;
    if ((pid = getpid()) < 0)
        return 0;
    sprintf(procdir, "/proc/%d/fd", pid);
    close(3); /* to be sure a descriptor is available for opendir */
    if ((dp = opendir(procdir)) == NULL)
        return 0;

    while ((dirp = readdir(dp)) != NULL) {
        if (*(dirp->d_name) != '.') { /* first symbol not a dot */
            if ((desc = strtol(dirp->d_name, NULL, 10)) > 2 &&
                                              desc != dp->fd) {
                close(desc);
            }
        }
    }
    closedir(dp);
    return 1;
}

JNIEXPORT jint JNICALL
Java_java_lang_HaikuProcess_spawnAndRun(JNIEnv *env,
				       jobject process,
				       jobjectArray cmdarray,
				       jobjectArray envp,
                                       jstring path,
				       jobject stdin_fd,
				       jobject stdout_fd,
				       jobject stderr_fd)
{
    jstring str;
    int resultPid = -1;
    int fdin[2], fdout[2], fderr[2], k;
    char **cmdv, **envv;
    int cmdlen, envlen = 0;
    char fullpath[MAXPATHLEN+1];
    int i, j;
    char *cwd = NULL;
    int old_in, old_out, old_err;

    if (initFieldIDs(env, process, stdin_fd) != 0)
	return -1;

    cmdlen = (*env)->GetArrayLength(env, cmdarray);
    if (cmdlen == 0) {
	JNU_ThrowIllegalArgumentException(env, NULL);
	return -1;
    }
    cmdv = (char **)malloc((cmdlen + 1) * sizeof(char *));
    if (cmdv == NULL) {
	JNU_ThrowOutOfMemoryError(env, 0);
	return -1;
    }
    cmdv[cmdlen] = NULL;
    for (i = 0; i < cmdlen; ++i) {
	str = (*env)->GetObjectArrayElement(env, cmdarray, i);
	if (str == 0) {
	    JNU_ThrowNullPointerException(env, NULL);
	    for (j = 0; j < i; ++j) free(cmdv[j]);
	    goto cleanup1;
	}
	cmdv[i] = (char *) JNU_GetStringPlatformChars(env, str, NULL);
	if (cmdv[i] == NULL) {
	    for (j = 0; j < i; ++j) free(cmdv[j]);
	    goto cleanup1;
	}
    }

    if (PATH == 0) {
	parsePath();
    }

    if (fullPath(env, cmdv[0], fullpath) == NULL) {
	/* fullPath has signalled an exception so we just return */
	free(cmdv[0]);
	goto cleanup2;
    }
    free(cmdv[0]);
    cmdv[0] = fullpath;

    if (0 != envp) {
	envlen = (*env)->GetArrayLength(env, envp);
    }
    envv = (char **)malloc((envlen+1) * sizeof(char *));
    if (envv == NULL) {
	JNU_ThrowOutOfMemoryError(env, 0);
	goto cleanup2;
    }
    envv[envlen] = NULL;
    if (envlen != 0) {
	for (i = 0; i < envlen; ++i) {
	    str = (*env)->GetObjectArrayElement(env, envp, i);
	    if (str == 0) {
		JNU_ThrowNullPointerException(env, NULL);
		for (j = 0; j < i; ++j) free(envv[j]);
		goto cleanup3;
	    }
	    envv[i] = (char *) JNU_GetStringPlatformChars(env, str, NULL);
	    if (envv[i] == NULL) {
		for (j = 0; j < i; ++j) free(envv[j]);
		goto cleanup3;
 	    }
	}
    }

    if ((k=0, pipe(fdin)<0) || (k=1, pipe(fdout)<0) || (k=2, pipe(fderr)<0)) {
	char errmsg[128];
        sprintf(errmsg, "errno: %d, error: %s\n", errno, "Bad file descriptor");
        JNU_ThrowIOExceptionWithLastError(env, errmsg);
	/* make sure we clean up */
	switch (k) {
	case 2:	close(fdout[0]);
		close(fdout[1]);
	case 1:	close(fdin[0]);
		close(fdin[1]);
	case 0: ;
	}
	goto cleanup4;
    }

    /*
     * Can't do this after the fork1 in child, because child can deadlock
     * on locks if they are held in parent
     */
    if (path != NULL) {
        cwd = (char *)JNU_GetStringPlatformChars(env, path, NULL);
    }
 
    old_in = dup(STDIN_FILENO);
    old_out = dup(STDOUT_FILENO);
    old_err = dup(STDERR_FILENO);

    /* 0 open for reading, 1 open for writing */
    /* (Note: it is possible for fdin[0] == 0 - 4180429) */
    dup2(fdin[0], STDIN_FILENO);
    dup2(fdout[1], STDOUT_FILENO);
    dup2(fderr[1], STDERR_FILENO);

	/* Close pipe fds here since they don't always show up
	 * in /proc/<pid>/fd
	 */
	if (fdin[0] > 2)
	    close(fdin[0]);
	if (fdout[1] > 2)
	    close(fdout[1]);
	if (fderr[1] > 2)
	    close(fderr[1]);

    /* change to the new cwd */
    if (cwd != NULL) {
        if (chdir(cwd) < 0) {
            /* failed to change directory, cleanup */
            char errmsg[128];
            sprintf(errmsg, "errno: %d, error: %s\n", errno, "Failed to change directory");
            JNU_ThrowByNameWithLastError(env, "java/io/IOException", errmsg);
            goto cleanup5;
        }
    }

    if (envp == NULL) {
        resultPid = load_image(cmdlen,cmdv,environ);
    } else {
        resultPid = load_image(cmdlen,cmdv,envv);
    }

    if (resultPid < 0) {
	char errmsg[128];
        sprintf(errmsg, "errno: %d, error: %s\n", errno, "load_image failed");
        JNU_ThrowIOExceptionWithLastError(env, errmsg);
	/* make sure we clean up our side of the pipes */
	close(fdin[1]);
	close(fdout[0]);
	close(fderr[0]);
	goto cleanup5;
    }

	resume_thread(resultPid);

    /* parent process */
    if (cwd != NULL)
        JNU_ReleaseStringPlatformChars(env, path, cwd);        
    (*env)->SetIntField(env, stdin_fd, field_fd, fdin[1]);
    (*env)->SetIntField(env, stdout_fd, field_fd, fdout[0]);
    (*env)->SetIntField(env, stderr_fd, field_fd, fderr[0]);

 cleanup5:
    close(STDIN_FILENO); dup(old_in); close(old_in);
    close(STDOUT_FILENO); dup(old_out); close(old_out);
    close(STDERR_FILENO); dup(old_err); close(old_err);
    /* clean up the child's side of the pipes */
    close(fdin[0]);
    close(fdout[1]);
    close(fderr[1]);
 cleanup4:
    for (j = 0; j < envlen; ++j) free(envv[j]);
 cleanup3:
    free(envv);
 cleanup2:
    for (j = 1; j < cmdlen; ++j) free(cmdv[j]);
 cleanup1:
    free(cmdv);

    return resultPid;
}

JNIEXPORT void JNICALL
Java_java_lang_HaikuProcess_destroyProcess(JNIEnv *env, jobject junk, jint pid) 
{
    kill(pid, SIGTERM);
}
