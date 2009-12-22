#ifndef OBJECT_ADAPTER_H
#define OBJECT_ADAPTER_H

#include <Handler.h>
#include "Debug.h"
#include "Utils.h"

class ObjectAdapter : public BHandler {
private:
	jobject   jpeer;
	bool      callbacksEnabled;

public:     // initialization and destruction
	        ObjectAdapter();
	virtual ~ObjectAdapter();
    void	LinkObjects(JNIEnv * jenv, jobject jpeer);
    void	UnlinkObjects();

public:
	// Return the associated AWT peer or target object.
	inline jobject GetPeer(JNIEnv * jenv) {
		return jpeer;
	}
	
	inline jobject GetTarget(JNIEnv * jenv) {
		jobject jpeer = GetPeer(jenv);
		jobject target = NULL;
		if (jpeer != NULL) {
			target = jenv->GetObjectField(jpeer, javaObject_ID);
		}
		return target;
	}
	
	inline jobject GetTargetAsGlobalRef(JNIEnv * jenv) {
		jobject jlocalRef = GetTarget(jenv);
		if (jlocalRef == NULL) {
			return NULL;
		}
		jobject jglobalRef = jenv->NewGlobalRef(jlocalRef);
		jenv->DeleteLocalRef(jlocalRef);
		return jglobalRef;
	}
	
	// Return the peer associated with some target
	jobject GetPeerForTarget(JNIEnv * jenv, jobject jtarget);
	
	// Java callback routines
	// Invoke a callback on the java peer object asynchronously
	void DoCallback(const char* methodName, const char* methodSig, ...);
	
	// Allocate and initialize a new event, and posit it to the peer's
	// target object. No response is expected from the target.
	void SendEvent(jobject event);
	
	inline void EnableCallbacks(bool e) { callbacksEnabled = e; }
	
	// Execute any code associated with a command ID -- only classes with
	// DoCommand() defined should associtate their instances with cmdIDs.
	virtual void DoCommand(void) {
		DASSERT(FALSE);
	}

public: // jni lookup function
	template <class T>
	static T * getAdapter(JNIEnv * jenv, jobject jpeer, bool inCreate = false) {
		DASSERT(jenv != NULL);
		if (jpeer == NULL) {
			JNU_ThrowNullPointerException(jenv, "ObjectAdapter::getAdapter(jenv, NULL): jpeer null");
			return NULL;
		}
		T * adapter = dynamic_cast<T*>((ObjectAdapter*)jenv->GetLongField(jpeer, nativeObject_ID));
		if (adapter == NULL) {
			if (inCreate) {
				JNU_ThrowOutOfMemoryError(jenv, "ObjectAdapter::getAdapter: failed to create an ObjectAdapter subclass");
			} else {
				JNU_ThrowNullPointerException(jenv, "ObjectAdapter::getAdapter: failed to cast an ObjectAdapter to the requested subclass");
			}
		}
		return adapter;
	}

public:
	/* sun.awt.haiku.BObjectPeer field and method ids */
	static jfieldID nativeObject_ID; // ButtonAdapter*  (aka pData)
	static jfieldID javaObject_ID;   // java.awt.Button (aka target)
	static jfieldID disposed_ID;
	static jmethodID getPeerForTarget_ID;
};

#endif // OBJECT_ADAPTER_H
