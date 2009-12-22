#include "sun_awt_haiku_BDialogPeer.h"
#include "DialogAdapter.h"

/*
 * Class:     sun_awt_haiku_BDialogPeer
 * Method:    createAwtDialog
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BDialogPeer_createAwtDialog
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BWindow * window = DialogAdapter::NewDialog(jenv, jpeer, jparent);
	new DialogAdapter(jenv, jpeer, jparent, window);
	ObjectAdapter::getAdapter<DialogAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BDialogPeer
 * Method:    _createFullSubset
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BDialogPeer__1createFullSubset
  (JNIEnv * jenv, jobject jpeer)
{
	DialogAdapter * adapter = ObjectAdapter::getAdapter<DialogAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->createFullSubset();
}


/*
 * Class:     sun_awt_haiku_BDialogPeer
 * Method:    _removeFromSubset
 * Signature: (Ljava/awt/peer/WindowPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BDialogPeer__1removeFromSubset
  (JNIEnv * jenv, jobject jpeer, jobject jwindow)
{
	DialogAdapter * adapter = ObjectAdapter::getAdapter<DialogAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	WindowAdapter * adapter2 = ObjectAdapter::getAdapter<WindowAdapter>(jenv, jwindow);
	if (adapter2 == NULL) {
		return;
	}
	adapter->removeFromSubset(adapter2->GetWindow());
}
