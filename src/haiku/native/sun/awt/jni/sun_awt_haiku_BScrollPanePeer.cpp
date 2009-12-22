#include "sun_awt_haiku_BScrollPanePeer.h"
#include "ScrollPaneAdapter.h"
#include "java_awt_Adjustable.h"

/*
 * Class:     sun_awt_haiku_BScrollPanePeer
 * Method:    _create
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BScrollPanePeer__1create
  (JNIEnv * jenv, jobject jpeer, jobject jparent)
{
	new ScrollPaneAdapter(jenv, jpeer, jparent);
	ObjectAdapter::getAdapter<ScrollPaneAdapter>(jenv, jpeer);
}


/*
 * Class:     sun_awt_haiku_BScrollPanePeer
 * Method:    _getHScrollbarHeight
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BScrollPanePeer__1getHScrollbarHeight
  (JNIEnv * jenv, jobject jpeer)
{
	ScrollPaneAdapter * adapter = ObjectAdapter::getAdapter<ScrollPaneAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return -1;
	}
	return adapter->getHScrollbarHeight();
}


/*
 * Class:     sun_awt_haiku_BScrollPanePeer
 * Method:    _getVScrollbarWidth
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BScrollPanePeer__1getVScrollbarWidth
  (JNIEnv * jenv, jobject jpeer)
{
	ScrollPaneAdapter * adapter = ObjectAdapter::getAdapter<ScrollPaneAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return -1;
	}
	return adapter->getVScrollbarWidth();
}


/*
 * Class:     sun_awt_haiku_BScrollPanePeer
 * Method:    _setScrollPosition
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BScrollPanePeer__1setScrollPosition
  (JNIEnv * jenv, jobject jpeer, jint x, jint y)
{
	ScrollPaneAdapter * adapter = ObjectAdapter::getAdapter<ScrollPaneAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->setScrollPosition(x, y);
}


/*
 * Class:     sun_awt_haiku_BScrollPanePeer
 * Method:    _childResized
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BScrollPanePeer__1childResized
  (JNIEnv * jenv, jobject jpeer, jint w, jint h)
{
	ScrollPaneAdapter * adapter = ObjectAdapter::getAdapter<ScrollPaneAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->childResized(w, h);
}


/*
 * Class:     sun_awt_haiku_BScrollPanePeer
 * Method:    _setIncrements
 * Signature: (Ljava/awt/Adjustable;II)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BScrollPanePeer__1setIncrements
  (JNIEnv * jenv, jobject jpeer, jint jorientation, jint u, jint b)
{
	ScrollPaneAdapter * adapter = ObjectAdapter::getAdapter<ScrollPaneAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	orientation adj = (jorientation == java_awt_Adjustable_HORIZONTAL ? B_HORIZONTAL : B_VERTICAL);
	adapter->setIncrements(adj, u, b);
}


/*
 * Class:     sun_awt_haiku_BScrollPanePeer
 * Method:    _setValue
 * Signature: (Ljava/awt/Adjustable;I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BScrollPanePeer__1setValue
  (JNIEnv * jenv, jobject jpeer, jint jorientation, jint v)
{
	ScrollPaneAdapter * adapter = ObjectAdapter::getAdapter<ScrollPaneAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	orientation adj = (jorientation == java_awt_Adjustable_HORIZONTAL ? B_HORIZONTAL : B_VERTICAL);
	adapter->setValue(adj, v);
}

