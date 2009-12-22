#include "sun_awt_haiku_BCheckboxPeer.h"
#include "CheckboxAdapter.h"
#include "Font.h"
#include "Dimension.h"

jclass   CheckboxAdapter::checkboxGroup_ID    = NULL;
jfieldID CheckboxAdapter::selectedCheckbox_ID = NULL;

/*
 * Class:     sun_awt_haiku_BCheckboxPeer
 * Method:    initIDs
 * Signature: (Ljava/lang/Class;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BCheckboxPeer_initIDs
  (JNIEnv * jenv, jclass jCheckboxPeer, jclass jCheckboxGroup)
{
	CheckboxAdapter::checkboxGroup_ID = (jclass)jenv->NewGlobalRef(jCheckboxGroup);
	CheckboxAdapter::selectedCheckbox_ID = jenv->GetFieldID(jCheckboxGroup, "selectedCheckbox", "Ljava/awt/Checkbox;");
	DASSERT(CheckboxAdapter::checkboxGroup_ID    != NULL);
	DASSERT(CheckboxAdapter::selectedCheckbox_ID != NULL);
}

/*
 * Class:     sun_awt_haiku_BCheckboxPeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BCheckboxPeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	new CheckboxAdapter(jenv, jpeer, jparent);
	ObjectAdapter::getAdapter<CheckboxAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BCheckboxPeer
 * Method:    _setState
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BCheckboxPeer__1setState
  (JNIEnv * jenv, jobject jpeer, jboolean state)
{
	CheckboxAdapter * adapter = ObjectAdapter::getAdapter<CheckboxAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setState(state);
}


/*
 * Class:     sun_awt_haiku_BCheckboxPeer
 * Method:    _setCheckboxGroup
 * Signature: (Ljava/awt/CheckboxGroup;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BCheckboxPeer__1setCheckboxGroup
  (JNIEnv * jenv, jobject jpeer, jobject jg)
{
	CheckboxAdapter * adapter = ObjectAdapter::getAdapter<CheckboxAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setCheckboxGroup(jenv, jg);
}


/*
 * Class:     sun_awt_haiku_BCheckboxPeer
 * Method:    _setLabel
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BCheckboxPeer__1setLabel
  (JNIEnv * jenv, jobject jpeer, jstring jlabel)
{
	CheckboxAdapter * adapter = ObjectAdapter::getAdapter<CheckboxAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	const char * label = UseString(jenv, jlabel);
	adapter->setLabel(label);
	ReleaseString(jenv, jlabel, label);
}


/*
 * Class:     sun_awt_haiku_BCheckboxPeer
 * Method:    _getMinimumSize
 * Signature: ()Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BCheckboxPeer__1getMinimumSize
  (JNIEnv * jenv, jobject jpeer)
{
	CheckboxAdapter * adapter = ObjectAdapter::getAdapter<CheckboxAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getMinimumSize();
	return Dimension::New(jenv, size.x, size.y);
}
