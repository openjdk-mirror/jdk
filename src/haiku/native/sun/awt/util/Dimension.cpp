#include "Dimension.h"
#include "debug_util.h"
#include <cstdio>


/* static */ jobject
Dimension::New(JNIEnv * jenv, int width, int height)
{
	return jenv->NewObject(classReference, constructor_ID, width, height);
}


// #pragma mark -
//
// JNI
//

#include "java_awt_Dimension.h"

jfieldID  Dimension::width_ID       = NULL;
jfieldID  Dimension::height_ID      = NULL;
jclass    Dimension::classReference = NULL;
jmethodID Dimension::constructor_ID = NULL;

/*
 * Class:     java_awt_Dimension
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Dimension_initIDs
  (JNIEnv * jenv, jclass jklass)
{
    Dimension::width_ID       = jenv->GetFieldID(jklass, "width", "I");
    Dimension::height_ID      = jenv->GetFieldID(jklass, "height", "I");
	Dimension::classReference = (jclass)jenv->NewGlobalRef(jklass);
	Dimension::constructor_ID = jenv->GetMethodID(jklass, "<init>", "(II)V");

    DASSERT(Dimension::width_ID       != NULL);
    DASSERT(Dimension::height_ID      != NULL);
    DASSERT(Dimension::classReference != NULL);
    DASSERT(Dimension::constructor_ID != NULL);
}
