#ifndef FONT_H
#define FONT_H

#include <jni.h>
#include <jni_util.h>
#include <interface/Font.h>
#include "java_awt_Font.h" // useful constants are in here

class Font {
public:
	/* java.awt.Font field ids */
	static jfieldID style_ID;
	static jfieldID pointSize_ID;
	static jmethodID getFamily_ID;

	static BFont GetFont(JNIEnv * jenv, jobject jfont);
};

#endif // FONT_H
