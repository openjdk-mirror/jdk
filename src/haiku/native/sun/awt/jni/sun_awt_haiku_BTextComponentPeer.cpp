#include "sun_awt_haiku_BTextComponentPeer.h"
#include "TextComponentAdapter.h"
#include "Rectangle.h"

/*
 * Class:     sun_awt_haiku_BTextComponentPeer
 * Method:    _setEditable
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BTextComponentPeer__1setEditable
  (JNIEnv * jenv, jobject jpeer, jboolean editable)
{
	TextComponentAdapter * adapter = ObjectAdapter::getAdapter<TextComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setEditable(editable);
}


/*
 * Class:     sun_awt_haiku_BTextComponentPeer
 * Method:    _getText
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_awt_haiku_BTextComponentPeer__1getText
  (JNIEnv * jenv, jobject jpeer)
{
	TextComponentAdapter * adapter = ObjectAdapter::getAdapter<TextComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	const char * text = adapter->getText();
	jstring jtext = jenv->NewStringUTF(text);
	return jtext;
}


/*
 * Class:     sun_awt_haiku_BTextComponentPeer
 * Method:    _setText
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BTextComponentPeer__1setText
  (JNIEnv * jenv, jobject jpeer, jstring jtext)
{
	TextComponentAdapter * adapter = ObjectAdapter::getAdapter<TextComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	const char * text = UseString(jenv, jtext);
	if (text != NULL) {
		adapter->setText((char*)text);
		ReleaseString(jenv, jtext, text);
	} else {
		adapter->setText("");
	}
}


/*
 * Class:     sun_awt_haiku_BTextComponentPeer
 * Method:    _getSelectionStart
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BTextComponentPeer__1getSelectionStart
  (JNIEnv * jenv, jobject jpeer)
{
	TextComponentAdapter * adapter = ObjectAdapter::getAdapter<TextComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return -1;
	}
	return adapter->getSelectionStart();
}


/*
 * Class:     sun_awt_haiku_BTextComponentPeer
 * Method:    _getSelectionEnd
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BTextComponentPeer__1getSelectionEnd
  (JNIEnv * jenv, jobject jpeer)
{
	TextComponentAdapter * adapter = ObjectAdapter::getAdapter<TextComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return -1;
	}
	return adapter->getSelectionEnd();
}


/*
 * Class:     sun_awt_haiku_BTextComponentPeer
 * Method:    _select
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BTextComponentPeer__1select
  (JNIEnv * jenv, jobject jpeer, jint selStart, jint selEnd)
{
	TextComponentAdapter * adapter = ObjectAdapter::getAdapter<TextComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->select(selStart, selEnd);
}


/*
 * Class:     sun_awt_haiku_BTextComponentPeer
 * Method:    _getCaretPosition
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BTextComponentPeer__1getCaretPosition
  (JNIEnv * jenv, jobject jpeer)
{
	TextComponentAdapter * adapter = ObjectAdapter::getAdapter<TextComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return -1;
	}
	return adapter->getCaretPosition();
}


/*
 * Class:     sun_awt_haiku_BTextComponentPeer
 * Method:    _getIndexAtPoint
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BTextComponentPeer__1getIndexAtPoint
  (JNIEnv * jenv, jobject jpeer, jint x, jint y)
{
	TextComponentAdapter * adapter = ObjectAdapter::getAdapter<TextComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return -1;
	}
	return adapter->getIndexAtPoint(x, y);
}


/*
 * Class:     sun_awt_haiku_BTextComponentPeer
 * Method:    _getCharacterBounds
 * Signature: (ILjava/awt/Rectangle;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BTextComponentPeer__1getCharacterBounds
  (JNIEnv * jenv, jobject jpeer, jint i, jobject jrect)
{
	TextComponentAdapter * adapter = ObjectAdapter::getAdapter<TextComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	BRect bounds = adapter->getCharacterBounds(i);
	DASSERT(jrect != NULL);
	jenv->SetIntField(jrect, Rectangle::x_ID, bounds.left);
	jenv->SetIntField(jrect, Rectangle::y_ID, bounds.top);
	jenv->SetIntField(jrect, Rectangle::width_ID, bounds.Width());
	jenv->SetIntField(jrect, Rectangle::height_ID, bounds.Height());
}
