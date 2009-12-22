#include "Color.h"
#include "Debug.h"
#include <cstdio>

rgb_color
Color::GetColor(JNIEnv * jenv, jobject jcolor)
{
	jint c = jenv->GetIntField(jcolor, value_ID);
	return GetColor(c);
}


rgb_color
Color::GetColor(jint jcolor)
{
	rgb_color color;
	color.red = (jcolor >> 16) & 0xFF;
	color.green = (jcolor >> 8) & 0xFF;
	color.blue = (jcolor >> 0) & 0xFF;
	color.alpha = (jcolor >> 24) & 0xFF;
	return color;
}


jobject
Color::NewColor(JNIEnv * jenv, rgb_color color)
{
	DEBUGGER();
	return NULL;
}


jint
Color::NewColor(rgb_color color)
{
	DEBUGGER();
	return 0;
}


// #pragma mark -
//
// JNI
//

jfieldID  Color::value_ID       = NULL;
jclass    Color::color_ID       = NULL;
jmethodID Color::constructor_ID = NULL;

#include "java_awt_Color.h"

/*
 * Class:     java_awt_Color
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_Color_initIDs
  (JNIEnv * jenv, jclass jklass)
{
    Color::value_ID       = jenv->GetFieldID(jklass, "value", "I");
	Color::color_ID       = (jclass)jenv->NewGlobalRef((jobject)jklass);
    Color::constructor_ID = jenv->GetMethodID(jklass, "<init>", "(IIII)V");
    DASSERT(Color::value_ID       != NULL);
    DASSERT(Color::color_ID       != NULL);
    DASSERT(Color::constructor_ID != NULL);
}
