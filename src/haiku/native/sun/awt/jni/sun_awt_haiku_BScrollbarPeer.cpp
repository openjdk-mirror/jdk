#include "sun_awt_haiku_BScrollbarPeer.h"
#include "ScrollbarAdapter.h"

/*
 * Class:     sun_awt_haiku_BScrollbarPeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BScrollbarPeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	new ScrollbarAdapter(jenv, jpeer, jparent);
	ObjectAdapter::getAdapter<ScrollbarAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BScrollbarPeer
 * Method:    _setValues
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BScrollbarPeer__1setValues
  (JNIEnv * jenv, jobject jpeer, jint value, jint visible, jint minimum, jint maximum)
{
	ScrollbarAdapter * adapter = ObjectAdapter::getAdapter<ScrollbarAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setValues(value, visible, minimum, maximum);
}


/*
 * Class:     sun_awt_haiku_BScrollbarPeer
 * Method:    _setLineIncrement
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BScrollbarPeer__1setLineIncrement
  (JNIEnv * jenv, jobject jpeer, jint l)
{
	ScrollbarAdapter * adapter = ObjectAdapter::getAdapter<ScrollbarAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setLineIncrement(l);
}


/*
 * Class:     sun_awt_haiku_BScrollbarPeer
 * Method:    _setPageIncrement
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BScrollbarPeer__1setPageIncrement
  (JNIEnv * jenv, jobject jpeer, jint l)
{
	ScrollbarAdapter * adapter = ObjectAdapter::getAdapter<ScrollbarAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setPageIncrement(l);
}

