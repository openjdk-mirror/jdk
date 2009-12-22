#include "sun_awt_haiku_BMenuComponentPeer.h"
#include "MenuComponentAdapter.h"

/*
 * Class:     sun_awt_haiku_BMenuComponentPeer
 * Method:    _dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BMenuComponentPeer__1dispose
  (JNIEnv * jenv, jobject jpeer)
{
	MenuComponentAdapter * adapter = ObjectAdapter::getAdapter<MenuComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->dispose();
}
