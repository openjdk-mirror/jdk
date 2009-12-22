#include "KeyboardFocusManager.h"
#include "debug_util.h"
#include "ComponentAdapter.h"

#include <View.h>
#include <Window.h>

// #pragma mark -
//
// JNI
//

#include "java_awt_KeyboardFocusManager.h"
#include "KeyEvent.h"

// ID's Managed by initIDs();
jfieldID KeyboardFocusManager::focusOwner_ID = NULL;
jfieldID KeyboardFocusManager::permanentFocusOwner_ID = NULL;
jfieldID KeyboardFocusManager::focusedWindow_ID = NULL;
jfieldID KeyboardFocusManager::activeWindow_ID = NULL;

jmethodID KeyboardFocusManager::heavyweightButtonDown_ID = NULL;
jmethodID KeyboardFocusManager::shouldNativelyFocusHeavyweight_ID = NULL;
jmethodID KeyboardFocusManager::processSynchronousLightweightTransfer_ID = NULL;


/*
 * Class:     java_awt_KeyboardFocusManager
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_KeyboardFocusManager_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	KeyboardFocusManager::focusOwner_ID = jenv->GetStaticFieldID(jklass, "focusOwner", "Ljava/awt/Component;");
	KeyboardFocusManager::permanentFocusOwner_ID = jenv->GetStaticFieldID(jklass, "permanentFocusOwner", "Ljava/awt/Component;");
	KeyboardFocusManager::focusedWindow_ID = jenv->GetStaticFieldID(jklass, "focusedWindow", "Ljava/awt/Window;");
	KeyboardFocusManager::activeWindow_ID = jenv->GetStaticFieldID(jklass, "activeWindow", "Ljava/awt/Window;");
	DASSERT(KeyboardFocusManager::focusOwner_ID);
	DASSERT(KeyboardFocusManager::permanentFocusOwner_ID);
	DASSERT(KeyboardFocusManager::focusedWindow_ID);
	DASSERT(KeyboardFocusManager::activeWindow_ID);

	KeyboardFocusManager::heavyweightButtonDown_ID = jenv->GetStaticMethodID(jklass, "heavyweightButtonDown", "(Ljava/awt/Component;J)V");
	KeyboardFocusManager::shouldNativelyFocusHeavyweight_ID = jenv->GetStaticMethodID(jklass, "shouldNativelyFocusHeavyweight", "(Ljava/awt/Component;Ljava/awt/Component;ZZJ)I");
	KeyboardFocusManager::processSynchronousLightweightTransfer_ID = jenv->GetStaticMethodID(jklass, "processSynchronousLightweightTransfer", "(Ljava/awt/Component;Ljava/awt/Component;ZZJ)Z");
	DASSERT(KeyboardFocusManager::heavyweightButtonDown_ID);
	DASSERT(KeyboardFocusManager::shouldNativelyFocusHeavyweight_ID);
	DASSERT(KeyboardFocusManager::processSynchronousLightweightTransfer_ID);
}


/*
 * Class:     java_awt_KeyboardFocusManager
 * Method:    _clearGlobalFocusOwner
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_KeyboardFocusManager__1clearGlobalFocusOwner
  (JNIEnv * jenv, jobject self)
{
	// Remove focus from whatever component currently has it.
	
	/* After this operation completes, the native windowing
     * system will discard all user-generated KeyEvents until the user selects
     * a new Component to receive focus, or a Component is given focus
     * explicitly via a call to <code>requestFocus()</code>. This operation
     * does not change the focused or active Windows.
     */
	KeyboardFocusManager::GetInstance()->SetFocusedComponent(NULL);
}


/*
 * Class:     java_awt_KeyboardFocusManager
 * Method:    getNativeFocusOwner
 * Signature: ()Ljava/awt/Component;
 */
JNIEXPORT jobject JNICALL Java_java_awt_KeyboardFocusManager_getNativeFocusOwner
  (JNIEnv * jenv, jclass jklass)
{
	jobject peer = KeyboardFocusManager::GetInstance()->getFocusedComponentPeer();
	if (peer == NULL) {
		return NULL;
	}
	return jenv->GetObjectField(peer, ObjectAdapter::javaObject_ID);
}


/*
 * Class:     java_awt_KeyboardFocusManager
 * Method:    getNativeFocusedWindow
 * Signature: ()Ljava/awt/Window;
 */
JNIEXPORT jobject JNICALL Java_java_awt_KeyboardFocusManager_getNativeFocusedWindow
  (JNIEnv * jenv, jclass jklass)
{
	fprintf(stdout, "*** %s ***\n", __func__);
	return NULL;
}


/*
 * Class:     java_awt_KeyboardFocusManager
 * Method:    isProxyActiveImpl
 * Signature: (Ljava/awt/event/KeyEvent;)Z
 */
JNIEXPORT jboolean JNICALL Java_java_awt_KeyboardFocusManager_isProxyActiveImpl
  (JNIEnv * jenv, jclass jklass, jobject jkeyevent)
{
	// Return the value of KeyEvent.isProxyActive. It's a private member of 
	// KeyEvent, and it appears someone decided to bypass the protection with
	// JNI rather than fix the real issue.
	return jenv->GetBooleanField(jkeyevent, KeyEvent::isProxyActive_ID);
}

// #pragma mark -
//
// KeyboardFocusManager
//

KeyboardFocusManager* KeyboardFocusManager::fInstance = NULL;

