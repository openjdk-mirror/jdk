/**
 * Haiku Graphics Config Implementation.
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
#include <Accelerant.h>
#include <Screen.h>
#include "awt_HaikuGraphicsConfig.h"

/*
 * Class:     sun_awt_HaikuGraphicsConfig
 * Method:    getWidth
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_HaikuGraphicsConfig_getWidth(JNIEnv *env, jobject obj, jint workspace)
{
	display_mode mode;
	if (BScreen().GetMode((uint32)workspace, &mode) == B_OK) {
		return mode.virtual_width;
	} else {
		return -1;
	}
}

/*
 * Class:     sun_awt_HaikuGraphicsConfig
 * Method:    getHeight
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_HaikuGraphicsConfig_getHeight(JNIEnv *env, jobject obj, jint workspace)
{
	display_mode mode;
	if (BScreen().GetMode((uint32)workspace, &mode) == B_OK) {
		return mode.virtual_height;
	} else {
		return -1;
	}
}
