/*
 * Haiku Graphics Device Implementation.
 */

#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <utime.h>

#include "jni.h"
#include "jni_util.h"

#include <InterfaceDefs.h>
#include "awt_HaikuGraphicsDevice.h"

/*
 * Class:     sun_awt_HaikuGraphicsDevice
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuGraphicsDevice_initIDs(JNIEnv *env, jclass cls)
{
	return;
}

/*
 * Class:     sun_awt_HaikuGraphicsDevice
 * Method:    getWorkspaceCount
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_HaikuGraphicsDevice_getWorkspaceCount(JNIEnv *env, jobject obj, jint i)
{
	return count_workspaces();
}

/*
 * Class:     sun_awt_HaikuGraphicsDevice
 * Method:    getCurrentWorkspace
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_HaikuGraphicsDevice_getCurrentWorkspace(JNIEnv *env, jobject obj, jint i)
{
	return current_workspace();
}
