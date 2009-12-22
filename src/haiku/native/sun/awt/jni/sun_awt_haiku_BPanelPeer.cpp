#include "sun_awt_haiku_BPanelPeer.h"
#include "PanelAdapter.h"

/*
 * Class:     sun_awt_haiku_BPanelPeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BPanelPeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	new PanelAdapter(jenv, jpeer, jparent);
	ObjectAdapter::getAdapter<PanelAdapter>(jenv, jpeer);
}
