#include "sun_awt_haiku_BCanvasPeer.h"
#include "CanvasAdapter.h"

/*
 * Class:     sun_awt_haiku_BCanvasPeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BCanvasPeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	new CanvasAdapter(jenv, jpeer, jparent);
	ObjectAdapter::getAdapter<CanvasAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BCanvasPeer
 * Method:    resetTargetGC
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BCanvasPeer_resetTargetGC
  (JNIEnv * jenv, jobject jpeer)
{
	CanvasAdapter * adapter = ObjectAdapter::getAdapter<CanvasAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->resetTargetGC();
}

