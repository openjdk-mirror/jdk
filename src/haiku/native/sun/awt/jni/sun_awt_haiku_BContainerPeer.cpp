#include "sun_awt_haiku_BContainerPeer.h"
#include "ContainerAdapter.h"
#include "debug_util.h"

jfieldID ContainerAdapter::insets_ID = NULL;

/*
 * Class:     sun_awt_haiku_BContainerPeer
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BContainerPeer_initIDs
  (JNIEnv * jenv, jclass jklass)
{
	ContainerAdapter::insets_ID = jenv->GetFieldID(jklass, "fInsets", "Ljava/awt/Insets;");
	DASSERT(ContainerAdapter::insets_ID != NULL);
}


/*
 * Class:     sun_awt_haiku_BContainerPeer
 * Method:    _layoutBeginning
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BContainerPeer__1layoutBeginning
  (JNIEnv * jenv, jobject jpeer)
{
	ContainerAdapter * adapter = ObjectAdapter::getAdapter<ContainerAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->layoutBeginning();
}


/*
 * Class:     sun_awt_haiku_BContainerPeer
 * Method:    _layoutEnded
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_haiku_BContainerPeer__1layoutEnded
  (JNIEnv * jenv, jobject jpeer)
{
	ContainerAdapter * adapter = ObjectAdapter::getAdapter<ContainerAdapter>(jenv, jpeer);
	if (adapter == NULL) {
		return;
	}
	adapter->layoutEnded();
}

