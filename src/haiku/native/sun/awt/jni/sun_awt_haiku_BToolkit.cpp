#include "sun_awt_SunToolkit.h"
#include "sun_awt_haiku_BToolkit.h"

#include "ComponentAdapter.h"
#include "MenuComponentAdapter.h"
#include "ToolkitAdapter.h"

#include <Beep.h>

#include "debug_util.h"
#include "jni.h"
#include "jni_util.h"

JavaVM *jvm = NULL;

/**
 * Called when the awt library is loaded.
 * 
 * Initializes the singleton instance of the ToolkitAdapter,
 * and tells it to begin the process of creating a BApplication
 * if we're not running embedded.
 */
JNIEXPORT jint JNICALL JNI_OnLoad
  (JavaVM *vm, void *reserved)
{
	jvm = vm;
	
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	toolkit->createApplicationThread();
	
	return JNI_VERSION_1_2;
}

/**
 * Handles setting up weather or not we should run in Headless AWT mode.
 */
extern "C" JNIEXPORT jboolean JNICALL AWTIsHeadless() {
	static JNIEnv *env = NULL;
	static jboolean isHeadless;
	jmethodID headlessFn;
	jclass graphicsEnvClass;

	if (env == NULL) {
		env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
		graphicsEnvClass = env->FindClass(
			"java/awt/GraphicsEnvironment");
		if (graphicsEnvClass == NULL) {
			return JNI_TRUE;
		}
		headlessFn = env->GetStaticMethodID(
			graphicsEnvClass, "isHeadless", "()Z");
		if (headlessFn == NULL) {
			return JNI_TRUE;
		}
		isHeadless = env->CallStaticBooleanMethod(graphicsEnvClass,
			headlessFn);
	}
	return isHeadless;
}

static jclass componentCls = NULL;
static jclass menuComponentCls = NULL;

/*
 * Class:	sun_awt_SunToolkit
 * Method:	getPrivateKey
 * Signature: (Ljava/lang/Object;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL
Java_sun_awt_SunToolkit_getPrivateKey(JNIEnv *env, jclass cls, jobject obj)
{
	jobject key = obj;

	// get global reference of java/awt/Component class (run only once)
	if (componentCls == NULL) {
		componentCls = (jclass)env->NewGlobalRef(env->FindClass("java/awt/Component"));
	}

	// get global reference of java/awt/MenuComponent class (run only once)
	if (menuComponentCls == NULL) {
		menuComponentCls = (jclass)env->NewGlobalRef(env->FindClass("java/awt/MenuComponent"));
	}

	/*
	* Fix for BugTraq ID 4254701.
	* Don't use Components and MenuComponents as keys in hash maps.
	* We use private keys instead.
	*/
	if (env->IsInstanceOf(obj, componentCls)) {
		key = env->GetObjectField(obj, ComponentAdapter::privateKey_ID);
	} else if (env->IsInstanceOf(obj, menuComponentCls)) {
		key = env->GetObjectField(obj, MenuComponentAdapter::privateKey_ID);
	}
	return key;
}


/*
 * Class:	sun_awt_SunToolkit
 * Method:	wakeupEventQueue
 * Signature: (Ljava/awt/EventQueue;Z)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_SunToolkit_wakeupEventQueue(JNIEnv *env, jclass cls, jobject eq, jboolean b)
{
	// get global reference of java/awt/EventQueue class and its wakeup method
	// (run only once)
	static jclass eventQueueCls = NULL;
	static jmethodID wakeupMethodID = NULL;
	if (eventQueueCls == NULL) {
		jclass eventQueueClsLocal = env->FindClass("java/awt/EventQueue");
		DASSERT(eventQueueClsLocal != NULL);
		if (eventQueueClsLocal == NULL) {
			/* exception already thrown */
			return;
		}
		eventQueueCls = (jclass)env->NewGlobalRef(eventQueueClsLocal);
		env->DeleteLocalRef(eventQueueClsLocal);

		wakeupMethodID = env->GetMethodID(eventQueueCls,
						"wakeup", "(Z)V");
		DASSERT(wakeupMethodID != NULL);
		if (wakeupMethodID == NULL) {
			/* exception already thrown */
			return;
		}
	}

	DASSERT(env->IsInstanceOf(eq, eventQueueCls));
	env->CallVoidMethod(eq, wakeupMethodID, b);
}


