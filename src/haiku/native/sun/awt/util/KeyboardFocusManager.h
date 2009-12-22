#ifndef KEYBOARD_FOCUS_MANAGER_H
#define KEYBOARD_FOCUS_MANAGER_H

#include "jni.h"

class ComponentAdapter;
class KeyboardFocusManager {
	public:
				KeyboardFocusManager();
				~KeyboardFocusManager();
		
		static	KeyboardFocusManager* GetInstance();
	
	public: // JNI Invoked methods
		void             	heavyweightButtonDown(JNIEnv *env, jobject target, jlong when);
		
		jobject	            getFocusedComponentPeer();
		
		jboolean            processSynchronousLightweightTransfer(JNIEnv *env, jobject heavyweight, 
		                                                          jobject descendant, 
		                                                          jboolean temporary, 
		                                                          jboolean focusedWindowChangeAllowed, 
		                                                          jlong time);
	public: // Called from Haiku classes	
		void             	SetFocusedComponent(ComponentAdapter *adapter);
		bool				NativelyFocus(JNIEnv *env, ComponentAdapter *component, BMessage* message);
		
	protected:
		static	KeyboardFocusManager *fInstance;
		
		void				UnfocusComponent(JNIEnv *jenv);
		ComponentAdapter*	GetFocusedAdapter(JNIEnv *jenv);
		
	private:
		jobject	focusedPeer;
		
	public:	// -- Field and MethodID cache
		static jfieldID		focusOwner_ID;
		static jfieldID		permanentFocusOwner_ID;
		static jfieldID		focusedWindow_ID;
		static jfieldID		activeWindow_ID;
		
		static jmethodID	heavyweightButtonDown_ID;
		static jmethodID	shouldNativelyFocusHeavyweight_ID;
		static jmethodID	processSynchronousLightweightTransfer_ID;
	public: // -- Cached objects owned by the instance
		jclass		keyboardFocusManagerClass_ID;
};

#endif // KEYBOARD_FOCUS_MANAGER_H
