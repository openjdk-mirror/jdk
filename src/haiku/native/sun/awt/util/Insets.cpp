#include "Insets.h"
#include "debug_util.h"


/* static */ jobject
Insets::New(JNIEnv * jenv, BRect insets)
{
	return jenv->NewObject(classReference, constructor_ID, insets.top, insets.left, insets.bottom, insets.right);
}


// #pragma mark -
//
// JNI
//

#include "java_awt_Insets.h"

jfieldID Insets::top_ID    = NULL;
jfieldID Insets::left_ID   = NULL;
jfieldID Insets::bottom_ID = NULL;
jfieldID Insets::right_ID  = NULL;
jclass    Insets::classReference = NULL;
jmethodID Insets::constructor_ID = NULL;

/*
 * Class:     java_awt_Insets
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Insets_initIDs
  (JNIEnv * jenv, jclass jklass)
{
    Insets::top_ID    = jenv->GetFieldID(jklass, "top", "I");
    Insets::left_ID   = jenv->GetFieldID(jklass, "left", "I");
    Insets::bottom_ID = jenv->GetFieldID(jklass, "bottom", "I");
    Insets::right_ID  = jenv->GetFieldID(jklass, "right", "I");
	Insets::classReference = (jclass)jenv->NewGlobalRef(jklass);
	Insets::constructor_ID = jenv->GetMethodID(jklass, "<init>", "(IIII)V");

    DASSERT(Insets::top_ID         != NULL);
    DASSERT(Insets::left_ID        != NULL);
    DASSERT(Insets::bottom_ID      != NULL);
    DASSERT(Insets::right_ID       != NULL);
    DASSERT(Insets::classReference != NULL);
    DASSERT(Insets::constructor_ID != NULL);
}