/*
 * Class:	sun_awt_SunToolkit
 * Method:	setLWRequestStatus
 * Signature: (Ljava/lang/Object;Z)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_SunToolkit_setLWRequestStatus(JNIEnv *env, jclass cls, jobject win,
						jboolean status)
{
	fprintf(stderr, "BToolkit::setLWRequestStatus()\n");
	static jclass windowCls = NULL;
	static jfieldID lwRequestStatus;

	if (windowCls == NULL) {
		jclass windowClsLocal = env->FindClass("java/awt/Window");
		DASSERT(windowClsLocal != NULL);
		if (windowClsLocal == NULL) {
			return;
		}
		windowCls = (jclass)env->NewGlobalRef(windowClsLocal);
		env->DeleteLocalRef(windowClsLocal);
		lwRequestStatus = env->GetFieldID(windowCls, "syncLWRequests", "Z");
	}
	env->SetBooleanField(win, lwRequestStatus, status);
}


/*
 * Class:	sun_awt_SunTookit
 * Method:	setZOrder
 * Signature: (Ljava/awt/Container;Ljava/awt/Component;I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_SunToolkit_setZOrder(JNIEnv *env, jclass cls, 
							jobject cont, jobject comp, jint index)
{
	static jmethodID setZOrderMID = NULL;

	if (JNU_IsNull(env, cont)) {
		return;
	}

	if ( JNU_IsNull(env, setZOrderMID) ) {
		jclass containerCls = env->FindClass("java/awt/Container");
		DASSERT(!JNU_IsNull(env, containerCls));
		if ( JNU_IsNull(env, containerCls) ) {
			return;
		}
		setZOrderMID = env->GetMethodID(containerCls, "setZOrder",
									"(Ljava/awt/Component;I)V");
		DASSERT( !JNU_IsNull(env, setZOrderMID) );
		if ( JNU_IsNull(env, setZOrderMID) ) {
			return;
		}
	}

	env->CallVoidMethod(cont, setZOrderMID, comp, index);
}




/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _printHaikuVersion
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BToolkit__1printHaikuVersion
  (JNIEnv * jenv, jclass jklass)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	toolkit->printSystemVersion();
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _init
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_haiku_BToolkit__1init
  (JNIEnv * jenv, jobject jpeer)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	toolkit->setPeer(jenv, jpeer);
	return JNI_TRUE;
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _eventLoop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BToolkit__1eventLoop
  (JNIEnv * jenv, jobject jpeer)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	toolkit->runApplicationThread();
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _shutdown
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BToolkit__1shutdown
  (JNIEnv * jenv, jobject jpeer)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	toolkit->haltApplication();
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _finalize
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BToolkit__1finalize
  (JNIEnv * jenv, jobject jpeer)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	delete toolkit;
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _getScreenResolution
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BToolkit__1getScreenResolution
  (JNIEnv * jenv, jobject jpeer)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	return toolkit->getScreenResolution();
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _getScreenWidth
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BToolkit__1getScreenWidth
  (JNIEnv * jenv, jobject jpeer)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	return toolkit->getScreenWidth();
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _getScreenHeight
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BToolkit__1getScreenHeight
  (JNIEnv * jenv, jobject jpeer)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	return toolkit->getScreenHeight();
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _getFontSize
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BToolkit__1getFontSize
  (JNIEnv * jenv, jclass jklass, jint type)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	return toolkit->getFontSize(type);
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _getFontStyle
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BToolkit__1getFontStyle
  (JNIEnv * jenv, jclass jklass, jint type)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	return toolkit->getFontStyle(type);
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _sync
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BToolkit__1sync
  (JNIEnv * jenv, jobject jpeer)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	toolkit->sync();
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _beep
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BToolkit__1beep
  (JNIEnv * jenv, jobject jpeer)
{
	beep();
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _loadSystemColors
 * Signature: ([I)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BToolkit__1loadSystemColors
  (JNIEnv * jenv, jobject jpeer, jintArray)
{
	fprintf(stderr, "TODO: private native BToolkit._loadSystemColors\n");
	return 0;
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _getMenuShortcutKeyMask
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BToolkit__1getMenuShortcutKeyMask
  (JNIEnv * jenv, jobject jpeer)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	return toolkit->getMenuShortcutKeyMask();
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _getLockingKeyState
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_haiku_BToolkit__1getLockingKeyState
  (JNIEnv * jenv, jobject jpeer, jint key)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	return toolkit->isKeyLocked(key);
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _setLockingKeyState
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BToolkit__1setLockingKeyState
  (JNIEnv * jenv, jobject jpeer, jint key, jboolean on)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	toolkit->setKeyLocked(key, on);
}

/*
 * Class:     sun_awt_haiku_BToolkit
 * Method:    _getMulticlickTime
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_haiku_BToolkit__1getMulticlickTime
  (JNIEnv * jenv, jobject jpeer)
{
	ToolkitAdapter *toolkit = ToolkitAdapter::GetInstance();
	return toolkit->getMulticlickTime();
}
