#include "sun_awt_haiku_BWindowPeer.h"
#include "WindowAdapter.h"

/*
 * Class:     sun_awt_haiku_BWindowPeer
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BWindowPeer_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	// no IDs
}


/*
 * Class:     sun_awt_haiku_BWindowPeer
 * Method:    createAwtWindow
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BWindowPeer_createAwtWindow
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BWindow * window = WindowAdapter::NewWindow(jenv, jpeer, jparent);
	new WindowAdapter(jenv, jpeer, jparent, window);
	ObjectAdapter::getAdapter<WindowAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BWindowPeer
 * Method:    _toFront
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BWindowPeer__1toFront
  (JNIEnv * jenv, jobject jpeer)
{
	WindowAdapter * adapter = ObjectAdapter::getAdapter<WindowAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->toFront();
}


/*
 * Class:     sun_awt_haiku_BWindowPeer
 * Method:    _toBack
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BWindowPeer__1toBack
  (JNIEnv * jenv, jobject jpeer)
{
	WindowAdapter * adapter = ObjectAdapter::getAdapter<WindowAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->toBack();
}


/*
 * Class:     sun_awt_haiku_BWindowPeer
 * Method:    _setResizable
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BWindowPeer__1setResizable
  (JNIEnv * jenv, jobject jpeer, jboolean resizable)
{
	WindowAdapter * adapter = ObjectAdapter::getAdapter<WindowAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setResizable(resizable);
}


/*
 * Class:     sun_awt_haiku_BWindowPeer
 * Method:    _setTitle
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BWindowPeer__1setTitle
  (JNIEnv * jenv, jobject jpeer, jstring jtitle)
{
	WindowAdapter * adapter = ObjectAdapter::getAdapter<WindowAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	const char * title = UseString(jenv, jtitle);
	if (title != NULL) {
		adapter->setTitle(title);
		ReleaseString(jenv, jtitle, title);
	} else {
		adapter->setTitle("");
	}
}


/*
 * Class:     sun_awt_haiku_BWindowPeer
 * Method:    _setUndecorated
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BWindowPeer__1setUndecorated
  (JNIEnv * jenv, jobject jpeer, jboolean undecorated)
{
	WindowAdapter * adapter = ObjectAdapter::getAdapter<WindowAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setUndecorated(undecorated);
}


/*
 * Class:     sun_awt_haiku_BWindowPeer
 * Method:    getSysMinWidth
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BWindowPeer_getSysMinWidth
  (JNIEnv * jenv, jclass jklass)
{
	return (jint)WindowAdapter::getMinimumWidth();
}


/*
 * Class:     sun_awt_haiku_BWindowPeer
 * Method:    getSysMinHeight
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BWindowPeer_getSysMinHeight
  (JNIEnv * jenv, jclass jklass)
{
	return (jint)WindowAdapter::getMinimumHeight();
}


/*
 * Class:     sun_awt_haiku_BWindowPeer
 * Method:    reshapeFrame
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BWindowPeer_reshapeFrame
  (JNIEnv * jenv, jobject jpeer, jint x, jint y, jint width, jint height)
{
	WindowAdapter * adapter = ObjectAdapter::getAdapter<WindowAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->reshapeFrame(x, y, width, height);
}


/*
 * Class:     sun_awt_haiku_BWindowPeer
 * Method:    getScreenImOn
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BWindowPeer_getScreenImOn
  (JNIEnv * jenv, jobject jpeer)
{
	WindowAdapter * adapter = ObjectAdapter::getAdapter<WindowAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return 0;
	}
	return (jint)adapter->getScreenImOn();
}

