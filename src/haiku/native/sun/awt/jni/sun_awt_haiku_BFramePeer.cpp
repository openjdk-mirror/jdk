#include "sun_awt_haiku_BFramePeer.h"
#include "FrameAdapter.h"
#include "MenuBarAdapter.h"

/*
 * Class:     sun_awt_haiku_BFramePeer
 * Method:    createAwtFrame
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BFramePeer_createAwtFrame
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BWindow * window = FrameAdapter::NewFrame(jenv, jpeer, jparent);
	new FrameAdapter(jenv, jpeer, jparent, window);
	ObjectAdapter::getAdapter<FrameAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BFramePeer
 * Method:    _setMenuBar
 * Signature: (Ljava/awt/peer/MenuBarPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BFramePeer__1setMenuBar
  (JNIEnv * jenv, jobject jpeer, jobject jmenubar)
{
	FrameAdapter * adapter = ObjectAdapter::getAdapter<FrameAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	if (jmenubar == NULL) {
		adapter->setMenuBar(NULL);
		return;
	}
	MenuBarAdapter * adapter2 = ObjectAdapter::getAdapter<MenuBarAdapter>(jenv, jmenubar);
	if (adapter2 == NULL) {
		return;
	}
	adapter->setMenuBar(adapter2->MenuBar());
}


/*
 * Class:     sun_awt_haiku_BFramePeer
 * Method:    _setState
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BFramePeer__1setState
  (JNIEnv * jenv, jobject jpeer, jint state)
{
	FrameAdapter * adapter = ObjectAdapter::getAdapter<FrameAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setState(state);
}


/*
 * Class:     sun_awt_haiku_BFramePeer
 * Method:    _getState
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BFramePeer__1getState
  (JNIEnv * jenv, jobject jpeer)
{
	FrameAdapter * adapter = ObjectAdapter::getAdapter<FrameAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return 0;
	}
	return adapter->getState();
}


/*
 * Class:     sun_awt_haiku_BFramePeer
 * Method:    _setMaximizedBounds
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BFramePeer__1setMaximizedBounds
  (JNIEnv * jenv, jobject jpeer, jint x, jint y, jint w, jint h)
{
	FrameAdapter * adapter = ObjectAdapter::getAdapter<FrameAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setMaximizedBounds(x, y, w, h);
}


/*
 * Class:     sun_awt_haiku_BFramePeer
 * Method:    _clearMaximizedBounds
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BFramePeer__1clearMaximizedBounds
  (JNIEnv * jenv, jobject jpeer)
{
	FrameAdapter * adapter = ObjectAdapter::getAdapter<FrameAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->clearMaximizedBounds();
}


