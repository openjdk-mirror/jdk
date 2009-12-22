#include "HaikuSurfaceData.h"

#include "sun_awt_HaikuSurfaceData.h"
#include "GraphicsPrimitiveMgr.h"
#include "Region.h"

#include "awt_HaikuGraphicsDevice.h"
#include "ToolkitAdapter.h"
#include "jni_util.h"

// Haiku Surface Data Operations
static LockFunc        Haiku_Lock;
static GetRasInfoFunc  Haiku_GetRasInfo;
static ReleaseFunc     Haiku_Release;
static UnlockFunc      Haiku_Unlock;
static SetupFunc       Haiku_Setup;
static DisposeFunc     Haiku_Dispose;

// Static members
static jboolean beingShutdown = JNI_FALSE;
static jclass xorCompClass;

extern "C" {

static int32 gfxState = tls_allocate();

/**
 * The heart n' soul of maintaining Graphics State within this thread.
 * 
 * Handles the creation / adaptation of the bufferView controller and BBitmap buffer
 * for surfaces. Stashes everything in a TLS Struct so we don't get confused, and 
 * generally handles all the dirty work of maintaining BBitmap and BView for a 
 * surface we're drawing to.
 *
 * If at any point something occurrs here that will cause a failure further in the 
 * drawing process, we set bsdo->lock = true. This will cause the next Haiku_Lock()
 * on this bsdo object to fail, resulting in a dropped update -- which might not
 * be such a bad thing. For instance, if the bounds width and heigth are 0, then
 * why bother with successful locking, drawing, unlocking? Fail faster I say!
 */
void SetupThreadGraphicsInfo(JNIEnv *env, HaikuSDOps *bsdo) {
	ThreadGraphicsInfo *info = (ThreadGraphicsInfo*)tls_get(gfxState);
	if (info == NULL) {
		// Initialize only what's necessary to get the TLS space.
		info = new ThreadGraphicsInfo();
		info->bsdo = NULL;
		info->count = 0;
		tls_set(gfxState, (void*)info);
	}
	
	// We've never run on this thread!
	if (info->bsdo != bsdo) {
		info->bsdo = bsdo;
		info->lockflags = 0;
		info->lock = false;
		//info->xorcolor = 
	}
}

/*
 * Class:     sun_awt_HaikuSurfaceData
 * Method:    initIDs
 * Signature: (Ljava/lang/Class;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuSurfaceData_initIDs
  (JNIEnv *env, jclass cls, jclass xorComp)
{
	xorCompClass = (jclass)env->NewGlobalRef(xorComp);
	return;
}

/*
 * Class:     sun_awt_HaikuSurfaceData
 * Method:    initOps
 * Signature: (Lsun/awt/haiku/BComponentPeer;IIIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuSurfaceData_initOps
  (JNIEnv *env, jobject self, jobject peer, jint depth, jint redMask, jint greenMask, jint blueMask, jint numBuffers, jint screen)
{
	HaikuSDOps *bsdo = (HaikuSDOps *)SurfaceData_InitOps(env, self, sizeof(HaikuSDOps));
	
	// Setup the standard sdOps functions.
	bsdo->sdOps.Lock = Haiku_Lock;
	bsdo->sdOps.GetRasInfo = Haiku_GetRasInfo;
	bsdo->sdOps.Release = Haiku_Release;
	bsdo->sdOps.Unlock = Haiku_Unlock;
	bsdo->sdOps.Setup = Haiku_Setup;
	bsdo->sdOps.Dispose = Haiku_Dispose;
	
	// Haiku Specific Ops.
	bsdo->invalid = JNI_FALSE;
	bsdo->peer = env->NewWeakGlobalRef(peer);
	bsdo->bufferCount = numBuffers;
	bsdo->component = HaikuSurfaceData_GetComp(env, bsdo);
	
	return;
}

/*
 * Class:     sun_awt_HaikuSurfaceData
 * Method:    _invalidate
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuSurfaceData__1invalidate
  (JNIEnv *env, jobject self)
{
	// Only invalidate the graphics object this thread last accessed.
	ThreadGraphicsInfo *info = (ThreadGraphicsInfo*)tls_get(gfxState);
	if (info != NULL) {
		info->bsdo->invalid = JNI_TRUE;
	}
}


/*
 * Class:     sun_awt_HaikuSurfaceData
 * Method:    restoreSurface
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuSurfaceData_restoreSurface
  (JNIEnv *, jobject)
{
	fprintf(stderr, "HaikuSurfaceData_restoreSurface()\n");
}

/*
 * Class:     sun_awt_HaikuSurfaceData
 * Method:    flip
 * Signature: (Lsun/java2d/SurfaceData;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuSurfaceData_flip
  (JNIEnv *, jobject, jobject)
{
	fprintf(stderr, "HaikuSurfaceData_flip()!\n");
}

}; // extern "C"


JNIEXPORT ComponentAdapter * JNICALL HaikuSurfaceData_GetComp
  (JNIEnv *env, HaikuSDOps *bsdo) 
{
	jobject localObj = env->NewLocalRef(bsdo->peer);

	ComponentAdapter *component = ObjectAdapter::getAdapter<ComponentAdapter>(env, localObj);
	// in case we failed to get the component...
	if (localObj == NULL || component == NULL) {
		if (beingShutdown == JNI_TRUE) {
			bsdo->invalid = JNI_TRUE;
		} else if (! ToolkitAdapter::GetInstance()->VerifyActive()) {
			beingShutdown = JNI_TRUE;
			bsdo->invalid = JNI_TRUE;
		} else {
			JNU_ThrowNullPointerException(env, "component argument pData");
		}
		component = NULL;
	}
	return component;
}


/* Common Surface Data Operations (SDOPS) that must be implemented. */

static jint Haiku_Lock(JNIEnv *env, 
				SurfaceDataOps *ops,
				SurfaceDataRasInfo *pRasInfo,
				jint lockflags)
{
	HaikuSDOps *bsdo = (HaikuSDOps*)ops;
	ThreadGraphicsInfo *info = (ThreadGraphicsInfo*)tls_get(gfxState);
	
	int ret = SD_FAILURE;
	
	if (bsdo->invalid == JNI_TRUE) {
		if (beingShutdown != JNI_TRUE) {
			SurfaceData_ThrowInvalidPipeException(env, "HaikuSurfaceData: bounds changed.");
			bsdo->invalid = JNI_FALSE;
		}
	} else if (! info->lock && (lockflags & SD_LOCK_RD_WR)) {
		SurfaceDataBounds *bounds = &pRasInfo->bounds;
		
		BRect compBounds = info->bsdo->component->GetSurface()->Bounds();
		if (compBounds.IsValid()) { 
			if (bounds->x1 < compBounds.left) {
				bounds->x1 = (jint)compBounds.left;
			}
			if (bounds->y1 < compBounds.top) {
				bounds->y1 = (jint)compBounds.top;
			}
			if (bounds->x2 > compBounds.right) {
				bounds->x2 = (jint)compBounds.right;
			}
			if (bounds->y2 > compBounds.bottom) {
				bounds->y2 = (jint)compBounds.bottom;
			}
			
			ret = SD_SUCCESS;
			
			// SD_LOCK_FASTEST not implemented. See GetRasInfo.
			if (lockflags & SD_LOCK_FASTEST) {
				ret = SD_SLOWLOCK;
			}
		}
	}
	info->lock = (ret != SD_FAILURE);
	info->lockflags = lockflags;
	
	return ret;
}

static void Haiku_GetRasInfo(JNIEnv *env,
				SurfaceDataOps *ops,
				SurfaceDataRasInfo *pRasInfo)
{
	HaikuSDOps *bsdo = (HaikuSDOps*)ops;
	ThreadGraphicsInfo *info = (ThreadGraphicsInfo*)tls_get(gfxState);
	
	if (! info->lock) {
		memset(pRasInfo, 0, sizeof(*pRasInfo));
		return;
	}
	
	if (info->lockflags & SD_LOCK_FASTEST) {
		// Windows trys to lock with DirectDraw, then reevaluates the necessary 
		// clipping region. Under Windows there's API hooks to tell if a window
		// is obstructed, reducing the size of the view that may need to be 
		// repainted. This can be a real boon to performance when the paint 
		// involves Text.
		//
		// Under Haiku, the only way we'd be able to do this now (since there's 
		// no method to tell if a window is obstructed through the normall api)
		// would be to use BDirectWindows for everything, navigate to the 
		// Window the current component is associated with, and perform an 
		// intersection of the current clipping region of the BDirectWindow 
		// with the components bounds within the window.
		//
		// However, since not all gfx cards support the BDirectWindow (most do)
		// API, and since we're trying to get things 'working' now, I feel this
		// should be skipped until a point in time when we have nothing better
		// to do.
		// 
		// Addendum 4.29.2005
		// 
		// Through the use of functions exposed for OpenTracker, it would be
		// _POSSIBLE_ however a terriffic kludge to find if our window is 
		// obstructed.
		// 
		// The functions get_window_list(...) and get_window_info(...)
		// could be used to grab the OS-wide window list. If our window is the first
		// in the list, it's reasonably to assume it is completely unobstructed.
		// From there, it becomes a task of building an un-clipped region based
		// upon the windows in front of ours.
		// I do not believe the effort in doing this would be worth the benefits
		// at this point, especially when I see "DANGER, WILL ROBINSON!" in the
		// necessary header from OpenTracker.
		//
		// -Varner 12.11.2004
	}
	
	// set pRasInfo to the current BBitmaps buffer!
	pRasInfo->rasBase = info->bsdo->component->GetSurface()->Bits();
	pRasInfo->pixelStride = 4;
	pRasInfo->scanStride = (uint)info->bsdo->component->GetSurface()->BytesPerRow();
	
	if (info->lockflags & SD_LOCK_LUT) {
		debugger("TODO: SD_LOCK_LUT\n");
		pRasInfo->lutBase = NULL; 
		pRasInfo->lutSize = 0;
	} else {
		pRasInfo->lutBase = NULL; 
		pRasInfo->lutSize = 0;
	}
	
	if (info->lockflags & SD_LOCK_INVCOLOR) {
		debugger("TODO: SD_LOCK_INVCOLOR\n");
		pRasInfo->invColorTable = NULL;
		pRasInfo->redErrTable = NULL;
		pRasInfo->grnErrTable = NULL;
		pRasInfo->bluErrTable = NULL;
	} else {
		pRasInfo->invColorTable = NULL;
		pRasInfo->redErrTable = NULL;
		pRasInfo->grnErrTable = NULL;
		pRasInfo->bluErrTable = NULL;
	}
	
	if (info->lockflags & SD_LOCK_INVGRAY) {
		debugger("TODO: SD_LOCK_INVGRAY\n");
		pRasInfo->invGrayTable = NULL;
	} else {
		pRasInfo->invGrayTable = NULL;
	}
}

static void Haiku_Release(JNIEnv *env,
				SurfaceDataOps *ops,
				SurfaceDataRasInfo *pRasInfo)
{
	return;
}

/**
 * Unlock and blit the data to the component.
 */
static void Haiku_Unlock(JNIEnv *env,
				SurfaceDataOps *ops,
				SurfaceDataRasInfo *pRasInfo)
{
	HaikuSDOps *bsdo = (HaikuSDOps*)ops;
	ThreadGraphicsInfo *info = (ThreadGraphicsInfo*)tls_get(gfxState);
	if (info->lock) {
		info->lock = false;
	}
}

/*
 * Called by HaikuGraphics2D class. This is a horrible way of doing things.
 * 
 * Class:     sun_awt_HaikuSurfaceData
 * Method:    repaint
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_HaikuSurfaceData_repaint
  (JNIEnv *env, jobject self, jint x, jint y, jint w, jint h)
{
// 	fprintf(stdout, "REPAINT %d,%d - %dx%d\n", x, y, w, h);
	ThreadGraphicsInfo *info = (ThreadGraphicsInfo*)tls_get(gfxState);
	if (info != NULL) {
		info->bsdo->component->RepaintView(BRect(x, y, x + w, y + h));
	}
}


static void Haiku_Setup(JNIEnv *env, SurfaceDataOps *ops) {
	SetupThreadGraphicsInfo(env, (HaikuSDOps*)ops);
}

static void Haiku_Dispose(JNIEnv *env, SurfaceDataOps *ops) {
	HaikuSDOps *bsdo = (HaikuSDOps*)ops;
	ThreadGraphicsInfo *info = (ThreadGraphicsInfo*)tls_get(gfxState);
	
	env->DeleteWeakGlobalRef(bsdo->peer);
}


/*
 * Class:     sun_awt_HaikuSurfaceData
 * Method:    _copyArea
 * Signature: (IIIIII)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_HaikuSurfaceData__1copyArea
  (JNIEnv * jenv, jobject jpeer, jint x, jint y, jint w, jint h, jint dx, jint dy)
{
	ThreadGraphicsInfo *info = (ThreadGraphicsInfo*)tls_get(gfxState);
	if (info == NULL) {
		return false;
	}
	
	int source_y = (dy > 0 ? y + h - 1 : y);
	int destination_y = source_y + dy;
	BBitmap * surface = info->bsdo->component->GetSurface();
	if (surface == NULL) {
		fprintf(stderr, "null surface!\n");
		return false;
	}
	size_t pixel_chunk;
	size_t row_alignment;
	size_t pixels_per_chunk;
	if (get_pixel_size_for(surface->ColorSpace(), &pixel_chunk, &row_alignment, &pixels_per_chunk) != B_OK) {
		fprintf(stderr, "couldn't get pixel size for color space\n");
		return false;
	}
	if ((x % pixels_per_chunk != 0) || (w % pixels_per_chunk != 0) || (dx % pixels_per_chunk != 0)) {
		fprintf(stderr, "warning: could not divide moved pixels cleanly\n");
	}
	uint8 * bits = (uint8*)surface->Bits();
	uint8 * source_ybits      = bits + source_y      * surface->BytesPerRow();
	uint8 * destination_ybits = bits + destination_y * surface->BytesPerRow();
	int source_offset      = x        * pixel_chunk / pixels_per_chunk;
	int destination_offset = (x + dx) * pixel_chunk / pixels_per_chunk;
	int chunks             = w        * pixel_chunk / pixels_per_chunk;
	int delta_ybits = (dy > 0 ? -1 : 1 ) * surface->BytesPerRow();
	for (int yi = 0 ; yi < h ; yi++) {
		memmove(destination_ybits + destination_offset, source_ybits + source_offset, chunks);
		source_ybits      += delta_ybits;
		destination_ybits += delta_ybits;
	}
	return true;
}

