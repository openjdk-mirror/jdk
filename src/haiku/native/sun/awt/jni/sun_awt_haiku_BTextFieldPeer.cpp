#include "sun_awt_haiku_BTextFieldPeer.h"
#include "TextFieldAdapter.h"
#include "Dimension.h"

/*
 * Class:     sun_awt_haiku_BTextFieldPeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BTextFieldPeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	new TextFieldAdapter(jenv, jpeer, jparent);
	ObjectAdapter::getAdapter<TextFieldAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BTextFieldPeer
 * Method:    _getPreferredSize
 * Signature: (I)Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BTextFieldPeer__1getPreferredSize__I
  (JNIEnv * jenv, jobject jpeer, jint columns)
{
	TextFieldAdapter * adapter = ObjectAdapter::getAdapter<TextFieldAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getPreferredSize(columns);
	return Dimension::New(jenv, size.x, size.y);
}


/*
 * Class:     sun_awt_haiku_BTextFieldPeer
 * Method:    _getMinimumSize
 * Signature: (I)Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BTextFieldPeer__1getMinimumSize__I
  (JNIEnv * jenv, jobject jpeer, jint columns)
{
	TextFieldAdapter * adapter = ObjectAdapter::getAdapter<TextFieldAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getMinimumSize(columns);
	return Dimension::New(jenv, size.x, size.y);
}


/*
 * Class:     sun_awt_haiku_BTextFieldPeer
 * Method:    _getPreferredSize
 * Signature: ()Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BTextFieldPeer__1getPreferredSize__
  (JNIEnv * jenv, jobject jpeer)
{
	TextFieldAdapter * adapter = ObjectAdapter::getAdapter<TextFieldAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getPreferredSize();
	return Dimension::New(jenv, size.x, size.y);
}


/*
 * Class:     sun_awt_haiku_BTextFieldPeer
 * Method:    _getMinimumSize
 * Signature: ()Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BTextFieldPeer__1getMinimumSize__
  (JNIEnv * jenv, jobject jpeer)
{
	TextFieldAdapter * adapter = ObjectAdapter::getAdapter<TextFieldAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getMinimumSize();
	return Dimension::New(jenv, size.x, size.y);
}