KeyboardFocusManager::KeyboardFocusManager() {
	JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
	
	keyboardFocusManagerClass_ID = (jclass)env->NewGlobalRef(env->FindClass("java/awt/KeyboardFocusManager"));
	
	focusedPeer = NULL;
}


KeyboardFocusManager::~KeyboardFocusManager() {
	JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
	
	if (keyboardFocusManagerClass_ID != NULL) {
		env->DeleteGlobalRef(keyboardFocusManagerClass_ID);
	}
}


KeyboardFocusManager* 
KeyboardFocusManager::GetInstance()
{
	if (KeyboardFocusManager::fInstance == NULL) {
		KeyboardFocusManager::fInstance = new KeyboardFocusManager();
	}
	return KeyboardFocusManager::fInstance;
}


jobject
KeyboardFocusManager::getFocusedComponentPeer()
{
	return focusedPeer;
}

jboolean
KeyboardFocusManager::processSynchronousLightweightTransfer(JNIEnv *env, jobject heavyweight, 
	                                                          jobject descendant, 
	                                                          jboolean temporary, 
	                                                          jboolean focusedWindowChangeAllowed, 
	                                                          jlong time)
{
	return env->CallStaticBooleanMethod(keyboardFocusManagerClass_ID,
	                                    KeyboardFocusManager::processSynchronousLightweightTransfer_ID,
	                                    heavyweight, descendant, temporary, focusedWindowChangeAllowed, time);
}


/**
 * If passed NULL, this will clear all focus
 */
void
KeyboardFocusManager::SetFocusedComponent(ComponentAdapter* component)
{
	JNIEnv * jenv = GetEnv();
	
	UnfocusComponent(jenv);
	if (focusedPeer != NULL) {
		jenv->DeleteGlobalRef(focusedPeer);
		focusedPeer = NULL;
	}
	if (component == NULL) {
		focusedPeer = NULL;
	} else {
		focusedPeer = component->GetPeer(jenv);
	}
}

/**
 * Invokes BView::MakeFocus(false) on the adapter of the previously focused component.
 */
void
KeyboardFocusManager::UnfocusComponent(JNIEnv *jenv)
{
	ComponentAdapter *adapter = GetFocusedAdapter(jenv);
	if (adapter != NULL) {
		adapter->MakeFocus(false);
	}
}

/**
 * Gets the Component Adapter of the previously focused component if the component hasn't been disposed.
 *
 * Returns NULL if the adapter is no longer valid.
 */
ComponentAdapter*
KeyboardFocusManager::GetFocusedAdapter(JNIEnv *jenv)
{
	ComponentAdapter *adapter = NULL;

	if (focusedPeer != NULL &&
	    (jenv->GetBooleanField(focusedPeer, ObjectAdapter::disposed_ID) == JNI_FALSE))
	{
		adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, focusedPeer);
	}

	return adapter;
}

bool
KeyboardFocusManager::NativelyFocus(JNIEnv *env, ComponentAdapter *component, BMessage *message) {
	bool ret = false;
	// Java invocation objects
	jobject jheavyweight = component->GetTarget(env);
	jobject jlightweightChild = NULL;
	jboolean jtemporary = JNI_FALSE;
	jboolean jfocusedWindowChangeAllowed = JNI_FALSE;
	jlong jwhen = 0;
	
	// BMessage payloads (created in sun_awt_haiku_BComponentPeer.h)
	bool temp;
	bool focused;
	int32 when;
	
	// Obtain and translate.
	message->FindPointer("lightweightChild", &(void*)jlightweightChild);
	message->FindBool("temporary", &temp);
	message->FindBool("focusedWindowChangeAllowed", &focused);
	message->FindInt32("when", &when);
	jtemporary = temp ? JNI_TRUE : JNI_FALSE;
	jfocusedWindowChangeAllowed = focused ? JNI_TRUE : JNI_FALSE;
	jwhen = (jlong)when;
	
	// Call the method.
	jint jretval = env->CallStaticIntMethod(keyboardFocusManagerClass_ID,
									KeyboardFocusManager::shouldNativelyFocusHeavyweight_ID,
									jheavyweight, jlightweightChild, jtemporary, 
									jfocusedWindowChangeAllowed, jwhen);
	// Cleanup the global ref pointer.
	env->DeleteGlobalRef(jlightweightChild);
	
	if (jretval == java_awt_KeyboardFocusManager_SNFH_SUCCESS_HANDLED) {
		// Duplicate focus request?
		fprintf(stderr, "  Focus Request Previously handled.\n");
		ret = true;
	} else if (jretval == java_awt_KeyboardFocusManager_SNFH_SUCCESS_PROCEED) {
		UnfocusComponent(env);
		
		// determine if we need to activate a new window.
		ComponentAdapter *adapter = GetFocusedAdapter(env);
		if (adapter != NULL &&
		   (adapter->GetView()->Window() != component->GetView()->Window()))
		{
			component->GetView()->Window()->PostMessage(B_WINDOW_ACTIVATED);
		}
		
		component->MakeFocus(true);
		ret = component->GetView()->IsFocus();
	} else {
		fprintf(stderr, "   Uh.... FAILURE?!\n");
	}
	return ret;
}




void 
KeyboardFocusManager::heavyweightButtonDown(JNIEnv *env, jobject target, jlong when)
{
	fprintf(stderr, "heavyweightButtonDown()\n");
	env->CallStaticVoidMethod(keyboardFocusManagerClass_ID, 
	                          heavyweightButtonDown_ID, target, when);
	return;
}
