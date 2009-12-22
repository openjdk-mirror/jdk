#include "sun_awt_haiku_BComponentPeer.h"
#include "Color.h"
#include "ComponentAdapter.h"
#include "Font.h"
#include "KeyboardFocusManager.h"
#include <Looper.h>

/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    _hide
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer__1hide
  (JNIEnv * jenv, jobject jpeer)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->hide();
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    getLocationOnScreen
 * Signature: ()Ljava/awt/Point;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BComponentPeer_getLocationOnScreen
  (JNIEnv * jenv, jobject jpeer)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return NULL;
	}
	BPoint point = adapter->getLocationOnScreen();
	jobject jpoint = NULL;
	jclass jklass = jenv->FindClass("java/awt/Point");
	if (jklass != NULL) {
		jmethodID jconstructor = jenv->GetMethodID(jklass, "<init>", "(II)V");
		if (jconstructor != NULL) {
			jpoint = jenv->NewObject(jklass, jconstructor, (int)point.x, (int)point.y);
		}
		jenv->DeleteLocalRef(jklass);
	}
	return jpoint;
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    _show
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer__1show
  (JNIEnv * jenv, jobject jpeer)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->show();
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    enable
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer_enable
  (JNIEnv * jenv, jobject jpeer)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->enable();
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    disable
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer_disable
  (JNIEnv * jenv, jobject jpeer)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->disable();
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    updateWindow
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer_updateWindow
  (JNIEnv * jenv, jobject jpeer)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->updateWindow();
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    _reshape
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer__1reshape
  (JNIEnv * jenv, jobject jpeer, jint x, jint y, jint width, jint height)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->reshape(x, y, width, height);
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    nativeHandleEvent
 * Signature: (Ljava/awt/AWTEvent;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer_nativeHandleEvent
  (JNIEnv * jenv, jobject jpeer, jobject jevent)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->nativeHandleEvent(jevent);
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    _dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer__1dispose
  (JNIEnv * jenv, jobject jpeer)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->dispose();
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    _setForeground
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer__1setForeground
  (JNIEnv * jenv, jobject jpeer, jint jcolor)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	rgb_color color = Color::GetColor(jcolor);
	adapter->setForeground(color);
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    _setBackground
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer__1setBackground
  (JNIEnv * jenv, jobject jpeer, jint jcolor)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	rgb_color color = Color::GetColor(jcolor);
	adapter->setBackground(color);
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    _setFont
 * Signature: (Ljava/awt/Font;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer__1setFont
  (JNIEnv * jenv, jobject jpeer, jobject jfont)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	BFont font = Font::GetFont(jenv, jfont);
	adapter->setFont(font);
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    processSynchronousLightweightTransfer
 * Signature: (Ljava/awt/Component;Ljava/awt/Component;ZZJ)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_haiku_BComponentPeer_processSynchronousLightweightTransfer
  (JNIEnv * jenv, jclass jklass, jobject jheavyweight, jobject jdescendent, 
   jboolean temporary, jboolean focusedWindowChangeAllowed, jlong time)
{
	return KeyboardFocusManager::GetInstance()->processSynchronousLightweightTransfer(
			jenv, jheavyweight, jdescendent, temporary, focusedWindowChangeAllowed, time);
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    _requestFocus
 * Signature: (Ljava/awt/Component;ZZJ)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_haiku_BComponentPeer__1requestFocus
  (JNIEnv * jenv, jobject jpeer, jobject lightweightChild, 
   jboolean temporary, jboolean focusedWindowChangeAllowed, jlong time)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return false;
	}

	// Package up the parameters and send a BMessage to the ComponentAdapter
	BMessage *msg = new BMessage(AWT_FOCUS_REQUESTED);
	msg->AddPointer("lightweightChild", jenv->NewGlobalRef(lightweightChild));
	msg->AddBool("temporary", (temporary == JNI_TRUE));
	msg->AddBool("focusedWindowChangeAllowed", (focusedWindowChangeAllowed == JNI_TRUE));
	msg->AddInt32("when", (int32)time);
	if (adapter->Looper() == NULL) {
		fprintf(stderr, "   No looper to send focus message to?!?\n");
		DEBUGGER();
	} else {
		adapter->Looper()->PostMessage(msg, adapter);
	}
	// Make sure we delete the GlobalRef for leightweightChild in ComponentAdapter... or else we leak.
	delete msg;
	return true;
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    setZOrderPosition
 * Signature: (Lsun/awt/haiku/BComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer_setZOrderPosition
  (JNIEnv * jenv, jobject jpeer, jobject jcomponentAbove)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
//	Andrew/Bryan: can't implement with our API
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    _addDropTarget
 * Signature: (Ljava/awt/dnd/DropTarget;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer__1addDropTarget
  (JNIEnv * jenv, jobject jpeer, jobject jdropTarget)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->addDropTarget(jdropTarget);
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    _removeDropTarget
 * Signature: (Ljava/awt/dnd/DropTarget;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BComponentPeer__1removeDropTarget
  (JNIEnv * jenv, jobject jpeer, jobject jdropTarget)
{
	ComponentAdapter * adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->removeDropTarget(jdropTarget);
}


/*
 * Class:     sun_awt_haiku_BComponentPeer
 * Method:    nativeHandlesWheelScrolling
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_haiku_BComponentPeer_nativeHandlesWheelScrolling
  (JNIEnv * jenv, jobject jpeer)
{
	// Yes, Haiku should handle this.
	return true;
}


