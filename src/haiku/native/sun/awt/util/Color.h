#ifndef COLOR_H
#define COLOR_H

#include <jni.h>
#include <jni_util.h>
#include <GraphicsDefs.h>
#include "java_awt_Color.h" // useful constants are in here

class Color {
public:
	/* java.awt.Color field ids */
	static jfieldID value_ID;
	static jclass   color_ID;
	static jmethodID constructor_ID;

	static rgb_color GetColor(JNIEnv * jenv, jobject jcolor);
	static rgb_color GetColor(jint jcolor);
	static jobject   NewColor(JNIEnv * jenv, rgb_color color);
	static jint      NewColor(rgb_color color);
};

#endif // COLOR_H
