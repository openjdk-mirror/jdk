#include "sun_awt_haiku_BPopupMenuPeer.h"
#include "PopupMenuAdapter.h"
#include "ComponentAdapter.h"

/*
 * Class:     sun_awt_haiku_BPopupMenuPeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BObjectPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BPopupMenuPeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	new PopupMenuAdapter(jenv, jpeer, jparent);
	ObjectAdapter::getAdapter<PopupMenuAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BPopupMenuPeer
 * Method:    _show
 * Signature: (Ljava/awt/peer/ComponentPeer;II)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BPopupMenuPeer__1show
  (JNIEnv * jenv, jobject jpeer, jobject jcomponent, jint x, jint y)
{
	PopupMenuAdapter * adapter = ObjectAdapter::getAdapter<PopupMenuAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	ComponentAdapter * adapter2 = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jcomponent);
	if (adapter2 == NULL) {
		return;
	}
	adapter->show(adapter2->GetView(), x, y);
}
