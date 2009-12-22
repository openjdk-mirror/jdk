#include "sun_awt_haiku_BChoicePeer.h"
#include "ChoiceAdapter.h"
#include "Dimension.h"

/*
 * Class:     sun_awt_haiku_BChoicePeer
 * Method:    initIDs
 * Signature: (Ljava/lang/Class;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BChoicePeer_initIDs
  (JNIEnv * jenv, jclass jChoicePeerClass, jclass jChoiceClass)
{
	ChoiceAdapter::selectedIndex_ID = jenv->GetFieldID(jChoiceClass, "selectedIndex", "I");
	DASSERT(ChoiceAdapter::selectedIndex_ID != NULL);
}


/*
 * Class:     sun_awt_haiku_BChoicePeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BChoicePeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	new ChoiceAdapter(jenv, jpeer, jparent);
	ObjectAdapter::getAdapter<ChoiceAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BChoicePeer
 * Method:    _initialize
 * Signature: ([Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BChoicePeer__1initialize
  (JNIEnv * jenv, jobject jpeer, jobjectArray jitems)
{
	ChoiceAdapter * adapter = ObjectAdapter::getAdapter<ChoiceAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	jsize itemCount = jenv->GetArrayLength(jitems);
	for (int i = 0 ; i < itemCount ; i++) {
		jstring jitem = (jstring)jenv->GetObjectArrayElement(jitems, i);
		if ((jitem == NULL) || (jenv->ExceptionOccurred() != NULL)) {
			break;
		}
		const char * item = UseString(jenv, jitem);
		adapter->add((char*)item, i);
		ReleaseString(jenv, jitem, item);
	}
}


/*
 * Class:     sun_awt_haiku_BChoicePeer
 * Method:    _add
 * Signature: (Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BChoicePeer__1add
  (JNIEnv * jenv, jobject jpeer, jstring jitem, jint index)
{
	ChoiceAdapter * adapter = ObjectAdapter::getAdapter<ChoiceAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	const char * item = UseString(jenv, jitem);
	adapter->add((char*)item, index);
	ReleaseString(jenv, jitem, item);
}


/*
 * Class:     sun_awt_haiku_BChoicePeer
 * Method:    _remove
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BChoicePeer__1remove
  (JNIEnv * jenv, jobject jpeer, jint index)
{
	ChoiceAdapter * adapter = ObjectAdapter::getAdapter<ChoiceAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->remove(index);
}


/*
 * Class:     sun_awt_haiku_BChoicePeer
 * Method:    _removeAll
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BChoicePeer__1removeAll
  (JNIEnv * jenv, jobject jpeer)
{
	ChoiceAdapter * adapter = ObjectAdapter::getAdapter<ChoiceAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->removeAll();
}


/*
 * Class:     sun_awt_haiku_BChoicePeer
 * Method:    _select
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BChoicePeer__1select
  (JNIEnv * jenv, jobject jpeer, jint index)
{
	ChoiceAdapter * adapter = ObjectAdapter::getAdapter<ChoiceAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->select(index);
}


/*
 * Class:     sun_awt_haiku_BChoicePeer
 * Method:    _getMinimumSize
 * Signature: ()Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BChoicePeer__1getMinimumSize
  (JNIEnv * jenv, jobject jpeer)
{
	ChoiceAdapter * adapter = ObjectAdapter::getAdapter<ChoiceAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getMinimumSize();
	return Dimension::New(jenv, size.x, size.y);
}
