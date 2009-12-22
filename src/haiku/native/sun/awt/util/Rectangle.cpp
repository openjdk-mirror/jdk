#include "Rectangle.h"
#include "java_awt_Rectangle.h"
#include "debug_util.h"

jfieldID Rectangle::x_ID      = NULL;
jfieldID Rectangle::y_ID      = NULL;
jfieldID Rectangle::width_ID  = NULL;
jfieldID Rectangle::height_ID = NULL;

extern "C" {

/*
 * Class:     java_awt_Rectangle
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Rectangle_initIDs
  (JNIEnv * jenv, jclass jklass)
{
    Rectangle::x_ID      = jenv->GetFieldID(jklass, "x", "I");
    Rectangle::y_ID      = jenv->GetFieldID(jklass, "y", "I");
    Rectangle::width_ID  = jenv->GetFieldID(jklass, "width", "I");
    Rectangle::height_ID = jenv->GetFieldID(jklass, "height", "I");

    DASSERT(Rectangle::x_ID      != NULL);
    DASSERT(Rectangle::y_ID      != NULL);
    DASSERT(Rectangle::width_ID  != NULL);
    DASSERT(Rectangle::height_ID != NULL);
}

} /* extern "C" */
