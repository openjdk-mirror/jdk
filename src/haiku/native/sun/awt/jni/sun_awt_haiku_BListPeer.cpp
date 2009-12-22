#include "sun_awt_haiku_BListPeer.h"
#include "ListAdapter.h"
#include "Dimension.h"

/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    initIDs
 * Signature: (Ljava/lang/Class;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BListPeer_initIDs
  (JNIEnv * jenv, jclass jListPeerClass, jclass jListClass)
{
	ListAdapter::rows_ID         = jenv->GetFieldID(jListClass, "rows", "I");
	ListAdapter::multipleMode_ID = jenv->GetFieldID(jListClass, "multipleMode", "Z");
	ListAdapter::visibleIndex_ID = jenv->GetFieldID(jListClass, "visibleIndex", "I");
	DASSERT(ListAdapter::rows_ID         != NULL);
	DASSERT(ListAdapter::multipleMode_ID != NULL);
	DASSERT(ListAdapter::visibleIndex_ID != NULL);
}


/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BListPeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	new ListAdapter(jenv, jpeer, jparent);
	ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _initialize
 * Signature: ([Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BListPeer__1initialize
  (JNIEnv * jenv, jobject jpeer, jobjectArray jitems)
{
	ListAdapter * adapter = ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
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
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _getSelectedIndexes
 * Signature: ()[I
 */
JNIEXPORT jintArray JNICALL Java_sun_awt_haiku_BListPeer__1getSelectedIndexes
  (JNIEnv * jenv, jobject jpeer)
{
	ListAdapter * adapter = ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	std::vector<int> indexes = adapter->getSelectedIndexes();
    jintArray jindexes = jenv->NewIntArray(indexes.size());
    jint * cindexes = jenv->GetIntArrayElements(jindexes, 0);
    for (unsigned i = 0 ; i < indexes.size() ; i++) {
		cindexes[i] = indexes[i];
    }
	jenv->ReleaseIntArrayElements(jindexes, cindexes, 0);
	return jindexes;
}


/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _add
 * Signature: (Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BListPeer__1add
  (JNIEnv * jenv, jobject jpeer, jstring jitem, jint index)
{
	ListAdapter * adapter = ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	const char * item = UseString(jenv, jitem);
	adapter->add((char*)item, index);
	ReleaseString(jenv, jitem, item);
}


/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _delItems
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BListPeer__1delItems
  (JNIEnv * jenv, jobject jpeer, jint start, jint end)
{
	ListAdapter * adapter = ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->delItems(start, end);
}


/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _removeAll
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BListPeer__1removeAll
  (JNIEnv * jenv, jobject jpeer)
{
	ListAdapter * adapter = ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->removeAll();
}


/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _select
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BListPeer__1select
  (JNIEnv * jenv, jobject jpeer, jint index)
{
	ListAdapter * adapter = ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->select(index);
}


/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _deselect
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BListPeer__1deselect
  (JNIEnv * jenv, jobject jpeer, jint index)
{
	ListAdapter * adapter = ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->deselect(index);
}


/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _makeVisible
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BListPeer__1makeVisible
  (JNIEnv * jenv, jobject jpeer, jint index)
{
	ListAdapter * adapter = ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->makeVisible(index);
}


/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _setMultipleMode
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BListPeer__1setMultipleMode
  (JNIEnv * jenv, jobject jpeer, jboolean b)
{
	ListAdapter * adapter = ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setMultipleMode(b);
}


/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _getPreferredSize
 * Signature: (I)Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BListPeer__1getPreferredSize__I
  (JNIEnv * jenv, jobject jpeer, jint rows)
{
	ListAdapter * adapter = ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getPreferredSize(rows);
	return Dimension::New(jenv, size.x, size.y);
}


/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _getMinimumSize
 * Signature: (I)Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BListPeer__1getMinimumSize__I
  (JNIEnv * jenv, jobject jpeer, jint rows)
{
	ListAdapter * adapter = ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getMinimumSize(rows);
	return Dimension::New(jenv, size.x, size.y);
}


/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _getPreferredSize
 * Signature: ()Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BListPeer__1getPreferredSize__
  (JNIEnv * jenv, jobject jpeer)
{
	ListAdapter * adapter = ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getPreferredSize();
	return Dimension::New(jenv, size.x, size.y);
}


/*
 * Class:     sun_awt_haiku_BListPeer
 * Method:    _getMinimumSize
 * Signature: ()Ljava/awt/Dimension;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BListPeer__1getMinimumSize__
  (JNIEnv * jenv, jobject jpeer)
{
	ListAdapter * adapter = ObjectAdapter::getAdapter<ListAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint size = adapter->getMinimumSize();
	return Dimension::New(jenv, size.x, size.y);
}

