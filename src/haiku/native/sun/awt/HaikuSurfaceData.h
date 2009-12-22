#ifndef HAIKU_SURFACE_DATA
#define HAIKU_SURFACE_DATA

#include "jni.h"
#include "colordata.h"
#include "awt_HaikuGraphicsDevice.h"
#include "sun_awt_HaikuSurfaceData.h"
#include "SurfaceData.h"
#include "ComponentAdapter.h"

#include <View.h>
#include <Bitmap.h>
#include <Rect.h>
#include <TLS.h>

/**
 * Object-specific surface-data. Each on-screen component gets a HaikuSDOps
 * 
 * These ops provide current global state of the surface data a given object.
 * Global state meaning accessible to all threads, at any time.
 */
typedef struct {
    SurfaceDataOps		sdOps;
    jboolean			invalid;
    jobject				peer;
    ComponentAdapter	*component;
    jint				bufferCount;
} HaikuSDOps;


/**
 * Thread specific state for a surface data object.
 * 
 * When drawing to a HaikuSDOps object, between calls invoked by the same thread
 * things such as pen size, location, etc. should be maintained.
 */
typedef struct {
    HaikuSDOps			*bsdo;
    jint				lockflags;
    bool				lock;
    jint				count;
	jint				xorcolor;
} ThreadGraphicsInfo;


JNIEXPORT ComponentAdapter* JNICALL HaikuSurfaceData_GetComp
  (JNIEnv *env, HaikuSDOps *bsdo);

#endif // HAIKU_SURFACE_DATA
