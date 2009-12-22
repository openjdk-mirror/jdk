#include "sun_awt_haiku_BMenuBarPeer.h"
#include "MenuBarAdapter.h"
#include "MenuAdapter.h"

/*
 * Class:     sun_awt_haiku_BMenuBarPeer
 * Method:    _createMenuBar
 * Signature: (Lsun/awt/haiku/BObjectPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BMenuBarPeer__1createMenuBar
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BMenuBar * menubar = MenuBarAdapter::NewMenuBar(jenv, jpeer, jparent);
	new MenuBarAdapter(jenv, jpeer, jparent, menubar);
	ObjectAdapter::getAdapter<MenuBarAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BMenuBarPeer
 * Method:    _addMenu
 * Signature: (Ljava/awt/peer/MenuPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BMenuBarPeer__1addMenu
  (JNIEnv * jenv, jobject jpeer, jobject jmenu)
{
	MenuBarAdapter * adapter = ObjectAdapter::getAdapter<MenuBarAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	MenuAdapter * adapter2 = ObjectAdapter::getAdapter<MenuAdapter>(jenv, jmenu);
	if (adapter2 == NULL) {
		return;
	}
	adapter->addMenu(adapter2->Menu());
}


/*
 * Class:     sun_awt_haiku_BMenuBarPeer
 * Method:    _delMenu
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BMenuBarPeer__1delMenu
  (JNIEnv * jenv, jobject jpeer, jint index)
{
	MenuBarAdapter * adapter = ObjectAdapter::getAdapter<MenuBarAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->delMenu(index);
}


/*
 * Class:     sun_awt_haiku_BMenuBarPeer
 * Method:    _addHelpMenu
 * Signature: (Ljava/awt/peer/MenuPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BMenuBarPeer__1addHelpMenu
  (JNIEnv * jenv, jobject jpeer, jobject jmenu)
{
	MenuBarAdapter * adapter = ObjectAdapter::getAdapter<MenuBarAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	MenuAdapter * adapter2 = ObjectAdapter::getAdapter<MenuAdapter>(jenv, jmenu);
	if (adapter2 == NULL) {
		return;
	}
	adapter->addHelpMenu(adapter2->Menu());
}


