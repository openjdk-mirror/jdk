#include "sun_awt_haiku_BLabelPeer.h"
#include "LabelAdapter.h"
#include "java_awt_Label.h"
#include "Dimension.h"

/*
 * Class:     sun_awt_haiku_BLabelPeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BLabelPeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	new LabelAdapter(jenv, jpeer, jparent);
	ObjectAdapter::getAdapter<LabelAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BLabelPeer
 * Method:    _setText
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BLabelPeer__1setText
  (JNIEnv * jenv, jobject jpeer, jstring jtext)
{
	LabelAdapter * adapter = ObjectAdapter::getAdapter<LabelAdapter>(jenv, jpeer);
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
 * Class:     sun_awt_haiku_BLabelPeer
 * Method:    _setAlignment
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BLabelPeer__1setAlignment
  (JNIEnv * jenv, jobject jpeer, jint jalignment)
{
	LabelAdapter * adapter = ObjectAdapter::getAdapter<LabelAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	alignment flag;
	switch (jalignment) {
		case java_awt_Label_LEFT:   flag = B_ALIGN_LEFT;   break;
		case java_awt_Label_CENTER: flag = B_ALIGN_CENTER; break;
		case java_awt_Label_RIGHT:  flag = B_ALIGN_RIGHT;  break;
		default:
			// incomprehensible alignment
			return;
	}
	adapter->setAlignment(flag);
}


/*
 * Class:     sun_awt_haiku_BLabelPeer
 * Method:    _getMinimumSize
 * Signature: (Ljava/awt/Font;Ljava/lang/String;)Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BLabelPeer__1getMinimumSize
  (JNIEnv * jenv, jobject jpeer, jobject jfont, jstring jtext)
{
	LabelAdapter * adapter = ObjectAdapter::getAdapter<LabelAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getMinimumSize();
	return Dimension::New(jenv, size.x, size.y);
}
