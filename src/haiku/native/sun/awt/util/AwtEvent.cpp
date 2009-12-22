#include "AwtEvent.h"
#include "debug_util.h"
#include <cstdio>

jfieldID AwtEvent::bdata_ID    = NULL;
jfieldID AwtEvent::id_ID       = NULL;
jfieldID AwtEvent::consumed_ID = NULL;

extern "C" {

/*
 * Class:     java_awt_AWTEvent
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_AWTEvent_initIDs
  (JNIEnv * jenv, jclass jklass)
{
    AwtEvent::bdata_ID    = jenv->GetFieldID(jklass, "bdata", "[B");
    AwtEvent::id_ID       = jenv->GetFieldID(jklass, "id", "I");
    AwtEvent::consumed_ID = jenv->GetFieldID(jklass, "consumed", "Z");

    DASSERT(AwtEvent::bdata_ID    != NULL);
    DASSERT(AwtEvent::id_ID       != NULL);
    DASSERT(AwtEvent::consumed_ID != NULL);
}

/*
 * Class:     java_awt_AWTEvent
 * Method:    nativeSetSource
 * Signature: (Ljava/awt/peer/ComponentPeer;)V
 */
JNIEXPORT void JNICALL Java_java_awt_AWTEvent_nativeSetSource
  (JNIEnv * jenv, jobject jself, jobject jnewSource)
{
	// I am not convinced anything needs to happen here.
	// TODO: convince ourselves more thoroughly that nothing needs to happen here.
//	ComponentAdapter *adapter = ObjectAdapter::getAdapter<ComponentAdapter>(jenv, jnewSource);
//	fprintf(stderr, "%s\n", __func__);
}


} /* extern "C" */
