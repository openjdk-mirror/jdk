#include "sun_awt_haiku_BTextAreaPeer.h"
#include "TextAreaAdapter.h"
#include "Dimension.h"

/*
 * Class:     sun_awt_haiku_BTextAreaPeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BTextAreaPeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	new TextAreaAdapter(jenv, jpeer, jparent);
	ObjectAdapter::getAdapter<TextAreaAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BTextAreaPeer
 * Method:    _insert
 * Signature: (Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BTextAreaPeer__1insert
  (JNIEnv * jenv, jobject jpeer, jstring jtext, jint pos)
{
	TextAreaAdapter * adapter = ObjectAdapter::getAdapter<TextAreaAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	const char * text = UseString(jenv, jtext);
	adapter->insert((char*)text, pos);
	ReleaseString(jenv, jtext, text);
}


/*
 * Class:     sun_awt_haiku_BTextAreaPeer
 * Method:    _replaceRange
 * Signature: (Ljava/lang/String;II)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BTextAreaPeer__1replaceRange
  (JNIEnv * jenv, jobject jpeer, jstring jtext, jint start, jint end)
{
	TextAreaAdapter * adapter = ObjectAdapter::getAdapter<TextAreaAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	const char * text = UseString(jenv, jtext);
	adapter->replaceRange((char*)text, start, end);
	ReleaseString(jenv, jtext, text);
}


/*
 * Class:     sun_awt_haiku_BTextAreaPeer
 * Method:    _getPreferredSize
 * Signature: (II)Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BTextAreaPeer__1getPreferredSize__II
  (JNIEnv * jenv, jobject jpeer, jint rows, jint columns)
{
	TextAreaAdapter * adapter = ObjectAdapter::getAdapter<TextAreaAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getPreferredSize(rows, columns);
	return Dimension::New(jenv, size.x, size.y);
}


/*
 * Class:     sun_awt_haiku_BTextAreaPeer
 * Method:    _getMinimumSize
 * Signature: (II)Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BTextAreaPeer__1getMinimumSize__II
  (JNIEnv * jenv, jobject jpeer, jint rows, jint columns)
{
	TextAreaAdapter * adapter = ObjectAdapter::getAdapter<TextAreaAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getMinimumSize(rows, columns);
	return Dimension::New(jenv, size.x, size.y);
}


/*
 * Class:     sun_awt_haiku_BTextAreaPeer
 * Method:    _getPreferredSize
 * Signature: ()Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BTextAreaPeer__1getPreferredSize__
  (JNIEnv * jenv, jobject jpeer)
{
	TextAreaAdapter * adapter = ObjectAdapter::getAdapter<TextAreaAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getPreferredSize();
	return Dimension::New(jenv, size.x, size.y);
}


/*
 * Class:     sun_awt_haiku_BTextAreaPeer
 * Method:    _getMinimumSize
 * Signature: ()Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BTextAreaPeer__1getMinimumSize__
  (JNIEnv * jenv, jobject jpeer)
{
	TextAreaAdapter * adapter = ObjectAdapter::getAdapter<TextAreaAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getMinimumSize();
	return Dimension::New(jenv, size.x, size.y);
}

