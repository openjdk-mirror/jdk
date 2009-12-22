#include "sun_awt_haiku_BColor.h"

#include "jni.h"
#include <InterfaceDefs.h>

/*
 * Class:     sun_awt_haiku_BColor
 * Method:    getDefaultColor
 * Signature: (I)Ljava/awt/Color;
 */
JNIEXPORT jobject JNICALL Java_sun_awt_haiku_BColor_getDefaultColor
  (JNIEnv * jenv, jclass jpeer, jint color)
{
	jclass jklass = jenv->FindClass("java/awt/Color");
	jmethodID constructor = jenv->GetMethodID(jklass, "<init>", "(III)V");
	jobject jcolor = jenv->NewObject(jklass, constructor, 0, 0, 0);
	rgb_color bcolor;
	switch (color) {
		case sun_awt_haiku_BColor_WINDOW_BKGND:
			bcolor = ui_color(B_PANEL_BACKGROUND_COLOR);
			constructor = jenv->GetMethodID(jklass, "<init>", "(IIII)V");
			jcolor = jenv->NewObject(jklass, constructor, bcolor.red, bcolor.green, bcolor.blue, bcolor.alpha);
			break;
	}
	jenv->DeleteLocalRef(jklass);
	return jcolor;
}
