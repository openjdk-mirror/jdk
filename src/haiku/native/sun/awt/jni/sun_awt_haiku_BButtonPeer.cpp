#include "sun_awt_haiku_BButtonPeer.h"
#include "ButtonAdapter.h"
#include "Font.h"
#include "Dimension.h"

/*
 * Class:     sun_awt_haiku_BButtonPeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BButtonPeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	new ButtonAdapter(jenv, jpeer, jparent);
	ObjectAdapter::getAdapter<ButtonAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BButtonPeer
 * Method:    _setLabel
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BButtonPeer__1setLabel
  (JNIEnv * jenv, jobject jpeer, jstring jlabel)
{
	ButtonAdapter * adapter = ObjectAdapter::getAdapter<ButtonAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	const char * label = UseString(jenv, jlabel);
	adapter->setLabel((char*)label);
	ReleaseString(jenv, jlabel, label);
}


/*
 * Class:     sun_awt_haiku_BButtonPeer
 * Method:    _getMinimumSize
 * Signature: ()Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BButtonPeer__1getMinimumSize
  (JNIEnv * jenv, jobject jpeer)
{
	ButtonAdapter * adapter = ObjectAdapter::getAdapter<ButtonAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getMinimumSize();
	return Dimension::New(jenv, size.x, size.y);
}
