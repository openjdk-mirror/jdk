#include "sun_awt_haiku_BObjectPeer.h"
#include "ObjectAdapter.h"

jfieldID ObjectAdapter::nativeObject_ID = NULL;
jfieldID ObjectAdapter::javaObject_ID = NULL;
jfieldID ObjectAdapter::disposed_ID = NULL;
jmethodID ObjectAdapter::getPeerForTarget_ID = NULL;

/*
 * Class:     sun_awt_haiku_BObjectPeer
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BObjectPeer_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	ObjectAdapter::nativeObject_ID     = jenv->GetFieldID(jklass, "pData", "J");
	ObjectAdapter::javaObject_ID       = jenv->GetFieldID(jklass, "target", "Ljava/lang/Object;");
	ObjectAdapter::disposed_ID       = jenv->GetFieldID(jklass, "disposed", "Z");
	ObjectAdapter::getPeerForTarget_ID = jenv->GetMethodID(jklass, "getPeerForTarget", 
	                                                       "(Ljava/lang/Object;)Lsun/awt/haiku/BObjectPeer;");
}



