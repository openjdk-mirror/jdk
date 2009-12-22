#include "sun_awt_haiku_BMenuItemPeer.h"
#include "MenuItemAdapter.h"
#include "KeyConversions.h"
#include <support/String.h>
#include <interface/MenuItem.h>

/*
 * Class:     sun_awt_haiku_BMenuItemPeer
 * Method:    _createMenuItem
 * Signature: (Lsun/awt/haiku/BObjectPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BMenuItemPeer__1createMenuItem
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BMenuItem * menuitem = MenuItemAdapter::NewMenuItem(jenv, jpeer, jparent);
	new MenuItemAdapter(jenv, jpeer, jparent, menuitem);
	ObjectAdapter::getAdapter<MenuItemAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BMenuItemPeer
 * Method:    _createSeparator
 * Signature: (Lsun/awt/haiku/BObjectPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BMenuItemPeer__1createSeparator
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	BMenuItem * menuitem = new BSeparatorItem();
	new MenuItemAdapter(jenv, jpeer, jparent, menuitem);
	ObjectAdapter::getAdapter<MenuItemAdapter>(jenv, jpeer);
}

/*
 * Class:     sun_awt_haiku_BMenuItemPeer
 * Method:    _setShortcut
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BMenuItemPeer__1setShortcut
  (JNIEnv * jenv, jobject jpeer, jint jkeycode, jboolean shifted)
{
	MenuItemAdapter * adapter = ObjectAdapter::getAdapter<MenuItemAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	BString *str = new BString("");
	GetKeyChar(str, ConvertKeyCodeToNative(jkeycode), 0);
	adapter->setShortcut(str->ByteAt(0), shifted);
	delete str;
}

/*
 * Class:     sun_awt_haiku_BMenuItemPeer
 * Method:    _setLabel
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BMenuItemPeer__1setLabel
  (JNIEnv * jenv, jobject jpeer, jstring jlabel)
{
	MenuItemAdapter * adapter = ObjectAdapter::getAdapter<MenuItemAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	const char * label = UseString(jenv, jlabel);
	if (label != NULL) {
		adapter->setLabel((char*)label);
		ReleaseString(jenv, jlabel, label);
	} else {
		adapter->setLabel("");
	}
}

/*
 * Class:     sun_awt_haiku_BMenuItemPeer
 * Method:    _setEnabled
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BMenuItemPeer__1setEnabled
  (JNIEnv * jenv, jobject jpeer, jboolean enabled)
{
	MenuItemAdapter * adapter = ObjectAdapter::getAdapter<MenuItemAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setEnabled(enabled);
}
