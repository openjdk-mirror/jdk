#include "sun_awt_haiku_BCheckboxMenuItemPeer.h"
#include "CheckboxMenuItemAdapter.h"

/*
 * Class:     sun_awt_haiku_BCheckboxMenuItemPeer
 * Method:    _createCheckboxMenuItem
 * Signature: (Lsun/awt/haiku/BObjectPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BCheckboxMenuItemPeer__1createCheckboxMenuItem
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	new CheckboxMenuItemAdapter(jenv, jpeer, jparent);
	ObjectAdapter::getAdapter<CheckboxMenuItemAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BCheckboxMenuItemPeer
 * Method:    _setState
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BCheckboxMenuItemPeer__1setState
  (JNIEnv * jenv, jobject jpeer, jboolean enabled)
{
	CheckboxMenuItemAdapter * adapter = ObjectAdapter::getAdapter<CheckboxMenuItemAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setState(enabled);
}

