#include "sun_awt_haiku_BMenuPeer.h"
#include "MenuAdapter.h"
#include "MenuItemAdapter.h"

/*
 * Class:     sun_awt_haiku_BMenuPeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BObjectPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BMenuPeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BMenu * menu = MenuAdapter::NewMenu(jenv, jpeer, jparent);
	new MenuAdapter(jenv, jpeer, jparent, menu);
	ObjectAdapter::getAdapter<MenuAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BMenuPeer
 * Method:    _addItem
 * Signature: (Ljava/awt/peer/MenuItemPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BMenuPeer__1addItem
  (JNIEnv * jenv, jobject jpeer, jobject jmenuitem)
{
	MenuAdapter * adapter = ObjectAdapter::getAdapter<MenuAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	MenuItemAdapter * adapter2 = ObjectAdapter::getAdapter<MenuItemAdapter>(jenv, jmenuitem);
	if (adapter2 == NULL) {
		return;
	}
	adapter->addItem(adapter2->MenuItem());
}


/*
 * Class:     sun_awt_haiku_BMenuPeer
 * Method:    _delItem
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BMenuPeer__1delItem
  (JNIEnv * jenv, jobject jpeer, jint index)
{
	MenuAdapter * adapter = ObjectAdapter::getAdapter<MenuAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->delItem(index);
}

